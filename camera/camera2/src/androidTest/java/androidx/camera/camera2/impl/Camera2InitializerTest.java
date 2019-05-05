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

package androidx.camera.camera2.impl;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.content.Intent;

import androidx.camera.testing.fakes.FakeActivity;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Unit tests for {@link Camera2Initializer}.
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public final class Camera2InitializerTest {

    @Rule
    public ActivityTestRule<FakeActivity> activityRule =
            new ActivityTestRule<>(
                    FakeActivity.class, /*initialTouchMode=*/ false, /*launchActivity=*/ false);
    private Context mAppContext;

    @Before
    public void setUp() {
        mAppContext = ApplicationProvider.getApplicationContext();
    }

    @Test
    public void cameraXIsInitialized_beforeActivityIsCreated() {
        activityRule.launchActivity(new Intent(mAppContext, FakeActivity.class));
        FakeActivity activity = activityRule.getActivity();

        assertThat(activity.isCameraXInitializedAtOnCreate()).isTrue();
    }
}
