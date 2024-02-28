package io.github.pocketrice.client.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import io.github.pocketrice.client.ChainInterlerper;
import io.github.pocketrice.client.Fontbook;
import io.github.pocketrice.shared.EasingFunction;
import io.github.pocketrice.shared.LinkInterlerper;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Batchable {
    Object batchObj;
    Set<LinkInterlerper> interlerps;
    Color color;
    int x, y;
    float rot, scl;

    public Batchable(Object obj) {
        this(obj, 0, 0);
    }

    public Batchable(Object obj, int x, int y) {
        if (!isBatchable(obj)) {
            System.err.println("Type unable to be batchable! (" + obj.getClass().getSimpleName() + ")");
        } else {
            batchObj = obj;
            interlerps = new HashSet<>();
            loadPresets();

            try {
                this.pos(x, y);
            } catch (BatchableException e) {
                System.err.println("Unable to bind object to batchable!");
            }
        }
    }

    public void draw(SpriteBatch batch) throws BatchableException, IllegalStateException {
        if (!batch.isDrawing()) {
            throw new IllegalStateException("Batch was not started before drawing!");
        }

       if (batchObj instanceof Sprite) {
           ((Sprite) batchObj).draw(batch);
       }
       else if (batchObj instanceof Table) {
           ((Table) batchObj).draw(batch, 1f);
       }
       else if (batchObj instanceof Label) {
           ((Label) batchObj).draw(batch, 1f);
       }
       else if (batchObj instanceof Drawable) {
           ((Drawable) batchObj).draw(batch, x, y, 9999f, 9999f); // stupid code pls fix now
       }
       else if (batchObj instanceof ChainInterlerper) {
           ((ChainInterlerper) batchObj).draw(batch);
       }
       else {
           throw new BatchableException("Invalid batchable operation!!");
       }
    }

    public Batchable color(Color col) throws BatchableException {
        color = col;

        if (batchObj instanceof Sprite) {
            ((Sprite) batchObj).setColor(col);
        }
        else if (batchObj instanceof TextButton) {
            ((TextButton) batchObj).getStyle().fontColor = col;
        }
        else if (batchObj instanceof Table) {
            ((Table) batchObj).setColor(col);
        }
        else if (batchObj instanceof Label) {
            ((Label) batchObj).getStyle().fontColor = col;
        }
        else {
            throw new BatchableException("Invalid batchable operation!!");
        }

        return this;
    }

    public Batchable opacity(float opacity) throws BatchableException {
        color(color.set(color.r, color.g, color.b, opacity));
        return this;
    }

    public Batchable fontSize(int fs) throws BatchableException {
        if (batchObj instanceof TextButton) {
            TextButtonStyle tbs = ((TextButton) batchObj).getStyle();
            tbs.font = Fontbook.quickFont(tbs.font.toString(), fs);
        }
        else if (batchObj instanceof Label) {
            LabelStyle ls = ((Label) batchObj).getStyle();
            ls.font = Fontbook.quickFont(ls.font.toString(), fs);
        }
        else if (batchObj instanceof Table) {
            Table table = (Table) batchObj;
            Arrays.stream(table.getChildren().toArray()).filter(c -> c instanceof Label || c instanceof TextButton).map(Batchable::new).forEach(tb -> {
                if (tb.batchObj instanceof Label) {
                    LabelStyle style = ((Label) tb.batchObj).getStyle();
                    style.font = Fontbook.quickFont(style.font.toString(), fs);
                } else {
                    TextButtonStyle style = ((TextButton) tb.batchObj).getStyle();
                    style.font = Fontbook.quickFont(style.font.toString(), fs);
                }
            });
        }
        else {
            throw new BatchableException("Invalid batchable operation!!");
        }

        return this;
    }

    public Batchable pos(int x, int y) throws BatchableException {
        if (batchObj instanceof Sprite) {
            ((Sprite) batchObj).setPosition(x,y);
        }
        else if (batchObj instanceof Table) {
            ((Table) batchObj).setPosition(x,y);
        }
        else if (batchObj instanceof Label) {
            ((Label) batchObj).setPosition(x,y);
        }
        else if (batchObj instanceof Drawable) {
            this.x = x;
            this.y = y;
        }
        else {
            throw new BatchableException("Invalid batchable operation!!");
        }

        return this;
    }

    public Batchable rot(float r) throws BatchableException {
        if (batchObj instanceof Sprite) {
            ((Sprite) batchObj).setRotation(r);
        }
        else if (batchObj instanceof Table) {
            ((Table) batchObj).setRotation(r);
        }
        else if (batchObj instanceof Label) {
            ((Label) batchObj).setRotation(r);
        }
        else {
            throw new BatchableException("Invalid batchable operation!");
        }

        return this;
    }

    public Batchable scl(float s) throws BatchableException {
        if (batchObj instanceof Sprite) {
            ((Sprite) batchObj).setScale(s);
        }
        else if (batchObj instanceof Table) {
            ((Table) batchObj).setScale(s);
        }
        else if (batchObj instanceof Label) {
            ((Label) batchObj).setScale(s);
        }
        else {
            throw new BatchableException("Invalid batchable operation!!");
        }

        return this;
    }

    public <U> Batchable bindInterlerp(U v1, U v2, InterlerpPreset preset, EasingFunction easing, double ss) throws BatchableException {
        if (batchObj instanceof ChainInterlerper) throw new BatchableException("Cannot bind interlerp to another interlerp!");
        LinkInterlerper lil = null;

        switch (preset) {
            case COLOR -> lil = LinkInterlerper.generateColorTransition(this, (Color) v1, (Color) v2, easing, ss);
            case OPACITY -> lil = LinkInterlerper.generateOpacityTransition(this, (float) v1, (float) v2, easing, ss);
            case POSITION -> lil = LinkInterlerper.generatePosTransition(this, (Vector2) v1, (Vector2) v2, easing, ss);
            case ROTATION -> lil = LinkInterlerper.generateRotTransition(this, (float) v1, (float) v2, easing, ss);
            case SCALE -> lil = LinkInterlerper.generateSclTransition(this, (float) v1, (float) v2, easing, ss);
            case FONT_SIZE -> { // font_size
                if (batchObj instanceof Label) {
                    lil = LinkInterlerper.generateFontTransition((Label) batchObj, (int) v1, (int) v2, easing, ss);
                }
                else if (batchObj instanceof TextButton) {
                    lil = LinkInterlerper.generateFontTransition((TextButton) batchObj, (int) v1, (int) v2, easing, ss);
                }
                else throw new BatchableException("Invalid batchable operation!!");
            }
        }

        interlerps.add(lil);
        return this;
    }

    public void loadPresets() {
        if (batchObj instanceof Sprite) {
            Sprite b = (Sprite) batchObj;
            color = b.getColor();
            x = (int) b.getX();
            y = (int) b.getY();
            rot = b.getRotation();
            scl = b.getScaleX();
        }
        else if (batchObj instanceof TextButton) {
            TextButton b = (TextButton) batchObj;
            color = b.getStyle().fontColor;
            x = (int) b.getX();
            y = (int) b.getY();
            rot = b.getRotation();
            scl = b.getScaleX();
        }
        else if (batchObj instanceof Table) {
            Table b = (Table) batchObj;
            color = b.getColor();
            x = (int) b.getX();
            y = (int) b.getY();
            rot = b.getRotation();
            scl = b.getScaleX();
        }
        else if (batchObj instanceof Label) {
            Label b = (Label) batchObj;
            color = b.getStyle().fontColor;
            x = (int) b.getX();
            y = (int) b.getY();
            rot = b.getRotation();
            scl = b.getScaleX();
        }
    }

    @Override
    public boolean equals(Object other) {
        return (other instanceof Batchable) ? this.batchObj.equals(((Batchable) other).batchObj) : this.batchObj.equals(other);
    }

    public static boolean isBatchable(Object obj) {
        return (obj instanceof Sprite) || (obj instanceof Table) || (obj instanceof Label) || (obj instanceof Drawable) || (obj instanceof ChainInterlerper);
    }

    public enum InterlerpPreset {
        COLOR,
        OPACITY,
        POSITION,
        ROTATION,
        SCALE,
        FONT_SIZE
    }
}
