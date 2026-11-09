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
package androidx.glance.adaptive.appwidget.components.badge

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.adaptive.core.slots.Badge

/** Design values of the [Badge] component that every backend draws it with. */
internal object BadgeDefaults {

    /** Type size of the badge label. */
    val LabelTextSize: TextUnit = 18.sp

    /**
     * Horizontal padding of the badge label, from the pill's outer edge, outline included.
     *
     * The pill has no vertical padding: it is exactly as tall as the label's line, and the outline
     * is drawn over the label's edges, as in the design.
     */
    val HorizontalPadding: Dp = 11.78.dp
}
