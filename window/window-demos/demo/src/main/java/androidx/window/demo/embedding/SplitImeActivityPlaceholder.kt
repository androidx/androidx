/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.window.demo.embedding

import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
import android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
import android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE

/** The placeholder activity that will be launched when there is enough space. */
class SplitImeActivityPlaceholder : SplitImeActivityBase() {

    private var imeLayeringTargetPlaceholder: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding.rootSplitActivityLayout.setBackgroundColor(Color.parseColor("#fff3e0"))

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.TIRAMISU) {
            // CAVEATS:
            // Before Android U, IME may have issue to be shown in the primary split.
            // A workaround is to add a child window to the secondary split, and set its window
            // attribute as "FLAG_NOT_FOCUSABLE or FLAG_ALT_FOCUSABLE_IM".
            val attrs = WindowManager.LayoutParams()
            // Make it transparent and not touchable to not affect the views below.
            attrs.format = PixelFormat.TRANSPARENT
            attrs.flags = FLAG_NOT_FOCUSABLE or FLAG_ALT_FOCUSABLE_IM or FLAG_NOT_TOUCHABLE
            attrs.fitInsetsTypes = WindowInsets.Type.statusBars()
            imeLayeringTargetPlaceholder = View(this)
            windowManager.addView(imeLayeringTargetPlaceholder, attrs)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (imeLayeringTargetPlaceholder != null) {
            windowManager.removeView(imeLayeringTargetPlaceholder)
        }
    }
}
