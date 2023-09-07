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

package androidx.glance

import androidx.annotation.RestrictTo
import androidx.glance.layout.Alignment
import androidx.glance.text.TextStyle

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface Emittable {
    var modifier: GlanceModifier
    fun copy(): Emittable
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
abstract class EmittableWithChildren(
    internal var maxDepth: Int = Int.MAX_VALUE,
    internal val resetsDepthForChildren: Boolean = false
) : Emittable {
    val children: MutableList<Emittable> = mutableListOf<Emittable>()

    protected fun childrenToString(): String =
        children.joinToString(",\n").prependIndent("  ")
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun EmittableWithChildren.addChild(e: Emittable) {
    this.children += e
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun EmittableWithChildren.addChildIfNotNull(e: Emittable?) {
    if (e != null) this.children += e
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
abstract class EmittableLazyItemWithChildren : EmittableWithChildren() {
    var alignment: Alignment = Alignment.CenterStart
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
abstract class EmittableWithText : Emittable {
    var text: String = ""
    var style: TextStyle? = null
    var maxLines: Int = Int.MAX_VALUE
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
abstract class EmittableCheckable : EmittableWithText() {
    var checked: Boolean = false
}
