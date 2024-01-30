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

package androidx.glance.layout

import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.glance.Emittable
import androidx.glance.GlanceModifier
import androidx.glance.GlanceNode

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class EmittableSpacer : Emittable {
    override var modifier: GlanceModifier = GlanceModifier

    override fun copy(): Emittable = EmittableSpacer().also {
        it.modifier = modifier
    }

    override fun toString(): String = "EmittableSpacer(modifier=$modifier)"
}

/**
 * Component that represents an empty space layout, whose size can be defined using
 * [GlanceModifier.width], [GlanceModifier.height], [GlanceModifier.size] modifiers.
 *
 * @param modifier Modifiers to set to this spacer
 */
@Composable
fun Spacer(modifier: GlanceModifier = GlanceModifier) {
    GlanceNode(
        factory = :: EmittableSpacer,
        update = {
          this.set(modifier) { this.modifier = it }
        }
    )
}
