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

import static androidx.camera.view.PreviewView.ImplementationMode.SURFACE_VIEW;
import static androidx.camera.view.PreviewView.ImplementationMode.TEXTURE_VIEW;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assume.assumeTrue;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import androidx.camera.testing.CameraUtil;
import androidx.camera.testing.CoreAppTestUtil;
import androidx.camera.testing.fakes.FakeActivity;
import androidx.camera.view.test.R;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

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

    private Context mContext;

    @Before
    public void setUp() {
        assumeTrue(CameraUtil.deviceHasCamera());
        CoreAppTestUtil.assumeCompatibleDevice();

        mContext = ApplicationProvider.getApplicationContext();
    }

    @Test
    @UiThreadTest
    public void implementationIsSurfaceView_whenAttributeSetImplementationModeIsSurfaceView()
            throws Throwable {
        final PreviewView previewView = (PreviewView) LayoutInflater.from(mContext).inflate(
                R.layout.preview_view_surface_view_mode, null, false);
        setContentView(previewView);

        assertThat(previewView.getImplementationMode()).isEqualTo(SURFACE_VIEW);
    }

    @Test
    @UiThreadTest
    public void implementationIsSurfaceView_whenImplementationModeIsSetToSurfaceView()
            throws Throwable {
        final PreviewView previewView = new PreviewView(mContext);
        previewView.setImplementationMode(SURFACE_VIEW);
        setContentView(previewView);

        assertThat(previewView.getImplementationMode()).isEqualTo(SURFACE_VIEW);
    }

    @Test
    @UiThreadTest
    public void implementationIsTextureView_whenAttributeSetImplementationModeIsTextureView()
            throws Throwable {

        final PreviewView previewView = (PreviewView) LayoutInflater.from(mContext).inflate(
                R.layout.preview_view_texture_view_mode, null, false);
        setContentView(previewView);

        assertThat(previewView.getImplementationMode()).isEqualTo(TEXTURE_VIEW);
    }

    @Test
    @UiThreadTest
    public void implementationIsTextureView_whenImplementationModeIsSetToTextureView()
            throws Throwable {
        final PreviewView previewView = new PreviewView(mContext);
        previewView.setImplementationMode(TEXTURE_VIEW);
        setContentView(previewView);

        assertThat(previewView.getImplementationMode()).isEqualTo(TEXTURE_VIEW);
    }

    @Test
    @UiThreadTest
    public void implementationIsTextureView_whenLastImplementationModeIsSetToTextureView()
            throws Throwable {
        final PreviewView previewView = new PreviewView(mContext);
        setContentView(previewView);

        previewView.setImplementationMode(SURFACE_VIEW);
        previewView.setImplementationMode(TEXTURE_VIEW);

        assertThat(previewView.getImplementationMode()).isEqualTo(TEXTURE_VIEW);
    }

    private void setContentView(View view) throws Throwable {
        final Activity activity = mActivityRule.getActivity();
        mActivityRule.runOnUiThread(() -> activity.setContentView(view));
    }
}
