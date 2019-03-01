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

package androidx.camera.integration.core;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.Until;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

// Test application lifecycle when using CameraX.
@RunWith(AndroidJUnit4.class)
@SmallTest
public final class AppLifecycleTest {
    private static final String BASIC_SAMPLE_PACKAGE = "androidx.camera.integration.core";
    private static final int LAUNCH_TIMEOUT_MS = 5000;

    @Rule
    public ActivityTestRule<CameraXActivity> activityRule =
            new ActivityTestRule<>(CameraXActivity.class);
    @Rule
    public GrantPermissionRule cameraPermissionRule =
            GrantPermissionRule.grant(android.Manifest.permission.CAMERA);
    @Rule
    public GrantPermissionRule storagePermissionRule =
            GrantPermissionRule.grant(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
    @Rule
    public GrantPermissionRule audioPermissionRule =
            GrantPermissionRule.grant(android.Manifest.permission.RECORD_AUDIO);

    // Test that the application starts and reaches displaying a TextureView.
    @Test
    public void startupAndDisplayTextureView() {
        onView(withId(R.id.textureView)).check(matches(isDisplayed()));
    }

    // Run the app, press home, then restart the app, without clearing the previous task.
    @Test
    public void startCoreTestTwiceWithoutClearingPreviousInstance() {
        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        onView(withId(R.id.textureView)).check(matches(isDisplayed()));

        device.pressHome();

        final String launcherPackage = device.getLauncherPackageName();
        assertThat(launcherPackage, notNullValue());
        device.wait(Until.hasObject(By.pkg(launcherPackage).depth(0)), LAUNCH_TIMEOUT_MS);
        Context context = ApplicationProvider.getApplicationContext();
        final Intent intent = context.getPackageManager()
                .getLaunchIntentForPackage(BASIC_SAMPLE_PACKAGE);

        // Do not clear out any previous instances.
        context.startActivity(intent);
        device.wait(Until.hasObject(By.pkg(BASIC_SAMPLE_PACKAGE).depth(0)), LAUNCH_TIMEOUT_MS);

        onView(withId(R.id.textureView)).check(matches(isDisplayed()));
    }

    // Run the app, press home, then restart the app, clearing the previous task when restarting.
    @Test
    public void startCoreTestTwiceClearingPreviousInstance() {
        UiDevice mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        onView(withId(R.id.textureView)).check(matches(isDisplayed()));

        mDevice.pressHome();

        final String launcherPackage = mDevice.getLauncherPackageName();
        assertThat(launcherPackage, notNullValue());
        mDevice.wait(Until.hasObject(By.pkg(launcherPackage).depth(0)), LAUNCH_TIMEOUT_MS);
        Context context = ApplicationProvider.getApplicationContext();
        final Intent intent = context.getPackageManager()
                .getLaunchIntentForPackage(BASIC_SAMPLE_PACKAGE);

        // Clear out any previous instances.
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        } catch (NullPointerException exception) {
            Assert.fail("Could not set intent flag to clear activity.  Cannot properly test.");
        }
        context.startActivity(intent);
        mDevice.wait(Until.hasObject(By.pkg(BASIC_SAMPLE_PACKAGE).depth(0)), LAUNCH_TIMEOUT_MS);

        onView(withId(R.id.textureView)).check(matches(isDisplayed()));
    }
}
