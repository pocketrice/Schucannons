package io.github.pocketrice;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import io.github.pocketrice.client.SchuGame;

import static io.github.pocketrice.client.SchuGame.VIEWPORT_HEIGHT;
import static io.github.pocketrice.client.SchuGame.VIEWPORT_WIDTH;

// Please note that on macOS your application needs to be started with the -XstartOnFirstThread JVM argument
public class DesktopLauncher {
	public static void main (String[] arg) {
		Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
		config.setForegroundFPS(120);
		config.setIdleFPS(30);
		config.setTitle("Schucannons");
		config.setBackBufferConfig(8,8,8,8, 24, 0, 4);
//		config.setHdpiMode(HdpiMode.Pixels);
		config.setWindowedMode(VIEWPORT_WIDTH, VIEWPORT_HEIGHT);
		config.setResizable(false);
		new Lwjgl3Application(new SchuGame(), config);
	}
}
