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

package androidx.recyclerview.widget

import android.content.Context
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.testutils.ActivityScenarioResetRule
import androidx.testutils.ResettableActivityScenarioRule
import org.junit.Assert.assertEquals
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class RecyclerViewWithTransitionsTest {

    val activity: TestActivity get() = mActivityRule.getActivity()

    @Rule
    @JvmField
    val mActivityResetRule: ActivityScenarioResetRule<TestActivity> =
        TestActivity.ResetRule(mActivityRule.scenario)

    @Test
    fun ignoreCachedViewWhileItIsAttachedToOverlay() {
        val testAdapter = TestAdapter()
        val recyclerView = WrappedRecyclerView(activity).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = testAdapter
        }
        activity.runOnUiThread {
            activity.container.addView(recyclerView)
        }

        // helper fun to change itemCount, wait for it to be applied and validate childCount
        val changeItemCount = { itemsCount: Int ->
            activity.runOnUiThread {
                testAdapter.itemCount = itemsCount
                testAdapter.notifyDataSetChanged()
            }
            recyclerView.waitUntilLayout()
            assertEquals(testAdapter.itemCount, recyclerView.childCount)
        }

        changeItemCount(2)
        // save current children of RV. there are 2 items
        val savedChildren = List(recyclerView.childCount) { recyclerView.getChildAt(it) }

        changeItemCount(1)
        // now there is only one item. so one of saved views is free (without parent)
        val viewForOverlay = savedChildren.first { it.parent == null }
        // let's add it to another layout (overlay)
        val overlay = FrameLayout(activity).apply { addView(viewForOverlay) }

        changeItemCount(2)
        // it will crash if try to reuse viewForOverlay. so it won't and create a new view instead

        // lets free our viewForOverlay (like when transition finished)
        overlay.removeView(viewForOverlay)

        // request third view and validate viewForOverlay is used instead of creating a new one
        changeItemCount(3)
        assertEquals(recyclerView, viewForOverlay.parent)
    }

    private class TransitionHolder(context: Context) : RecyclerView.ViewHolder(TextView(context))

    private class TestAdapter : RecyclerView.Adapter<TransitionHolder>() {

        internal var itemCount = 0

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransitionHolder {
            return TransitionHolder(parent.context)
        }

        override fun onBindViewHolder(holder: TransitionHolder, position: Int) {}

        override fun getItemCount() = itemCount
    }

    companion object {
        @ClassRule
        @JvmField
        val mActivityRule = ResettableActivityScenarioRule(TestActivity::class.java)
    }
}
