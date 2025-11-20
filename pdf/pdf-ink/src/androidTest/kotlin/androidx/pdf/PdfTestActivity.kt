/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.pdf

import android.app.Activity
import android.os.Bundle
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

/** Bare bones test helper [android.app.Activity] for integration tests */
open class PdfTestActivity : Activity() {

    lateinit var container: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        container = FrameLayout(this)
        setContentView(container)

        // With targetSdk of AndroidX = 35, UI is drawn beneath the top system bars,
        // which causes click interactions to be blocked and not being propagated
        // properly to PdfView. Hence we add padding to offset the PdfView so that it lies
        // below the system bars.
        ViewCompat.setOnApplyWindowInsetsListener(container) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = systemBars.top)
            insets
        }
        // disable enter animation.
        @Suppress("Deprecation") overridePendingTransition(0, 0)
    }

    override fun finish() {
        super.finish()
        // disable exit animation.
        @Suppress("Deprecation") overridePendingTransition(0, 0)
    }
}
