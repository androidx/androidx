/*
 * Copyright 2018 The Android Open Source Project
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
package androidx.ui.text.matchers

import android.graphics.Bitmap
import org.hamcrest.BaseMatcher
import org.hamcrest.Description

/**
 * Checks equality of two bitmaps.
 */
class IsEqualBitmap(
    private val bitmap: Bitmap
) : BaseMatcher<Bitmap>() {

    override fun matches(item: Any?): Boolean {
        if (item !is Bitmap) {
            return false
        }
        if (item == bitmap) {
            return true
        }
        return bitmap.sameAs(item)
    }

    override fun describeTo(description: Description?) {
        description?.appendText("${describeBitmap(bitmap)}")
    }

    fun describeBitmap(bitmap: Bitmap): String {
        return "($bitmap ${bitmap.getWidth()}x${bitmap.getHeight()} ${bitmap.config.name})"
    }
}