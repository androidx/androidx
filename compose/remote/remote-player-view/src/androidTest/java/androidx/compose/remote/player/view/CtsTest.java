/*
 * Copyright (C) 2024 The Android Open Source Project
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
package androidx.compose.remote.player.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;

import androidx.compose.remote.player.view.platform.RemoteComposeView;
import androidx.compose.remote.player.view.test.R;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.runners.Parameterized;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;

/** Test emulates the CTS useful to verify if we are still passing CTS */
@RunWith(Parameterized.class)
@SdkSuppress(minSdkVersion = 34)
@Category(JUnit4.class)
public class CtsTest {

    @Parameterized.Parameters(name = "{index}: {2}")
    public static Collection<Object[]> data() {
        return Arrays.asList(
                new Object[][] {
                    {R.drawable.blend_clear, R.raw.blend_clear, "blend_clear"},
                    {R.drawable.blend_color, R.raw.blend_color, "blend_color"},
                    {R.drawable.blend_color_burn, R.raw.blend_color_burn, "blend_color_burn"},
                    {R.drawable.blend_color_dodge, R.raw.blend_color_dodge, "blend_color_dodge"},
                    {R.drawable.blend_darken, R.raw.blend_darken, "blend_darken"},
                    {R.drawable.blend_difference, R.raw.blend_difference, "blend_difference"},
                    {R.drawable.blend_dst, R.raw.blend_dst, "blend_dst"},
                    {R.drawable.blend_dst_atop, R.raw.blend_dst_atop, "blend_dst_atop"},
                    {R.drawable.blend_dst_in, R.raw.blend_dst_in, "blend_dst_in"},
                    {R.drawable.blend_dst_out, R.raw.blend_dst_out, "blend_dst_out"},
                    {R.drawable.blend_dst_over, R.raw.blend_dst_over, "blend_dst_over"},
                    {R.drawable.blend_exclusion, R.raw.blend_exclusion, "blend_exclusion"},
                    {R.drawable.blend_hard_light, R.raw.blend_hard_light, "blend_hard_light"},
                    {R.drawable.blend_hue, R.raw.blend_hue, "blend_hue"},
                    {R.drawable.blend_lighten, R.raw.blend_lighten, "blend_lighten"},
                    {R.drawable.blend_luminosity, R.raw.blend_luminosity, "blend_luminosity"},
                    {R.drawable.blend_modulate, R.raw.blend_modulate, "blend_modulate"},
                    {R.drawable.blend_multiply, R.raw.blend_multiply, "blend_multiply"},
                    {R.drawable.blend_overlay, R.raw.blend_overlay, "blend_overlay"},
                    {R.drawable.blend_plus, R.raw.blend_plus, "blend_plus"},
                    {R.drawable.blend_saturation, R.raw.blend_saturation, "blend_saturation"},
                    {R.drawable.blend_screen, R.raw.blend_screen, "blend_screen"},
                    {R.drawable.blend_soft_light, R.raw.blend_soft_light, "blend_soft_light"},
                    {R.drawable.blend_src, R.raw.blend_src, "blend_src"},
                    {R.drawable.blend_src_atop, R.raw.blend_src_atop, "blend_src_atop"},
                    {R.drawable.blend_src_in, R.raw.blend_src_in, "blend_src_in"},
                    {R.drawable.blend_src_out, R.raw.blend_src_out, "blend_src_out"},
                    {R.drawable.blend_src_over, R.raw.blend_src_over, "blend_src_over"},
                    {R.drawable.blend_xor, R.raw.blend_xor, "blend_xor"},
                    {R.drawable.clip_path, R.raw.clip_path, "clip_path"},
                    {R.drawable.clip_rect, R.raw.clip_rect, "clip_rect"},
                    {R.drawable.draw_text, R.raw.draw_text, "draw_text"},
                    {R.drawable.linear_gradient_1, R.raw.linear_gradient_1, "linear_gradient_1"},
                    {R.drawable.linear_gradient_2, R.raw.linear_gradient_2, "linear_gradient_2"},
                    {R.drawable.matrix_rotate_1, R.raw.matrix_rotate_1, "matrix_rotate_1"},
                    {R.drawable.matrix_rotate_2, R.raw.matrix_rotate_2, "matrix_rotate_2"},
                    {R.drawable.matrix_scale_1, R.raw.matrix_scale_1, "matrix_scale_1"},
                    {R.drawable.matrix_scale_2, R.raw.matrix_scale_2, "matrix_scale_2"},
                    {R.drawable.matrix_skew, R.raw.matrix_skew, "matrix_skew"},
                    {R.drawable.matrix_translate, R.raw.matrix_translate, "matrix_translate"},
                    {R.drawable.multi_path, R.raw.multi_path, "multi_path"},
                    {
                        R.drawable.multiple_draw_commands,
                        R.raw.multiple_draw_commands,
                        "multiple_draw_commands"
                    },
                    {
                        R.drawable.paint_filter_bitmap_1,
                        R.raw.paint_filter_bitmap_1,
                        "paint_filter_bitmap_1"
                    },
                    {
                        R.drawable.paint_filter_bitmap_2,
                        R.raw.paint_filter_bitmap_2,
                        "paint_filter_bitmap_2"
                    },
                    {
                        R.drawable.paint_filter_bitmap_3,
                        R.raw.paint_filter_bitmap_3,
                        "paint_filter_bitmap_3"
                    },
                    {R.drawable.paint_set_color, R.raw.paint_set_color, "paint_set_color"},
                    {R.drawable.paint_set_stroke, R.raw.paint_set_stroke, "paint_set_stroke"},
                    {R.drawable.paint_stroke_miter, R.raw.paint_stroke_miter, "paint_stroke_miter"},
                    {R.drawable.path_cubic_to, R.raw.path_cubic_to, "path_cubic_to"},
                    {R.drawable.path_line_to, R.raw.path_line_to, "path_line_to"},
                    {R.drawable.path_quad_to, R.raw.path_quad_to, "path_quad_to"},
                    {R.drawable.path_tween_test_1, R.raw.path_tween_test_1, "path_tween_test_1"},
                    {R.drawable.path_tween_test_2, R.raw.path_tween_test_2, "path_tween_test_2"},
                    {R.drawable.porter_add, R.raw.porter_add, "porter_add"},
                    {R.drawable.porter_clear, R.raw.porter_clear, "porter_clear"},
                    {R.drawable.porter_darken, R.raw.porter_darken, "porter_darken"},
                    {R.drawable.porter_dst, R.raw.porter_dst, "porter_dst"},
                    {R.drawable.porter_dst_atop, R.raw.porter_dst_atop, "porter_dst_atop"},
                    {R.drawable.porter_dst_in, R.raw.porter_dst_in, "porter_dst_in"},
                    {R.drawable.porter_dst_out, R.raw.porter_dst_out, "porter_dst_out"},
                    {R.drawable.porter_dst_over, R.raw.porter_dst_over, "porter_dst_over"},
                    {R.drawable.porter_lighten, R.raw.porter_lighten, "porter_lighten"},
                    {R.drawable.porter_multiply, R.raw.porter_multiply, "porter_multiply"},
                    {R.drawable.porter_overlay, R.raw.porter_overlay, "porter_overlay"},
                    {R.drawable.porter_screen, R.raw.porter_screen, "porter_screen"},
                    {R.drawable.porter_src, R.raw.porter_src, "porter_src"},
                    {R.drawable.porter_src_atop, R.raw.porter_src_atop, "porter_src_atop"},
                    {R.drawable.porter_src_in, R.raw.porter_src_in, "porter_src_in"},
                    {R.drawable.porter_src_out, R.raw.porter_src_out, "porter_src_out"},
                    {R.drawable.porter_src_over, R.raw.porter_src_over, "porter_src_over"},
                    {R.drawable.porter_xor, R.raw.porter_xor, "porter_xor"},
                    {R.drawable.radial_gradient_1, R.raw.radial_gradient_1, "radial_gradient_1"},
                    {R.drawable.radial_gradient_2, R.raw.radial_gradient_2, "radial_gradient_2"},
                    {R.drawable.sweep_gradient_1, R.raw.sweep_gradient_1, "sweep_gradient_1"},
                    {R.drawable.sweep_gradient_2, R.raw.sweep_gradient_2, "sweep_gradient_2"},
                    {R.drawable.text_font, R.raw.text_font, "text_font"},
                    {
                        R.drawable.text_font_monospace,
                        R.raw.text_font_monospace,
                        "text_font_monospace"
                    },
                    {
                        R.drawable.text_font_san_serif,
                        R.raw.text_font_san_serif,
                        "text_font_san_serif"
                    },
                    {
                        R.drawable.text_font_san_serif_800,
                        R.raw.text_font_san_serif_800,
                        "text_font_san_serif_800"
                    },
                    {R.drawable.text_font_serif, R.raw.text_font_serif, "text_font_serif"},
                });
    }

    private final int mBitmapId;
    private final int mDocId;
    private final String mName;
    static Context sAppContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

    public CtsTest(int bitmap, int doc, String name) {
        mName = name;
        this.mDocId = doc;
        this.mBitmapId = bitmap;
    }

    @Test
    public void test() {
        Bitmap expectedBitmap = getBitmapFromFile(sAppContext, mBitmapId);
        int tw = expectedBitmap.getWidth();
        int th = expectedBitmap.getHeight();
        Bitmap localBitmap = blank(tw, th);
        RemoteComposeDocument fileDoc = getDoc(mDocId, sAppContext);
        Bitmap fromFileBitmap = docToBitmap(tw, th, sAppContext, fileDoc);
        float diff = compareImages(expectedBitmap, fromFileBitmap, mName);
        if (diff > 8.0f) {
            Assert.fail(diff + " > 8 rms");
        } else {
            System.out.println(" diff " + diff);
        }
    }

    private static Bitmap getBitmapFromFile(final Context context, final int resourceId) {
        final BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inScaled = false;
        return BitmapFactory.decodeResource(context.getResources(), resourceId, opts);
    }

    static RemoteComposeDocument getDoc(int resourceId, Context context) {

        try (InputStream fis = context.getResources().openRawResource(resourceId)) {
            // Compress and write the bitmap to the file
            RemoteComposeDocument doc = new RemoteComposeDocument(fis);
            return doc;
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static Bitmap docToBitmap(int w, int h, Context appContext, RemoteComposeDocument doc) {
        Bitmap bitmap = blank(w, h); // see below
        RemoteComposeView remoteCanvas = new RemoteComposeView(appContext);
        remoteCanvas.setDocument(doc);
        remoteCanvas.draw(new Canvas(bitmap));
        return bitmap;
    }

    // we create a pale blue background to fill
    static Bitmap blank(int w, int h) {
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(0xFFAABBCC);
        return bitmap;
    }

    static float compareImages(Bitmap bitmap1, Bitmap bitmap2) {
        if (bitmap1.getWidth() != bitmap2.getWidth()
                || bitmap1.getHeight() != bitmap2.getHeight()) {
            return 0;
        }
        float sqr_sum = 0;
        int count = 0;
        for (int y = 0; y < bitmap1.getHeight(); y++) {
            for (int x = 0; x < bitmap1.getWidth(); x++) {
                int pix1 = bitmap1.getPixel(x, y);
                float r1 = (pix1 & 0xFF0000) >> 16;
                float g1 = (pix1 & 0xFF00) >> 8;
                float b1 = (pix1 & 0xFF);
                int pix2 = bitmap2.getPixel(x, y);
                float r2 = (pix2 & 0xFF0000) >> 16;
                float g2 = (pix2 & 0xFF00) >> 8;
                float b2 = (pix2 & 0xFF);
                sqr_sum += (r1 - r2) * (r1 - r2);
                sqr_sum += (g1 - g2) * (g1 - g2);
                sqr_sum += (b1 - b2) * (b1 - b2);
                count += 3;
            }
        }
        return (float) Math.sqrt(sqr_sum / count);
    }

    private static float compareImages(Bitmap bitmap1, Bitmap bitmap2, String testLabel) {
        if (bitmap1.getWidth() != bitmap2.getWidth()
                || bitmap1.getHeight() != bitmap2.getHeight()) {
            throw new IllegalArgumentException(
                    "Size mismatches when running "
                            + testLabel
                            + " expected:[w="
                            + bitmap1.getWidth()
                            + ",h="
                            + bitmap1.getHeight()
                            + "]"
                            + " actual:[w="
                            + bitmap2.getWidth()
                            + ",h="
                            + bitmap2.getHeight()
                            + "]");
        }
        float sqr_sum = 0;
        int count = 0;
        for (int y = 1; y < bitmap1.getHeight() - 1; y++) {
            for (int x = 1; x < bitmap1.getWidth() - 1; x++) {
                int pix1 = bitmap1.getPixel(x, y);
                if (pix1 != bitmap1.getPixel(x, y - 1)
                        || pix1 != bitmap1.getPixel(x, y + 1)
                        || pix1 != bitmap1.getPixel(x - 1, y - 1)
                        || pix1 != bitmap1.getPixel(x - 1, y - 1)) {
                    // Skips if a pixel's color doesn't match its neighboring pixel
                    continue;
                }
                // Ignores least significant bit
                float r1 = (pix1 & 0xFF0000) >> 17;
                float g1 = (pix1 & 0xFF00) >> 9;
                float b1 = (pix1 & 0xFF) >> 1;
                int pix2 = bitmap2.getPixel(x, y);
                float r2 = (pix2 & 0xFF0000) >> 17;
                float g2 = (pix2 & 0xFF00) >> 9;
                float b2 = (pix2 & 0xFF) >> 1;
                sqr_sum += (r1 - r2) * (r1 - r2);
                sqr_sum += (g1 - g2) * (g1 - g2);
                sqr_sum += (b1 - b2) * (b1 - b2);
                count += 3;
            }
        }
        return (float) Math.sqrt(sqr_sum / count);
    }
}
