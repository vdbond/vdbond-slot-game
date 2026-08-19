package com.vdbond.slots;

import com.vdbond.slots.cli.OutputRenderer;
import com.vdbond.slots.cli.ReplLoop;
import com.vdbond.slots.config.ConfigLoadException;
import com.vdbond.slots.config.ConfigLoader;
import com.vdbond.slots.model.GameConfig;
import com.vdbond.slots.session.GameSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class Main {

    private static final String DEFAULT_CONFIG_RESOURCE = "/config/default-config.json";

    private static final String BALANCE_FLAG = "--balance";

    public static void main(String[] args) {
        try {
            play(List.of(args));
        } catch (ConfigLoadException | IllegalArgumentException ex) {
            System.err.println(ex.getMessage());
        }
    }

    private static void play(List<String> args) {
        GameConfig config = loadConfig();
        GameSession session = new GameSession(config, startingBalance(args, config));
        OutputRenderer renderer = new OutputRenderer(System.out, config);
        renderer.welcome(session.getBalance());
        renderer.help();
        new ReplLoop(session, renderer, console()).run();
    }

    private static GameConfig loadConfig() {
        try (InputStream json = Main.class.getResourceAsStream(DEFAULT_CONFIG_RESOURCE)) {
            if (json == null) {
                throw new ConfigLoadException(
                        "The game configuration %s is missing from the application.".formatted(DEFAULT_CONFIG_RESOURCE)
                );
            }
            return ConfigLoader.load(json);
        } catch (IOException ex) {
            throw new ConfigLoadException(
                    "The game configuration %s could not be read: %s"
                            .formatted(DEFAULT_CONFIG_RESOURCE, ex.getMessage()), ex
            );
        }
    }

    private static BigDecimal startingBalance(List<String> args, GameConfig config) {
        int flag = args.indexOf(BALANCE_FLAG);
        if (flag < 0) {
            return config.defaultBalance();
        }
        if (flag + 1 == args.size()) {
            throw new IllegalArgumentException(
                    "%s needs an amount after it, for example: %s 500".formatted(BALANCE_FLAG, BALANCE_FLAG)
            );
        }
        String amount = args.get(flag + 1);
        try {
            return new BigDecimal(amount);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(
                    "'%s' is not a balance I can read. Use plain digits, like %s 500".formatted(amount, BALANCE_FLAG)
            );
        }
    }

    private static BufferedReader console() {
        return new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
    }

}
