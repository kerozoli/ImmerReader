package com.keroleap.immerreader.SharedData;

public enum TrimMode {
    BOTH,
    LOWER,
    UPPER;

    public static TrimMode fromString(String value) {
        if (value == null) {
            return BOTH;
        }
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return BOTH;
        }
    }
}
