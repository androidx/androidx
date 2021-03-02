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
import android.view.ViewGroup
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.IllegalArgumentException

@RunWith(AndroidJUnit4::class)
@SmallTest
public class LinearLayoutManagerMeasureAnchorTest {

    @Test
    public fun testStackFromEndVertical() {
        test(true, false, true)
    }

    @Test
    public fun testReverseLayoutVertical() {
        test(false, true, true)
    }

    @Test
    public fun testStackFromEndHorizontal() {
        test(true, false, false)
    }

    @Test
    public fun testReverseLayoutHorizontal() {
        test(false, true, false)
    }

    public fun test(reverseLayout: Boolean, stackFromEnd: Boolean, vertical: Boolean) {

        if (!(reverseLayout xor stackFromEnd)) {
            throw IllegalArgumentException("Must be reverseLayout xor stackFromEnd")
        }

        // Arrange

        val context = ApplicationProvider.getApplicationContext<Context>()
        val recyclerView = RecyclerView(context)
        val orientation = if (vertical) RecyclerView.VERTICAL else RecyclerView.HORIZONTAL
        recyclerView.layoutManager =
            LinearLayoutManager(context, orientation, reverseLayout).apply {
                this.stackFromEnd = stackFromEnd
            }
        recyclerView.adapter = TestAdapter(context, vertical)

        // Act

        recyclerView.measure(
            View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.AT_MOST),
            View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.AT_MOST)
        )
        recyclerView.measure(
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.AT_MOST),
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.AT_MOST)
        )
        recyclerView.layout(0, 0, 100, 100)

        // Assert

        val view: View? =
            (0 until recyclerView.childCount)
                .map { recyclerView.getChildAt(it) }
                .find { it.tag == if (reverseLayout) 0 else 9 }

        assertThat(view).isNotNull()
        if (vertical) {
            assertThat(view!!.top).isEqualTo(0)
        } else {
            assertThat(view!!.left).isEqualTo(0)
        }
    }

    internal class TestAdapter(val context: Context, val vertical: Boolean) :
        RecyclerView.Adapter<TestViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TestViewHolder {
            val view = View(context)
            view.layoutParams = if (vertical) {
                ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 100)
            } else {
                ViewGroup.LayoutParams(100, ViewGroup.LayoutParams.MATCH_PARENT)
            }
            return TestViewHolder(view)
        }

        override fun onBindViewHolder(holder: TestViewHolder, position: Int) {
            holder.itemView.tag = position
        }

        override fun getItemCount(): Int {
            return 10
        }
    }

    internal class TestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}