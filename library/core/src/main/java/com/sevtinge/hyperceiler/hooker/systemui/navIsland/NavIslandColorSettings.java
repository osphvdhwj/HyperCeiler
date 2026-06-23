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

import com.sevtinge.hyperceiler.core.R;
import com.sevtinge.hyperceiler.dashboard.DashboardFragment;

/**
 * Color settings for Nav Island.
 *
 * Controls:
 * - Wallpaper extraction toggle + trigger mode
 *   (ON_CHANGE=0 / ON_LAUNCH=1 / ON_DEMAND=2)
 * - Per-element color pickers:
 *     label text, value text, icon,
 *     slider filled track, slider unfilled track, button background
 *
 * Default color values (stored as ARGB integers):
 *   -1             = #FFFFFFFF  white
 *   -13041667      = #FF3878BD  blue (slider fill)
 *   -5066062       = #FFB2B2B2  grey (slider track)
 *   536936448      = #200A0A0A  near-transparent dark (button bg)
 */
public class NavIslandColorSettings extends DashboardFragment {

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.nav_island_colors;
    }

    @Override
    public void initPrefs() {
        // Dependency logic handled declaratively in XML.
    }
}
