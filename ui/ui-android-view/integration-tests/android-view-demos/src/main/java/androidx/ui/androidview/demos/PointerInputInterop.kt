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

import android.view.View
import android.view.ViewGroup
import androidx.compose.Composable
import androidx.ui.androidview.adapters.setOnClick
import androidx.ui.core.Alignment
import androidx.ui.core.Modifier
import androidx.ui.core.gesture.tapGestureFilter
import androidx.ui.demos.common.ActivityDemo
import androidx.ui.demos.common.ComposableDemo
import androidx.ui.demos.common.DemoCategory
import androidx.ui.foundation.Box
import androidx.ui.foundation.HorizontalScroller
import androidx.ui.foundation.Text
import androidx.ui.layout.Column
import androidx.ui.layout.fillMaxSize
import androidx.ui.layout.preferredSize
import androidx.ui.layout.wrapContentSize
import androidx.ui.unit.dp
import androidx.ui.viewinterop.AndroidView

val ComposeInAndroidDemos = DemoCategory(
    "Compose in Android Interop", listOf(
        ActivityDemo(
            "Compose with no gestures in Android tap",
            ComposeNothingInAndroidTap::class
        ),
        ActivityDemo(
            "Compose tap in Android tap",
            ComposeTapInAndroidTap::class
        ),
        ActivityDemo(
            "Compose tap in Android scroll",
            ComposeTapInAndroidScroll::class
        ),
        ActivityDemo(
            "Compose scroll in Android scroll (same orientation)",
            ComposeScrollInAndroidScrollSameOrientation::class
        ),
        ActivityDemo(
            "Compose scroll in Android scroll (different orientations)",
            ComposeScrollInAndroidScrollDifferentOrientation::class
        )
    )
)

val AndroidInComposeDemos = DemoCategory("Android In Compose Interop", listOf(
    ComposableDemo("4 Android tap in Compose") { FourAndroidTapInCompose() },
    ComposableDemo("Android tap in Compose tap") { AndroidTapInComposeTap() },
    ComposableDemo("Android tap in Compose scroll") { AndroidTapInComposeScroll() }
))

@Composable
private fun FourAndroidTapInCompose() {
    Column {
        Text("Demonstrates that pointer locations are dispatched to Android correctly.")
        Text(
            "Below is a ViewGroup with 4 Android buttons in it.  When each button is tapped, the" +
                    " background of the ViewGroup is updated."
        )
        Box(
            Modifier
                .fillMaxSize()
                .wrapContentSize(Alignment.Center)
                .preferredSize(240.dp)
        ) {
            AndroidView(R.layout.pointer_interop_targeting_demo) { view ->
                view as ViewGroup
                view.findViewById<View>(R.id.buttonBlue).setOnClick {
                    view.setBackgroundColor(android.graphics.Color.BLUE)
                }
                view.findViewById<View>(R.id.buttonRed).setOnClick {
                    view.setBackgroundColor(android.graphics.Color.RED)
                }
                view.findViewById<View>(R.id.buttonGreen).setOnClick {
                    view.setBackgroundColor(android.graphics.Color.GREEN)
                }
                view.findViewById<View>(R.id.buttonYellow).setOnClick {
                    view.setBackgroundColor(android.graphics.Color.YELLOW)
                }
            }
        }
    }
}

@Composable
private fun AndroidTapInComposeTap() {
    var theView: View? = null

    val onTap: () -> Unit = {
        theView?.setBackgroundColor(android.graphics.Color.BLUE)
    }

    Column {
        Text(
            "Demonstrates that pointer input interop is working correctly in the simple case of " +
                    "tapping."
        )
        Text(
            "Below there is an Android ViewGroup with a button in it.  The whole thing is wrapped" +
                    " in a Box with a tapGestureFilter modifier on it.  When you click the " +
                    "button, the ViewGroup's background turns red.  When you click anywhere else " +
                    "in the ViewGroup, the tapGestureFilter \"fires\" and the background turns " +
                    "Blue."
        )
        Box(
            Modifier
                .tapGestureFilter(onTap)
                .fillMaxSize()
                .wrapContentSize(Alignment.Center)
                .preferredSize(240.dp)
        ) {
            AndroidView(R.layout.pointer_interop_tap_in_tap_demo) { view ->
                theView = view
                theView?.setBackgroundColor(android.graphics.Color.GREEN)
                view.findViewById<View>(R.id.buttonRed).setOnClick {
                    theView?.setBackgroundColor(android.graphics.Color.RED)
                }
            }
        }
    }
}

@Composable
private fun AndroidTapInComposeScroll() {
    Column {
        Text(
            "Demonstrates that pointer input interop is working correctly when tappable things in" +
                    " Android are put inside of something scrollable in Compose."
        )
        Text(
            "Below is a Compose HorizontalScroller with a wide horizontal LinearLayout in it, " +
                    "that is comprised of 4 buttons.  Clicking buttons changes the LinearLayout's" +
                    " background color.  When you drag horizontally, the HorizontalScroller drags" +
                    ". If a pointer starts on a button and then drags horizontally, the button " +
                    "will not be clicked when released."
        )
        HorizontalScroller {
            AndroidView(R.layout.pointer_interop_tap_in_drag_demo) { view ->
                view.setBackgroundColor(android.graphics.Color.YELLOW)
                view.findViewById<View>(R.id.buttonRed).apply {
                    isClickable = false
                    setOnClick {
                        view.setBackgroundColor(android.graphics.Color.RED)
                    }
                }
                view.findViewById<View>(R.id.buttonGreen).apply {
                    isClickable = false
                    setOnClick {
                        view.setBackgroundColor(android.graphics.Color.GREEN)
                    }
                }
                view.findViewById<View>(R.id.buttonBlue).apply {
                    isClickable = false
                    setOnClick {
                        view.setBackgroundColor(android.graphics.Color.BLUE)
                    }
                }
                view.findViewById<View>(R.id.buttonYellow).apply {
                    isClickable = false
                    setOnClick {
                        view.setBackgroundColor(android.graphics.Color.YELLOW)
                    }
                }
            }
        }
    }
}