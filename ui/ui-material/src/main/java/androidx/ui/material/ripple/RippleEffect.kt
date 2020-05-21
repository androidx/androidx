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

package androidx.ui.material.ripple

import androidx.ui.graphics.Color
import androidx.ui.graphics.drawscope.DrawScope

/**
 * [RippleEffect]s are drawn as part of [ripple] as a visual indicator for a pressed state.
 *
 * Use [ripple] to add an animation for your component.
 */
interface RippleEffect {

    /**
     * Override this method to draw the ripple.
     *
     * @param color The [Color] for this [RippleEffect].
     */
    fun DrawScope.draw(color: Color)

    /**
     * Called when the user input that triggered this effect was confirmed or canceled.
     *
     * Typically causes the ripple to start disappearance animation.
     */
    fun finish(canceled: Boolean)

    /**
     * Free up the resources associated with this ripple.
     */
    fun dispose() {}
}