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

package androidx.wear.compose.prototypes.windowinsets

import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.wear.compose.material3.MaterialTheme

class WindowInsetsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Diagnostic: monitor DecorView mutation of mContentRoot LayoutParams topMargin
        window.decorView.post {
            (window.decorView as? ViewGroup)?.getChildAt(0)?.let { contentRoot ->
                contentRoot.addOnLayoutChangeListener { v, _, top, _, _, _, oldTop, _, _ ->
                    val lp = v.layoutParams as? ViewGroup.MarginLayoutParams
                    Log.d(
                        "WearInsetsDebug",
                        "DecorView mContentRoot topMargin=${lp?.topMargin} | viewTop=$top (was $oldTop)",
                    )
                }
            }
        }

        setContent { MaterialTheme { WindowInsetsApp() } }
    }
}
