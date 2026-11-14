package ps.reso.instaeclipse.mods.network;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;

import ps.reso.instaeclipse.xposedcompat.XC_MethodHook;
import ps.reso.instaeclipse.xposedcompat.XposedBridge;
import ps.reso.instaeclipse.xposedcompat.XposedHelpers;
import ps.reso.instaeclipse.xposedcompat.XC_LoadPackage;
import ps.reso.instaeclipse.mods.misc.FollowStatusHook;
import ps.reso.instaeclipse.utils.feature.FeatureFlags;
import ps.reso.instaeclipse.utils.feature.FeatureStatusTracker;
import ps.reso.instaeclipse.utils.log.ModuleLog;
import ps.reso.instaeclipse.utils.core.DexKitQueryCache;

public class IGNetworkInterceptor {

    private static final URI FAKE_URI = URI.create("https://127.0.0.1/404");
    private static final String FAKE_URL = "https://127.0.0.1/404";

    /**
     * Neutralise a dropped request by redirecting it to a dead local URL. IG 446+/447.0.0.39+ keeps
     * the request URL in MULTIPLE fields on the request object (e.g. a String copy plus two java.net.URI
     * copies), and the actual dispatch reads one of the copies — not necessarily the single URI field
     * we resolve for inspection. Rewriting only that one field is silently ignored, so the request
     * still goes out. Rewrite EVERY url-bearing field: every java.net.URI field, and every String
     * field that currently holds this request's URL (matched by the original value / same path).
     */
    private static void neutralizeUrlFields(Object requestObj, URI original) {
        String orig = original.toString();
        String origPath = original.getPath();
        for (java.lang.reflect.Field f : ps.reso.instaeclipse.utils.core.SafeReflect.fields(requestObj.getClass())) {
            try {
                // Never touch statics: the request class keeps a shared static URI constant
                // ("https://malformed.url.invalid/") that other requests read.
                if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
                f.setAccessible(true);
                Object v = f.get(requestObj);
                if (v instanceof URI) {
                    f.set(requestObj, FAKE_URI);
                } else if (v instanceof String s && s.startsWith("http")
                        && (s.equals(orig) || (origPath != null && !origPath.isEmpty() && s.contains(origPath)))) {
                    f.set(requestObj, FAKE_URL);
                }
            } catch (Throwable ignored) {}
        }
    }

    /**
     * TigonServiceLayer is the LAST of ~25 decorator layers (RequestCacheServiceLayer,
     * ZeroRewriteAsyncServiceLayer, ...) that all implement the same startRequest interface. A
     * layer that answers from cache never reaches TigonServiceLayer, so also apply the same
     * (idempotent) drop at the chain ENTRY, AsyncHttpService.startRequest — anchored by its stable
     * systrace string (present in 447 and 451). Rewritten URLs no longer match any drop rule, so a
     * request passing through both hooks is handled exactly once. Best-effort: on failure the
     * TigonServiceLayer hook alone still applies, as before.
     */
    private static void hookChainEntry(ClassLoader classLoader, Class<?> p1, Class<?> p2, Class<?> p3,
                                       XC_MethodHook dropHook) {
        try {
            if (ps.reso.instaeclipse.Xposed.Module.dexKitBridge == null) return;
            java.util.List<org.luckypray.dexkit.result.MethodData> found =
                    DexKitQueryCache.findMethod(ps.reso.instaeclipse.Xposed.Module.dexKitBridge, 
                            org.luckypray.dexkit.query.FindMethod.create().matcher(
                                    org.luckypray.dexkit.query.matchers.MethodMatcher.create()
                                            .name("startRequest")
                                            .usingStrings("AsyncHttpService.startRequest")));
            int n = 0;
            for (org.luckypray.dexkit.result.MethodData md : found) {
                Method m = md.getMethodInstance(classLoader);
                Class<?>[] pt = m.getParameterTypes();
                if (pt.length != 3 || pt[0] != p1 || pt[1] != p2 || pt[2] != p3) continue;
                XposedBridge.hookMethod(m, dropHook);
                n++;
            }
            ModuleLog.line("(InstaEclipse | Interceptor): chain-entry hooks: " + n);
        } catch (Throwable t) {
            ModuleLog.line("(InstaEclipse | Interceptor): ⚠️ chain-entry hook: " + t.getMessage());
        }
    }

    public void handleInterceptor(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            ClassLoader classLoader = lpparam.classLoader;

            // Locate the TigonServiceLayer class dynamically
            Class<?> tigonClass = classLoader.loadClass("com.instagram.api.tigon.TigonServiceLayer");
            Method[] methods = ps.reso.instaeclipse.utils.core.SafeReflect.methods(tigonClass);

            Class<?> random_param_1 = null;
            Class<?> random_param_2 = null;
            Class<?> random_param_3 = null;
            String uriFieldName = null;

            // Analyze methods in TigonServiceLayer
            for (Method method : methods) {
                if (method.getName().equals("startRequest") && method.getParameterCount() == 3) {
                    Class<?>[] paramTypes = method.getParameterTypes();
                    random_param_1 = paramTypes[0];
                    random_param_2 = paramTypes[1];
                    random_param_3 = paramTypes[2];
                    break;
                }
            }

            // Dynamically identify the URI field in the request object
            if (random_param_1 != null) {
                for (Field field : ps.reso.instaeclipse.utils.core.SafeReflect.fields(random_param_1)) {
                    if (field.getType().equals(URI.class)
                            && !java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                        uriFieldName = field.getName();
                        break;
                    }
                }
            }

            // If classes and fields are resolved, hook the method
            if (random_param_1 != null && random_param_2 != null && random_param_3 != null && uriFieldName != null) {
                String finalUriFieldName = uriFieldName;
                XC_MethodHook dropHook = new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) { ps.reso.instaeclipse.utils.core.JitGuard.tick(param.method);
                              try {
                                Object requestObj = param.args[0];
                                URI uri = (URI) XposedHelpers.getObjectField(requestObj, finalUriFieldName);

                                if (uri != null && uri.getPath() != null) {
                                    final String host = uri.getHost(); // null for opaque/relative URIs — guard before use
                                    final String path = uri.getPath();
                                    boolean shouldDrop = false;


                                    // Don't save searches (#280): the server keeps Recent from these
                                    if (FeatureFlags.disableSearchHistory) {
                                        shouldDrop |= path.contains("/fbsearch/register_recent_search_click/");
                                    }

                                    // Ghost Mode URIs
                                    if (FeatureFlags.isGhostSeen) {
                                        shouldDrop |= path.contains("/threads/") && path.contains("/opened")
                                                && !ps.reso.instaeclipse.utils.ghost.ReadReceiptWhitelist.pathMentionsAllowed(path);
                                    }
                                    if (FeatureFlags.keepEphemeralMessages) {
                                        shouldDrop |= path.contains("/mark_ephemeral_item_ranges_viewed");
                                    }
                                    if (FeatureFlags.isGhostScreenshot) {
                                        shouldDrop |= path.endsWith("/screenshot/") || path.endsWith("/ephemeral_screenshot/");
                                    }
                                    if (FeatureFlags.isGhostViewOnce) {
                                        shouldDrop |= path.endsWith("/item_replayed/");
                                        shouldDrop |= (path.contains("/direct") && path.endsWith("/item_seen/"));
                                    }
                                    if (FeatureFlags.isGhostStory) {
                                        // Version-agnostic: getPath() excludes the ?reel=... query, so it
                                        // reads /api/vN/media/seen/ on both old (v2) and new (447: v1) builds.
                                        shouldDrop |= path.contains("/media/seen/");
                                        FeatureStatusTracker.setHooked("GhostStories");
                                    }
                                    if (FeatureFlags.isGhostLive) {
                                        shouldDrop |= path.contains("/heartbeat_and_get_viewer_count/");
                                        FeatureStatusTracker.setHooked("GhostLive");
                                    }
                                    // Remove Meta AI (#179): stop Meta AI from replying/loading.
                                    if (FeatureFlags.removeMetaAI) {
                                        shouldDrop |= path.contains("/ig_meta_ai_side_chat_send_contextual_query/")
                                                || path.contains("/ig_meta_ai_side_chat_new_session/")
                                                || path.contains("/create_ig_meta_ai_side_chat/")
                                                || path.contains("/genai/response")
                                                || (host != null && host.contains("aistudio.instagram.com"));
                                    }

                                    // Distraction Free
                                    if (FeatureFlags.disableStories) {
                                        shouldDrop |= path.contains("/feed/reels_tray/")
                                                || path.contains("feed/get_latest_reel_media/")
                                                || path.contains("direct_v2/pending_inbox/?visual_message")
                                                || path.contains("stories/hallpass/")
                                                || path.contains("/api/v1/feed/reels_media_stream/");
                                    }
                                    if (FeatureFlags.disableFeed) {
                                        shouldDrop |= path.endsWith("/feed/timeline/");
                                    }
                                    // Full Disable Reels takes precedence over the except-DM exception:
                                    // when it is on, every clips endpoint is dropped — including
                                    // "Allow in DM" (disableReelsExceptDM) is the switch that controls DM
                                    // reels: when it is ON, DM-opened reels (/api/v1/clips/items/) are
                                    // allowed and only the reels feed/discover is dropped; when it is OFF,
                                    // full Disable Reels drops every clips endpoint including clips/items/.
                                    if (FeatureFlags.disableReels && !FeatureFlags.disableReelsExceptDM) {
                                        shouldDrop |= path.endsWith("/qp/batch_fetch/")
                                                || path.contains("api/v1/clips")
                                                || path.contains("clips")
                                                || path.contains("mixed_media")
                                                || path.contains("mixed_media/discover/stream/");
                                    }
                                    if (FeatureFlags.disableReelsExceptDM) {
                                        if (path.startsWith("/api/v1/direct_v2/")) {
                                            return;
                                        }
                                        shouldDrop |= (path.startsWith("/api/v1/clips/") && uri.getQuery() != null
                                                && (uri.getQuery().contains("next_media_ids=")
                                                || uri.getQuery().contains("max_id=")))
                                                || path.contains("/clips/discover/")
                                                || path.contains("/mixed_media/discover/stream/");
                                    }
                                    if (FeatureFlags.disableExplore) {
                                        shouldDrop |= path.contains("/discover/topical_explore")
                                                || path.contains("/discover/topical_explore_stream")
                                                || (host != null && host.contains("i.instagram.com") && path.contains("/api/v1/fbsearch/top_serp/"));
                                    }
                                    // Suggested accounts under the DM inbox (#282): the inbox's
                                    // "empty state" unit carries them; the chat list is unaffected.
                                    if (FeatureFlags.hideDmSuggestions) {
                                        shouldDrop |= path.contains("/dynamic_onboarding/get_direct_empty_state/");
                                    }
                                    if (FeatureFlags.disableComments) {
                                        shouldDrop |= path.contains("/api/v1/media/") && path.contains("comments/");
                                    }

                                    // Ads
                                    if (FeatureFlags.isAdBlockEnabled) {
                                        shouldDrop |= path.contains("profile_ads/get_profile_ads/")
                                                || path.contains("/async_ads/")
                                                || path.contains("/feed/injected_reels_media/")
                                                || path.equals("/api/v1/ads/graphql/");
                                    }

                                    // Analytics
                                    if (FeatureFlags.isAnalyticsBlocked) {
                                        shouldDrop |= (host != null && (host.contains("graph.instagram.com")
                                                || host.contains("graph.facebook.com")))
                                                || path.contains("/logging_client_events");
                                    }

                                    // Lite Performance Mode: drop background profiling, QPL, QP promos, zero-rating & trace uploads
                                    if (FeatureFlags.litePerformanceMode) {
                                        shouldDrop |= path.contains("/api/v1/loom/")
                                                || path.contains("/loom/")
                                                || path.contains("/api/v1/qpl/")
                                                || path.contains("/qpl/")
                                                || path.contains("/qp/batch_fetch/")
                                                || path.contains("/api/v1/qp/")
                                                || path.contains("/api/v1/zr/")
                                                || path.contains("/api/v1/analytics/")
                                                || path.contains("/client_telemetry/")
                                                || path.contains("/api/v1/fbsearch/suggested_searches/")
                                                || (host != null && host.startsWith("rupload.")
                                                && (path.contains("log") || path.contains("trace") || path.contains("loom") || path.contains("crash") || path.contains("analytics")));
                                    }

                                    // Battery Saver Prefetch Limiter: drop speculative background prefetches (keep DMs untouched)
                                    if (FeatureFlags.batteryPrefetchLimiter && !path.startsWith("/api/v1/direct_v2/")) {
                                        String query = uri.getQuery();
                                        shouldDrop |= path.contains("/prefetch/")
                                                || (query != null && (query.contains("is_prefetch=1")
                                                || query.contains("is_prefetch=true")
                                                || query.contains("prefetch_reason=")
                                                || query.contains("reason=cold_start_prefetch")
                                                || query.contains("reason=pull_to_refresh_prefetch")
                                                || query.contains("feed_fetch_reason=prefetch")));
                                    }

                                    // Misc
                                    if (FeatureFlags.spoofLastSeen) {
                                        shouldDrop |= path.contains("/push/setForegroundState/")
                                                || path.contains("/accounts/update_active_status")
                                                || path.contains("/notes/create_note")
                                                || path.contains("/accounts/set_presence_disabled")
                                                || path.contains("/update_active_status")
                                                || path.contains("/banyan/banyan/")
                                                || path.endsWith("/last_active/")
                                                || path.contains("/presence/");
                                        FeatureStatusTracker.setHooked("SpoofLastSeen");
                                    }
                                    // NOTE: Disable Repost is handled at the UI/action level in
                                    // mods.ui.DisableRepostHook — NOT here. Dropping the repost network
                                    // request is ineffective because IG applies the repost optimistically
                                    // client-side, so the repost completes even when the request is dropped.
                                    if (FeatureFlags.disableDiscoverPeople) {
                                        shouldDrop |= path.contains("/discover/ayml/");
                                        shouldDrop |= path.contains("discover/chaining/");
                                        FeatureStatusTracker.setHooked("DisableDiscoverPeople");
                                    }

                                    if (shouldDrop) {
                                        neutralizeUrlFields(requestObj, uri);
                                    }

                                    // Follow status
                                    if (FeatureFlags.showFollowerToast || FeatureFlags.profileFollowsYouLabel) {
                                        FeatureStatusTracker.setHooked("FollowerToast");
                                        FollowStatusHook.handleRequest(uri, param.args);
                                    }
                                }
                              } catch (Throwable ignored) {
                                  // A malformed/opaque request must never crash IG's network dispatch.
                              }
                            }
                        };
                XposedHelpers.findAndHookMethod("com.instagram.api.tigon.TigonServiceLayer", classLoader, "startRequest",
                        random_param_1, random_param_2, random_param_3, dropHook);
                hookChainEntry(classLoader, random_param_1, random_param_2, random_param_3, dropHook);
                // These act on every matching request, so they are in place once the hook is.
                for (String key : new String[]{"BlockAnalytics", "DisableStoriesSection", "DisableFeedSection",
                        "DisableReelsSection", "DisableExploreSection", "DisableCommentsSection", "HideDmSuggestions"})
                    FeatureStatusTracker.setHooked(key);
            } else {
                ModuleLog.line("(InstaEclipse | Interceptor): Could not resolve required classes or fields.");
            }

        } catch (Exception e) {
            ModuleLog.line("(InstaEclipse | Interceptor): ❌ " + e.getMessage());
        }
    }
}
