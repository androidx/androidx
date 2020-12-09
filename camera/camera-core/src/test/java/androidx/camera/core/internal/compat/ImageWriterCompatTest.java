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

package androidx.camera.core.internal.compat;

import static com.google.common.truth.Truth.assertThat;

import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.media.ImageWriter;
import android.os.Build;
import android.view.Surface;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.O)
public final class ImageWriterCompatTest {

    private static final int TEST_MAX_IMAGES = 4;
    private static final int TEST_IMAGE_FORMAT = ImageFormat.YUV_420_888;
    private Surface mTestSurface;
    private SurfaceTexture mTestSurfaceTexture;

    @Before
    public void setUp() {
        mTestSurfaceTexture = new SurfaceTexture(/* singleBufferMode= */ false);
        mTestSurface = new Surface(mTestSurfaceTexture);
    }

    @After
    public void tearDown() {
        mTestSurface.release();
        mTestSurfaceTexture.release();
    }

    @Test
    public void canCreateNewInstance() {
        ImageWriter imageWriter = ImageWriterCompat.newInstance(mTestSurface,
                TEST_MAX_IMAGES, TEST_IMAGE_FORMAT);

        assertThat(imageWriter).isNotNull();
    }
}
