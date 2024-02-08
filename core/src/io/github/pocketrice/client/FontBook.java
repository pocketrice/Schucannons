package io.github.pocketrice.client;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.Vector2;
import io.github.pocketrice.shared.FuzzySearch;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

// Manages fonts!
public class Fontbook {
    List<FreeTypeFontGenerator> ftfs;
    Map<Integer, List<BitmapFont>> bmfCache;

    public Fontbook(FreeTypeFontGenerator... fonts) {
        ftfs = new ArrayList<>(List.of(fonts));
        bmfCache = new TreeMap<>();
    }

    // fuzzy search bc why not. prolly not
    public FreeTypeFontGenerator getFuzzyFont(String name) {
        FuzzySearch fs = new FuzzySearch(ftfs);
        String res = fs.getFuzzy(name)[0];

        return ftfs.stream().filter(ftf -> ftf.toString().equals(res)).findFirst().get(); // return null if not there
    }

    public FreeTypeFontGenerator getFont(String name) {
        return ftfs.stream().filter(ftf -> ftf.toString().equals(name)).findFirst().get();
    }

    public BitmapFont getSizedBitmap(String name, int fontSize) {
        BitmapFont result;

        List<BitmapFont> bmfs = bmfCache.getOrDefault(fontSize, List.of());
        BitmapFont bmf = bmfs.stream().filter(f -> f.toString().contains(name)).findFirst().orElse(null);
        if (bmf != null) {
            result = bmf;
        } else {
            FreeTypeFontGenerator ftfg = getFuzzyFont(name);
            FreeTypeFontGenerator.FreeTypeFontParameter ftparam = new FreeTypeFontGenerator.FreeTypeFontParameter();
            ftparam.size = fontSize;
            result = ftfg.generateFont(ftparam);

            bmfCache.putIfAbsent(fontSize, new ArrayList<>());
            bmfCache.get(fontSize).add(result);
        }

        // tip: DON'T dispose the ftfg yet. It is still stored in the ftfs list, so it disappears from memory and you get a lovely lil' SEGSIGV. fun :D
        return result;
    }


    public void draw(String font, SpriteBatch batch, CharSequence text, Vector2 loc) {
        draw(font, 12, batch, text, loc, 500f);
    }

    public void draw(String font, int fontSize, SpriteBatch batch, CharSequence text, Vector2 loc, float width) {
        BitmapFont bmf = getSizedBitmap(font, fontSize);
        bmf.draw(batch, text, loc.x, loc.y, width, 0, true);
    }

    public void formatDraw(String font, Color color, SpriteBatch batch, CharSequence text, Vector2 loc) {
        formatDraw(font, 12, color, batch, text, loc, 500f);
    }

    public void formatDraw(String font, int fontSize, Color color, SpriteBatch batch, CharSequence text, Vector2 loc, float width) {
        BitmapFont bmf = getSizedBitmap(font, fontSize);

        Color oldCol = bmf.getColor();
        bmf.setColor(color);
        bmf.draw(batch, text, loc.x, loc.y, width, 0, true);
        bmf.setColor(oldCol); // Set color back to default
    }


    public void loadFont(String fontFile) {
        if (!fontFile.matches(".*(\\.ttf).*")) System.err.println("Warning: " + fontFile + " is not .ttf format. Likely will not load.");
        FreeTypeFontGenerator ftfg = new FreeTypeFontGenerator(Gdx.files.internal("fonts/" + fontFile));
        ftfs.add(ftfg);
    }

    public void loadFonts(String... fontFiles) {
        for (String ff : fontFiles) {
            loadFont(ff);
        }
    }

    public static Fontbook of(String... fontFiles) {
        Fontbook fb = new Fontbook();
        fb.loadFonts(fontFiles);

        return fb;
    }

    public void dispose() {
        ftfs.forEach(FreeTypeFontGenerator::dispose);
        ftfs.clear();
    }
}
