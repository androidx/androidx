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

package androidx.xr.compose.testing

import android.content.res.Resources
import androidx.annotation.RestrictTo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.isUnspecified
import androidx.xr.compose.unit.DpVolumeSize
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import com.google.errorprone.annotations.CanIgnoreReturnValue
import kotlin.math.abs

/**
 * Asserts that the layout of this node has width equal to [expectedWidth].
 *
 * @param expectedWidth The width to assert.
 * @throws AssertionError if comparison fails.
 */
@CanIgnoreReturnValue
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public fun SubspaceSemanticsNodeInteraction.assertWidthIsEqualTo(
    expectedWidth: Dp
): SubspaceSemanticsNodeInteraction {
    return withSize { it.width.assertIsEqualTo(expectedWidth, "width") }
}

/**
 * Asserts that the layout of this node has width that is NOT equal to [expectedWidth].
 *
 * @param expectedWidth The width to assert.
 * @throws AssertionError if comparison fails.
 */
@CanIgnoreReturnValue
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public fun SubspaceSemanticsNodeInteraction.assertWidthIsNotEqualTo(
    expectedWidth: Dp
): SubspaceSemanticsNodeInteraction {
    return withSize { it.width.assertIsNotEqualTo(expectedWidth, "width") }
}

/**
 * Asserts that the layout of this node has height equal to [expectedHeight].
 *
 * @param expectedHeight The height to assert.
 * @throws AssertionError if comparison fails.
 */
@CanIgnoreReturnValue
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public fun SubspaceSemanticsNodeInteraction.assertHeightIsEqualTo(
    expectedHeight: Dp
): SubspaceSemanticsNodeInteraction {
    return withSize { it.height.assertIsEqualTo(expectedHeight, "height") }
}

/**
 * Asserts that the layout of this node has height that is NOT equal to [expectedHeight].
 *
 * @param expectedHeight The height to assert.
 * @throws AssertionError if comparison fails.
 */
@CanIgnoreReturnValue
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public fun SubspaceSemanticsNodeInteraction.assertHeightIsNotEqualTo(
    expectedHeight: Dp
): SubspaceSemanticsNodeInteraction {
    return withSize { it.height.assertIsNotEqualTo(expectedHeight, "height") }
}

/**
 * Asserts that the layout of this node has depth equal to [expectedDepth].
 *
 * @param expectedDepth The depth to assert.
 * @throws AssertionError if comparison fails.
 */
@CanIgnoreReturnValue
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public fun SubspaceSemanticsNodeInteraction.assertDepthIsEqualTo(
    expectedDepth: Dp
): SubspaceSemanticsNodeInteraction {
    return withSize { it.depth.assertIsEqualTo(expectedDepth, "depth") }
}

/**
 * Asserts that the layout of this node has width that is greater than or equal to
 * [expectedMinWidth].
 *
 * @param expectedMinWidth The minimum width to assert.
 * @throws AssertionError if comparison fails.
 */
@CanIgnoreReturnValue
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public fun SubspaceSemanticsNodeInteraction.assertWidthIsAtLeast(
    expectedMinWidth: Dp
): SubspaceSemanticsNodeInteraction {
    return withSize { it.width.assertIsAtLeast(expectedMinWidth, "width") }
}

/**
 * Asserts that the layout of this node has height that is greater than or equal to
 * [expectedMinHeight].
 *
 * @param expectedMinHeight The minimum height to assert.
 * @throws AssertionError if comparison fails.
 */
@CanIgnoreReturnValue
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public fun SubspaceSemanticsNodeInteraction.assertHeightIsAtLeast(
    expectedMinHeight: Dp
): SubspaceSemanticsNodeInteraction {
    return withSize { it.height.assertIsAtLeast(expectedMinHeight, "height") }
}

/**
 * Asserts that the layout of this node has depth that is greater than or equal to
 * [expectedMinDepth].
 *
 * @param expectedMinDepth The minimum depth to assert.
 * @throws AssertionError if comparison fails.
 */
@CanIgnoreReturnValue
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public fun SubspaceSemanticsNodeInteraction.assertDepthIsAtLeast(
    expectedMinDepth: Dp
): SubspaceSemanticsNodeInteraction {
    return withSize { it.depth.assertIsAtLeast(expectedMinDepth, "depth") }
}

/**
 * Asserts that the layout of this node has position in the root composable that is equal to the
 * given position.
 *
 * @param expectedX The x position to assert.
 * @param expectedY The y position to assert.
 * @param expectedZ The z position to assert.
 * @throws AssertionError if comparison fails.
 */
@CanIgnoreReturnValue
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public fun SubspaceSemanticsNodeInteraction.assertPositionInRootIsEqualTo(
    expectedX: Dp,
    expectedY: Dp,
    expectedZ: Dp,
): SubspaceSemanticsNodeInteraction {

    return withPositionInRoot {
        it.x.toDp().assertIsEqualTo(expectedX, "x")
        it.y.toDp().assertIsEqualTo(expectedY, "y")
        it.z.toDp().assertIsEqualTo(expectedZ, "z")
    }
}

/**
 * Asserts that the layout of this node has the x position in the root composable that is equal to
 * the given position.
 *
 * @param expectedX The x position to assert.
 * @throws AssertionError if comparison fails.
 */
@CanIgnoreReturnValue
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public fun SubspaceSemanticsNodeInteraction.assertXPositionInRootIsEqualTo(
    expectedX: Dp
): SubspaceSemanticsNodeInteraction {
    return withPositionInRoot { it.x.toDp().assertIsEqualTo(expectedX, "x") }
}

/**
 * Asserts that the layout of this node has the left position in the root composable that is equal
 * to the given position.
 *
 * @param expectedLeft The left position to assert.
 * @throws AssertionError if comparison fails.
 */
@CanIgnoreReturnValue
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public fun SubspaceSemanticsNodeInteraction.assertLeftPositionInRootIsEqualTo(
    expectedLeft: Dp
): SubspaceSemanticsNodeInteraction {
    val node = fetchSemanticsNode("Failed to retrieve the node.")
    (node.poseInRoot.translation.x.toDp() - node.size.width.toDp() / 2.0f).assertIsEqualTo(
        expectedLeft,
        "left",
    )
    return this
}

/**
 * Asserts that the layout of this node has the y position in the root composable that is equal to
 * the given position.
 *
 * @param expectedY The y position to assert.
 * @throws AssertionError if comparison fails.
 */
@CanIgnoreReturnValue
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public fun SubspaceSemanticsNodeInteraction.assertYPositionInRootIsEqualTo(
    expectedY: Dp
): SubspaceSemanticsNodeInteraction {
    return withPositionInRoot { it.y.toDp().assertIsEqualTo(expectedY, "y") }
}

/**
 * Asserts that the layout of this node has the top position in the root composable that is equal to
 * the given position.
 *
 * @param expectedTop The top position to assert.
 * @throws AssertionError if comparison fails.
 */
@CanIgnoreReturnValue
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public fun SubspaceSemanticsNodeInteraction.assertTopPositionInRootIsEqualTo(
    expectedTop: Dp
): SubspaceSemanticsNodeInteraction {
    val node = fetchSemanticsNode("Failed to retrieve the node.")
    (node.poseInRoot.translation.y.toDp() + node.size.height.toDp() / 2.0f).assertIsEqualTo(
        expectedTop,
        "top",
    )
    return this
}

/**
 * Asserts that the layout of this node has the z position in the root composable that is equal to
 * the given position.
 *
 * @param expectedZ The z position to assert.
 * @throws AssertionError if comparison fails.
 */
@CanIgnoreReturnValue
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public fun SubspaceSemanticsNodeInteraction.assertZPositionInRootIsEqualTo(
    expectedZ: Dp
): SubspaceSemanticsNodeInteraction {
    return withPositionInRoot { it.z.toDp().assertIsEqualTo(expectedZ, "z") }
}

/**
 * Asserts that the layout of this node has position that is equal to the given position.
 *
 * @param expectedX The x position to assert.
 * @param expectedY The y position to assert.
 * @param expectedZ The z position to assert.
 * @throws AssertionError if comparison fails.
 */
@CanIgnoreReturnValue
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public fun SubspaceSemanticsNodeInteraction.assertPositionIsEqualTo(
    expectedX: Dp,
    expectedY: Dp,
    expectedZ: Dp,
): SubspaceSemanticsNodeInteraction {

    return withPosition {
        it.x.toDp().assertIsEqualTo(expectedX, "x")
        it.y.toDp().assertIsEqualTo(expectedY, "y")
        it.z.toDp().assertIsEqualTo(expectedZ, "z")
    }
}

/**
 * Asserts that the layout of this node has the x position that is equal to the given position.
 *
 * @param expectedX The x position to assert.
 * @throws AssertionError if comparison fails.
 */
@CanIgnoreReturnValue
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public fun SubspaceSemanticsNodeInteraction.assertXPositionIsEqualTo(
    expectedX: Dp
): SubspaceSemanticsNodeInteraction {
    return withPosition { it.x.toDp().assertIsEqualTo(expectedX, "x") }
}

/**
 * Asserts that the layout of this node has the y position that is equal to the given position.
 *
 * @param expectedY The y position to assert.
 * @throws AssertionError if comparison fails.
 */
@CanIgnoreReturnValue
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public fun SubspaceSemanticsNodeInteraction.assertYPositionIsEqualTo(
    expectedY: Dp
): SubspaceSemanticsNodeInteraction {
    return withPosition { it.y.toDp().assertIsEqualTo(expectedY, "y") }
}

/**
 * Asserts that the layout of this node has the z position that is equal to the given position.
 *
 * @param expectedZ The z position to assert.
 * @throws AssertionError if comparison fails.
 */
@CanIgnoreReturnValue
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public fun SubspaceSemanticsNodeInteraction.assertZPositionIsEqualTo(
    expectedZ: Dp
): SubspaceSemanticsNodeInteraction {
    return withPosition { it.z.toDp().assertIsEqualTo(expectedZ, "z") }
}

/**
 * Asserts that the layout of this node has rotation in the root composable that is equal to the
 * given rotation.
 *
 * @param expected The rotation to assert.
 * @throws AssertionError if comparison fails.
 */
@CanIgnoreReturnValue
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public fun SubspaceSemanticsNodeInteraction.assertRotationInRootIsEqualTo(
    expected: Quaternion
): SubspaceSemanticsNodeInteraction {

    val makeError = { subject: String, exp: Float, actual: Float ->
        "Actual $subject is $actual: expected $exp"
    }

    return withRotationInRoot {
        check(it.x.equals(expected.x)) { makeError.invoke("x", expected.x, it.x) }
        check(it.y.equals(expected.y)) { makeError.invoke("y", expected.y, it.y) }
        check(it.z.equals(expected.z)) { makeError.invoke("z", expected.z, it.z) }
        check(it.w.equals(expected.w)) { makeError.invoke("w", expected.w, it.w) }
    }
}

/**
 * Asserts that the layout of this node has rotation that is equal to the given rotation.
 *
 * @param expected The rotation to assert.
 * @throws AssertionError if comparison fails.
 */
@CanIgnoreReturnValue
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public fun SubspaceSemanticsNodeInteraction.assertRotationIsEqualTo(
    expected: Quaternion
): SubspaceSemanticsNodeInteraction {

    return withRotation {
        check(it.x.equals(expected.x)) { "Actual x is ${it.x}: expected ${expected.x}" }
        check(it.y.equals(expected.y)) { "Actual y is ${it.y}: expected ${expected.y}" }
        check(it.z.equals(expected.z)) { "Actual z is ${it.z}: expected ${expected.z}" }
        check(it.w.equals(expected.w)) { "Actual w is ${it.w}: expected ${expected.w}" }
    }
}

/**
 * Returns the size of the node.
 *
 * Additional assertions with custom tolerances may be performed on the individual values.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public fun SubspaceSemanticsNodeInteraction.getSize(): DpVolumeSize {
    lateinit var size: DpVolumeSize
    withSize { size = it }
    return size
}

/**
 * Returns the position of the node relative to its parent layout node.
 *
 * Additional assertions with custom tolerances may be performed on the individual values.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public fun SubspaceSemanticsNodeInteraction.getPosition(): Vector3 {
    lateinit var position: Vector3
    withPosition { position = it }
    return position
}

/**
 * Returns the position of the node relative to the root node.
 *
 * Additional assertions with custom tolerances may be performed on the individual values.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public fun SubspaceSemanticsNodeInteraction.getPositionInRoot(): Vector3 {
    lateinit var position: Vector3
    withPositionInRoot { position = it }
    return position
}

/**
 * Returns the rotation of the node relative to its parent layout node.
 *
 * Additional assertions with custom tolerances may be performed on the individual values.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public fun SubspaceSemanticsNodeInteraction.getRotation(): Quaternion {
    lateinit var rotation: Quaternion
    withRotation { rotation = it }
    return rotation
}

/**
 * Returns the rotation of the node relative to the root node.
 *
 * Additional assertions with custom tolerances may be performed on the individual values.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public fun SubspaceSemanticsNodeInteraction.getRotationInRoot(): Quaternion {
    lateinit var rotation: Quaternion
    withRotationInRoot { rotation = it }
    return rotation
}

@CanIgnoreReturnValue
private fun SubspaceSemanticsNodeInteraction.withSize(
    assertion: (DpVolumeSize) -> Unit
): SubspaceSemanticsNodeInteraction {
    val node = fetchSemanticsNode("Failed to retrieve size of the node.")
    val size = node.size.let { DpVolumeSize(it.width.toDp(), it.height.toDp(), it.depth.toDp()) }
    assertion.invoke(size)
    return this
}

@CanIgnoreReturnValue
private fun SubspaceSemanticsNodeInteraction.withPosition(
    assertion: (Vector3) -> Unit
): SubspaceSemanticsNodeInteraction {
    val node = fetchSemanticsNode("Failed to retrieve position of the node.")
    assertion.invoke(node.pose.translation)
    return this
}

@CanIgnoreReturnValue
private fun SubspaceSemanticsNodeInteraction.withPositionInRoot(
    assertion: (Vector3) -> Unit
): SubspaceSemanticsNodeInteraction {
    val node = fetchSemanticsNode("Failed to retrieve position of the node.")
    assertion.invoke(node.poseInRoot.translation)
    return this
}

@CanIgnoreReturnValue
private fun SubspaceSemanticsNodeInteraction.withRotation(
    assertion: (Quaternion) -> Unit
): SubspaceSemanticsNodeInteraction {
    val node = fetchSemanticsNode("Failed to retrieve rotation of the node.")
    assertion.invoke(node.pose.rotation)
    return this
}

@CanIgnoreReturnValue
private fun SubspaceSemanticsNodeInteraction.withRotationInRoot(
    assertion: (Quaternion) -> Unit
): SubspaceSemanticsNodeInteraction {
    val node = fetchSemanticsNode("Failed to retrieve rotation of the node.")
    assertion.invoke(node.poseInRoot.rotation)
    return this
}

/**
 * Returns if this value is equal to the [reference], within a given [tolerance]. If the reference
 * value is [Float.NaN], [Float.POSITIVE_INFINITY] or [Float.NEGATIVE_INFINITY], this only returns
 * true if this value is exactly the same (tolerance is disregarded).
 */
private fun Dp.isWithinTolerance(reference: Dp, tolerance: Dp): Boolean {
    return when {
        reference.isUnspecified -> this.isUnspecified
        reference.value.isInfinite() -> this.value == reference.value
        else -> abs(this.value - reference.value) <= tolerance.value
    }
}

/**
 * Asserts that this value is equal to the given [expected] value.
 *
 * Performs the comparison with the given [tolerance] or the default one if none is provided. It is
 * recommended to use tolerance when comparing positions and size coming from the framework as there
 * can be rounding operation performed by individual layouts so the values can be slightly off from
 * the expected ones.
 *
 * @param expected The expected value to which this one should be equal to.
 * @param subject Used in the error message to identify which item this assertion failed on.
 * @param tolerance The tolerance within which the values should be treated as equal.
 * @throws AssertionError if comparison fails.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public fun Dp.assertIsEqualTo(expected: Dp, subject: String, tolerance: Dp = Dp(.5f)) {
    if (!isWithinTolerance(expected, tolerance)) {
        // Comparison failed, report the error in DPs
        throw AssertionError("Actual $subject is $this, expected $expected (tolerance: $tolerance)")
    }
}

/**
 * Asserts that this value is NOT equal to the given [expected] value.
 *
 * Performs the comparison with the given [tolerance] or the default one if none is provided.
 *
 * @param expected The expected value to which this one should NOT be equal to.
 * @param subject Used in the error message to identify which item this assertion failed on.
 * @param tolerance The tolerance within which the values should be treated as equal.
 * @throws AssertionError if comparison fails.
 */
private fun Dp.assertIsNotEqualTo(expected: Dp, subject: String, tolerance: Dp = Dp(.5f)) {
    if (isWithinTolerance(expected, tolerance)) {
        // Comparison failed, report the error in DPs
        throw AssertionError(
            "Actual $subject is $this, should NOT be $expected (tolerance: $tolerance)"
        )
    }
}

/**
 * Asserts that this value is greater than or equal to the given [expected] value.
 *
 * Performs the comparison with the given [tolerance] or the default one if none is provided. It is
 * recommended to use tolerance when comparing positions and size coming from the framework as there
 * can be rounding operation performed by individual layouts so the values can be slightly off from
 * the expected ones.
 *
 * @param expected The expected value to which this one should be greater than or equal to.
 * @param subject Used in the error message to identify which item this assertion failed on.
 * @param tolerance The tolerance within which the values should be treated as equal.
 * @throws AssertionError if comparison fails.
 */
private fun Dp.assertIsAtLeast(expected: Dp, subject: String, tolerance: Dp = Dp(.5f)) {
    if (!(isWithinTolerance(expected, tolerance) || (!isUnspecified && this > expected))) {
        // Comparison failed, report the error in DPs
        throw AssertionError(
            "Actual $subject is $this, expected at least $expected (tolerance: $tolerance)"
        )
    }
}

/** Converts a float to a [Dp] value. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public fun Float.toDp(): Dp {
    return Dp(this / Resources.getSystem().displayMetrics.density)
}

/** Converts an integer to a [Dp] value. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public fun Int.toDp(): Dp {
    return Dp(this.toFloat() / Resources.getSystem().displayMetrics.density)
}
