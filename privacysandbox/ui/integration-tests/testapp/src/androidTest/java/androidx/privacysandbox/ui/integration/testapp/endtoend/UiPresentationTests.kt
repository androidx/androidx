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

package androidx.privacysandbox.ui.integration.testapp.endtoend

import android.content.pm.ActivityInfo
import androidx.privacysandbox.ui.client.view.SandboxedSdkView
import androidx.privacysandbox.ui.integration.sdkproviderutils.SdkApiConstants.Companion.MediationOption
import androidx.privacysandbox.ui.integration.sdkproviderutils.SdkApiConstants.Companion.UiFrameworkOption
import androidx.privacysandbox.ui.integration.sdkproviderutils.SdkApiConstants.Companion.ZOrderOption
import androidx.privacysandbox.ui.integration.testapp.fragments.FragmentOptions
import androidx.privacysandbox.ui.integration.testapp.fragments.hidden.AbstractResizeHiddenFragment
import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasChildCount
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import kotlin.math.floor
import org.hamcrest.Matchers.instanceOf
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class UiPresentationTests(
    @UiFrameworkOption private val uiFrameworkOption: String,
    @MediationOption private val mediationOption: String,
    @ZOrderOption private val zOrdering: String,
) :
    AutomatedEndToEndTest(
        FragmentOptions.FRAGMENT_RESIZE_HIDDEN,
        uiFrameworkOption,
        FragmentOptions.AD_FORMAT_BANNER_AD,
        mediationOption,
        zOrdering,
    ) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(
            name = "uiFrameworkOption={0}, mediationOption={1}, zOrdering={2}"
        )
        fun parameters(): Collection<Array<String>> {
            return baseParameters()
        }
    }

    private lateinit var resizeHiddenFragment: AbstractResizeHiddenFragment

    @Before
    fun setup() {
        resizeHiddenFragment = getFragment() as AbstractResizeHiddenFragment
    }

    @Test
    fun resizeTest() {
        val resizedWidth = 100
        val resizedHeight = 200
        scenario.onActivity { activity ->
            resizeHiddenFragment.performResize(resizedWidth, resizedHeight)
        }

        val sandboxedSdkView = getSandboxedSdkView()
        val contentView = sandboxedSdkView.getChildAt(0)
        assertThat(sdkToClientCallback.resizeLatch.await(CALLBACK_WAIT_MS, TimeUnit.MILLISECONDS))
            .isTrue()
        assertThat(sandboxedSdkView.width).isEqualTo(resizedWidth)
        assertThat(sandboxedSdkView.height).isEqualTo(resizedHeight)
        assertThat(contentView.width).isEqualTo(resizedWidth)
        assertThat(contentView.height).isEqualTo(resizedHeight)
        assertThat(sdkToClientCallback.remoteViewWidth).isEqualTo(resizedWidth)
        assertThat(sdkToClientCallback.remoteViewHeight).isEqualTo(resizedHeight)
    }

    @Test
    fun paddingAppliedTest() {
        val sandboxedSdkView = getSandboxedSdkView()
        val resizableViewWidth = sandboxedSdkView.width
        val resizableViewHeight = sandboxedSdkView.height
        val paddingLeft = floor(resizableViewWidth * 0.05).toInt()
        val paddingTop = floor(resizableViewHeight * 0.05).toInt()
        val paddingRight = paddingLeft
        val paddingBottom = paddingTop

        scenario.onActivity { activity ->
            resizeHiddenFragment.applyPadding(paddingLeft, paddingTop, paddingRight, paddingBottom)
        }

        assertThat(sdkToClientCallback.resizeLatch.await(CALLBACK_WAIT_MS, TimeUnit.MILLISECONDS))
            .isTrue()

        // TODO(b/411324280): Verify the size of SandboxedSdkView after applying padding.
        val contentView = sandboxedSdkView.getChildAt(0)
        assertThat(contentView.width).isEqualTo(resizableViewWidth - paddingLeft - paddingRight)
        assertThat(contentView.height).isEqualTo(resizableViewHeight - paddingTop - paddingBottom)
        assertThat(sdkToClientCallback.remoteViewWidth).isEqualTo(contentView.width)
        assertThat(sdkToClientCallback.remoteViewHeight).isEqualTo(contentView.height)
    }

    @Test
    fun orientationChangedTest() {
        scenario.onActivity { activity ->
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }

        assertThat(sdkToClientCallback.configLatch.await(CALLBACK_WAIT_MS, TimeUnit.MILLISECONDS))
            .isTrue()
        assertThat(getSandboxedSdkView().context.resources.configuration)
            .isEqualTo(sdkToClientCallback.remoteViewConfiguration)
    }

    private fun getSandboxedSdkView(): SandboxedSdkView {
        var sandboxedSdkView: SandboxedSdkView? = null
        Espresso.onView(instanceOf(SandboxedSdkView::class.java))
            .check(matches(isDisplayed()))
            .check(matches(hasChildCount(1)))
            .check { view, exception -> sandboxedSdkView = view as SandboxedSdkView }
        requireNotNull(sandboxedSdkView) { "SandboxedSdkView was not displayed." }
        return sandboxedSdkView
    }
}
