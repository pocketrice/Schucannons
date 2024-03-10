package io.github.pocketrice.client;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;

import java.util.ArrayList;
import java.util.List;

import static io.github.pocketrice.client.SchuGame.getGlobalAmgr;
import static io.github.pocketrice.shared.AnsiCode.*;

// TODO: implement TuningFork's custom sound effects.
public class Audiobox {
    static final float MASTER_VOLUME = 0.2f;
    List<String> sfxs;
    List<String> bgms;
    SchuAssetManager amgr;

    public Audiobox() {
        sfxs = new ArrayList<>();
        bgms = new ArrayList<>();
    }

    public void setAmgr(SchuAssetManager am) {
        amgr = am;
    }

    public void loadAudio(boolean isSfx, String alias) {
        String audioFile = (alias.matches(".*\\.[a-z0-9]+") ? alias : amgr.fzf(alias)[0]);
        if (!Gdx.files.internal("audio/" + audioFile).exists()) System.err.println("Warning: " + audioFile + " does not exist.");
        if (!audioFile.matches(".*(\\.ogg|\\.mp3|\\.wav)")) System.err.println("Warning: " + audioFile + " is not .ogg, .wav, or .mp3 format. Likely will not load.");

        if (!amgr.isLoaded("audio/" + audioFile)) {
            if (isSfx) {
                amgr.aliasedLoad("audio/" + audioFile, alias, Sound.class);
                sfxs.add(audioFile);
            } else {
                amgr.aliasedLoad("audio/" + audioFile, alias, Music.class);
                bgms.add(audioFile);
            }
        }
    }

    public void loadAudios(boolean isSfx, String... audioFiles) {
        for (String af : audioFiles) {
            loadAudio(isSfx, af);
        }
    }

    public void playSfx(String sfx, float volume) {
        if (!sfx.isBlank()) {
            Sound sound = amgr.fuzzyGet(sfx, Sound.class);
            sound.play(volume * MASTER_VOLUME);
        }
    }

    public void playBgm(String bgm, float volume) {
        if (!bgm.isBlank()) {
            Music music = amgr.fuzzyGet(bgm, Music.class);
            music.setLooping(true);
            music.setVolume(volume * MASTER_VOLUME);
            music.play();
        }
    }

    // Should not store assets to collector class since that creates duplicates. Thus, only stores names (""pointers"")
    public void stopBgm(String bgm) {
        if (!bgm.isBlank()) {
            Music music = amgr.fuzzyGet(bgm, Music.class);
            music.stop();
        }
    }

    // Assumes all audio are SFX
    public void importAll() {
        FileHandle audioDir = Gdx.files.internal("assets/audio/");

        if (audioDir.isDirectory()) {
            for (FileHandle audio : audioDir.list()) {
                String filename = audio.name();
                if (filename.matches("[^*][^*].*(\\.ogg|\\.mp3|\\.wav)")) { // To disable a file, append ** to beginning.
                    loadAudio(true, filename);
                }
            }
        }

        System.out.println(ANSI_BLUE + "[✧˖°] Loaded all audio from assets/audio!" + ANSI_RESET);
    }

    public static Audiobox of(String... sfxs) {
        Audiobox abox = new Audiobox();
        abox.amgr = getGlobalAmgr();
        abox.loadAudios(true, sfxs);
        abox.loadAudios(false);

        return abox;
    }

    public static Audiobox of(List<String> sfxs, List<String> bgms) {
        Audiobox abox = new Audiobox();
        abox.amgr = getGlobalAmgr();
        abox.loadAudios(true, sfxs.toArray(new String[0]));
        abox.loadAudios(false, bgms.toArray(new String[0]));

        return abox;
    }

    public void dispose() {
        sfxs.forEach(s -> amgr.unload("audio/" + s));
        bgms.forEach(s -> amgr.unload("audio/" + s));

        sfxs.clear();
        bgms.clear();
    }
}
