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

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.LayerDrawable
import androidx.appcompat.resources.test.R
import androidx.core.content.ContextCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertArrayEquals
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class ResourceManagerInternalTest {
    /** Test the workaround for b/231320562 where LayerDrawable loses state on mutate(). */
    @Test
    fun testLayerDrawableStatePreservedOnTint() {
        val state = intArrayOf(android.R.attr.state_enabled, android.R.attr.state_checked)

        val context = InstrumentationRegistry.getInstrumentation().context
        val drawable = ContextCompat.getDrawable(context, R.drawable.layer_list)!! as LayerDrawable
        drawable.state = state

        val tint =
            TintInfo().apply {
                mHasTintList = true
                mTintList = ColorStateList.valueOf(Color.RED)
            }

        // Before tinting, all layers have the correct state.
        assertArrayEquals(state, drawable.getDrawable(0).state)
        assertArrayEquals(state, drawable.getDrawable(1).state)

        ResourceManagerInternal.tintDrawable(drawable, tint, state)

        // After tinting, all layers have the correct state.
        assertArrayEquals(state, drawable.getDrawable(0).state)
        assertArrayEquals(state, drawable.getDrawable(1).state)
    }
}
