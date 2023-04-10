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

package androidx.recyclerview.widget

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.testutils.ResettableActivityScenarioRule
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.ClassRule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class RecycledViewPoolInstrumentedTest {

    companion object {
        // One activity launch per test class
        @ClassRule
        @JvmField
        var mActivityRule = ResettableActivityScenarioRule(
            TestActivity::class.java
        )
    }

    @Test
    fun attachToWindow_setAdapter() {
        doTest(
            Step.AttachToWindow to 0,
            Step.SetAdapter to 1,
            Step.SetPool to 1
        )
    }

    @Test
    fun attachToWindow_setPool_setAdapter() {
        doTest(
            Step.AttachToWindow to null,
            Step.SetPool to 0,
            Step.SetAdapter to 1,
            Step.SetPool to 1,
        )
    }

    @Test
    fun setAdapter_attachToWindow() {
        doTest(
            Step.SetAdapter to 0,
            Step.AttachToWindow to 1,
            Step.SetPool to 1,
        )
    }

    @Test
    fun setAdapter_setPool_attachToWindow() {
        doTest(
            Step.SetAdapter to 0,
            Step.SetPool to 0,
            Step.AttachToWindow to 1,
            Step.SetPool to 1,
        )
    }

    @Test
    fun setPool_attachToWindow_setAdapter() {
        doTest(
            Step.SetPool to 0,
            Step.AttachToWindow to 0,
            Step.SetAdapter to 1,
            Step.SetPool to 1,
        )
    }

    @Test
    fun setPool_setAdapter_attachToWindow() {
        doTest(
            Step.SetPool to 0,
            Step.SetAdapter to 0,
            Step.AttachToWindow to 1,
            Step.SetPool to 1,
        )
    }

    @Test
    fun twoRecyclerViews_poolAddedBeforeAttach() {
        doTwoRecyclerViewsTest(poolAddedBeforeAttach = true)
    }

    @Test
    fun twoRecyclerViews_poolAddedAfterAttach() {
        doTwoRecyclerViewsTest(poolAddedBeforeAttach = false)
    }

    private fun doTwoRecyclerViewsTest(poolAddedBeforeAttach: Boolean) {
        lateinit var parent: LinearLayout
        lateinit var rv1: RecyclerView
        lateinit var rv2: RecyclerView
        lateinit var pool: RecyclerView.RecycledViewPool
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            parent = LinearLayout(activity).also {
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
            activity.setContentView(parent)

            rv1 = RecyclerView(activity)
            rv2 = RecyclerView(activity)

            pool = RecyclerView.RecycledViewPool()

            rv1.adapter = TestAdapter()
            rv2.adapter = TestAdapter()

            if (poolAddedBeforeAttach) {
                rv1.setRecycledViewPool(pool)
                rv2.setRecycledViewPool(pool)
            }
            parent.addView(rv1)
            parent.addView(rv2)
        }

        val expectedCount = if (poolAddedBeforeAttach) 2 else 1
        assertThat(rv1.attachCount).isEqualTo(expectedCount)
        assertThat(rv2.attachCount).isEqualTo(expectedCount)

        if (!poolAddedBeforeAttach) {
            rv1.setRecycledViewPool(pool)
            rv2.setRecycledViewPool(pool)
            assertThat(rv1.attachCount).isEqualTo(2)
            assertThat(rv2.attachCount).isEqualTo(2)
        }
    }

    enum class Step {
        SetAdapter,
        SetPool,
        AttachToWindow,
    }

    private fun doTest(vararg steps: Pair<Step, Int?>) {
        lateinit var parent: LinearLayout
        lateinit var rv: RecyclerView

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            parent = LinearLayout(activity).also {
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
            activity.setContentView(parent)

            rv = RecyclerView(activity)
        }

        steps.forEachIndexed { index, step ->
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                when (step.first) {
                    Step.SetAdapter -> {
                        rv.adapter = TestAdapter()
                    }
                    Step.SetPool -> {
                        rv.setRecycledViewPool(RecyclerView.RecycledViewPool())
                    }
                    Step.AttachToWindow -> {
                        parent.addView(rv)
                    }
                }
            }
            if (step.second != null) {
                assertWithMessage("At step $index, attach count was incorrect")
                    .that(rv.attachCount)
                    .isEqualTo(step.second)
            }
        }

        // Remove the adapter, make sure it goes back to 0
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            rv.adapter = null
        }
        assertThat(rv.attachCount).isEqualTo(0)
    }

    private val activity get() = mActivityRule.getActivity()

    private inner class TestAdapter : RecyclerView.Adapter<TestAdapter.ViewHolder>() {
        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(View(activity))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        }

        override fun getItemCount(): Int = 10
    }

    private val RecyclerView.attachCount: Int
        get() = this.recycledViewPool.mAttachedAdaptersForPoolingContainer.size
}