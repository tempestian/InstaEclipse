package ps.reso.instaeclipse.utils.feature;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import ps.reso.instaeclipse.utils.core.PrefsMerge;

public class LitePresetTest {

    @Test
    public void applyToFlagsSetsCorePrivacyDownloadAndPerformanceFlags() {
        FeatureFlags.isDevEnabled = false;
        FeatureFlags.isAdBlockEnabled = false;
        FeatureFlags.isGhostSeen = true;
        FeatureFlags.isGhostTyping = true;
        FeatureFlags.isGhostStory = true;
        FeatureFlags.isGhostScreenshot = false;
        FeatureFlags.isGhostViewOnce = false;
        FeatureFlags.permanentViewMode = false;
        FeatureFlags.enablePostDownload = false;
        FeatureFlags.activeHooksOnly = false;
        FeatureFlags.litePerformanceMode = false;
        FeatureFlags.batteryPrefetchLimiter = false;
        FeatureFlags.hideShareCreateGroup = false;
        FeatureFlags.confirmCalls = false;
        FeatureFlags.forceReelQuality = 0;
        FeatureFlags.autoClearCacheSizeMb = 0;

        LitePreset.applyToFlags();

        assertTrue(FeatureFlags.isDevEnabled);
        assertTrue(FeatureFlags.removeBuildExpiredPopup);
        assertTrue(FeatureFlags.isAdBlockEnabled);
        assertTrue(FeatureFlags.isAnalyticsBlocked);
        assertTrue(FeatureFlags.disableTrackingLinks);
        // Seen, Typing, and Story View must be OFF in LitePreset v2 while Screenshot/View-Once stay ON
        assertFalse(FeatureFlags.isGhostSeen);
        assertFalse(FeatureFlags.isGhostTyping);
        assertFalse(FeatureFlags.isGhostStory);
        assertTrue(FeatureFlags.isGhostScreenshot);
        assertTrue(FeatureFlags.isGhostViewOnce);
        assertTrue(FeatureFlags.permanentViewMode);
        assertTrue(FeatureFlags.keepEphemeralMessages);
        assertTrue(FeatureFlags.enablePostDownload);
        assertTrue(FeatureFlags.enableReelDownload);
        assertTrue(FeatureFlags.enableStoryDownload);
        assertTrue(FeatureFlags.enableProfileDownload);
        assertTrue(FeatureFlags.enableStickerDownload);
        assertTrue(FeatureFlags.offerAudioDownload);
        assertTrue(FeatureFlags.highResolutionImages);
        assertTrue(FeatureFlags.maxUploadQuality);
        assertTrue(FeatureFlags.forceReelQuality == Integer.MAX_VALUE);
        assertTrue(FeatureFlags.hideShareCreateGroup);
        assertTrue(FeatureFlags.confirmCalls);
        assertTrue(FeatureFlags.activeHooksOnly);
        assertTrue(FeatureFlags.litePerformanceMode);
        assertTrue(FeatureFlags.batteryPrefetchLimiter);
        assertTrue(FeatureFlags.autoClearCache);
        assertTrue(FeatureFlags.autoClearCacheSizeMb >= 100);
        // DisableVideoAutoPlayHook remains off by default to avoid extra framework drawable/click hooks
        assertFalse(FeatureFlags.disableVideoAutoPlay);
    }

    @Test
    public void initMarkerIsInternalAndPresetKeysAreShared() {
        assertFalse(PrefsMerge.isSharedSetting(LitePreset.PREF_INITIALIZED, Boolean.TRUE));
        for (String key : LitePreset.PRESET_BOOL_KEYS) {
            assertTrue("Expected preset key to be shared: " + key,
                    PrefsMerge.isSharedSetting(key, Boolean.TRUE));
        }
        for (String key : LitePreset.PRESET_FALSE_KEYS) {
            assertTrue("Expected preset false key to be shared: " + key,
                    PrefsMerge.isSharedSetting(key, Boolean.FALSE));
        }
    }
}
