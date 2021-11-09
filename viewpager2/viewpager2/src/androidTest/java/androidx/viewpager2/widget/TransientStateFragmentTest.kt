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

package androidx.viewpager2.widget

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.viewpager2.widget.ViewPager2.ORIENTATION_HORIZONTAL
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit.MILLISECONDS

/**
 * Verifies that [androidx.viewpager2.adapter.FragmentStateAdapter] can handle [Fragment]s
 * having transient state.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class TransientStateFragmentTest : BaseTest() {
    private val orientation = ORIENTATION_HORIZONTAL
    private val totalPages = 10
    private val adapterProvider = fragmentAdapterProviderValueId
    private val timeoutMs = 3000L

    @Test
    @SdkSuppress(minSdkVersion = 16) /** [View.setHasTransientState] was introduced in API 16 */
    fun test_swipeBetweenPages() {
        setUpTest(orientation).apply {
            val expectedValues = stringSequence(totalPages)
            val adapter = adapterProvider.provider(expectedValues)

            val fragmentManager = activity.supportFragmentManager

            val transientStateCallback = createTransientStateCallback()
            fragmentManager.registerFragmentLifecycleCallbacks(transientStateCallback, false)
            setAdapterSync(adapter)

            assertBasicState(0)
            listOf(1, 0, 1, 2, 3, 4, 3).plus(4 until totalPages).forEach { target ->
                val latch = viewPager.addWaitForIdleLatch()
                swipe(viewPager.currentItem, target)
                latch.await(timeoutMs, MILLISECONDS)
                assertBasicState(target)
            }

            fragmentManager.unregisterFragmentLifecycleCallbacks(transientStateCallback)
        }
    }

    private fun createTransientStateCallback(): FragmentManager.FragmentLifecycleCallbacks {
        return object : FragmentManager.FragmentLifecycleCallbacks() {
            override fun onFragmentViewCreated(
                fm: FragmentManager,
                f: Fragment,
                v: View,
                savedInstanceState: Bundle?
            ) {
                v.setHasTransientState(true)
            }
        }
    }
}
