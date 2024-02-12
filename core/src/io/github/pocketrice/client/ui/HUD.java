package io.github.pocketrice.client.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import io.github.pocketrice.client.Fontbook;
import io.github.pocketrice.client.GameManager;
import io.github.pocketrice.client.GameRenderer;
import io.github.pocketrice.client.Match;

import static io.github.pocketrice.client.Match.truncate;
import static io.github.pocketrice.client.SchuGame.IS_DEBUG;


public class HUD {
    private final Fontbook fontbook;
    private GameManager gmgr;
    private GameRenderer grdr;
    private SpriteBatch batch;




    public HUD(GameManager gm, GameRenderer gr) {
        batch = new SpriteBatch();
        fontbook = Fontbook.of("tinyislanders.ttf", "koholint.ttf", "dina.ttc", "tf2build.ttf", "tf2segundo.ttf", "delfino.ttf", "kyomadoka.ttf");
        fontbook.setBatch(batch);

        gmgr = gm;
        grdr = gr;
    }

    public void render() {
        Match matchState = gmgr.getMatchState();
        Vector3 projVec = (matchState.getCurrentPlayer() == null) ? Vector3.Zero : matchState.getCurrentPlayer().getProjVector();
        batch.begin();
        fontbook.formatDraw("delfino", 30, Color.valueOf("#b8b1f22F"), matchState.getIdentifier(), new Vector2(730, 800));

        fontbook.font("tinyislanders").fontSize(30).fontColor(Color.valueOf("#d0cee08F"));
        fontbook.formatDraw("X: " + projVec.x, new Vector2(30, 110));
        fontbook.formatDraw("Y: " + projVec.y, new Vector2(30, 85));
        fontbook.formatDraw("Z: " + projVec.z, new Vector2(30, 60));

        if (IS_DEBUG) {
            fontbook.font("koholint").fontSize(20).fontColor(Color.valueOf("#DFE6D17F"));
            Vector3 camPos = grdr.getGameCam().position;
            fontbook.formatDraw("loc: (" + truncate(camPos.x,3) + ", " + truncate(camPos.y,3) + ", " + truncate(camPos.z,3) + ")", new Vector2(30, 800));
            fontbook.formatDraw("fps: " + Gdx.graphics.getFramesPerSecond(), new Vector2(30, 780));
            fontbook.formatDraw("tps: " + gmgr.getClient().getServerTps(), new Vector2(30, 760));
            fontbook.formatDraw("server: " + gmgr.getClient().getServerName(), new Vector2(30, 740));
            fontbook.formatDraw("client: " + gmgr.getClient().getClientName(), new Vector2(30, 720));
            fontbook.formatDraw("ping: " + gmgr.getClient().getPing(), new Vector2(30, 700));
            fontbook.formatDraw("mem: " + truncate((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1E6f, 2) + "mb / " + truncate(Runtime.getRuntime().totalMemory() / 1E6f, 2) + "mb", new Vector2(30, 680));
        }
        batch.end();
    }

    public void dispose() {
        batch.dispose();
    }
}
