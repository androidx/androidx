/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.wear.protolayout.material;

import static androidx.wear.protolayout.material.test.GoldenTestActivity.VIEW_TAG;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;

import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.screenshot.AndroidXScreenshotTestRule;
import androidx.test.screenshot.matchers.MSSIMMatcher;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;
import androidx.wear.protolayout.LayoutElementBuilders.Layout;
import androidx.wear.protolayout.material.test.GoldenTestActivity;

import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RunnerUtils {
    // This isn't totally ideal right now. The screenshot tests run on a phone, so emulate some
    // watch dimensions here.
    public static final int SCREEN_WIDTH = 390;
    public static final int SCREEN_HEIGHT = 390;
    private static final int FIND_OBJECT_TIMEOUT_MILLIS = 1000;
    private static final UiDevice uiDevice =
            UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

    private RunnerUtils() {}

    public static void runSingleScreenshotTest(
            @NonNull AndroidXScreenshotTestRule rule,
            @NonNull TestCase testCase,
            @NonNull String expected) {
        if (testCase.isForLtr) {
            runSingleScreenshotTest(rule, testCase.mLayout, expected, /* isRtlDirection= */ false);
        }
        if (testCase.isForRtl) {
            runSingleScreenshotTest(
                    rule, testCase.mLayout, expected + "_rtl", /* isRtlDirection= */ true);
        }
    }

    public static void runSingleScreenshotTest(
            @NonNull AndroidXScreenshotTestRule rule,
            @NonNull Layout layout,
            @NonNull String expected,
            boolean isRtlDirection) {
        byte[] layoutPayload = layout.toByteArray();

        Intent startIntent =
                new Intent(
                        InstrumentationRegistry.getInstrumentation().getTargetContext(),
                        GoldenTestActivity.class);
        startIntent.putExtra("layout", layoutPayload);
        startIntent.putExtra(GoldenTestActivity.USE_RTL_DIRECTION, isRtlDirection);

        try (ActivityScenario<GoldenTestActivity> scenario = ActivityScenario.launch(startIntent)) {
            List<UiObject2> objects =
                    uiDevice.wait(Until.findObjects(By.desc(VIEW_TAG)), FIND_OBJECT_TIMEOUT_MILLIS);
            if (objects == null) {
                throw new IllegalStateException("Timed out waiting for the PL host view.");
            }
            if (objects.size() != 1) {
                throw new IllegalStateException(
                        "Expected 1 PL host view, but found " + objects.size());
            }
            Rect plViewRect = objects.get(0).getVisibleBounds();

            Bitmap bitmap =
                    Bitmap.createBitmap(
                            InstrumentationRegistry.getInstrumentation()
                                    .getUiAutomation()
                                    .takeScreenshot(),
                            plViewRect.left,
                            plViewRect.top,
                            plViewRect.width(),
                            plViewRect.height());
            rule.assertBitmapAgainstGolden(bitmap, expected, new MSSIMMatcher());
        }
    }

    @SuppressLint("BanThreadSleep")
    public static void waitForNotificationToDisappears() {
        try {
            // Wait for the initial notification to disappear.
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Log.e("MaterialGoldenTest", "Error sleeping", e);
        }
    }

    public static List<Object[]> convertToTestParameters(
            Map<String, Layout> testCases, boolean isForRtr, boolean isForLtr) {
        return testCases.entrySet().stream()
                .map(
                        test ->
                                new Object[] {
                                    test.getKey(), new TestCase(test.getValue(), isForRtr, isForLtr)
                                })
                .collect(Collectors.toList());
    }

    /** Holds testcase parameters. */
    public static final class TestCase {
        final Layout mLayout;
        final boolean isForRtl;
        final boolean isForLtr;

        public TestCase(Layout layout, boolean isForRtl, boolean isForLtr) {
            mLayout = layout;
            this.isForRtl = isForRtl;
            this.isForLtr = isForLtr;
        }
    }

    public static float getFontScale(Context context) {
        return context.getResources().getConfiguration().fontScale;
    }

    @SuppressWarnings("deprecation")
    public static void setFontScale(Context context, float fontScale) {
        Configuration newConfiguration =
                new Configuration(context.getResources().getConfiguration());
        newConfiguration.fontScale = fontScale;
        context.getResources()
                .updateConfiguration(newConfiguration, context.getResources().getDisplayMetrics());
    }
}
