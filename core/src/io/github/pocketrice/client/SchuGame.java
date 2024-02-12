package io.github.pocketrice.client;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.utils.viewport.FillViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import io.github.pocketrice.client.screens.MenuScreen;
import io.github.pocketrice.server.DedicatedServer;
import io.github.pocketrice.server.GameServer;
import lombok.Getter;

import static io.github.pocketrice.shared.AnsiCode.*;

// TODO: AWFUL memory leaks abound, pls fix

public class SchuGame extends Game {
	public static final int VIEWPORT_WIDTH = 960, VIEWPORT_HEIGHT = 880;
	public static final boolean IS_DEBUG = true;

	private Viewport vp;
	private SchuClient sclient;
	@Getter
	private GameRenderer grdr;
	@Getter
	private GameManager gmgr;


	@Override
	public void create() {
		if (IS_DEBUG) System.out.println(ANSI_YELLOW + "<!> Debug mode enabled.\n\n" + ANSI_RESET);
		vp = new FillViewport(VIEWPORT_WIDTH, VIEWPORT_HEIGHT);
		gmgr = new GameManager();
		grdr = new GameRenderer(gmgr);

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
		gmgr.setGrdr(grdr);

		setScreen(new MenuScreen(this, vp));
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
	}
}
