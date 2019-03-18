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
import androidx.test.rule.GrantPermissionRule;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.Until;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

// Test application lifecycle when using CameraX.
@RunWith(AndroidJUnit4.class)
@SmallTest
public final class AppLifecycleTest {
    private static final String BASIC_SAMPLE_PACKAGE = "androidx.camera.integration.core";
    private static final int LAUNCH_TIMEOUT_MS = 5000;

    private final UiDevice mDevice =
            UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
    private final String mLauncherPackageName = mDevice.getLauncherPackageName();
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private final Intent mIntent = mContext.getPackageManager()
            .getLaunchIntentForPackage(BASIC_SAMPLE_PACKAGE);

    @Rule
    public GrantPermissionRule cameraPermissionRule =
            GrantPermissionRule.grant(android.Manifest.permission.CAMERA);
    @Rule
    public GrantPermissionRule storagePermissionRule =
            GrantPermissionRule.grant(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
    @Rule
    public GrantPermissionRule audioPermissionRule =
            GrantPermissionRule.grant(android.Manifest.permission.RECORD_AUDIO);

    @Before
    public void setup() {
        assertThat(mLauncherPackageName, notNullValue());
        returnHomeScreen();
    }


    // Test that the application starts and reaches displaying a TextureView.
    @Test
    public void startupAndDisplayTextureView() {
        mContext.startActivity(mIntent);
        waitUntilTextureViewIsReady();
    }

    // Run the app, press home, then restart the app, always creating new instance.
    @Test
    public void startCoreTestTwiceAlwaysNewInstance() {
        mContext.startActivity(mIntent);
        waitUntilTextureViewIsReady();

        returnHomeScreen();

        Intent newIntent = new Intent(Intent.ACTION_MAIN);
        newIntent.setClass(mContext, CameraXActivity.class);
        newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(newIntent);
        waitUntilTextureViewIsReady();
    }

    // Run the app, press home, then restart the app, clearing the previous task when restarting.
    // The behavior is the same to simulate press back key.
    @Test
    public void startCoreTestTwiceClearingPreviousInstance() {
        mContext.startActivity(mIntent);
        waitUntilTextureViewIsReady();

        returnHomeScreen();

        // Clear out any previous instances.
        mIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        mContext.startActivity(mIntent);
        waitUntilTextureViewIsReady();
    }


    // Run the app, press home, then restart the app, and in practically simulate app lifecycle
    // state when pressing home key.
    @Test
    public void startCoreTestTwiceToSimulatePauseResume() {
        mContext.startActivity(mIntent);
        waitUntilTextureViewIsReady();

        returnHomeScreen();

        mContext.startActivity(mIntent);
        waitUntilTextureViewIsReady();
    }

    private void waitUntilTextureViewIsReady() {
        mDevice.wait(Until.hasObject(By.pkg(BASIC_SAMPLE_PACKAGE).depth(0)), LAUNCH_TIMEOUT_MS);
        onView(withId(R.id.textureView)).check(matches(isDisplayed()));
    }

    private void returnHomeScreen() {
        mDevice.pressHome();
        mDevice.wait(Until.hasObject(By.pkg(mLauncherPackageName).depth(0)), LAUNCH_TIMEOUT_MS);
    }

}
