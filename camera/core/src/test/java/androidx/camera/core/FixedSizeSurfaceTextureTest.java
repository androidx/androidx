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

package androidx.camera.core;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.os.Build;
import android.util.Size;

import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

@SmallTest
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class FixedSizeSurfaceTextureTest {
    private FixedSizeSurfaceTexture.Owner mOwner;

    @Before
    public void setup() {
        mOwner = Mockito.mock(FixedSizeSurfaceTexture.Owner.class);
    }

    @Test
    public void release_selfOwner() {
        FixedSizeSurfaceTexture surfaceTextureSelfOwner =
                new FixedSizeSurfaceTexture(0, new Size(1920, 1080));

        surfaceTextureSelfOwner.release();

        assertThat(surfaceTextureSelfOwner.mIsSuperReleased).isTrue();
    }

    @Test
    public void release_withOwner() {
        FixedSizeSurfaceTexture surfaceTexture = new FixedSizeSurfaceTexture(0,
                new Size(1920, 1080), mOwner);

        when(mOwner.requestRelease()).thenReturn(false);

        surfaceTexture.release();
        assertThat(surfaceTexture.mIsSuperReleased).isFalse();

        when(mOwner.requestRelease()).thenReturn(true);
        surfaceTexture.release();
        assertThat(surfaceTexture.mIsSuperReleased).isTrue();
    }


}
