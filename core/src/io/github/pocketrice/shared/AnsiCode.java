package io.github.pocketrice.shared;

import lombok.AllArgsConstructor;
import lombok.Getter;

// Ansi codes are escape codes that can be appended to strings to apply colors to any console prints
@AllArgsConstructor
public enum AnsiCode {
    ANSI_BLACK(0, "\u001B[30m"),
    ANSI_RED(1, "\u001B[31m"),
    ANSI_GREEN(2, "\u001B[32m"),
    ANSI_YELLOW(3, "\u001B[33m"),
    ANSI_BLUE(4, "\u001B[34m"),
    ANSI_PURPLE(5, "\u001B[35m"),
    ANSI_CYAN(6, "\u001B[36m"),
    ANSI_WHITE(7, "\u001B[37m"),
    ANSI_RESET(8, "\u001B[0m");

    @Getter
    final int value;
    final String code;

    // Allows for using AnsiCodes just as they were used when they were declared as static constants within the Class; allows for compat with previous projects (only need to import AnsiCode statically).
    @Override
    public String toString() {
        return code;
    }
}
