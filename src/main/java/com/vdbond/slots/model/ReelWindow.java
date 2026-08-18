package com.vdbond.slots.model;

public record ReelWindow(Symbol top, Symbol middle, Symbol bottom) {

    public Symbol symbolAt(Row row) {
        return switch (row) {
            case TOP -> top;
            case MIDDLE -> middle;
            case BOTTOM -> bottom;
        };
    }

}
