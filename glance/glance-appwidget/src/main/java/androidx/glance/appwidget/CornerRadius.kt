/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.glance.appwidget

import androidx.annotation.DimenRes
import androidx.compose.ui.unit.Dp
import androidx.glance.GlanceModifier
import androidx.glance.unit.Dimension

/**
 * Modifier to add Rounded Corners on Android S+.
 */
internal data class CornerRadiusModifier(val radius: Dimension) : GlanceModifier.Element

/**
 * Adds rounded corners for the current view.
 *
 * Note: Only works on Android S+.
 */
fun GlanceModifier.cornerRadius(radius: Dp): GlanceModifier =
    this.then(CornerRadiusModifier(Dimension.Dp(radius)))

/**
 * Adds rounded corners for the current view, using resources.
 *
 * Note: Only works on Android S+.
 */
fun GlanceModifier.cornerRadius(@DimenRes radius: Int): GlanceModifier =
    this.then(CornerRadiusModifier(Dimension.Resource(radius)))
