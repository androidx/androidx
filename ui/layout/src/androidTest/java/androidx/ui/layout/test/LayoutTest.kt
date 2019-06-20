/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.layout.test

import android.app.Activity
import android.os.Handler
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.test.rule.ActivityTestRule
import androidx.ui.core.AndroidCraneView
import androidx.ui.core.CraneWrapper
import androidx.ui.core.Density
import androidx.ui.core.OnPositioned
import androidx.ui.core.PxPosition
import androidx.ui.core.PxSize
import androidx.ui.core.Ref
import androidx.ui.core.px
import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.composer
import androidx.compose.setContent
import androidx.ui.core.ComplexLayout
import androidx.ui.core.IntPx
import androidx.ui.core.ipx
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

open class LayoutTest {
    @get:Rule
    val activityTestRule = ActivityTestRule<TestActivity>(
        TestActivity::class.java
    )
    lateinit var activity: TestActivity
    lateinit var handler: Handler
    internal lateinit var density: Density

    @Before
    fun setup() {
        activity = activityTestRule.activity
        density = Density(activity)
        activity.hasFocusLatch.await(5, TimeUnit.SECONDS)

        // Kotlin IR compiler doesn't seem too happy with auto-conversion from
        // lambda to Runnable, so separate it here
        val runnable: Runnable = object : Runnable {
            override fun run() {
                handler = Handler()
            }
        }
        activityTestRule.runOnUiThread(runnable)
    }

    internal fun show(@Children composable: @Composable() () -> Unit) {
        val runnable: Runnable = object : Runnable {
            override fun run() {
                activity.setContent {
                    CraneWrapper {
                        composable()
                    }
                }
            }
        }
        activityTestRule.runOnUiThread(runnable)
    }

    internal fun findAndroidCraneView(): AndroidCraneView {
        return findAndroidCraneView(activity)
    }

    internal fun findAndroidCraneView(activity: Activity): AndroidCraneView {
        val contentViewGroup = activity.findViewById<ViewGroup>(android.R.id.content)
        return findAndroidCraneView(contentViewGroup)!!
    }

    internal fun findAndroidCraneView(parent: ViewGroup): AndroidCraneView? {
        for (index in 0 until parent.childCount) {
            val child = parent.getChildAt(index)
            if (child is AndroidCraneView) {
                return child
            } else if (child is ViewGroup) {
                val craneView = findAndroidCraneView(child)
                if (craneView != null) {
                    return craneView
                }
            }
        }
        return null
    }

    internal fun waitForDraw(view: View) {
        val viewDrawLatch = CountDownLatch(1)
        val listener = object : ViewTreeObserver.OnDrawListener {
            override fun onDraw() {
                viewDrawLatch.countDown()
            }
        }
        view.post(object : Runnable {
            override fun run() {
                view.viewTreeObserver.addOnDrawListener(listener)
                view.invalidate()
            }
        })
        assertTrue(viewDrawLatch.await(1, TimeUnit.SECONDS))
    }

    @Composable
    internal fun SaveLayoutInfo(
        size: Ref<PxSize>,
        position: Ref<PxPosition>,
        positionedLatch: CountDownLatch
    ) {
        OnPositioned(onPositioned = { coordinates ->
            size.value = PxSize(coordinates.size.width, coordinates.size.height)
            position.value = coordinates.localToGlobal(PxPosition(0.px, 0.px))
            positionedLatch.countDown()
        })
    }

    internal fun testIntrinsics(
        vararg layouts: @Composable() () -> Unit,
        test: ((IntPx) -> IntPx, (IntPx) -> IntPx, (IntPx) -> IntPx, (IntPx) -> IntPx) -> Unit
    ) {
        layouts.forEach { layout ->
            val layoutLatch = CountDownLatch(1)
            show {
                ComplexLayout(layout) {
                    layout { measurables, _ ->
                        val measurable = measurables.first()
                        test(
                            { h -> measurable.minIntrinsicWidth(h) },
                            { w -> measurable.minIntrinsicHeight(w) },
                            { h -> measurable.maxIntrinsicWidth(h) },
                            { w -> measurable.maxIntrinsicHeight(w) }
                        )
                        layoutLatch.countDown()
                    }
                    minIntrinsicWidth { _, _ -> 0.ipx }
                    maxIntrinsicWidth { _, _ -> 0.ipx }
                    minIntrinsicHeight { _, _ -> 0.ipx }
                    maxIntrinsicHeight { _, _ -> 0.ipx }
                }
            }
            assertTrue(layoutLatch.await(1, TimeUnit.SECONDS))
        }
    }

    internal fun assertEquals(expected: PxSize?, actual: PxSize?) {
        assertNotNull("Null expected size", expected)
        expected as PxSize
        assertNotNull("Null actual size", actual)
        actual as PxSize

        assertEquals(
            "Expected width ${expected.width.value} but obtained ${actual.width.value}",
            expected.width.value,
            actual.width.value,
            1f
        )
        assertEquals(
            "Expected height ${expected.height.value} but obtained ${actual.height.value}",
            expected.height.value,
            actual.height.value,
            1f
        )
        if (actual.width.value != actual.width.value.toInt().toFloat()) {
            fail("Expected integer width")
        }
        if (actual.height.value != actual.height.value.toInt().toFloat()) {
            fail("Expected integer height")
        }
    }

    internal fun assertEquals(expected: PxPosition?, actual: PxPosition?) {
        assertNotNull("Null expected position", expected)
        expected as PxPosition
        assertNotNull("Null actual position", actual)
        actual as PxPosition

        assertEquals(
            "Expected x ${expected.x.value} but obtained ${actual.x.value}",
            expected.x.value,
            actual.x.value,
            1f
        )
        assertEquals(
            "Expected y ${expected.y.value} but obtained ${actual.y.value}",
            expected.y.value,
            actual.y.value,
            1f
        )
        if (actual.x.value != actual.x.value.toInt().toFloat()) {
           fail("Expected integer x coordinate")
        }
        if (actual.y.value != actual.y.value.toInt().toFloat()) {
            fail("Expected integer y coordinate")
        }
    }

    internal fun assertEquals(expected: IntPx, actual: IntPx) {
        assertEquals(
            "Expected ${expected} but obtained ${actual}",
            expected.value.toFloat(),
            actual.value.toFloat(),
            1f
        )
    }
}
