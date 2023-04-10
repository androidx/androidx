/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.appcompat.widget

import android.content.Context
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.MarginLayoutParams
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class LinearLayoutCompatTest {

    @Test
    fun testGenerateLayoutParamsFromMarginLayoutParams() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val layout = LinearLayoutCompatImpl(ctx)
        val margin = MarginLayoutParams(MATCH_PARENT, MATCH_PARENT).apply { bottomMargin = 10 }
        val params = layout.generateLayoutParams(margin)
        assertEquals(margin.bottomMargin, params.bottomMargin)
    }

    class LinearLayoutCompatImpl(ctx: Context) : LinearLayoutCompat(ctx) {
        public override fun generateLayoutParams(p: ViewGroup.LayoutParams): LayoutParams {
            return super.generateLayoutParams(p)
        }
    }
}
