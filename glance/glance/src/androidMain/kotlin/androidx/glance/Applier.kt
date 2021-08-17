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

import androidx.compose.runtime.AbstractApplier
import java.lang.IllegalStateException

/** Applier for the Glance composition. */
@GlanceInternalApi
public class Applier(root: EmittableWithChildren) : AbstractApplier<Emittable>(root) {
    override fun onClear() {
        (root as EmittableWithChildren).children.clear()
    }

    override fun insertBottomUp(index: Int, instance: Emittable) {
        // Ignored, the tree is built top-down.
    }

    override fun insertTopDown(index: Int, instance: Emittable) {
        currentChildren.add(index, instance)
    }

    override fun move(from: Int, to: Int, count: Int) {
        currentChildren.move(from, to, count)
    }

    override fun remove(index: Int, count: Int) {
        currentChildren.remove(index, count)
    }

    private val currentChildren: MutableList<Emittable>
        get() {
            current.let {
                if (it is EmittableWithChildren) {
                    return it.children
                }
            }
            throw IllegalStateException("Current node cannot accept children")
        }
}
