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

package androidx.window.extensions.embedding;

import static androidx.window.extensions.embedding.DividerAttributes.DIVIDER_TYPE_DRAGGABLE;
import static androidx.window.extensions.embedding.DividerAttributes.DIVIDER_TYPE_FIXED;
import static androidx.window.extensions.embedding.DividerAttributes.RATIO_SYSTEM_DEFAULT;
import static androidx.window.extensions.embedding.DividerAttributes.WIDTH_SYSTEM_DEFAULT;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/** Verifies {@link DividerAttributes} behavior. */
@SmallTest
@RunWith(RobolectricTestRunner.class)
public class DividerAttributesTest {

    @Test
    public void testDividerAttributesDefaults() {
        final DividerAttributes defaultAttrs =
                new DividerAttributes.Builder(DIVIDER_TYPE_FIXED).build();
        assertThat(defaultAttrs.getDividerType()).isEqualTo(DIVIDER_TYPE_FIXED);
        assertThat(defaultAttrs.getWidthDp()).isEqualTo(WIDTH_SYSTEM_DEFAULT);
        assertThat(defaultAttrs.getPrimaryMinRatio()).isEqualTo(RATIO_SYSTEM_DEFAULT);
        assertThat(defaultAttrs.getPrimaryMaxRatio()).isEqualTo(RATIO_SYSTEM_DEFAULT);
    }

    @Test
    public void testDividerAttributesBuilder() {
        final DividerAttributes dividerAttributes1 =
                new DividerAttributes.Builder(DIVIDER_TYPE_FIXED)
                        .setWidthDp(20)
                        .build();
        assertThat(dividerAttributes1.getDividerType()).isEqualTo(DIVIDER_TYPE_FIXED);
        assertThat(dividerAttributes1.getWidthDp()).isEqualTo(20);
        assertThat(dividerAttributes1.getPrimaryMinRatio()).isEqualTo(RATIO_SYSTEM_DEFAULT);
        assertThat(dividerAttributes1.getPrimaryMaxRatio()).isEqualTo(RATIO_SYSTEM_DEFAULT);

        final DividerAttributes dividerAttributes2 =
                new DividerAttributes.Builder(DIVIDER_TYPE_DRAGGABLE)
                        .setWidthDp(20)
                        .setPrimaryMinRatio(0.2f)
                        .setPrimaryMaxRatio(0.8f)
                        .build();
        assertThat(dividerAttributes2.getDividerType()).isEqualTo(DIVIDER_TYPE_DRAGGABLE);
        assertThat(dividerAttributes2.getWidthDp()).isEqualTo(20);
        assertThat(dividerAttributes2.getPrimaryMinRatio()).isEqualTo(0.2f);
        assertThat(dividerAttributes2.getPrimaryMaxRatio()).isEqualTo(0.8f);

        final DividerAttributes dividerAttributes3 =
                new DividerAttributes.Builder(DIVIDER_TYPE_DRAGGABLE)
                        .setWidthDp(20)
                        .build();
        assertThat(dividerAttributes3.getDividerType()).isEqualTo(DIVIDER_TYPE_DRAGGABLE);
        assertThat(dividerAttributes3.getWidthDp()).isEqualTo(20);
        assertThat(dividerAttributes3.getPrimaryMinRatio()).isEqualTo(RATIO_SYSTEM_DEFAULT);
        assertThat(dividerAttributes3.getPrimaryMaxRatio()).isEqualTo(RATIO_SYSTEM_DEFAULT);

        final DividerAttributes dividerAttributes4 =
                new DividerAttributes.Builder(DIVIDER_TYPE_DRAGGABLE)
                        .setWidthDp(20)
                        .setPrimaryMinRatio(0.2f)
                        .build();
        assertThat(dividerAttributes4.getDividerType()).isEqualTo(DIVIDER_TYPE_DRAGGABLE);
        assertThat(dividerAttributes4.getWidthDp()).isEqualTo(20);
        assertThat(dividerAttributes4.getPrimaryMinRatio()).isEqualTo(0.2f);
        assertThat(dividerAttributes4.getPrimaryMaxRatio()).isEqualTo(RATIO_SYSTEM_DEFAULT);

        final DividerAttributes dividerAttributes5 =
                new DividerAttributes.Builder(DIVIDER_TYPE_DRAGGABLE)
                        .setWidthDp(20)
                        .setPrimaryMaxRatio(0.2f)
                        .build();
        assertThat(dividerAttributes5.getDividerType()).isEqualTo(DIVIDER_TYPE_DRAGGABLE);
        assertThat(dividerAttributes5.getWidthDp()).isEqualTo(20);
        assertThat(dividerAttributes5.getPrimaryMinRatio()).isEqualTo(RATIO_SYSTEM_DEFAULT);
        assertThat(dividerAttributes5.getPrimaryMaxRatio()).isEqualTo(0.2f);
    }

    @Test
    public void testDividerAttributesEquals() {
        final DividerAttributes dividerAttributes1 =
                new DividerAttributes.Builder(DIVIDER_TYPE_DRAGGABLE)
                        .setWidthDp(20)
                        .setPrimaryMinRatio(0.2f)
                        .setPrimaryMaxRatio(0.8f)
                        .build();

        final DividerAttributes dividerAttributes2 =
                new DividerAttributes.Builder(DIVIDER_TYPE_DRAGGABLE)
                        .setWidthDp(20)
                        .setPrimaryMinRatio(0.2f)
                        .setPrimaryMaxRatio(0.8f)
                        .build();

        final DividerAttributes dividerAttributes3 =
                new DividerAttributes.Builder(DIVIDER_TYPE_FIXED)
                        .setWidthDp(20)
                        .build();

        assertThat(dividerAttributes1).isEqualTo(dividerAttributes2);
        assertThat(dividerAttributes1).isNotEqualTo(dividerAttributes3);
    }

    @Test
    public void testDividerAttributesValidation() {
        assertThrows(
                "Must not set min max ratio for DIVIDER_TYPE_FIXED",
                IllegalStateException.class,
                () -> new DividerAttributes.Builder(DIVIDER_TYPE_FIXED)
                        .setPrimaryMinRatio(0.2f)
                        .setPrimaryMaxRatio(0.8f)
                        .build()
        );

        assertThrows(
                "Min ratio must be less than or equal to max ratio",
                IllegalStateException.class,
                () -> new DividerAttributes.Builder(DIVIDER_TYPE_DRAGGABLE)
                        .setPrimaryMinRatio(0.8f)
                        .setPrimaryMaxRatio(0.2f)
                        .build()
        );

        assertThrows(
                "Min ratio must be in range [0.0, 1.0] or RATIO_UNSET",
                IllegalArgumentException.class,
                () -> new DividerAttributes.Builder(DIVIDER_TYPE_DRAGGABLE)
                        .setPrimaryMinRatio(2.0f)
        );

        assertThrows(
                "Max ratio must be in range [0.0, 1.0] or RATIO_UNSET",
                IllegalArgumentException.class,
                () -> new DividerAttributes.Builder(DIVIDER_TYPE_DRAGGABLE)
                        .setPrimaryMaxRatio(2.0f)
        );

        assertThrows(
                "Width must be greater than or equal to zero or WIDTH_UNSET",
                IllegalArgumentException.class,
                () -> new DividerAttributes.Builder(DIVIDER_TYPE_DRAGGABLE)
                        .setWidthDp(-10)
        );
    }
}
