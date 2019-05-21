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

package androidx.camera.camera2.impl;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;

import androidx.camera.core.CameraX;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowCameraCharacteristics;
import org.robolectric.shadows.ShadowCameraManager;

import java.util.HashSet;
import java.util.Set;

@SmallTest
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class Camera2LensFacingCameraIdFilterTest {
    private static final String CAMERA0_ID = "0";
    private static final int CAMERA0_LENS_FACING_INT = CameraCharacteristics.LENS_FACING_BACK;

    private static final String CAMERA1_ID = "1";
    private static final int CAMERA1_LENS_FACING_INT = CameraCharacteristics.LENS_FACING_FRONT;

    private CameraManager mCameraManager;
    private Set<String> mCameraIds = new HashSet<>();

    @Before
    public void setUp() {
        CameraCharacteristics characteristics0 =
                ShadowCameraCharacteristics.newCameraCharacteristics();
        ShadowCameraCharacteristics shadowCharacteristics0 = Shadow.extract(characteristics0);
        shadowCharacteristics0.set(CameraCharacteristics.LENS_FACING, CAMERA0_LENS_FACING_INT);
        ((ShadowCameraManager)
                Shadow.extract(
                        ApplicationProvider.getApplicationContext()
                                .getSystemService(Context.CAMERA_SERVICE)))
                .addCamera(CAMERA0_ID, characteristics0);

        CameraCharacteristics characteristics1 =
                ShadowCameraCharacteristics.newCameraCharacteristics();
        ShadowCameraCharacteristics shadowCharacteristics1 = Shadow.extract(characteristics1);
        shadowCharacteristics1.set(CameraCharacteristics.LENS_FACING, CAMERA1_LENS_FACING_INT);
        ((ShadowCameraManager)
                Shadow.extract(
                        ApplicationProvider.getApplicationContext()
                                .getSystemService(Context.CAMERA_SERVICE)))
                .addCamera(CAMERA1_ID, characteristics1);

        mCameraManager =
                (CameraManager) ApplicationProvider.getApplicationContext().getSystemService(
                        Context.CAMERA_SERVICE);
        mCameraIds.add(CAMERA0_ID);
        mCameraIds.add(CAMERA1_ID);
    }

    @Test
    public void canFilterBackCamera() {
        Camera2LensFacingCameraIdFilter lensFacingCameraIdFilter =
                new Camera2LensFacingCameraIdFilter(mCameraManager, CameraX.LensFacing.BACK);
        mCameraIds = lensFacingCameraIdFilter.filter(mCameraIds);
        assertThat(mCameraIds).contains(CAMERA0_ID);
        assertThat(mCameraIds).doesNotContain(CAMERA1_ID);
    }

    @Test
    public void canFilterFrontCamera() {
        Camera2LensFacingCameraIdFilter lensFacingCameraIdFilter =
                new Camera2LensFacingCameraIdFilter(mCameraManager, CameraX.LensFacing.FRONT);
        mCameraIds = lensFacingCameraIdFilter.filter(mCameraIds);
        assertThat(mCameraIds).contains(CAMERA1_ID);
        assertThat(mCameraIds).doesNotContain(CAMERA0_ID);
    }
}
