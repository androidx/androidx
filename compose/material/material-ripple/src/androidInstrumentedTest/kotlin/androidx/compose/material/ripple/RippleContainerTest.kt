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

package androidx.compose.material.ripple

import android.content.Context
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.children
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Test for [RippleContainer] */
@MediumTest
@RunWith(AndroidJUnit4::class)
class RippleContainerTest {

    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun cachesViews() {
        val activity = rule.activity
        val container = RippleContainer(activity)

        val instance = TestRippleHostKey()

        with(container) {
            val hostView = instance.getRippleHostView()
            // The same View should be returned
            Truth.assertThat(hostView).isEqualTo(instance.getRippleHostView())
        }
    }

    @Test
    fun returnsNewViews() {
        val activity = rule.activity
        val container = RippleContainer(activity)

        val instance1 = TestRippleHostKey()
        val instance2 = TestRippleHostKey()

        with(container) {
            val hostView1 = instance1.getRippleHostView()
            val hostView2 = instance2.getRippleHostView()
            // A new View should be returned
            Truth.assertThat(hostView1).isNotEqualTo(hostView2)
        }
    }

    @Test
    fun reassignsExistingViews() {
        val activity = rule.activity
        val container = RippleContainer(activity)

        val instance1 = TestRippleHostKey()
        val instance2 = TestRippleHostKey()
        val instance3 = TestRippleHostKey()
        val instance4 = TestRippleHostKey()
        val instance5 = TestRippleHostKey()
        val instance6 = TestRippleHostKey()

        with(container) {
            // Assign the maximum number of host views
            val hostView1 = instance1.getRippleHostView()
            val hostView2 = instance2.getRippleHostView()
            instance3.getRippleHostView()
            instance4.getRippleHostView()
            instance5.getRippleHostView()

            // When we try and get a new view on the 6th instance
            val hostView6 = instance6.getRippleHostView()

            // It should be the same as hostView1, now re-assigned to a new view
            Truth.assertThat(hostView6).isEqualTo(hostView1)

            // When the first instance tries to get the instance again
            val hostView = instance1.getRippleHostView()

            // It should now be the same view used for the second instance, as we continue to
            // recycle in order
            Truth.assertThat(hostView).isNotEqualTo(hostView6)
            Truth.assertThat(hostView).isEqualTo(hostView2)
        }
    }

    @Test
    fun reusesDisposedViews() {
        val activity = rule.activity
        val container = RippleContainer(activity)

        val instance1 = TestRippleHostKey()
        val instance2 = TestRippleHostKey()
        val instance3 = TestRippleHostKey()
        val instance4 = TestRippleHostKey()
        val instance5 = TestRippleHostKey()
        val instance6 = TestRippleHostKey()

        with(container) {
            // Assign some initial views
            val hostView1 = instance1.getRippleHostView()
            val hostView2 = instance2.getRippleHostView()
            val hostView3 = instance3.getRippleHostView()

            // Dispose the first two ripples
            instance1.disposeRippleIfNeeded()
            instance2.disposeRippleIfNeeded()

            // The host views previously used by instance1 and instance1 should now be reused,
            // before allocating new views
            val hostView4 = instance4.getRippleHostView()
            val hostView5 = instance5.getRippleHostView()

            Truth.assertThat(hostView4).isEqualTo(hostView1)
            Truth.assertThat(hostView5).isEqualTo(hostView2)

            // When we try and get a view for the 6th instance
            val hostView6 = instance6.getRippleHostView()

            // It should now be a totally new host view, not previously used by any of the other
            // instances, since there are no more unused views
            Truth.assertThat(hostView6).isNotEqualTo(hostView1)
            Truth.assertThat(hostView6).isNotEqualTo(hostView2)
            Truth.assertThat(hostView6).isNotEqualTo(hostView3)
            Truth.assertThat(hostView6).isNotEqualTo(hostView4)
            Truth.assertThat(hostView6).isNotEqualTo(hostView5)
        }
    }

    private object TestRipple : IndicationNodeFactory {
        override fun create(interactionSource: InteractionSource): DelegatableNode {
            return createRippleModifierNode(
                interactionSource = interactionSource,
                bounded = true,
                radius = Dp.Unspecified,
                color = { Color.Red },
                rippleAlpha = { RippleAlpha(0.2f, 0.2f, 0.2f, 0.2f) }
            )
        }

        override fun equals(other: Any?) = other === this

        override fun hashCode(): Int = -1
    }

    @Test
    fun addingRippleContainerDoesNotCauseRelayout() {
        val requestLayoutTrackingFrameLayout = RequestLayoutTrackingFrameLayout(rule.activity)
        val composeView = ComposeView(rule.activity)
        val interactionSource1 = MutableInteractionSource()
        val interactionSource2 = MutableInteractionSource()

        var androidComposeView: ViewGroup? = null
        var scope: CoroutineScope? = null

        rule.runOnUiThread {
            requestLayoutTrackingFrameLayout.addView(composeView)
            rule.activity.setContentView(requestLayoutTrackingFrameLayout)
            composeView.setContent {
                scope = rememberCoroutineScope()
                androidComposeView = LocalView.current as ViewGroup
                Column {
                    Box(
                        Modifier.wrapContentSize(align = Alignment.Center)
                            .size(40.dp)
                            .indication(
                                interactionSource = interactionSource1,
                                indication = TestRipple
                            )
                    )
                    Box(
                        Modifier.wrapContentSize(align = Alignment.Center)
                            .size(40.dp)
                            .indication(
                                interactionSource = interactionSource2,
                                indication = TestRipple
                            )
                    )
                }
            }
        }

        rule.runOnIdle {
            // RippleContainer should be lazily added
            val children = androidComposeView!!.children
            val hasRippleContainer = children.filterIsInstance<RippleContainer>().any()
            Truth.assertThat(hasRippleContainer).isFalse()
            Truth.assertThat(requestLayoutTrackingFrameLayout.requestLayoutCalled).isTrue()
            // reset tracking
            requestLayoutTrackingFrameLayout.requestLayoutCalled = false

            // Emit press on first ripple
            scope!!.launch { interactionSource1.emit(PressInteraction.Press(Offset.Zero)) }
        }

        rule.runOnIdle {
            // Emitting the interaction should cause us to create the ripple container and initial
            // host view without triggering a relayout
            val children = androidComposeView!!.children
            val rippleContainer = children.filterIsInstance<RippleContainer>().singleOrNull()
            Truth.assertThat(rippleContainer).isNotNull()
            val rippleHostView =
                rippleContainer!!.children.filterIsInstance<RippleHostView>().singleOrNull()
            Truth.assertThat(rippleHostView).isNotNull()
            Truth.assertThat(requestLayoutTrackingFrameLayout.requestLayoutCalled).isFalse()

            // Emit press on second ripple
            scope!!.launch { interactionSource2.emit(PressInteraction.Press(Offset.Zero)) }
        }

        rule.runOnIdle {
            // Emitting another interaction should cause us to create an addition host view
            // without triggering a relayout
            val children = androidComposeView!!.children
            val rippleContainer = children.filterIsInstance<RippleContainer>().singleOrNull()
            Truth.assertThat(rippleContainer).isNotNull()
            val rippleHostViews =
                rippleContainer!!.children.filterIsInstance<RippleHostView>().toList()
            Truth.assertThat(rippleHostViews.size).isEqualTo(2)
            Truth.assertThat(requestLayoutTrackingFrameLayout.requestLayoutCalled).isFalse()
        }
    }
}

private class TestRippleHostKey : RippleHostKey {
    override fun onResetRippleHostView() {}
}

private class RequestLayoutTrackingFrameLayout(context: Context) : FrameLayout(context) {
    var requestLayoutCalled = false

    override fun requestLayout() {
        super.requestLayout()
        requestLayoutCalled = true
    }
}
