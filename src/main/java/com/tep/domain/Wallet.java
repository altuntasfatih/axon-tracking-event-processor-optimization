package com.tep.domain;

import com.tep.command.CreateWalletCommand;
import com.tep.command.DepositCommand;
import com.tep.event.DepositedEvent;
import com.tep.event.WalletCreatedEvent;
import com.tep.exception.DepositLimitExceedException;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;

import java.math.BigDecimal;


@Getter
@NoArgsConstructor
public class Wallet {
    public static final BigDecimal DEPOSIT_LIMIT = new BigDecimal("750.00");

    @AggregateIdentifier
    private String walletId;
    private BigDecimal balance;

    @CommandHandler
    public Wallet(CreateWalletCommand command) {
        var event = new WalletCreatedEvent(command.getWalletId(), BigDecimal.ZERO);
        AggregateLifecycle.apply(event);
    }

    @EventSourcingHandler
    protected void on(WalletCreatedEvent event) {
        this.walletId = event.getWalletId();
        this.balance = event.getBalance();
    }

    @CommandHandler
    public void handle(DepositCommand command) {
        final BigDecimal depositAmount = command.getDepositAmount();
        checkDepositLimit(command.getDepositAmount());

        var event = new DepositedEvent(depositAmount);
        AggregateLifecycle.apply(event);
    }

    @EventSourcingHandler
    protected void on(DepositedEvent event) {
        this.balance = this.balance.add(event.getDepositAmount());
    }

    private void checkDepositLimit(BigDecimal depositAmount) {
        if (this.balance.add(depositAmount).compareTo(BigDecimal.valueOf(750)) > 0) {
            throw new DepositLimitExceedException();
        }
    }
}