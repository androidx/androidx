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

package androidx.glance.adaptive.appwidget.components.icon

import androidx.glance.adaptive.core.slots.Icon

/** Design values of the [Icon] component that every backend draws it with. */
internal object IconDefaults {

    /** Edge length of a contained icon's glyph, as a fraction of its circle: 30 dp in 56 dp. */
    const val ContainedGlyphRatio: Float = 30f / 56f
}
