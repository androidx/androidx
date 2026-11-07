/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.glance.adaptive.samples

import android.os.Bundle
import androidx.annotation.DrawableRes
import androidx.glance.adaptive.core.action.GlanceAdaptiveAction
import androidx.glance.adaptive.core.templates.DashboardTemplateScope
import androidx.glance.adaptive.core.templates.TemplateIcon

/**
 * Taps of the Dashboard demo widgets.
 *
 * Every tile and button opens [AdaptiveDemoActivity] with its label in [EXTRA_DASHBOARD_ACTION],
 * and the activity shows that label, so each tap can be checked on the device. The widgets draw
 * with the library's default Dashboard renderer, which cannot deliver
 * [GlanceAdaptiveAction.CallbackAction]s yet, so the sample uses
 * [GlanceAdaptiveAction.LaunchAction].
 */
internal object DashboardSample {

    /** Extra of [AdaptiveDemoActivity] naming the Dashboard tile or button that was tapped. */
    const val EXTRA_DASHBOARD_ACTION = "androidx.glance.adaptive.samples.DASHBOARD_ACTION"

    /** What tapping the Dashboard tile or button labelled [label] does. */
    fun action(label: String): GlanceAdaptiveAction =
        GlanceAdaptiveAction.LaunchAction<AdaptiveDemoActivity>(
            Bundle().apply { putString(EXTRA_DASHBOARD_ACTION, label) }
        )
}

/**
 * Appends a tile labelled [label], with an optional [subtitle] beneath it, that runs
 * [DashboardSample.action] when tapped.
 */
internal fun DashboardTemplateScope.ActionsScope.shortcut(
    @DrawableRes icon: Int,
    label: String,
    subtitle: String? = null,
) {
    action(TemplateIcon.fromResource(icon), label) {
        if (subtitle != null) subtitle(subtitle)
        onClick(DashboardSample.action(label))
    }
}
