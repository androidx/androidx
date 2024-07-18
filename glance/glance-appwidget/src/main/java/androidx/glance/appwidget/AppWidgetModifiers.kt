/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.glance.GlanceModifier

/**
 * An internal AppWidget specific modifier that hints the item should try and clip its content to
 * the outline of its background or rounded corners.
 */
internal fun GlanceModifier.clipToOutline(clip: Boolean): GlanceModifier =
    this.then(ClipToOutlineModifier(clip))
internal data class ClipToOutlineModifier(val clip: Boolean) : GlanceModifier.Element

/**
 * An internal to AppWidget specific modifier used to specify that the item should be "enabled" in
 * the Android View meaning of enabled.
 */
internal fun GlanceModifier.enabled(enabled: Boolean) = this.then(EnabledModifier(enabled))
internal data class EnabledModifier(val enabled: Boolean) : GlanceModifier.Element
