/*
 * Copyright (C) 2019 The Android Open Source Project
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.graphics.BlendMode;
import android.graphics.Paint;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 29)
@SmallTest
public class PaintTestApi29 {

    @Test
    public void testBlendModeCompatMatchesPlatform() {
        TestHelper.verifyBlendModeMatchesCompat(BlendModeCompat.CLEAR, BlendMode.CLEAR);
        TestHelper.verifyBlendModeMatchesCompat(BlendModeCompat.SRC, BlendMode.SRC);
        TestHelper.verifyBlendModeMatchesCompat(BlendModeCompat.DST, BlendMode.DST);
        TestHelper.verifyBlendModeMatchesCompat(BlendModeCompat.SRC_OVER, BlendMode.SRC_OVER);
        TestHelper.verifyBlendModeMatchesCompat(BlendModeCompat.DST_OVER, BlendMode.DST_OVER);
        TestHelper.verifyBlendModeMatchesCompat(BlendModeCompat.SRC_IN, BlendMode.SRC_IN);
        TestHelper.verifyBlendModeMatchesCompat(BlendModeCompat.DST_IN, BlendMode.DST_IN);
        TestHelper.verifyBlendModeMatchesCompat(BlendModeCompat.SRC_OUT, BlendMode.SRC_OUT);
        TestHelper.verifyBlendModeMatchesCompat(BlendModeCompat.DST_OUT, BlendMode.DST_OUT);
        TestHelper.verifyBlendModeMatchesCompat(BlendModeCompat.SRC_ATOP, BlendMode.SRC_ATOP);
        TestHelper.verifyBlendModeMatchesCompat(BlendModeCompat.DST_ATOP, BlendMode.DST_ATOP);
        TestHelper.verifyBlendModeMatchesCompat(BlendModeCompat.XOR, BlendMode.XOR);
        TestHelper.verifyBlendModeMatchesCompat(BlendModeCompat.PLUS, BlendMode.PLUS);
        TestHelper.verifyBlendModeMatchesCompat(BlendModeCompat.MODULATE, BlendMode.MODULATE);
        TestHelper.verifyBlendModeMatchesCompat(BlendModeCompat.SCREEN, BlendMode.SCREEN);
        TestHelper.verifyBlendModeMatchesCompat(BlendModeCompat.OVERLAY, BlendMode.OVERLAY);
        TestHelper.verifyBlendModeMatchesCompat(BlendModeCompat.DARKEN, BlendMode.DARKEN);
        TestHelper.verifyBlendModeMatchesCompat(BlendModeCompat.LIGHTEN, BlendMode.LIGHTEN);
        TestHelper.verifyBlendModeMatchesCompat(BlendModeCompat.COLOR_DODGE, BlendMode.COLOR_DODGE);
        TestHelper.verifyBlendModeMatchesCompat(BlendModeCompat.COLOR_BURN, BlendMode.COLOR_BURN);
        TestHelper.verifyBlendModeMatchesCompat(BlendModeCompat.HARD_LIGHT, BlendMode.HARD_LIGHT);
        TestHelper.verifyBlendModeMatchesCompat(BlendModeCompat.SOFT_LIGHT, BlendMode.SOFT_LIGHT);
        TestHelper.verifyBlendModeMatchesCompat(BlendModeCompat.DIFFERENCE, BlendMode.DIFFERENCE);
        TestHelper.verifyBlendModeMatchesCompat(BlendModeCompat.EXCLUSION, BlendMode.EXCLUSION);
        TestHelper.verifyBlendModeMatchesCompat(BlendModeCompat.MULTIPLY, BlendMode.MULTIPLY);
        TestHelper.verifyBlendModeMatchesCompat(BlendModeCompat.HUE, BlendMode.HUE);
        TestHelper.verifyBlendModeMatchesCompat(BlendModeCompat.SATURATION, BlendMode.SATURATION);
        TestHelper.verifyBlendModeMatchesCompat(BlendModeCompat.COLOR, BlendMode.COLOR);
        TestHelper.verifyBlendModeMatchesCompat(BlendModeCompat.LUMINOSITY, BlendMode.LUMINOSITY);
    }

    @Test
    public void testNullBlendModeRemovesBlendMode() {
        Paint p = new Paint();
        assertTrue(PaintCompat.setBlendMode(p, BlendModeCompat.CLEAR));
        assertEquals(BlendMode.CLEAR, p.getBlendMode());

        assertTrue(PaintCompat.setBlendMode(p, null));
        assertNull(p.getBlendMode());
    }

    /**
     * Helper test class to hide usages of new APIs and avoid ClassNotFoundExceptions
     * in tests
     */
    private static class TestHelper {
        private static void verifyBlendModeMatchesCompat(@NonNull BlendModeCompat compat,
                                                         @NonNull BlendMode blendMode) {
            Paint p = new Paint();
            PaintCompat.setBlendMode(p, compat);
            assertEquals(blendMode, p.getBlendMode());
        }
    }
}
