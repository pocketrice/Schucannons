package io.github.pocketrice.shared;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum Orientation {
    /**
     * Can usually be captured with 'default' branch (not 'case DEFAULT').
     */
    DEFAULT(0),
    TOP_LEFT (1),
    LEFT(2),
    CENTER(3),
    RIGHT(4),
    TOP(5),
    TOP_RIGHT(6),
    BOTTOM_LEFT(7),
    BOTTOM(8),
    BOTTOM_RIGHT(9);

    final int val;
}