/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.viewfinder;

import static androidx.camera.viewfinder.CameraViewfinder.shouldUseTextureView;
import static androidx.camera.viewfinder.surface.ImplementationMode.EMBEDDED;
import static androidx.camera.viewfinder.surface.ImplementationMode.EXTERNAL;

import static com.google.common.truth.Truth.assertThat;

import android.os.Build;

import androidx.camera.viewfinder.internal.quirk.QuirkInjector;
import androidx.camera.viewfinder.internal.quirk.SurfaceViewNotCroppedByParentQuirk;
import androidx.camera.viewfinder.internal.quirk.SurfaceViewStretchedQuirk;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class CameraViewfinderTest {

    @After
    public void tearDown() {
        QuirkInjector.clear();
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.N_MR1)
    public void surfaceViewNormal_useSurfaceView() {
        // Assert: SurfaceView is used.
        assertThat(shouldUseTextureView(EXTERNAL)).isFalse();
    }

    @Test
    public void surfaceViewStretchedQuirk_useTextureView() {
        // Arrange:
        QuirkInjector.inject(new SurfaceViewStretchedQuirk());

        // Assert: TextureView is used even the SurfaceRequest is compatible with SurfaceView.
        assertThat(shouldUseTextureView(EXTERNAL)).isTrue();
    }

    @Test
    public void surfaceViewNotCroppedQuirk_useTextureView() {
        // Arrange:
        QuirkInjector.inject(new SurfaceViewNotCroppedByParentQuirk());

        // Assert: TextureView is used even the SurfaceRequest is compatible with SurfaceView.
        assertThat(shouldUseTextureView(EXTERNAL)).isTrue();
    }

    @Test
    public void legacyDevice_useTextureView() {
        // Arrange:
        QuirkInjector.inject(new SurfaceViewNotCroppedByParentQuirk());

        // Assert: TextureView is used even the SurfaceRequest is compatible with SurfaceView.
        assertThat(shouldUseTextureView(EMBEDDED)).isTrue();
    }
}
