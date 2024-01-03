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

package androidx.viewpager2.widget.swipe

import android.view.View
import androidx.test.espresso.action.CoordinatesProvider

class TranslatedCoordinatesProvider(
    private val provider: CoordinatesProvider,
    private val dx: Float,
    private val dy: Float
) : CoordinatesProvider {
    override fun calculateCoordinates(view: View?): FloatArray {
        val coords = provider.calculateCoordinates(view)
        coords[0] += dx
        coords[1] += dy
        return coords
    }
}
