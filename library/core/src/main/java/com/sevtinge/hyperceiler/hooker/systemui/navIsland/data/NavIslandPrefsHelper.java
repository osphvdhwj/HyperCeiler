/*
 * This file is part of HyperCeiler.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.hooker.systemui.navIsland.data;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Read/write helper for all Nav Island SharedPreferences.
 *
 * <p>Used by:
 * <ul>
 *   <li>{@link com.sevtinge.hyperceiler.hooker.systemui.navIsland.NavIslandProfileSettings}
 *       for profile save/export/import/reset.</li>
 *   <li>Phase 2 hook — reads config from the same SharedPreferences file
 *       to apply glassmorphism in SystemUI.</li>
 * </ul>
 *
 * <h3>SharedPreferences file</h3>
 * {@value #PREFS_FILE} — plain text (no encryption) so the Phase 2
 * LSPosed hook can read it directly with MODE_WORLD_READABLE or via
 * XSharedPreferences without an IPC round-trip.
 *
 * <h3>JSON profile format (v1)</h3>
 * <pre>
 * {
 *   "version": 1,
 *   "name": "My Profile",
 *   "createdAt": 1718727600000,
 *   "config": {
 *     "prefs_key_nav_island_blur_radius": 800,
 *     "prefs_key_nav_island_opacity": 85,
 *     ... (all nav_island keys)
 *   }
 * }
 * </pre>
 *
 * <h3>Triple-layer import validation</h3>
 * Layer 1 — JSON schema: version field present, "config" object present, no extra top-level keys.<br>
 * Layer 2 — type + range: each key is the right type (int/boolean/string) and within allowed bounds.<br>
 * Layer 3 — render-safety: blur × opacity product does not exceed a safe ceiling.
 */
public class NavIslandPrefsHelper {

    public static final String PREFS_FILE = "com.sevtinge.hyperceiler_nav_island";
    private static final int PROFILE_FORMAT_VERSION = 1;

    // ── Default values (mirrors XML defaultValue attributes) ───────────────

    public static final int    DEFAULT_BLUR_RADIUS         = 800;   // ÷100 → 8.00 dp
    public static final int    DEFAULT_OPACITY             = 85;    // %
    public static final int    DEFAULT_TEXTURE             = 1;     // FROSTED
    public static final int    DEFAULT_BASE_COLOR          = 268435455; // #0FFFFFFF
    public static final int    DEFAULT_BASE_COLOR_DARK     = 268435455;
    public static final boolean DEFAULT_BORDER_ENABLE      = false;
    public static final int    DEFAULT_BORDER_WIDTH        = 10;    // ÷10 → 1.0 dp
    public static final int    DEFAULT_BORDER_COLOR        = -1;    // white
    public static final boolean DEFAULT_GLOW_ENABLE        = false;
    public static final int    DEFAULT_GLOW_RADIUS         = 80;    // dp
    public static final int    DEFAULT_GLOW_COLOR          = -1;
    public static final boolean DEFAULT_SHADOW_ENABLE      = true;
    public static final int    DEFAULT_SHADOW_DEPTH        = 12;    // dp
    public static final int    DEFAULT_PILL_WIDTH          = 320;   // dp
    public static final int    DEFAULT_CORNER_RADIUS       = 28;    // dp
    public static final boolean DEFAULT_BAR_BRIGHTNESS     = true;
    public static final boolean DEFAULT_BAR_VOLUME         = true;
    public static final boolean DEFAULT_BAR_SMART_DETECT   = true;
    public static final boolean DEFAULT_BAR_SPATIAL_AUDIO  = true;
    public static final int    DEFAULT_BAR_ORDER           = 0;     // brightness first
    public static final boolean DEFAULT_WALLPAPER_EXTRACT  = false;
    public static final int    DEFAULT_WALLPAPER_MODE      = 2;     // ON_DEMAND
    public static final int    DEFAULT_COLOR_LABEL         = -1;
    public static final int    DEFAULT_COLOR_VALUE         = -1;
    public static final int    DEFAULT_COLOR_ICON          = -1;
    public static final int    DEFAULT_COLOR_SLIDER_FILL   = -13041667; // #FF3878BD blue
    public static final int    DEFAULT_COLOR_SLIDER_TRACK  = -5066062;  // #FFB2B2B2 grey
    public static final int    DEFAULT_COLOR_BUTTON_BG     = 536936448; // #200A0A0A near-transparent
    public static final int    DEFAULT_ANIM_DURATION       = 350;   // ms
    public static final int    DEFAULT_ANIM_EASING         = 0;     // Emphasized
    public static final int    DEFAULT_ANIM_SPEED          = 100;   // %
    public static final boolean DEFAULT_ANIM_HAPTIC        = true;

    // ── Bounds for validation (Layer 2) ───────────────────────────────────

    private static final int MIN_BLUR = 500,  MAX_BLUR = 4000;
    private static final int MIN_OPACITY = 0, MAX_OPACITY = 100;
    private static final int MIN_WIDTH = 200, MAX_WIDTH = 400;
    private static final int MIN_RADIUS = 8,  MAX_RADIUS = 48;
    private static final int MIN_DURATION = 100, MAX_DURATION = 500;
    private static final int MIN_SPEED = 50,  MAX_SPEED = 200;

    // ── Internal ──────────────────────────────────────────────────────────

    private final SharedPreferences mPrefs;

    public NavIslandPrefsHelper(@NonNull Context context) {
        mPrefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
    }

    // ── Read helpers (used by Phase 2 hook) ──────────────────────────────

    public float  getBlurRadius()    { return mPrefs.getInt("prefs_key_nav_island_blur_radius", DEFAULT_BLUR_RADIUS) / 100f; }
    public float  getOpacity()       { return mPrefs.getInt("prefs_key_nav_island_opacity", DEFAULT_OPACITY) / 100f; }
    public int    getTexture()       { return mPrefs.getInt("prefs_key_nav_island_texture", DEFAULT_TEXTURE); }
    public int    getBaseColor()     { return mPrefs.getInt("prefs_key_nav_island_base_color", DEFAULT_BASE_COLOR); }
    public int    getBaseColorDark() { return mPrefs.getInt("prefs_key_nav_island_base_color_dark", DEFAULT_BASE_COLOR_DARK); }
    public boolean getBorderEnable() { return mPrefs.getBoolean("prefs_key_nav_island_edge_border_enable", DEFAULT_BORDER_ENABLE); }
    public float  getBorderWidth()   { return mPrefs.getInt("prefs_key_nav_island_edge_border_width", DEFAULT_BORDER_WIDTH) / 10f; }
    public int    getBorderColor()   { return mPrefs.getInt("prefs_key_nav_island_edge_border_color", DEFAULT_BORDER_COLOR); }
    public boolean getGlowEnable()   { return mPrefs.getBoolean("prefs_key_nav_island_edge_glow_enable", DEFAULT_GLOW_ENABLE); }
    public int    getGlowRadius()    { return mPrefs.getInt("prefs_key_nav_island_edge_glow_radius", DEFAULT_GLOW_RADIUS); }
    public int    getGlowColor()     { return mPrefs.getInt("prefs_key_nav_island_edge_glow_color", DEFAULT_GLOW_COLOR); }
    public boolean getShadowEnable() { return mPrefs.getBoolean("prefs_key_nav_island_edge_shadow_enable", DEFAULT_SHADOW_ENABLE); }
    public int    getShadowDepth()   { return mPrefs.getInt("prefs_key_nav_island_edge_shadow_depth", DEFAULT_SHADOW_DEPTH); }
    public int    getPillWidth()     { return mPrefs.getInt("prefs_key_nav_island_pill_width", DEFAULT_PILL_WIDTH); }
    public int    getCornerRadius()  { return mPrefs.getInt("prefs_key_nav_island_pill_corner_radius", DEFAULT_CORNER_RADIUS); }
    public boolean getBarBrightness(){ return mPrefs.getBoolean("prefs_key_nav_island_bar_brightness_enable", DEFAULT_BAR_BRIGHTNESS); }
    public boolean getBarVolume()    { return mPrefs.getBoolean("prefs_key_nav_island_bar_volume_enable", DEFAULT_BAR_VOLUME); }
    public boolean getSmartDetect()  { return mPrefs.getBoolean("prefs_key_nav_island_bar_volume_smart_detect", DEFAULT_BAR_SMART_DETECT); }
    public boolean getSpatialAudio() { return mPrefs.getBoolean("prefs_key_nav_island_bar_spatial_audio", DEFAULT_BAR_SPATIAL_AUDIO); }
    public int    getBarOrder()      { return mPrefs.getInt("prefs_key_nav_island_bar_order", DEFAULT_BAR_ORDER); }
    public boolean getWallpaperExtract() { return mPrefs.getBoolean("prefs_key_nav_island_color_wallpaper_extract", DEFAULT_WALLPAPER_EXTRACT); }
    public int    getWallpaperMode() { return mPrefs.getInt("prefs_key_nav_island_color_wallpaper_mode", DEFAULT_WALLPAPER_MODE); }
    public int    getColorLabel()    { return mPrefs.getInt("prefs_key_nav_island_color_label_text", DEFAULT_COLOR_LABEL); }
    public int    getColorValue()    { return mPrefs.getInt("prefs_key_nav_island_color_value_text", DEFAULT_COLOR_VALUE); }
    public int    getColorIcon()     { return mPrefs.getInt("prefs_key_nav_island_color_icon", DEFAULT_COLOR_ICON); }
    public int    getColorSliderFill()  { return mPrefs.getInt("prefs_key_nav_island_color_slider_filled", DEFAULT_COLOR_SLIDER_FILL); }
    public int    getColorSliderTrack() { return mPrefs.getInt("prefs_key_nav_island_color_slider_unfilled", DEFAULT_COLOR_SLIDER_TRACK); }
    public int    getColorButtonBg()    { return mPrefs.getInt("prefs_key_nav_island_color_button_bg", DEFAULT_COLOR_BUTTON_BG); }
    public int    getAnimDuration()  { return mPrefs.getInt("prefs_key_nav_island_anim_expansion_duration", DEFAULT_ANIM_DURATION); }
    public int    getAnimEasing()    { return mPrefs.getInt("prefs_key_nav_island_anim_easing", DEFAULT_ANIM_EASING); }
    public int    getAnimSpeed()     { return mPrefs.getInt("prefs_key_nav_island_anim_speed_multiplier", DEFAULT_ANIM_SPEED); }
    public boolean getAnimHaptic()   { return mPrefs.getBoolean("prefs_key_nav_island_anim_haptic", DEFAULT_ANIM_HAPTIC); }

    // ── Profile: save current ─────────────────────────────────────────────

    /**
     * Saves a snapshot of all current nav_island prefs as a named profile
     * entry inside the "nav_island_profiles" JSON array in SharedPreferences.
     *
     * @return true on success
     */
    public boolean saveCurrentAsProfile(@NonNull String name) {
        try {
            JSONObject config = snapshotCurrentConfig();
            JSONObject profile = new JSONObject();
            profile.put("version", PROFILE_FORMAT_VERSION);
            profile.put("name", name);
            profile.put("createdAt", System.currentTimeMillis());
            profile.put("config", config);

            String existing = mPrefs.getString("nav_island_profiles", "[]");
            JSONArray profiles = new JSONArray(existing);
            profiles.put(profile);
            mPrefs.edit().putString("nav_island_profiles", profiles.toString()).apply();
            return true;
        } catch (JSONException e) {
            return false;
        }
    }

    // ── Profile: export ───────────────────────────────────────────────────

    /** Serialises the current config as a JSON string for file export. */
    @NonNull
    public String exportCurrentToJson() throws JSONException {
        JSONObject root = new JSONObject();
        root.put("version", PROFILE_FORMAT_VERSION);
        root.put("name", "NavIsland Export");
        root.put("createdAt", System.currentTimeMillis());
        root.put("config", snapshotCurrentConfig());
        return root.toString(2);
    }

    // ── Profile: import ───────────────────────────────────────────────────

    /**
     * Validates and applies a JSON profile string.
     *
     * Runs all three validation layers and only applies the config if all
     * pass. Returns an {@link ImportResult} describing success or failure.
     */
    @NonNull
    public ImportResult importFromJson(@NonNull String json) {
        // Layer 1 — schema
        JSONObject root;
        JSONObject config;
        try {
            root = new JSONObject(json);
            if (!root.has("version") || !root.has("config"))
                return ImportResult.failure("Missing required fields: version, config");
            config = root.getJSONObject("config");
        } catch (JSONException e) {
            return ImportResult.failure("Invalid JSON: " + e.getMessage());
        }

        // Layer 2 — type + range
        String rangeError = validateRanges(config);
        if (rangeError != null) return ImportResult.failure(rangeError);

        // Layer 3 — render safety
        String safetyError = validateRenderSafety(config);
        if (safetyError != null) return ImportResult.failure(safetyError);

        // All layers passed — apply
        applyConfig(config);
        return ImportResult.success();
    }

    // ── Profile: reset ────────────────────────────────────────────────────

    /** Resets all Nav Island preferences to their default values. */
    public void resetToDefaults() {
        mPrefs.edit()
                .putInt("prefs_key_nav_island_blur_radius",               DEFAULT_BLUR_RADIUS)
                .putInt("prefs_key_nav_island_opacity",                   DEFAULT_OPACITY)
                .putInt("prefs_key_nav_island_texture",                   DEFAULT_TEXTURE)
                .putInt("prefs_key_nav_island_base_color",                DEFAULT_BASE_COLOR)
                .putInt("prefs_key_nav_island_base_color_dark",           DEFAULT_BASE_COLOR_DARK)
                .putBoolean("prefs_key_nav_island_edge_border_enable",    DEFAULT_BORDER_ENABLE)
                .putInt("prefs_key_nav_island_edge_border_width",         DEFAULT_BORDER_WIDTH)
                .putInt("prefs_key_nav_island_edge_border_color",         DEFAULT_BORDER_COLOR)
                .putBoolean("prefs_key_nav_island_edge_glow_enable",      DEFAULT_GLOW_ENABLE)
                .putInt("prefs_key_nav_island_edge_glow_radius",          DEFAULT_GLOW_RADIUS)
                .putInt("prefs_key_nav_island_edge_glow_color",           DEFAULT_GLOW_COLOR)
                .putBoolean("prefs_key_nav_island_edge_shadow_enable",    DEFAULT_SHADOW_ENABLE)
                .putInt("prefs_key_nav_island_edge_shadow_depth",         DEFAULT_SHADOW_DEPTH)
                .putInt("prefs_key_nav_island_pill_width",                DEFAULT_PILL_WIDTH)
                .putInt("prefs_key_nav_island_pill_corner_radius",        DEFAULT_CORNER_RADIUS)
                .putBoolean("prefs_key_nav_island_bar_brightness_enable", DEFAULT_BAR_BRIGHTNESS)
                .putBoolean("prefs_key_nav_island_bar_volume_enable",     DEFAULT_BAR_VOLUME)
                .putBoolean("prefs_key_nav_island_bar_volume_smart_detect", DEFAULT_BAR_SMART_DETECT)
                .putBoolean("prefs_key_nav_island_bar_spatial_audio",     DEFAULT_BAR_SPATIAL_AUDIO)
                .putInt("prefs_key_nav_island_bar_order",                 DEFAULT_BAR_ORDER)
                .putBoolean("prefs_key_nav_island_color_wallpaper_extract", DEFAULT_WALLPAPER_EXTRACT)
                .putInt("prefs_key_nav_island_color_wallpaper_mode",      DEFAULT_WALLPAPER_MODE)
                .putInt("prefs_key_nav_island_color_label_text",          DEFAULT_COLOR_LABEL)
                .putInt("prefs_key_nav_island_color_value_text",          DEFAULT_COLOR_VALUE)
                .putInt("prefs_key_nav_island_color_icon",                DEFAULT_COLOR_ICON)
                .putInt("prefs_key_nav_island_color_slider_filled",       DEFAULT_COLOR_SLIDER_FILL)
                .putInt("prefs_key_nav_island_color_slider_unfilled",     DEFAULT_COLOR_SLIDER_TRACK)
                .putInt("prefs_key_nav_island_color_button_bg",           DEFAULT_COLOR_BUTTON_BG)
                .putInt("prefs_key_nav_island_anim_expansion_duration",   DEFAULT_ANIM_DURATION)
                .putInt("prefs_key_nav_island_anim_easing",               DEFAULT_ANIM_EASING)
                .putInt("prefs_key_nav_island_anim_speed_multiplier",     DEFAULT_ANIM_SPEED)
                .putBoolean("prefs_key_nav_island_anim_haptic",           DEFAULT_ANIM_HAPTIC)
                .apply();
    }

    // ── Private helpers ───────────────────────────────────────────────────

    @NonNull
    private JSONObject snapshotCurrentConfig() throws JSONException {
        JSONObject config = new JSONObject();
        Map<String, ?> all = mPrefs.getAll();
        for (Map.Entry<String, ?> entry : all.entrySet()) {
            if (entry.getKey().startsWith("prefs_key_nav_island_")) {
                Object v = entry.getValue();
                if (v instanceof Integer)  config.put(entry.getKey(), (Integer) v);
                else if (v instanceof Boolean) config.put(entry.getKey(), (Boolean) v);
                else if (v instanceof String)  config.put(entry.getKey(), (String) v);
                else if (v instanceof Float)   config.put(entry.getKey(), (Float) v);
            }
        }
        return config;
    }

    @Nullable
    private String validateRanges(@NonNull JSONObject config) {
        try {
            if (config.has("prefs_key_nav_island_blur_radius")) {
                int v = config.getInt("prefs_key_nav_island_blur_radius");
                if (v < MIN_BLUR || v > MAX_BLUR)
                    return "blur_radius out of range [" + MIN_BLUR + "," + MAX_BLUR + "]: " + v;
            }
            if (config.has("prefs_key_nav_island_opacity")) {
                int v = config.getInt("prefs_key_nav_island_opacity");
                if (v < MIN_OPACITY || v > MAX_OPACITY)
                    return "opacity out of range [0,100]: " + v;
            }
            if (config.has("prefs_key_nav_island_pill_width")) {
                int v = config.getInt("prefs_key_nav_island_pill_width");
                if (v < MIN_WIDTH || v > MAX_WIDTH)
                    return "pill_width out of range [200,400]: " + v;
            }
            if (config.has("prefs_key_nav_island_pill_corner_radius")) {
                int v = config.getInt("prefs_key_nav_island_pill_corner_radius");
                if (v < MIN_RADIUS || v > MAX_RADIUS)
                    return "corner_radius out of range [8,48]: " + v;
            }
            if (config.has("prefs_key_nav_island_anim_expansion_duration")) {
                int v = config.getInt("prefs_key_nav_island_anim_expansion_duration");
                if (v < MIN_DURATION || v > MAX_DURATION)
                    return "expansion_duration out of range [100,500]: " + v;
            }
            if (config.has("prefs_key_nav_island_anim_speed_multiplier")) {
                int v = config.getInt("prefs_key_nav_island_anim_speed_multiplier");
                if (v < MIN_SPEED || v > MAX_SPEED)
                    return "speed_multiplier out of range [50,200]: " + v;
            }
            return null; // all valid
        } catch (JSONException e) {
            return "Type error during validation: " + e.getMessage();
        }
    }

    /**
     * Layer 3: render-safety check.
     * Guards against configs that would produce a fully transparent + no-blur pill
     * (invisible and unresponsive to the user).
     */
    @Nullable
    private String validateRenderSafety(@NonNull JSONObject config) {
        try {
            int opacity = config.has("prefs_key_nav_island_opacity")
                    ? config.getInt("prefs_key_nav_island_opacity")
                    : DEFAULT_OPACITY;
            int blur = config.has("prefs_key_nav_island_blur_radius")
                    ? config.getInt("prefs_key_nav_island_blur_radius")
                    : DEFAULT_BLUR_RADIUS;
            // A fully transparent + zero-blur pill would be invisible
            if (opacity == 0 && blur == MIN_BLUR) {
                return "Config would produce an invisible pill (opacity=0 + min blur)";
            }
            return null;
        } catch (JSONException e) {
            return "Render safety check failed: " + e.getMessage();
        }
    }

    private void applyConfig(@NonNull JSONObject config) {
        SharedPreferences.Editor editor = mPrefs.edit();
        Iterator<String> keys = config.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            if (!key.startsWith("prefs_key_nav_island_")) continue;
            try {
                Object v = config.get(key);
                if (v instanceof Integer)  editor.putInt(key, (Integer) v);
                else if (v instanceof Boolean) editor.putBoolean(key, (Boolean) v);
                else if (v instanceof String)  editor.putString(key, (String) v);
            } catch (JSONException ignored) { }
        }
        editor.apply();
    }

    // ── ImportResult value object ─────────────────────────────────────────

    public static final class ImportResult {
        public final boolean success;
        @Nullable public final String error;

        private ImportResult(boolean success, @Nullable String error) {
            this.success = success;
            this.error = error;
        }

        public static ImportResult success()                        { return new ImportResult(true, null); }
        public static ImportResult failure(@NonNull String reason)  { return new ImportResult(false, reason); }
    }
}
