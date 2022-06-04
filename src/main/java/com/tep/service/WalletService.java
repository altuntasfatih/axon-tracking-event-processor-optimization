package com.tep.service;

import com.tep.command.*;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class WalletService {

    private final CommandGateway commandGateway;

    public WalletService(CommandGateway commandGateway) {
        this.commandGateway = commandGateway;
    }

    public void create(String walletId) {
        final CreateWalletCommand command = new CreateWalletCommand(walletId);
        sendCommand(command);
    }

    public void deposit(String walletId, BigDecimal depositAmount) {
        final DepositCommand command = new DepositCommand(walletId, depositAmount);
        sendCommand(command);
    }

    protected <T> T sendCommand(Object command) {
        return commandGateway.sendAndWait(command);
    }
}