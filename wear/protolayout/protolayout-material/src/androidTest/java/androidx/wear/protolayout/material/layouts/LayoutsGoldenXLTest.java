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

package androidx.wear.protolayout.material.layouts;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static androidx.wear.protolayout.material.RunnerUtils.SCREEN_HEIGHT;
import static androidx.wear.protolayout.material.RunnerUtils.SCREEN_WIDTH;
import static androidx.wear.protolayout.material.RunnerUtils.convertToTestParameters;
import static androidx.wear.protolayout.material.RunnerUtils.runSingleScreenshotTest;
import static androidx.wear.protolayout.material.RunnerUtils.waitForNotificationToDisappears;
import static androidx.wear.protolayout.material.layouts.TestCasesGenerator.XXXL_SCALE_SUFFIX;
import static androidx.wear.protolayout.material.layouts.TestCasesGenerator.generateTestCases;

import android.content.Context;
import android.util.DisplayMetrics;

import androidx.annotation.Dimension;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.screenshot.AndroidXScreenshotTestRule;
import androidx.wear.protolayout.DeviceParametersBuilders;
import androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters;
import androidx.wear.protolayout.material.RunnerUtils.TestCase;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Collection;
import java.util.List;

@RunWith(Parameterized.class)
@LargeTest
public class LayoutsGoldenXLTest {
    /* We set DisplayMetrics in the data() method for creating test cases. However, when running all
    tests together, first all parametrization (data()) methods are called, and then individual
    tests, causing that actual DisplayMetrics will be different. So we need to restore it before
    each test. */
    private static final DisplayMetrics DISPLAY_METRICS_FOR_TEST = new DisplayMetrics();
    private static final DisplayMetrics OLD_DISPLAY_METRICS = new DisplayMetrics();

    private static final float FONT_SCALE_XXXL = 1.24f;

    private final TestCase mTestCase;
    private final String mExpected;

    @Rule
    public AndroidXScreenshotTestRule mScreenshotRule =
            new AndroidXScreenshotTestRule("wear/wear-protolayout-material");

    public LayoutsGoldenXLTest(String expected, TestCase testCase) {
        mTestCase = testCase;
        mExpected = expected;
    }

    @Dimension(unit = Dimension.DP)
    static int pxToDp(int px, float scale) {
        return (int) ((px - 0.5f) / scale);
    }

    @SuppressWarnings("deprecation")
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
        DeviceParameters deviceParameters =
                new DeviceParameters.Builder()
                        .setScreenWidthDp(pxToDp(SCREEN_WIDTH, scale))
                        .setScreenHeightDp(pxToDp(SCREEN_HEIGHT, scale))
                        .setScreenDensity(displayMetrics.density)
                        // TODO(b/231543947): Add test cases for round screen.
                        .setScreenShape(DeviceParametersBuilders.SCREEN_SHAPE_RECT)
                        .build();

        List<Object[]> testCaseList =
                convertToTestParameters(
                        generateTestCases(context, deviceParameters, XXXL_SCALE_SUFFIX),
                        /* isForRtl= */ true,
                        /* isForLtr= */ true);

        // Restore state before this method, so other test have correct context. This is needed here
        // too, besides in restoreBefore and restoreAfter as the test cases builder uses the context
        // to apply font scaling, so we need that display metrics passed in. However, after
        // generating cases we need to restore the state as other data() methods in this package can
        // work correctly with the default state, as when the tests are run, first all data() static
        // methods are called, and then parameterized test cases.
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
        waitForNotificationToDisappears();

        return testCaseList;
    }

    @Parameterized.BeforeParam
    public static void restoreBefore() {
        // Set the state as it was in data() method when we generated test cases. This was
        // overridden by other static data() methods, so we need to restore it.
        OLD_DISPLAY_METRICS.setTo(getApplicationContext().getResources().getDisplayMetrics());
        getApplicationContext().getResources().getDisplayMetrics().setTo(DISPLAY_METRICS_FOR_TEST);
    }

    @Parameterized.AfterParam
    public static void restoreAfter() {
        // Restore the state to default, so the other tests and emulator have the correct starter
        // state.
        getApplicationContext().getResources().getDisplayMetrics().setTo(OLD_DISPLAY_METRICS);
    }

    @Test
    public void test() {
        runSingleScreenshotTest(mScreenshotRule, mTestCase, mExpected);
    }
}
