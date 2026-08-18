package com.vdbond.slots.config;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record ConfigDto(
        BigDecimal defaultBalance,
        BigDecimal defaultBet,
        Double targetRtp,
        Map<String, List<String>> reels,
        List<List<String>> paylines,
        Map<String, BigDecimal> paytable
) {
}
