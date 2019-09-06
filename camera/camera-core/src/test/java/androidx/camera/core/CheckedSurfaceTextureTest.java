/*
 * Copyright (C) 2019 The Android Open Source Project
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

package androidx.camera.core;

import static com.google.common.truth.Truth.assertThat;

import android.graphics.SurfaceTexture;
import android.os.Build;
import android.util.Size;
import android.view.Surface;

import androidx.test.filters.SmallTest;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.concurrent.ExecutionException;

@SmallTest
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class CheckedSurfaceTextureTest {

    private Size mDefaultResolution;
    private CheckedSurfaceTexture mCheckedSurfaceTexture;
    private SurfaceTexture mLatestSurfaceTexture;

    @Before
    public void setup() {
        mDefaultResolution = new Size(640, 480);
        mCheckedSurfaceTexture = new CheckedSurfaceTexture(mDefaultResolution);
        mLatestSurfaceTexture = mCheckedSurfaceTexture.getSurfaceTexture();
    }


    @Test
    public void surfaceTextureIsReleasedByOwnerLaterWhenDetached() throws InterruptedException,
            ExecutionException {
        mCheckedSurfaceTexture.notifySurfaceAttached();
        ListenableFuture<Surface> surfaceFuture = mCheckedSurfaceTexture.getSurface();
        surfaceFuture.get();
        FixedSizeSurfaceTexture surfaceTexture = (FixedSizeSurfaceTexture) mLatestSurfaceTexture;

        surfaceTexture.release();

        assertThat(surfaceTexture.mIsSuperReleased).isFalse();

        mCheckedSurfaceTexture.notifySurfaceDetached();
        Thread.sleep(100);

        assertThat(surfaceTexture.mIsSuperReleased).isTrue();
    }

    @Test
    public void releaseCheckedSurfaceTexture() throws InterruptedException, ExecutionException {
        mCheckedSurfaceTexture.notifySurfaceAttached();
        ListenableFuture<Surface> surfaceFuture = mCheckedSurfaceTexture.getSurface();
        surfaceFuture.get();
        FixedSizeSurfaceTexture surfaceTexture = (FixedSizeSurfaceTexture) mLatestSurfaceTexture;

        mCheckedSurfaceTexture.release();
        assertThat(surfaceTexture.mIsSuperReleased).isFalse();

        mCheckedSurfaceTexture.notifySurfaceDetached();
        Thread.sleep(100);

        assertThat(surfaceTexture.mIsSuperReleased).isTrue();

    }
}
