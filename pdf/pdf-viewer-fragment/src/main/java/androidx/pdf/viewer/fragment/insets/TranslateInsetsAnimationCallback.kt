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

package androidx.pdf.viewer.fragment.insets

import android.view.View
import android.view.WindowManager
import androidx.core.graphics.Insets
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat

/**
 * A callback that will update bottom margin for provided view as per keyboard visibility.
 *
 * @param view: A view whose bottom margin needs to be updated with keyboard.
 * @param windowManager: An interface to interact with window params, here helps to fetch screen
 *   height.
 * @param pdfContainer: Container view where PDF is hosted.
 * @param dispatchMode: Specifies whether children view should get callback, by default callback
 *   will be propagated.
 */
internal class TranslateInsetsAnimationCallback(
    private val view: View,
    private val windowManager: WindowManager?,
    private val pdfContainer: View?,
    dispatchMode: Int = DISPATCH_MODE_CONTINUE_ON_SUBTREE
) : WindowInsetsAnimationCompat.Callback(dispatchMode) {

    init {
        view.setOnApplyWindowInsetsListener { _, insets ->
            val keyboardInsets =
                insets.getInsets(WindowInsetsCompat.Type.ime()).run {
                    Insets.of(left, top, right, bottom)
                }
            // Avoid consuming null ime events which are set as zero on configuration changes.
            if (keyboardInsets.bottom > 0) {
                translateViewWithKeyboard(keyboardInsets)
            }

            insets
        }
    }

    override fun onProgress(
        insets: WindowInsetsCompat,
        runningAnimations: List<WindowInsetsAnimationCompat>
    ): WindowInsetsCompat {
        // onProgress() is called when any of the running animations progress...

        translateViewWithKeyboard(insets.getInsets(WindowInsetsCompat.Type.ime()))
        return insets
    }

    private fun translateViewWithKeyboard(keyboardInsets: Insets) {

        var absoluteContainerBottom = 0
        /*
        Calculate absolute pdfContainer bottom on screen
        This is necessary as our fragment may not span the complete screen
         */
        pdfContainer?.let {
            val containerLocation = IntArray(2)
            pdfContainer.getLocationInWindow(containerLocation)
            absoluteContainerBottom = pdfContainer.height + containerLocation[1]
        }

        // Extract keyboard insets

        /*
        By default the keyboard top should be aligned with container bottom;
        This is same as keyboard is in closed state
         */
        var keyboardTop = absoluteContainerBottom

        // Calculate keyboard top wrt screen height
        windowManager?.let {
            val screenHeight = windowManager.currentWindowMetrics.bounds.height()
            keyboardTop = screenHeight - keyboardInsets.bottom
        }

        // Calculate the required translationY value for the view
        val translationY =
            if (absoluteContainerBottom >= keyboardTop)
                (keyboardTop - absoluteContainerBottom).toFloat()
            else 0f

        // Apply the translationY to the view
        view.translationY = translationY
    }
}
