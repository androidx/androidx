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
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;
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
@RunWith(AndroidJUnit4.class)
public final class Camera2InitializerAndroidTest {
  private static final String TEST_AUTHORITY = "androidx.camera.core";

  private Context appContext;
  private Context testContext;
  private ProviderInfo providerInfo;
  private Camera2Initializer provider;

  @Rule
  public ActivityTestRule<FakeActivity> activityRule =
      new ActivityTestRule<>(
          FakeActivity.class, /*initialTouchMode=*/ false, /*launchActivity=*/ false);

  @Before
  public void setUp() {
    appContext = ApplicationProvider.getApplicationContext();
    Context targetContextWrapper =
        new RenamingDelegatingContext(new MockContext(), appContext, "test.");
    MockContentResolver resolver = new MockContentResolver();
    testContext =
        new IsolatedContext(resolver, targetContextWrapper) {
          @Override
          public Object getSystemService(String name) {
            if (Context.CAMERA_SERVICE.equals(name) || Context.WINDOW_SERVICE.equals(name)) {
              return appContext.getSystemService(name);
            }
            return super.getSystemService(name);
          }
        };

    providerInfo = new ProviderInfo();
    providerInfo.authority = TEST_AUTHORITY;
    provider = new Camera2Initializer();
    provider.attachInfo(testContext, providerInfo);

    resolver.addProvider(TEST_AUTHORITY, provider);
  }

  @Test
  public void initializerIsConnectedToContext() {
    assertThat(provider.getContext()).isSameAs(testContext);
  }

  @Test
  public void cameraXIsInitialized_beforeActivityIsCreated() {
    activityRule.launchActivity(new Intent(appContext, FakeActivity.class));
    FakeActivity activity = activityRule.getActivity();

    assertThat(activity.isCameraXInitializedAtOnCreate()).isTrue();
  }
}
