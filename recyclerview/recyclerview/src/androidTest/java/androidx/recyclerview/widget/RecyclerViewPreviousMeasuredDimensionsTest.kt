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

import android.content.Context
import android.view.View
import android.view.View.MeasureSpec.UNSPECIFIED
import android.view.View.MeasureSpec.AT_MOST
import android.view.View.MeasureSpec.EXACTLY
import android.view.ViewGroup
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
@SmallTest
public class RecyclerViewPreviousMeasuredDimensionsTest(
    private val firstWidthMode: Int,
    private val firstHeightMode: Int,
    private val secondWidthMode: Int,
    private val secondHeightMode: Int
) {

    public companion object {
        @JvmStatic
        @Parameterized.Parameters(
            name = "firstWidthMode={0}, firstHeightMode={1}, " +
                "secondWidthMode={2}, secondHeightMode={3}"
        )
        public fun data(): Collection<Array<Int>> {
            val widthModes = listOf(
                UNSPECIFIED,
                EXACTLY,
                AT_MOST
            )

            val data = ArrayList<Array<Int>>()
            for (firstWidthMode in widthModes) {
                for (firstHeightMode in widthModes) {
                    for (secondWidthMode in widthModes) {
                        for (secondHeightMode in widthModes) {
                            data.add(
                                listOf(
                                    firstWidthMode,
                                    firstHeightMode,
                                    secondWidthMode,
                                    secondHeightMode
                                ).toTypedArray()
                            )
                        }
                    }
                }
            }
            return data
        }
    }

    @Test
    public fun previousMeasuredDimensionTest() {

        // Arrange

        val context = ApplicationProvider.getApplicationContext<Context>()
        val recyclerView = RecyclerView(context)
        recyclerView.layoutManager =
            LinearLayoutManager(context).apply {
                this.stackFromEnd = stackFromEnd
            }
        recyclerView.adapter = TestAdapter(context)

        // Assert 0

        assertThat(recyclerView.mState.previousMeasuredWidth).isEqualTo(0)
        assertThat(recyclerView.mState.previousMeasuredHeight).isEqualTo(0)

        // Act 1

        recyclerView.measure(
            View.MeasureSpec.makeMeasureSpec(100, firstWidthMode),
            View.MeasureSpec.makeMeasureSpec(100, firstHeightMode)
        )

        // Assert 1

        val expectedPreviousWidth1 = if (firstWidthMode == UNSPECIFIED) 300 else 100
        val expectedPreviousHeight1 = if (firstHeightMode == UNSPECIFIED) 300 else 100
        assertThat(recyclerView.mState.previousMeasuredWidth).isEqualTo(expectedPreviousWidth1)
        assertThat(recyclerView.mState.previousMeasuredHeight).isEqualTo(expectedPreviousHeight1)

        // Act 2

        recyclerView.measure(
            View.MeasureSpec.makeMeasureSpec(200, secondWidthMode),
            View.MeasureSpec.makeMeasureSpec(200, secondHeightMode)
        )

        // Assert 2

        val expectedPreviousWidth2 = if (secondWidthMode == UNSPECIFIED) 300 else 200
        val expectedPreviousHeight2 = if (secondHeightMode == UNSPECIFIED) 300 else 200
        assertThat(recyclerView.mState.previousMeasuredWidth).isEqualTo(expectedPreviousWidth2)
        assertThat(recyclerView.mState.previousMeasuredHeight).isEqualTo(expectedPreviousHeight2)

        // Act 3, layout should not have any affect on previous measured values.

        recyclerView.layout(0, 0, 500, 500)

        // Assert 3

        assertThat(recyclerView.mState.previousMeasuredWidth).isEqualTo(expectedPreviousWidth2)
        assertThat(recyclerView.mState.previousMeasuredHeight).isEqualTo(expectedPreviousHeight2)
    }

    internal class TestAdapter(val context: Context) :
        RecyclerView.Adapter<TestViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TestViewHolder {
            val view = View(context)
            view.layoutParams = ViewGroup.LayoutParams(300, 300)
            return TestViewHolder(view)
        }

        override fun onBindViewHolder(holder: TestViewHolder, position: Int) {
            holder.itemView.tag = position
        }

        override fun getItemCount(): Int {
            return 1
        }
    }

    internal class TestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}