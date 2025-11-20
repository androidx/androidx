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

package androidx.xr.scenecore.impl.perception;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class FovTest {

    @Test
    public void createFov_returnsFov() {
        Fov fov = new Fov(1.0f, 2.0f, 3.0f, 4.0f);
        assertThat(fov.getAngleLeft()).isEqualTo(1.0f);
        assertThat(fov.getAngleRight()).isEqualTo(2.0f);
        assertThat(fov.getAngleUp()).isEqualTo(3.0f);
        assertThat(fov.getAngleDown()).isEqualTo(4.0f);
    }

    @Test
    public void equals_returnsTrue() {
        Fov fov1 = new Fov(1.0f, 2.0f, 3.0f, 4.0f);
        Fov fov2 = new Fov(1.0f, 2.0f, 3.0f, 4.0f);
        assertThat(fov1.equals(fov2)).isTrue();
    }

    @Test
    public void equals_returnsFalse() {
        Fov fov1 = new Fov(1.0f, 2.0f, 3.0f, 4.0f);
        Fov fov2 = new Fov(1.0f, 2.0f, 3.0f, 5.0f);
        assertThat(fov1.equals(fov2)).isFalse();
    }
}
