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

package androidx.xr.runtime.math

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class GeospatialPoseTest {

    @Test
    fun constructor_noArguments_returnsDefaultValues() {
        val defaultPose = GeospatialPose()

        assertThat(defaultPose.latitude).isEqualTo(0.0)
        assertThat(defaultPose.longitude).isEqualTo(0.0)
        assertThat(defaultPose.altitude).isEqualTo(0.0)
        assertThat(defaultPose.eastUpSouthQuaternion).isEqualTo(Quaternion.Identity)
    }

    @Test
    fun equals_sameValues_returnsTrue() {
        val pose1 =
            GeospatialPose(
                latitude = 10.0,
                longitude = 20.0,
                altitude = 30.0,
                eastUpSouthQuaternion = Quaternion.Identity,
            )
        val pose1Duplicate =
            GeospatialPose(
                latitude = 10.0,
                longitude = 20.0,
                altitude = 30.0,
                eastUpSouthQuaternion = Quaternion.Identity,
            )

        assertThat(pose1).isEqualTo(pose1Duplicate)
    }

    @Test
    fun equals_differentValues_returnsFalse() {
        val pose1 =
            GeospatialPose(
                latitude = 10.0,
                longitude = 20.0,
                altitude = 30.0,
                eastUpSouthQuaternion = Quaternion.Identity,
            )
        val pose2 =
            GeospatialPose(
                latitude = 40.0,
                longitude = 50.0,
                altitude = 60.0,
                eastUpSouthQuaternion = Quaternion(0f, 0.7071f, 0f, 0.7071f),
            )

        assertThat(pose1).isNotEqualTo(pose2)
    }

    @Test
    fun hashCode_sameValues_returnsSameCode() {
        val pose1 =
            GeospatialPose(
                latitude = 10.0,
                longitude = 20.0,
                altitude = 30.0,
                eastUpSouthQuaternion = Quaternion.Identity,
            )
        val pose1Duplicate =
            GeospatialPose(
                latitude = 10.0,
                longitude = 20.0,
                altitude = 30.0,
                eastUpSouthQuaternion = Quaternion.Identity,
            )

        assertThat(pose1.hashCode()).isEqualTo(pose1Duplicate.hashCode())
    }

    @Test
    fun hashCode_differentValues_returnsDifferentCode() {
        val pose1 =
            GeospatialPose(
                latitude = 10.0,
                longitude = 20.0,
                altitude = 30.0,
                eastUpSouthQuaternion = Quaternion.Identity,
            )
        val pose2 =
            GeospatialPose(
                latitude = 40.0,
                longitude = 50.0,
                altitude = 60.0,
                eastUpSouthQuaternion = Quaternion(0f, 0.7071f, 0f, 0.7071f),
            )

        assertThat(pose1.hashCode()).isNotEqualTo(pose2.hashCode())
    }

    @Test
    fun toString_returnsCorrectFormat() {
        val pose =
            GeospatialPose(
                latitude = 10.0,
                longitude = 20.0,
                altitude = 30.0,
                eastUpSouthQuaternion = Quaternion.Identity,
            )
        val expectedString =
            "GeospatialPose{\n" +
                "\tLatitude=10.0\n" +
                "\tLongitude=20.0\n" +
                "\tAltitude=30.0\n" +
                "\tEastUpSouthQuaternion=[x=0.0, y=0.0, z=0.0, w=1.0]\n" +
                "}"

        assertThat(pose.toString()).isEqualTo(expectedString)
    }
}
