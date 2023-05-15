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

package androidx.wear.tiles.material;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static androidx.wear.tiles.material.RunnerUtils.SCREEN_HEIGHT;
import static androidx.wear.tiles.material.RunnerUtils.SCREEN_WIDTH;
import static androidx.wear.tiles.material.RunnerUtils.runSingleScreenshotTest;
import static androidx.wear.tiles.material.TestCasesGenerator.XXXL_SCALE_SUFFIX;
import static androidx.wear.tiles.material.TestCasesGenerator.generateTestCases;

import android.content.Context;
import android.util.DisplayMetrics;

import androidx.annotation.Dimension;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.screenshot.AndroidXScreenshotTestRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

@RunWith(Parameterized.class)
@LargeTest
@SuppressWarnings("deprecation")
public class MaterialGoldenXLTest {
    /* We set DisplayMetrics in the data() method for creating test cases. However, when running all
    tests together, first all parametrization (data()) methods are called, and then individual
    tests, causing that actual DisplayMetrics will be different. So we need to restore it before
    each test. */
    private static final DisplayMetrics DISPLAY_METRICS_FOR_TEST = new DisplayMetrics();
    private static final DisplayMetrics OLD_DISPLAY_METRICS = new DisplayMetrics();

    private static final float FONT_SCALE_XXXL = 1.24f;

    private final androidx.wear.tiles.LayoutElementBuilders.LayoutElement mLayoutElement;
    private final String mExpected;

    @Rule
    public AndroidXScreenshotTestRule mScreenshotRule =
            new AndroidXScreenshotTestRule("wear/wear-tiles-material");

    public MaterialGoldenXLTest(
            String expected,
            androidx.wear.tiles.LayoutElementBuilders.LayoutElement layoutElement) {
        mLayoutElement = layoutElement;
        mExpected = expected;
    }

    @Dimension(unit = Dimension.DP)
    static int pxToDp(int px, float scale) {
        return (int) ((px - 0.5f) / scale);
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        DisplayMetrics currentDisplayMetrics = new DisplayMetrics();
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        currentDisplayMetrics.setTo(displayMetrics);
        displayMetrics.scaledDensity *= FONT_SCALE_XXXL;

        InstrumentationRegistry.getInstrumentation()
                .getContext()
                .getResources()
                .getDisplayMetrics()
                .setTo(displayMetrics);
        InstrumentationRegistry.getInstrumentation()
                .getTargetContext()
                .getResources()
                .getDisplayMetrics()
                .setTo(displayMetrics);

        DISPLAY_METRICS_FOR_TEST.setTo(displayMetrics);

        float scale = displayMetrics.density;
        androidx.wear.tiles.DeviceParametersBuilders.DeviceParameters deviceParameters =
                new androidx.wear.tiles.DeviceParametersBuilders.DeviceParameters.Builder()
                        .setScreenWidthDp(pxToDp(SCREEN_WIDTH, scale))
                        .setScreenHeightDp(pxToDp(SCREEN_HEIGHT, scale))
                        .setScreenDensity(displayMetrics.density)
                        // Not important for components.
                        .setScreenShape(
                                androidx.wear.tiles.DeviceParametersBuilders.SCREEN_SHAPE_RECT)
                        .build();

        Map<String, androidx.wear.tiles.LayoutElementBuilders.LayoutElement> testCases =
                generateTestCases(context, deviceParameters, XXXL_SCALE_SUFFIX);

        // Restore state before this method, so other test have correct context.
        InstrumentationRegistry.getInstrumentation()
                .getContext()
                .getResources()
                .getDisplayMetrics()
                .setTo(currentDisplayMetrics);
        InstrumentationRegistry.getInstrumentation()
                .getTargetContext()
                .getResources()
                .getDisplayMetrics()
                .setTo(currentDisplayMetrics);

        return testCases.entrySet().stream()
                .map(test -> new Object[] {test.getKey(), test.getValue()})
                .collect(Collectors.toList());
    }

    @Parameterized.BeforeParam
    public static void restoreBefore() {
        OLD_DISPLAY_METRICS.setTo(getApplicationContext().getResources().getDisplayMetrics());
        getApplicationContext().getResources().getDisplayMetrics().setTo(DISPLAY_METRICS_FOR_TEST);
    }

    @Parameterized.AfterParam
    public static void restoreAfter() {
        getApplicationContext().getResources().getDisplayMetrics().setTo(OLD_DISPLAY_METRICS);
    }

    @SdkSuppress(maxSdkVersion = 32) // b/271486183
    @Test
    public void test() {
        runSingleScreenshotTest(mScreenshotRule, mLayoutElement, mExpected);
    }
}
