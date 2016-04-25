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

import static android.support.v7.graphics.TestUtils.assertCloseColors;
import static android.support.v7.graphics.TestUtils.loadSampleBitmap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;

@RunWith(AndroidJUnit4.class)
public class BucketTests {

    @Test
    @SmallTest
    public void testSourceBitmapNotRecycled() {
        final Bitmap sample = loadSampleBitmap();

        Palette.from(sample).generate();
        assertFalse(sample.isRecycled());
    }

    @Test(expected = UnsupportedOperationException.class)
    @SmallTest
    public void testSwatchesUnmodifiable() {
        Palette p = Palette.from(loadSampleBitmap()).generate();
        p.getSwatches().remove(0);
    }

    @Test
    @SmallTest
    public void testSwatchesBuilder() {
        ArrayList<Palette.Swatch> swatches = new ArrayList<>();
        swatches.add(new Palette.Swatch(Color.BLACK, 40));
        swatches.add(new Palette.Swatch(Color.GREEN, 60));
        swatches.add(new Palette.Swatch(Color.BLUE, 10));

        Palette p = Palette.from(swatches);

        assertEquals(swatches, p.getSwatches());
    }

    @Test
    @SmallTest
    public void testRegionWhole() {
        final Bitmap sample = loadSampleBitmap();

        Palette.Builder b = new Palette.Builder(sample);
        b.setRegion(0, 0, sample.getWidth(), sample.getHeight());
        b.generate();
    }

    @Test
    @SmallTest
    public void testRegionUpperLeft() {
        final Bitmap sample = loadSampleBitmap();

        Palette.Builder b = new Palette.Builder(sample);
        b.setRegion(0, 0, sample.getWidth() / 2, sample.getHeight() / 2);
        b.generate();
    }

    @Test
    @SmallTest
    public void testRegionBottomRight() {
        final Bitmap sample = loadSampleBitmap();

        Palette.Builder b = new Palette.Builder(sample);
        b.setRegion(sample.getWidth() / 2, sample.getHeight() / 2,
                sample.getWidth(), sample.getHeight());
        b.generate();
    }

    @Test
    @SmallTest
    public void testOnePixelTallBitmap() {
        final Bitmap bitmap = Bitmap.createBitmap(1000, 1, Bitmap.Config.ARGB_8888);

        Palette.Builder b = new Palette.Builder(bitmap);
        b.generate();
    }

    @Test
    @SmallTest
    public void testOnePixelWideBitmap() {
        final Bitmap bitmap = Bitmap.createBitmap(1, 1000, Bitmap.Config.ARGB_8888);

        Palette.Builder b = new Palette.Builder(bitmap);
        b.generate();
    }

    @Test
    @SmallTest
    public void testBlueBitmapReturnsBlueSwatch() {
        final Bitmap bitmap = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.BLUE);

        final Palette palette = Palette.from(bitmap).generate();

        assertEquals(1, palette.getSwatches().size());

        final Palette.Swatch swatch = palette.getSwatches().get(0);
        assertCloseColors(Color.BLUE, swatch.getRgb());
    }

    @Test
    @SmallTest
    public void testBlueBitmapWithRegionReturnsBlueSwatch() {
        final Bitmap bitmap = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.BLUE);

        final Palette palette = Palette.from(bitmap)
                .setRegion(0, bitmap.getHeight() / 2, bitmap.getWidth(), bitmap.getHeight())
                .generate();

        assertEquals(1, palette.getSwatches().size());

        final Palette.Swatch swatch = palette.getSwatches().get(0);
        assertCloseColors(Color.BLUE, swatch.getRgb());
    }

}
