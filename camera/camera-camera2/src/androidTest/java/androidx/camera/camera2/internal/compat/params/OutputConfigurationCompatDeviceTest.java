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

import android.graphics.SurfaceTexture;
import android.view.Surface;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests some of the methods of OutputConfigurationCompat on device.
 *
 * <p>These need to run on device since they rely on native implementation details of the
 * {@link Surface} class.
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public final class OutputConfigurationCompatDeviceTest {

    private static final int DEFAULT_WIDTH = 1024;
    private static final int DEFAULT_HEIGHT = 768;

    private static final int SECONDARY_WIDTH = 640;
    private static final int SECONDARY_HEIGHT = 480;

    // Same surface
    private OutputConfigurationCompat mOutputConfigCompat0;
    private OutputConfigurationCompat mOutputConfigCompat1;

    // Different surface, same SurfaceTexture
    private OutputConfigurationCompat mOutputConfigCompat2;

    // Different Surface and SurfaceTexture
    private OutputConfigurationCompat mOutputConfigCompat3;

    @Before
    public void setUp() {
        SurfaceTexture surfaceTexture0 = new SurfaceTexture(0);
        surfaceTexture0.setDefaultBufferSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        Surface surface0 = new Surface(surfaceTexture0);
        Surface surface1 = new Surface(surfaceTexture0);

        SurfaceTexture surfaceTexture1 = new SurfaceTexture(0);
        surfaceTexture0.setDefaultBufferSize(SECONDARY_WIDTH, SECONDARY_HEIGHT);
        Surface surface2 = new Surface(surfaceTexture1);

        mOutputConfigCompat0 = new OutputConfigurationCompat(surface0);
        mOutputConfigCompat1 = new OutputConfigurationCompat(surface0);
        mOutputConfigCompat2 = new OutputConfigurationCompat(surface1);
        mOutputConfigCompat3 = new OutputConfigurationCompat(surface2);
    }

    @Test
    public void hashCode_producesExpectedResults() {
        assertThat(mOutputConfigCompat0.hashCode()).isEqualTo(mOutputConfigCompat1.hashCode());
        assertThat(mOutputConfigCompat0.hashCode()).isNotEqualTo(mOutputConfigCompat2.hashCode());
        assertThat(mOutputConfigCompat0.hashCode()).isNotEqualTo(mOutputConfigCompat3.hashCode());
        assertThat(mOutputConfigCompat2.hashCode()).isNotEqualTo(mOutputConfigCompat3.hashCode());
    }

    @Test
    public void equals_producesExpectedResults() {
        assertThat(mOutputConfigCompat0).isEqualTo(mOutputConfigCompat1);
        assertThat(mOutputConfigCompat0).isNotEqualTo(mOutputConfigCompat2);
        assertThat(mOutputConfigCompat0).isNotEqualTo(mOutputConfigCompat3);
        assertThat(mOutputConfigCompat2).isNotEqualTo(mOutputConfigCompat3);
    }
}
