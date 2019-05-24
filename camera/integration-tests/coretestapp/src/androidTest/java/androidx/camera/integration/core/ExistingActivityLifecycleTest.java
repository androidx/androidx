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
import static org.junit.Assume.assumeTrue;

import android.content.Context;
import android.content.Intent;

import androidx.camera.integration.core.idlingresource.ElapsedTimeIdlingResource;
import androidx.camera.testing.CameraUtil;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.IdlingResource;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.Until;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

// Test application lifecycle when using CameraX.
@RunWith(AndroidJUnit4.class)
@LargeTest
public final class ExistingActivityLifecycleTest {
    private static final String BASIC_SAMPLE_PACKAGE = "androidx.camera.integration.core";
    private static final int LAUNCH_TIMEOUT_MS = 5000;
    private static final int IDLE_TIMEOUT_MS = 1000;


    private final UiDevice mDevice =
            UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
    private final String mLauncherPackageName = mDevice.getLauncherPackageName();
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private final Intent mIntent = mContext.getPackageManager()
            .getLaunchIntentForPackage(BASIC_SAMPLE_PACKAGE);

    @Rule
    public GrantPermissionRule mCameraPermissionRule =
            GrantPermissionRule.grant(android.Manifest.permission.CAMERA);
    @Rule
    public GrantPermissionRule mStoragePermissionRule =
            GrantPermissionRule.grant(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
    @Rule
    public GrantPermissionRule mAudioPermissionRule =
            GrantPermissionRule.grant(android.Manifest.permission.RECORD_AUDIO);

    @Before
    public void setup() {
        assumeTrue(CameraUtil.deviceHasCamera());

        assertThat(mLauncherPackageName, notNullValue());
        returnHomeScreen();
    }

    @After
    public void tearDown() {
        returnHomeScreen();
    }

    // Starts the activity, returns to the home screen to pause the activity, starts the activity
    // again. This simulates lifecycle changes caused by a user pressing the HOME button.
    @Test
    public void startCoreTestTwiceToSimulatePauseResume() {
        mContext.startActivity(mIntent);
        waitUntilTextureViewIsReady();

        returnHomeScreen();

        mContext.startActivity(mIntent);
        waitUntilTextureViewIsReady();

        waitForIdlingRegistryAndPressBackButton();
    }

    // Starts the activity, returns to the home screen to pause the activity, starts the activity
    // with a flag which clears any previous instance of the activity. This simulates lifecycle
    // changes caused by a user pressing the BACK button.
    @Test
    public void startCoreTestTwiceClearingPreviousInstance() {
        mContext.startActivity(mIntent);
        waitUntilTextureViewIsReady();

        returnHomeScreen();

        // Clears out any previous instances.
        mIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        mContext.startActivity(mIntent);
        waitUntilTextureViewIsReady();

        waitForIdlingRegistryAndPressBackButton();
    }

    private void waitUntilTextureViewIsReady() {
        mDevice.wait(Until.hasObject(By.pkg(BASIC_SAMPLE_PACKAGE).depth(0)), LAUNCH_TIMEOUT_MS);
        onView(withId(R.id.textureView)).check(matches(isDisplayed()));
    }

    private void returnHomeScreen() {
        mDevice.pressHome();
        mDevice.wait(Until.hasObject(By.pkg(mLauncherPackageName).depth(0)), LAUNCH_TIMEOUT_MS);
    }

    private void waitForIdlingRegistryAndPressBackButton() {
        IdlingResource idlingResource = new ElapsedTimeIdlingResource(IDLE_TIMEOUT_MS);
        IdlingRegistry.getInstance().register(idlingResource);
        Espresso.onIdle();
        IdlingRegistry.getInstance().unregister(idlingResource);

        // Finishs the activity finally.
        mDevice.pressBack();
    }

}
