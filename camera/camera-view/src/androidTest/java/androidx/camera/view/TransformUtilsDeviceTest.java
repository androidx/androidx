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
import android.graphics.RectF;
import android.util.Size;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrument test for {@link TransformUtils}
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 21)
public class TransformUtilsDeviceTest {

    @Test
    public void viewPortMatchAllowRoundingError() {
        // Arrange: create two 1:1 crop rect. Due to rounding error, one is 11:9 and another is
        // 9:11.
        Rect cropRect1 = new Rect();
        new RectF(0.4999f, 0.5f, 10.5f, 10.4999f).round(cropRect1);
        Rect cropRect2 = new Rect();
        new RectF(0.5f, 0.4999f, 10.4999f, 10.5f).round(cropRect2);

        // Assert: they are within rounding error.
        assertThat(TransformUtils.isAspectRatioMatchingWithRoundingError(
                new Size(cropRect1.width(), cropRect1.height()), false,
                new Size(cropRect2.width(), cropRect2.height()), false)).isTrue();
    }
}
