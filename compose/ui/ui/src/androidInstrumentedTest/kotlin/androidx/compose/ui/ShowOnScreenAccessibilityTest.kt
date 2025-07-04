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

package androidx.compose.ui

import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.AndroidComposeView
import androidx.compose.ui.platform.AndroidComposeViewAccessibilityDelegateCompat
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.semanticsId
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.Constraints
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeProviderCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@OptIn(ExperimentalMaterialApi::class)
@RunWith(AndroidJUnit4::class)
class ShowOnScreenAccessibilityTest {
    @get:Rule val rule = createAndroidComposeRule<ShowOnScreenRecyclerViewActivity>()

    private lateinit var lastItemComposeView: AndroidComposeView
    private lateinit var lastItemProvider: AccessibilityNodeProviderCompat

    @Before
    fun setup() {
        InstrumentationRegistry.getInstrumentation().uiAutomation
    }

    @Test
    fun testPerformAction_showOnScreen_noScrollableComposeParent() {
        rule.mainClock.autoAdvance = false

        rule.activityRule.scenario.onActivity { activity ->
            (activity as ShowOnScreenRecyclerViewActivity).setAdapter(3) { index ->
                // first item is fully visible and the last item is only partially visible. When
                // we perform an ACTION_SHOW_ON_SCREEN on the last item, the first one goes away
                // from the screen.
                val size =
                    when (index) {
                        0 -> 20
                        1 -> 70
                        2 -> 40
                        else -> 0
                    }
                if (index == 2) {
                    lastItemComposeView = LocalView.current as AndroidComposeView
                }
                Box(Modifier.fixedPxSize(size).semantics { contentDescription = "$index" })
            }
        }

        rule.runOnIdle {
            val lastItemDelegate =
                ViewCompat.getAccessibilityDelegate(lastItemComposeView)
                    as AndroidComposeViewAccessibilityDelegateCompat
            lastItemDelegate.accessibilityForceEnabledForTesting = true
            lastItemProvider = lastItemDelegate.getAccessibilityNodeProvider(lastItemComposeView)
        }
        // verify first item is visible before action
        rule.onNodeWithContentDescription("0").assertExists().assertIsDisplayed()

        // perform ACTION_SHOW_ON_SCREEN on the last item
        val showOnScreenAction = android.R.id.accessibilityActionShowOnScreen
        val lastItemId = rule.onNodeWithContentDescription("2").semanticsId()
        rule.runOnIdle {
            assertThat(lastItemProvider.performAction(lastItemId, showOnScreenAction, null))
                .isTrue()
        }
        rule.mainClock.advanceTimeBy(5000)

        // verify first item is gone after action
        rule.onNodeWithContentDescription("0").assertExists().assertIsNotDisplayed()
    }
}

class ShowOnScreenRecyclerViewActivity : TestActivity() {

    lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        recyclerView = RecyclerView(this)
        recyclerView.layoutManager = LinearLayoutManager(this)
        setContentView(recyclerView, ViewGroup.LayoutParams(100, 100))
    }

    fun setAdapter(itemCount: Int, itemContent: @Composable (Int) -> Unit) {
        recyclerView.adapter = ComposeRecyclerViewAdapter(itemCount, itemContent)
    }
}

private class RecyclerViewViewHolder(context: Context) :
    RecyclerView.ViewHolder(ComposeView(context))

private class ComposeRecyclerViewAdapter(
    val count: Int,
    val itemContent: @Composable (Int) -> Unit,
) : RecyclerView.Adapter<RecyclerViewViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        RecyclerViewViewHolder(parent.context)

    override fun onBindViewHolder(holder: RecyclerViewViewHolder, position: Int) {
        (holder.itemView as ComposeView).setContent { itemContent(position) }
    }

    override fun getItemCount() = count
}

private fun Modifier.fixedPxSize(size: Int) = layout { measurable, _ ->
    val constraints = Constraints.fixed(size, size)
    val placeable = measurable.measure(constraints)
    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
}
