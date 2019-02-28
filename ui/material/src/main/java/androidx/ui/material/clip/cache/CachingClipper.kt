/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.material.clip.cache

import androidx.ui.core.Density
import androidx.ui.core.PxSize
import androidx.ui.material.clip.CustomClipper
import androidx.ui.painting.Path
import com.google.r4a.Children
import com.google.r4a.Component
import com.google.r4a.composer

/**
 * A Composable to help cache the clip object and not execute [CustomClipper.getClip]
 * when the parent size didn't change. It helps to save extra allocations of the
 * complex objects like [Path].
 */
class CachingClipper<T>(
    var clipper: CustomClipper<T>,
    @Children var children: (clipper: CustomClipper<T>) -> Unit
) : Component() {

    internal var lastClipper: CustomClipper<T> = clipper
    internal var lastSize: PxSize? = null
    internal var lastClip: T? = null

    private val cachingClipper = object : CustomClipper<T> {

        /**
         * Returns a clip.
         *
         * It will recreate a clip only when clipper or parent size have been changed,
         * otherwise will return the cached object.
         */
        override fun getClip(size: PxSize, density: Density): T {
            if (lastClipper != clipper) {
                lastClip = null
            }
            if (size != lastSize) {
                lastClip = null
                lastSize = size
            }
            val clip = lastClip ?: clipper.getClip(size, density)
            lastClip = clip
            return clip
        }
    }

    override fun compose() {
        <children clipper=cachingClipper />
    }
}
