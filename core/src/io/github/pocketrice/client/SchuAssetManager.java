package io.github.pocketrice.client;

import com.badlogic.gdx.assets.AssetManager;
import io.github.pocketrice.shared.FuzzySearch;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

// Handles base AssetManager operations as well as storing global utilities (e.g. fontbook)
public class SchuAssetManager extends AssetManager {
    @Getter @Setter
    private Fontbook fontbook;
    @Getter @Setter
    private Audiobox audiobox;
    private final Map<String, String> aliases;

    public SchuAssetManager() {
        aliases = new HashMap<>();
    }
    public String[] fzf(String query) {
        FuzzySearch fs = new FuzzySearch(aliases.keySet());
        return fs.getFuzzy(query);
    }

    public String unalias(String alias) {
        return aliases.get(alias);
    }

    public <T> T fuzzyGet(String fileName, Class<T> type) {
        return aliasedGet(fzf(fileName)[0], type);
    }

    public <T> T aliasedGet(String alias, Class<T> type) {
        return this.get(aliases.get(alias), type);
    }

    public synchronized <T> void aliasedLoad(String filename, String alias, Class<T> type) {
        this.load(filename, type);
        aliases.put(alias, filename);
    }

    @Override
    public <T> T finishLoadingAsset (String fileId) {
        String identifier = (!this.contains(fileId)) ? aliases.get(fileId) : fileId;
        return super.finishLoadingAsset(identifier);
    }
}
