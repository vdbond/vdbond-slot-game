package com.vdbond.slots.cli;

import com.vdbond.slots.session.GameSession;
import com.vdbond.slots.session.InsufficientBalanceException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ReplLoop {

    private final GameSession session;

    private final OutputRenderer renderer;

    private final BufferedReader input;

    public void run() {
        boolean playing = true;
        while (playing) {
            renderer.prompt();
            String line = readLine();
            if (line == null) {
                renderer.goodbye(session.report());
                playing = false;
            } else if (!line.isBlank()) {
                playing = execute(line);
            }
        }
    }

    private boolean execute(String line) {
        try {
            return dispatch(CommandParser.parse(line));
        } catch (CommandParseException | InsufficientBalanceException | IllegalArgumentException ex) {
            renderer.error(ex.getMessage());
            return true;
        }
    }

    private boolean dispatch(Command command) {
        switch (command) {
            case Spin spin -> spin(spin);
            case Deposit deposit -> deposit(deposit);
            case ShowPaytable ignored -> renderer.paytable();
            case ShowRules ignored -> renderer.rules();
            case ShowReport ignored -> renderer.report(session.report());
            case ShowHelp ignored -> renderer.help();
            case Exit ignored -> {
                renderer.goodbye(session.report());
                return false;
            }
        }
        return true;
    }

    private void spin(Spin command) {
        BigDecimal bet = command.amount()
                .orElseGet(() -> session.getConfig().defaultBet());
        renderer.spin(bet, session.spin(bet), session.getBalance());
    }

    private void deposit(Deposit command) {
        session.deposit(command.amount());
        renderer.deposit(command.amount(), session.getBalance());
    }

    /**
     * A null line means the player closed the input stream — Ctrl-D, or a script that ran out of commands — which ends
     * the session the same way {@code --exit} does.
     */
    private String readLine() {
        try {
            return input.readLine();
        } catch (IOException ex) {
            throw new UncheckedIOException("The console could not be read from.", ex);
        }
    }

}
