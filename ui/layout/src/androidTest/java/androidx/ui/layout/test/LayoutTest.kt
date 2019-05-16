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
import org.junit.Assert.assertTrue
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
}
