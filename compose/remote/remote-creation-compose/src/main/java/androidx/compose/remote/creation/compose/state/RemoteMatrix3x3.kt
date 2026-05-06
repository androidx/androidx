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
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.compose.remote.creation.compose.state

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.utilities.MatrixOperations
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState

/** Represents a 3x3 transformation matrix. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteMatrix3x3
internal constructor(
    private val arrayProvider: (creationState: RemoteComposeCreationState) -> FloatArray,
    override val cacheKey: RemoteStateCacheKey,
) : BaseRemoteState<Any>() {
    internal enum class OperationKey {
        IDENTITY,
        ROTATE,
        TRANSLATE_X,
        TRANSLATE_Y,
        TRANSLATE_XY,
        SCALE_X,
        SCALE_Y,
        ROTATION_AROUND,
        MUL,
    }

    override val constantValueOrNull: Any?
        get() = null

    /**
     * Creates a new [RemoteMatrix3x3] that represents the multiplication of this matrix by another.
     *
     * @param v The [RemoteMatrix3x3] to multiply with this one (this * v).
     * @return A new [RemoteMatrix3x3] representing the multiplication.
     */
    public operator fun times(v: RemoteMatrix3x3): RemoteMatrix3x3 {
        val key = RemoteOperationCacheKey.create(OperationKey.MUL, this, v)
        return RemoteMatrix3x3(
            cacheKey = key,
            arrayProvider = { creationState ->
                floatArrayOf(
                    // Note there is an implicit MatrixOperations.IDENTITY for the first entry,
                    // see MatrixOperations#eval.
                    *this@RemoteMatrix3x3.arrayProvider(creationState),
                    MatrixOperations.IDENTITY,
                    *v.arrayProvider(creationState),
                    MatrixOperations.MUL,
                )
            },
        )
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public override fun writeToDocument(creationState: RemoteComposeCreationState): Int =
        Utils.idFromNan(creationState.document.matrixExpression(*arrayProvider(creationState)))

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public companion object {
        /** Creates a [RemoteMatrix3x3] representing an identity matrix. */
        public fun createIdentity(): RemoteMatrix3x3 =
            RemoteMatrix3x3(
                cacheKey = RemoteOperationCacheKey.create(OperationKey.IDENTITY),
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
            RemoteMatrix3x3(
                cacheKey = RemoteOperationCacheKey.create(OperationKey.ROTATE, angle),
                arrayProvider = { creationState ->
                    floatArrayOf(
                        angle.getFloatIdForCreationState(creationState),
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
            RemoteMatrix3x3(
                cacheKey = RemoteOperationCacheKey.create(OperationKey.TRANSLATE_X, x),
                arrayProvider = { creationState ->
                    floatArrayOf(
                        x.getFloatIdForCreationState(creationState),
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
            RemoteMatrix3x3(
                cacheKey = RemoteOperationCacheKey.create(OperationKey.TRANSLATE_Y, y),
                arrayProvider = { creationState ->
                    floatArrayOf(
                        y.getFloatIdForCreationState(creationState),
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
            RemoteMatrix3x3(
                cacheKey = RemoteOperationCacheKey.create(OperationKey.TRANSLATE_XY, x, y),
                arrayProvider = { creationState ->
                    floatArrayOf(
                        x.getFloatIdForCreationState(creationState),
                        y.getFloatIdForCreationState(creationState),
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
            RemoteMatrix3x3(
                cacheKey = RemoteOperationCacheKey.create(OperationKey.SCALE_X, scale),
                arrayProvider = { creationState ->
                    floatArrayOf(
                        scale.getFloatIdForCreationState(creationState),
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
            RemoteMatrix3x3(
                cacheKey = RemoteOperationCacheKey.create(OperationKey.SCALE_Y, scale),
                arrayProvider = { creationState ->
                    floatArrayOf(
                        scale.getFloatIdForCreationState(creationState),
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
            RemoteMatrix3x3(
                cacheKey =
                    RemoteOperationCacheKey.create(
                        OperationKey.ROTATION_AROUND,
                        angle,
                        centerX,
                        centerY,
                    ),
                arrayProvider = { creationState ->
                    floatArrayOf(
                        angle.getFloatIdForCreationState(creationState),
                        centerX.getFloatIdForCreationState(creationState),
                        centerY.getFloatIdForCreationState(creationState),
                        MatrixOperations.ROT_PZ,
                    )
                },
            )
    }
}
