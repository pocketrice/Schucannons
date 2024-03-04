package io.github.pocketrice.client.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import io.github.pocketrice.client.SchuAssetManager;
import io.github.pocketrice.shared.EasingFunction;
import io.github.pocketrice.shared.LinkInterlerper;

import java.util.List;

public class NumberButton extends SchuButton {
    Label label;

    boolean isIncrement, isWrapping;
    float upperBound, lowerBound, value, stepSize;
    String suffix;

    public NumberButton(String s, TextButtonStyle tbs, Label l, SchuAssetManager am) {
        this(true, true, s, tbs, 9, 0, 1, l, am);
    }
    public NumberButton(boolean isIncr, boolean isWrap, String s, TextButtonStyle tbs, float upper, float lower, float ss, Label l, SchuAssetManager am) {
        super((isIncr) ? "+" : "-", tbs, (isIncr) ? "slide_up" : "slide_down", "", "", "", 0, 0, am);
        amgr = am;

        value = 0f;
        isIncrement = isIncr;
        isWrapping = isWrap;
        suffix = s;
        upperBound = upper;
        lowerBound = lower;
        stepSize = ss;
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
        activeFunc = (objs) -> {
            Label label = (Label) objs.get(0);
            value = Float.parseFloat(revertSuffix(String.valueOf(label.getText()), suffix));

            float newVal;
            if (isIncrement) {
                newVal = (value == upperBound) ? lowerBound : value + stepSize;  // "extra" wrap since clampWrap() can't account for relative
            } else {
                newVal = (value == lowerBound) ? upperBound : value - stepSize; // same bestie
            }

            value = (isWrapping) ? clampWrap(lowerBound, upperBound, newVal) : clamp(lowerBound, upperBound, newVal);

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

    public static float clamp(float lower, float upper, float n) {
        return Math.max(lower, Math.min(upper, n));
    }

    public static float clampWrap(float lower, float upper, float n) {
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
}
