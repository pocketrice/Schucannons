package io.github.pocketrice.client.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import io.github.pocketrice.client.Audiobox;
import io.github.pocketrice.client.Fontbook;
import io.github.pocketrice.shared.EasingFunction;
import io.github.pocketrice.shared.LinkInterlerper;
import lombok.Getter;

public class NumberButton extends TextButton {
    Audiobox audiobox;
    Fontbook fontbook;
    @Getter
    TextButtonStyle tbs;
    Label label;
    boolean isIncrement, isWrapping;
    float upperBound, lowerBound, value, stepSize;
    String suffix;
    @Getter
    LinkInterlerper<Float, ? super NumberButton> interlerpButtonSize; // Java PECS acronym - consumers should ? super T!
    @Getter
    LinkInterlerper<Color, ? super NumberButton> interlerpColor;


    public NumberButton(Audiobox ab, Fontbook fb, String s, Label l, Skin skin) {
        this(ab, fb, true, true, s, 9, 0, 1, l, skin);
    }
    public NumberButton(Audiobox ab, Fontbook fb, boolean isIncr, boolean isWrap, String s, float upper, float lower, float ss, Label l, Skin skin) {
        super((isIncr) ? "+" : "-", skin);
        this.setHeight(100);
        audiobox = ab;
        fontbook = fb;
        value = 0f;
        isIncrement = isIncr;
        isWrapping = isWrap;
        suffix = s;
        upperBound = upper;
        lowerBound = lower;
        stepSize = ss;
        label = l;

        tbs = new TextButtonStyle();
        tbs.font = fontbook.getSizedBitmap("tf2build", 35);
        this.setStyle(tbs);

        interlerpButtonSize = new LinkInterlerper<>(1f, 1.2f, EasingFunction.EASE_IN_OUT_SINE, 0.04)
                .linkObj(this)
                .linkFunc((t, obj) -> {
                    NumberButton tmb = (NumberButton) obj; // vv The easing is ALWAYS linear here, because step() already applies an easing.
                    float lerpScl = interlerpButtonSize.interlerp(t, EasingFunction.LINEAR); // Rmeember that interlerp returns double b/c covers most numbertypes.
                    tmb.tbs.font.getData().setScale(lerpScl);
                    tmb.setScale(lerpScl);
                    tmb.setStyle(tbs);
                });

        interlerpColor = new LinkInterlerper<>(Color.valueOf("#afafdd"), Color.valueOf("#e2e5f3"), EasingFunction.EASE_IN_OUT_SINE, 0.04)
                .linkObj(this)
                .linkFunc((t, obj) -> {
                    NumberButton rb = (NumberButton) obj;
                    rb.tbs.fontColor = interlerpColor.interlerp(t, EasingFunction.LINEAR);
                    rb.setStyle(tbs);
                });


        this.addListener(new ClickListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                value = Float.parseFloat(revertSuffix(String.valueOf(label.getText()), suffix));
                if (button == com.badlogic.gdx.Input.Buttons.LEFT) {
                    audiobox.playSfx((isIncrement) ? "slide_up" : "slide_down", 100f);
                }

                float newVal;
                if (isIncrement) {
                     newVal = (value == upperBound) ? lowerBound : value + stepSize;  // "extra" wrap since clampWrap() can't account for relative
                } else {
                    newVal = (value == lowerBound) ? upperBound : value - stepSize; // same bestie
                }

                value = (isWrapping) ? clampWrap(lowerBound, upperBound, newVal) : clamp(lowerBound, upperBound, newVal);

                label.setText(applySuffix(value, suffix));

                return true;
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                interlerpButtonSize.setInterlerp(true);
                interlerpColor.setInterlerp(true);
                interlerpButtonSize.setForward(false);
                interlerpColor.setForward(false);

                Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
            }

            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
               // audiobox.playSfx("hint", 5f);
                interlerpButtonSize.setInterlerp(true);
                interlerpColor.setInterlerp(true);
                interlerpButtonSize.setForward(true);
                interlerpColor.setForward(true);

                Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Hand);
            }
        });
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
