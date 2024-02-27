package io.github.pocketrice.client.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import io.github.pocketrice.shared.EasingFunction;
import io.github.pocketrice.shared.LinkInterlerper;

import java.io.InvalidClassException;
import java.util.Arrays;
import java.util.Set;

import static io.github.pocketrice.client.SchuGame.fontbook;

public class Batchable {
    Object batchObj;
    Set<LinkInterlerper> interlerps;
    Color color;
    int x, y;

    public Batchable(Object obj) {
        this(obj, 0, 0);
    }

    public Batchable(Object obj, int x, int y){
        batchObj = obj;
        try {
            this.pos(x,y);
        }
        catch (InvalidClassException e) {
            System.err.println("Unable to bind object to batchable!");
        }
    }

    public void draw(SpriteBatch batch) throws InvalidClassException {
       if (batchObj instanceof Sprite) {
           ((Sprite) batchObj).draw(batch);
       }
       else if (batchObj instanceof Button) {
           ((Button) batchObj).draw(batch, 1f);
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
       else {
           throw new InvalidClassException("Invalid batchable operation!!");
       }
    }

    public Batchable color(Color col) throws InvalidClassException {
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
            throw new InvalidClassException("Invalid batchable operation!!");
        }

        return this;
    }

    public Batchable fontSize(int fs) throws InvalidClassException {
        if (batchObj instanceof TextButton) {
            TextButtonStyle tbs = ((TextButton) batchObj).getStyle();
            tbs.font = fontbook.getSizedBitmap(tbs.font.toString(), fs);
        }
        else if (batchObj instanceof Label) {
            LabelStyle ls = ((Label) batchObj).getStyle();
            ls.font = fontbook.getSizedBitmap(ls.font.toString(), fs);
        }
        else if (batchObj instanceof Table) {
            Table table = (Table) batchObj;
            Arrays.stream(table.getChildren().toArray()).filter(c -> c instanceof Label || c instanceof TextButton).map(Batchable::new).forEach(tb -> {
                if (tb.batchObj instanceof Label) {
                    LabelStyle style = ((Label) tb.batchObj).getStyle();
                    style.font = fontbook.getSizedBitmap(style.font.toString(), fs);
                } else {
                    TextButtonStyle style = ((TextButton) tb.batchObj).getStyle();
                    style.font = fontbook.getSizedBitmap(style.font.toString(), fs);
                }
            });
        }
        else {
            throw new InvalidClassException("Invalid batchable operation!!");
        }

        return this;
    }

    public Batchable opacity(float opacity) throws InvalidClassException {
        color(color.set(color.r, color.g, color.b, opacity));
        return this;
    }

    public Batchable pos(int x, int y) throws InvalidClassException {
        if (batchObj instanceof Sprite) {
            ((Sprite) batchObj).setPosition(x,y);
        }
        else if (batchObj instanceof Button) {
            ((Button) batchObj).setPosition(x,y);
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
            throw new InvalidClassException("Invalid batchable operation!!");
        }

        return this;
    }

    public Batchable rot(float r) throws InvalidClassException {
        if (batchObj instanceof Sprite) {
            ((Sprite) batchObj).setRotation(r);
        }
        else if (batchObj instanceof Button) {
            ((Button) batchObj).setRotation(r);
        }
        else if (batchObj instanceof Table) {
            ((Table) batchObj).setRotation(r);
        }
        else if (batchObj instanceof Label) {
            ((Label) batchObj).setRotation(r);
        }
        else {
            throw new InvalidClassException("Invalid batchable operation!");
        }

        return this;
    }

    public Batchable scl(float s) throws InvalidClassException {
        if (batchObj instanceof Sprite) {
            ((Sprite) batchObj).setScale(s);
        }
        else if (batchObj instanceof Button) {
            ((Button) batchObj).setScale(s);
        }
        else if (batchObj instanceof Table) {
            ((Table) batchObj).setScale(s);
        }
        else if (batchObj instanceof Label) {
            ((Label) batchObj).setScale(s);
        }
        else {
            throw new InvalidClassException("Invalid batchable operation!!");
        }

        return this;
    }

    public <U> void bindInterlerp(U v1, U v2, InterlerpPreset preset, EasingFunction easing) throws InvalidClassException {
        LinkInterlerper lil = null;

        switch (preset) {
            case COLOR -> lil = LinkInterlerper.generateColorLinkLerp(this, (Color) v1, (Color) v2, easing);
            case OPACITY -> lil = LinkInterlerper.generateOpacityLinkLerp(this, (float) v1, (float) v2, easing);
            case POSITION -> lil = LinkInterlerper.generatePosLinkLerp(this, (Vector2) v1, (Vector2) v2, easing);
            case ROTATION -> lil = LinkInterlerper.generateRotLinkLerp(this, (float) v1, (float) v2, easing);
            case SCALE -> lil = LinkInterlerper.generateSclLinkLerp(this, (float) v1, (float) v2, easing);
            case FONT_SIZE -> { // font_size
                if (batchObj instanceof Label) {
                    lil = LinkInterlerper.generateFontLinkLerp(((Label) batchObj).getStyle(), (int) v1, (int) v2, easing);
                }
                else if (batchObj instanceof TextButton) {
                    lil = LinkInterlerper.generateFontLinkLerp(((TextButton) batchObj).getStyle(), (int) v1, (int) v2, easing);
                }
                else throw new InvalidClassException("Invalid batchable operation!!");
            }
        }

        interlerps.add(lil);
    }

    enum InterlerpPreset {
        COLOR,
        OPACITY,
        POSITION,
        ROTATION,
        SCALE,
        FONT_SIZE
    }
}
