package io.github.pocketrice;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.async.AsyncExecutor;
import com.badlogic.gdx.utils.async.AsyncResult;
import com.badlogic.gdx.utils.viewport.FillViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.github.pocketrice.HumanPlayer.prompt;

// ** PRYSM is my own custom physics "engine". Currently only needed bits for projectile motion are implemented, but that's why it's in a separate folder.
// TODO: AWFUL memory leaks abound, pls fix

public class App extends ApplicationAdapter {
	private Viewport vp;
	public static final int VIEWPORT_WIDTH = 960, VIEWPORT_HEIGHT = 880;
	private Matchmaker mm;
	private GameEnvironment genv; // TODO REMOVE THIS LATER
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

		mm = new Matchmaker();
		System.out.println(mm);

		async = new AsyncExecutor(4);
		List<String> availableMatchStrs = mm.availableMatches.stream().map(m -> "" + (m.matchName.isEmpty() ? m.matchId : m.matchName)).collect(Collectors.toList());
		availableMatchStrs.add("RANDOM");

		String matchSel = "RANDOM";//prompt("Select a match to join, or pick RANDOM.", "invalid match identifier or option.", availableMatchStrs.toArray(new String[0]), false, true, true);
		Match match;
		if (matchSel.equals("RANDOM")) {
			match = mm.connectPlayers(hp, bp);
		} else {
			match = mm.connectPlayers(mm.findMatch(matchSel), hp, bp);
		}

		genv = new GameEnvironment(match);


		task = async.submit(() -> {
		try {
			match.start();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
            return null;
        });
	}

	@Override
	public void render() {
		ScreenUtils.clear(0.2f, 0.2f, 0.2f, 1);
		//mm.update();
		genv.render();
	}
	
	@Override
	public void dispose() {
		mm.dispose();
		genv.dispose();
	}
}
