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

package androidx.biometric;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class PackageUtilsTest {
    @Mock private Context mContext;
    @Mock private PackageManager mPackageManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    @Config(maxSdk = Build.VERSION_CODES.LOLLIPOP_MR1)
    public void testHasSystemFeatureFingerprint_ReturnsFalse_OnApi22AndBelow() {
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mPackageManager.hasSystemFeature(anyString())).thenReturn(true);
        assertThat(PackageUtils.hasSystemFeatureFingerprint(mContext)).isFalse();
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.M)
    public void testHasSystemFeatureFingerprint_ReturnsFalse_WhenContextIsNull() {
        assertThat(PackageUtils.hasSystemFeatureFingerprint(null)).isFalse();
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.M)
    public void testHasSystemFeatureFingerprint_ReturnsFalse_WhenPackageManagerIsNull() {
        when(mContext.getPackageManager()).thenReturn(null);
        assertThat(PackageUtils.hasSystemFeatureFingerprint(mContext)).isFalse();
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.M)
    public void testHasSystemFeatureFingerprint_ReturnsFalse_WhenPackageManagerReturnsFalse() {
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT))
                .thenReturn(false);
        assertThat(PackageUtils.hasSystemFeatureFingerprint(mContext)).isFalse();
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.M)
    public void testHasSystemFeatureFingerprint_ReturnsTrue_WhenPackageManagerReturnsTrue() {
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)).thenReturn(true);
        assertThat(PackageUtils.hasSystemFeatureFingerprint(mContext)).isTrue();
    }

    @Test
    @Config(maxSdk = Build.VERSION_CODES.P)
    public void testHasSystemFeatureFace_ReturnsFalse_OnApi28AndBelow() {
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mPackageManager.hasSystemFeature(anyString())).thenReturn(true);
        assertThat(PackageUtils.hasSystemFeatureFace(mContext)).isFalse();
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.Q)
    public void testHasSystemFeatureFace_ReturnsFalse_WhenContextIsNull() {
        assertThat(PackageUtils.hasSystemFeatureFace(null)).isFalse();
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.Q)
    public void testHasSystemFeatureFace_ReturnsFalse_WhenPackageManagerIsNull() {
        when(mContext.getPackageManager()).thenReturn(null);
        assertThat(PackageUtils.hasSystemFeatureFace(mContext)).isFalse();
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.Q)
    public void testHasSystemFeatureFace_ReturnsFalse_WhenPackageManagerReturnsFalse() {
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_FACE)).thenReturn(false);
        assertThat(PackageUtils.hasSystemFeatureFace(mContext)).isFalse();
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.Q)
    public void testHasSystemFeatureFace_ReturnsTrue_WhenPackageManagerReturnsTrue() {
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_FACE)).thenReturn(true);
        assertThat(PackageUtils.hasSystemFeatureFace(mContext)).isTrue();
    }

    @Test
    @Config(maxSdk = Build.VERSION_CODES.P)
    public void testHasSystemFeatureIris_ReturnsFalse_OnApi28AndBelow() {
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mPackageManager.hasSystemFeature(anyString())).thenReturn(true);
        assertThat(PackageUtils.hasSystemFeatureIris(mContext)).isFalse();
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.Q)
    public void testHasSystemFeatureIris_ReturnsFalse_WhenContextIsNull() {
        assertThat(PackageUtils.hasSystemFeatureIris(null)).isFalse();
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.Q)
    public void testHasSystemFeatureIris_ReturnsFalse_WhenPackageManagerIsNull() {
        when(mContext.getPackageManager()).thenReturn(null);
        assertThat(PackageUtils.hasSystemFeatureIris(mContext)).isFalse();
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.Q)
    public void testHasSystemFeatureIris_ReturnsFalse_WhenPackageManagerReturnsFalse() {
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_IRIS)).thenReturn(false);
        assertThat(PackageUtils.hasSystemFeatureIris(mContext)).isFalse();
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.Q)
    public void testHasSystemFeatureIris_ReturnsTrue_WhenPackageManagerReturnsTrue() {
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_IRIS)).thenReturn(true);
        assertThat(PackageUtils.hasSystemFeatureIris(mContext)).isTrue();
    }
}
