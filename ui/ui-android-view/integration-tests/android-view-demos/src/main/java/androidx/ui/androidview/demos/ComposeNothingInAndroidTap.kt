/*
 * Copyright 2020 The Android Open Source Project
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
package androidx.ui.androidview.demos

import android.os.Bundle
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.compose.Composition
import androidx.compose.Recomposer
import androidx.ui.androidview.adapters.setOnClick
import androidx.ui.core.Modifier
import androidx.ui.core.setContent
import androidx.ui.foundation.Box
import androidx.ui.foundation.drawBackground
import androidx.ui.graphics.Color
import androidx.ui.graphics.RectangleShape
import androidx.ui.graphics.toArgb
import androidx.ui.layout.fillMaxSize

open class ComposeNothingInAndroidTap : ComponentActivity() {

    private var currentColor = Color.DarkGray

    private lateinit var composition: Composition

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.compose_in_android_tap)

        findViewById<TextView>(R.id.text1).text =
            "Intended to Demonstrate that when no gestureFilterModifiers are added to compose, " +
                    "Compose will not interact with the pointer input stream. This currently " +
                    "isn't actually the case however. "

        findViewById<TextView>(R.id.text2).text =
            "When you tap anywhere within the bounds of the colored, including the grey box in " +
                    "the middle, the color is supposed to change.  This currently does not occur " +
                    "when you tap on the grey box however."

        val container = findViewById<ViewGroup>(R.id.clickableContainer)
        container.isClickable = true
        container.setBackgroundColor(currentColor.toArgb())
        container.setOnClick {
            currentColor = if (currentColor == Color.Green) {
                Color.Red
            } else {
                Color.Green
            }
            container.setBackgroundColor(currentColor.toArgb())
        }
        composition = container.setContent(Recomposer.current()) {
            Box(Modifier.drawBackground(Color.LightGray, RectangleShape).fillMaxSize())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        composition.dispose()
    }
}