package io.github.pocketrice.client.ui;

public interface Focusable {
    void handleFocus(boolean isFocused);
    void handleSelDown();
    void handleSelUp();
    boolean isStable();
}
