/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.pdf.util;

import static com.google.common.truth.Truth.assertThat;

import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link ZoomUtils}. */
@SmallTest
@RunWith(RobolectricTestRunner.class)
public class ZoomUtilsTest {

    @Test
    public void testCalculateZoomToFit() {
        assertThat(ZoomUtils.calculateZoomToFit(2, 20, 1, 2)).isEqualTo(2f);
        assertThat(ZoomUtils.calculateZoomToFit(2, 20, 1, 9)).isEqualTo(2f);
        assertThat(ZoomUtils.calculateZoomToFit(2, 20, 1, 10)).isEqualTo(2f);
        assertThat(ZoomUtils.calculateZoomToFit(2, 20, 1, 11)).isEqualTo((20 / 11f));

        // Inner bigger than outer
        assertThat(ZoomUtils.calculateZoomToFit(1, 2, 2, 20)).isEqualTo(0.1f);
        assertThat(ZoomUtils.calculateZoomToFit(1, 9, 2, 20)).isEqualTo(0.45f);
        assertThat(ZoomUtils.calculateZoomToFit(1, 10, 2, 20)).isEqualTo(0.5f);
        assertThat(ZoomUtils.calculateZoomToFit(1, 11, 2, 20)).isEqualTo(0.5f);

        // Reverse
        assertThat(ZoomUtils.calculateZoomToFit(20, 2, 1, 2)).isEqualTo(1f);
        assertThat(ZoomUtils.calculateZoomToFit(20, 2, 1, 9)).isEqualTo((2f / 9f));
        assertThat(ZoomUtils.calculateZoomToFit(20, 2, 1, 10)).isEqualTo(0.2f);
        assertThat(ZoomUtils.calculateZoomToFit(20, 2, 1, 11)).isEqualTo((2f / 11f));

        assertThat(ZoomUtils.calculateZoomToFit(20, 2, 0, 2)).isEqualTo(1f);
        assertThat(ZoomUtils.calculateZoomToFit(20, 2, 1, 0)).isEqualTo(1f);
        assertThat(ZoomUtils.calculateZoomToFit(0, 0, 1, 2)).isEqualTo(0f);
    }
}
