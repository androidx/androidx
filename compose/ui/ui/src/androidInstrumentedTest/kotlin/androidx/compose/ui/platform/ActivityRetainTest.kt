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

package androidx.compose.ui.platform

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.retain
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.test.CountingRetainObject
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasTextExactly
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.core.view.get
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import org.junit.Rule
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class ActivityRetainTest {
    @get:Rule val activityScenarioRule = activityScenarioRule<ComponentActivity>()
    @get:Rule val composeTestRule = createEmptyComposeRule()

    private val activityScenario
        get() = activityScenarioRule.scenario

    @Test
    fun retain_survivesActivityRecreation() {
        val factoryResults = mutableListOf<CountingRetainObject>()
        var lastSeen: CountingRetainObject? = null
        val content: @Composable () -> Unit = {
            lastSeen = retain { CountingRetainObject().also { factoryResults += it } }
        }

        activityScenario.setContent(content)
        waitForIdleSync()
        assertSame(factoryResults.last(), lastSeen)
        assertEquals(1, factoryResults.size, "Factory should only be called once")
        val retained = factoryResults.first()
        retained.assertCounts(retained = 1, entered = 1, exited = 0)

        lastSeen = null
        activityScenario.recreate()
        activityScenario.setContent(content)

        waitForIdleSync()
        assertSame(factoryResults.last(), lastSeen)
        assertEquals(1, factoryResults.size, "Factory should only be called once")
        retained.assertCounts(retained = 1, entered = 2, exited = 1)
    }

    @Test
    fun retain_discardsAfterRecreationSettles() {
        val factoryResults = mutableListOf<CountingRetainObject>()
        var lastSeen: CountingRetainObject? = null
        val content: @Composable () -> Unit = {
            lastSeen = retain { CountingRetainObject().also { factoryResults += it } }
        }

        activityScenario.setContent(content)

        waitForIdleSync()
        assertSame(factoryResults.last(), lastSeen)
        assertEquals(1, factoryResults.size, "Factory should only be called once")
        val retained = factoryResults.first()
        retained.assertCounts(retained = 1, entered = 1, exited = 0, retired = 0)

        activityScenario.recreate()
        activityScenario.setContent {}

        composeTestRule.mainClock.advanceTimeByFrame()
        assertSame(factoryResults.last(), lastSeen)
        assertEquals(1, factoryResults.size, "Factory should only be called once")
        retained.assertCounts(retained = 1, entered = 1, exited = 1, retired = 1)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun retain_properlyRestoresIdenticalCompositionsAndSubcompositions() {
        var contentInvocations = 0

        @Composable
        fun Content() {
            val viewNumber = contentInvocations++
            Column {
                Text(
                    retain { "ComposeView#$viewNumber" },
                    modifier = Modifier.testTag("ComposeView#$viewNumber"),
                )
                Row {
                    repeat(5) { childNumber ->
                        SubcomposeLayout { constraints ->
                            val measurable =
                                subcompose(null) {
                                        Text(
                                            text = retain { "C#$viewNumber,$childNumber" },
                                            modifier =
                                                Modifier.testTag("C#$viewNumber,$childNumber"),
                                        )
                                    }
                                    .single()
                            val placeable = measurable.measure(constraints)
                            layout(placeable.width, placeable.height) {
                                placeable.place(x = 0, y = 0)
                            }
                        }
                    }
                }
            }
        }

        fun createViewHierarchy(context: Context): View {
            return LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                repeat(5) {
                    addView(
                        ComposeView(context).apply {
                            tag = "ComposeView#$it"
                            setContent { Content() }
                        }
                    )
                }
            }
        }

        fun checkViewHierarchy(previouslyCreatedViews: Int = 0) {
            repeat(5) { viewIndex ->
                lateinit var view: ComposeView
                activityScenario.onActivity { activity ->
                    val parent =
                        activity.findViewById<ViewGroup>(android.R.id.content)[0] as LinearLayout
                    view = parent[viewIndex] as ComposeView
                }

                val viewTagNumber = viewIndex + previouslyCreatedViews
                composeTestRule
                    .onNode(hasTestTag("ComposeView#$viewTagNumber"))
                    .assert(hasTextExactly("ComposeView#$viewIndex"))
                    .assert(inTestRoot(view.viewRootForTest))

                repeat(5) { childIndex ->
                    composeTestRule
                        .onNode(hasTestTag("C#$viewTagNumber,$childIndex"))
                        .assert(hasTextExactly("C#$viewIndex,$childIndex"))
                        .assert(inTestRoot(view.viewRootForTest))
                }
            }
        }

        activityScenario.onActivity { it.setContentView(createViewHierarchy(it)) }
        waitForIdleSync()
        checkViewHierarchy()

        activityScenario.recreate()
        activityScenario.onActivity { it.setContentView(createViewHierarchy(it)) }
        waitForIdleSync()
        checkViewHierarchy(previouslyCreatedViews = 5)
    }

    @Test
    fun test_ambiguousViewRestoration() {
        var retained1: Any? = null
        var retained2: Any? = null

        fun createActivityContentView(activity: ComponentActivity) =
            FrameLayout(activity).apply {
                addView(
                    ComposeView(activity).apply {
                        setViewTreeLifecycleOwner(
                            object : LifecycleOwner {
                                override val lifecycle = activity.lifecycle
                            }
                        )
                        setContent { retained1 = retain { Any() } }
                    }
                )

                addView(
                    ComposeView(activity).apply {
                        setViewTreeLifecycleOwner(
                            object : LifecycleOwner {
                                override val lifecycle = activity.lifecycle
                            }
                        )
                        setContent { retained2 = retain { Any() } }
                    }
                )
            }

        activityScenario.onActivity { activity ->
            activity.setContentView(createActivityContentView(activity))
        }

        waitForIdleSync()
        val originalRetained1 = retained1
        val originalRetained2 = retained2
        retained1 = null
        retained2 = null

        assertNotNull(originalRetained1, "First view did not retain a value")
        assertNotNull(originalRetained2, "Second view did not retain a value")

        activityScenario.recreate()
        activityScenario.onActivity { activity ->
            activity.setContentView(createActivityContentView(activity))
        }
        waitForIdleSync()

        assertSame(originalRetained1, retained1, "First view retained unexpected value")
        assertSame(originalRetained2, retained2, "Second view retained unexpected value")
    }

    @Test
    fun test_stableIdViewRestoration() {
        var reverseViewOrder = false
        val id1 = View.generateViewId()
        val id2 = View.generateViewId()
        val retained = mutableMapOf<Int, String>()
        var retainsCalled = 0

        fun createActivityContentView(activity: ComponentActivity) =
            FrameLayout(activity).apply {
                val content: @Composable () -> Unit = {
                    val viewId = LocalView.current.findParentOrNull { it is ComposeView }!!.id
                    retained[viewId] = retain { "Retained value ${retainsCalled++}" }
                }

                addView(
                    ComposeView(activity).apply {
                        id = if (reverseViewOrder) id2 else id1
                        setViewTreeLifecycleOwner(
                            object : LifecycleOwner {
                                override val lifecycle = activity.lifecycle
                            }
                        )
                        setContent(content)
                    }
                )

                addView(
                    ComposeView(activity).apply {
                        id = if (reverseViewOrder) id1 else id2
                        setViewTreeLifecycleOwner(
                            object : LifecycleOwner {
                                override val lifecycle = activity.lifecycle
                            }
                        )
                        setContent(content)
                    }
                )
            }

        activityScenario.onActivity { activity ->
            activity.setContentView(createActivityContentView(activity))
        }

        waitForIdleSync()

        assertEquals(mapOf(id1 to "Retained value 0", id2 to "Retained value 1"), retained)
        retained.clear()

        activityScenario.recreate()
        reverseViewOrder = true
        activityScenario.onActivity { activity ->
            activity.setContentView(createActivityContentView(activity))
        }
        waitForIdleSync()
        assertEquals(mapOf(id1 to "Retained value 0", id2 to "Retained value 1"), retained)
    }

    private fun waitForIdleSync() = InstrumentationRegistry.getInstrumentation().waitForIdleSync()

    private fun <T : ComponentActivity> ActivityScenario<T>.setContent(
        content: @Composable () -> Unit
    ) {
        onActivity { it.setContent(null, content) }
    }

    private fun View.findParentOrNull(predicate: (View) -> Boolean): View? {
        var current = parent as? View
        while (current != null && !predicate(current)) current = parent.parent as? View
        return current
    }
}

private val ComposeView.viewRootForTest: ViewRootForTest
    get() = get(0) as ViewRootForTest

private fun inTestRoot(root: RootForTest): SemanticsMatcher =
    SemanticsMatcher("in test root $root") { it.root == root }
