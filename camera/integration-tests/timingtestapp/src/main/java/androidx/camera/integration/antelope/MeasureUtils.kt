/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.camera.integration.antelope

import android.util.Log
import android.util.Size
import java.util.Collections
import kotlin.collections.ArrayList

/**
 * Compares two `Size`s based on their areas.
 */
internal class CompareSizesByArea : Comparator<Size> {
    override fun compare(lhs: Size, rhs: Size): Int {
        // We cast here to ensure the multiplications won't overflow
        return java.lang.Long.signum(lhs.width.toLong() * lhs.height -
            rhs.width.toLong() * rhs.height)
    }
}

/**
 * Given `choices` of `Size`s supported by a camera, chooses the smallest one whose
 * width and height are at least as large as the respective requested values.
 * @param choices The list of sizes that the camera supports for the intended output class
 * @param width The minimum desired width
 * @param height The minimum desired height
 * @return The optimal `Size`, or an arbitrary one if none were big enough
 */
internal fun chooseBigEnoughSize(choices: Array<Size>, width: Int, height: Int): Size {
    // Collect the supported resolutions that are at least as big as the preview Surface
    val bigEnough = ArrayList<Size>()
    for (option in choices) {
        if (option.width >= width && option.height >= height) {
            bigEnough.add(option)
        }
    }
    // Pick the smallest of those, assuming we found any
    if (bigEnough.size > 0) {
        return Collections.min(bigEnough, CompareSizesByArea())
    } else {
        Log.e(MainActivity.LOG_TAG, "Couldn't find any suitable preview size")
        return choices[0]
    }
}