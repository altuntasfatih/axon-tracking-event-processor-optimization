package com.tep.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.axonframework.serialization.Revision;

import java.math.BigDecimal;

@Revision("1.0")
@Getter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class DepositedEvent {
    private BigDecimal depositAmount;
}
