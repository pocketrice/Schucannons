package io.github.pocketrice.client;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import io.github.pocketrice.shared.FuzzySearch;

import java.util.ArrayList;
import java.util.List;

public class Audiobox {
    List<Sound> sfxs;
    List<Music> bgms;

    public Audiobox(List<Sound> sfxs, List<Music> bgms) {
        this.sfxs = new ArrayList<>(sfxs);
        this.bgms = new ArrayList<>(bgms);
    }

    public Sound getFuzzySfx(String name) {
        FuzzySearch fs = new FuzzySearch(sfxs);
        String res = fs.getFuzzy(name)[0];

        return sfxs.stream().filter(sfx -> sfx.toString().equals(res)).findFirst().get();
    }

    public Music getFuzzyBgm(String name) {
        FuzzySearch fs = new FuzzySearch(bgms);
        String res = fs.getFuzzy(name)[0];

        return bgms.stream().filter(bgm -> bgm.toString().equals(res)).findFirst().get();
    }

    public void loadAudio(boolean isSfx, String audioFile) {
        if (!audioFile.matches(".*(\\.ogg|\\.mp3|\\.wav).*")) System.err.println("Warning: " + audioFile + " is not .ogg, .wav, or .mp3 format. Likely will not load.");

        if (isSfx) {
            Sound sfx = Gdx.audio.newSound(Gdx.files.internal("audio/" + audioFile));
            sfxs.add(sfx);
        } else {
            Music bgm = Gdx.audio.newMusic(Gdx.files.internal("audio/" + audioFile));
            bgm.setLooping(true);
            bgms.add(bgm);
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
        Audiobox abox = new Audiobox(List.of(), List.of());
        abox.loadAudios(true, sfxs.toArray(new String[0]));
        abox.loadAudios(false, bgms.toArray(new String[0]));

        return abox;
    }

    public void dispose() {
        sfxs.forEach(Sound::dispose);
        bgms.forEach(Music::dispose);
    }
}
