package com.tep.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.axonframework.serialization.Revision;

import java.math.BigDecimal;

@Revision("1.0")
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class WalletCreatedEvent {
    private String walletId;
    private BigDecimal balance;
}
