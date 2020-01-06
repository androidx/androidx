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

import android.os.SystemClock.sleep
import android.util.Log
import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.testutils.PollingCheck
import androidx.viewpager2.widget.ViewPager2.ORIENTATION_HORIZONTAL
import androidx.viewpager2.widget.ViewPager2.SCROLL_STATE_IDLE
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class ChangeDataSetWhileScrollingTest : BaseTest() {
    private val orientation = ORIENTATION_HORIZONTAL
    private val adapterProvider = viewAdapterProviderValueId

    @Test
    fun test_regression01() {
        setUpTest(orientation).apply {
            val items = listOf("49", "51").toMutableList()
            setAdapterSync(adapterProvider.provider(items))
            assertBasicState(0, items[0])

            viewPager.post {
                viewPager.setCurrentItem(1, true)
            }

            viewPager.post {
                items.remove("51")
                viewPager.adapter!!.notifyDataSetChanged()
            }

            sleep(200) // introduce some delay, follow-up with pollingCheck

            PollingCheck.waitFor {
                viewPager.scrollState == SCROLL_STATE_IDLE && viewPager.currentItem == 0
            }

            PollingCheck.waitFor {
                debugInfo.run {
                    val isStable = firstCompletelyVisibleItemPosition == 0
                    if (!isStable) {
                        Log.d(
                            "ChangeDataSetWhileScrollingTest",
                            "Final item not reached despite VP2 idle. $debugInfo"
                        )
                    }
                    isStable
                }
            }

            Log.d("ChangeDataSetWhileScrollingTest", "$debugInfo")

            // confirming if unfinished RV animations are the source of flakiness
            PollingCheck.waitFor { !debugInfo.rvIsAnimating }

            Log.d("ChangeDataSetWhileScrollingTest", "$debugInfo")

            assertBasicState(0, "49")
        }
    }

    private data class DebugInfo(
        val firstCompletelyVisibleItemPosition: Int,
        val firstCompletelyVisibleItem: View?,
        val rvX: Int,
        val firstItemX: Int?,
        val rvScrollState: Int,
        val vpScrollState: Int,
        val rvIsAnimating: Boolean,
        val firstItemTranslationX: Float?
    )

    private val Context.debugInfo: DebugInfo
        get() {
            val recyclerView = viewPager.recyclerView
            val linearLayoutManager = viewPager.linearLayoutManager

            val firstCompletelyVisibleItemPosition =
                linearLayoutManager.findFirstCompletelyVisibleItemPosition()
            val firstCompletelyVisibleItem =
                linearLayoutManager.findViewByPosition(firstCompletelyVisibleItemPosition)

            return DebugInfo(
                firstCompletelyVisibleItemPosition,
                firstCompletelyVisibleItem,
                recyclerView.locationOnScreenX,
                firstCompletelyVisibleItem?.locationOnScreenX,
                recyclerView.scrollState,
                viewPager.scrollState,
                recyclerView.isAnimating,
                firstCompletelyVisibleItem?.translationX
            )
        }

    private val View.locationOnScreenX: Int
        get() = IntArray(2).let { result ->
            getLocationOnScreen(result)
            result[0]
        }
}
