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
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.Nullable;
import android.util.Size;
import androidx.camera.core.CheckedSurfaceTexture.OnTextureChangedListener;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class CheckedSurfaceTextureRobolectricTest {

  private Size defaultResolution;
  private CheckedSurfaceTexture checkedSurfaceTexture;
  private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());
  private SurfaceTexture latestSurfaceTexture;
  private final CheckedSurfaceTexture.OnTextureChangedListener textureChangedListener =
      new OnTextureChangedListener() {
        @Override
        public void onTextureChanged(
            @Nullable SurfaceTexture newOutput, @Nullable Size newResolution) {
          latestSurfaceTexture = newOutput;
        }
      };

  @Before
  public void setup() {
    defaultResolution = new Size(640, 480);
    checkedSurfaceTexture = new CheckedSurfaceTexture(textureChangedListener, mainThreadHandler);
    checkedSurfaceTexture.setResolution(defaultResolution);
  }

  @Test
  public void viewFinderOutputUpdatesWhenReset() {
    // Create the initial surface texture
    checkedSurfaceTexture.resetSurfaceTexture();

    // Surface texture should have been set
    SurfaceTexture initialOutput = latestSurfaceTexture;

    // Create a new surface texture
    checkedSurfaceTexture.resetSurfaceTexture();

    assertThat(initialOutput).isNotNull();
    assertThat(latestSurfaceTexture).isNotNull();
    assertThat(latestSurfaceTexture).isNotEqualTo(initialOutput);
  }
}
