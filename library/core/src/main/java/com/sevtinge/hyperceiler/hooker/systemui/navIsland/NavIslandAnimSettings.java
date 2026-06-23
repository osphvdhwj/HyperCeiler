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
 * Animation settings for Nav Island.
 *
 * Controls:
 * - Expansion duration (100–500 ms, default 350 ms)
 * - Easing curve preset:
 *     0 = Emphasized   cubic-bezier(0.2, 0, 0, 1) — Material default
 *     1 = Standard     cubic-bezier(0.4, 0, 0.2, 1)
 *     2 = Decelerate   cubic-bezier(0, 0, 0.2, 1)
 *     3 = Spring       spring physics (stiffness=200, damping=0.8)
 * - Speed multiplier (50–200 %, default 100 %)
 * - Haptic feedback on expansion and slider steps
 *
 * Phase 2: hook reads these values and applies them when the pill
 * expands/collapses in SystemUI.
 */
public class NavIslandAnimSettings extends DashboardFragment {

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.nav_island_anim;
    }

    @Override
    public void initPrefs() {
        // All preferences are standalone — no programmatic dependencies needed.
    }
}
