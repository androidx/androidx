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
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.compose.Composition
import androidx.compose.Recomposer
import androidx.compose.state
import androidx.ui.core.Modifier
import androidx.ui.core.gesture.tapGestureFilter
import androidx.ui.core.setContent
import androidx.ui.foundation.Box
import androidx.ui.foundation.drawBackground
import androidx.ui.graphics.Color
import androidx.ui.graphics.RectangleShape
import androidx.ui.graphics.toArgb
import androidx.ui.layout.fillMaxWidth
import androidx.ui.layout.preferredHeight
import androidx.ui.layout.preferredSize
import androidx.ui.layout.wrapContentSize
import androidx.ui.material.ripple.ripple
import androidx.ui.unit.dp

open class ComposeTapInAndroidScroll : ComponentActivity() {

    private lateinit var composition: Composition

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.compose_in_android_scroll)

        findViewById<View>(R.id.container).setBackgroundColor(Color.DarkGray.toArgb())

        findViewById<TextView>(R.id.text1).text =
            "Demonstrates that press gestures and movement gestures interact correctly between " +
                    "Android and Compose when Compose is inside of Android."

        findViewById<TextView>(R.id.text2).text =
            "The inner box is Compose, the rest is Android.  Tapping the inner box will change " +
                    "it's color.  Putting a finger down on the inner box and dragging vertically," +
                    " will cause the outer Android ScrollView to scroll and removing the finger " +
                    "from the screen will not cause the Compose box to change colors. "

        val container = findViewById<ViewGroup>(R.id.container)
        composition = container.setContent(Recomposer.current()) {

            val currentColor = state { Color.LightGray }

            val tap =
                Modifier.tapGestureFilter {
                    currentColor.value =
                        if (currentColor.value == Color.Blue) Color.Yellow else Color.Blue
                }

            Box(
                Modifier
                    .drawBackground(Color.Gray, RectangleShape)
                    .fillMaxWidth()
                    .preferredHeight(456.dp)
                    .wrapContentSize()
                    .ripple()
                    .plus(tap)
                    .drawBackground(currentColor.value, RectangleShape)
                    .preferredSize(192.dp)
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        composition.dispose()
    }
}