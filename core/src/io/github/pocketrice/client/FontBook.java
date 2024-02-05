package io.github.pocketrice.client;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import io.github.pocketrice.shared.FuzzySearch;

import java.util.ArrayList;
import java.util.List;

// Manages fonts!
public class FontBook {
    List<BitmapFont> bmfs;


    public FontBook(BitmapFont... fonts) {
        bmfs = new ArrayList<>(List.of(fonts));
    }

    public void addFont(BitmapFont bmf) {
        bmfs.add(bmf);
    }

    // fuzzy search bc why not
    public BitmapFont getFuzzyFont(String name) {
        FuzzySearch fs = new FuzzySearch(bmfs);
        String res = fs.getFuzzy(name)[0];

        return bmfs.stream().filter(bmf -> bmf.toString().equals(res)).findFirst().get(); // return null if not there
    }

    public BitmapFont getFont(String name) {
        return bmfs.stream().filter(bmf -> bmf.toString().equals(name)).findFirst().get();
    }
}
