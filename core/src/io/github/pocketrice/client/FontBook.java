package io.github.pocketrice.client;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreetypeFontLoader.FreeTypeFontLoaderParameter;
import com.badlogic.gdx.math.Vector2;
import io.github.pocketrice.shared.Orientation;
import lombok.Getter;
import lombok.Setter;
import org.javatuples.Quintet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static io.github.pocketrice.client.SchuGame.*;
import static io.github.pocketrice.shared.AnsiCode.ANSI_BLUE;
import static io.github.pocketrice.shared.AnsiCode.ANSI_RESET;

// Manages fonts!

// TODO implement Textural's Font (SDMF)
public class Fontbook {
    List<String> ftfCache;
    Map<Integer, List<String>> bmfCache;
    @Getter
    Quintet<String, Integer, Color, Vector2, Float> presetSettings;
    @Setter
    SchuAssetManager amgr;
    boolean isCoverAware;
    float textCover;

    public Fontbook() {
        ftfCache = new ArrayList<>();
        bmfCache = new TreeMap<>();
        presetSettings = new Quintet<>("tinyislanders", 18, Color.WHITE, new Vector2(20f, 60f), 20f);
        isCoverAware = false;
        textCover = 0;
    }

    public BitmapFont getSizedBitmap(String name, int fontSize) {
        return getSizedBitmap(name, fontSize, null);
    }

    public BitmapFont getSizedBitmap(String name, int fontSize, Color color) {
        String assetName = name + fontSize + ".ttf";

        List<String> bmfs = bmfCache.getOrDefault(fontSize, List.of());
        String bmf = bmfs.stream().filter(f -> f.contains(name)).findFirst().orElse(null);
        if (bmf == null) {
            FreeTypeFontLoaderParameter param = new FreeTypeFontLoaderParameter();
            param.fontFileName = amgr.unalias(amgr.fzf(name)[0]);
            param.fontParameters.size = fontSize;
            if (color != null) param.fontParameters.color = color;
            amgr.load(assetName, BitmapFont.class, param);
            amgr.finishLoadingAsset(assetName);

            bmfCache.putIfAbsent(fontSize, new ArrayList<>());
            bmfCache.get(fontSize).add(name);
        }

        return amgr.get(assetName, BitmapFont.class);
    }

    // .bind is deprecated

    public void draw(String font, int fontSize, Object text, Vector2 loc, SpriteBatch batch) {
        BitmapFont bmf = getSizedBitmap(font, fontSize);
        Color oldCol = bmf.getColor();
        bmf.setColor(presetSettings.getValue2());
        bmf.draw(batch, text.toString(), loc.x, loc.y);
        bmf.setColor(oldCol);
    }

    public void draw(Object text, Vector2 loc, SpriteBatch batch) {
        draw(presetSettings.getValue0(), presetSettings.getValue1(), text.toString(), loc, batch);
    }

    public void formatDraw(String font, int fontSize, Color color, Object text, Orientation or, float padX, float padY, SpriteBatch batch) {
        BitmapFont bmf = getSizedBitmap(font, fontSize);
        GlyphLayout gl = new GlyphLayout();
        gl.setText(bmf, text.toString());
        Vector2 loc;

        float adjPadY = padY + ((isCoverAware) ? textCover : 0);

        switch (or) {
            case TOP_LEFT -> loc = new Vector2(padX, VIEWPORT_HEIGHT - adjPadY);

            case TOP -> loc = new Vector2(VIEWPORT_WIDTH / 2f, VIEWPORT_HEIGHT - adjPadY);

            case TOP_RIGHT -> loc = new Vector2(VIEWPORT_WIDTH - gl.width - padX, VIEWPORT_HEIGHT - adjPadY);

            case LEFT -> loc = new Vector2(padX, VIEWPORT_HEIGHT / 2f);

            case CENTER -> loc = new Vector2(VIEWPORT_WIDTH / 2f, VIEWPORT_HEIGHT / 2f);

            case RIGHT -> loc = new Vector2(VIEWPORT_WIDTH - gl.width - padX, VIEWPORT_HEIGHT / 2f);

            case BOTTOM_LEFT -> loc = new Vector2(padX, adjPadY);

            case BOTTOM -> loc = new Vector2(VIEWPORT_WIDTH / 2f, adjPadY);

            case BOTTOM_RIGHT -> loc = new Vector2(VIEWPORT_WIDTH - gl.width - padX, adjPadY);

            default -> loc = Vector2.Zero;
        }

        formatDraw(font, fontSize, color, text.toString(), loc, batch);

        if (isCoverAware) {
            textCover += gl.height + presetSettings.getValue4();
        }
    }

    public void formatDraw(String font, int fontSize, Color color, Object text, Vector2 loc, SpriteBatch batch) { // No cover-aware b/c orientation-based format draw relies on this.
        BitmapFont bmf = getSizedBitmap(font, fontSize);
        GlyphLayout gl = new GlyphLayout();
        gl.setText(bmf, text.toString());

        Color oldCol = bmf.getColor();
        bmf.setColor(color);
        bmf.draw(batch, text.toString(), loc.x, loc.y);
        bmf.setColor(oldCol); // Set color back to default
    }

    public void formatDraw(Object text, Vector2 loc, SpriteBatch batch) {
        formatDraw(presetSettings.getValue0(), presetSettings.getValue1(), presetSettings.getValue2(), text, loc, batch);
    }

    public void formatDraw(Object text, Orientation or, SpriteBatch batch) {
        formatDraw(presetSettings.getValue0(), presetSettings.getValue1(), presetSettings.getValue2(), text, or, presetSettings.getValue3().x, presetSettings.getValue3().y, batch);
    }

    public float getCover() { // Does not account for polarity/direction.
        BitmapFont bmf = getSizedBitmap(presetSettings.getValue0(), presetSettings.getValue1());
        GlyphLayout gl = new GlyphLayout();
        return gl.height;
    }

    public void toggleCoverAware(boolean isAware) {
        isCoverAware = isAware;
        textCover = 0; // Reset text cover
    }


    // --------------- BUILDER METHODS -----------------
    public Fontbook reset() {
        presetSettings = new Quintet<>("tinyislanders", 18, Color.WHITE, new Vector2(20f, 60f), 5f);
        return this;
    }

    //
    public Fontbook font(String font) {
        presetSettings = presetSettings.setAt0(font);
        return this;
    }

    public Fontbook fontSize(int fs) {
        presetSettings = presetSettings.setAt1(fs);
        return this;
    }

    public Fontbook fontColor(Color fc) {
        presetSettings = presetSettings.setAt2(fc);
        return this;
    }

    public Fontbook pad(Vector2 padXY) {
        presetSettings = presetSettings.setAt3(padXY);
        return this;
    }

    public Fontbook padX(float x) {
        Vector2 presetPad = presetSettings.getValue3();
        presetPad.x = x;

        presetSettings = presetSettings.setAt3(presetPad);
        return this;
    }

    public Fontbook padY(float y) {
        Vector2 presetPad = presetSettings.getValue3();
        presetPad.y = y;

        presetSettings = presetSettings.setAt3(presetPad);
        return this;
    }

    public Fontbook coverPad(float y) {
        presetSettings = presetSettings.setAt4(y);
        return this;
    }

    // ----------------------------------------------------


//    // Terminating branch of builder — deprecated since one single Fontbook is now shared.
//    public void bind(SpriteBatch batch) {
//        presetBatch = batch;
//    }

    // Validate file and add to validated fonts list. Cannot precache them due to stupid AssetManager shenanigans >:(
    public void loadFont(String alias) {
        String fontFile = (alias.matches(".*\\.[a-z0-9]+") ? alias : amgr.fzf(alias)[0]); // Default to ttf
        if (!Gdx.files.internal("fonts/" + fontFile).exists()) System.err.println("Warning: " + fontFile + " does not exist.");
        if (!fontFile.matches(".*(\\.ttf|\\.ttc|\\.otf)")) System.err.println("Warning: " + fontFile + " is not .ttf/.ttc/.otf format. Likely will not load.");

        if (!amgr.isLoaded("fonts/" + fontFile)) {
            amgr.aliasedLoad("fonts/" + fontFile, alias, FreeTypeFontGenerator.class);
        }

        ftfCache.add(fontFile);
    }

    public void loadFonts(String... fontFiles) {
        for (String ff : fontFiles) {
            loadFont(ff);
        }
    }

    public void dispose() {
        ftfCache.forEach(s -> amgr.unload("/fonts/" + s + ".ttf"));
        for (int i : bmfCache.keySet()) {
            bmfCache.get(i).forEach(b -> amgr.unload("/fonts/" + b + i + ".ttf"));
        }

        ftfCache.clear();
        bmfCache.clear();
    }

    public void importAll() {
        FileHandle fontDir = Gdx.files.internal("assets/fonts/");

        if (fontDir.isDirectory()) {
            for (FileHandle font : fontDir.list()) {
                String filename = font.name();
                if (filename.matches("[^*][^*].*(\\.ttc|\\.ttf|\\.otf)")) { // To disable a file, append ** to beginning.
                    loadFont(filename);
                }
            }
        }

        System.out.println(ANSI_BLUE + "[✧˖°] Loaded all fonts from assets/fonts!" + ANSI_RESET);
    }

    public static Fontbook of(String... fontFiles) {
        Fontbook fb = new Fontbook();
        fb.amgr = globalAmgr();
        fb.loadFonts(fontFiles);

        return fb;
    }

    public static BitmapFont quickFont(String font, int fontSize) {
        Fontbook fb = globalAmgr().getFontbook();

        return fb.getSizedBitmap(font, fontSize);
    }
}
