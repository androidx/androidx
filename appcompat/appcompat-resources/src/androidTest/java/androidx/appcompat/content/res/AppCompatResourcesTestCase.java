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

package androidx.appcompat.content.res;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorStateListDrawable;
import android.graphics.drawable.Drawable;

import androidx.appcompat.graphics.drawable.MyDrawable;
import androidx.appcompat.resources.test.R;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;

import org.junit.Test;
import org.junit.runner.RunWith;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class AppCompatResourcesTestCase {
    private final Context mContext;

    public AppCompatResourcesTestCase() {
        mContext = ApplicationProvider.getApplicationContext();
    }

    @Test
    public void testColorStateListCaching() {
        final ColorStateList result1 = AppCompatResources.getColorStateList(
                mContext, R.color.color_state_list_themed_attrs);
        final ColorStateList result2 = AppCompatResources.getColorStateList(
                mContext, R.color.color_state_list_themed_attrs);
        assertNotNull(result1);
        assertNotNull(result2);
        assertEquals(result1, result2);
    }

    @Test
    public void testGetDrawableVectorResource() {
        assertNotNull(AppCompatResources.getDrawable(mContext, R.drawable.test_vector_off));
    }

    @Test
    public void testGetAnimatedStateListDrawable() {
        assertNotNull(AppCompatResources.getDrawable(mContext, R.drawable.asl_heart));
    }

    @Test
    public void testGetCustomDrawable() {
        Drawable custom = AppCompatResources.getDrawable(mContext, R.drawable.my_drawable);
        assertNotNull(custom);
        assertTrue(custom instanceof MyDrawable);
    }

    /**
     * Test workaround for platform bug on SDKs 29 and 30 where the second ColorStateListDrawable
     * for a given resource ID will be cloned from the cache without setting a default color.
     */
    @SdkSuppress(minSdkVersion = 29, maxSdkVersion = 30)
    @Test
    public void testGetColorStateListDrawable() {
        Bitmap b = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);

        ColorStateListDrawable csld1 = (ColorStateListDrawable) AppCompatResources.getDrawable(
                mContext, R.color.color_state_list_red);
        assertNotNull(csld1);
        csld1.setBounds(0, 0, 1, 1);
        csld1.draw(c);
        assertEquals(Color.RED, b.getPixel(0, 0));

        b.eraseColor(Color.TRANSPARENT);

        ColorStateListDrawable csld2 = (ColorStateListDrawable) AppCompatResources.getDrawable(
                mContext, R.color.color_state_list_red);
        assertNotNull(csld2);
        csld2.setBounds(0, 0, 1, 1);
        csld2.draw(c);
        assertEquals(Color.RED, b.getPixel(0, 0));
    }
}
