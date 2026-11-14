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
        FeatureFlags.isGhostSeen = false;
        FeatureFlags.enablePostDownload = false;
        FeatureFlags.activeHooksOnly = false;
        FeatureFlags.litePerformanceMode = false;
        FeatureFlags.autoClearCacheSizeMb = 0;

        LitePreset.applyToFlags();

        assertTrue(FeatureFlags.isDevEnabled);
        assertTrue(FeatureFlags.removeBuildExpiredPopup);
        assertTrue(FeatureFlags.isAdBlockEnabled);
        assertTrue(FeatureFlags.isAnalyticsBlocked);
        assertTrue(FeatureFlags.disableTrackingLinks);
        assertTrue(FeatureFlags.isGhostSeen);
        assertTrue(FeatureFlags.isGhostTyping);
        assertTrue(FeatureFlags.isGhostStory);
        assertTrue(FeatureFlags.isGhostScreenshot);
        assertTrue(FeatureFlags.isGhostViewOnce);
        assertTrue(FeatureFlags.enablePostDownload);
        assertTrue(FeatureFlags.enableReelDownload);
        assertTrue(FeatureFlags.enableStoryDownload);
        assertTrue(FeatureFlags.enableProfileDownload);
        assertTrue(FeatureFlags.activeHooksOnly);
        assertTrue(FeatureFlags.litePerformanceMode);
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
    }
}
