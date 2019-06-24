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
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Xfermode;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class PaintTest {

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
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
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.P)
    public void testBlendModeCompatMatchesPorterDuff() {
        verifyPorterDuffMatchesCompat(BlendModeCompat.CLEAR, PorterDuff.Mode.CLEAR);
        verifyPorterDuffMatchesCompat(BlendModeCompat.SRC, PorterDuff.Mode.SRC);
        verifyPorterDuffMatchesCompat(BlendModeCompat.DST, PorterDuff.Mode.DST);
        verifyPorterDuffMatchesCompat(BlendModeCompat.SRC_OVER, PorterDuff.Mode.SRC_OVER);
        verifyPorterDuffMatchesCompat(BlendModeCompat.DST_OVER, PorterDuff.Mode.DST_OVER);
        verifyPorterDuffMatchesCompat(BlendModeCompat.SRC_IN, PorterDuff.Mode.SRC_IN);
        verifyPorterDuffMatchesCompat(BlendModeCompat.DST_IN, PorterDuff.Mode.DST_IN);
        verifyPorterDuffMatchesCompat(BlendModeCompat.SRC_OUT, PorterDuff.Mode.SRC_OUT);
        verifyPorterDuffMatchesCompat(BlendModeCompat.DST_OUT, PorterDuff.Mode.DST_OUT);
        verifyPorterDuffMatchesCompat(BlendModeCompat.SRC_ATOP, PorterDuff.Mode.SRC_ATOP);
        verifyPorterDuffMatchesCompat(BlendModeCompat.DST_ATOP, PorterDuff.Mode.DST_ATOP);
        verifyPorterDuffMatchesCompat(BlendModeCompat.XOR, PorterDuff.Mode.XOR);
        verifyPorterDuffMatchesCompat(BlendModeCompat.PLUS, PorterDuff.Mode.ADD);
        // b/73224934 PorterDuff Multiply maps to Skia Modulate
        verifyPorterDuffMatchesCompat(BlendModeCompat.MODULATE, PorterDuff.Mode.MULTIPLY);
        verifyPorterDuffMatchesCompat(BlendModeCompat.SCREEN, PorterDuff.Mode.SCREEN);
        verifyPorterDuffMatchesCompat(BlendModeCompat.OVERLAY, PorterDuff.Mode.OVERLAY);
        verifyPorterDuffMatchesCompat(BlendModeCompat.DARKEN, PorterDuff.Mode.DARKEN);
        verifyPorterDuffMatchesCompat(BlendModeCompat.LIGHTEN, PorterDuff.Mode.LIGHTEN);
        // Not supported before Q
        verifyPorterDuffMatchesCompat(BlendModeCompat.COLOR_DODGE, null);
        verifyPorterDuffMatchesCompat(BlendModeCompat.COLOR_BURN, null);
        verifyPorterDuffMatchesCompat(BlendModeCompat.HARD_LIGHT, null);
        verifyPorterDuffMatchesCompat(BlendModeCompat.SOFT_LIGHT, null);
        verifyPorterDuffMatchesCompat(BlendModeCompat.DIFFERENCE, null);
        verifyPorterDuffMatchesCompat(BlendModeCompat.EXCLUSION, null);
        // Technically BlendMode.MULTIPLY should map to PorterDuff.Mode.MULTIPLY
        // However b/73224934 PorterDuff Multiply maps to Skia Modulate
        verifyPorterDuffMatchesCompat(BlendModeCompat.MULTIPLY, null);
        verifyPorterDuffMatchesCompat(BlendModeCompat.HUE, null);
        verifyPorterDuffMatchesCompat(BlendModeCompat.SATURATION, null);
        verifyPorterDuffMatchesCompat(BlendModeCompat.COLOR, null);
        verifyPorterDuffMatchesCompat(BlendModeCompat.LUMINOSITY, null);
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    public void testNullBlendModeRemovesBlendMode() {
        Paint p = new Paint();
        assertTrue(PaintCompat.setBlendMode(p, BlendModeCompat.CLEAR));
        assertEquals(BlendMode.CLEAR, p.getBlendMode());

        assertTrue(PaintCompat.setBlendMode(p, null));
        assertNull(p.getBlendMode());
    }


    @Test
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.P)
    public void testNullBlendModeRemovesXfermode() {
        Paint p = new Paint();
        assertTrue(PaintCompat.setBlendMode(p, BlendModeCompat.CLEAR));
        verifyPorterDuffMatchesCompat(BlendModeCompat.CLEAR, PorterDuff.Mode.CLEAR);

        verifyPorterDuffMatchesCompat(null, null);
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

    /**
     * Helper method to verify that the provided {@link BlendModeCompat} instance
     * matches the given {@link PorterDuff.Mode} which may be null if there is no
     * equivalent PorterDuff.Mode for the BlendMode
     */
    private void verifyPorterDuffMatchesCompat(@Nullable BlendModeCompat compat,
                                               @Nullable PorterDuff.Mode mode) {
        Paint p = new Paint();
        boolean result = PaintCompat.setBlendMode(p, compat);
        if (compat != null && mode == null) {
            // If there is not a compatible PorterDuff mode for this BlendMode, configuration
            // of the blend mode should return false
            assertTrue(!result);
        } else if (compat != null) {
            // .. otherwise if there is a corresponding PorterDuff mode with the given BlendMode
            // then the assignment should complete successfully
            assertTrue(result);
        } else if (mode == null) {
            // If null is provided, then the assignment should complete successfully and the
            // default blending algorithm will be utilized on the paint
            assertTrue(result);
        }

        // Fields on PorterDuffXfermode are private, so verify the mapping helper method is correct
        // and the resultant Xfermode returned from Paint#getXfermode is an instance of
        // PorterDuffXferMode
        Xfermode xfermode = p.getXfermode();
        if (compat != null) {
            assertEquals(mode, BlendModeUtils.obtainPorterDuffFromCompat(compat));
        }

        if (mode != null) {
            assertTrue(xfermode instanceof PorterDuffXfermode);
        } else {
            assertNull(xfermode);
        }
    }
}
