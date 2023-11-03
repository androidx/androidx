/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.slidingpanelayout.widget

import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class DisallowOverlapTest {
    @Test
    fun twoPaneModeTest() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val spl = SlidingPaneLayout(context).apply {
            isOverlappingEnabled = false
        }

        val firstPane = View(context).also {
            spl.addView(it, SlidingPaneLayout.LayoutParams(30, MATCH_PARENT))
        }
        val secondPane = View(context).also {
            spl.addView(it, SlidingPaneLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        }

        spl.measure(
            MeasureSpec.makeMeasureSpec(100, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(100, MeasureSpec.EXACTLY)
        )

        assertThat(spl.measuredWidth).isEqualTo(100)
        assertThat(spl.measuredHeight).isEqualTo(100)

        spl.layout(0, 0, spl.measuredWidth, spl.measuredHeight)

        assertThat(firstPane.measuredWidth).isEqualTo(30)
        assertThat(secondPane.measuredWidth).isEqualTo(70)
    }
}
