package io.github.pocketrice.client;

import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.AssetManager;
import io.github.pocketrice.shared.FuzzySearch;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

// Handles base AssetManager operations as well as storing global utilities (e.g. fontbook)
public class SchuAssetManager extends AssetManager {
    @Getter @Setter
    private Fontbook fontbook;
    @Getter @Setter
    private Audiobox audiobox;
    private final Map<String, String> aliases;
    private Queue<String> assetQueue; // Prioritises aliases.
    private int numAssetQueueLoaded;

    public SchuAssetManager() {
        aliases = new HashMap<>();
        assetQueue = new ArrayDeque<>();
        numAssetQueueLoaded = 0;
    }

    public String[] fzf(String query) {
        FuzzySearch fs = new FuzzySearch(aliases.keySet());
        return fs.getFuzzy(query);
    }

    public String unalias(String alias) {
        return aliases.get(alias);
    }

    public String realias(String fileName) {
        return aliases.entrySet().stream().filter(e -> e.getValue().equals(fileName)).findFirst().orElseThrow().getKey();
    }

    // Prioritise alias over filename, and compute and add alias if not present. This means rewrapId should only be called for files guaranteed to be loaded.
    public String rewrapIdentifier(String candidate) {
        if (!aliases.containsKey(candidate) && !aliases.containsValue(candidate)) {
            aliases.put(candidate.replaceAll(".*/(?=[^,]*$)", ""), candidate); // Compute alias (local file)
        }

        return (aliases.containsKey(candidate)) ? candidate : realias(candidate);
    }

    public <T> T fuzzyGet(String fileName, Class<T> type) {
        return aliasedGet(fzf(fileName)[0], type);
    }

    public <T> T aliasedGet(String alias, Class<T> type) {
        return this.get(aliases.get(alias), type);
    }

    public boolean aliasedContains(String alias) {
        return aliases.containsKey(alias);
    }
    public synchronized <T> void aliasedLoad(String filename, String alias, Class<T> type) {
        this.load(filename, type);
        aliases.put(alias, filename);
    }

    @Override
    public synchronized <T> void load(String fileName, Class<T> type, AssetLoaderParameters<T> parameter) {
        super.load(fileName, type, parameter);
        assetQueue.add(rewrapIdentifier(fileName));
    }

    public String getCurrentLoad() {
        for (int i = 0; i < this.getLoadedAssets() - numAssetQueueLoaded; i++) {
            assetQueue.poll();
            numAssetQueueLoaded++;
        }

        //System.out.println(assetQueue);
        return (assetQueue.peek() != null) ? assetQueue.peek() : "done";
    }

    @Override
    public <T> T finishLoadingAsset(String fileId) {
        String identifier = (!this.contains(fileId)) ? aliases.get(fileId) : fileId;
        return super.finishLoadingAsset(identifier);
    }

    @Override
    public void dispose() {
        super.dispose();
        audiobox.dispose();
        fontbook.dispose();
    }
}
