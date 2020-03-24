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

package androidx.camera.view.preview.transform.transformation;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TransformationTest {

    @Test
    public void addTransformations() {
        final Transformation transformation = new Transformation(2.3F, 7.5F, 100F, 23.4F, 3.14F);
        final Transformation other = new Transformation(12.3F, 0.5F, 0F, 0.24F, 90F);

        final Transformation sum = transformation.add(other);

        assertThat(sum.getScaleX()).isEqualTo(transformation.getScaleX() * other.getScaleX());
        assertThat(sum.getScaleY()).isEqualTo(transformation.getScaleY() * other.getScaleY());
        assertThat(sum.getTransX()).isEqualTo(transformation.getTransX() + other.getTransX());
        assertThat(sum.getTransY()).isEqualTo(transformation.getTransY() + other.getTransY());
        assertThat(sum.getRotation()).isEqualTo(transformation.getRotation() + other.getRotation());
    }

    @Test
    public void subtractTransformations() {
        final Transformation transformation = new Transformation(2.3F, 7.5F, 100F, 23.4F, 3.14F);
        final Transformation other = new Transformation(12.3F, 0.5F, 0F, 0.24F, 90F);

        final Transformation sum = transformation.subtract(other);

        assertThat(sum.getScaleX()).isEqualTo(transformation.getScaleX() / other.getScaleX());
        assertThat(sum.getScaleY()).isEqualTo(transformation.getScaleY() / other.getScaleY());
        assertThat(sum.getTransX()).isEqualTo(transformation.getTransX() - other.getTransX());
        assertThat(sum.getTransY()).isEqualTo(transformation.getTransY() - other.getTransY());
        assertThat(sum.getRotation()).isEqualTo(transformation.getRotation() - other.getRotation());
    }
}
