/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.car.app.activity;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;
import static org.robolectric.Shadows.shadowOf;

import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.shadows.ShadowPackageManager;

/** Tests for {@link LauncherActivity}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class LauncherActivityTest {
    private final ComponentName defaultComponentName = new ComponentName(getApplicationContext(),
            getClass().getName());
    private final ComponentName launcherComponent = new ComponentName(getApplicationContext(),
            "androidx.car.app.activity.LauncherActivity");
    private final ComponentName carAppActivityComponentName = new ComponentName(
            getApplicationContext(), "androidx.car.app.activity.CarAppActivity");

    private ShadowPackageManager mShadowPackageManager;

    @Before
    public void setup() {
        PackageManager packageManager = getApplicationContext().getPackageManager();
        mShadowPackageManager = shadowOf(packageManager);

        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_MAIN);
        intentFilter.addCategory(Intent.CATEGORY_LAUNCHER);

        mShadowPackageManager.addActivityIfNotPresent(launcherComponent);
        mShadowPackageManager.addIntentFilterForActivity(launcherComponent,
                intentFilter);
    }

    @Test
    public void getCarAppActivityIntent_returnsCarAppActivityIntent() {
        assertThat(
                LauncherActivity.getCarAppActivityIntent(
                        getApplicationContext()).getComponent()).isEqualTo(
                carAppActivityComponentName);
    }

    @Test
    public void getDefaultIntent_throwsIllegalStateException() {
        assertThrows(IllegalStateException.class,
                () -> LauncherActivity.getDefaultIntent(
                        getApplicationContext()));
    }

    @Test
    public void getDefaultIntent_returnsDefaultComponentName() {
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_MAIN);
        intentFilter.addCategory(Intent.CATEGORY_DEFAULT);

        mShadowPackageManager.addActivityIfNotPresent(defaultComponentName);
        mShadowPackageManager.addActivityIfNotPresent(carAppActivityComponentName);
        mShadowPackageManager.addIntentFilterForActivity(defaultComponentName,
                intentFilter);
        Intent defaultIntent = LauncherActivity.getDefaultIntent(
                getApplicationContext());

        assertThat(defaultIntent.getComponent()).isEqualTo(defaultComponentName);
    }

    @Test
    public void isDistractionOptimizedActivityRequired_returnsFalse() {
        mShadowPackageManager.setSystemFeature(PackageManager.FEATURE_AUTOMOTIVE, false);

        assertThat(LauncherActivity.isDistractionOptimizedActivityRequired(
                getApplicationContext())).isFalse();
    }

}
