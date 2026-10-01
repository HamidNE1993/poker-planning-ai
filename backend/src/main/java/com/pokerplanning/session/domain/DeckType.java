package com.pokerplanning.session.domain;

import java.util.List;

public enum DeckType {
    FIBONACCI(List.of("0", "1", "2", "3", "5", "8", "13", "21", "34", "55", "89", "?", "☕")),
    MODIFIED_FIBONACCI(List.of("0", "0.5", "1", "2", "3", "5", "8", "13", "20", "40", "100", "?", "☕")),
    T_SHIRT(List.of("XS", "S", "M", "L", "XL", "XXL", "?", "☕")),
    POWERS_OF_TWO(List.of("0", "1", "2", "4", "8", "16", "32", "64", "?", "☕"));

    private final List<String> values;

    DeckType(List<String> values) {
        this.values = values;
    }

    public List<String> getValues() {
        return values;
    }
}
