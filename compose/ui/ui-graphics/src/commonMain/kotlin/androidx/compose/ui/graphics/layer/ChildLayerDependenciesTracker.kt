/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui.graphics.layer

import androidx.collection.MutableScatterSet
import androidx.collection.mutableScatterSetOf
import androidx.compose.ui.graphics.requirePrecondition

/**
 * Helps [GraphicsLayer] implementations to keep track of child layers those layers currently draw.
 */
internal class ChildLayerDependenciesTracker {

    // we will use next two variables when we have 0 or 1 dependencies
    private var dependency: GraphicsLayer? = null
    private var oldDependency: GraphicsLayer? = null
    // otherwise we will allocate a set if we have 2 or more dependencies
    private var dependenciesSet: MutableScatterSet<GraphicsLayer>? = null
    private var oldDependenciesSet: MutableScatterSet<GraphicsLayer>? = null

    private var trackingInProgress = false

    /**
     * Rerun the tracking. it will remember what dependencies we had during the previous run, then
     * will track what dependencies are added via [onDependencyAdded] during this run, compare them
     * and invoke [onDependencyRemoved] on dependencies which were removed.
     */
    inline fun withTracking(onDependencyRemoved: (GraphicsLayer) -> Unit, block: () -> Unit) {
        // move current dependencies to old dependencies
        oldDependency = dependency
        dependenciesSet?.let { currentSet ->
            if (currentSet.isNotEmpty()) {
                val oldSet =
                    oldDependenciesSet
                        ?: mutableScatterSetOf<GraphicsLayer>().also { oldDependenciesSet = it }
                oldSet.addAll(currentSet)
                currentSet.clear()
            }
        }
        trackingInProgress = true
        block()
        trackingInProgress = false

        // invoke [onDependencyRemoved] on dependencies which we had during the previous run,
        // and which wasn't added via [onDependencyAdded] during this run.
        oldDependency?.let(onDependencyRemoved)
        oldDependenciesSet?.let { oldSet ->
            if (oldSet.isNotEmpty()) {
                oldSet.forEach(onDependencyRemoved)
                oldSet.clear()
            }
        }
    }

    /** @return true if this dependency is new (wasn't added during the last tracking) */
    fun onDependencyAdded(graphicsLayer: GraphicsLayer): Boolean {
        requirePrecondition(trackingInProgress) { "Only add dependencies during a tracking" }

        // add a new dependency:
        if (dependenciesSet != null) {
            dependenciesSet!!.add(graphicsLayer)
        } else if (dependency != null) {
            dependenciesSet =
                mutableScatterSetOf<GraphicsLayer>().also {
                    it.add(dependency!!)
                    it.add(graphicsLayer)
                }
            dependency = null
        } else {
            dependency = graphicsLayer
        }

        // check if we had this dependency during the previous run:
        if (oldDependenciesSet != null) {
            // return true if we didn't have this layer in the old set
            return !oldDependenciesSet!!.remove(graphicsLayer)
        } else {
            if (oldDependency !== graphicsLayer) {
                return true // the dependency is new
            } else {
                // we had this dependency in the previous run
                oldDependency = null
                return false
            }
        }
    }

    /** [block] will be executed for each dependency before removing it. */
    inline fun removeDependencies(block: (GraphicsLayer) -> Unit) {
        dependency?.let {
            block(it)
            dependency = null
        }
        dependenciesSet?.let {
            it.forEach(block)
            it.clear()
        }
    }
}
