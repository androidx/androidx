/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.scenecore.integration.tests

import androidx.test.platform.app.InstrumentationRegistry
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlin.math.sqrt

/**
 * Golden image directory path for SceneCore integration screenshot tests, used with
 * `XrScreenshotTestRule(GOLDEN_PATH)`.
 */
internal const val GOLDEN_PATH = "xr/scenecore/integration-tests/xr-integration-tests"

/** Default per-component tolerance for exact pose, vector, and unit-quaternion comparisons. */
internal const val DEFAULT_EPSILON = 1e-5f

/**
 * Per-component tolerance for comparisons involving composed spatial transforms or matrix
 * decomposition round-trips.
 */
internal const val COMPOSED_TRANSFORM_EPSILON = 1e-4f

/**
 * Whether binary protobuf CPM scene capture (`dumpsys spf_cpm --proto`) is enabled via the
 * `xrSceneUseProto` instrumentation runner argument.
 */
// TODO(b/585229482): Remove this guard and make proto-only scene assertions unconditional once all
// CI runners pass xrSceneUseProto=true (or proto capture becomes the default in :xr:testutils).
internal val isProtoSceneCaptureEnabled: Boolean
    get() =
        InstrumentationRegistry.getArguments()
            .getString("xrSceneUseProto")
            .equals("true", ignoreCase = true)

/**
 * Asserts that [actual] and [expected] have finite components within [translationEpsilon] per
 * coordinate and [rotationEpsilon] per sign-aligned unit-quaternion component (not degrees or
 * radians; `q` and `-q` are treated as equivalent).
 */
internal fun assertPose(
    actual: Pose,
    expected: Pose,
    translationEpsilon: Float = DEFAULT_EPSILON,
    rotationEpsilon: Float = DEFAULT_EPSILON,
) {
    assertVector3(actual.translation, expected.translation, translationEpsilon)
    assertQuaternion(actual.rotation, expected.rotation, rotationEpsilon)
}

/** Asserts that [actual] and [expected] have finite components within [epsilon] per coordinate. */
internal fun assertVector3(
    actual: Vector3,
    expected: Vector3,
    epsilon: Float = DEFAULT_EPSILON,
) {
    assertFiniteVector3(actual, "actual")
    assertFiniteVector3(expected, "expected")
    assertThat(actual.x).isWithin(epsilon).of(expected.x)
    assertThat(actual.y).isWithin(epsilon).of(expected.y)
    assertThat(actual.z).isWithin(epsilon).of(expected.z)
}

/**
 * Asserts that [actual] and [expected] are finite, positive-norm quaternions whose sign-aligned
 * unit components match within [epsilon] per component (not degrees or radians; `q` and `-q` are
 * treated as equivalent).
 */
internal fun assertQuaternion(
    actual: Quaternion,
    expected: Quaternion,
    epsilon: Float = DEFAULT_EPSILON,
) {
    assertFiniteQuaternion(actual, "actual")
    assertFiniteQuaternion(expected, "expected")

    val actualNorm = quaternionNorm(actual)
    val expectedNorm = quaternionNorm(expected)
    assertWithMessage("actual norm ($actualNorm) must be positive")
        .that(actualNorm)
        .isGreaterThan(1e-6f)
    assertWithMessage("expected norm ($expectedNorm) must be positive")
        .that(expectedNorm)
        .isGreaterThan(1e-6f)

    val ax = actual.x / actualNorm
    val ay = actual.y / actualNorm
    val az = actual.z / actualNorm
    val aw = actual.w / actualNorm

    val ex = expected.x / expectedNorm
    val ey = expected.y / expectedNorm
    val ez = expected.z / expectedNorm
    val ew = expected.w / expectedNorm

    val dot = ax * ex + ay * ey + az * ez + aw * ew
    val sign = if (dot < 0f) -1f else 1f
    assertThat(ax * sign).isWithin(epsilon).of(ex)
    assertThat(ay * sign).isWithin(epsilon).of(ey)
    assertThat(az * sign).isWithin(epsilon).of(ez)
    assertThat(aw * sign).isWithin(epsilon).of(ew)
}

private fun assertFiniteVector3(vector: Vector3, label: String) {
    assertWithMessage("$label.x (${vector.x}) must be finite").that(vector.x.isFinite()).isTrue()
    assertWithMessage("$label.y (${vector.y}) must be finite").that(vector.y.isFinite()).isTrue()
    assertWithMessage("$label.z (${vector.z}) must be finite").that(vector.z.isFinite()).isTrue()
}

private fun assertFiniteQuaternion(rotation: Quaternion, label: String) {
    assertWithMessage("$label.x (${rotation.x}) must be finite")
        .that(rotation.x.isFinite())
        .isTrue()
    assertWithMessage("$label.y (${rotation.y}) must be finite")
        .that(rotation.y.isFinite())
        .isTrue()
    assertWithMessage("$label.z (${rotation.z}) must be finite")
        .that(rotation.z.isFinite())
        .isTrue()
    assertWithMessage("$label.w (${rotation.w}) must be finite")
        .that(rotation.w.isFinite())
        .isTrue()
}

private fun quaternionNorm(rotation: Quaternion): Float =
    sqrt(
        rotation.x * rotation.x +
            rotation.y * rotation.y +
            rotation.z * rotation.z +
            rotation.w * rotation.w
    )
