package io.github.pocketrice.client.ui;

public interface Focusable {
    void handleFocus(boolean isFocused);
    void handleSel();
    boolean isStable();
}
