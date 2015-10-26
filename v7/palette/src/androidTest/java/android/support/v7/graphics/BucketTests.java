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

package android.support.v7.graphics;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.test.InstrumentationTestCase;

import java.util.ArrayList;

/**
 * @hide
 */
public class BucketTests extends InstrumentationTestCase {

    private Bitmap mSource;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mSource = BitmapFactory.decodeResource(getInstrumentation().getContext().getResources(),
                android.R.drawable.sym_def_app_icon);
    }

    public void testSourceBitmapNotRecycled() {
        Palette.from(mSource).generate();
        assertFalse(mSource.isRecycled());
    }

    public void testSwatchesUnmodifiable() {
        Palette p = Palette.from(mSource).generate();
        boolean thrown = false;

        try {
            p.getSwatches().remove(0);
        } catch (UnsupportedOperationException e) {
            thrown = true;
        }

        assertTrue(thrown);
    }

    public void testSwatchesBuilder() {
        ArrayList<Palette.Swatch> swatches = new ArrayList<>();
        swatches.add(new Palette.Swatch(Color.BLACK, 40));
        swatches.add(new Palette.Swatch(Color.GREEN, 60));
        swatches.add(new Palette.Swatch(Color.BLUE, 10));

        Palette p = Palette.from(swatches);

        assertEquals(swatches, p.getSwatches());
    }

    public void testRegionWhole() {
        Palette.Builder b = new Palette.Builder(mSource);
        b.setRegion(0, 0, mSource.getWidth(), mSource.getHeight());

        Throwable thrown = null;
        try {
            b.generate();
        } catch (Exception e) {
            thrown = e;
        }
        assertNull(thrown);
    }

    public void testRegionUpperLeft() {
        Palette.Builder b = new Palette.Builder(mSource);
        b.setRegion(0, 0, mSource.getWidth() / 2, mSource.getHeight() / 2);

        Throwable thrown = null;
        try {
            b.generate();
        } catch (Exception e) {
            thrown = e;
        }
        assertNull(thrown);
    }

    public void testRegionBottomRight() {
        Palette.Builder b = new Palette.Builder(mSource);
        b.setRegion(mSource.getWidth() / 2, mSource.getHeight() / 2,
                mSource.getWidth(), mSource.getHeight());

        Throwable thrown = null;
        try {
            b.generate();
        } catch (Exception e) {
            thrown = e;
        }
        assertNull(thrown);
    }

    public void testOnePixelTallBitmap() {
        Bitmap bitmap = Bitmap.createBitmap(1000, 1, Bitmap.Config.ARGB_8888);

        Palette.Builder b = new Palette.Builder(bitmap);

        Throwable thrown = null;
        try {
            b.generate();
        } catch (Exception e) {
            thrown = e;
        }
        assertNull(thrown);
    }

    public void testOnePixelWideBitmap() {
        Bitmap bitmap = Bitmap.createBitmap(1, 1000, Bitmap.Config.ARGB_8888);

        Palette.Builder b = new Palette.Builder(bitmap);

        Throwable thrown = null;
        try {
            b.generate();
        } catch (Exception e) {
            thrown = e;
        }
        assertNull(thrown);
    }
}
