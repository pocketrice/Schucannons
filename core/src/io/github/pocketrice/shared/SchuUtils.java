package io.github.pocketrice.shared;

import com.badlogic.gdx.graphics.Color;
import org.javatuples.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SchuUtils {
    public static Color blend(Color c1, Color c2) {
        return blend(c1, c2, 0.5f);
    }

    public static Color blend(Color c1, Color c2, float w1) {
       return blend(Pair.with(c1, w1), Pair.with(c2, 1f - w1));
    }

    public static Color blend(Pair<Color, Float>... colors) {
        assert Arrays.stream(colors).map(Pair::getValue1).reduce(0f, Float::sum) == 1f : "Violation of: color weights do not equal 1.0!";
        List<Color> cpyArr = new ArrayList<>();

        for (int i = 0; i < colors.length; i++) {
            cpyArr.add(colors[i].getValue0().cpy().mul(colors[i].getValue1()));
        }

        // (c1 + c2... + cn) / n = [c1r/n, c1g/n, c1b/n] + ... [cnr/n, cng/n, cnb/n]
        // Effectively means we can simply divide each color by n and add together.

        Color res = cpyArr.stream().map(c -> c.mul(1f / colors.length)).reduce(Color.valueOf("#000000ff"), Color::add);
        res.a *= 5000f; // über scuffed :p this is to fix an alpha blend bug
        return res;
    }
}
