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

package androidx.vectordrawable.graphics.drawable.tests;

import static android.graphics.Bitmap.Config.ARGB_8888;
import static android.graphics.Color.TRANSPARENT;
import static android.graphics.Color.alpha;
import static android.graphics.Color.blue;
import static android.graphics.Color.green;
import static android.graphics.Color.red;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SdkSuppress;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;
import androidx.vectordrawable.test.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

@RunWith(AndroidJUnit4.class)
@MediumTest
public class VectorDrawableTest {
    private static final String LOGTAG = "VectorDrawableTest";

    private static final int[] ICON_RES_IDS = new int[]{
            R.drawable.vector_icon_create,
            R.drawable.vector_icon_delete,
            R.drawable.vector_icon_heart,
            R.drawable.vector_icon_schedule,
            R.drawable.vector_icon_settings,
            R.drawable.vector_icon_random_path_1,
            R.drawable.vector_icon_random_path_2,
            R.drawable.vector_icon_repeated_cq,
            R.drawable.vector_icon_repeated_st,
            R.drawable.vector_icon_repeated_a_1,
            R.drawable.vector_icon_repeated_a_2,
            R.drawable.vector_icon_clip_path_1,
            R.drawable.vector_icon_transformation_1,
            R.drawable.vector_icon_transformation_4,
            R.drawable.vector_icon_transformation_5,
            R.drawable.vector_icon_transformation_6,
            R.drawable.vector_icon_render_order_1,
            R.drawable.vector_icon_render_order_2,
            R.drawable.vector_icon_stroke_1,
            R.drawable.vector_icon_stroke_2,
            R.drawable.vector_icon_stroke_3,
            R.drawable.vector_icon_scale_1,
            R.drawable.vector_icon_group_clip,
            R.drawable.vector_icon_share,
            R.drawable.vector_icon_wishlist,
            R.drawable.vector_icon_five_bars,
            R.drawable.vector_icon_filltype_evenodd,
            R.drawable.vector_icon_filltype_nonzero,
            R.drawable.vector_icon_clip_filltype_evenodd,
    };

    private static final int[] GOLDEN_IMAGES = new int[]{
            R.drawable.vector_icon_create_golden,
            R.drawable.vector_icon_delete_golden,
            R.drawable.vector_icon_heart_golden,
            R.drawable.vector_icon_schedule_golden,
            R.drawable.vector_icon_settings_golden,
            R.drawable.vector_icon_random_path_1_golden,
            R.drawable.vector_icon_random_path_2_golden,
            R.drawable.vector_icon_repeated_cq_golden,
            R.drawable.vector_icon_repeated_st_golden,
            R.drawable.vector_icon_repeated_a_1_golden,
            R.drawable.vector_icon_repeated_a_2_golden,
            R.drawable.vector_icon_clip_path_1_golden,
            R.drawable.vector_icon_transformation_1_golden,
            R.drawable.vector_icon_transformation_4_golden,
            R.drawable.vector_icon_transformation_5_golden,
            R.drawable.vector_icon_transformation_6_golden,
            R.drawable.vector_icon_render_order_1_golden,
            R.drawable.vector_icon_render_order_2_golden,
            R.drawable.vector_icon_stroke_1_golden,
            R.drawable.vector_icon_stroke_2_golden,
            R.drawable.vector_icon_stroke_3_golden,
            R.drawable.vector_icon_scale_1_golden,
            R.drawable.vector_icon_group_clip_golden,
            R.drawable.vector_icon_share_golden,
            R.drawable.vector_icon_wishlist_golden,
            R.drawable.vector_icon_five_bars_golden,
            R.drawable.vector_icon_filltype_evenodd_golden,
            R.drawable.vector_icon_filltype_nonzero_golden,
            R.drawable.vector_icon_clip_filltype_evenodd_golden,
    };

    private static final int[] EDGES = new int[]{
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            R.drawable.vector_icon_repeated_a_1_edge,
            R.drawable.vector_icon_repeated_a_2_edge,
            R.drawable.vector_icon_clip_path_1_edge,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            R.drawable.vector_icon_five_bars_edge,
            -1,
            -1,
            -1,
    };

    private static final int[] GRADIENT_ICON_RES_IDS = new int[]{
            R.drawable.vector_icon_gradient_1,
            R.drawable.vector_icon_gradient_2,
            R.drawable.vector_icon_gradient_3,
            R.drawable.vector_icon_gradient_1_clamp,
            R.drawable.vector_icon_gradient_2_repeat,
            R.drawable.vector_icon_gradient_3_mirror,
            R.drawable.vector_icon_gradient_then_solid
    };

    /* Golden images for vectors with gradients. Note some images have platform level variants:
            * v26: Skia gradient rendering changed in O.
            * v28: Skia anti-aliasing changed in P.
            * v29: Skia gradient rendering changes in Q.
    */
    private static final int[] GRADIENT_GOLDEN_IMAGES = new int[]{
            R.drawable.vector_icon_gradient_1_golden,
            R.drawable.vector_icon_gradient_2_golden,
            R.drawable.vector_icon_gradient_3_golden,
            R.drawable.vector_icon_gradient_1_clamp_golden,
            R.drawable.vector_icon_gradient_2_repeat_golden,
            R.drawable.vector_icon_gradient_3_mirror_golden,
            R.drawable.vector_icon_gradient_then_solid_golden
    };

    private static final int[] STATEFUL_RES_IDS = new int[]{
            // All these icons are using the same color state list, make sure it works for either
            // the same drawable ID or different ID but same content.
            R.drawable.vector_icon_state_list,
            R.drawable.vector_icon_state_list,
            R.drawable.vector_icon_state_list_2,
    };

    private static final int[][] STATEFUL_GOLDEN_IMAGES = new int[][]{
            {
                    R.drawable.vector_icon_state_list_golden,
                    R.drawable.vector_icon_state_list_golden,
                    R.drawable.vector_icon_state_list_2_golden
            },
            {
                    R.drawable.vector_icon_state_list_pressed_golden,
                    R.drawable.vector_icon_state_list_pressed_golden,
                    R.drawable.vector_icon_state_list_2_pressed_golden
            }
    };

    private static final int[][] STATEFUL_STATE_SETS = new int[][]{
            {},
            {android.R.attr.state_pressed}
    };

    private static final int TEST_ICON = R.drawable.vector_icon_create;

    private static final int IMAGE_WIDTH = 64;
    private static final int IMAGE_HEIGHT = 64;

    // A small value is actually making sure that the values are matching
    // exactly with the golden image.
    // We can increase the threshold if the Skia is drawing with some variance
    // on different devices. So far, the tests show they are matching correctly.
    private static final float PIXEL_ERROR_THRESHOLD = 0.33f;
    private static final float PIXEL_DIFF_COUNT_THRESHOLD = 0.1f;
    private static final float PIXEL_DIFF_THRESHOLD = 0.025f;

    private static final boolean DBG_DUMP_PNG = false;

    private Context mContext;
    private Resources mResources;
    private VectorDrawableCompat mVectorDrawable;
    private Bitmap mBitmap;
    private Canvas mCanvas;
    private Theme mTheme;

    @Before
    public void setup() {
        mBitmap = Bitmap.createBitmap(IMAGE_WIDTH, IMAGE_HEIGHT, ARGB_8888);
        mCanvas = new Canvas(mBitmap);

        mContext = ApplicationProvider.getApplicationContext();
        mResources = mContext.getResources();
        mTheme = mContext.getTheme();
        mTheme.applyStyle(R.style.VectorDrawableTestTheme, true);
    }

    @Test
    public void testSimpleVectorDrawables() throws Exception {
        verifyVectorDrawables(ICON_RES_IDS, GOLDEN_IMAGES, EDGES, null);
    }


    @Test
    @SdkSuppress(maxSdkVersion = 33)
    // Skia changes resulted in inconsistent rendering of VectorDrawable gradient across
    // different QPR releases for U. Suppress test on U for now until a better solution
    // is worked out. b/322404096
    public void testVectorDrawableGradient() throws Exception {
        int[] edges = new int[GRADIENT_ICON_RES_IDS.length];
        Arrays.fill(edges, -1);
        verifyVectorDrawables(GRADIENT_ICON_RES_IDS, GRADIENT_GOLDEN_IMAGES, edges, null);
    }

    @Test
    public void testColorStateList() throws XmlPullParserException, IOException {
        int[] edges = new int[STATEFUL_RES_IDS.length];
        Arrays.fill(edges, -1);
        for (int i = 0; i < STATEFUL_STATE_SETS.length; i++) {
            verifyVectorDrawables(
                    STATEFUL_RES_IDS, STATEFUL_GOLDEN_IMAGES[i], edges, STATEFUL_STATE_SETS[i]);
        }
    }

    private void verifyVectorDrawables(int[] resIds, int[] goldenImages, int[] edges,
            int[] stateSet) throws XmlPullParserException, IOException {
        for (int i = 0; i < resIds.length; i++) {
            // Setup VectorDrawable from xml file and draw into the bitmap.
            mVectorDrawable = VectorDrawableCompat.create(mResources, resIds[i], mTheme);
            mVectorDrawable.setBounds(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);
            if (stateSet != null) {
                mVectorDrawable.setState(stateSet);
            }

            mBitmap.eraseColor(TRANSPARENT);
            mVectorDrawable.draw(mCanvas);

            if (DBG_DUMP_PNG) {
                saveVectorDrawableIntoPNG(mBitmap, resIds, i, stateSet);
            } else {
                // Start to compare
                Bitmap golden = BitmapFactory.decodeResource(mResources, goldenImages[i]);
                Bitmap edge = null;
                if (edges[i] > 0) {
                    edge = BitmapFactory.decodeResource(mResources, edges[i]);
                }
                compareImages(mBitmap, golden, edge, mResources.getString(resIds[i]));
            }
        }
    }

    // This is only for debugging or golden image (re)generation purpose.
    private void saveVectorDrawableIntoPNG(Bitmap bitmap, int[] resIds, int index, int[] stateSet)
            throws IOException {
        // Save the image to the disk.
        FileOutputStream out = null;
        try {
            String outputFolder = "/sdcard/temp/";
            File folder = new File(outputFolder);
            if (!folder.exists()) {
                folder.mkdir();
            }
            String originalFilePath = mResources.getString(resIds[index]);
            File originalFile = new File(originalFilePath);
            String fileFullName = originalFile.getName();
            String fileTitle = fileFullName.substring(0, fileFullName.lastIndexOf("."));
            String stateSetTitle = getTitleForStateSet(stateSet);
            String outputFilename = outputFolder + fileTitle + "_golden" + stateSetTitle + ".png";
            File outputFile = new File(outputFilename);
            if (!outputFile.exists()) {
                outputFile.createNewFile();
            }

            out = new FileOutputStream(outputFile, false);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            Log.v(LOGTAG, "Write test No." + index + " to file successfully.");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    /**
     * Generates an underline-delimited list of states in a given state set.
     * <p/>
     * For example, the array {@code {R.attr.state_pressed}} would return
     * {@code "_pressed"}.
     *
     * @param stateSet a state set
     * @return a string representing the state set, or the empty string if the
     * state set is empty or {@code null}
     */
    private String getTitleForStateSet(int[] stateSet) {
        if (stateSet == null || stateSet.length == 0) {
            return "";
        }

        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < stateSet.length; i++) {
            builder.append('_');

            final String state = mResources.getResourceName(stateSet[i]);
            final int stateIndex = state.indexOf("state_");
            if (stateIndex >= 0) {
                builder.append(state.substring(stateIndex + 6));
            } else {
                builder.append(stateSet[i]);
            }
        }

        return builder.toString();
    }

    private void compareImages(Bitmap ideal, Bitmap given, Bitmap edge, String filename) {
        int idealWidth = ideal.getWidth();
        int idealHeight = ideal.getHeight();

        assertTrue(idealWidth == given.getWidth());
        assertTrue(idealHeight == given.getHeight());

        int totalDiffPixelCount = 0;
        float totalPixelCount = idealWidth * idealHeight;
        for (int x = 0; x < idealWidth; x++) {
            for (int y = 0; y < idealHeight; y++) {
                int idealColor = ideal.getPixel(x, y);
                int givenColor = given.getPixel(x, y);
                if (idealColor == givenColor) {
                    continue;
                }

                float totalError = 0;
                totalError += Math.abs(red(idealColor) - red(givenColor));
                totalError += Math.abs(green(idealColor) - green(givenColor));
                totalError += Math.abs(blue(idealColor) - blue(givenColor));
                totalError += Math.abs(alpha(idealColor) - alpha(givenColor));

                if ((totalError / 1024.0f) >= PIXEL_ERROR_THRESHOLD) {
                    if (edge == null) {
                        fail((filename + ": totalError is " + totalError));
                    } else if (getColorFromBitmap(edge, x - 1, y - 1) > 0
                            || getColorFromBitmap(edge, x - 1, y) > 0
                            || getColorFromBitmap(edge, x - 1, y + 1) > 0
                            || getColorFromBitmap(edge, x, y - 1) > 0
                            || getColorFromBitmap(edge, x, y + 1) > 0
                            || getColorFromBitmap(edge, x + 1, y - 1) > 0
                            || getColorFromBitmap(edge, x + 1, y) > 0
                            || getColorFromBitmap(edge, x + 1, y + 1) > 0) {
                        continue;
                    }
                }

                if ((totalError / 1024.0f) >= PIXEL_DIFF_THRESHOLD) {
                    totalDiffPixelCount++;
                }

            }
        }
        if ((totalDiffPixelCount / totalPixelCount) >= PIXEL_DIFF_COUNT_THRESHOLD) {
            fail((filename + ": totalDiffPixelCount is " + totalDiffPixelCount));
        }

    }

    private static int getColorFromBitmap(Bitmap bitmap, int x, int y) {
        if (x < 0 || y < 0) {
            return 0;
        }

        if (x >= bitmap.getWidth() || y >= bitmap.getHeight()) {
            return 0;
        }
        return bitmap.getPixel(x, y);
    }

    @Test
    public void testGetChangingConfigurations() {
        VectorDrawableCompat vectorDrawable =
                VectorDrawableCompat.create(mResources, TEST_ICON, mTheme);
        Drawable.ConstantState constantState = vectorDrawable.getConstantState();

        // default
        assertEquals(0, constantState.getChangingConfigurations());
        assertEquals(0, vectorDrawable.getChangingConfigurations());

        // change the drawable's configuration does not affect the state's configuration
        vectorDrawable.setChangingConfigurations(0xff);
        assertEquals(0xff, vectorDrawable.getChangingConfigurations());
        assertEquals(0, constantState.getChangingConfigurations());

        // the state's configuration get refreshed
        constantState = vectorDrawable.getConstantState();
        assertEquals(0xff, constantState.getChangingConfigurations());

        // set a new configuration to drawable
        vectorDrawable.setChangingConfigurations(0xff00);
        assertEquals(0xff, constantState.getChangingConfigurations());
        assertEquals(0xffff, vectorDrawable.getChangingConfigurations());
    }

    @Test
    public void testGetConstantState() {
        VectorDrawableCompat vectorDrawable =
                VectorDrawableCompat.create(mResources, R.drawable.vector_icon_delete, mTheme);
        Drawable.ConstantState constantState = vectorDrawable.getConstantState();
        assertNotNull(constantState);
        assertEquals(0, constantState.getChangingConfigurations());

        vectorDrawable.setChangingConfigurations(1);
        constantState = vectorDrawable.getConstantState();
        assertNotNull(constantState);
        assertEquals(1, constantState.getChangingConfigurations());
    }

    @Test
    public void testMutate() {
        VectorDrawableCompat d1 =
                VectorDrawableCompat.create(mResources, TEST_ICON, mTheme);
        VectorDrawableCompat d2 =
                (VectorDrawableCompat) d1.getConstantState().newDrawable(mResources);
        VectorDrawableCompat d3 =
                (VectorDrawableCompat) d1.getConstantState().newDrawable(mResources);

        // d1 will be mutated, while d2 / d3 will not.
        int originalAlpha = d2.getAlpha();

        d1.mutate();
        d1.setAlpha(0x40);
        assertEquals(0x40, d1.getAlpha());
        assertEquals(originalAlpha, d2.getAlpha());
        assertEquals(originalAlpha, d3.getAlpha());

        d2.mutate();
        d2.setAlpha(0x20);
        assertEquals(0x40, d1.getAlpha());
        assertEquals(0x20, d2.getAlpha());
        assertEquals(originalAlpha, d3.getAlpha());
    }

    @Test
    public void testBounds() {
        VectorDrawableCompat vectorDrawable =
                VectorDrawableCompat.create(mResources, R.drawable.vector_icon_delete, mTheme);
        Rect expectedRect = new Rect(0, 0, 100, 100);
        vectorDrawable.setBounds(0, 0, 100, 100);
        Rect rect = vectorDrawable.getBounds();
        assertEquals("Bounds should be same value for setBound(int ...)", rect, expectedRect);

        vectorDrawable.setBounds(expectedRect);
        rect = vectorDrawable.getBounds();
        assertEquals("Bounds should be same value for setBound(Rect)", rect, expectedRect);

        vectorDrawable.copyBounds(rect);
        assertEquals("Bounds should be same value for copyBounds", rect, expectedRect);
    }

    @Test
    public void testTint() {
        VectorDrawableCompat d =
                VectorDrawableCompat.create(mResources, R.drawable.heart, mTheme);

        d.setBounds(0, 0, 64, 64);
        Bitmap bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        d.draw(canvas);
        assertEquals(0xff00fff0, bitmap.getPixel(32, 32));
    }

    @Test
    public void testGetColorFilter() {
        VectorDrawableCompat d =
                VectorDrawableCompat.create(mResources, R.drawable.heart, mTheme);

        d.setBounds(0, 0, 64, 64);

        int color = Color.BLACK;
        ColorFilter blackFilter = new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_OVER);
        d.setColorFilter(blackFilter);
        assertEquals(blackFilter, d.getColorFilter());

        Bitmap bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        d.draw(canvas);
        assertEquals(color, bitmap.getPixel(32, 32));

        color = Color.WHITE;
        ColorFilter whiteFilter = new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_OVER);
        d.setColorFilter(whiteFilter);
        assertEquals(whiteFilter, d.getColorFilter());

        d.draw(canvas);
        assertEquals(color, bitmap.getPixel(32, 32));
    }

    /**
     * Test VectorDrawableCompat#setColorFilter(int, Mode)
     */
    @Test
    public void testSetColorFilter() {
        VectorDrawableCompat d =
                VectorDrawableCompat.create(mResources, R.drawable.heart, mTheme);
        int color = Color.CYAN;
        d.setBounds(0, 0, 64, 64);
        d.setColorFilter(color, PorterDuff.Mode.SRC_OVER);

        Bitmap bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        d.draw(canvas);
        assertEquals(color, bitmap.getPixel(32, 32));

        color = Color.YELLOW;
        d.setColorFilter(color, PorterDuff.Mode.SRC_OVER);
        d.draw(canvas);
        assertEquals(color, bitmap.getPixel(32, 32));
    }
}
