package io.github.pocketrice.client;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreetypeFontLoader.FreeTypeFontLoaderParameter;
import com.badlogic.gdx.math.Vector2;
import lombok.Getter;
import org.javatuples.Triplet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static io.github.pocketrice.client.SchuGame.getGlobalAmgr;

// Manages fonts!

// TODO implement Textural's Font (SDMF)
public class Fontbook {
    List<String> ftfCache;
    Map<Integer, List<String>> bmfCache;
    @Getter
    Triplet<String, Integer, Color> presetSettings;
    SchuAssetManager amgr;


    public Fontbook() {
        ftfCache = new ArrayList<>();
        bmfCache = new TreeMap<>();
        presetSettings = new Triplet<>("tinyislanders", 18, Color.WHITE);
    }

    public void setAmgr(SchuAssetManager am) {
        amgr = am;
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

    public void draw(String font, int fontSize, CharSequence text, Vector2 loc, SpriteBatch batch) {
        BitmapFont bmf = getSizedBitmap(font, fontSize);
        Color oldCol = bmf.getColor();
        bmf.setColor(presetSettings.getValue2());
        bmf.draw(batch, text, loc.x, loc.y);
        bmf.setColor(oldCol);
    }

    public void draw(CharSequence text, Vector2 loc, SpriteBatch batch) {
        draw(presetSettings.getValue0(), presetSettings.getValue1(), text, loc, batch);
    }

    public void formatDraw(String font, int fontSize, Color color, CharSequence text, Vector2 loc, SpriteBatch batch) {
        BitmapFont bmf = getSizedBitmap(font, fontSize);

        Color oldCol = bmf.getColor();
        bmf.setColor(color);
        bmf.draw(batch, text, loc.x, loc.y);
        bmf.setColor(oldCol); // Set color back to default
    }

    public void formatDraw(CharSequence text, Vector2 loc, SpriteBatch batch) {
        formatDraw(presetSettings.getValue0(), presetSettings.getValue1(), presetSettings.getValue2(), text, loc, batch);
    }


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

        System.out.println("Loaded all fonts from assets/fonts!");
    }

    public static Fontbook of(String... fontFiles) {
        Fontbook fb = new Fontbook();
        fb.amgr = getGlobalAmgr();
        fb.loadFonts(fontFiles);

        return fb;
    }

    public static BitmapFont quickFont(String font, int fontSize) {
        Fontbook fb = getGlobalAmgr().getFontbook();

        return fb.getSizedBitmap(font, fontSize);
    }
}
