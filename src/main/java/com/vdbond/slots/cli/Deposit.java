package com.vdbond.slots.cli;

import java.math.BigDecimal;

public record Deposit(BigDecimal amount) implements Command {
}
