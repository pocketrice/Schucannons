package io.github.pocketrice.shared;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import io.github.pocketrice.client.ui.Batchable;
import lombok.Getter;
import lombok.Setter;

import java.io.InvalidClassException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static io.github.pocketrice.client.SchuGame.fontbook;

@Getter
public class LinkInterlerper<T,U> extends Interlerper<T>{
    private BiConsumer<Double, U> linkFunc;
    private Consumer<U> preFunc, postFunc;
    @Setter
    private U linkObj;


    public LinkInterlerper(T start, T end) {
        this(start, end, EasingFunction.LINEAR, 0.1);
    }

    public LinkInterlerper(T start, T end, EasingFunction e, double ss) {
        super(start, end, e, ss);
    }


    public LinkInterlerper<T,U> linkFunc(BiConsumer<Double, U> func) {
        linkFunc = func;
        return this;
    }

    public LinkInterlerper<T,U> preFunc(Consumer<U> func) {
        preFunc = func;
        return this;
    }

    public LinkInterlerper<T,U> postFunc(Consumer<U> func) {
        postFunc = func;
        return this;
    }

    public LinkInterlerper<T,U> linkObj(U obj) {
        linkObj = obj;
        return this;
    }


    // A bulkier variant of advance() that can run a func on a separate obj (usually parent) for each advance. Useful for frame-based render().
    public void step() {
        if (isInterlerp()) {
            double easedT = getEasing().apply(advanceParam());
            if (getT() == 0 && preFunc != null) preFunc.accept(linkObj);
            if (getT() == 1 && postFunc != null) postFunc.accept(linkObj);

            if (isForward() && getT() >= 1 || getT() <= 0) {
                setInterlerp(false);
            } else {
                linkFunc.accept(easedT, linkObj);
            }
        }
    }

    // Convenience initialisers
    public static LinkInterlerper<Integer, ? super LabelStyle> generateFontLinkLerp(LabelStyle ls, int v1, int v2, EasingFunction easing) {
        LinkInterlerper<Integer, ? super LabelStyle> lil = new LinkInterlerper<>(v1, v2, easing, 0.04)
                .linkObj(ls);

        lil.linkFunc((t, obj) -> {
            LabelStyle style = (LabelStyle) obj;  // vv The easing is ALWAYS linear here, because step() already applies an easing.
            int fontSize = lil.interlerp(t, EasingFunction.LINEAR); // Rmeember that interlerp returns double b/c covers most numbertypes.
            style.font = fontbook.getSizedBitmap(style.font.toString(), fontSize);
        });

        return lil;
    }

    public static LinkInterlerper<Integer, ? super TextButtonStyle> generateFontLinkLerp(TextButtonStyle tbs, int v1, int v2, EasingFunction easing) {
        LinkInterlerper<Integer, ? super TextButtonStyle> lil = new LinkInterlerper<>(v1, v2, easing, 0.04)
                .linkObj(tbs);

        lil.linkFunc((t, obj) -> {
            TextButtonStyle style = (TextButtonStyle) obj;
            int fontSize = lil.interlerp(t, EasingFunction.LINEAR);
            style.font = fontbook.getSizedBitmap(style.font.toString(), fontSize);
        });

        return lil;
    }

    public static LinkInterlerper<Color, ? super Batchable> generateColorLinkLerp(Batchable ba, Color v1, Color v2, EasingFunction easing) {
        LinkInterlerper<Color, ? super Batchable> lil = new LinkInterlerper<>(v1, v2, easing, 0.04)
                .linkObj(ba);

        lil.linkFunc((t, obj) -> {
            try {
                ((Batchable) obj).color(lil.interlerp(t, EasingFunction.LINEAR));
            } catch (InvalidClassException e) {
                throw new RuntimeException(e);
            }
        });

        return lil;
    }

    public static LinkInterlerper<Float, ? super Batchable> generateOpacityLinkLerp(Batchable ba, float v1, float v2, EasingFunction easing) {
        LinkInterlerper<Float, ? super Batchable> lil = new LinkInterlerper<>(v1, v2, easing, 0.04)
                .linkObj(ba);

        lil.linkFunc((t, obj) -> {
            try {
                ((Batchable) obj).opacity(lil.interlerp(t, EasingFunction.LINEAR));
            } catch (InvalidClassException e) {
                throw new RuntimeException(e);
            }
        });

        return lil;
    }

    public static LinkInterlerper<Vector2, ? super Batchable> generatePosLinkLerp(Batchable ba, Vector2 v1, Vector2 v2, EasingFunction easing) {
        LinkInterlerper<Vector2, ? super Batchable> lil = new LinkInterlerper<>(v1, v2, easing, 0.04)
                .linkObj(ba);

        lil.linkFunc((t, obj) -> {
            try {
                Vector2 lerpVal = lil.interlerp(t, EasingFunction.LINEAR);
                ((Batchable) obj).pos((int) lerpVal.x, (int) lerpVal.y);
            } catch (InvalidClassException e) {
                throw new RuntimeException(e);
            }
        });

        return lil;
    }

    public static LinkInterlerper<Float, ? super Batchable> generateRotLinkLerp(Batchable ba, Float v1, Float v2, EasingFunction easing) {
        LinkInterlerper<Float, ? super Batchable> lil = new LinkInterlerper<>(v1, v2, easing, 0.04)
                .linkObj(ba);

        lil.linkFunc((t, obj) -> {
            try {
                ((Batchable) obj).rot(lil.interlerp(t, EasingFunction.LINEAR));
            } catch (InvalidClassException e) {
                throw new RuntimeException(e);
            }
        });

        return lil;
    }

    public static LinkInterlerper<Float, ? super Batchable> generateSclLinkLerp(Batchable ba, Float v1, Float v2, EasingFunction easing) {
        LinkInterlerper<Float, ? super Batchable> lil = new LinkInterlerper<>(v1, v2, easing, 0.04)
                .linkObj(ba);

        lil.linkFunc((t, obj) -> {
            try {
                ((Batchable) obj).scl(lil.interlerp(t, EasingFunction.LINEAR));
            } catch (InvalidClassException e) {
                throw new RuntimeException(e);
            }
        });

        return lil;
    }
}
