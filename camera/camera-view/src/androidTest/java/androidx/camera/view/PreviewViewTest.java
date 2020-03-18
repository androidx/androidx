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

import static org.mockito.Mockito.when;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.view.View;

import androidx.camera.core.CameraInfo;
import androidx.camera.testing.fakes.FakeActivity;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

/**
 * Instrumented tests for {@link PreviewView}.
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public class PreviewViewTest {

    @Rule
    public final GrantPermissionRule mRuntimePermissionRule = GrantPermissionRule.grant(
            Manifest.permission.CAMERA);
    @Rule
    public final ActivityTestRule<FakeActivity> mActivityRule = new ActivityTestRule<>(
            FakeActivity.class);
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private final CameraInfo mCameraInfo = Mockito.mock(CameraInfo.class);

    @Test
    @UiThreadTest
    public void implementationIsTextureView_whenImplModeSetToSurfaceView_andDeviceLegacy()
            throws Throwable {
        when(mCameraInfo.getImplementationType()).thenReturn(
                CameraInfo.IMPLEMENTATION_TYPE_CAMERA2_LEGACY);

        final PreviewView previewView = new PreviewView(mContext);
        previewView.setPreferredImplementationMode(SURFACE_VIEW);
        previewView.createSurfaceProvider(mCameraInfo);
        setContentView(previewView);

        assertThat(previewView.getImplementation()).isInstanceOf(TextureViewImplementation.class);
    }

    @Test
    @UiThreadTest
    public void implementationIsSurfaceView_whenImplModeSetToSurfaceView_andDeviceNotLegacy()
            throws Throwable {
        when(mCameraInfo.getImplementationType()).thenReturn(
                CameraInfo.IMPLEMENTATION_TYPE_CAMERA2);

        final PreviewView previewView = new PreviewView(mContext);
        previewView.setPreferredImplementationMode(SURFACE_VIEW);
        previewView.createSurfaceProvider(mCameraInfo);
        setContentView(previewView);

        assertThat(previewView.getImplementation()).isInstanceOf(SurfaceViewImplementation.class);
    }

    @Test
    @UiThreadTest
    public void implementationIsTextureView_whenImplModeSetToTextureView()
            throws Throwable {
        when(mCameraInfo.getImplementationType()).thenReturn(
                CameraInfo.IMPLEMENTATION_TYPE_CAMERA2_LEGACY);

        final PreviewView previewView = new PreviewView(mContext);
        previewView.setPreferredImplementationMode(TEXTURE_VIEW);
        previewView.createSurfaceProvider(mCameraInfo);
        setContentView(previewView);

        assertThat(previewView.getImplementation()).isInstanceOf(TextureViewImplementation.class);
    }

    private void setContentView(View view) throws Throwable {
        final Activity activity = mActivityRule.getActivity();
        mActivityRule.runOnUiThread(() -> activity.setContentView(view));
    }
}
