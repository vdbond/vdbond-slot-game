package com.vdbond.slots.config;

import com.vdbond.slots.model.GameConfig;
import com.vdbond.slots.model.Payline;
import com.vdbond.slots.model.Reel;
import com.vdbond.slots.model.Row;
import com.vdbond.slots.model.Symbol;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@UtilityClass
public class ConfigLoader {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    public GameConfig load(InputStream json) {
        ConfigDto dto = read(json);
        return new GameConfig(
                positiveAmount(required(dto.defaultBalance(), "defaultBalance"), "defaultBalance"),
                positiveAmount(required(dto.defaultBet(), "defaultBet"), "defaultBet"),
                targetRtp(required(dto.targetRtp(), "targetRtp")),
                reelStrips(required(dto.reels(), "reels")),
                paylines(required(dto.paylines(), "paylines")),
                paytable(required(dto.paytable(), "paytable")));
    }

    private ConfigDto read(InputStream json) {
        try {
            return MAPPER.readValue(json, ConfigDto.class);
        } catch (JacksonException ex) {
            throw new ConfigLoadException(
                    "The configuration could not be read as JSON: %s".formatted(ex.getOriginalMessage()), ex
            );
        }
    }

    private <T> T required(T value, String field) {
        if (value == null) {
            throw new ConfigLoadException("The configuration is missing the required field '%s'".formatted(field));
        }
        return value;
    }

    private BigDecimal positiveAmount(BigDecimal amount, String field) {
        if (amount.signum() <= 0) {
            throw new ConfigLoadException(
                    "'%s' must be greater than zero, but was %s".formatted(field, amount.toPlainString())
            );
        }
        return amount;
    }

    private double targetRtp(double rtp) {
        if (rtp <= 0) {
            throw new ConfigLoadException(
                    ("'targetRtp' must be greater than zero — it is the share of all stakes the game pays "
                            + "back over time, so 0.96 means 96%% — but was %s").formatted(rtp)
            );
        }
        return rtp;
    }

    private Map<Reel, List<Symbol>> reelStrips(Map<String, List<String>> reels) {
        Map<Reel, List<Symbol>> strips = new EnumMap<>(Reel.class);
        reels.forEach((name, codes) -> {
            Reel reel = parseReel(name);
            if (codes == null || codes.isEmpty()) {
                throw new ConfigLoadException(
                        "The %s reel has no symbols on its strip. Every reel needs at least one".formatted(reel)
                );
            }
            strips.put(reel, codes.stream()
                    .map(code -> parseSymbol(code, "the %s reel strip".formatted(reel)))
                    .toList());
        });
        for (Reel reel : Reel.values()) {
            if (!strips.containsKey(reel)) {
                throw new ConfigLoadException(
                        "The configuration has no strip for the %s reel. 'reels' must list all %d: %s"
                                .formatted(reel, Reel.values().length, names(Reel.values()))
                );
            }
        }
        return strips;
    }

    private List<Payline> paylines(List<List<String>> lines) {
        if (lines.isEmpty()) {
            throw new ConfigLoadException(
                    "'paylines' is empty, so no spin could ever win. At least one payline is required"
            );
        }
        List<Payline> paylines = new ArrayList<>();
        for (int index = 0; index < lines.size(); index++) {
            paylines.add(payline(index, lines.get(index)));
        }
        return paylines;
    }

    private Payline payline(int index, List<String> rows) {
        int number = index + 1;
        if (rows == null || rows.size() != Reel.values().length) {
            throw new ConfigLoadException(
                    ("Payline %d names %d rows, but a payline needs exactly one row per reel (%d), "
                            + "in reel order: %s").formatted(
                            number,
                            rows == null ? 0 : rows.size(),
                            Reel.values().length,
                            names(Reel.values())
                    )
            );
        }
        Iterator<String> rowNames = rows.iterator();
        return new Payline(index,
                parseRow(rowNames.next(), number, Reel.FIRST),
                parseRow(rowNames.next(), number, Reel.SECOND),
                parseRow(rowNames.next(), number, Reel.THIRD));
    }

    private Map<Symbol, BigDecimal> paytable(Map<String, BigDecimal> entries) {
        Map<Symbol, BigDecimal> paytable = new EnumMap<>(Symbol.class);
        entries.forEach((code, multiplier) -> {
            Symbol symbol = parseSymbol(code, "the paytable");
            if (multiplier == null || multiplier.signum() < 0) {
                throw new ConfigLoadException(
                        "The paytable multiplier for %s must be zero or more, but was %s".formatted(code, multiplier)
                );
            }
            paytable.put(symbol, multiplier);
        });
        String missing = Arrays.stream(Symbol.values())
                .filter(symbol -> !paytable.containsKey(symbol))
                .map(Symbol::name)
                .collect(Collectors.joining(", "));
        if (!missing.isEmpty()) {
            throw new ConfigLoadException(
                    ("The paytable has no multiplier for: %s. Every symbol needs an entry — use 0 for "
                            + "one that never pays on its own").formatted(missing)
            );
        }
        return paytable;
    }

    private Symbol parseSymbol(String code, String where) {
        try {
            return Symbol.fromCode(code);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new ConfigLoadException("'%s' in %s is not a known symbol code. Valid codes are: %s"
                    .formatted(code, where, names(Symbol.values())));
        }
    }

    private Row parseRow(String name, int paylineNumber, Reel reel) {
        try {
            return Row.valueOf(name);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new ConfigLoadException(
                    "Payline %d uses '%s' for the %s reel, which is not a row name. Valid rows are: %s"
                            .formatted(paylineNumber, name, reel, names(Row.values()))
            );
        }
    }

    private Reel parseReel(String name) {
        try {
            return Reel.valueOf(name);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new ConfigLoadException(
                    "'%s' is not a reel name. 'reels' must be keyed by: %s".formatted(name, names(Reel.values()))
            );
        }
    }

    private String names(Enum<?>[] values) {
        return Arrays.stream(values).map(Enum::name).collect(Collectors.joining(", "));
    }

}
