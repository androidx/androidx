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

import android.content.Context
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.TestActivity2
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

class MixedFocusChangeTest {
    @get:Rule val rule = createAndroidComposeRule<TestActivity2>()

    @Test
    fun siblingWithWorseBeam() {
        val view = rule.runOnUiThread { MyComposeView(rule.activity, false) }
        rule.runOnIdle { rule.activity.setContentView(view) }
        rule.waitUntil {
            view.findViewWithTag<View>("item 0") != null &&
                view.findViewWithTag<View>("item 1") != null
        }
        val first = view.findViewWithTag<View>("item 0")
        val second = view.findViewWithTag<View>("item 1")
        rule.runOnIdle {
            view.inputModeManager.requestInputMode(InputMode.Keyboard)
            first.requestFocus()
        }
        rule.runOnIdle { assertThat(first.isFocused).isTrue() }
        InstrumentationRegistry.getInstrumentation()
            .sendKeySync(KeyEvent(KeyEvent.ACTION_DOWN, Key.DirectionDown.nativeKeyCode))
        rule.runOnIdle { assertThat(second.isFocused).isTrue() }
    }

    @Test
    fun previousEscapesRecyclerView() {
        val view = rule.runOnUiThread { MyComposeView(rule.activity, false) }
        rule.runOnIdle { rule.activity.setContentView(view) }
        rule.waitUntil { view.findViewWithTag<View>("item 0") != null }
        val first = view.findViewWithTag<View>("item 0")
        rule.runOnIdle {
            view.inputModeManager.requestInputMode(InputMode.Keyboard)
            first.requestFocus()
        }
        rule.runOnIdle { assertThat(first.isFocused).isTrue() }
        InstrumentationRegistry.getInstrumentation()
            .sendKeySync(KeyEvent(KeyEvent.ACTION_DOWN, Key.NavigatePrevious.nativeKeyCode))
        rule.onNodeWithTag(clickableBoxTag).assertIsFocused()
    }

    @Test
    fun nextEscapesReverseRecyclerView() {
        val view = rule.runOnUiThread { MyComposeView(rule.activity, true) }
        rule.runOnIdle { rule.activity.setContentView(view) }
        rule.waitUntil { view.findViewWithTag<View>("item 0") != null }
        val first = view.findViewWithTag<View>("item 0")
        rule.runOnIdle {
            view.inputModeManager.requestInputMode(InputMode.Keyboard)
            first.requestFocus()
        }
        rule.runOnIdle { assertThat(first.isFocused).isTrue() }
        InstrumentationRegistry.getInstrumentation()
            .sendKeySync(KeyEvent(KeyEvent.ACTION_DOWN, Key.NavigateNext.nativeKeyCode))
        rule.onNodeWithTag(clickableBoxTag).assertIsFocused()
    }

    class MyComposeView(context: Context, val reverse: Boolean) : AbstractComposeView(context) {
        lateinit var inputModeManager: InputModeManager
        lateinit var view: View
        lateinit var recyclerView: RecyclerView

        @Composable
        override fun Content() {
            view = LocalView.current
            inputModeManager = LocalInputModeManager.current
            Box(Modifier.fillMaxSize()) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = {
                        RecyclerView(it).apply {
                            recyclerView = this
                            layoutManager =
                                LinearLayoutManager(context, RecyclerView.VERTICAL, reverse)
                            adapter = MyAdapter()
                        }
                    }
                )
                Box(
                    modifier =
                        Modifier.padding(30.dp)
                            .width(100.dp)
                            .height(400.dp)
                            .background(Color.Red)
                            .align(AbsoluteAlignment.BottomRight)
                            .testTag(clickableBoxTag)
                            .clickable {}
                            .fillMaxSize()
                ) {
                    Text(
                        text = "Click Me",
                        color = Color.White,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }

    class MyAdapter : RecyclerView.Adapter<MyAdapter.MyViewHolder>() {
        private val items = List(100) { "Item ${it + 1}" }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            val textView =
                TextView(parent.context).apply {
                    layoutParams =
                        ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                    setPadding(16, 16, 16, 16)
                    isFocusable = true // Makes the item focusable
                    isFocusableInTouchMode = true // Required for touch navigation
                }
            return MyViewHolder(textView)
        }

        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            (holder.itemView as TextView).text = items[position]
            holder.itemView.tag = "item $position"
        }

        override fun getItemCount(): Int = items.size

        class MyViewHolder(view: View) : RecyclerView.ViewHolder(view)
    }

    companion object {
        private const val clickableBoxTag = "clickableBox"
    }
}
