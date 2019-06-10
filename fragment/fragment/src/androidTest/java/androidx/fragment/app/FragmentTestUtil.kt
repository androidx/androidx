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

import android.os.Build
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.testutils.runOnUiThreadRethrow
import com.google.common.truth.Truth.assertWithMessage
import java.lang.ref.WeakReference
import java.util.ArrayList

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
            .isSameInstanceAs(container.getChildAt(index))
    }
}

/**
 * Allocates until a garbage collection occurs.
 */
fun forceGC() {
    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1) {
        // The following works on O+
        Runtime.getRuntime().gc()
        Runtime.getRuntime().gc()
        Runtime.getRuntime().runFinalization()
    } else {
        // The following works on older versions
        for (i in 0..1) {
            // Use a random index in the list to detect the garbage collection each time because
            // .get() may accidentally trigger a strong reference during collection.
            val leak = ArrayList<WeakReference<ByteArray>>()
            do {
                val arr = WeakReference(ByteArray(100))
                leak.add(arr)
            } while (leak[(Math.random() * leak.size).toInt()].get() != null)
        }
    }
}
