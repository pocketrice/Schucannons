package io.github.pocketrice.client;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGeneratorLoader;
import com.badlogic.gdx.graphics.g2d.freetype.FreetypeFontLoader;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.viewport.FillViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import io.github.pocketrice.client.screens.MenuScreen;
import io.github.pocketrice.server.DedicatedServer;
import io.github.pocketrice.server.GameServer;
import lombok.Getter;
import lombok.Setter;
import net.mgsx.gltf.loaders.glb.GLBAssetLoader;
import net.mgsx.gltf.loaders.gltf.GLTFAssetLoader;
import net.mgsx.gltf.scene3d.scene.SceneAsset;

// TODO: AWFUL memory leaks abound, pls fix

@Getter
public class SchuGame extends Game {
	public static final int VIEWPORT_WIDTH = 960, VIEWPORT_HEIGHT = 880;
	private Viewport vp;
	private SchuClient sclient;
	@Setter
	private GameRenderer grdr;
	private GameManager gmgr;
	private SchuAssetManager amgr;
	@Setter
	boolean isDebug;


	@Override
	public void create() {
		vp = new FillViewport(VIEWPORT_WIDTH, VIEWPORT_HEIGHT);

		amgr = new SchuAssetManager();
		FileHandleResolver resolver = new InternalFileHandleResolver(); // For gdx-freetype's .TTF support
		amgr.setLoader(FreeTypeFontGenerator.class, new FreeTypeFontGeneratorLoader(resolver));
		amgr.setLoader(BitmapFont.class, ".ttf", new FreetypeFontLoader(resolver));
		amgr.setLoader(SceneAsset.class, ".gltf", new GLTFAssetLoader());
		amgr.setLoader(SceneAsset.class, ".glb", new GLBAssetLoader());

//		amgr.setAudiobox(Audiobox.of("aero-seatbelt.ogg", "buttonclick.ogg", "buttonclickrelease.ogg", "buttonrollover.ogg", "combine-radio.ogg", "crit.ogg", "dominate.ogg", "duel_challenge.ogg", "hint.ogg", "hitsound.ogg", "hl2_buttonclickrelease.ogg", "notification_alert.ogg", "panel_close.ogg", "panel_open.ogg", "revenge.ogg", "slide_down.ogg", "slide_up.ogg", "unitisinbound.ogg", "vote_started.ogg", "wpn_select.ogg", "wpn_moveselect.ogg"));
//		amgr.setFontbook(Fontbook.of("99occupy.ttf", "benzin.ttf", "carat.otf", "delfino.ttf", "dina.ttc", "eastseadokdo.ttf", "koholint.ttf", "tf2build.ttf", "tf2segundo.ttf", "tinyislanders.ttf", "kyomadoka.ttf", "sm64.otf", "kurokane.otf"));
		amgr.aliasedLoad("models/schupano.obj", "modelPano", Model.class);
		amgr.finishLoadingAsset("modelPano");
		amgr.aliasedLoad("skins/onett/skin/terra-mother-ui.json", "defaultSkin", Skin.class);
		amgr.finishLoadingAsset("defaultSkin");

		Audiobox ab = new Audiobox(0.6f);
		ab.setAmgr(amgr);
		ab.importAll();
		amgr.setAudiobox(ab);

		Fontbook fb = new Fontbook();
		fb.setAmgr(amgr);
		fb.importAll();
		amgr.setFontbook(fb);

		gmgr = new GameManager(this);

		try {
			// for testing purposes
			GameServer server = new DedicatedServer(3074);
			server.start();

			sclient = new SchuClient(gmgr);
			sclient.start();
			sclient.connect(5000, "localhost", new int[]{3074});
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		gmgr.setClient(sclient);

		setScreen(new MenuScreen(this));
	}

	@Override
	public void render() {
		screen.render(0f);
	}
	
	@Override
	public void dispose() {
		grdr.dispose();
		screen.dispose();
		sclient.disconnect();
		amgr.dispose();
	}

	public static SchuAssetManager globalAmgr() {
		return ((SchuGame) Gdx.app.getApplicationListener()).getAmgr();
	}

	public static SchuGame globalGame() {
		return (SchuGame) Gdx.app.getApplicationListener();
	}
}
