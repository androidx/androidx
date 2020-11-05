/*
 * Copyright 2020 The Android Open Source Project
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

import static android.os.Looper.getMainLooper;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.os.Build;
import android.util.Size;
import android.view.WindowManager;

import androidx.camera.core.CameraInfo;
import androidx.camera.core.Preview;
import androidx.camera.core.SurfaceRequest;
import androidx.camera.core.impl.CameraInfoInternal;
import androidx.camera.testing.fakes.FakeCamera;
import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.shadows.ShadowDisplayManager;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class PreviewViewTest {

    private final Context mContext = ApplicationProvider.getApplicationContext();

    private SurfaceRequest mSurfaceRequest;

    private SurfaceRequest createSurfaceRequest(CameraInfo cameraInfo) {
        FakeCamera fakeCamera = spy(new FakeCamera());
        when(fakeCamera.getCameraInfo()).thenReturn(cameraInfo);
        return new SurfaceRequest(new Size(640, 480), fakeCamera, false);
    }

    @After
    public void tearDown() {
        if (mSurfaceRequest != null) {
            mSurfaceRequest.willNotProvideSurface();
            // Ensure all successful requests have their returned future finish.
            mSurfaceRequest.getDeferrableSurface().close();
            mSurfaceRequest = null;
        }
    }

    private CameraInfoInternal createCameraInfo(String implementationType) {
        final CameraInfoInternal cameraInfo = mock(CameraInfoInternal.class);
        when(cameraInfo.getImplementationType()).thenReturn(implementationType);
        return cameraInfo;
    }

    @Test
    public void forceUseTextureViewMode_whenNonLegacyDevice_andInRemoteDisplayMode() {
        // The remote display simulation can only work from sdk 21 to 25. Limit to 24 because the
        // test may be failed when running presubmit in API level 25.
        assumeTrue(Build.VERSION.SDK_INT <= 24);
        final PreviewView previewView = new PreviewView(mContext);

        // Provides mock CameraInfo to make the device return non-legacy type.
        final CameraInfo cameraInfo = createCameraInfo(CameraInfo.IMPLEMENTATION_TYPE_CAMERA2);

        // Simulates the remote display mode by adding an additional display and returns the
        // second display's id when PreviewView is querying the default display's id.
        int secondDisplayId = addNewDisplay();
        WindowManager windowManager =
                (WindowManager) ApplicationProvider.getApplicationContext().getSystemService(
                        Context.WINDOW_SERVICE);
        shadowOf(windowManager.getDefaultDisplay()).setDisplayId(secondDisplayId);

        previewView.setImplementationMode(PreviewView.ImplementationMode.PERFORMANCE);
        Preview.SurfaceProvider surfaceProvider = previewView.getSurfaceProvider();
        mSurfaceRequest = createSurfaceRequest(cameraInfo);
        surfaceProvider.onSurfaceRequested(mSurfaceRequest);
        shadowOf(getMainLooper()).idle();
        assertThat(previewView.mImplementation).isInstanceOf(TextureViewImplementation.class);
    }

    @Test
    public void forceUseTextureViewMode_whenLegacyDevice_andInRemoteDisplayMode() {
        // The remote display simulation can only work from sdk 21 to 25. Limit to 24 because the
        // test may be failed when running presubmit in API level 25.
        assumeTrue(Build.VERSION.SDK_INT <= 24);
        final PreviewView previewView = new PreviewView(mContext);

        // Provides mock CameraInfo to make the device return legacy type.
        final CameraInfo cameraInfo = createCameraInfo(
                CameraInfo.IMPLEMENTATION_TYPE_CAMERA2_LEGACY);

        // Simulates the remote display mode by adding an additional display and returns the
        // second display's id when PreviewView is querying the default display's id.
        int secondDisplayId = addNewDisplay();
        WindowManager windowManager =
                (WindowManager) ApplicationProvider.getApplicationContext().getSystemService(
                        Context.WINDOW_SERVICE);
        shadowOf(windowManager.getDefaultDisplay()).setDisplayId(secondDisplayId);

        previewView.setImplementationMode(PreviewView.ImplementationMode.PERFORMANCE);
        Preview.SurfaceProvider surfaceProvider = previewView.getSurfaceProvider();
        mSurfaceRequest = createSurfaceRequest(cameraInfo);
        surfaceProvider.onSurfaceRequested(mSurfaceRequest);
        shadowOf(getMainLooper()).idle();
        assertThat(previewView.mImplementation).isInstanceOf(TextureViewImplementation.class);
    }

    private int addNewDisplay() {
        String displayQualifierString = String.format("w%ddp-h%ddp", 480, 640);
        ShadowDisplayManager.addDisplay(displayQualifierString);
        return ShadowDisplayManager.addDisplay(displayQualifierString);
    }
}
