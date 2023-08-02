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

package androidx.camera.camera2.internal.compat.params;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.graphics.SurfaceTexture;
import android.hardware.camera2.params.DynamicRangeProfiles;
import android.hardware.camera2.params.OutputConfiguration;
import android.os.Build;
import android.util.Size;
import android.view.Surface;

import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public final class OutputConfigurationCompatTest {

    private static final int TEST_GROUP_ID = 100;
    private static final String PHYSICAL_CAMERA_ID = "1";

    private static final long DYNAMIC_RANGE_PROFILE = DynamicRangeProfiles.HLG10;

    private static void assumeSurfaceSharingAvailable(
            OutputConfigurationCompat outputConfigCompat) {
        Assume.assumeTrue("API level does not support surface sharing.",
                outputConfigCompat.getMaxSharedSurfaceCount() > 1);
    }

    @Test
    public void canRetrieveSurface() {
        Surface surface = mock(Surface.class);
        OutputConfigurationCompat outputConfigCompat = new OutputConfigurationCompat(surface);

        assertThat(outputConfigCompat.getSurface()).isSameInstanceAs(surface);
    }

    @Test
    public void defaultSurfaceGroupIdIsSet() {
        Surface surface = mock(Surface.class);
        OutputConfigurationCompat outputConfigCompat = new OutputConfigurationCompat(surface);

        assertThat(outputConfigCompat.getSurfaceGroupId()).isEqualTo(
                OutputConfigurationCompat.SURFACE_GROUP_ID_NONE);
    }

    @Config(minSdk = 24)  // surfaceGroupId supported since api level 24
    @Test
    public void canSetSurfaceGroupId() {
        Surface surface = mock(Surface.class);
        int surfaceGroupId = 1;
        OutputConfigurationCompat outputConfigCompat =
                new OutputConfigurationCompat(surfaceGroupId, surface);

        assertThat(outputConfigCompat.getSurfaceGroupId()).isEqualTo(surfaceGroupId);
        assertThat(outputConfigCompat.getSurface()).isSameInstanceAs(surface);
    }

    @Test(expected = IllegalArgumentException.class)
    public void addSurfaceThrows_whenAddingMoreThanMax() {
        Surface surface = mock(Surface.class);
        OutputConfigurationCompat outputConfigCompat = new OutputConfigurationCompat(surface);

        outputConfigCompat.enableSurfaceSharing();

        // Since we already have 1 surface added, if we try to add the max we will be adding a
        // total of max surfaces + 1
        for (int i = 0; i < outputConfigCompat.getMaxSharedSurfaceCount(); ++i) {
            outputConfigCompat.addSurface(mock(Surface.class));
        }
    }

    @Test(expected = IllegalStateException.class)
    public void addSurfaceThrows_whenSurfaceSharingNotEnabled() {
        Surface surface = mock(Surface.class);
        OutputConfigurationCompat outputConfigCompat = new OutputConfigurationCompat(surface);

        // Adding a second surface should fail if OutputConfigurationCompat#enableSurfaceSharing()
        // has not been called.
        outputConfigCompat.addSurface(mock(Surface.class));
    }

    @Test
    public void maxSurfaces_canBeAdded_andRetrieved() {
        Surface surface = mock(Surface.class);
        OutputConfigurationCompat outputConfigCompat = new OutputConfigurationCompat(surface);

        outputConfigCompat.enableSurfaceSharing();
        List<Surface> allSurfaces = new ArrayList<Surface>();
        allSurfaces.add(surface);

        for (int i = 0; i < outputConfigCompat.getMaxSharedSurfaceCount() - 1; ++i) {
            Surface newSurface = mock(Surface.class);
            allSurfaces.add(newSurface);
            outputConfigCompat.addSurface(newSurface);
        }

        assertThat(outputConfigCompat.getSurfaces()).containsExactlyElementsIn(allSurfaces);
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannotRemoveMainSurface() {
        Surface surface = mock(Surface.class);
        OutputConfigurationCompat outputConfigCompat = new OutputConfigurationCompat(surface);
        outputConfigCompat.removeSurface(surface);
    }

    @Test(expected = IllegalArgumentException.class)
    public void removeNonAddedSurfaceThrows() {
        Surface surface = mock(Surface.class);
        OutputConfigurationCompat outputConfigCompat = new OutputConfigurationCompat(surface);
        outputConfigCompat.removeSurface(mock(Surface.class));
    }

    @Test
    public void canRemoveSharedSurface() {
        Surface surface = mock(Surface.class);
        OutputConfigurationCompat outputConfigCompat = new OutputConfigurationCompat(surface);

        assumeSurfaceSharingAvailable(outputConfigCompat);
        outputConfigCompat.enableSurfaceSharing();
        List<Surface> allSurfaces = new ArrayList<Surface>();
        allSurfaces.add(surface);

        for (int i = 0; i < outputConfigCompat.getMaxSharedSurfaceCount() - 1; ++i) {
            Surface newSurface = mock(Surface.class);
            allSurfaces.add(newSurface);
            outputConfigCompat.addSurface(newSurface);
        }

        Surface lastSurface = allSurfaces.remove(allSurfaces.size() - 1);
        boolean containedBeforeRemoval = outputConfigCompat.getSurfaces().contains(lastSurface);

        outputConfigCompat.removeSurface(lastSurface);

        assertThat(containedBeforeRemoval).isTrue();
        assertThat(outputConfigCompat.getSurfaces()).doesNotContain(lastSurface);
        assertThat(outputConfigCompat.getSurfaces()).containsExactlyElementsIn(allSurfaces);
    }

    @Test
    @Config(maxSdk = 23)
    public void cannotRetrieveOutputConfiguration_onApi23AndBelow() {
        Surface surface = mock(Surface.class);
        OutputConfigurationCompat outputConfigCompat = new OutputConfigurationCompat(surface);

        Object outputConfig = outputConfigCompat.unwrap();
        assertThat(outputConfig).isNull();
    }

    @Test
    @Config(minSdk = 24)
    public void canRetrieveOutputConfiguration_onApi24AndUp() {
        Surface surface = mock(Surface.class);
        OutputConfigurationCompat outputConfigCompat = new OutputConfigurationCompat(surface);

        Object outputConfig = outputConfigCompat.unwrap();
        assertThat(outputConfig).isNotNull();
        assertThat(outputConfig).isInstanceOf(OutputConfiguration.class);
    }

    @Test
    @Config(minSdk = 24)
    public void canWrapOutputConfiguration() {
        Surface surface = mock(Surface.class);
        OutputConfiguration outputConfig = new OutputConfiguration(surface);
        OutputConfigurationCompat outputConfigCompat = OutputConfigurationCompat.wrap(outputConfig);

        assertThat(outputConfigCompat.unwrap()).isSameInstanceAs(outputConfig);
    }

    @Test
    @Config(minSdk = 24)
    public void canSetGroupId_andRetrieveThroughCompatObject() {
        Surface surface = mock(Surface.class);
        OutputConfiguration outputConfig = new OutputConfiguration(TEST_GROUP_ID, surface);
        OutputConfigurationCompat outputConfigCompat = OutputConfigurationCompat.wrap(outputConfig);

        assertThat(outputConfigCompat.getSurfaceGroupId()).isEqualTo(TEST_GROUP_ID);
    }

    @Test
    @Config(minSdk = 26)
    public void canCreateDeferredOutputConfiguration_andRetrieveNullSurface() {
        OutputConfigurationCompat outputConfigCompat = new OutputConfigurationCompat(
                new Size(1024, 768), SurfaceTexture.class);

        assertThat(outputConfigCompat.getSurface()).isNull();
    }

    @Test
    @Config(minSdk = 26, maxSdk = 26)
    public void sharedSurfaceCount_canBeRetrievedOnApi26() {
        Surface surface = mock(Surface.class);
        OutputConfigurationCompat outputConfigCompat = new OutputConfigurationCompat(surface);

        // API 26 hard-codes max shared surface count to 2, but we have to retrieve that via
        // reflection
        assertThat(outputConfigCompat.getMaxSharedSurfaceCount()).isEqualTo(2);
    }

    @Test
    @Config(minSdk = 28)
    public void canSetPhysicalCameraId() {
        OutputConfiguration outputConfig = mock(OutputConfiguration.class);

        OutputConfigurationCompat outputConfigCompat = OutputConfigurationCompat.wrap(outputConfig);

        outputConfigCompat.setPhysicalCameraId(PHYSICAL_CAMERA_ID);

        verify(outputConfig, times(1)).setPhysicalCameraId(PHYSICAL_CAMERA_ID);
    }

    @Test
    public void canSetDynamicRangeProfile() {
        OutputConfigurationCompat outputConfigCompat =
                new OutputConfigurationCompat(mock(Surface.class));

        outputConfigCompat.setDynamicRangeProfile(DYNAMIC_RANGE_PROFILE);

        assertThat(outputConfigCompat.getDynamicRangeProfile()).isEqualTo(DYNAMIC_RANGE_PROFILE);
    }
}
