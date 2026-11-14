package ps.reso.instaeclipse.utils.feature;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.SharedPreferences;

import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import ps.reso.instaeclipse.mods.ui.theme.IgThemePalette;
import ps.reso.instaeclipse.utils.core.PrefsMerge;

public class LitePresetTest {

    @Test
    public void applyToFlagsSetsCorePrivacyDownloadVisualAndPerformanceFlags() {
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
        FeatureFlags.offerPlaybackSpeed = true;
        FeatureFlags.customThemeEnabled = false;
        FeatureFlags.eclipseModernUi = false;
        FeatureFlags.feedCornerRadius = 0;
        FeatureFlags.storyTrayScale = 100;
        FeatureFlags.storyRingColor = "";
        FeatureFlags.themePaletteJson = "";
        FeatureFlags.forceReelQuality = 0;
        FeatureFlags.autoClearCacheSizeMb = 0;

        LitePreset.applyToFlags();

        assertTrue(FeatureFlags.isDevEnabled);
        assertTrue(FeatureFlags.removeBuildExpiredPopup);
        assertTrue(FeatureFlags.isAdBlockEnabled);
        assertTrue(FeatureFlags.isAnalyticsBlocked);
        assertTrue(FeatureFlags.disableTrackingLinks);
        // Seen, Typing, and Story View must be OFF in LitePreset V3 while Screenshot/View-Once stay ON
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
        assertEquals(Integer.MAX_VALUE, FeatureFlags.forceReelQuality);
        assertTrue(FeatureFlags.hideShareCreateGroup);
        assertTrue(FeatureFlags.confirmCalls);
        // offerPlaybackSpeed stays false by default so Reels/Post overflow menus never carry debug rows
        assertFalse(FeatureFlags.offerPlaybackSpeed);
        assertTrue(FeatureFlags.holdLeftSlowMotion);
        assertTrue(FeatureFlags.forceReelScrubber);
        // V3 Eclipse Neo-OLED Modern UI defaults
        assertTrue(FeatureFlags.customThemeEnabled);
        assertTrue(FeatureFlags.eclipseModernUi);
        assertEquals(0, FeatureFlags.themePresetId);
        assertEquals(-1, FeatureFlags.themeLightPresetId);
        assertEquals(LitePreset.ECLIPSE_FEED_CORNER_RADIUS, FeatureFlags.feedCornerRadius);
        assertEquals(LitePreset.ECLIPSE_STORY_TRAY_SCALE, FeatureFlags.storyTrayScale);
        assertEquals(LitePreset.ECLIPSE_STORY_RING_COLOR, FeatureFlags.storyRingColor);
        for (String slot : IgThemePalette.SLOT_KEYS) {
            assertTrue("Missing palette slot " + slot,
                    LitePreset.ECLIPSE_PALETTE_JSON.contains("\"" + slot + "\":"));
        }
        assertTrue(FeatureFlags.activeHooksOnly);
        assertTrue(FeatureFlags.litePerformanceMode);
        assertTrue(FeatureFlags.batteryPrefetchLimiter);
        assertTrue(FeatureFlags.autoClearCache);
        assertTrue(FeatureFlags.autoClearCacheSizeMb >= 100);
        // DisableVideoAutoPlayHook remains off by default to avoid extra framework drawable/click hooks
        assertFalse(FeatureFlags.disableVideoAutoPlay);
    }

    @Test
    public void seedMigratesV2UsersToV3WithoutClobberingExistingPrivacyToggles() {
        Map<String, Object> store = new HashMap<>();
        store.put("__litePresetInitializedV2", Boolean.TRUE);
        store.put("isGhostSeen", Boolean.TRUE);
        store.put("offerPlaybackSpeed", Boolean.TRUE);
        SharedPreferences prefs = mapPrefs(store);

        boolean seeded = LitePreset.seedIfFirstRun(prefs);

        assertFalse(seeded);
        assertEquals(Boolean.TRUE, store.get(LitePreset.PREF_INITIALIZED));
        assertEquals(Boolean.TRUE, store.get("customThemeEnabled"));
        assertEquals(Boolean.TRUE, store.get("eclipseModernUi"));
        assertEquals(Boolean.FALSE, store.get("offerPlaybackSpeed"));
        assertEquals(LitePreset.ECLIPSE_FEED_CORNER_RADIUS, store.get("feedCornerRadius"));
        assertEquals(LitePreset.ECLIPSE_STORY_TRAY_SCALE, store.get("storyTrayScale"));
        assertEquals(LitePreset.ECLIPSE_STORY_RING_COLOR, store.get("storyRingColor"));
        assertEquals(LitePreset.ECLIPSE_PALETTE_JSON, store.get("themePaletteJson"));
        // Unrelated user toggle preserved
        assertEquals(Boolean.TRUE, store.get("isGhostSeen"));
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

    private static SharedPreferences mapPrefs(Map<String, Object> map) {
        SharedPreferences.Editor editor = (SharedPreferences.Editor) Proxy.newProxyInstance(
                LitePresetTest.class.getClassLoader(),
                new Class<?>[]{SharedPreferences.Editor.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if (name.startsWith("put") && args != null && args.length == 2) {
                        map.put((String) args[0], args[1]);
                        return proxy;
                    }
                    if ("commit".equals(name)) return Boolean.TRUE;
                    return null;
                });
        return (SharedPreferences) Proxy.newProxyInstance(
                LitePresetTest.class.getClassLoader(),
                new Class<?>[]{SharedPreferences.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("edit".equals(name)) return editor;
                    if ("contains".equals(name)) return map.containsKey(args[0]);
                    if ("getBoolean".equals(name)) {
                        Object v = map.get(args[0]);
                        return v instanceof Boolean ? v : args[1];
                    }
                    return null;
                });
    }
}
