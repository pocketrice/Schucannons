package io.github.pocketrice.shared;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import io.github.pocketrice.client.Fontbook;
import io.github.pocketrice.client.ui.Batchable;
import io.github.pocketrice.client.ui.BatchableException;
import lombok.Getter;
import lombok.Setter;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Getter
public class LinkInterlerper<T,U> extends Interlerper<T> {
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
    public static LinkInterlerper<Integer, ? super Label> generateFontTransition(Label l, int v1, int v2, EasingFunction easing, double ss) {
        LinkInterlerper<Integer, ? super Label> lil = new LinkInterlerper<>(v1, v2, easing, 0.04)
                .linkObj(l);

        lil.setInterlerp(false);

        lil.linkFunc((t, obj) -> {
            LabelStyle style = ((Label) obj).getStyle();  // vv The easing is ALWAYS linear here, because step() already applies an easing.
            int fontSize = lil.interlerp(t, EasingFunction.LINEAR); // Rmeember that interlerp returns double b/c covers most numbertypes.
            style.font = Fontbook.quickFont(style.font.toString(), fontSize);
        });

        return lil;
    }

    // Deprecated; use scl instead.

//    public static LinkInterlerper<Float, ? super TextButton> generateFontTransition(TextButton tb, float v1, float v2, EasingFunction easing, double ss) {
//        LinkInterlerper<Float, ? super TextButton> lil = new LinkInterlerper<>(v1, v2, easing, ss)
//                .linkObj(tb);
//
//        lil.setInterlerp(false);
//
//        lil.linkFunc((t, obj) -> {
////            TextButtonStyle style = ((TextButton) obj).getStyle();
////            int fontSize = lil.interlerp(t, EasingFunction.LINEAR);
////            style.font = Fontbook.quickFont(style.font.toString(), fontSize);
//            ((TextButton) obj).setScale(lil.interlerp(t, EasingFunction.LINEAR));
//        });
//
//        return lil;
//    }

    public static LinkInterlerper<Color, ? super Batchable> generateColorTransition(Batchable ba, Color v1, Color v2, EasingFunction easing, double ss) {
        LinkInterlerper<Color, ? super Batchable> lil = new LinkInterlerper<>(v1, v2, easing, ss)
                .linkObj(ba);

        lil.setInterlerp(false);

        lil.linkFunc((t, obj) -> {
            try {
                ((Batchable) obj).color(lil.interlerp(t, EasingFunction.LINEAR));
            } catch (BatchableException e) {
                throw new RuntimeException(e);
            }
        });

        return lil;
    }

    public static LinkInterlerper<Float, ? super Batchable> generateOpacityTransition(Batchable ba, float v1, float v2, EasingFunction easing, double ss) {
        LinkInterlerper<Float, ? super Batchable> lil = new LinkInterlerper<>(v1, v2, easing, ss)
                .linkObj(ba);

        lil.setInterlerp(false);

        lil.linkFunc((t, obj) -> {
            try {
                ((Batchable) obj).opacity(lil.interlerp(t, EasingFunction.LINEAR));
            } catch (BatchableException e) {
                throw new RuntimeException(e);
            }
        });

        return lil;
    }

    public static LinkInterlerper<Vector2, ? super Batchable> generatePosTransition(Batchable ba, Vector2 v1, Vector2 v2, EasingFunction easing, double ss) {
        LinkInterlerper<Vector2, ? super Batchable> lil = new LinkInterlerper<>(v1, v2, easing, ss)
                .linkObj(ba);

        lil.setInterlerp(false);

        lil.linkFunc((t, obj) -> {
            //System.out.println(((Batchable) obj).getX() + ((Batchable) obj).getY());
            try {
                Vector2 lerpVal = lil.interlerp(t, EasingFunction.LINEAR);
                ((Batchable) obj).pos((int) lerpVal.x, (int) lerpVal.y);
            } catch (BatchableException e) {
                throw new RuntimeException(e);
            }
        });

        return lil;
    }

    public static LinkInterlerper<Float, ? super Batchable> generateRotTransition(Batchable ba, Float v1, Float v2, EasingFunction easing, double ss) {
        LinkInterlerper<Float, ? super Batchable> lil = new LinkInterlerper<>(v1, v2, easing, ss)
                .linkObj(ba);

        lil.setInterlerp(false);

        lil.linkFunc((t, obj) -> {
            try {
                ((Batchable) obj).rot(lil.interlerp(t, EasingFunction.LINEAR));
            } catch (BatchableException e) {
                throw new RuntimeException(e);
            }
        });

        return lil;
    }

    public static LinkInterlerper<Float, ? super Batchable> generateSclTransition(Batchable ba, Float v1, Float v2, EasingFunction easing, double ss) {
        LinkInterlerper<Float, ? super Batchable> lil = new LinkInterlerper<>(v1, v2, easing, ss)
                .linkObj(ba);

        lil.setInterlerp(false);

        lil.linkFunc((t, obj) -> {
            try {
                ((Batchable) obj).scl(lil.interlerp(t, EasingFunction.LINEAR));
            } catch (BatchableException e) {
                throw new RuntimeException(e);
            }
        });

        return lil;
    }
}
