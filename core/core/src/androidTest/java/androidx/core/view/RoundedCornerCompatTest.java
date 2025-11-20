/*
 * Copyright 2025 The Android Open Source Project
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

import static androidx.core.view.RoundedCornerCompat.POSITION_BOTTOM_LEFT;
import static androidx.core.view.RoundedCornerCompat.POSITION_BOTTOM_RIGHT;
import static androidx.core.view.RoundedCornerCompat.POSITION_TOP_LEFT;
import static androidx.core.view.RoundedCornerCompat.POSITION_TOP_RIGHT;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

import android.graphics.Point;
import android.view.RoundedCorner;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SdkSuppress;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@MediumTest
public class RoundedCornerCompatTest {

    @Test
    public void testConstructor() {
        final RoundedCornerCompat topLeft = new RoundedCornerCompat(
                POSITION_TOP_LEFT, 1 /* radius */, 2 /* centerX */, 3 /* centerY */);
        assertEquals("Position must be the same.",
                POSITION_TOP_LEFT, topLeft.getPosition());
        assertEquals("Radius must be the same.", 1, topLeft.getRadius());
        assertEquals("Center X must be the same.", 2, topLeft.getCenterX());
        assertEquals("Center Y must be the same.", 3, topLeft.getCenterY());

        final RoundedCornerCompat topRight = new RoundedCornerCompat(
                POSITION_TOP_RIGHT, 1 /* radius */, 2 /* centerX */, 3 /* centerY */);
        assertEquals("Position must be the same.",
                POSITION_TOP_RIGHT, topRight.getPosition());
        assertEquals("Radius must be the same.", 1, topRight.getRadius());
        assertEquals("Center X must be the same.", 2, topRight.getCenterX());
        assertEquals("Center Y must be the same.", 3, topRight.getCenterY());

        final RoundedCornerCompat bottomRight = new RoundedCornerCompat(
                POSITION_BOTTOM_RIGHT, 1 /* radius */, 2 /* centerX */, 3 /* centerY */);
        assertEquals("Position must be the same.",
                POSITION_BOTTOM_RIGHT, bottomRight.getPosition());
        assertEquals("Radius must be the same.", 1, bottomRight.getRadius());
        assertEquals("Center X must be the same.", 2, bottomRight.getCenterX());
        assertEquals("Center Y must be the same.", 3, bottomRight.getCenterY());

        final RoundedCornerCompat bottomLeft = new RoundedCornerCompat(
                POSITION_BOTTOM_LEFT, 1 /* radius */, 2 /* centerX */, 3 /* centerY */);
        assertEquals("Position must be the same.",
                POSITION_BOTTOM_LEFT, bottomLeft.getPosition());
        assertEquals("Radius must be the same.", 1, bottomLeft.getRadius());
        assertEquals("Center X must be the same.", 2, bottomLeft.getCenterX());
        assertEquals("Center Y must be the same.", 3, bottomLeft.getCenterY());
    }

    @SdkSuppress(minSdkVersion = 31)
    @Test
    public void testToRoundedCornerCompat() {
        assertNull("The returned rounded corner must be null.",
                RoundedCornerCompat.toRoundedCornerCompat(null));

        final RoundedCornerCompat topLeft = RoundedCornerCompat.toRoundedCornerCompat(
                new RoundedCorner(RoundedCorner.POSITION_TOP_LEFT,
                        1 /* radius */, 2 /* centerX */, 3 /* centerY */));
        assertEquals("Position must be the same.",
                POSITION_TOP_LEFT, topLeft.getPosition());
        assertEquals("Radius must be the same.", 1, topLeft.getRadius());
        assertEquals("Center X must be the same.", 2, topLeft.getCenterX());
        assertEquals("Center Y must be the same.", 3, topLeft.getCenterY());

        final RoundedCornerCompat topRight = RoundedCornerCompat.toRoundedCornerCompat(
                new RoundedCorner(RoundedCorner.POSITION_TOP_RIGHT,
                        1 /* radius */, 2 /* centerX */, 3 /* centerY */));
        assertEquals("Position must be the same.",
                POSITION_TOP_RIGHT, topRight.getPosition());
        assertEquals("Radius must be the same.", 1, topRight.getRadius());
        assertEquals("Center X must be the same.", 2, topRight.getCenterX());
        assertEquals("Center Y must be the same.", 3, topRight.getCenterY());

        final RoundedCornerCompat bottomRight = RoundedCornerCompat.toRoundedCornerCompat(
                new RoundedCorner(RoundedCorner.POSITION_BOTTOM_RIGHT,
                        1 /* radius */, 2 /* centerX */, 3 /* centerY */));
        assertEquals("Position must be the same.",
                POSITION_BOTTOM_RIGHT, bottomRight.getPosition());
        assertEquals("Radius must be the same.", 1, bottomRight.getRadius());
        assertEquals("Center X must be the same.", 2, bottomRight.getCenterX());
        assertEquals("Center Y must be the same.", 3, bottomRight.getCenterY());

        final RoundedCornerCompat bottomLeft = RoundedCornerCompat.toRoundedCornerCompat(
                new RoundedCorner(RoundedCorner.POSITION_BOTTOM_LEFT,
                        1 /* radius */, 2 /* centerX */, 3 /* centerY */));
        assertEquals("Position must be the same.",
                POSITION_BOTTOM_LEFT, bottomLeft.getPosition());
        assertEquals("Radius must be the same.", 1, bottomLeft.getRadius());
        assertEquals("Center X must be the same.", 2, bottomLeft.getCenterX());
        assertEquals("Center Y must be the same.", 3, bottomLeft.getCenterY());
    }

    @SdkSuppress(minSdkVersion = 31)
    @Test
    public void testToPlatformRoundedCorner() {
        assertNull("The returned rounded corner must be null.",
                RoundedCornerCompat.toPlatformRoundedCorner(null));

        final RoundedCorner topLeft = RoundedCornerCompat.toPlatformRoundedCorner(
                new RoundedCornerCompat(POSITION_TOP_LEFT,
                        1 /* radius */, 2 /* centerX */, 3 /* centerY */));
        assertEquals("Position must be the same.",
                RoundedCorner.POSITION_TOP_LEFT, topLeft.getPosition());
        assertEquals("Radius must be the same.", 1, topLeft.getRadius());
        assertEquals("Center must be the same.", new Point(2, 3), topLeft.getCenter());

        final RoundedCorner topRight = RoundedCornerCompat.toPlatformRoundedCorner(
                new RoundedCornerCompat(POSITION_TOP_RIGHT,
                        1 /* radius */, 2 /* centerX */, 3 /* centerY */));
        assertEquals("Position must be the same.",
                RoundedCorner.POSITION_TOP_RIGHT, topRight.getPosition());
        assertEquals("Radius must be the same.", 1, topRight.getRadius());
        assertEquals("Center must be the same.", new Point(2, 3), topRight.getCenter());

        final RoundedCorner bottomRight = RoundedCornerCompat.toPlatformRoundedCorner(
                new RoundedCornerCompat(POSITION_BOTTOM_RIGHT,
                        1 /* radius */, 2 /* centerX */, 3 /* centerY */));
        assertEquals("Position must be the same.",
                RoundedCorner.POSITION_BOTTOM_RIGHT, bottomRight.getPosition());
        assertEquals("Radius must be the same.", 1, bottomRight.getRadius());
        assertEquals("Center must be the same.", new Point(2, 3), bottomRight.getCenter());

        final RoundedCorner bottomLeft = RoundedCornerCompat.toPlatformRoundedCorner(
                new RoundedCornerCompat(POSITION_BOTTOM_LEFT,
                        1 /* radius */, 2 /* centerX */, 3 /* centerY */));
        assertEquals("Position must be the same.",
                RoundedCorner.POSITION_BOTTOM_LEFT, bottomLeft.getPosition());
        assertEquals("Radius must be the same.", 1, bottomLeft.getRadius());
        assertEquals("Center must be the same.", new Point(2, 3), bottomLeft.getCenter());
    }

    @Test
    public void testEquals() {
        final int position = POSITION_BOTTOM_RIGHT;
        final int radius = 1;
        final int centerX = 2;
        final int centerY = 3;
        final RoundedCornerCompat roundedCornerCompat = new RoundedCornerCompat(
                position, radius, centerX, centerY);
        final RoundedCornerCompat roundedCornerCompat0 = new RoundedCornerCompat(
                position, radius, centerX, centerY);

        assertEquals("roundedCornerCompat must equal to roundedCornerCompat0.",
                roundedCornerCompat, roundedCornerCompat0);

        final RoundedCornerCompat roundedCornerCompat1 = new RoundedCornerCompat(
                POSITION_BOTTOM_LEFT, radius, centerX, centerY);
        final RoundedCornerCompat roundedCornerCompat2 = new RoundedCornerCompat(
                position, 4 /* radius */, centerX, centerY);
        final RoundedCornerCompat roundedCornerCompat3 = new RoundedCornerCompat(
                position, radius, 5 /* centerX */, centerY);
        final RoundedCornerCompat roundedCornerCompat4 = new RoundedCornerCompat(
                position, radius, centerX, 6 /* centerY */);

        assertNotEquals("roundedCornerCompat must not equal to roundedCornerCompat1.",
                roundedCornerCompat, roundedCornerCompat1);
        assertNotEquals("roundedCornerCompat must not equal to roundedCornerCompat2.",
                roundedCornerCompat, roundedCornerCompat2);
        assertNotEquals("roundedCornerCompat must not equal to roundedCornerCompat3.",
                roundedCornerCompat, roundedCornerCompat3);
        assertNotEquals("roundedCornerCompat must not equal to roundedCornerCompat4.",
                roundedCornerCompat, roundedCornerCompat4);
    }

}
