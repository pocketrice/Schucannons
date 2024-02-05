package io.github.pocketrice.shared;

public enum ResponseStatus {
    OK(1),
    FAIL(-1),
    TIMEOUT(0);

    ResponseStatus(int c) {
        code = c;
    }

    final int code;
}
