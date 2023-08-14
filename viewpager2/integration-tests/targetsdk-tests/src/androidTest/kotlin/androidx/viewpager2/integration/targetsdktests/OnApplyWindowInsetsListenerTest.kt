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

package androidx.viewpager2.integration.targetsdktests

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.WindowInsets
import android.widget.LinearLayout
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.ORIENTATION_HORIZONTAL
import androidx.viewpager2.widget.WindowInsetsApplier
import java.lang.reflect.Field
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.assertNotNull
import org.junit.Assume
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@SdkSuppress(minSdkVersion = 30) // TODO(b/273945673): fix test on API 21..30
@RunWith(Parameterized::class)
class OnApplyWindowInsetsListenerTest(private val config: TestConfig) {

    data class TestConfig(
        val applyFix: Boolean,
        val pagesConsumeInsets: Boolean
    )

    companion object {
        private const val numPages = 3
        private val mSystemWindowInsetsConsumedField: Field? by lazy {
            // Only need reflection on API < 29 to create an unconsumed WindowInsets.
            // On API 29+, a new builder is used that will do that for us.
            if (Build.VERSION.SDK_INT < 29) {
                field("mSystemWindowInsetsConsumed").also { it.isAccessible = true }
            } else null
        }

        @Suppress("SameParameterValue")
        private fun field(name: String): Field {
            // Need to use double reflection for API 28
            val getDeclaredField = Class::class.java
                .getDeclaredMethod("getDeclaredField", String::class.java)
            return getDeclaredField.invoke(WindowInsets::class.java, name) as Field
        }

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params() = mutableListOf<TestConfig>().apply {
            for (applyFix in listOf(true, false)) {
                for (pagesConsumeInsets in listOf(true, false)) {
                    add(TestConfig(applyFix, pagesConsumeInsets))
                }
            }
        }
    }

    @get:Rule
    val activityTestRule = ActivityScenarioRule(FragmentActivity::class.java)

    private val fragments = Array<InsetsRecordingFragment?>(3) { null }
    private lateinit var viewRoot: ViewGroup
    private lateinit var viewPager: ViewPager2
    private val siblingInsets = mutableListOf<WindowInsetsCompat>()

    // Create an insets object with non-zero insets
    private val dispatchedWindowInsets = createWindowInsets()

    @Test
    fun test() {
        // Broken on UDC, but fixed on UDC-QPR; no easy way to differentiate in a test, so
        // disabling for the whole API 34. See b/284406283 for more context.
        Assume.assumeTrue(Build.VERSION.SDK_INT != 34)
        setupTest(config.applyFix, config.pagesConsumeInsets)
        runTest()
        checkResult(
            hasAppliedFix = config.applyFix,
            pagesConsumeInsets = config.pagesConsumeInsets
        )
    }

    private fun setupTest(applyFix: Boolean, consumeInsetsInPages: Boolean) {
        // Setup the test
        activityTestRule.scenario.onActivity {
            viewRoot = LinearLayout(it).apply {
                layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                orientation = LinearLayout.VERTICAL
            }

            viewPager = ViewPager2(it).apply {
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f)
                orientation = ORIENTATION_HORIZONTAL
                offscreenPageLimit = numPages
                adapter = FragmentAdapter(it, consumeInsetsInPages)
                if (applyFix) {
                    WindowInsetsApplier.install(this)
                }
                viewRoot.addView(this)
            }

            InsetsRecordingView(it, siblingInsets, false).apply {
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, 20)
                tag = "SIBLING"
                viewRoot.addView(this)
            }

            it.setContentView(viewRoot)
        }
    }

    private fun runTest() {
        // Create a defensive copy; the mutability of WindowInsets has changed over API levels
        val windowInsets = WindowInsetsCompat.Builder(dispatchedWindowInsets).build()
        // Test relies on the created WindowInsets being unconsumed (by default they aren't)
        assertThat(windowInsets.isConsumed, equalTo(false))

        // Dispatch the insets
        activityTestRule.scenario.onActivity {
            ViewCompat.dispatchApplyWindowInsets(viewRoot, windowInsets)
        }
    }

    private fun checkResult(
        hasAppliedFix: Boolean,
        pagesConsumeInsets: Boolean
    ) {
        val appInfo = InstrumentationRegistry.getInstrumentation().targetContext.applicationInfo
        val frameworkIsFixed = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            appInfo.targetSdkVersion >= Build.VERSION_CODES.R

        // Check expected results for pages
        fragments.forEachIndexed { i, fragment ->
            assertNotNull(fragment)
            val recordedInsets = fragment!!.recordedInsets

            // Note: the first page always has the same insets,
            // as nothing was before it to consume anything
            val isFirstToReceiveInsets = i == 0

            val expectInsetsOnAllPages =
                isFirstToReceiveInsets || // Nothing consumed insets yet
                    frameworkIsFixed || // Should always work
                    hasAppliedFix || // We fixed it!
                    !pagesConsumeInsets // Nothing consumes insets ever

            val expectedRecordedSize = if (expectInsetsOnAllPages) 1 else 0
            assertThat(
                "WindowInsets were dispatched an incorrect number of times to page $i:\n" +
                    "expected: $expectedRecordedSize time\n" +
                    "actual:   ${recordedInsets.size} time(s)",
                recordedInsets.size,
                equalTo(expectedRecordedSize)
            )

            if (expectInsetsOnAllPages) {
                // Expect same insets
                @Suppress("DEPRECATION")
                assertThat(
                    "Page $i received modified insets:\n" +
                        "expected: ${recordedInsets[0].systemWindowInsets}\n" +
                        "actual:   ${dispatchedWindowInsets.systemWindowInsets}",
                    recordedInsets[0],
                    equalTo(dispatchedWindowInsets)
                )
            }
            // else: no insets recorded, so nothing to check
        }

        // Check expected results for the sibling
        val expectInsetsInSibling =
            frameworkIsFixed || // Should always work
                (!hasAppliedFix && // Our fix blocks propagation to siblings
                    !pagesConsumeInsets) // Nothing consumes insets ever

        val expectedRecordedSize = if (expectInsetsInSibling) 1 else 0
        assertThat(
            "WindowInsets were dispatched an incorrect number of times to the ViewPager2's " +
                "sibling:\n" +
                "expected: $expectedRecordedSize time\n" +
                "actual:   ${siblingInsets.size} time(s)",
            siblingInsets.size,
            equalTo(expectedRecordedSize)
        )

        if (expectInsetsInSibling) {
            // Expect same insets
            @Suppress("DEPRECATION")
            assertThat(
                "ViewPager2's sibling received modified insets:\n" +
                    "expected: ${siblingInsets[0].systemWindowInsets}\n" +
                    "actual:   ${dispatchedWindowInsets.systemWindowInsets}",
                siblingInsets[0],
                equalTo(dispatchedWindowInsets)
            )
        }
    }

    private fun createWindowInsets(): WindowInsetsCompat {
        val insets = Insets.of(10, 11, 12, 13)
        @Suppress("DEPRECATION")
        val windowInsets = WindowInsetsCompat.Builder().setSystemWindowInsets(insets).build()
        if (Build.VERSION.SDK_INT < 29) {
            // On API < 29 the builder creates a consumed WindowInsets.
            // Use reflection to make it unconsumed, or it won't be dispatched at all.
            val platformInsets = windowInsets.toWindowInsets()
            mSystemWindowInsetsConsumedField!!.set(platformInsets, false)
        }
        return windowInsets
    }

    private inner class FragmentAdapter(
        activity: FragmentActivity,
        private val consumeInsets: Boolean
    ) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int {
            return numPages
        }

        override fun createFragment(position: Int): Fragment {
            return InsetsRecordingFragment(consumeInsets).also { fragments[position] = it }
        }
    }

    class InsetsRecordingFragment(private val consumeInsets: Boolean) : Fragment() {
        val recordedInsets = mutableListOf<WindowInsetsCompat>()

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {
            return InsetsRecordingView(inflater.context, recordedInsets, consumeInsets).apply {
                layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                tag = "PAGE"
            }
        }
    }

    private class InsetsRecordingView(
        context: Context,
        private val recordedInsets: MutableList<WindowInsetsCompat>,
        private val consumeInsets: Boolean
    ) : View(context) {
        init {
            ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
                // Record a copy, in case they get modified down the line
                recordedInsets.add(WindowInsetsCompat(insets))
                if (consumeInsets) {
                    @Suppress("DEPRECATION")
                    insets.consumeSystemWindowInsets()
                } else {
                    insets
                }
            }
        }
    }
}
