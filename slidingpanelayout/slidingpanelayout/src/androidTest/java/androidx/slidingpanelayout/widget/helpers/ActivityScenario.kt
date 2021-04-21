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

package androidx.slidingpanelayout.widget.helpers

import android.app.Activity
import android.view.View
import androidx.annotation.IdRes
import androidx.slidingpanelayout.widget.SlidingPaneLayout
import androidx.test.core.app.ActivityScenario
import androidx.testutils.withActivity
import java.util.concurrent.CountDownLatch

public inline fun <reified A : Activity> ActivityScenario<A>.findViewX(
    @IdRes resId: Int
): Float {
    return withActivity { findViewById<View>(resId).x }
}

public inline fun <reified A : Activity> ActivityScenario<A>.findViewById(@IdRes resId: Int): View {
    return withActivity { findViewById(resId) }
}

public inline fun <reified A : Activity> ActivityScenario<A>.addWaitForOpenLatch(
    @IdRes resId: Int
): CountDownLatch {
    val latch = CountDownLatch(1)
    withActivity {
        val slidingPaneLayout = findViewById<SlidingPaneLayout>(resId)
        slidingPaneLayout.addPanelSlideListener(object : SlidingPaneLayout.PanelSlideListener {
            override fun onPanelSlide(panel: View, slideOffset: Float) {}
            override fun onPanelOpened(panel: View) {
                latch.countDown()
                slidingPaneLayout.removePanelSlideListener(this)
            }
            override fun onPanelClosed(panel: View) {}
        })
    }
    return latch
}

public inline fun <reified A : Activity> ActivityScenario<A>.addWaitForCloseLatch(
    @IdRes resId: Int
): CountDownLatch {
    val latch = CountDownLatch(1)
    withActivity {
        val slidingPaneLayout = findViewById<SlidingPaneLayout>(resId)
        slidingPaneLayout.addPanelSlideListener(object : SlidingPaneLayout.PanelSlideListener {
            override fun onPanelSlide(panel: View, slideOffset: Float) {}
            override fun onPanelOpened(panel: View) {}
            override fun onPanelClosed(panel: View) {
                latch.countDown()
                slidingPaneLayout.removePanelSlideListener(this)
            }
        })
    }
    return latch
}

public inline fun <reified A : Activity> ActivityScenario<A>.addWaitForSlideLatch(
    @IdRes resId: Int
): CountDownLatch {
    val latch = CountDownLatch(1)
    withActivity {
        val slidingPaneLayout = findViewById<SlidingPaneLayout>(resId)
        slidingPaneLayout.addPanelSlideListener(object : SlidingPaneLayout.PanelSlideListener {
            override fun onPanelSlide(panel: View, slideOffset: Float) {
                latch.countDown()
                slidingPaneLayout.removePanelSlideListener(this)
            }
            override fun onPanelOpened(panel: View) {}
            override fun onPanelClosed(panel: View) {}
        })
    }
    return latch
}