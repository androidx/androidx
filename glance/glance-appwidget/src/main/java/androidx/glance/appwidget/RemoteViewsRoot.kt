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

import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope
import androidx.glance.Emittable
import androidx.glance.EmittableWithChildren
import androidx.glance.GlanceModifier

/**
 * Root view, with a maximum depth. No default value is specified, as the exact value depends on
 * specific circumstances.
 */
@RestrictTo(Scope.LIBRARY_GROUP)
 class RemoteViewsRoot(private val maxDepth: Int) : EmittableWithChildren(maxDepth) {
    override var modifier: GlanceModifier = GlanceModifier
    override fun copy(): Emittable = RemoteViewsRoot(maxDepth).also {
        it.modifier = modifier
        it.children.addAll(children.map { it.copy() })
    }

    override fun toString(): String = "RemoteViewsRoot(" +
        "modifier=$modifier, " +
        "children=[\n${childrenToString()}\n]" +
        ")"
}
