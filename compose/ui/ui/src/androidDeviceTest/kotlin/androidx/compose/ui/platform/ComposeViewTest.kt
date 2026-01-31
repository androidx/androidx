/*
 * Copyright 2021 The Android Open Source Project
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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.tests.R
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.TruthJUnit.assume
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class ComposeViewTest {
    @get:Rule val rule = createAndroidComposeRule<TestActivity>(StandardTestDispatcher())

    @Test
    fun composeViewIsTransitionGroup() {
        val view = ComposeView(rule.activity)
        assertTrue("ComposeView isTransitionGroup by default", view.isTransitionGroup)
    }

    @Test
    fun composeViewInflatesTransitionGroup() {
        val view =
            rule.activity.layoutInflater.inflate(R.layout.composeview_transition_group_false, null)
                as ViewGroup
        assertFalse("XML overrides ComposeView.isTransitionGroup", view.isTransitionGroup)
    }

    @Test
    fun createComposeViewWithApplicationContext_doesNotCrash() {
        val activity = rule.activity
        val view = ComposeView(activity.applicationContext)
        rule.runOnIdle { activity.setContentView(view) }
        rule.waitForIdle()
    }

    @Test
    fun composeWithComposeViewContext() {
        @OptIn(ExperimentalComposeUiApi::class)
        assume().that(ComposeUiFlags.isAdaptiveRefreshRateEnabled).isTrue()
        rule.runOnUiThread { rule.activity.setContentView(FrameLayout(rule.activity)) }
        rule.waitForIdle()
        val contentView = rule.activity.findViewById<View>(android.R.id.content)
        val composeViewContext = rule.runOnUiThread { ComposeViewContext(contentView) }
        val composeView = ComposeView(rule.activity)
        var text by mutableStateOf("Hello")
        lateinit var view: View
        lateinit var readText: String
        rule.runOnUiThread {
            composeView.setContent {
                view = LocalView.current
                readText = text
            }
            composeView.createComposition(composeViewContext)
        }
        rule.waitForIdle()
        assertThat(view.parent).isEqualTo(composeView)
        assertThat(readText).isEqualTo("Hello")
        text = "World"
        rule.waitForIdle()
        assertThat(readText).isEqualTo("World")
    }

    @Test
    fun setComposeViewContextToNullStopsObserving() {
        @OptIn(ExperimentalComposeUiApi::class)
        assume().that(ComposeUiFlags.isAdaptiveRefreshRateEnabled).isTrue()
        lateinit var composeViewContext: ComposeViewContext
        rule.setContent {
            val view = LocalView.current
            composeViewContext = remember { ComposeViewContext(view) }
        }

        lateinit var composeView: ComposeView
        var isComposed = false
        rule.runOnIdle {
            composeView = ComposeView(rule.activity)
            composeView.setContent { isComposed = true }
            composeView.createComposition(composeViewContext)
        }

        rule.waitForIdle()
        assertThat(isComposed).isTrue()
        assertThat(composeViewContext.viewCount).isEqualTo(1)
        rule.runOnUiThread {
            composeView.disposeComposition()
            assertThat(composeViewContext.viewCount).isEqualTo(0)

            // Doing it a second time does nothing
            composeView.disposeComposition()
            assertThat(composeViewContext.viewCount).isEqualTo(0)
        }
    }

    @Test
    fun detachingComposeViewWithComposeViewContextStopsObserving() {
        @OptIn(ExperimentalComposeUiApi::class)
        assume().that(ComposeUiFlags.isAdaptiveRefreshRateEnabled).isTrue()
        lateinit var composeViewContext: ComposeViewContext
        var addView by mutableStateOf(false)
        lateinit var composeView: ComposeView

        rule.setContent {
            val view = LocalView.current
            composeViewContext = remember { ComposeViewContext(view) }
            if (addView) {
                AndroidView(factory = { composeView })
            }
        }

        var isComposed = false
        rule.runOnIdle {
            composeView = ComposeView(rule.activity)
            composeView.setContent { isComposed = true }
            composeView.createComposition(composeViewContext)
        }

        rule.waitForIdle()
        assertThat(isComposed).isTrue()
        assertThat(composeViewContext.viewCount).isEqualTo(1)

        // Add the View
        addView = true
        rule.waitForIdle()

        // Removing the View should stop the ComposeViewContext from observing
        addView = false
        rule.waitForIdle()
        assertThat(composeViewContext.viewCount).isEqualTo(0)

        // Disposing the composition doesn't need do anything to the ComposeViewContext
        rule.runOnIdle {
            composeView.disposeComposition()
            assertThat(composeViewContext.viewCount).isEqualTo(0)
        }
    }

    @Test
    fun reattachingComposeViewWithComposeViewContextStartsObserving() {
        lateinit var composeViewContext: ComposeViewContext
        var addView by mutableStateOf(true)
        lateinit var composeView: ComposeView
        var isComposed = false

        rule.setContent {
            val view = LocalView.current
            composeViewContext = remember { ComposeViewContext(view) }
            if (addView) {
                AndroidView(
                    factory = {
                        composeView = ComposeView(rule.activity)
                        composeView.setContent { isComposed = true }
                        composeView.createComposition(composeViewContext)
                        composeView
                    }
                )
            }
        }

        rule.waitForIdle()
        assertThat(isComposed).isTrue()
        assertThat(composeViewContext.viewCount).isEqualTo(1)

        // Removing the View should stop the ComposeViewContext from observing
        addView = false
        rule.waitForIdle()
        assertThat(composeViewContext.viewCount).isEqualTo(0)

        // Adding it back should start the ComposeViewContext observing
        addView = true
        rule.waitForIdle()
        assertThat(composeViewContext.viewCount).isEqualTo(1)
    }

    @Test
    fun multipleComposeViewsSharingComposeViewContext() {
        lateinit var composeViewContext: ComposeViewContext
        var addView1 by mutableStateOf(true)
        var addView2 by mutableStateOf(true)

        rule.setContent {
            val view = LocalView.current
            composeViewContext = remember { ComposeViewContext(view) }
            if (addView1) {
                AndroidView(
                    factory = {
                        ComposeView(rule.activity).also {
                            it.setContent { Box(Modifier.fillMaxSize()) }
                            it.createComposition(composeViewContext)
                        }
                    }
                )
            }
            if (addView2) {
                AndroidView(
                    factory = {
                        ComposeView(rule.activity).also {
                            it.setContent { Box(Modifier.fillMaxSize()) }
                            it.createComposition(composeViewContext)
                        }
                    }
                )
            }
        }

        rule.waitForIdle()
        assertThat(composeViewContext.viewCount).isEqualTo(2)

        addView1 = false
        rule.waitForIdle()
        assertThat(composeViewContext.viewCount).isEqualTo(1)

        addView2 = false
        rule.waitForIdle()
        assertThat(composeViewContext.viewCount).isEqualTo(0)

        addView1 = true
        rule.waitForIdle()
        assertThat(composeViewContext.viewCount).isEqualTo(1)
    }

    @Test
    fun composeViewWithComposeViewContextDisposeCompositionRecompose() {
        lateinit var composeView: ComposeView
        lateinit var localView: View
        var addView by mutableStateOf(false)
        var isComposed = false

        rule.setContent {
            localView = LocalView.current
            if (addView) {
                AndroidView(factory = { composeView })
            }
        }
        lateinit var composeViewContext: ComposeViewContext
        rule.runOnIdle {
            composeViewContext = ComposeViewContext(localView)
            composeView =
                ComposeView(rule.activity).also {
                    it.setContent { isComposed = true }
                    it.createComposition(composeViewContext)
                }
        }
        addView = true
        rule.waitForIdle()
        addView = false
        rule.runOnIdle {
            isComposed = false
            composeView.disposeComposition()
        }

        rule.waitForIdle()
        assertThat(isComposed).isFalse()
        addView = true
        rule.waitForIdle()
        assertThat(isComposed).isTrue()
    }

    @Test
    fun detachedComposition() {
        @OptIn(ExperimentalComposeUiApi::class)
        assume().that(ComposeUiFlags.isAdaptiveRefreshRateEnabled).isTrue()
        lateinit var view: View
        rule.setContent { view = LocalView.current }
        rule.waitForIdle()
        val composeView = rule.runOnUiThread { ComposeView(view.context) }
        var isComposed by mutableStateOf(false)
        rule.runOnUiThread {
            composeView.setContent { Box { isComposed = true } }
            composeView.createComposition(ComposeViewContext(view))
        }
        rule.waitForIdle()
        assertThat(isComposed).isTrue()
    }

    @Test
    fun setContentAfterCreateComposition() {
        @OptIn(ExperimentalComposeUiApi::class)
        assume().that(ComposeUiFlags.isAdaptiveRefreshRateEnabled).isTrue()
        lateinit var view: View
        rule.setContent { view = LocalView.current }
        rule.waitForIdle()
        val composeViewContext = rule.runOnUiThread { ComposeViewContext(view) }
        val composeView =
            rule.runOnUiThread {
                ComposeView(view.context).also { it.createComposition(composeViewContext) }
            }
        var isComposed by mutableStateOf(false)
        rule.runOnUiThread { composeView.setContent { Box { isComposed = true } } }
        rule.waitForIdle()
        assertThat(isComposed).isTrue()
    }

    @Test
    fun reuseAutomaticComposeViewContext() {
        @OptIn(ExperimentalComposeUiApi::class)
        assume().that(ComposeUiFlags.isAdaptiveRefreshRateEnabled).isTrue()
        lateinit var view: View
        var addComposeView by mutableStateOf(false)
        lateinit var composeView: ComposeView
        rule.setContent {
            view = LocalView.current
            if (addComposeView) {
                AndroidView(factory = { composeView })
            }
        }
        rule.waitForIdle()
        val composeViewContext = rule.runOnUiThread { view.findViewTreeComposeViewContext()!! }
        var isComposed by mutableStateOf(false)
        composeView =
            rule.runOnUiThread {
                ComposeView(view.context).also {
                    it.createComposition(composeViewContext)
                    it.setContent { isComposed = true }
                }
            }
        rule.waitForIdle()
        assertThat(isComposed).isTrue()
        assertThat(composeViewContext.viewCount).isEqualTo(2)

        // Adding the ComposeView to the hierarchy shouldn't add to the view count
        addComposeView = true
        rule.waitForIdle()
        assertThat(composeViewContext.viewCount).isEqualTo(2)

        // clear the hierarchy
        rule.runOnUiThread { rule.activity.setContentView(View(rule.activity)) }
        rule.waitForIdle()
        assertThat(composeViewContext.viewCount).isEqualTo(0)
    }

    @Test
    fun disposedComposeViewContextCanRecompose() {
        @OptIn(ExperimentalComposeUiApi::class)
        assume().that(ComposeUiFlags.isAdaptiveRefreshRateEnabled).isTrue()
        lateinit var view: View
        var addComposeView by mutableStateOf(false)
        lateinit var composeView: ComposeView
        var recomposeInt by mutableIntStateOf(1)
        rule.setContent {
            view = LocalView.current
            if (recomposeInt > 0 && addComposeView) {
                AndroidView(factory = { composeView })
            }
        }
        rule.waitForIdle()
        val composeViewContext = rule.runOnUiThread { view.findViewTreeComposeViewContext()!! }
        var isComposed by mutableStateOf(false)
        composeView =
            rule.runOnUiThread {
                ComposeView(view.context).also {
                    it.createComposition(composeViewContext)
                    it.setContent { isComposed = true }
                }
            }
        rule.waitForIdle()
        // Adding the ComposeView to the hierarchy shouldn't add to the view count
        addComposeView = true
        rule.runOnUiThread {
            isComposed = false
            composeView.disposeComposition()
            // now force recomposition
            recomposeInt++
        }

        rule.waitForIdle()
        assertThat(isComposed).isTrue()
        assertThat(composeViewContext.viewCount).isEqualTo(2)
    }

    @Test
    fun findViewTreeComposeViewContextExists() {
        lateinit var view: View
        rule.setContent { view = LocalView.current }
        rule.waitForIdle()
        assertThat(view.findViewTreeComposeViewContext())
            .isEqualTo((view as AndroidComposeView).composeViewContext)
    }

    @Test
    fun findViewTreeComposeViewContextSiblings() {
        lateinit var view1: View
        lateinit var view2: View
        rule.runOnUiThread {
            view1 = ComposeView(rule.activity)
            view2 = ComposeView(rule.activity)
            val group = FrameLayout(rule.activity)
            group.addView(view1)
            group.addView(view2)
            rule.activity.setContentView(group)
        }
        rule.waitForIdle()
        assertThat(view1.findViewTreeComposeViewContext())
            .isEqualTo(view2.findViewTreeComposeViewContext())
    }

    @Test
    fun findViewTreeComposeViewContextChild() {
        lateinit var view1: View
        lateinit var view2: View
        rule.setContent {
            view1 = LocalView.current
            AndroidView(
                factory = {
                    view2 = ComposeView(it)
                    view2
                }
            )
        }
        rule.waitForIdle()
        assertThat(view1.findViewTreeComposeViewContext())
            .isEqualTo(view2.findViewTreeComposeViewContext())
    }

    @Test
    fun findViewTreeComposeViewContextDoesNotExists() {
        val view = rule.activity.findViewById<View>(android.R.id.content)
        assertThat(view.findViewTreeComposeViewContext()).isNull()
    }

    @Test
    fun findViewTreeComposeViewContextLifecycleDifferent() {
        lateinit var outer: View
        rule.setContent {
            outer = LocalView.current
            AndroidView(
                factory = { FragmentContainerView(it).also { it.id = R.id.lifecycleContainer } }
            )
        }
        class MyFragment : Fragment() {
            lateinit var inner: ViewGroup

            override fun onCreateView(
                inflater: LayoutInflater,
                container: ViewGroup?,
                savedInstanceState: Bundle?,
            ): View {
                return FrameLayout(inflater.context).also { inner = it }
            }
        }
        val fragment = MyFragment()
        rule.runOnIdle {
            with(rule.activity) {
                supportFragmentManager
                    .beginTransaction()
                    .add(R.id.lifecycleContainer, fragment)
                    .commit()
            }
        }
        rule.runOnIdle {
            assertThat(fragment.inner.findViewTreeComposeViewContext()).isNull()
            fragment.inner.addView(
                ComposeView(rule.activity).also { it.setContent { Box(Modifier.fillMaxSize()) } }
            )
        }
        rule.runOnIdle {
            assertThat(fragment.inner.findViewTreeComposeViewContext()).isNotNull()
            assertThat(fragment.inner.findViewTreeComposeViewContext())
                .isNotEqualTo(outer.findViewTreeComposeViewContext())
        }
    }

    @Test
    fun composeViewContextViewProperty() {
        lateinit var view: View
        rule.setContent { view = LocalView.current }
        rule.runOnIdle {
            assertThat(view.composeViewContext).isNull()
            val composeViewContext = view.findViewTreeComposeViewContext()!!
            assertThat(composeViewContext).isNotNull()
            view.composeViewContext = composeViewContext
            assertThat(view.composeViewContext).isSameInstanceAs(composeViewContext)
        }
    }

    @Test
    fun reuseComposeViewWithComposeViewContext() {
        lateinit var view: View
        lateinit var childView: ComposeView
        var addView by mutableStateOf(false)
        rule.setContent {
            view = LocalView.current
            Box(Modifier.fillMaxSize())
            if (addView) {
                AndroidView(factory = { childView })
            }
        }
        val composeViewContext = rule.runOnUiThread { view.findViewTreeComposeViewContext()!! }
        childView =
            rule.runOnUiThread {
                ComposeView(rule.activity).also {
                    it.setContent { Box(Modifier.fillMaxSize()) }
                    it.createComposition(composeViewContext)
                }
            }
        rule.waitForIdle()
        addView = true
        rule.waitForIdle()
        addView = false
        rule.waitForIdle()

        // Now that the ComposeView has been added and removed, if we call setContent on it again,
        // it should compose
        var isComposed = false
        childView.setContent { isComposed = true }
        rule.waitForIdle()
        assertThat(isComposed).isTrue()
    }

    @Test
    @OptIn(ExperimentalComposeUiApi::class)
    fun frameRateCategoryViewProtection() {
        ComposeUiFlags.isAdaptiveRefreshRateEnabled = false

        var addComposeView by mutableStateOf(true)
        var isAdded by mutableStateOf(false)
        rule.setContent {
            if (addComposeView) {
                AndroidView(
                    factory = {
                        ComposeView(it).apply { setContent { Box(Modifier.fillMaxSize()) } }
                    }
                )
            }
            isAdded = addComposeView
        }

        rule.waitForIdle()
        ComposeUiFlags.isAdaptiveRefreshRateEnabled = true
        addComposeView = false
        rule.waitForIdle()
        assertThat(isAdded).isFalse()
    }
}
