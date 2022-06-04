package com.tep.command;

import lombok.Getter;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.math.BigDecimal;

@Getter
public class DepositCommand {

    @TargetAggregateIdentifier
    private final String walletId;
    private final BigDecimal depositAmount;

    public DepositCommand(String walletId, BigDecimal depositAmount) {
        this.walletId = walletId;
        this.depositAmount = depositAmount;
    }
}