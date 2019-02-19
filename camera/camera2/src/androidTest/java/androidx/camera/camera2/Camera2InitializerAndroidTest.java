/*
 * Copyright (C) 2019 The Android Open Source Project
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

package androidx.camera.camera2;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ProviderInfo;
import android.test.IsolatedContext;
import android.test.RenamingDelegatingContext;
import android.test.mock.MockContentResolver;
import android.test.mock.MockContext;

import androidx.camera.core.FakeActivity;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.test.rule.ActivityTestRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Unit tests for {@link Camera2Initializer}.
 *
 * <p>The default ProviderTestCase2 cannot be used, because its mock Context returns null when we
 * call Context.getSystemService(Context.CAMERA_SERVICE). We need to be able to get the camera
 * service to test CameraX initialization. Therefore, we copy the test strategy employed in
 * CalendarProvider2Test, where we override a method of the mock Context.
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public final class Camera2InitializerAndroidTest {
    private static final String TEST_AUTHORITY = "androidx.camera.core";
    @Rule
    public ActivityTestRule<FakeActivity> activityRule =
            new ActivityTestRule<>(
                    FakeActivity.class, /*initialTouchMode=*/ false, /*launchActivity=*/ false);
    private Context mAppContext;
    private Context mTestContext;
    private ProviderInfo mProviderInfo;
    private Camera2Initializer mProvider;

    @Before
    public void setUp() {
        mAppContext = ApplicationProvider.getApplicationContext();
        Context targetContextWrapper =
                new RenamingDelegatingContext(new MockContext(), mAppContext, "test.");
        MockContentResolver resolver = new MockContentResolver();
        mTestContext =
                new IsolatedContext(resolver, targetContextWrapper) {
                    @Override
                    public Object getSystemService(String name) {
                        if (Context.CAMERA_SERVICE.equals(name)
                                || Context.WINDOW_SERVICE.equals(name)) {
                            return mAppContext.getSystemService(name);
                        }
                        return super.getSystemService(name);
                    }
                };

        mProviderInfo = new ProviderInfo();
        mProviderInfo.authority = TEST_AUTHORITY;
        mProvider = new Camera2Initializer();
        mProvider.attachInfo(mTestContext, mProviderInfo);

        resolver.addProvider(TEST_AUTHORITY, mProvider);
    }

    @Test
    public void initializerIsConnectedToContext() {
        assertThat(mProvider.getContext()).isSameAs(mTestContext);
    }

    @Test
    public void cameraXIsInitialized_beforeActivityIsCreated() {
        activityRule.launchActivity(new Intent(mAppContext, FakeActivity.class));
        FakeActivity activity = activityRule.getActivity();

        assertThat(activity.isCameraXInitializedAtOnCreate()).isTrue();
    }
}
