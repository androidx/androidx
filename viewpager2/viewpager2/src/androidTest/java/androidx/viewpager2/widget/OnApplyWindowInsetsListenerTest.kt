/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.viewpager2.widget

import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.WindowInsets
import android.widget.FrameLayout
import androidx.core.graphics.Insets
import androidx.core.view.DisplayCutoutCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2.ORIENTATION_HORIZONTAL
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThat
import org.junit.Test
import java.lang.reflect.Field

private const val numPages = 3

@LargeTest
@SdkSuppress(minSdkVersion = 21)
class OnApplyWindowInsetsListenerTest : BaseTest() {

    companion object {
        var mSystemWindowInsetsConsumedField: Field? = null
        var mStableInsetsConsumedField: Field? = null

        init {
            // Only need reflection on API < 29 to create an unconsumed WindowInsets.
            // On API 29+, a new builder is used that will do that for us.
            if (Build.VERSION.SDK_INT < 29) {
                mSystemWindowInsetsConsumedField = field("mSystemWindowInsetsConsumed")
                mSystemWindowInsetsConsumedField!!.isAccessible = true
                mStableInsetsConsumedField = field("mStableInsetsConsumed")
                mStableInsetsConsumedField!!.isAccessible = true
            }
        }

        private fun field(name: String): Field {
            // Need to use double reflection for API 28
            val getDeclaredField = Class::class.java
                .getDeclaredMethod("getDeclaredField", String::class.java)
            return getDeclaredField.invoke(WindowInsets::class.java, name) as Field
        }
    }

    private val fragments = Array<InsetsConsumingFragment?>(3) { null }

    @Test
    fun consumedInsets_notPropagated_toSiblings() {
        // Basic setup of the test
        val test = setUpTest(ORIENTATION_HORIZONTAL)
        test.runOnUiThreadSync {
            test.viewPager.offscreenPageLimit = numPages
        }
        test.setAdapterSync { FragmentAdapter(it) }

        // Create an insets object with all values filled in
        val windowInsets = createWindowInsets()
        // Test relies on the created WindowInsets being unconsumed (by default they aren't)
        assertThat(windowInsets.isConsumed, equalTo(false))
        // Create a defensive copy; the mutability of WindowInsets has changed over API levels
        val expectedWindowInsets = WindowInsetsCompat.Builder(windowInsets).build()

        // Dispatch the insets
        test.runOnUiThreadSync {
            ViewCompat.dispatchApplyWindowInsets(test.viewPager, windowInsets)
        }

        // Check expected values
        fragments.forEachIndexed { i, fragment ->
            assertNotNull(fragment)
            val recordedInsets = fragment!!.recordedInsets
            // ViewPager2's WindowInsets dispatcher should consume all insets at the end to
            // prevent the default dispatcher to traverse further down the hierarchy, as VP2
            // has already done that.
            // If we record more than 1 insets being applied on a page, it means that VP2's
            // dispatcher didn't stop the default dispatcher
            assertThat(
                "WindowInsets were dispatched ${recordedInsets.size} times to page $i. " +
                    "Expected only 1 dispatch",
                recordedInsets.size,
                equalTo(1)
            )
            assertThat(
                "Page $i received modified insets:\n" +
                    "expected: $expectedWindowInsets\n" +
                    "actual:   ${recordedInsets[0]}",
                insetsEquals(recordedInsets[0], expectedWindowInsets),
                equalTo(true)
            )
        }
    }

    // TODO(b/153341849): WindowInsetsCompat.equals was fixed in androidx.core:core:1.3.0-beta02.
    //  Remove this method when we move to that version.
    private fun insetsEquals(a: WindowInsetsCompat, b: WindowInsetsCompat): Boolean {
        return if (Build.VERSION.SDK_INT >= 28) {
            // WindowInsetsCompat.equals calls through to WindowInsets.equals,
            // which wasn't introduced until API 28
            a == b
        } else {
            // Otherwise, check what we can access (and what was present < 28)
            a.isConsumed == b.isConsumed &&
                a.isRound == b.isRound &&
                a.systemWindowInsets == b.systemWindowInsets &&
                a.stableInsets == b.stableInsets
        }
    }

    private fun createWindowInsets(): WindowInsetsCompat {
        val insets = Insets.of(10, 10, 10, 10)
        val rect = Rect(2, 2, 2, 2)
        val windowInsets = WindowInsetsCompat.Builder()
            .setSystemWindowInsets(insets)
            .setSystemGestureInsets(insets)
            .setStableInsets(insets)
            .setTappableElementInsets(insets)
            .setMandatorySystemGestureInsets(insets)
            .setDisplayCutout(DisplayCutoutCompat(rect, listOf(rect, rect, rect, rect)))
            .build()
        if (Build.VERSION.SDK_INT < 29) {
            // On API < 29 the builder creates a consumed WindowInsets.
            // Use reflection to make it unconsumed, or it won't be dispatched at all.
            val platformInsets = windowInsets.toWindowInsets()
            mSystemWindowInsetsConsumedField!!.set(platformInsets, false)
            mStableInsetsConsumedField!!.set(platformInsets, false)
        }
        return windowInsets
    }

    inner class FragmentAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int {
            return numPages
        }

        override fun createFragment(position: Int): Fragment {
            return InsetsConsumingFragment().also { fragments[position] = it }
        }
    }

    class InsetsConsumingFragment : Fragment() {
        val recordedInsets = mutableListOf<WindowInsetsCompat>()

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            return FrameLayout(inflater.context).apply {
                layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
                    recordedInsets.add(insets)
                    insets.consumeSystemWindowInsets()
                        .consumeStableInsets()
                        .consumeDisplayCutout()
                }
            }
        }
    }
}
