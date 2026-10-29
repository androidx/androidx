/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.pdf.view.annotation

import android.annotation.SuppressLint
import android.content.Context
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.R as MaterialR
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class ViewExtensionsTest {

    private lateinit var context: Context
    private lateinit var view: View

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        view = View(context)
        // Define the view's bounds for testing. Let's assume it's at (100, 100)
        // with a size of 200x100. So its rect is (100, 100, 300, 200).
        view.layout(100, 100, 300, 200)
    }

    @Test
    fun isTouchInView_whenTouchIsInside_returnsTrue() {
        // Create a touch event in the middle of the view.
        val touchEvent = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 150f, 150f, 0)

        assertThat(view.isTouchInView(touchEvent)).isTrue()
    }

    @Test
    fun isTouchInView_whenTouchIsOutside_returnsFalse() {
        // Create a touch event far outside the view's bounds.
        val touchEvent = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 500f, 500f, 0)

        assertThat(view.isTouchInView(touchEvent)).isFalse()
    }

    @Test
    fun isTouchInView_whenTouchIsOnLeftEdge_returnsTrue() {
        // Create a touch event exactly on the left edge.
        val touchEvent = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 100f, 150f, 0)

        assertThat(view.isTouchInView(touchEvent)).isTrue()
    }

    @Test
    fun isTouchInView_whenTouchIsJustOutsideRightEdge_returnsFalse() {
        // The rect's right is 300, so a touch at 300.0f is outside because
        // Rect.contains checks for left <= x < right.
        val touchEvent = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 300f, 150f, 0)

        assertThat(view.isTouchInView(touchEvent)).isFalse()
    }

    @Test
    fun isTouchInView_whenTouchIsOnTopEdge_returnsTrue() {
        // Create a touch event exactly on the top edge.
        val touchEvent = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 150f, 100f, 0)

        assertThat(view.isTouchInView(touchEvent)).isTrue()
    }

    @Test
    fun isTouchInView_whenTouchIsJustOutsideBottomEdge_returnsFalse() {
        // The rect's bottom is 200, so a touch at 200.0f is outside.
        val touchEvent = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 150f, 200f, 0)

        assertThat(view.isTouchInView(touchEvent)).isFalse()
    }

    @Test
    fun findHighestLayoutRequestedAncestor_returnsSelf_whenDetachedOrParentIsNotLayoutRequested() {
        val detachedView = View(context)
        assertThat(detachedView.findHighestLayoutRequestedAncestor()).isSameInstanceAs(detachedView)
        detachedView.propagateRequestLayoutToRoot()

        val parent = FrameLayout(context)
        val child = View(context)
        parent.addView(child)
        parent.layout(0, 0, 500, 500)
        child.layout(0, 0, 100, 100)

        assertThat(parent.isLayoutRequested).isFalse()
        assertThat(child.findHighestLayoutRequestedAncestor()).isSameInstanceAs(child)
    }

    @Test
    fun findHighestLayoutRequestedAncestor_returnsTopmostLayoutRequestedParent() {
        val grandParent = FrameLayout(context)
        val interceptingParent =
            object : FrameLayout(context) {
                @SuppressLint("MissingSuperCall")
                override fun requestLayout() {
                    // Simulates Compose AndroidViewsHandler swallowing requestLayout()
                }
            }
        val holder = FrameLayout(context)
        val child = View(context)

        grandParent.addView(interceptingParent)
        interceptingParent.addView(holder)
        holder.addView(child)

        grandParent.layout(0, 0, 500, 500)
        interceptingParent.layout(0, 0, 500, 500)
        holder.layout(0, 0, 500, 500)
        child.layout(0, 0, 100, 100)

        child.requestLayout()

        assertThat(child.isLayoutRequested).isTrue()
        assertThat(holder.isLayoutRequested).isTrue()
        assertThat(interceptingParent.isLayoutRequested).isFalse()
        assertThat(grandParent.isLayoutRequested).isFalse()
        assertThat(child.findHighestLayoutRequestedAncestor()).isSameInstanceAs(holder)
    }

    @Test
    fun propagateRequestLayoutToRoot_propagatesPastInterceptingParentToRoot() {
        val rootContainer = FrameLayout(context)
        val composeHost = FrameLayout(context)
        val interceptingHandler =
            object : FrameLayout(context) {
                @SuppressLint("MissingSuperCall")
                override fun requestLayout() {
                    // Simulates Compose AndroidViewsHandler swallowing requestLayout()
                }
            }
        val holder = FrameLayout(context)
        val child = View(context)

        rootContainer.addView(composeHost)
        composeHost.addView(interceptingHandler)
        interceptingHandler.addView(holder)
        holder.addView(child)

        rootContainer.layout(0, 0, 500, 500)
        composeHost.layout(0, 0, 500, 500)
        interceptingHandler.layout(0, 0, 500, 500)
        holder.layout(0, 0, 500, 500)
        child.layout(0, 0, 100, 100)

        // Standard requestLayout() stops at holder because interceptingHandler swallows it
        child.requestLayout()
        assertThat(composeHost.isLayoutRequested).isFalse()
        assertThat(rootContainer.isLayoutRequested).isFalse()

        // Propagating past the intercepting container marks ancestors up to rootContainer as dirty
        child.propagateRequestLayoutToRoot()
        assertThat(composeHost.isLayoutRequested).isTrue()
        assertThat(rootContainer.isLayoutRequested).isTrue()
    }

    @Test
    fun withMaterial3ThemeFallback_wrapsContextOnlyWhenMaterial3ThemeIsMissing() {
        val fallbackContext = context.withMaterial3ThemeFallback()
        assertThat(fallbackContext).isNotSameInstanceAs(context)
        assertThat(
                fallbackContext.theme.resolveAttribute(
                    MaterialR.attr.colorSurfaceContainer,
                    TypedValue(),
                    true,
                )
            )
            .isTrue()

        val existingMaterial3Context =
            ContextThemeWrapper(context, MaterialR.style.Theme_Material3_DayNight)
        assertThat(existingMaterial3Context.withMaterial3ThemeFallback())
            .isSameInstanceAs(existingMaterial3Context)
    }
}
