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

package androidx.camera.view;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assume.assumeTrue;

import android.Manifest;
import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.camera.camera2.Camera2AppConfig;
import androidx.camera.core.CameraX;
import androidx.camera.testing.CameraUtil;
import androidx.camera.testing.CoreAppTestUtil;
import androidx.camera.testing.fakes.FakeActivity;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.ExecutionException;

/**
 * Instrumented tests for {@link PreviewView}.
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public class PreviewViewTest {

    @Rule
    public GrantPermissionRule mRuntimePermissionRule = GrantPermissionRule.grant(
            Manifest.permission.CAMERA);
    @Rule
    public ActivityTestRule<FakeActivity> mActivityRule =
            new ActivityTestRule<>(FakeActivity.class);

    private final Instrumentation mInstrumentation = InstrumentationRegistry.getInstrumentation();
    private Context mContext;

    @Before
    public void setUp() {
        assumeTrue(CameraUtil.deviceHasCamera());
        CoreAppTestUtil.assumeCompatibleDevice();

        mContext = ApplicationProvider.getApplicationContext();
        CameraX.init(mContext, Camera2AppConfig.create(mContext));
    }

    @After
    public void tearDown() throws ExecutionException, InterruptedException {
        mInstrumentation.runOnMainSync(CameraX::unbindAll);
        CameraX.deinit().get();
    }

    @Test
    public void defaultImplementation_isSurfaceView() throws Throwable {
        PreviewView previewView = new PreviewView(mContext);
        setContentView(previewView);
        assertThat(getImplementationView()).isInstanceOf(SurfaceView.class);
    }

    private void setContentView(View view) throws Throwable {
        final Activity activity = mActivityRule.getActivity();
        mActivityRule.runOnUiThread(() -> activity.setContentView(view));
    }

    /**
     * Gets sub View inside of the PreviewView.
     *
     * @return the first grandchild of the root.
     */
    private View getImplementationView() {
        return ((FrameLayout) ((ViewGroup) mActivityRule.getActivity().findViewById(
                android.R.id.content)).getChildAt(0)).getChildAt(0);
    }
}
