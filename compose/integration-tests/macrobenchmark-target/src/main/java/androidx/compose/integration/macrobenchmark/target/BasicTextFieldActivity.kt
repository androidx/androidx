/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.integration.macrobenchmark.target

import android.R
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentDataType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.contentDataType
import androidx.compose.ui.semantics.semantics
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat

class BasicTextFieldActivity : ComponentActivity() {

    private var didRequestFocus = false
    private var isImeAnimating = false
    private var contentView: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        contentView =
            requireNotNull(findViewById(R.id.content)) { "No view with R.id.content was found." }
        // We use the content view to report the IME animation state to the benchmark.
        // importantForAccessibility because we communicate with UiAutomator through content desc.
        contentView!!.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES

        ViewCompat.setWindowInsetsAnimationCallback(contentView!!, imeAnimationCallback)

        setContent {
            val focusRequester = remember { FocusRequester() }

            // Compose handles the padding natively entirely in the Layout phase
            Box(Modifier.windowInsetsPadding(WindowInsets.safeContent)) {
                val textFieldState = rememberTextFieldState()
                BasicTextField(
                    state = textFieldState,
                    modifier =
                        Modifier.focusRequester(focusRequester).semantics {
                            if (intent.getStringExtra("CONTENT_TYPE") == "NONE") {
                                contentDataType = ContentDataType.None
                            }
                        },
                )
            }

            DisposableEffect(focusRequester) {
                focusRequester.requestFocus()
                didRequestFocus = true
                onDispose { didRequestFocus = false }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        contentView = null
    }

    // We track the IME state at the View level to not muddy the Compose contents of this benchmark
    private val imeAnimationCallback =
        object : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_CONTINUE_ON_SUBTREE) {
            override fun onPrepare(animation: WindowInsetsAnimationCompat) {
                if ((animation.typeMask and WindowInsetsCompat.Type.ime()) != 0) {
                    isImeAnimating = true
                }
                super.onPrepare(animation)
            }

            override fun onProgress(
                insets: WindowInsetsCompat,
                runningAnimations: MutableList<WindowInsetsAnimationCompat>,
            ): WindowInsetsCompat {
                return insets // Pass through unmodified
            }

            override fun onEnd(animation: WindowInsetsAnimationCompat) {
                if ((animation.typeMask and WindowInsetsCompat.Type.ime()) != 0) {
                    if (didRequestFocus && isImeAnimating) {
                        reportFullyDrawn()
                        contentView?.contentDescription = "IME_ANIMATION_DONE"
                    }
                    isImeAnimating = false
                }
                super.onEnd(animation)
            }
        }
}
