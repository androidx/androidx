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

import androidx.xr.runtime.testing.math.assertRotation
import androidx.xr.runtime.testing.math.assertVector3
import com.google.common.truth.Truth.assertThat
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.test.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class PoseTest {

    @Test
    fun constructor_noArguments_returnsZeroVectorAndIdentityQuaternion() {
        val underTest = Pose()

        assertThat(underTest.translation).isEqualTo(Vector3(0f, 0f, 0f))
        assertThat(underTest.rotation).isEqualTo(Quaternion(0f, 0f, 0f, 1f))
    }

    @Test
    fun equals_sameValues_returnsTrue() {
        val underTest =
            Pose(translation = Vector3(1f, 2f, 3f), rotation = Quaternion(4f, 5f, 6f, 7f))
        val underTest2 =
            Pose(translation = Vector3(1f, 2f, 3f), rotation = Quaternion(4f, 5f, 6f, 7f))

        assertThat(underTest).isEqualTo(underTest2)
    }

    @Test
    fun equals_differentValues_returnsFalse() {
        val underTest =
            Pose(translation = Vector3(1f, 2f, 3f), rotation = Quaternion(4f, 5f, 6f, 7f))
        val underTest2 =
            Pose(translation = Vector3(9f, 10f, 11f), rotation = Quaternion(4f, 5f, 6f, 7f))
        val underTest3 = Vector3()

        assertThat(underTest).isNotEqualTo(underTest2)
        assertThat(underTest).isNotEqualTo(underTest3)
    }

    @Test
    fun hashCodeEquals_sameValues_returnsTrue() {
        val underTest =
            Pose(translation = Vector3(1f, 2f, 3f), rotation = Quaternion(4f, 5f, 6f, 7f))
        val underTest2 =
            Pose(translation = Vector3(1f, 2f, 3f), rotation = Quaternion(4f, 5f, 6f, 7f))

        assertThat(underTest.hashCode()).isEqualTo(underTest2.hashCode())
    }

    @Test
    fun hashCodeEquals_differentValues_returnsFalse() {
        val underTest =
            Pose(translation = Vector3(1f, 2f, 3f), rotation = Quaternion(4f, 5f, 6f, 7f))
        val underTest2 =
            Pose(translation = Vector3(9f, 10f, 11f), rotation = Quaternion(4f, 5f, 6f, 7f))
        val underTest3 = Vector3()

        assertThat(underTest.hashCode()).isNotEqualTo(underTest2.hashCode())
        assertThat(underTest.hashCode()).isNotEqualTo(underTest3.hashCode())
    }

    @Test
    fun constructorEquals_expectedToString_returnsTrue() {
        val underTest =
            Pose(translation = Vector3(1f, 2f, 3f), rotation = Quaternion(4f, 5f, 6f, 7f))
        val underTest2 = Pose()

        assertThat(underTest.toString())
            .isEqualTo(
                "Pose{\n\tTranslation=[x=1.0, y=2.0, z=3.0]\n\tRotation=[x=0.35634834, y=0.4454354, z=0.5345225, w=0.6236096]\n}"
            )
        assertThat(underTest2.toString())
            .isEqualTo(
                "Pose{\n\tTranslation=[x=0.0, y=0.0, z=0.0]\n\tRotation=[x=0.0, y=0.0, z=0.0, w=1.0]\n}"
            )
    }

    @Test
    fun distance_returnsLengthOfVectorBetweenTranslations() {
        val underTest = Pose(translation = Vector3(0F, 3f, 4F), rotation = Quaternion())

        // (0, 3, 4) - (0, 0, 0) = (0, 3, 4) -> sqrt(0^2 + 3^2 + 4^2) = sqrt(25)
        assertThat(Pose.distance(underTest, Pose())).isEqualTo(5F)
    }

    @Test
    fun constructor_fromPose_returnsSameValues() {
        val underTest =
            Pose(translation = Vector3(1f, 2f, 3f), rotation = Quaternion(4f, 5f, 6f, 7f))
        val underTest2 = Pose(underTest)

        assertThat(underTest).isEqualTo(underTest2)
    }

    @Test
    fun compose_returnsPoseWithTranslationAndRotation() {
        val underTest =
            Pose(
                translation = Vector3(3f, 0f, 0f),
                rotation = Quaternion(0f, sqrt(2f) / 2, 0f, sqrt(2f) / 2),
            )
        val underTest2 =
            Pose(
                translation = Vector3(0f, 0f, -3f),
                rotation = Quaternion(0f, sqrt(2f) / 2, 0f, sqrt(2f) / 2),
            )

        val underTestCompose = underTest.compose(underTest2)

        assertTranslation(underTestCompose.translation, 0f, 0f, 0f)
        assertRotation(underTestCompose.rotation, 0f, 1f, 0f, 0f)
    }

    @Test
    fun translate_withZeroVector3_returnsSamePose() {
        val underTest = Pose(translation = Vector3(1f, 2f, 3f))
        val translation = Vector3.Zero

        val translatedPose = underTest.translate(translation)

        assertTranslation(translatedPose.translation, 1f, 2f, 3f)
        assertThat(translatedPose.rotation).isEqualTo(underTest.rotation)
    }

    @Test
    fun translate_withVector3_returnsTranslatedPose() {
        val underTest = Pose(translation = Vector3(1f, 2f, 3f))
        val translation = Vector3(1f, 1f, 1f)

        val translatedPose = underTest.translate(translation)

        assertTranslation(translatedPose.translation, 2f, 3f, 4f)
        assertThat(translatedPose.rotation).isEqualTo(underTest.rotation)
    }

    @Test
    fun rotate_withIdentityQuaternion_returnsSamePose() {
        val underTest =
            Pose(translation = Vector3(1f, 2f, 3f), rotation = Quaternion(1f, 2f, 3f, 4f))
        val rotation = Quaternion.Identity

        val rotatedPose = underTest.rotate(rotation)

        assertTranslation(rotatedPose.translation, 1f, 2f, 3f)
        assertRotation(
            rotatedPose.rotation,
            underTest.rotation.x,
            underTest.rotation.y,
            underTest.rotation.z,
            underTest.rotation.w,
        )
    }

    @Test
    fun rotate_withQuaternion_returnsRotatedPose() {
        val underTest = Pose(rotation = Quaternion.Identity)
        val rotation = Quaternion.fromAxisAngle(Vector3.Forward, 180f)

        val rotatedPose = underTest.rotate(rotation)

        assertThat(rotatedPose.translation).isEqualTo(underTest.translation)
        assertRotation(rotatedPose.rotation, rotation.x, rotation.y, rotation.z, rotation.w)
    }

    @Test
    fun rotate_withNonIdentityQuaternion_returnsRotatedPose() {
        // A pose with a rotation of 45 degrees around the Y-axis.
        val underTest = Pose(rotation = Quaternion(0f, sqrt(2f) / 2, 0f, sqrt(2f) / 2))
        // A Quaternion representing a 45-degree rotation around the Y-axis.
        val rotation = Quaternion(0f, sqrt(2f) / 2, 0f, sqrt(2f) / 2)

        val rotatedPose = underTest.rotate(rotation)

        assertThat(rotatedPose.translation).isEqualTo(underTest.translation)
        // The rotation of the rotated Pose is equal to a 90-degree rotation around the Y-axis.
        assertRotation(rotatedPose.rotation, 0f, 1f, 0f, 0f)
    }

    @Test
    fun inverse_returnsPoseWithOppositeTransformation() {
        val underTest =
            Pose(
                translation = Vector3(3f, 0f, 0f),
                rotation = Quaternion(0f, sqrt(2f) / 2, 0f, sqrt(2f) / 2),
            )

        val underTestInverted = underTest.inverse

        assertTranslation(underTestInverted.translation, 0f, 0f, -3f)
        assertRotation(underTestInverted.rotation, 0f, -sqrt(2f) / 2, 0f, sqrt(2f) / 2)
    }

    @Test
    fun transform_returnsTransformedPointByPose() {
        val underTest =
            Pose(translation = Vector3(0f, 0f, 1f), rotation = Quaternion(0f, 0.7071f, 0f, 0.7071f))
        val point = Vector3(0f, 0f, 1f)

        val transformedPoint = underTest.transformPoint(point)

        assertTranslation(transformedPoint, 1f, 0f, 1f)
    }

    @Test
    fun lerp_returnsInterpolatedPose() {
        val underTest =
            Pose(translation = Vector3(1f, 2f, 3f), rotation = Quaternion(4f, 5f, 6f, 7f))
        val underTest2 =
            Pose(translation = Vector3(4f, 5f, 6f), rotation = Quaternion(8f, 9f, 10f, 11f))

        val interpolatedPose = Pose.lerp(underTest, underTest2, 0.5f)

        assertTranslation(interpolatedPose.translation, 2.5f, 3.5f, 4.5f)
        assertRotation(interpolatedPose.rotation, 0.38759f, 0.45833f, 0.52907f, 0.59981f)
    }

    @Test
    fun up_returnsUpVectorInLocalCoordinateSystem1() {
        val underTest =
            Pose(translation = Vector3(1f, 2f, 3f), rotation = Quaternion(0f, 0.7071f, 0f, 0.7071f))

        assertTranslation(underTest.up, 0f, 1f, 0f)
    }

    @Test
    fun up_returnsUpVectorInLocalCoordinateSystem2() {
        val underTest =
            Pose(translation = Vector3(1f, 2f, 3f), rotation = Quaternion(0.7071f, 0f, 0f, 0.7071f))

        assertTranslation(underTest.up, 0f, 0f, 1f)
    }

    @Test
    fun down_returnsDownVectorInLocalCoordinateSystem1() {
        val underTest =
            Pose(translation = Vector3(1f, 2f, 3f), rotation = Quaternion(0f, 0.7071f, 0f, 0.7071f))

        assertTranslation(underTest.down, 0f, -1f, 0f)
    }

    @Test
    fun down_returnsDownVectorInLocalCoordinateSystem2() {
        val underTest =
            Pose(translation = Vector3(1f, 2f, 3f), rotation = Quaternion(0.7071f, 0f, 0f, 0.7071f))

        assertTranslation(underTest.down, 0f, 0f, -1f)
    }

    @Test
    fun left_returnsLeftVectorInLocalCoordinateSystem1() {
        val underTest =
            Pose(translation = Vector3(1f, 2f, 3f), rotation = Quaternion(0.7071f, 0f, 0f, 0.7071f))

        assertTranslation(underTest.left, -1f, 0f, 0f)
    }

    @Test
    fun left_returnsLeftVectorInLocalCoordinateSystem2() {
        val underTest =
            Pose(translation = Vector3(1f, 2f, 3f), rotation = Quaternion(0f, 0.7071f, 0f, 0.7071f))

        assertTranslation(underTest.left, 0f, 0f, 1f)
    }

    @Test
    fun right_returnsRightVectorInLocalCoordinateSystem1() {
        val underTest =
            Pose(translation = Vector3(1f, 2f, 3f), rotation = Quaternion(0.7071f, 0f, 0f, 0.7071f))

        assertTranslation(underTest.right, 1f, 0f, 0f)
    }

    @Test
    fun right_returnsRightVectorInLocalCoordinateSystem2() {
        val underTest =
            Pose(translation = Vector3(1f, 2f, 3f), rotation = Quaternion(0f, 0.7071f, 0f, 0.7071f))

        assertTranslation(underTest.right, 0f, 0f, -1f)
    }

    @Test
    fun forward_returnsForwardVectorInLocalCoordinateSystem1() {
        val underTest =
            Pose(translation = Vector3(1f, 2f, 3f), rotation = Quaternion(0f, 0f, 0.7071f, 0.7071f))

        assertTranslation(underTest.forward, 0f, 0f, -1f)
    }

    @Test
    fun forward_returnsForwardVectorInLocalCoordinateSystem2() {
        val underTest =
            Pose(translation = Vector3(1f, 2f, 3f), rotation = Quaternion(0.7071f, 0f, 0f, 0.7071f))

        assertTranslation(underTest.forward, 0f, 1f, 0f)
    }

    @Test
    fun backward_returnsBackwardVectorInLocalCoordinateSystem1() {
        val underTest =
            Pose(translation = Vector3(1f, 2f, 3f), rotation = Quaternion(0f, 0f, 0.7071f, 0.7071f))

        assertTranslation(underTest.backward, 0f, 0f, 1f)
    }

    @Test
    fun backward_returnsBackwardVectorInLocalCoordinateSystem2() {
        val underTest =
            Pose(translation = Vector3(1f, 2f, 3f), rotation = Quaternion(0.7071f, 0f, 0f, 0.7071f))

        assertTranslation(underTest.backward, 0f, -1f, 0f)
    }

    @Test
    fun transformVector_returnsVectorTransformedByPose() {
        val underTest =
            Pose(translation = Vector3(1f, 2f, 3f), rotation = Quaternion(1f, 2f, 3f, 4f))
        val vector = Vector3(1f, 2f, 3f)
        val underTestRotated = underTest.transformVector(vector)

        assertTranslation(underTestRotated, 1f, 2f, 3f)
    }

    @Test
    fun copy_returnsCopyOfPose() {
        val underTest =
            Pose(translation = Vector3(1f, 2f, 3f), rotation = Quaternion(4f, 5f, 6f, 7f))
        val underTest2 = underTest.copy()

        assertThat(underTest).isEqualTo(underTest2)
    }

    @Test
    fun fromLookAt_returnsPoseLookingAtTarget() {
        val underTest =
            Pose.fromLookAt(
                eye = Vector3.Zero,
                target = Vector3(0f, 0f, 10f),
                up = Vector3(0f, 1f, 0f),
            )

        assertTranslation(underTest.translation, 0f, 0f, 0f)
        assertRotation(underTest.rotation, 0f, 0f, 0f, 1f)
    }

    private fun rotate(result: Quaternion, vector: Vector3): Vector3 {
        val pureQuaternion = Quaternion(vector.x, vector.y, vector.z, 0f)
        val rotatedP = result * pureQuaternion * result.inverse
        return Vector3(rotatedP.x, rotatedP.y, rotatedP.z)
    }

    @Test
    fun getForwardVectorToUpRotation_alignsToPlane() {
        // Moving from (0f, 0f, 0f, 1f) int the common space to (0f, 0f, 0f, 1f) in the plane
        // rotation results in an updated rotation of (-0.707f, 0f, 0f, 0.707f). This quaternion
        // represents a 90-degree rotation around the x-axis Which is expected when the panel is
        // rotated into the plane's reference space.
        val planePose = Pose.Identity
        val proposedPose = Pose.Identity
        val updatedRotation = proposedPose.getForwardVectorToUpRotation(other = planePose)
        val expectedRotation = Quaternion(-0.707f, 0f, 0f, 0.707f)

        assertRotation(updatedRotation, expectedRotation)
    }

    @Test
    fun getForwardVectorToUpRotation_normalY_proposedX_alignsPanelZToY() {
        val planePose = Pose(rotation = Quaternion())
        val entityPose = Pose(rotation = Quaternion())

        val resultQuat = entityPose.getForwardVectorToUpRotation(other = planePose)

        val panelForward = rotate(resultQuat, Z_AXIS)
        assertVector3(Y_AXIS, Vector3(panelForward.x, panelForward.y, panelForward.z))

        val panelUp = rotate(resultQuat, Y_AXIS)
        assertVector3(-Z_AXIS, Vector3(panelUp.x, panelUp.y, panelUp.z))
    }

    @Test
    fun getForwardVectorToUpRotation_ParallelVectors_notNaN() {
        val planePose = Pose(Vector3(0f, 0f, 0f), Quaternion())

        val entityRotation = Quaternion.fromEulerAngles(0f, 0f, 90f)
        val entityPose = Pose(Vector3(0f, 0f, 0f), entityRotation)

        val result = entityPose.getForwardVectorToUpRotation(other = planePose)

        assertNotNull(result)
        assertFalse("Quaternion should not contain NaN", result.x.isNaN() || result.y.isNaN())
    }

    @Test
    fun getUpVectorToUpRotation_rotatesToPlane() {
        val entityPose = Pose.Identity
        val planePose = Pose.Identity
        val updatedRotation = entityPose.getUpVectorToUpRotation(other = planePose)
        val expectedRotation = Quaternion(0.0f, 0.0f, 0.0f, 1.0f)

        assertRotation(updatedRotation, expectedRotation)
    }

    @Test
    fun getUpVectorToUpRotation_proposedYOrthogonalToPlaneY_alignsToPlaneY() {
        val entityPose = Pose.Identity

        val planeRotation = Quaternion.fromAxisAngle(Vector3(0f, 0f, 1f), -90f)
        val planePose = Pose(Vector3.Zero, planeRotation)

        val expectedPlaneUp = Vector3(1f, 0f, 0f)
        val expectedPlaneRight = Vector3(0f, -1f, 0f)
        assertVector3(expectedPlaneUp, planePose.up)
        assertVector3(expectedPlaneRight, planePose.right)

        val pY = entityPose.up.toNormalized()
        val lY = planePose.up.toNormalized()
        assertEquals((pY.dot(lY)), 0.0f, 0.0001f)

        val updatedRotation = entityPose.getUpVectorToUpRotation(other = planePose)

        val resultingYAxis = updatedRotation * Vector3(0f, 1f, 0f)
        val targetYAxis = planePose.up.toNormalized()
        assertVector3(targetYAxis, resultingYAxis)

        val resultingZAxis = updatedRotation * Vector3(0f, 0f, 1f)
        val expectedZAxis = Vector3(0f, 1f, 0f) // Should align with forwardDirection
        assertVector3(expectedZAxis, resultingZAxis)
    }

    @Test
    fun getUpVectorToUpRotation_generalOrientation_alignsUp() {
        val entityPose = Pose.Identity

        val angle = 30f
        val planePose = Pose(Vector3.Zero, Quaternion.fromAxisAngle(Vector3(1f, 0f, 0f), angle))
        val planeNormal = planePose.up // (0, cos(30), sin(30))
        val entityUp = entityPose.up

        val resultQuat = entityPose.getUpVectorToUpRotation(planePose)

        val newUp = resultQuat * Vector3(0f, 1f, 0f)
        assertVector3(planeNormal, newUp) // New entity up should match plane normal

        val newForward = resultQuat * Vector3(0f, 0f, 1f)
        assertEquals(
            0f,
            newForward.dot(planeNormal),
            0.00001f,
        ) // New entity forward should be in the plane

        val s = sin(Math.toRadians(angle.toDouble())).toFloat()
        val c = cos(Math.toRadians(angle.toDouble())).toFloat()
        val expectedForwardDir = Vector3(0f, -s, c).toNormalized()
        assertVector3(expectedForwardDir, newForward)
    }

    companion object {
        private val Y_AXIS = Vector3(y = 1f)
        private val Z_AXIS = Vector3(z = 1f)
    }

    private fun assertTranslation(
        translation: Vector3,
        expectedX: Float,
        expectedY: Float,
        expectedZ: Float,
    ) {
        assertThat(translation.x).isWithin(1.0e-4f).of(expectedX)
        assertThat(translation.y).isWithin(1.0e-4f).of(expectedY)
        assertThat(translation.z).isWithin(1.0e-4f).of(expectedZ)
    }

    private fun assertRotation(
        rotation: Quaternion,
        expectedX: Float,
        expectedY: Float,
        expectedZ: Float,
        expectedW: Float,
    ) {
        assertThat(rotation.x).isWithin(1.0e-4f).of(expectedX)
        assertThat(rotation.y).isWithin(1.0e-4f).of(expectedY)
        assertThat(rotation.z).isWithin(1.0e-4f).of(expectedZ)
        assertThat(rotation.w).isWithin(1.0e-4f).of(expectedW)
    }
}
