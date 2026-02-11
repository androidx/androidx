/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.ui.semantics

import androidx.compose.ui.unit.IntRect
import org.jetbrains.skia.IRect
import org.jetbrains.skia.Region

private class SemanticRegionImpl : SemanticsRegion {
    val region = Region()

    override fun set(rect: IntRect) {
        region.setRect(rect.left, rect.top, rect.right, rect.bottom)
    }

    override val bounds: IntRect
        get() = region.bounds.let {
            IntRect(it.left, it.top, it.right, it.bottom)
        }

    override val isEmpty: Boolean
        get() = region.isEmpty

    override fun intersect(region: SemanticsRegion): Boolean {
        return this.region.op((region as SemanticRegionImpl).region, Region.Op.INTERSECT)
    }

    override fun difference(rect: IntRect): Boolean {
        return region.op(rect.left, rect.top, rect.right, rect.bottom, Region.Op.DIFFERENCE)
    }
}

internal actual fun SemanticsRegion(): SemanticsRegion = SemanticRegionImpl()
