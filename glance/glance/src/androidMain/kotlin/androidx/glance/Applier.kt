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
import androidx.compose.runtime.AbstractApplier
import java.lang.IllegalStateException

/**
 * Applier for the Glance composition.
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class Applier(root: EmittableWithChildren) : AbstractApplier<Emittable>(root) {
    private val newRootMaxDepth = root.maxDepth

    override fun onClear() {
        (root as EmittableWithChildren).children.clear()
    }

    override fun insertBottomUp(index: Int, instance: Emittable) {
        // Ignored, the tree is built top-down.
    }

    override fun insertTopDown(index: Int, instance: Emittable) {
        val parent = current as EmittableWithChildren
        require(parent.maxDepth > 0) {
            "Too many embedded views for the current surface. The maximum depth is: " +
                "${(root as EmittableWithChildren).maxDepth}"
        }
        if (instance is EmittableWithChildren) {
            instance.maxDepth = if (instance.resetsDepthForChildren) {
                newRootMaxDepth
            } else {
                parent.maxDepth - 1
            }
        }
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
