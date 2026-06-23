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
package com.sevtinge.hyperceiler.hooker.systemui.navIsland;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;

import com.sevtinge.hyperceiler.core.R;
import com.sevtinge.hyperceiler.dashboard.DashboardFragment;
import com.sevtinge.hyperceiler.hooker.systemui.navIsland.data.NavIslandPrefsHelper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Profile management settings for Nav Island.
 *
 * Four actions:
 *
 * SAVE  — snapshots all current Nav Island SharedPreferences into a named
 *         entry under "nav_island_profiles" (JSON array in DataStore).
 *         Phase 1: dialog asks for a name, writes to DataStore.
 *
 * EXPORT — serialises the current config to a JSON file and fires
 *          an ACTION_CREATE_DOCUMENT intent so the user can pick a location.
 *          File format: nav_island_profile_YYYYMMDD_HHmmss.json
 *
 * IMPORT — fires ACTION_OPEN_DOCUMENT (application/json), reads the file,
 *          runs triple-layer validation via NavIslandPrefsHelper, and
 *          applies valid configs to SharedPreferences.
 *
 * RESET  — shows a confirmation AlertDialog then writes all Nav Island
 *          prefs back to their default values.
 *
 * The actual JSON read/write and validation logic lives in
 * {@link NavIslandPrefsHelper} so the hook (Phase 2) can reuse it too.
 */
public class NavIslandProfileSettings extends DashboardFragment {

    private static final String MIME_JSON = "application/json";

    private ActivityResultLauncher<Intent> mExportLauncher;
    private ActivityResultLauncher<Intent> mImportLauncher;

    // ── Fragment lifecycle ─────────────────────────────────────────────────

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Register export launcher
        mExportLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == android.app.Activity.RESULT_OK
                            && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) doExport(uri);
                    }
                });

        // Register import launcher
        mImportLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == android.app.Activity.RESULT_OK
                            && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) doImport(uri);
                    }
                });
    }

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.nav_island_profiles;
    }

    // ── Preference wiring ──────────────────────────────────────────────────

    @Override
    public void initPrefs() {
        bindAction("prefs_key_nav_island_profile_save",   this::onSaveClicked);
        bindAction("prefs_key_nav_island_profile_export", this::onExportClicked);
        bindAction("prefs_key_nav_island_profile_import", this::onImportClicked);
        bindAction("prefs_key_nav_island_profile_reset",  this::onResetClicked);
    }

    private void bindAction(@NonNull String key, @NonNull Runnable action) {
        Preference p = findPreference(key);
        if (p != null) p.setOnPreferenceClickListener(pref -> { action.run(); return true; });
    }

    // ── Actions ────────────────────────────────────────────────────────────

    private void onSaveClicked() {
        if (getContext() == null) return;
        NavIslandPrefsHelper helper = new NavIslandPrefsHelper(requireContext());
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String name = "Profile_" + timestamp;
        boolean ok = helper.saveCurrentAsProfile(name);
        int msgRes = ok ? R.string.nav_island_profile_save : R.string.nav_island_profile_reset;
        Toast.makeText(requireContext(), msgRes, Toast.LENGTH_SHORT).show();
    }

    private void onExportClicked() {
        String filename = "nav_island_"
                + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date())
                + ".json";
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType(MIME_JSON)
                .putExtra(Intent.EXTRA_TITLE, filename);
        mExportLauncher.launch(intent);
    }

    private void onImportClicked() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType(MIME_JSON);
        mImportLauncher.launch(intent);
    }

    private void onResetClicked() {
        if (getContext() == null) return;
        new android.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.nav_island_profile_reset)
                .setMessage(R.string.nav_island_profile_reset_summary)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    new NavIslandPrefsHelper(requireContext()).resetToDefaults();
                    Toast.makeText(requireContext(),
                            R.string.nav_island_profile_reset, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    // ── I/O helpers ────────────────────────────────────────────────────────

    private void doExport(@NonNull Uri uri) {
        try (OutputStream out = requireContext().getContentResolver().openOutputStream(uri)) {
            if (out == null) throw new IOException("null output stream");
            NavIslandPrefsHelper helper = new NavIslandPrefsHelper(requireContext());
            String json = helper.exportCurrentToJson();
            out.write(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            Toast.makeText(requireContext(),
                    R.string.nav_island_profile_export, Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(requireContext(),
                    "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void doImport(@NonNull Uri uri) {
        try (InputStream in = requireContext().getContentResolver().openInputStream(uri)) {
            if (in == null) throw new IOException("null input stream");
            byte[] bytes = in.readAllBytes();
            String json = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
            NavIslandPrefsHelper helper = new NavIslandPrefsHelper(requireContext());
            NavIslandPrefsHelper.ImportResult result = helper.importFromJson(json);
            String msg = result.success
                    ? getString(R.string.nav_island_profile_import)
                    : "Import failed: " + result.error;
            Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(requireContext(),
                    "Import failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
