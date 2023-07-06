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

import androidx.compose.runtime.Composable
import androidx.glance.Emittable
import androidx.glance.EmittableWithChildren
import androidx.glance.GlanceComposable
import androidx.glance.GlanceModifier
import androidx.glance.GlanceNode

/**
 * This composable is a marker used by [AppWidgetSession] to indicate that the result of a
 * composition should be ignored.
 *
 * If this composable is present in the [Emittable] tree, [AppWidgetSession] will not translate the
 * tree and update the app widget.
 */
@Composable
@GlanceComposable
internal fun IgnoreResult() {
    GlanceNode(::EmittableIgnoreResult) {}
}

internal class EmittableIgnoreResult : Emittable {
    override var modifier: GlanceModifier = GlanceModifier
    override fun copy() = EmittableIgnoreResult().also {
        it.modifier = modifier
    }
}

/**
 * Returns true if this [Emittable] is an [EmittableIgnoreResult] or contains an
 * [EmittableIgnoreResult] in its children.
 */
internal fun Emittable.shouldIgnoreResult(): Boolean {
    if (this is EmittableIgnoreResult) {
        return true
    } else if (this is EmittableWithChildren) {
        if (children.any { it.shouldIgnoreResult() }) return true
    }
    return false
}
