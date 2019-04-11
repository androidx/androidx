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

import androidx.annotation.Nullable;
import androidx.camera.core.CheckedSurfaceTexture.OnTextureChangedListener;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

@SmallTest
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class CheckedSurfaceTextureTest {

    private Size mDefaultResolution;
    private CheckedSurfaceTexture mCheckedSurfaceTexture;
    private SurfaceTexture mLatestSurfaceTexture;
    private final CheckedSurfaceTexture.OnTextureChangedListener mTextureChangedListener =
            new OnTextureChangedListener() {
                @Override
                public void onTextureChanged(
                        @Nullable SurfaceTexture newOutput, @Nullable Size newResolution) {
                    mLatestSurfaceTexture = newOutput;
                }
            };

    @Before
    public void setup() {
        mDefaultResolution = new Size(640, 480);
        mCheckedSurfaceTexture =
                new CheckedSurfaceTexture(mTextureChangedListener);
        mCheckedSurfaceTexture.setResolution(mDefaultResolution);
    }

    @Test
    public void previewOutputUpdatesWhenReset() {
        // Create the initial surface texture
        mCheckedSurfaceTexture.resetSurfaceTexture();

        // Surface texture should have been set
        SurfaceTexture initialOutput = mLatestSurfaceTexture;

        // Create a new surface texture
        mCheckedSurfaceTexture.resetSurfaceTexture();

        assertThat(initialOutput).isNotNull();
        assertThat(mLatestSurfaceTexture).isNotNull();
        assertThat(mLatestSurfaceTexture).isNotEqualTo(initialOutput);
    }
}
