package io.github.pocketrice.client.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import io.github.pocketrice.shared.EasingFunction;
import io.github.pocketrice.shared.LinkInterlerper;
import lombok.Getter;

import java.util.List;

public class NumberButton extends SchuButton {
    Label label;
    boolean isIncrement, isWrapping;
    float upperBound, lowerBound, step;
    @Getter
    float value;
    String suffix;

    public NumberButton(String str, TextButtonStyle tbs, Label l) {
        this(1, true, str, tbs, 9, 0, l);
    }
    public NumberButton(float s, boolean isWrap, String str, TextButtonStyle tbs, float upper, float lower, Label l) {
        super((s > 0) ? "+" : "-", tbs, (s > 0) ? "slide_up" : "slide_down", "", "", "", 0, 0);

        value = 0f;
        isIncrement = s > 0;
        isWrapping = isWrap;
        suffix = str;
        upperBound = upper;
        lowerBound = lower;
        step = s;
        label = l;

        this.setStyle(tbs);

        LinkInterlerper<Float, ? super NumberButton> interlerpButtonSize = new LinkInterlerper<>(1f, 1.2f, EasingFunction.EASE_IN_OUT_SINE, 0.04)
                .linkObj(this);

        interlerpButtonSize.linkFunc((t, obj) -> {
                    NumberButton tmb = (NumberButton) obj; // vv The easing is ALWAYS linear here, because step() already applies an easing.
                    float lerpScl = interlerpButtonSize.interlerp(t, EasingFunction.LINEAR); // Rmeember that interlerp returns double b/c covers most numbertypes.
                    tmb.getStyle().font.getData().setScale(lerpScl);
                    tmb.setScale(lerpScl);
                    tmb.setStyle(tbs);
                });

        interlerps.add(interlerpButtonSize);
        interlerps.add(LinkInterlerper.generateColorTransition(new Batchable(this), Color.valueOf("#afafdd"), Color.valueOf("#e2e5f3"), EasingFunction.EASE_IN_OUT_SINE, 0.04));
        activeObjs = List.of(label);
        inactiveFunc = (objs) -> {
            Label label = (Label) objs.get(0);
            value = Float.parseFloat(revertSuffix(String.valueOf(label.getText()), suffix));

            float newVal;
            if (isIncrement) {
                newVal = (value == upperBound) ? lowerBound : value + step;  // "extra" wrap since clampWrap() can't account for relative
            } else {
                newVal = (value == lowerBound) ? upperBound : value + step; // same bestie
            }

            value = (isWrapping) ? clampWrap(newVal, lowerBound, upperBound) : clamp(newVal, lowerBound, upperBound);

            label.setText(applySuffix(value, suffix));
        };
    }

    public static String applySuffix(float value, String suffix) {
        StringBuilder result = new StringBuilder("" + value);
        for (char c : suffix.toCharArray()) {
            if (c == '*') // Special character meaning remove one
                result.deleteCharAt(result.length()-1);
            else
                result.append(c);
        }

        return result.toString();
    }

    public static String revertSuffix(String full, String suffix) {
        StringBuilder result = new StringBuilder(full);
        // Note: some lost precision due to not knowing the trimmed amount from *.
        for (int i = full.length()-1; i >= full.length() - suffix.replace("*", "").length(); i--) {
            result.deleteCharAt(i);
        }

        return result.toString();
    }

    public static float clamp(float n, float lower, float upper) {
        return Math.max(lower, Math.min(upper, n));
    }

    public static float clamp(float n, float upper) {
        return clamp(n, 0, upper);
    }

    public static int clamp(int n, int lower, int upper) {
        return (int) clamp((float) n, lower, upper);
    }


    public static float clampWrap(float n, float lower, float upper) {
        float res;

        // Cannot take modulus of 0, so workaround is needed. Essentially, take dist between n and bound, find remainder, and count from bound.
        if (n > upper) {
            res = (upper != 0) ? lower + Math.abs(n - upper) % (upper - lower) : -n % lower;
        } else if (n < lower) {
            res = (lower != 0) ? upper - Math.abs(n - lower) % (upper - lower) : upper - (-n % upper);
        } else {
            res = n;
        }

        return res;
    }

    public static float clampWrap(float n, float upper) {
        return clampWrap(n, 0, upper);
    }

    public static int clampWrap(int n, int lower, int upper) {
        return (int) clampWrap((float) n, lower, upper);
    }
}
