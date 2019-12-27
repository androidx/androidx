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

package androidx.camera.extensions;

import static org.junit.Assume.assumeTrue;

import android.Manifest;
import android.content.Context;
import android.hardware.camera2.CameraAccessException;

import androidx.camera.camera2.Camera2Config;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraX;
import androidx.camera.extensions.impl.ImageCaptureExtenderImpl;
import androidx.camera.extensions.util.ExtensionsTestUtil;
import androidx.camera.testing.CameraUtil;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.GrantPermissionRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@RunWith(AndroidJUnit4.class)
public class ImageCaptureExtenderValidationTest {
    @Rule
    public GrantPermissionRule mRuntimePermissionRule = GrantPermissionRule.grant(
            Manifest.permission.CAMERA);

    @Before
    public void setUp() throws InterruptedException, ExecutionException, TimeoutException {
        assumeTrue(CameraUtil.deviceHasCamera());
        Context context = ApplicationProvider.getApplicationContext();
        CameraX.initialize(context, Camera2Config.defaultConfig());

        assumeTrue(ExtensionsTestUtil.initExtensions());
    }

    @After
    public void tearDown() throws ExecutionException, InterruptedException {
        CameraX.shutdown().get();
    }

    @Test
    @LargeTest
    public void getSupportedResolutionsImplementationTest()
            throws CameraInfoUnavailableException, CameraAccessException {
        // getSupportedResolutions supported since version 1.1
        assumeTrue(ExtensionVersion.getRuntimeVersion().compareTo(Version.VERSION_1_1) >= 0);

        // Uses for-loop to check all possible effect/lens facing combinations
        for (Object[] EffectLensFacingPair :
                ExtensionsTestUtil.getAllEffectLensFacingCombinations()) {
            ExtensionsManager.EffectMode effectMode =
                    (ExtensionsManager.EffectMode) EffectLensFacingPair[0];
            @CameraSelector.LensFacing int lensFacing = (int) EffectLensFacingPair[1];

            assumeTrue(CameraUtil.hasCameraWithLensFacing(lensFacing));
            assumeTrue(ExtensionsManager.isExtensionAvailable(effectMode, lensFacing));

            // Retrieves the target format/resolutions pair list from vendor library for the
            // target effect mode.
            ImageCaptureExtenderImpl impl = ExtensionsTestUtil.createImageCaptureExtenderImpl(
                    effectMode, lensFacing);

            // NoSuchMethodError will be thrown if getSupportedResolutions is not
            // implemented in vendor library, and then the test will fail.
            impl.getSupportedResolutions();
        }
    }
}
