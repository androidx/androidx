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
            setAdapterSync(adapterProvider(items))
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

            // investigating b/141486375 TODO: clean-up once investigation finished
            PollingCheck.waitFor {
                val firstCompletelyVisible =
                    viewPager.linearLayoutManager.findFirstCompletelyVisibleItemPosition()
                val isStable = firstCompletelyVisible == 0

                if (!isStable) {
                    val isAnimating = viewPager.recyclerView.isAnimating
                    val rvScrollState = viewPager.recyclerView.scrollState
                    Log.d(
                        "ChangeDataSetWhileScrollingTest",
                        "rv.isAnimating:$isAnimating" +
                                " | firstCompletelyVisible:$firstCompletelyVisible" +
                                " | rv.scrollState:$rvScrollState"
                    )
                }

                isStable
            }

            assertBasicState(0, "49")
        }
    }
}
