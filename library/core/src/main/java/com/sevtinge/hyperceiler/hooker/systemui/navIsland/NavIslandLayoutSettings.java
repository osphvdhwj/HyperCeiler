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

import androidx.preference.SwitchPreference;

import com.sevtinge.hyperceiler.core.R;
import com.sevtinge.hyperceiler.dashboard.DashboardFragment;

/**
 * Layout settings for Nav Island.
 *
 * Controls:
 * - Pill width (200–400 dp)
 * - Pill corner radius (8–48 dp)
 * - Brightness bar: show / hide
 * - Volume bar: show / hide
 * - Volume smart detection (show only active audio streams)
 * - Spatial Audio button: show / hide
 * - Bar order (brightness first vs volume first)
 */
public class NavIslandLayoutSettings extends DashboardFragment {

    private SwitchPreference mVolumeEnable;
    private SwitchPreference mSmartDetect;
    private SwitchPreference mSpatialAudio;

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.nav_island_layout;
    }

    @Override
    public void initPrefs() {
        mVolumeEnable  = findPreference("prefs_key_nav_island_bar_volume_enable");
        mSmartDetect   = findPreference("prefs_key_nav_island_bar_volume_smart_detect");
        mSpatialAudio  = findPreference("prefs_key_nav_island_bar_spatial_audio");

        // The dependency is already declared in XML, but we also guard
        // programmatically so future code additions don't silently misbehave.
        if (mVolumeEnable != null) {
            mVolumeEnable.setOnPreferenceChangeListener((pref, newVal) -> {
                boolean enabled = (boolean) newVal;
                if (mSmartDetect  != null) mSmartDetect.setEnabled(enabled);
                if (mSpatialAudio != null) mSpatialAudio.setEnabled(enabled);
                return true;
            });
        }
    }
}
