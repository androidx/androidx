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

package androidx.compose.ui.demos.viewinterop

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.integration.demos.common.ComposableDemo
import androidx.compose.integration.demos.common.DemoCategory
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.demos.R
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputFilter
import androidx.compose.ui.input.pointer.PointerInputModifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

val AndroidInComposeDemos =
    DemoCategory(
        "Android In Compose Interop",
        listOf(
            ComposableDemo("4 Android tap in Compose") { FourAndroidTapInCompose() },
            ComposableDemo("Android tap in Compose tap") { AndroidTapInComposeTap() },
            ComposableDemo("Android tap in Compose scroll") { AndroidTapInComposeScroll() },
            ComposableDemo("Android scroll in Compose scroll (different orientation)") {
                AndroidScrollInComposeScrollDifferentOrientation()
            },
            ComposableDemo("Android scroll in Compose scroll (same orientation)") {
                AndroidScrollInComposeScrollSameOrientation()
            },
            ComposableDemo("2 ScrollViews as separate children of Compose") {
                TwoAndroidScrollViewsInCompose()
            },
            ComposableDemo("MotionEventPointerInputFilter") { PointerInteropFilterDemo() },
            ComposableDemo("Sharing event with sibling of parent Demo") {
                SharingEventWithSiblingOfParent()
            },
        )
    )

@Composable
private fun FourAndroidTapInCompose() {
    Column {
        Text("Demonstrates that pointer locations are dispatched to Android correctly.")
        Text(
            "Below is a ViewGroup with 4 Android buttons in it.  When each button is tapped, the" +
                " background of the ViewGroup is updated."
        )
        Box(Modifier.fillMaxSize().wrapContentSize(Alignment.Center).size(240.dp)) {
            AndroidView({ context ->
                LayoutInflater.from(context)
                    .inflate(R.layout.android_4_buttons_in_compose, null)
                    .let { view ->
                        view as ViewGroup
                        view.findViewById<View>(R.id.buttonBlue).setOnClickListener {
                            view.setBackgroundColor(Color.BLUE)
                        }
                        view.findViewById<View>(R.id.buttonRed).setOnClickListener {
                            view.setBackgroundColor(Color.RED)
                        }
                        view.findViewById<View>(R.id.buttonGreen).setOnClickListener {
                            view.setBackgroundColor(Color.GREEN)
                        }
                        view.findViewById<View>(R.id.buttonYellow).setOnClickListener {
                            view.setBackgroundColor(Color.YELLOW)
                        }
                        view
                    }
            })
        }
    }
}

@Composable
private fun AndroidTapInComposeTap() {
    var theView: View? = null

    val onTap: (Offset) -> Unit = { theView?.setBackgroundColor(Color.BLUE) }

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
            Modifier.fillMaxSize().wrapContentSize(Alignment.Center).size(240.dp).pointerInput(
                Unit
            ) {
                detectTapGestures(onTap = onTap)
            }
        ) {
            AndroidView({ context ->
                LayoutInflater.from(context)
                    .inflate(R.layout.android_tap_in_compose_tap, null)
                    .let { view ->
                        theView = view
                        theView?.setBackgroundColor(Color.GREEN)
                        view.findViewById<View>(R.id.buttonRed).setOnClickListener {
                            theView?.setBackgroundColor(Color.RED)
                        }
                        view
                    }
            })
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
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            AndroidView({ context ->
                LayoutInflater.from(context)
                    .inflate(R.layout.android_tap_in_compose_scroll, null)
                    .let { view ->
                        view.setBackgroundColor(Color.YELLOW)
                        view.findViewById<View>(R.id.buttonRed).apply {
                            isClickable = false
                            setOnClickListener { view.setBackgroundColor(Color.RED) }
                        }
                        view.findViewById<View>(R.id.buttonGreen).apply {
                            isClickable = false
                            setOnClickListener { view.setBackgroundColor(Color.GREEN) }
                        }
                        view.findViewById<View>(R.id.buttonBlue).apply {
                            isClickable = false
                            setOnClickListener { view.setBackgroundColor(Color.BLUE) }
                        }
                        view.findViewById<View>(R.id.buttonYellow).apply {
                            isClickable = false
                            setOnClickListener { view.setBackgroundColor(Color.YELLOW) }
                        }
                        view
                    }
            })
        }
    }
}

@Composable
private fun AndroidScrollInComposeScrollDifferentOrientation() {
    Column {
        Text(
            "Demonstrates correct \"scroll orientation\" locking when something scrollable in " +
                "Android is nested inside something scrollable in Compose."
        )
        Text("You should only be able to scroll in one orientation at a time.")
        Row(
            Modifier.background(androidx.compose.ui.graphics.Color.Blue)
                .horizontalScroll(rememberScrollState())
        ) {
            Box(
                modifier =
                    Modifier.padding(96.dp).background(androidx.compose.ui.graphics.Color.Red)
            ) {
                AndroidView({ context ->
                    LayoutInflater.from(context)
                        .inflate(
                            R.layout.android_scroll_in_compose_scroll_different_orientation,
                            null
                        )
                })
            }
        }
    }
}

@Composable
private fun AndroidScrollInComposeScrollSameOrientation() {
    Column {
        Text(
            "Supposed to demonstrate correct nested scrolling when something scrollable in " +
                "Android is inside something scrollable in Compose."
        )
        Text(
            "This doesn't actually work because nested scrolling isn't implemented between " +
                "Compose and Android.  Normally, this lack of implementation would mean the " +
                "parent would always intercept first and thus block the child from ever " +
                "scrolling. However, currently, the touch slop for Android is smaller than " +
                "that for Compose, and thus the child scrolls and prevents the parent from " +
                "intercepting. "
        )
        Column(
            Modifier.background(androidx.compose.ui.graphics.Color.Blue)
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier =
                    Modifier.padding(96.dp)
                        .background(color = androidx.compose.ui.graphics.Color.Red)
                        .height(750.dp)
            ) {
                AndroidView({ context ->
                    LayoutInflater.from(context)
                        .inflate(R.layout.android_scroll_in_compose_scroll_same_orientation, null)
                })
            }
        }
    }
}

@Composable
private fun TwoAndroidScrollViewsInCompose() {
    Column {
        Text(
            "Below are two Android Scrollviews that are nested in two different children of " +
                "Compose. The user should be able to scroll each independently at the same " +
                "time, but given that we currently don't split motion, this is not work."
        )
        Row {
            AndroidView(
                { context ->
                    LayoutInflater.from(context).inflate(R.layout.android_scrollview, null)
                },
                Modifier.weight(2f)
            )
            AndroidView(
                { context ->
                    LayoutInflater.from(context).inflate(R.layout.android_scrollview, null)
                },
                Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PointerInteropFilterDemo() {

    val motionEventString: MutableState<String> = remember { mutableStateOf("Touch here!") }

    Column {
        Text("Demonstrates the functionality of pointerInteropFilter.")
        Text(
            "Touch the grey space below and it will be updated with the String representation of" +
                " the MotionEvent that the pointerInteropFilter outputs."
        )
        Box(
            Modifier.fillMaxSize()
                .background(androidx.compose.ui.graphics.Color.Gray)
                .pointerInteropFilter { newMotionEvent ->
                    motionEventString.value = newMotionEvent.toString()
                    true
                }
        ) {
            Text(motionEventString.value)
        }
    }
}

@Preview
@Composable
fun SharingEventWithSiblingOfParent() {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "Search \"SHARING\" in Logcat to see events (Cyan box is child to parent that " +
                "is sharing)."
        )
        Box {
            ClickableSurface()

            MyView(
                0xFFFF0000.toInt(), // Manually created hex for Color.Red,
                Modifier.align(Alignment.TopStart)
            )

            // Parent
            Box(
                modifier =
                    Modifier.align(Alignment.BottomEnd)
                        // This is the part that shares with the sibling...
                        .then(
                            object : PointerInputModifier {
                                override val pointerInputFilter: PointerInputFilter =
                                    object : PointerInputFilter() {
                                        override fun onPointerEvent(
                                            pointerEvent: PointerEvent,
                                            pass: PointerEventPass,
                                            bounds: IntSize
                                        ) {}

                                        override fun onCancel() {}

                                        override val shareWithSiblings: Boolean = true
                                    }
                            }
                        )
            ) {
                // Tapping here with code above will allow ClickableSurface (sibling of a parent)
                // to get the event.
                MyView(
                    0xFF00FFFF.toInt(), // Manually created hex for Color.Cyan,
                )
            }
        }
    }
}

@Composable
fun ClickableSurface(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().clickable { Log.d("SHARING", "onClick") })
}

@Composable
fun MyView(color: Int, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier.fillMaxSize(0.5f),
        factory = { context -> View(context).apply { setBackgroundColor(color) } },
    )
}
