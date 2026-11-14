package ps.reso.instaeclipse.utils.feature;

import android.content.SharedPreferences;

/**
 * Defines and applies the lightweight A72 / low-lag preset:
 * enables core privacy, ad-blocking, media download, developer options,
 * and aggressive performance/battery optimizations while keeping disabled
 * modules unhooked.
 */
public final class LitePreset {

    public static final String PREF_INITIALIZED = "__litePresetInitializedV1";

    public static final String[] PRESET_BOOL_KEYS = {
            "isDevEnabled", "removeBuildExpiredPopup",
            "enablePostDownload", "enableStoryDownload", "enableReelDownload",
            "enableProfileDownload", "enableVoiceDownload", "enableDmMediaDownload",
            "isAdBlockEnabled", "isAnalyticsBlocked", "disableTrackingLinks",
            "isGhostSeen", "isGhostTyping", "isGhostStory", "isGhostScreenshot", "isGhostViewOnce",
            "activeHooksOnly", "litePerformanceMode",
            "hideSuggestionsInFeed", "hideThreadsSuggestions", "removeMetaAI", "hidePlusUpsells",
            "autoClearCache"
    };

    private LitePreset() {}

    /** Applies the recommended A72 Lite & Essentials flags to {@link FeatureFlags}. */
    public static void applyToFlags() {
        // Core requested features
        FeatureFlags.isDevEnabled = true;
        FeatureFlags.removeBuildExpiredPopup = true;

        // Media downloading
        FeatureFlags.enablePostDownload = true;
        FeatureFlags.enableStoryDownload = true;
        FeatureFlags.enableReelDownload = true;
        FeatureFlags.enableProfileDownload = true;
        FeatureFlags.enableVoiceDownload = true;
        FeatureFlags.enableDmMediaDownload = true;

        // Ads & tracking removal
        FeatureFlags.isAdBlockEnabled = true;
        FeatureFlags.isAnalyticsBlocked = true;
        FeatureFlags.disableTrackingLinks = true;

        // Ghost mode (seen / typing / story / view-once / screenshot)
        FeatureFlags.isGhostSeen = true;
        FeatureFlags.isGhostTyping = true;
        FeatureFlags.isGhostStory = true;
        FeatureFlags.isGhostScreenshot = true;
        FeatureFlags.isGhostViewOnce = true;

        // Smoothness, anti-lag & battery saving
        FeatureFlags.activeHooksOnly = true;
        FeatureFlags.litePerformanceMode = true;
        FeatureFlags.hideSuggestionsInFeed = true;
        FeatureFlags.hideThreadsSuggestions = true;
        FeatureFlags.removeMetaAI = true;
        FeatureFlags.hidePlusUpsells = true;
        FeatureFlags.autoClearCache = true;
        if (FeatureFlags.autoClearCacheSizeMb <= 0) {
            FeatureFlags.autoClearCacheSizeMb = 100;
        }
    }

    /**
     * Seeds the preset once on a fresh install (when neither {@link #PREF_INITIALIZED} nor any
     * core user setting is present yet). Returns true if the preset was applied.
     */
    public static boolean seedIfFirstRun(SharedPreferences prefs) {
        if (prefs == null || prefs.getBoolean(PREF_INITIALIZED, false)) {
            return false;
        }
        boolean hasExisting = prefs.contains("isAdBlockEnabled")
                || prefs.contains("isGhostSeen")
                || prefs.contains("enablePostDownload")
                || prefs.contains("isDevEnabled");
        if (hasExisting) {
            prefs.edit().putBoolean(PREF_INITIALIZED, true).apply();
            return false;
        }
        applyToFlags();
        prefs.edit().putBoolean(PREF_INITIALIZED, true).apply();
        return true;
    }

    /** Writes all preset flags directly into a {@link SharedPreferences} (e.g. companion cache). */
    public static void writeToPrefs(SharedPreferences prefs, long timestampMs) {
        if (prefs == null) return;
        SharedPreferences.Editor ed = prefs.edit();
        for (String k : PRESET_BOOL_KEYS) {
            ed.putBoolean(k, true);
            if (timestampMs > 0) ed.putLong("__ts_" + k, timestampMs);
        }
        ed.putInt("autoClearCacheSizeMb", 100);
        if (timestampMs > 0) ed.putLong("__ts_autoClearCacheSizeMb", timestampMs);
        ed.putBoolean(PREF_INITIALIZED, true);
        ed.apply();
    }
}
