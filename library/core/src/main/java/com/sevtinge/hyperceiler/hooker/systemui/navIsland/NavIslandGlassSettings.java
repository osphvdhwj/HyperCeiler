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
 * Glass Effect and Edge Effects settings for Nav Island.
 *
 * Controls:
 * - Blur radius (5–40 dp, stored as integer × 100 for float precision)
 * - Opacity (0–100 %)
 * - Glass texture (Smooth / Frosted / Ribbed)
 * - Base tint color (light and dark mode separately)
 * - Border: enable, width, color
 * - Glow: enable, radius, color
 * - Shadow: enable, depth
 */
public class NavIslandGlassSettings extends DashboardFragment {

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.nav_island_glass;
    }

    @Override
    public void initPrefs() {
        // All dependency logic handled declaratively in XML.
    }
}
