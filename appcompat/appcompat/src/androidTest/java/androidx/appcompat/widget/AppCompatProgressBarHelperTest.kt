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

package androidx.appcompat.widget

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.LayerDrawable
import android.view.Gravity
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppCompatProgressBarHelperTest {

    @Test
    fun testTileifyLayerDrawable() {
        val drawable = LayerDrawable(arrayOf(ColorDrawable(Color.RED)))
        drawable.setLayerGravity(0, Gravity.LEFT)

        val helper = AppCompatProgressBarHelper(null)
        val tiled = helper.tileify(drawable, false)

        assertTrue(tiled is LayerDrawable)
        assertEquals(Gravity.LEFT, (tiled as LayerDrawable).getLayerGravity(0))
    }
}
