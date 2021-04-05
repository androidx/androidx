/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.view;

import static com.google.common.truth.Truth.assertThat;

import android.graphics.Rect;
import android.os.Build;
import android.util.Size;

import androidx.camera.core.SurfaceRequest;
import androidx.camera.view.internal.compat.quirk.PreviewOneThirdWiderQuirk;
import androidx.camera.view.internal.compat.quirk.QuirkInjector;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

/**
 * Unit tests for {@link PreviewTransformation}.
 */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class PreviewTransformationTest {

    private static final Rect CROP_RECT = new Rect(0, 0, 600, 400);

    private final PreviewTransformation mPreviewTransformation = new PreviewTransformation();

    @Test
    public void withPreviewStretchedQuirk_cropRectIsAdjusted() {
        // Arrange.
        QuirkInjector.inject(new PreviewOneThirdWiderQuirk());

        // Act.
        mPreviewTransformation.setTransformationInfo(
                SurfaceRequest.TransformationInfo.of(CROP_RECT, 0, 0),
                new Size(CROP_RECT.width(), CROP_RECT.height()),
                /*isFrontCamera*/ false);

        // Assert: the crop rect is corrected.
        assertThat(mPreviewTransformation.getSurfaceCropRect()).isEqualTo(new Rect(75, 0, 525,
                400));
    }
}
