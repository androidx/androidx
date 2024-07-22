/*
 * Copyright 2023 The Android Open Source Project
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

import static androidx.window.extensions.embedding.WindowAttributes.DIM_AREA_ON_ACTIVITY_STACK;
import static androidx.window.extensions.embedding.WindowAttributes.DIM_AREA_ON_TASK;

import static com.google.common.truth.Truth.assertThat;

import android.graphics.Rect;

import org.junit.Test;

/**
 * Verifies {@link ActivityStackAttributes} behavior.
 */
public class ActivityStackAttributesTest {

    @Test
    public void testActivityStackAttributesDefaults() {
        final ActivityStackAttributes defaultAttrs = new ActivityStackAttributes.Builder().build();
        assertThat(defaultAttrs.getRelativeBounds().isEmpty()).isTrue();
        assertThat(defaultAttrs.getWindowAttributes().getDimAreaBehavior())
                .isEqualTo(DIM_AREA_ON_ACTIVITY_STACK);
    }

    @Test
    public void testActivityStackAttributesEqualsMatchHashCode() {
        final ActivityStackAttributes attrs1 = new ActivityStackAttributes.Builder()
                .setRelativeBounds(new Rect(0, 0, 10, 10))
                .setWindowAttributes(new WindowAttributes(DIM_AREA_ON_ACTIVITY_STACK))
                .build();

        final ActivityStackAttributes attrs2 = new ActivityStackAttributes.Builder()
                .setRelativeBounds(new Rect(0, 0, 10, 10))
                .setWindowAttributes(new WindowAttributes(DIM_AREA_ON_TASK))
                .build();

        final ActivityStackAttributes attrs3 = new ActivityStackAttributes.Builder()
                .setRelativeBounds(new Rect(10, 0, 20, 10))
                .setWindowAttributes(new WindowAttributes(DIM_AREA_ON_ACTIVITY_STACK))
                .build();

        final ActivityStackAttributes attrs4 = new ActivityStackAttributes.Builder()
                .setRelativeBounds(new Rect(10, 0, 20, 10))
                .setWindowAttributes(new WindowAttributes(DIM_AREA_ON_TASK))
                .build();

        final ActivityStackAttributes attrs5 = new ActivityStackAttributes.Builder()
                .setRelativeBounds(new Rect(0, 0, 10, 10))
                .setWindowAttributes(new WindowAttributes(DIM_AREA_ON_ACTIVITY_STACK))
                .build();

        assertThat(attrs1).isNotEqualTo(attrs2);
        assertThat(attrs1.hashCode()).isNotEqualTo(attrs2.hashCode());
        assertThat(attrs1).isNotEqualTo(attrs3);
        assertThat(attrs1.hashCode()).isNotEqualTo(attrs3.hashCode());
        assertThat(attrs1).isNotEqualTo(attrs4);
        assertThat(attrs1.hashCode()).isNotEqualTo(attrs4.hashCode());
        assertThat(attrs1).isEqualTo(attrs5);
        assertThat(attrs1.hashCode()).isEqualTo(attrs5.hashCode());
    }
}
