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

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.screenshot.AndroidXScreenshotTestRule;
import androidx.test.screenshot.matchers.MSSIMMatcher;
import androidx.wear.tiles.material.testapp.GoldenTestActivity;

@SuppressWarnings("deprecation")
public class RunnerUtils {
    // This isn't totally ideal right now. The screenshot tests run on a phone, so emulate some
    // watch dimensions here.
    public static final int SCREEN_WIDTH = 390;
    public static final int SCREEN_HEIGHT = 390;

    private RunnerUtils() {}

    public static void runSingleScreenshotTest(
            @NonNull AndroidXScreenshotTestRule rule,
            @NonNull androidx.wear.tiles.LayoutElementBuilders.LayoutElement layoutElement,
            @NonNull String expected) {
        byte[] layoutElementPayload = layoutElement.toLayoutElementProto().toByteArray();

        Intent startIntent =
                new Intent(
                        InstrumentationRegistry.getInstrumentation().getTargetContext(),
                        GoldenTestActivity.class);
        startIntent.putExtra("layout", layoutElementPayload);

        ActivityScenario<GoldenTestActivity> scenario = ActivityScenario.launch(startIntent);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        try {
            // Wait 1s after launching the activity. This allows for the old white layout in the
            // bootstrap activity to fully go away before proceeding.
            Thread.sleep(1000);
        } catch (Exception ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            Log.e("MaterialGoldenTest", "Error sleeping", ex);
        }

        Bitmap bitmap =
                Bitmap.createBitmap(
                        InstrumentationRegistry.getInstrumentation()
                                .getUiAutomation()
                                .takeScreenshot(),
                        0,
                        0,
                        SCREEN_WIDTH,
                        SCREEN_HEIGHT);
        rule.assertBitmapAgainstGolden(bitmap, expected, new MSSIMMatcher());

        // There's a weird bug (related to b/159805732) where, when calling .close() on
        // ActivityScenario or calling finish() and immediately exiting the test, the test can hang
        // on a white screen for 45s. Closing the activity here and waiting for 1s seems to fix
        // this.
        scenario.onActivity(Activity::finish);

        try {
            Thread.sleep(1000);
        } catch (Exception ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            Log.e("MaterialGoldenTest", "Error sleeping", ex);
        }
    }
}
