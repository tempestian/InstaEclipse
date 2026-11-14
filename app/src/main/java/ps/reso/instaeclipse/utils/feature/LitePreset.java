package ps.reso.instaeclipse.utils.feature;

import android.content.SharedPreferences;

/**
 * Defines and applies the lightweight A72 Lite Pro preset:
 * enables user-selected privacy/quality/UI/download/developer tweaks,
 * aggressive performance/battery optimizations, and leaves normal read/typing/story
 * indicators enabled while keeping screenshot &amp; view-once bypasses active.
 */
public final class LitePreset {

    public static final String PREF_INITIALIZED = "__litePresetInitializedV2";

    public static final String[] PRESET_BOOL_KEYS = {
            // Core & Developer Options
            "isDevEnabled", "removeBuildExpiredPopup",
            // Media Downloads & Quality (#8, #11)
            "enablePostDownload", "enableStoryDownload", "enableReelDownload",
            "enableProfileDownload", "enableVoiceDownload", "enableDmMediaDownload",
            "enableStickerDownload", "offerAudioDownload", "downloaderUseOriginal",
            "maxUploadQuality", "highResolutionImages",
            // Ads & Tracking Removal
            "isAdBlockEnabled", "isAnalyticsBlocked", "disableTrackingLinks",
            // Ghost Mode (Seen/Typing/Story stay OFF; Screenshot + View-Once stay ON)
            "isGhostScreenshot", "isGhostViewOnce", "permanentViewMode",
            "keepEphemeralMessages", "keepUnsentMessages", "keepEditHistory",
            // Misclick Protection & Interaction (#1, #2, #5, #6, #7, #12, #15)
            "hideShareCreateGroup", "confirmCalls", "confirmFollows", "confirmFeedRefresh",
            "disableSwipeToCamera", "openLinksExternally", "disableSearchHistory",
            "disableDiscoverPeople", "hideOnboardingPrompts",
            // Reels & Feed UX (#9 + Clean UI)
            "forceReelScrubber", "offerPlaybackSpeed", "holdLeftSlowMotion",
            "exactTimestamps", "expandCaptions",
            // Clean Feed & DM (#13, #14)
            "hideSuggestionsInFeed", "hideThreadsSuggestions", "removeMetaAI",
            "hidePlusUpsells", "hideNotesTray", "hideDmSuggestions",
            "hideDmBlend", "hideThreadsButton", "profileFollowsYouLabel",
            // Performance, Battery & Cache (#20)
            "activeHooksOnly", "litePerformanceMode", "batteryPrefetchLimiter", "autoClearCache"
    };

    public static final String[] PRESET_FALSE_KEYS = {
            "isGhostSeen", "isGhostTyping", "isGhostStory"
    };

    private LitePreset() {}

    /** Applies the recommended A72 Lite Pro flags to {@link FeatureFlags}. */
    public static void applyToFlags() {
        // Core & Developer Options
        FeatureFlags.isDevEnabled = true;
        FeatureFlags.removeBuildExpiredPopup = true;

        // Media Downloads & Quality
        FeatureFlags.enablePostDownload = true;
        FeatureFlags.enableStoryDownload = true;
        FeatureFlags.enableReelDownload = true;
        FeatureFlags.enableProfileDownload = true;
        FeatureFlags.enableVoiceDownload = true;
        FeatureFlags.enableDmMediaDownload = true;
        FeatureFlags.enableStickerDownload = true;
        FeatureFlags.offerAudioDownload = true;
        FeatureFlags.downloaderUseOriginal = true;
        FeatureFlags.maxUploadQuality = true;
        FeatureFlags.highResolutionImages = true;
        ExtrasFeatures.set("highResolutionImages", true);
        FeatureFlags.forceReelQuality = Integer.MAX_VALUE;

        // Ads & Tracking Removal
        FeatureFlags.isAdBlockEnabled = true;
        FeatureFlags.isAnalyticsBlocked = true;
        FeatureFlags.disableTrackingLinks = true;

        // Ghost Mode (Seen/Typing/Story normal; Screenshot & View-Once bypass active)
        FeatureFlags.isGhostSeen = false;
        FeatureFlags.isGhostTyping = false;
        FeatureFlags.isGhostStory = false;
        FeatureFlags.isGhostScreenshot = true;
        FeatureFlags.isGhostViewOnce = true;
        FeatureFlags.permanentViewMode = true;
        FeatureFlags.keepEphemeralMessages = true;
        FeatureFlags.keepUnsentMessages = true;
        FeatureFlags.keepEditHistory = true;

        // Protection & Extras
        FeatureFlags.hideShareCreateGroup = true;
        FeatureFlags.confirmCalls = true;
        FeatureFlags.confirmFollows = true;
        FeatureFlags.confirmFeedRefresh = true;
        FeatureFlags.disableSwipeToCamera = true;
        ExtrasFeatures.set("disableSwipeToCamera", true);
        FeatureFlags.openLinksExternally = true;
        FeatureFlags.disableSearchHistory = true;
        FeatureFlags.disableDiscoverPeople = true;
        FeatureFlags.hideOnboardingPrompts = true;
        ExtrasFeatures.set("hideOnboardingPrompts", true);

        // Reels & Feed UX
        FeatureFlags.forceReelScrubber = true;
        FeatureFlags.offerPlaybackSpeed = true;
        FeatureFlags.holdLeftSlowMotion = true;
        FeatureFlags.exactTimestamps = true;
        FeatureFlags.expandCaptions = true;
        FeatureFlags.feedCornerRadius = 16;
        FeatureFlags.storyTrayScale = 92;

        // Clean Feed & DM
        FeatureFlags.hideSuggestionsInFeed = true;
        FeatureFlags.hideThreadsSuggestions = true;
        FeatureFlags.removeMetaAI = true;
        FeatureFlags.hidePlusUpsells = true;
        FeatureFlags.hideNotesTray = true;
        FeatureFlags.hideDmSuggestions = true;
        FeatureFlags.hideDmBlend = true;
        FeatureFlags.hideThreadsButton = true;
        FeatureFlags.profileFollowsYouLabel = true;

        // Smoothness, anti-lag & battery saving
        FeatureFlags.activeHooksOnly = true;
        FeatureFlags.litePerformanceMode = true;
        FeatureFlags.batteryPrefetchLimiter = true;
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
        for (String k : PRESET_FALSE_KEYS) {
            ed.putBoolean(k, false);
            if (timestampMs > 0) ed.putLong("__ts_" + k, timestampMs);
        }
        ed.putInt("autoClearCacheSizeMb", 100);
        ed.putInt("feedCornerRadius", 16);
        ed.putInt("storyTrayScale", 92);
        ed.putInt("forceReelQuality", Integer.MAX_VALUE);
        if (timestampMs > 0) {
            ed.putLong("__ts_autoClearCacheSizeMb", timestampMs);
            ed.putLong("__ts_feedCornerRadius", timestampMs);
            ed.putLong("__ts_storyTrayScale", timestampMs);
            ed.putLong("__ts_forceReelQuality", timestampMs);
        }
        ed.putBoolean(PREF_INITIALIZED, true);
        ed.apply();
    }
}
