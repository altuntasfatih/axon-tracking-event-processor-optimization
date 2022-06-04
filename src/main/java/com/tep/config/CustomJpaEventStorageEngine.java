package com.tep.config;

import lombok.extern.slf4j.Slf4j;
import org.axonframework.common.Assert;
import org.axonframework.common.DateTimeUtils;
import org.axonframework.common.transaction.TransactionManager;
import org.axonframework.eventhandling.*;
import org.axonframework.eventsourcing.eventstore.jpa.JpaEventStorageEngine;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

@Slf4j
public class CustomJpaEventStorageEngine extends JpaEventStorageEngine {

    private final TransactionManager transactionManager;
    private static final int DEFAULT_GAP_TIMEOUT = 60000;
    private static final int DEFAULT_GAP_CLEANING_THRESHOLD = 250;
    private static final int DEFAULT_MAX_GAP_OFFSET = 10000;
    private static final long DEFAULT_LOWEST_GLOBAL_SEQUENCE = 1L;

    protected CustomJpaEventStorageEngine(Builder builder, TransactionManager transactionManager) {
        super(builder);
        this.transactionManager = transactionManager;
    }

    private List<Object[]> getEvents(GapAwareTrackingToken previousToken, int batchSize) {

        List<Object[]> events = new ArrayList<>();
        if (!Objects.isNull(previousToken) && !previousToken.getGaps().isEmpty()) {
            List<Object[]> gapResult = entityManager()
                    .createQuery("SELECT e.globalIndex, e.type, e.aggregateIdentifier, e.sequenceNumber, e.eventIdentifier, " + "e.timeStamp, e.payloadType, e.payloadRevision, e.payload, e.metaData " + "FROM " + domainEventEntryEntityName() + " e " + "WHERE e.globalIndex IN :gaps ORDER BY e.globalIndex ASC", Object[].class)
                    .setParameter("gaps", previousToken.getGaps())
                    .setMaxResults(batchSize)
                    .getResultList();
            events.addAll(gapResult);
        }
        List<Object[]> indexResult = entityManager()
                .createQuery("SELECT e.globalIndex, e.type, e.aggregateIdentifier, e.sequenceNumber, e.eventIdentifier, " + "e.timeStamp, e.payloadType, e.payloadRevision, e.payload, e.metaData " + "FROM " + domainEventEntryEntityName() + " e " + "WHERE e.globalIndex > :token ORDER BY e.globalIndex ASC", Object[].class)
                .setParameter("token", previousToken == null ? -1L : previousToken.getIndex())
                .setMaxResults(batchSize - events.size())
                .getResultList();

        events.addAll(indexResult);
        return events;
    }

    @Override
    protected List<? extends TrackedEventData<?>> fetchTrackedEvents(TrackingToken lastToken, int batchSize) {
        Assert.isTrue(lastToken == null || lastToken instanceof GapAwareTrackingToken, () -> String.format("Token [%s] is of the wrong type. Expected [%s]", lastToken, GapAwareTrackingToken.class.getSimpleName()));

        GapAwareTrackingToken previousToken = cleanedToken((GapAwareTrackingToken) lastToken);

        List<Object[]> entries = transactionManager.fetchInTransaction(() -> (getEvents(previousToken, batchSize)));

        List<TrackedEventData<?>> result = new ArrayList<>();
        GapAwareTrackingToken token = previousToken;
        for (Object[] entry : entries) {
            long globalSequence = (Long) entry[0];
            String aggregateIdentifier = (String) entry[2];
            String eventIdentifier = (String) entry[4];
            GenericDomainEventEntry<?> domainEvent = new GenericDomainEventEntry<>((String) entry[1], eventIdentifier.equals(aggregateIdentifier) ? null : aggregateIdentifier, (long) entry[3], eventIdentifier, entry[5], (String) entry[6], (String) entry[7], entry[8], entry[9]);

            // Now that we have the event itself, we can calculate the token
            boolean allowGaps = domainEvent.getTimestamp().isAfter(gapTimeoutFrame());
            if (token == null) {
                token = GapAwareTrackingToken.newInstance(globalSequence, allowGaps ? LongStream.range(Math.min(DEFAULT_LOWEST_GLOBAL_SEQUENCE, globalSequence), globalSequence).boxed().collect(Collectors.toCollection(TreeSet::new)) : Collections.emptySortedSet());
            } else {
                token = token.advanceTo(globalSequence, DEFAULT_MAX_GAP_OFFSET);
                if (!allowGaps) {
                    token = token.withGapsTruncatedAt(globalSequence);
                }
            }
            result.add(new TrackedDomainEventData<>(token, domainEvent));
        }
        return result;
    }

    private GapAwareTrackingToken cleanedToken(GapAwareTrackingToken lastToken) {
        GapAwareTrackingToken previousToken = lastToken;
        if (lastToken != null && lastToken.getGaps().size() > DEFAULT_GAP_CLEANING_THRESHOLD) {
            List<Object[]> results = transactionManager.fetchInTransaction(() -> entityManager().createQuery("SELECT e.globalIndex, e.timeStamp FROM " + domainEventEntryEntityName() + " e " + "WHERE e.globalIndex >= :firstGapOffset " + "AND e.globalIndex <= :maxGlobalIndex", Object[].class).setParameter("firstGapOffset", lastToken.getGaps().first()).setParameter("maxGlobalIndex", lastToken.getGaps().last() + 1L).getResultList());
            for (Object[] result : results) {
                try {
                    Instant timestamp = DateTimeUtils.parseInstant(result[1].toString());
                    long sequenceNumber = (long) result[0];
                    if (previousToken.getGaps().contains(sequenceNumber) || timestamp.isAfter(gapTimeoutFrame())) {
                        // filled a gap, should not continue cleaning up
                        break;
                    }
                    if (previousToken.getGaps().contains(sequenceNumber - 1)) {
                        previousToken = previousToken.withGapsTruncatedAt(sequenceNumber);
                    }
                } catch (DateTimeParseException e) {
                    log.info("Unable to parse timestamp to clean old gaps", e);
                    break;
                }
            }
        }
        return previousToken;
    }

    private Instant gapTimeoutFrame() {
        return GenericEventMessage.clock.instant().minus(DEFAULT_GAP_TIMEOUT, ChronoUnit.MILLIS);
    }
}