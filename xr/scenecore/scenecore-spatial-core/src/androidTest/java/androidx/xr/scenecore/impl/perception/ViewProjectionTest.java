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
public final class ViewProjectionTest {

    @Test
    public void createViewProjection_returnsViewProjection() {
        ViewProjection viewProjection =
                new ViewProjection(new Pose(1, 2, 3, 4, 5, 6, 7), new Fov(8, 9, 10, 11));

        assertThat(viewProjection.getPose()).isEqualTo(new Pose(1, 2, 3, 4, 5, 6, 7));
        assertThat(viewProjection.getFov()).isEqualTo(new Fov(8, 9, 10, 11));
    }

    @Test
    public void equals_returnsTrue() {
        ViewProjection viewProjection1 =
                new ViewProjection(new Pose(1, 2, 3, 4, 5, 6, 7), new Fov(8, 9, 10, 11));
        ViewProjection viewProjection2 =
                new ViewProjection(new Pose(1, 2, 3, 4, 5, 6, 7), new Fov(8, 9, 10, 11));
        assertThat(viewProjection1).isEqualTo(viewProjection2);
    }

    @Test
    public void equals_returnsFalse_forDifferentFov() {
        ViewProjection viewProjection1 =
                new ViewProjection(new Pose(1, 2, 3, 4, 5, 6, 7), new Fov(8, 9, 10, 11));
        ViewProjection viewProjection2 =
                new ViewProjection(new Pose(1, 2, 3, 4, 5, 6, 7), new Fov(8, 9, 10, 13));
        assertThat(viewProjection1).isNotEqualTo(viewProjection2);
    }

    @Test
    public void equals_returnsFalse_forDifferentPose() {
        ViewProjection viewProjection1 =
                new ViewProjection(new Pose(1, 2, 3, 4, 5, 6, 77), new Fov(8, 9, 10, 11));
        ViewProjection viewProjection2 =
                new ViewProjection(new Pose(1, 2, 3, 4, 5, 6, 7), new Fov(8, 9, 10, 11));
        assertThat(viewProjection1).isNotEqualTo(viewProjection2);
    }
}
