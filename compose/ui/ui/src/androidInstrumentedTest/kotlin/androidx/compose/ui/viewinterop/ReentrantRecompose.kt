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
package androidx.compose.ui.viewinterop

import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_UP
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerCoords
import androidx.compose.ui.input.pointer.PointerProperties
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.test.TestActivity2
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class ReentrantRecompose {
    @get:Rule val rule = ActivityScenarioRule(TestActivity2::class.java)

    @Test
    fun composerWithFocusChange() {
        lateinit var editText: EditText

        val instrumentation = InstrumentationRegistry.getInstrumentation()

        instrumentation.runOnMainSync {
            rule.scenario.onActivity { activity ->
                activity.setContentView(
                    ComposeView(activity).apply {
                        setContent {
                            var showItem by remember { mutableStateOf(true) }
                            LazyColumn(Modifier.fillMaxSize()) {
                                item { Button(onClick = {}) { Text("Hello") } }
                                if (showItem) {
                                    item(key = "Foo") {
                                        AndroidView(
                                            factory = {
                                                EditText(it).also {
                                                    editText = it
                                                    it.maxLines = 1
                                                    it.inputType = EditorInfo.TYPE_CLASS_TEXT
                                                    it.setText("Hello World")
                                                    it.setOnFocusChangeListener { _, hasFocus ->
                                                        if (hasFocus) {
                                                            showItem = false
                                                        }
                                                    }
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                                item(key = "box") {
                                    val height =
                                        (LocalConfiguration.current.screenHeightDp * 0.7f).dp
                                    Box(
                                        Modifier.fillMaxWidth()
                                            .height(height)
                                            .background(Color.Yellow)
                                    )
                                }
                                repeat(100) { index ->
                                    item(key = index) {
                                        AndroidView(
                                            factory = {
                                                EditText(it).also {
                                                    it.maxLines = 1
                                                    it.inputType = EditorInfo.TYPE_CLASS_TEXT
                                                    it.setText("Hello $index")
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }
                )
            }
        }
        instrumentation.waitForIdleSync()

        // Poke the EditText so that the IME pops up
        val downTime = SystemClock.uptimeMillis()
        val location = intArrayOf(0, 0)
        instrumentation.runOnMainSync { editText.getLocationOnScreen(location) }
        val centerX = editText.width / 2f + location[0]
        val centerY = editText.height / 2f + location[1]
        val down =
            MotionEvent.obtain(
                downTime, // downTime
                downTime, // eventTime
                ACTION_DOWN, // action
                1, // pointerCount
                arrayOf(PointerProperties(0)), // pointerProperties
                arrayOf(PointerCoords(centerX, centerY)), // pointerCoords
                0, // metaState
                0, // buttonState
                1f, // xPrecision
                1f, // yPrecision
                0, // deviceId
                0, // edgeFlags
                InputDevice.SOURCE_TOUCHSCREEN, // source
                0, // flags
            )
        instrumentation.sendPointerSync(down)

        val up =
            MotionEvent.obtain(
                downTime, // downTime
                SystemClock.uptimeMillis(), // eventTime
                ACTION_UP, // action
                1, // pointerCount
                arrayOf(PointerProperties(0)), // pointerProperties
                arrayOf(PointerCoords(centerX, centerY)), // pointerCoords
                0, // metaState
                0, // buttonState
                1f, // xPrecision
                1f, // yPrecision
                0, // deviceId
                0, // edgeFlags
                InputDevice.SOURCE_TOUCHSCREEN, // source
                0, // flags
            )

        instrumentation.sendPointerSync(up)

        // Make sure it doesn't crash after all the focus change, IME motion, and recomposition.
        // I don't know a test that works on all versions (e.g. resulting focus), but we really
        // only care that we're not crashing
        SystemClock.sleep(1000)
    }
}
