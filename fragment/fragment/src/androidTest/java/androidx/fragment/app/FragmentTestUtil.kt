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

import android.graphics.Rect
import android.os.Build
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.fragment.test.R
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.testutils.runOnUiThreadRethrow
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.lang.ref.WeakReference
import java.util.ArrayList

fun FragmentTransaction.setReorderingAllowed(
    reorderingAllowed: ReorderingAllowed
) = setReorderingAllowed(reorderingAllowed is Reordered)

sealed class ReorderingAllowed {
    override fun toString(): String = this.javaClass.simpleName
}
object Reordered : ReorderingAllowed()
object Ordered : ReorderingAllowed()

@Suppress("DEPRECATION")
fun androidx.test.rule.ActivityTestRule<out FragmentActivity>.executePendingTransactions(
    fm: FragmentManager = activity.supportFragmentManager
): Boolean {
    var ret = false
    runOnUiThreadRethrow { ret = fm.executePendingTransactions() }
    return ret
}

inline fun <reified A : FragmentActivity> ActivityScenario<A>.executePendingTransactions(
    fm: FragmentManager = withActivity { supportFragmentManager }
) {
    onActivity { fm.executePendingTransactions() }
}

@Suppress("DEPRECATION")
fun androidx.test.rule.ActivityTestRule<out FragmentActivity>.popBackStackImmediate(): Boolean {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    var ret = false
    instrumentation.runOnMainSync {
        ret = activity.supportFragmentManager.popBackStackImmediate()
    }
    return ret
}

@Suppress("DEPRECATION")
fun androidx.test.rule.ActivityTestRule<out FragmentActivity>.popBackStackImmediate(
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

@Suppress("DEPRECATION")
fun androidx.test.rule.ActivityTestRule<out FragmentActivity>.popBackStackImmediate(
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

@Suppress("DEPRECATION")
fun androidx.test.rule.ActivityTestRule<out FragmentActivity>.setContentView(
    @LayoutRes layoutId: Int
) {
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

@Suppress("DEPRECATION")
// Transition test methods start
fun androidx.test.rule.ActivityTestRule<out FragmentActivity>.findGreen(): View {
    return activity.findViewById(R.id.greenSquare)
}

@Suppress("DEPRECATION")
fun androidx.test.rule.ActivityTestRule<out FragmentActivity>.findBlue(): View {
    return activity.findViewById(R.id.blueSquare)
}

@Suppress("DEPRECATION")
fun androidx.test.rule.ActivityTestRule<out FragmentActivity>.findRed(): View? {
    return activity.findViewById(R.id.redSquare)
}

val View.boundsOnScreen: Rect
    get() {
        val loc = IntArray(2)
        getLocationOnScreen(loc)
        return Rect(loc[0], loc[1], loc[0] + width, loc[1] + height)
    }

data class TransitionVerificationInfo(
    var epicenter: Rect? = null,
    val exitingViews: MutableList<View> = mutableListOf(),
    val enteringViews: MutableList<View> = mutableListOf()
)

fun TargetTracking.verifyAndClearTransition(block: TransitionVerificationInfo.() -> Unit) {
    val (epicenter, exitingViews, enteringViews) = TransitionVerificationInfo().apply { block() }

    assertThat(exitingTargets).containsExactlyElementsIn(exitingViews)
    assertThat(enteringTargets).containsExactlyElementsIn(enteringViews)
    if (epicenter == null) {
        assertThat(capturedEpicenter).isNull()
    } else {
        assertThat(capturedEpicenter).isEqualTo(epicenter)
    }
    clearTargets()
}

fun verifyNoOtherTransitions(fragment: TransitionFragment) {
    assertThat(fragment.enterTransition.enteringTargets).isEmpty()
    assertThat(fragment.enterTransition.exitingTargets).isEmpty()
    assertThat(fragment.exitTransition.enteringTargets).isEmpty()
    assertThat(fragment.exitTransition.exitingTargets).isEmpty()

    assertThat(fragment.reenterTransition.enteringTargets).isEmpty()
    assertThat(fragment.reenterTransition.exitingTargets).isEmpty()
    assertThat(fragment.returnTransition.enteringTargets).isEmpty()
    assertThat(fragment.returnTransition.exitingTargets).isEmpty()

    assertThat(fragment.sharedElementEnter.enteringTargets).isEmpty()
    assertThat(fragment.sharedElementEnter.exitingTargets).isEmpty()
    assertThat(fragment.sharedElementReturn.enteringTargets).isEmpty()
    assertThat(fragment.sharedElementReturn.exitingTargets).isEmpty()
}
// Transition test methods end

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
