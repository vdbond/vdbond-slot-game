package com.vdbond.slots.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SymbolKind {

    LOW("Low-paying symbol"),
    HIGH("High-paying symbol"),
    WILD("Wild — substitutes for any symbol except the scatter"),
    SCATTER("Scatter — pays wherever it lands, ignoring paylines");

    private final String description;

}
