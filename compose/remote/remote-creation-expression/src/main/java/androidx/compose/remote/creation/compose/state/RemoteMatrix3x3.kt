/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.remote.creation.compose.state

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.utilities.MatrixOperations
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.runtime.Stable
import kotlin.reflect.KClass

/**
 * Represents a 3x3 transformation matrix.
 *
 * Comparing [RemoteMatrix3x3] values with `==` checks structural equality (matching expression
 * DAGs); see [RemoteState].
 */
@Stable
public sealed interface RemoteMatrix3x3 : RemoteState<Any> {
    /**
     * Creates a new [RemoteMatrix3x3] that represents the multiplication of this matrix by another.
     *
     * @param other The [RemoteMatrix3x3] to multiply with this one (this * other).
     * @return A new [RemoteMatrix3x3] representing the multiplication.
     */
    public operator fun times(other: RemoteMatrix3x3): RemoteMatrix3x3

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public companion object {
        /** Creates a [RemoteMatrix3x3] representing an identity matrix. */
        public fun createIdentity(): RemoteMatrix3x3 =
            RemoteMatrix3x3Impl(
                cacheKey = RemoteOperationCacheKey.create(MatrixOperationKey.IDENTITY),
                arrayProvider = { _ ->
                    // Note there is an implicit MatrixOperations.IDENTITY for the first entry,
                    // see MatrixOperations#eval.
                    floatArrayOf()
                },
            )

        /**
         * Creates a [RemoteMatrix3x3] that rotates around the Z-axis.
         *
         * @param angle The angle of rotation.
         */
        public fun createRotate(angle: RemoteFloat): RemoteMatrix3x3 =
            RemoteMatrix3x3Impl(
                cacheKey = RemoteOperationCacheKey.create(MatrixOperationKey.ROTATE, angle),
                arrayProvider = { creationState ->
                    floatArrayOf(
                        creationState.getFloatId(angle),
                        MatrixOperations.ROT_Z,
                    )
                },
            )

        /**
         * Creates a [RemoteMatrix3x3] that translates along the X-axis.
         *
         * @param x The distance to translate along the X-axis.
         */
        public fun createTranslateX(x: RemoteFloat): RemoteMatrix3x3 =
            RemoteMatrix3x3Impl(
                cacheKey = RemoteOperationCacheKey.create(MatrixOperationKey.TRANSLATE_X, x),
                arrayProvider = { creationState ->
                    floatArrayOf(
                        creationState.getFloatId(x),
                        MatrixOperations.TRANSLATE_X,
                    )
                },
            )

        /**
         * Creates a [RemoteMatrix3x3] that translates along the Y-axis.
         *
         * @param y The distance to translate along the Y-axis.
         */
        public fun createTranslateY(y: RemoteFloat): RemoteMatrix3x3 =
            RemoteMatrix3x3Impl(
                cacheKey = RemoteOperationCacheKey.create(MatrixOperationKey.TRANSLATE_Y, y),
                arrayProvider = { creationState ->
                    floatArrayOf(
                        creationState.getFloatId(y),
                        MatrixOperations.TRANSLATE_Y,
                    )
                },
            )

        /**
         * Creates a [RemoteMatrix3x3] that translates along the X-axis and the Y-axis.
         *
         * @param x The distance to translate along the X-axis.
         * @param y The distance to translate along the Y-axis.
         */
        public fun createTranslateXy(x: RemoteFloat, y: RemoteFloat): RemoteMatrix3x3 =
            RemoteMatrix3x3Impl(
                cacheKey = RemoteOperationCacheKey.create(MatrixOperationKey.TRANSLATE_XY, x, y),
                arrayProvider = { creationState ->
                    floatArrayOf(
                        creationState.getFloatId(x),
                        creationState.getFloatId(y),
                        MatrixOperations.TRANSLATE2,
                    )
                },
            )

        /**
         * Creates a [RemoteMatrix3x3] that scales along the X-axis.
         *
         * @param scale The scaling factor.
         */
        public fun createScaleX(scale: RemoteFloat): RemoteMatrix3x3 =
            RemoteMatrix3x3Impl(
                cacheKey = RemoteOperationCacheKey.create(MatrixOperationKey.SCALE_X, scale),
                arrayProvider = { creationState ->
                    floatArrayOf(
                        creationState.getFloatId(scale),
                        MatrixOperations.SCALE_X,
                    )
                },
            )

        /**
         * Creates a [RemoteMatrix3x3] that scales along the Y-axis.
         *
         * @param scale The scaling factor.
         */
        public fun createScaleY(scale: RemoteFloat): RemoteMatrix3x3 =
            RemoteMatrix3x3Impl(
                cacheKey = RemoteOperationCacheKey.create(MatrixOperationKey.SCALE_Y, scale),
                arrayProvider = { creationState ->
                    floatArrayOf(
                        creationState.getFloatId(scale),
                        MatrixOperations.SCALE_Y,
                    )
                },
            )

        /**
         * Creates a [RemoteMatrix3x3] that rotates around a pivot point on the Z-plane.
         *
         * @param angle The angle of rotation.
         * @param centerX The X-coordinate of the pivot point.
         * @param centerY The Y-coordinate of the pivot point.
         */
        public fun createRotationAround(
            angle: RemoteFloat,
            centerX: RemoteFloat,
            centerY: RemoteFloat,
        ): RemoteMatrix3x3 =
            RemoteMatrix3x3Impl(
                cacheKey =
                    RemoteOperationCacheKey.create(
                        MatrixOperationKey.ROTATION_AROUND,
                        angle,
                        centerX,
                        centerY,
                    ),
                arrayProvider = { creationState ->
                    floatArrayOf(
                        creationState.getFloatId(angle),
                        creationState.getFloatId(centerX),
                        creationState.getFloatId(centerY),
                        MatrixOperations.ROT_PZ,
                    )
                },
            )
    }
}

/** Returns `true` if this [RemoteMatrix3x3] is the identity matrix. */
@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public val RemoteMatrix3x3.isIdentity: Boolean
    get() =
        when (this) {
            is RemoteMatrix3x3Impl -> isIdentity
        }

private val RemoteMatrix3x3.arrayProvider: (creationState: RemoteComposeCreationState) -> FloatArray
    get() = (this as RemoteMatrix3x3Impl).arrayProvider

private class RemoteMatrix3x3Impl(
    val arrayProvider: (creationState: RemoteComposeCreationState) -> FloatArray,
    cacheKey: RemoteStateCacheKey,
) : BaseRemoteState<Any>(cacheKey), RemoteMatrix3x3 {
    override val equalityType: KClass<*>
        get() = RemoteMatrix3x3::class

    val isIdentity: Boolean
        get() = (cacheKey as? RemoteOperationCacheKey)?.op == MatrixOperationKey.IDENTITY

    override val constantValueOrNull: Any?
        get() = null

    override fun writeToDocument(creationState: RemoteComposeCreationState): Int =
        Utils.idFromNan(creationState.document.matrixExpression(*arrayProvider(creationState)))

    override fun times(other: RemoteMatrix3x3): RemoteMatrix3x3 {
        val key = RemoteOperationCacheKey.create(MatrixOperationKey.MUL, this, other)
        return RemoteMatrix3x3Impl(
            cacheKey = key,
            arrayProvider = { creationState ->
                floatArrayOf(
                    // Note there is an implicit MatrixOperations.IDENTITY for the first entry,
                    // see MatrixOperations#eval.
                    *this@RemoteMatrix3x3Impl.arrayProvider(creationState),
                    MatrixOperations.IDENTITY,
                    *other.arrayProvider(creationState),
                    MatrixOperations.MUL,
                )
            },
        )
    }
}

private enum class MatrixOperationKey(override val precedence: Int = 100) : RemoteOperation {
    IDENTITY {
        override fun toDebugString(args: List<RemoteStateCacheKey>): String = "identity()"
    },
    ROTATE {
        override fun toDebugString(args: List<RemoteStateCacheKey>): String =
            "rotate(${args[0].toDebugString()})"
    },
    TRANSLATE_X {
        override fun toDebugString(args: List<RemoteStateCacheKey>): String =
            "translateX(${args[0].toDebugString()})"
    },
    TRANSLATE_Y {
        override fun toDebugString(args: List<RemoteStateCacheKey>): String =
            "translateY(${args[0].toDebugString()})"
    },
    TRANSLATE_XY {
        override fun toDebugString(args: List<RemoteStateCacheKey>): String =
            "translate(${args[0].toDebugString()}, ${args[1].toDebugString()})"
    },
    SCALE_X {
        override fun toDebugString(args: List<RemoteStateCacheKey>): String =
            "scaleX(${args[0].toDebugString()})"
    },
    SCALE_Y {
        override fun toDebugString(args: List<RemoteStateCacheKey>): String =
            "scaleY(${args[0].toDebugString()})"
    },
    ROTATION_AROUND {
        override fun toDebugString(args: List<RemoteStateCacheKey>): String {
            val params = args.joinToDebugString()
            return "rotateAround($params)"
        }
    },
    MUL(3) {
        override fun toDebugString(args: List<RemoteStateCacheKey>): String =
            args.formatOp("*", precedence)
    };

    override fun reconstruct(args: List<RemoteState<*>>): RemoteState<*> {
        return when (this) {
            IDENTITY -> RemoteMatrix3x3.createIdentity()
            ROTATE -> RemoteMatrix3x3.createRotate(args[0] as RemoteFloat)
            TRANSLATE_X -> RemoteMatrix3x3.createTranslateX(args[0] as RemoteFloat)
            TRANSLATE_Y -> RemoteMatrix3x3.createTranslateY(args[0] as RemoteFloat)
            TRANSLATE_XY ->
                RemoteMatrix3x3.createTranslateXy(args[0] as RemoteFloat, args[1] as RemoteFloat)
            SCALE_X -> RemoteMatrix3x3.createScaleX(args[0] as RemoteFloat)
            SCALE_Y -> RemoteMatrix3x3.createScaleY(args[0] as RemoteFloat)
            ROTATION_AROUND ->
                RemoteMatrix3x3.createRotationAround(
                    args[0] as RemoteFloat,
                    args[1] as RemoteFloat,
                    args[2] as RemoteFloat,
                )
            MUL -> (args[0] as RemoteMatrix3x3) * (args[1] as RemoteMatrix3x3)
        }
    }
}
