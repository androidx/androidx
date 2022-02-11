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

package androidx.core.graphics;

import static androidx.core.graphics.BlendModeColorFilterCompat.createBlendModeColorFilterCompat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.graphics.BlendMode;
import android.graphics.BlendModeColorFilter;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.os.Build;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class BlendModeColorFilterCompatTest {

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    public void testNullBlendModeRemovesBlendModeColorFilter() {
        ColorFilter filter = createBlendModeColorFilterCompat(Color.RED, BlendModeCompat.CLEAR);
        assertTrue(filter instanceof BlendModeColorFilter);

        BlendModeColorFilter blendModeFilter = (BlendModeColorFilter) filter;
        assertEquals(Color.RED, blendModeFilter.getColor());
        assertEquals(BlendMode.CLEAR, blendModeFilter.getMode());
    }

    @Test
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.P)
    public void testNullBlendModeRemovesPorterDuffColorFilter() {
        ColorFilter filter = createBlendModeColorFilterCompat(Color.RED, BlendModeCompat.CLEAR);
        assertTrue(filter instanceof PorterDuffColorFilter);

        // PorterDuffColorFilter did not have an equals and hashcode implementation until L
        // So only do the comparison of parameters when running on L+ to avoid test failures that
        // are done using reference equality
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            assertEquals(new PorterDuffColorFilter(Color.RED, PorterDuff.Mode.CLEAR), filter);
        }
    }
}
