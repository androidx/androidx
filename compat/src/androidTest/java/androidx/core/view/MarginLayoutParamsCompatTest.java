/*
 * Copyright 2018 The Android Open Source Project
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
package androidx.core.view;

import static org.junit.Assert.assertEquals;

import android.os.Build;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.view.ViewGroup;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class MarginLayoutParamsCompatTest {
    @Test
    public void testLayoutDirection() {
        ViewGroup.MarginLayoutParams mlp = new ViewGroup.MarginLayoutParams(0, 0);

        assertEquals("Default LTR layout direction", ViewCompat.LAYOUT_DIRECTION_LTR,
                MarginLayoutParamsCompat.getLayoutDirection(mlp));

        MarginLayoutParamsCompat.setLayoutDirection(mlp, ViewCompat.LAYOUT_DIRECTION_RTL);
        if (Build.VERSION.SDK_INT >= 17) {
            assertEquals("RTL layout direction", ViewCompat.LAYOUT_DIRECTION_RTL,
                    MarginLayoutParamsCompat.getLayoutDirection(mlp));
        } else {
            assertEquals("Still LTR layout direction on older devices",
                    ViewCompat.LAYOUT_DIRECTION_LTR,
                    MarginLayoutParamsCompat.getLayoutDirection(mlp));
        }

        MarginLayoutParamsCompat.setLayoutDirection(mlp, ViewCompat.LAYOUT_DIRECTION_LTR);
        assertEquals("Back to LTR layout direction", ViewCompat.LAYOUT_DIRECTION_LTR,
                MarginLayoutParamsCompat.getLayoutDirection(mlp));
    }

    @Test
    public void testMappingOldMarginsToNewMarginsLtr() {
        ViewGroup.MarginLayoutParams mlp = new ViewGroup.MarginLayoutParams(0, 0);

        mlp.leftMargin = 50;
        mlp.rightMargin = 80;

        assertEquals("Mapping left to start under LTR", 50,
                MarginLayoutParamsCompat.getMarginStart(mlp));
        assertEquals("Mapping right to end under LTR", 80,
                MarginLayoutParamsCompat.getMarginEnd(mlp));
    }

    @Test
    public void testMappingOldMarginsToNewMarginsRtl() {
        ViewGroup.MarginLayoutParams mlp = new ViewGroup.MarginLayoutParams(0, 0);

        mlp.leftMargin = 50;
        mlp.rightMargin = 80;

        MarginLayoutParamsCompat.setLayoutDirection(mlp, ViewCompat.LAYOUT_DIRECTION_RTL);

        if (Build.VERSION.SDK_INT >= 17) {
            assertEquals("Mapping right to start under RTL", 80,
                    MarginLayoutParamsCompat.getMarginStart(mlp));
            assertEquals("Mapping left to end under RTL", 50,
                    MarginLayoutParamsCompat.getMarginEnd(mlp));
        } else {
            assertEquals("Mapping left to start under RTL on older devices", 50,
                    MarginLayoutParamsCompat.getMarginStart(mlp));
            assertEquals("Mapping right to end under RTL on older devices", 80,
                    MarginLayoutParamsCompat.getMarginEnd(mlp));
        }
    }

    @Test
    public void testMappingNewMarginsToNewMarginsLtr() {
        ViewGroup.MarginLayoutParams mlp = new ViewGroup.MarginLayoutParams(0, 0);

        MarginLayoutParamsCompat.setMarginStart(mlp, 50);
        assertEquals("Resolved start margin under LTR", 50,
                MarginLayoutParamsCompat.getMarginStart(mlp));
        // Check that initial end / right margins are still 0
        assertEquals("Default end margin under LTR", 0,
                MarginLayoutParamsCompat.getMarginEnd(mlp));

        MarginLayoutParamsCompat.setMarginEnd(mlp, 80);
        assertEquals("Resolved end margin under LTR", 80,
                MarginLayoutParamsCompat.getMarginEnd(mlp));
        // Check that start / left margins are still the same
        assertEquals("Keeping start margin under LTR", 50,
                MarginLayoutParamsCompat.getMarginStart(mlp));
    }

    @Test
    public void testMappingNewMarginsToNewMarginsRtl() {
        ViewGroup.MarginLayoutParams mlp = new ViewGroup.MarginLayoutParams(0, 0);

        // Note that unlike the test that checks mapping of left/right to start/end and has
        // to do platform-specific checks, the checks in this test are platform-agnostic,
        // relying on the relevant MarginLayoutParamsCompat to do the right mapping internally.

        MarginLayoutParamsCompat.setLayoutDirection(mlp, ViewCompat.LAYOUT_DIRECTION_RTL);
        MarginLayoutParamsCompat.setMarginStart(mlp, 50);

        assertEquals("Resolved start margin under RTL", 50,
                MarginLayoutParamsCompat.getMarginStart(mlp));
        // Check that initial end / right margins are still 0
        assertEquals("Default end margin under RTL", 0,
                MarginLayoutParamsCompat.getMarginEnd(mlp));

        MarginLayoutParamsCompat.setMarginEnd(mlp, 80);
        assertEquals("Resolved end margin under RTL", 80,
                MarginLayoutParamsCompat.getMarginEnd(mlp));
        // Check that start / left margins are still the same
        assertEquals("Keeping start margin under RTL", 50,
                MarginLayoutParamsCompat.getMarginStart(mlp));
    }

    @Test
    public void testResolveMarginsLtr() {
        ViewGroup.MarginLayoutParams mlp = new ViewGroup.MarginLayoutParams(0, 0);

        MarginLayoutParamsCompat.setMarginStart(mlp, 50);
        MarginLayoutParamsCompat.resolveLayoutDirection(mlp, ViewCompat.LAYOUT_DIRECTION_LTR);

        // While there's no guarantee that left/right margin fields have been set / resolved
        // prior to the resolveLayoutDirection call, they should be now
        assertEquals("Resolved left margin field under LTR", 50, mlp.leftMargin);
        assertEquals("Default right margin field under LTR", 0, mlp.rightMargin);

        MarginLayoutParamsCompat.setMarginEnd(mlp, 80);
        MarginLayoutParamsCompat.resolveLayoutDirection(mlp, ViewCompat.LAYOUT_DIRECTION_LTR);

        assertEquals("Resolved right margin field under LTR", 80, mlp.rightMargin);
        assertEquals("Keeping left margin field under LTR", 50, mlp.leftMargin);
    }

    @Test
    public void testResolveMarginsRtl() {
        ViewGroup.MarginLayoutParams mlp = new ViewGroup.MarginLayoutParams(0, 0);

        MarginLayoutParamsCompat.setMarginStart(mlp, 50);
        MarginLayoutParamsCompat.resolveLayoutDirection(mlp, ViewCompat.LAYOUT_DIRECTION_RTL);

        // While there's no guarantee that left/right margin fields have been set / resolved
        // prior to the resolveLayoutDirection call, they should be now
        if (Build.VERSION.SDK_INT >= 17) {
            assertEquals("Default left margin field under RTL", 0, mlp.leftMargin);
            assertEquals("Resolved right margin field under RTL", 50, mlp.rightMargin);
        } else {
            assertEquals("Resolved left margin field under RTL on older devices",
                    50, mlp.leftMargin);
            assertEquals("Default right margin field under RTL on older devices",
                    0, mlp.rightMargin);
        }

        MarginLayoutParamsCompat.setMarginEnd(mlp, 80);
        MarginLayoutParamsCompat.resolveLayoutDirection(mlp, ViewCompat.LAYOUT_DIRECTION_RTL);

        if (Build.VERSION.SDK_INT >= 17) {
            assertEquals("Resolved left margin field under RTL", 80, mlp.leftMargin);
            assertEquals("Keeping right margin field under RTL", 50, mlp.rightMargin);
        } else {
            assertEquals("Resolved right margin field under RTL on older devices",
                    80, mlp.rightMargin);
            assertEquals("Keeping left margin field under RTL on older devices",
                    50, mlp.leftMargin);
        }
    }
}
