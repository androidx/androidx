/*
 * Copyright 2018 The Android Open Source Project
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
package androidx.fragment.app

import android.app.Activity
import android.os.Looper
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import com.google.common.truth.Truth.assertWithMessage

fun ActivityTestRule<out FragmentActivity>.waitForExecution() {
    // Wait for two cycles. When starting a postponed transition, it will post to
    // the UI thread and then the execution will be added onto the queue after that.
    // The two-cycle wait makes sure fragments have the opportunity to complete both
    // before returning.
    try {
        runOnUiThread {}
        runOnUiThread {}
    } catch (throwable: Throwable) {
        throw RuntimeException(throwable)
    }
}

fun ActivityTestRule<out Activity>.runOnUiThreadRethrow(block: () -> Unit) {
    if (Looper.getMainLooper() == Looper.myLooper()) {
        block()
    } else {
        try {
            runOnUiThread {
                block()
            }
        } catch (t: Throwable) {
            throw RuntimeException(t)
        }
    }
}

fun ActivityTestRule<out FragmentActivity>.executePendingTransactions(
    fm: FragmentManager = activity.supportFragmentManager
): Boolean {
    var ret = false
    runOnUiThreadRethrow { ret = fm.executePendingTransactions() }
    return ret
}

fun ActivityTestRule<out FragmentActivity>.popBackStackImmediate(): Boolean {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    var ret = false
    instrumentation.runOnMainSync {
        ret = activity.supportFragmentManager.popBackStackImmediate()
    }
    return ret
}

fun ActivityTestRule<out FragmentActivity>.popBackStackImmediate(
    id: Int,
    flags: Int = 0
): Boolean {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    var ret = false
    instrumentation.runOnMainSync {
        ret = activity.supportFragmentManager.popBackStackImmediate(id, flags)
    }
    return ret
}

fun ActivityTestRule<out FragmentActivity>.popBackStackImmediate(
    name: String,
    flags: Int = 0
): Boolean {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    var ret = false
    instrumentation.runOnMainSync {
        ret = activity.supportFragmentManager.popBackStackImmediate(name, flags)
    }
    return ret
}

fun ActivityTestRule<out FragmentActivity>.setContentView(@LayoutRes layoutId: Int) {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    instrumentation.runOnMainSync { activity.setContentView(layoutId) }
}

fun assertChildren(container: ViewGroup, vararg fragments: Fragment) {
    val numFragments = fragments.size
    assertWithMessage("There aren't the correct number of fragment Views in its container")
        .that(container.childCount)
        .isEqualTo(numFragments)
    fragments.forEachIndexed { index, fragment ->
        assertWithMessage("Wrong Fragment View order for [$index]")
            .that(fragment.requireView())
            .isSameAs(container.getChildAt(index))
    }
}

inline fun <reified A : Activity, T : Any> ActivityScenario<A>.withActivity(
    crossinline block: A.() -> T
): T {
    lateinit var value: T
    onActivity { activity ->
        value = block(activity)
    }
    return value
}
