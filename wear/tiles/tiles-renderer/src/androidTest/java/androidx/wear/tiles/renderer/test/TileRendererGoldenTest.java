/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.tiles.renderer.test;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static androidx.wear.tiles.renderer.test.GoldenTestActivity.EXTRA_LAYOUT_KEY;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Build;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.MeasureSpec;
import android.widget.FrameLayout;

import androidx.core.content.ContextCompat;
import androidx.test.core.app.ActivityScenario;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.screenshot.AndroidXScreenshotTestRule;
import androidx.test.screenshot.matchers.MSSIMMatcher;
import androidx.wear.protolayout.LayoutElementBuilders;
import androidx.wear.protolayout.ResourceBuilders;
import androidx.wear.protolayout.proto.LayoutElementProto.Layout;
import androidx.wear.protolayout.proto.LayoutElementProto.LayoutElement;
import androidx.wear.protolayout.proto.ResourceProto.AndroidImageResourceByResId;
import androidx.wear.protolayout.proto.ResourceProto.ImageFormat;
import androidx.wear.protolayout.proto.ResourceProto.ImageResource;
import androidx.wear.protolayout.proto.ResourceProto.InlineImageResource;
import androidx.wear.protolayout.proto.ResourceProto.Resources;
import androidx.wear.protolayout.protobuf.ByteString;
import androidx.wear.tiles.renderer.TileRenderer;

import com.google.protobuf.TextFormat;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

@RunWith(Parameterized.class)
@LargeTest
@SdkSuppress(
        minSdkVersion = 35,
        maxSdkVersion = 35
)
public class TileRendererGoldenTest {
    // Defines the name of the textproto and whether to use fullscreen or not.
    @Parameterized.Parameters(name = "{0} - fullscreen={1}")
    public static Collection<Object[]> data() {
        return Arrays.asList(
                new Object[][] {
                    {"all_modifiers", false},
                    {"arc_above_360", false},
                    {"arc_alignment_mixed_types", true},
                    {"arc_alignment", false},
                    {"arc_anchors", false},
                    {"arc_text_and_lines", false},
                    {"arc_with_buttons_rotated", true},
                    {"arc_with_buttons_unrotated", true},
                    {"box_with_corners_and_border_rtlaware", false},
                    {"box_with_corners_and_border", false},
                    {"box_with_fixed_size", false},
                    {"broken_drawable", false},
                    {"column_with_alignment_rtlaware", false},
                    {"column_with_alignment", false},
                    {"column_with_height", false},
                    {"expanded_box_horizontal_right_align", false},
                    {"expanded_box_horizontal", false},
                    {"expanded_box_vertical", false},
                    {"expanded_children_in_row", false},
                    {"font_weights_in_arc", false},
                    {"font_weights_in_spannable", false},
                    {"image_expanded_to_parent", false},
                    {"image_expand_modes", false},
                    {"image_oversized_in_box_proportional", false},
                    {"image_oversized_in_box", false},
                    {"image_proportional_resize", false},
                    {"image_with_dimensions", false},
                    {"image_with_inline_data", false},
                    {"image_with_padding", true},
                    {"line_in_arc", false},
                    {"line_multi_height", true},
                    {"long_text", false},
                    {"mixed_language_text", false},
                    {"multi_line_text_alignment", false},
                    {"row_column_space_test", false},
                    {"row_with_alignment", false},
                    {"row_with_width", false},
                    {"simple_text", false},
                    {"single_line_text_alignment", false},
                    {"spacer_horizontal", false},
                    {"spacer_in_arc", true},
                    {"spacer_vertical", false},
                    {"spannable_image", false},
                    {"spannable_image_with_clickable", false},
                    {"spannable_image_wrapped", false},
                    {"spannable_text", false},
                    {"text_and_image_in_box", false},
                    {"text_default_size", false},
                    {"text_in_column", false},
                    {"text_in_row", false},
                    {"text_with_font_weights_italic", false},
                    {"text_with_font_weights", false},
                    {"text_with_spacing", false},
                });
    }

    @Rule
    public AndroidXScreenshotTestRule screenshotRule =
            new AndroidXScreenshotTestRule("wear/tiles/tiles-renderer");

    // This isn't totally ideal right now. The screenshot tests run on a phone, so emulate some
    // watch dimensions here.
    private static final int SCREEN_WIDTH = 390;
    private static final int SCREEN_HEIGHT = 390;

    private static final int INLINE_IMAGE_WIDTH = 8;
    private static final int INLINE_IMAGE_HEIGHT = 8;
    private static final int INLINE_IMAGE_PIXEL_STRIDE = 2; // RGB565 = 2 bytes per pixel

    private final String mProtoFile;
    private final boolean mUseFullScreen;

    public TileRendererGoldenTest(String protoFile, boolean useFullScreen) {
        mProtoFile = protoFile;
        mUseFullScreen = useFullScreen;
    }

    @Test
    public void renderer_goldenTest() throws Exception {
        int id =
                getApplicationContext()
                        .getResources()
                        .getIdentifier(mProtoFile, "raw", getApplicationContext().getPackageName());

        if (mUseFullScreen) {
            runFullScreenScreenshotTest(id, mProtoFile);
        } else {
            runSingleScreenshotTest(id, mProtoFile);
        }
    }

    private static Resources generateResources() {
        byte[] inlineImagePayload =
                new byte[INLINE_IMAGE_WIDTH * INLINE_IMAGE_HEIGHT * INLINE_IMAGE_PIXEL_STRIDE];

        // Generate a square image, with a white square in the center. This replaces an inline
        // payload as a byte array. We could hardcode it, but the autoformatter will ruin the
        // formatting.
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                int index = ((y * INLINE_IMAGE_WIDTH) + x) * INLINE_IMAGE_PIXEL_STRIDE;
                short color = 0x0000;

                if (y > 2 && y < 6 && x > 2 && x < 6) {
                    color = (short) 0xFFFF;
                }

                inlineImagePayload[index + 0] = (byte) ((color >> 0) & 0xFF);
                inlineImagePayload[index + 1] = (byte) ((color >> 8) & 0xFF);
            }
        }

        return Resources.newBuilder()
                .putIdToImage(
                        "android",
                        ImageResource.newBuilder()
                                .setAndroidResourceByResId(
                                        AndroidImageResourceByResId.newBuilder()
                                                .setResourceId(R.drawable.android_24dp))
                                .build())
                .putIdToImage(
                        "android_withbg_120dp",
                        ImageResource.newBuilder()
                                .setAndroidResourceByResId(
                                        AndroidImageResourceByResId.newBuilder()
                                                .setResourceId(R.drawable.android_withbg_120dp))
                                .build())
                .putIdToImage(
                        "inline",
                        ImageResource.newBuilder()
                                .setInlineResource(
                                        InlineImageResource.newBuilder()
                                                .setFormat(ImageFormat.IMAGE_FORMAT_RGB_565)
                                                .setWidthPx(INLINE_IMAGE_WIDTH)
                                                .setHeightPx(INLINE_IMAGE_HEIGHT)
                                                .setData(ByteString.copyFrom(inlineImagePayload)))
                                .build())
                .putIdToImage(
                        "broken_image",
                        ImageResource.newBuilder()
                                .setAndroidResourceByResId(
                                        AndroidImageResourceByResId.newBuilder()
                                                .setResourceId(R.drawable.broken_drawable))
                                .build())
                .putIdToImage(
                        "missing_image",
                        ImageResource.newBuilder()
                                .setAndroidResourceByResId(
                                        AndroidImageResourceByResId.newBuilder().setResourceId(-1))
                                .build())
                .build();
    }

    private LayoutElement parseLayoutElement(int protoResId) throws IOException {
        // This is a hack, but use the full proto lib to translate the textproto into a serialized
        // proto, then pass into a Layout.
        TextFormat.Parser parser = TextFormat.getParser();
        androidx.wear.tiles.testing.proto.LayoutElementProto.LayoutElement.Builder
                layoutElementProto =
                        androidx.wear.tiles.testing.proto.LayoutElementProto.LayoutElement
                                .newBuilder();

        InputStream rawResStream =
                getApplicationContext().getResources().openRawResource(protoResId);
        try (InputStreamReader reader = new InputStreamReader(rawResStream)) {
            parser.merge(reader, layoutElementProto);
        }

        byte[] contents = layoutElementProto.build().toByteArray();

        return LayoutElement.parseFrom(contents);
    }

    private void runSingleScreenshotTest(int protoResId, String expectedKey) throws Exception {
        FrameLayout mainFrame = new FrameLayout(getApplicationContext());
        mainFrame.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);

        Context appContext = getApplicationContext();
        LayoutElement rootElement = parseLayoutElement(protoResId);

        // Inflate and go!
        TileRenderer renderer =
                new TileRenderer.Builder(
                                appContext,
                                ContextCompat.getMainExecutor(getApplicationContext()),
                                i -> {})
                        .build();

        View firstChild =
                renderer.inflateAsync(
                                LayoutElementBuilders.Layout.fromProto(
                                        Layout.newBuilder().setRoot(rootElement).build()),
                                ResourceBuilders.Resources.fromProto(generateResources()),
                                mainFrame)
                        .get(30, TimeUnit.MILLISECONDS);

        if (firstChild == null) {
            throw new RuntimeException("Failed to inflate " + expectedKey);
        }

        // Simulate what the thing outside the renderer should do. Fix the frame at the "screen"
        // size, and center the contents.
        FrameLayout.LayoutParams layoutParams =
                (FrameLayout.LayoutParams) firstChild.getLayoutParams();
        layoutParams.gravity = Gravity.CENTER;

        int screenWidth = MeasureSpec.makeMeasureSpec(SCREEN_WIDTH, MeasureSpec.EXACTLY);
        int screenHeight = MeasureSpec.makeMeasureSpec(SCREEN_HEIGHT, MeasureSpec.EXACTLY);

        mainFrame.measure(screenWidth, screenHeight);
        mainFrame.layout(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);

        // Blit it to a bitmap for further testing.
        Bitmap bmp = Bitmap.createBitmap(SCREEN_WIDTH, SCREEN_HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        mainFrame.draw(canvas);

        screenshotRule.assertBitmapAgainstGolden(bmp, expectedKey, new MSSIMMatcher());
    }

    @SuppressLint("BanThreadSleep")
    private static void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (Exception ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            Log.e("TileRendererGoldenTest", "Error sleeping", ex);
        }
    }

    private void runFullScreenScreenshotTest(int protoResId, String expectedKEy) throws Exception {
        LayoutElement rootElement = parseLayoutElement(protoResId);
        Layout layout = Layout.newBuilder().setRoot(rootElement).build();

        Intent startIntent =
                new Intent(
                        InstrumentationRegistry.getInstrumentation().getTargetContext(),
                        GoldenTestActivity.class);
        startIntent.putExtra(EXTRA_LAYOUT_KEY, layout.toByteArray());

        ActivityScenario<GoldenTestActivity> ignored = ActivityScenario.launch(startIntent);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        // Wait 100ms after launching the activity. This allows for the old white layout in the
        // bootstrap activity to fully go away before proceeding.
        sleep(100);

        Bitmap bitmap =
                Bitmap.createBitmap(
                        InstrumentationRegistry.getInstrumentation()
                                .getUiAutomation()
                                .takeScreenshot(),
                        0,
                        0,
                        SCREEN_WIDTH,
                        SCREEN_HEIGHT);

        // Increase the threshold of Structural Similarity Index for image comparison to 0.995,
        // so that we do not miss the image differences.
        screenshotRule.assertBitmapAgainstGolden(bitmap, expectedKEy, new MSSIMMatcher(0.995));
    }
}
