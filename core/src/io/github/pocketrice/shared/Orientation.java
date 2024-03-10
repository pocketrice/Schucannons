package io.github.pocketrice.shared;

public enum Orientation {
    /**
     * Can usually be captured with 'default' branch (not 'case DEFAULT').
     */
    DEFAULT(0),
    LEFT(1),
    CENTER(2),
    RIGHT(3),
    TOP_LEFT (4),
    TOP_CENTER(5),
    TOP_RIGHT(6),
    MID_LEFT(7),
    MID_CENTER(8),
    MID_RIGHT(9),
    BOTTOM_LEFT(10),
    BOTTOM_CENTER(11),
    BOTTOM_RIGHT(12);

    Orientation(int i) {
        val = i;
    }

    final int val;
}