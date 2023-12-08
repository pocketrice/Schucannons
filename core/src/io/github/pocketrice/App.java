package io.github.pocketrice;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FillViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.util.ArrayList;

public class App extends ApplicationAdapter {
	private GameEnvironment gameEnv;
	private Match match;
	private Viewport vp;
	public static final int VIEWPORT_WIDTH = 960, VIEWPORT_HEIGHT = 880;
	//private Matchmaker mm;

	@Override
	public void create() {
		match = new Match(new HumanPlayer(), new HumanPlayer(), new ArrayList<>());
		gameEnv = new GameEnvironment(match);
		vp = new FillViewport(VIEWPORT_WIDTH, VIEWPORT_HEIGHT);
	}

	@Override
	public void render() {
		ScreenUtils.clear(0.2f, 0.2f, 0.2f, 1);
		gameEnv.render();
	}
	
	@Override
	public void dispose() {
		gameEnv.dispose();
		match.dispose();
	}
}
