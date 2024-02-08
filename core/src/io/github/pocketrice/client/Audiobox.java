package io.github.pocketrice.client;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import io.github.pocketrice.shared.FuzzySearch;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Audiobox {
    Map<String, Sound> sfxs;
    Map<String, Music> bgms;

    public Audiobox() {
        sfxs = new HashMap<>();
        bgms = new HashMap<>();
    }

    public Sound getFuzzySfx(String name) {
        FuzzySearch fs = new FuzzySearch(sfxs.keySet());
        String res = fs.getFuzzy(name)[0];
        return sfxs.get(res);
    }

    public Music getFuzzyBgm(String name) {
        FuzzySearch fs = new FuzzySearch(bgms.keySet());
        String res = fs.getFuzzy(name)[0];

        return bgms.get(res);
    }

    public void loadAudio(boolean isSfx, String audioFile) {
        if (!audioFile.matches(".*(\\.ogg|\\.mp3|\\.wav).*")) System.err.println("Warning: " + audioFile + " is not .ogg, .wav, or .mp3 format. Likely will not load.");

        if (isSfx) {
            Sound sfx = Gdx.audio.newSound(Gdx.files.internal("audio/" + audioFile));
            sfxs.put(audioFile, sfx);
        } else {
            Music bgm = Gdx.audio.newMusic(Gdx.files.internal("audio/" + audioFile));
            bgm.setLooping(true);
            bgms.put(audioFile, bgm);
        }
    }

    public void loadAudios(boolean isSfx, String... audioFiles) {
        for (String af : audioFiles) {
            loadAudio(isSfx, af);
        }
    }

    public void playSfx(String sfx, float volume) {
        Sound sound = getFuzzySfx(sfx);
        sound.play(volume);
    }

    public void playBgm(String bgm, float volume) {
        Music music = getFuzzyBgm(bgm);
        music.setVolume(volume);
        music.play();
    }

    public void stopBgm(String bgm) {
       Music music = getFuzzyBgm(bgm);
       music.stop();
    }

    public static Audiobox of(List<String> sfxs, List<String> bgms) {
        Audiobox abox = new Audiobox();
        abox.loadAudios(true, sfxs.toArray(new String[0]));
        abox.loadAudios(false, bgms.toArray(new String[0]));

        return abox;
    }

    public void dispose() {
        sfxs.values().forEach(Sound::dispose);
        bgms.values().forEach(Music::dispose);

        sfxs.clear();
        bgms.clear();
    }
}
