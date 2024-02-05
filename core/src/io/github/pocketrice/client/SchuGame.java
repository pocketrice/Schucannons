package io.github.pocketrice.client;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.utils.async.AsyncExecutor;
import com.badlogic.gdx.utils.async.AsyncResult;
import com.badlogic.gdx.utils.viewport.FillViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import io.github.pocketrice.client.screens.GameScreen;

import java.util.List;
import java.util.stream.Collectors;

// TODO: AWFUL memory leaks abound, pls fix

public class SchuGame extends Game {
	private Viewport vp;
	public static final int VIEWPORT_WIDTH = 960, VIEWPORT_HEIGHT = 880;
	private SchuClient sclient;
	private GameRenderer grdr;
	private GameManager gmgr;
	private AsyncExecutor async;
	private AsyncResult<Void> task;

	@Override
	public void create() {
//		match = new Match(new HumanPlayer(3f, new Vector3(3f,0,3f), Vector3.Zero, Vector3.Zero), BotPlayer.FILLER, new ArrayList<>());
//		match.oppoPlayer.setPlayerName("Gorb");
//		match.currentPlayer.setPlayerName("Anna");
//		gameEnv = new GameEnvironment(match);
		vp = new FillViewport(VIEWPORT_WIDTH, VIEWPORT_HEIGHT);
		HumanPlayer hp = new HumanPlayer();
		BotPlayer bp = new BotPlayer();

		try {
			sclient = new SchuClient();
			sclient.start();
			sclient.connect(5000, "192.168.1.64", new int[]{3074});
		} catch (Exception e) {
			throw new RuntimeException(e);
		}




		async = new AsyncExecutor(4);
		List<String> availableMatchStrs = mm.availableMatches.stream().map(m -> "" + (m.matchName.isEmpty() ? m.matchId : m.matchName)).collect(Collectors.toList());
		availableMatchStrs.add("AUTO");

		String matchSel = "AUTO";//prompt("Select a match to join, or pick AUTO.", "invalid match identifier or option.", availableMatchStrs.toArray(new String[0]), false, true, true);
		Match match = (matchSel.equals("AUTO")) ? mm.connectPlayers(hp, bp) : mm.connectPlayers(mm.findMatch(matchSel), hp, bp);

		gmgr = new GameManager(sclient);
		grdr = new GameRenderer(gmgr);

		setScreen(new GameScreen(this));
	}

	@Override
	public void render() {
		grdr.render();
	}
	
	@Override
	public void dispose() {
		grdr.dispose();
	}
}
