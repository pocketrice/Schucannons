package io.github.pocketrice.client.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
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
import lombok.Getter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Batchable implements Comparable<Batchable> {
    Object batchObj;
    Set<LinkInterlerper> interlerps;
    Color color;
    @Getter
    int x, y, zIndex;
    float rot, scl, opacity;

    public Batchable(Object obj) {
        this(obj, 0);
    }

    public Batchable(Object obj, int z) {
        if (!isBatchable(obj)) {
            System.err.println("Type " + obj.getClass().getSimpleName() + " unable to be batchable!");
        } else {
            batchObj = (obj instanceof Batchable ba) ? ba.batchObj : obj;
            interlerps = new HashSet<>();
            loadPresets();
        }

        zIndex = z;
    }

    public void draw(SpriteBatch batch) throws BatchableException, IllegalStateException {
        if (!batch.isDrawing()) {
            throw new IllegalStateException("Batch was not started before drawing!");
        }

       if (batchObj instanceof Sprite spr) {
           //spr.draw(batch);
           Texture sprTexture = spr.getTexture();
           batch.draw(
                   sprTexture,
                   spr.getX(),
                   spr.getY(),
                   sprTexture.getWidth(),
                   sprTexture.getHeight(),
                   sprTexture.getWidth(),
                   sprTexture.getHeight(),
                   spr.getScaleX(),
                   spr.getScaleY(),
                   spr.getRotation(),
                   spr.getRegionX(),
                   spr.getRegionY(),
                   spr.getRegionWidth(),
                   spr.getRegionHeight(),
                   false,
                   false);
       }
       else if (batchObj instanceof Table table) {
           table.draw(batch, 1f);
       }
       else if (batchObj instanceof Label label) {
           label.draw(batch, 1f);
       }
       else if (batchObj instanceof Drawable drawable) {
           drawable.draw(batch, x, y, 9999f, 9999f); // stupid code pls fix now
       }
       else if (batchObj instanceof ChainInterlerper chint) {
           chint.draw(batch);
       }
       else {
           throw new BatchableException("Invalid batchable operation!!");
       }
    }

    public Batchable color(Color col) throws BatchableException {
        color = col;

        if (batchObj instanceof Sprite spr) {
            spr.setColor(col);
        }
        else if (batchObj instanceof TextButton tb) {
            tb.getStyle().fontColor = col;
        }
        else if (batchObj instanceof Table table) {
            table.setColor(col);
        }
        else if (batchObj instanceof Label label) {
            label.getStyle().fontColor = col;
        }
        else if (batchObj instanceof BatchGroup bg) {
            bg.forEach(b -> {
                try {
                    b.color(col);
                } catch (BatchableException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        else {
            throw new BatchableException("Invalid batchable operation!!");
        }

        return this;
    }

    public Batchable opacity(float o) throws BatchableException { // Cannot do a simple setColor(r, g, b, a * o) b/c of 0 shenanigans
        opacity = o;
        Color newColor = new Color(color.r, color.g, color.b, color.a * opacity);

        if (batchObj instanceof Sprite spr) {
            spr.setColor(newColor);
        }
        else if (batchObj instanceof TextButton tb) {
            tb.getStyle().fontColor = newColor;
        }
        else if (batchObj instanceof Table table) {
            table.setColor(newColor);
        }
        else if (batchObj instanceof Label label) {
            label.getStyle().fontColor = newColor;
        }
        else if (batchObj instanceof BatchGroup bg) {
            bg.forEach(b -> {
                try {
                    b.opacity(o);
                } catch (BatchableException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        else {
            throw new BatchableException("Invalid batchable operation!!");
        }

        return this;
    }

    public Batchable fontSize(int fs) throws BatchableException {
        if (batchObj instanceof TextButton tb) {
            TextButtonStyle tbs = tb.getStyle();
            tbs.font = Fontbook.quickFont(tbs.font.toString(), fs);
        }
        else if (batchObj instanceof Label label) {
            LabelStyle ls = label.getStyle();
            ls.font = Fontbook.quickFont(ls.font.toString(), fs);
        }
        else if (batchObj instanceof Table table) {
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
        else if (batchObj instanceof BatchGroup bg) {
            bg.forEach(b -> {
                try {
                    b.fontSize(fs);
                } catch (BatchableException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        else {
            throw new BatchableException("Invalid batchable operation!!");
        }

        return this;
    }

    public Batchable pos(int x, int y) throws BatchableException {
        if (batchObj instanceof Sprite spr) {
            spr.setPosition(x,y);
        }
        else if (batchObj instanceof Table table) {
            table.setPosition(x,y);
        }
        else if (batchObj instanceof Label label) {
            label.setPosition(x,y);
        }
        else if (batchObj instanceof Drawable) {
            this.x = x;
            this.y = y;
        }
        else if (batchObj instanceof BatchGroup bg) {
            bg.forEach(b -> {
                try {
                    b.pos(x,y);
                } catch (BatchableException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        else {
            throw new BatchableException("Invalid batchable operation!!");
        }

        return this;
    }

    public Batchable rot(float r) throws BatchableException {
        if (batchObj instanceof Sprite spr) {
            spr.setRotation(r);
        }
        else if (batchObj instanceof Table table) {
            table.setRotation(r);
        }
        else if (batchObj instanceof Label label) {
            label.setRotation(r);
        }
        else if (batchObj instanceof BatchGroup bg) {
            bg.forEach(b -> {
                try {
                    b.rot(r);
                } catch (BatchableException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        else {
            throw new BatchableException("Invalid batchable operation!");
        }

        return this;
    }

    public Batchable scl(float s) throws BatchableException {
        if (batchObj instanceof Sprite spr) {
            spr.setScale(s);
        }
        else if (batchObj instanceof Table table) {
            table.setScale(s);
        }
        else if (batchObj instanceof Label label) {
            label.setScale(s);
        }
        else if (batchObj instanceof BatchGroup bg) {
            bg.forEach(b -> {
                try {
                    b.scl(s);
                } catch (BatchableException e) {
                    throw new RuntimeException(e);
                }
            });
        } else {
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
//            case FONT_SIZE -> { // font_size
//                if (batchObj instanceof Label) {
//                    lil = LinkInterlerper.generateFontTransition((Label) batchObj, (int) v1, (int) v2, easing, ss);
//                }
//                else if (batchObj instanceof TextButton) {
//                    lil = LinkInterlerper.generateFontTransition((TextButton) batchObj, (int) v1, (int) v2, easing, ss);
//                }
//                else throw new BatchableException("Invalid batchable operation!!");
//            }
        }

        interlerps.add(lil);
        return this;
    }

    public void loadPresets() {
        if (batchObj instanceof Sprite spr) {
            color = spr.getColor();
            x = (int) spr.getX();
            y = (int) spr.getY();
            rot = spr.getRotation();
            scl = spr.getScaleX();
        }
        else if (batchObj instanceof TextButton tb) {
            tb.setTransform(true);
            Color tbsCol = tb.getStyle().fontColor;
            color = (tbsCol != null) ? tbsCol : new Color(Color.WHITE);
            x = (int) tb.getX();
            y = (int) tb.getY();
            rot = tb.getRotation();
            scl = tb.getScaleX();

            if (batchObj instanceof SchuButton schub) {
                interlerps.addAll(schub.interlerps);
            }
        }
        else if (batchObj instanceof Table table) {
            table.setTransform(true);
            color = table.getColor();
            x = (int) table.getX();
            y = (int) table.getY();
            rot = table.getRotation();
            scl = table.getScaleX();
        }
        else if (batchObj instanceof Label label) {
            color = label.getStyle().fontColor;
            Color lsCol = label.getStyle().fontColor;
            color = (lsCol != null) ? lsCol : new Color(Color.WHITE);
            x = (int) label.getX();
            y = (int) label.getY();
            rot = label.getRotation();
            scl = label.getScaleX();
        }
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof Batchable other) ? this.batchObj.equals(other.batchObj) : this.batchObj.equals(obj);
    }

    @Override
    public String toString() {
        return "Batchable@" + Integer.toHexString(hashCode()) + "-" + batchObj.toString();
    }

    @Override
    public int compareTo(Batchable other) {
        return this.zIndex - other.zIndex;
    }

    public static boolean isBatchable(Object obj) {
        return (obj instanceof Sprite || obj instanceof Table || obj instanceof Label || obj instanceof Drawable || obj instanceof ChainInterlerper || obj instanceof Batchable || obj instanceof BatchGroup);
    }

    public enum InterlerpPreset {
        COLOR,
        OPACITY,
        POSITION,
        ROTATION,
        SCALE
       // FONT_SIZE
    }
}
