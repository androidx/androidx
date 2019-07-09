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

package androidx.ui.material.demos

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.ui.androidview.adapters.dp
import androidx.ui.androidview.adapters.setPadding
import androidx.compose.Composable
import androidx.compose.composer
import androidx.compose.setContent

class RippleActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            val gravity = Gravity.CENTER_HORIZONTAL
            LinearLayout(orientation = LinearLayout.VERTICAL) {
                TextView(gravity = gravity, text = "Crane card with ripple:")
                FrameLayout(layoutParams = layoutParams) {
                    RippleDemo()
                }
                TextView(gravity = gravity, text = "Platform button with ripple:")
                FrameLayout(layoutParams = layoutParams, padding = 50.dp) {
                    Button(background = getDrawable(R.drawable.ripple))
                }
            }
        }
    }
}
