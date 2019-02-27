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

package androidx.ui.material.clip

import androidx.ui.core.Density
import androidx.ui.core.PxSize

/**
 * An interface for providing custom clips.
 *
 * The [getClip] method is called whenever the custom clip needs to be updated.
 *
 * See also:
 *
 * TODO("Andrey: provide this extra clip composables")
 *  * [ClipRect], which can be customized with a [CustomClipper].
 *  * [ClipRRect], which can be customized with a [CustomClipper].
 *  * [ClipOval], which can be customized with a [CustomClipper].
 *  * [ClipPath], which can be customized with a [CustomClipper].
 */
interface CustomClipper<T> {

    /**
     * Returns a description of the clip given that the layout being
     * clipped is of the given size.
     */
    fun getClip(size: PxSize, density: Density): T
}
