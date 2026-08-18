package com.vdbond.slots.cli;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CommandParser {

    public Command parse(String line) {
        List<String> words = Arrays.stream(line.trim().split("\\s+"))
                .filter(word -> !word.isEmpty())
                .toList();
        if (words.isEmpty()) {
            throw new CommandParseException(
                    "Type a command to play — --spin spins the reels, and --help lists everything you can do."
            );
        }
        String typed = words.getFirst();
        List<String> arguments = words.subList(1, words.size());
        return switch (typed.toLowerCase().replaceFirst("^--", "")) {
            case "spin" -> new Spin(optionalAmount(arguments, "--spin"));
            case "deposit" -> new Deposit(requiredAmount(arguments, "--deposit"));
            case "paytable" -> alone(arguments, "--paytable", new ShowPaytable());
            case "rules" -> alone(arguments, "--rules", new ShowRules());
            case "report" -> alone(arguments, "--report", new ShowReport());
            case "help" -> alone(arguments, "--help", new ShowHelp());
            case "exit" -> alone(arguments, "--exit", new Exit());
            default -> throw new CommandParseException(
                    "'%s' is not something I know how to do. Type --help to see the full list of commands."
                            .formatted(typed)
            );
        };
    }

    private <T extends Command> T alone(List<String> arguments, String command, T parsed) {
        if (!arguments.isEmpty()) {
            throw new CommandParseException(
                    "%s is typed on its own, but '%s' followed it. Just type %s."
                            .formatted(command, String.join(" ", arguments), command)
            );
        }
        return parsed;
    }

    private Optional<BigDecimal> optionalAmount(List<String> arguments, String command) {
        return arguments.isEmpty() ? Optional.empty() : Optional.of(onlyAmount(arguments, command));
    }

    private BigDecimal requiredAmount(List<String> arguments, String command) {
        if (arguments.isEmpty()) {
            throw new CommandParseException(
                    "%s needs an amount after it, for example: %s 100".formatted(command, command)
            );
        }
        return onlyAmount(arguments, command);
    }

    private BigDecimal onlyAmount(List<String> arguments, String command) {
        if (arguments.size() > 1) {
            throw new CommandParseException(
                    "%s takes one amount at most, but '%s' came after it. For example: %s 25"
                            .formatted(command, String.join(" ", arguments), command)
            );
        }
        return amount(arguments.getFirst(), command);
    }

    private BigDecimal amount(String text, String command) {
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException ex) {
            throw new CommandParseException(
                    "'%s' is not an amount I can read. Use plain digits, like %s 25 or %s 12.50."
                            .formatted(text, command, command)
            );
        }
    }

}
