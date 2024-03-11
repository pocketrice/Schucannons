package io.github.pocketrice.shared;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ResponseStatus {
    OK(1),
    FAIL(-1),
    TIMEOUT(0);

    final int code;
}
