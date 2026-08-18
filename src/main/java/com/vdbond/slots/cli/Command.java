package com.vdbond.slots.cli;

public sealed interface Command
        permits Spin, Deposit, ShowPaytable, ShowRules, ShowReport, ShowHelp, Exit {
}
