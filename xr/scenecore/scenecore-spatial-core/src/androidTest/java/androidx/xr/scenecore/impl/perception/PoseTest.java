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
public final class PoseTest {

    @Test
    public void identityPose_returnsPose() {
        Pose pose = Pose.identity();

        assertThat(pose.tx()).isEqualTo(0);
        assertThat(pose.ty()).isEqualTo(0);
        assertThat(pose.tz()).isEqualTo(0);
        assertThat(pose.qx()).isEqualTo(0);
        assertThat(pose.qy()).isEqualTo(0);
        assertThat(pose.qz()).isEqualTo(0);
        assertThat(pose.qw()).isEqualTo(1);
    }

    @Test
    public void createPose_returnsPose() {
        Pose pose = new Pose(1, 2, 3, 4, 5, 6, 7);

        assertThat(pose.tx()).isEqualTo(1);
        assertThat(pose.ty()).isEqualTo(2);
        assertThat(pose.tz()).isEqualTo(3);
        assertThat(pose.qx()).isEqualTo(4);
        assertThat(pose.qy()).isEqualTo(5);
        assertThat(pose.qz()).isEqualTo(6);
        assertThat(pose.qw()).isEqualTo(7);
    }

    @Test
    public void updateTranslation_updatseTranslation() {
        Pose pose = new Pose(1, 2, 3, 4, 5, 6, 7);

        pose.updateTranslation(10, 20, 30);

        assertThat(pose.tx()).isEqualTo(10);
        assertThat(pose.ty()).isEqualTo(20);
        assertThat(pose.tz()).isEqualTo(30);
        assertThat(pose.qx()).isEqualTo(4);
        assertThat(pose.qy()).isEqualTo(5);
        assertThat(pose.qz()).isEqualTo(6);
        assertThat(pose.qw()).isEqualTo(7);
    }

    @Test
    public void updateRotation_updatesRotation() {
        Pose pose = new Pose(1, 2, 3, 4, 5, 6, 7);

        pose.updateRotation(40, 50, 60, 70);

        assertThat(pose.tx()).isEqualTo(1);
        assertThat(pose.ty()).isEqualTo(2);
        assertThat(pose.tz()).isEqualTo(3);
        assertThat(pose.qx()).isEqualTo(40);
        assertThat(pose.qy()).isEqualTo(50);
        assertThat(pose.qz()).isEqualTo(60);
        assertThat(pose.qw()).isEqualTo(70);
    }

    @Test
    public void equals_returnsTrue() {
        Pose pose = new Pose(1, 2, 3, 4, 5, 6, 7);
        assertThat(pose).isEqualTo(new Pose(1, 2, 3, 4, 5, 6, 7));
    }
}
