/*
 * Copyright (C) 2015 The Android Open Source Project
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
package android.support.v17.leanback.graphics;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.test.suitebuilder.annotation.SmallTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import android.support.test.runner.AndroidJUnit4;
import org.mockito.MockitoAnnotations;

/**
 * Unit test for {@link RegionDrawable}
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class RegionDrawableTest {

    private static final int HEIGHT = 800;
    private static final int WIDTH = 600;
    Bitmap bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);

    @Test
    public void updateBounds_noBoundsRule() {
        BitmapRegionDrawable drawable = new BitmapRegionDrawable(bitmap, null);
        Rect bounds = new Rect(0, 0, WIDTH, HEIGHT);
        drawable.updateBounds(bounds);
        Rect adjustedBounds = drawable.getBounds();
        assertEquals(bounds, adjustedBounds);
    }

    @Test
    public void updateBounds_withBoundsRule() {
        float fraction = 0.5f;
        BitmapRegionDrawable drawable = new BitmapRegionDrawable(bitmap, null);
        Rect bounds = new Rect(0, 0, WIDTH, HEIGHT);
        assertEquals(HEIGHT, bounds.height());
        assertEquals(WIDTH, bounds.width());

        // inherit from parent
        drawable.getBoundsRule().bottom = BoundsRule.inheritFromParent(fraction);
        drawable.updateBounds(bounds);
        Rect adjustedBounds = drawable.getBounds();
        Rect expectedBounds = new Rect(bounds);
        expectedBounds.bottom = (int) (HEIGHT * fraction);
        assertEquals(expectedBounds, adjustedBounds);

        // absolute value
        drawable.getBoundsRule().bottom = BoundsRule.absoluteValue(200);
        drawable.updateBounds(bounds);
        adjustedBounds = drawable.getBounds();
        expectedBounds = new Rect(bounds);
        expectedBounds.bottom = 200;
        assertEquals(expectedBounds, adjustedBounds);

        // inherit with offset
        drawable.getBoundsRule().bottom = BoundsRule.inheritFromParentWithOffset(.5f, 100);
        drawable.updateBounds(bounds);
        adjustedBounds = drawable.getBounds();
        expectedBounds = new Rect(bounds);
        expectedBounds.bottom = (int) (HEIGHT * fraction + 100);
        assertEquals(expectedBounds, adjustedBounds);
    }

    @Test
    public void updateBounds_withOverride() {
        float fraction = 0.5f;
        BitmapRegionDrawable drawable = new BitmapRegionDrawable(bitmap, null);
        Rect bounds = new Rect(0, 0, WIDTH, HEIGHT);
        assertEquals(HEIGHT, bounds.height());
        assertEquals(WIDTH, bounds.width());

        // inherit from parent
        drawable.getBoundsRule().top = BoundsRule.inheritFromParent(fraction);
        drawable.getBoundsRule().bottom = BoundsRule.inheritFromParent(fraction);
        drawable.setOverrideTop(-100);
        drawable.updateBounds(bounds);
        Rect adjustedBounds = drawable.getBounds();
        Rect expectedBounds = new Rect(bounds);
        expectedBounds.top = -100;
        expectedBounds.bottom = (int) (HEIGHT * fraction);
        assertEquals(expectedBounds, adjustedBounds);
    }
}
