package com.vdbond.slots.cli;

import java.math.BigDecimal;
import java.util.Optional;

public record Spin(Optional<BigDecimal> amount) implements Command {
}
