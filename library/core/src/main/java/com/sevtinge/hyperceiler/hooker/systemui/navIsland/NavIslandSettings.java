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
 * Root settings page for Nav Island.
 *
 * Shows the master enable toggle and links to the five sub-pages:
 * Glass Effect, Layout, Colors, Animations, Profiles.
 *
 * Hooks are wired separately (Phase 2). This fragment and its
 * sub-fragments only persist user preferences via DataStore.
 */
public class NavIslandSettings extends DashboardFragment {

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.nav_island;
    }

    @Override
    public void initPrefs() {
        // No runtime logic needed in Phase 1 — all controlled by XML dependencies.
    }
}
