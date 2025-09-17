/*
 * Copyright (C) 2025 The Android Open Source Project
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

package androidx.compose.remote.frontend.state

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.utilities.MatrixOperations
import androidx.compose.remote.frontend.capture.RemoteComposeCreationState

/**
 * Represents a 3x3 transformation matrix.
 *
 * @property hasConstantValue Indicates whether the matrix value is constant
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteMatrix3x3
internal constructor(
    public override val hasConstantValue: Boolean,
    private val idProvider: (creationState: RemoteComposeCreationState) -> Int,
) : BaseRemoteState {

    /**
     * Creates a new [RemoteMatrix3x3] that represents the multiplication of this matrix by another.
     *
     * @param v The [RemoteMatrix3x3] to multiply with this one (this * v).
     * @return A new [RemoteMatrix3x3] representing the multiplication.
     */
    public operator fun times(v: RemoteMatrix3x3): RemoteMatrix3x3 =
        RemoteMatrix3x3(
            true,
            { creationState ->
                Utils.idFromNan(
                    creationState.document.matrixExpression(
                        getFloatIdForCreationState(creationState),
                        v.getFloatIdForCreationState(creationState),
                        MatrixOperations.MUL,
                    )
                )
            },
        )

    public override fun writeToDocument(creationState: RemoteComposeCreationState): Int =
        idProvider(creationState)

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public companion object {
        /** Creates a [RemoteMatrix3x3] representing an identity matrix. */
        public fun createIdentity(): RemoteMatrix3x3 =
            RemoteMatrix3x3(
                true,
                { creationState ->
                    Utils.idFromNan(
                        creationState.document.matrixExpression(MatrixOperations.IDENTITY)
                    )
                },
            )

        /**
         * Creates a [RemoteMatrix3x3] that rotates around the Z-axis.
         *
         * @param angle The angle of rotation.
         */
        public fun createRotate(angle: Number): RemoteMatrix3x3 =
            RemoteMatrix3x3(
                angle.hasConstantValue,
                { creationState ->
                    Utils.idFromNan(
                        creationState.document.matrixExpression(
                            angle.getFloatIdForCreationState(creationState),
                            MatrixOperations.ROT_Z,
                        )
                    )
                },
            )

        /**
         * Creates a [RemoteMatrix3x3] that translates along the X-axis.
         *
         * @param x The distance to translate along the X-axis.
         */
        public fun createTranslateX(x: Number): RemoteMatrix3x3 =
            RemoteMatrix3x3(
                x.hasConstantValue,
                { creationState ->
                    Utils.idFromNan(
                        creationState.document.matrixExpression(
                            x.getFloatIdForCreationState(creationState),
                            MatrixOperations.TRANSLATE_X,
                        )
                    )
                },
            )

        /**
         * Creates a [RemoteMatrix3x3] that translates along the Y-axis.
         *
         * @param y The distance to translate along the Y-axis.
         */
        public fun createTranslateY(y: Number): RemoteMatrix3x3 =
            RemoteMatrix3x3(
                y.hasConstantValue,
                { creationState ->
                    Utils.idFromNan(
                        creationState.document.matrixExpression(
                            y.getFloatIdForCreationState(creationState),
                            MatrixOperations.TRANSLATE_Y,
                        )
                    )
                },
            )

        /**
         * Creates a [RemoteMatrix3x3] that scales along the X-axis.
         *
         * @param scale The scaling factor.
         */
        public fun createScaleX(scale: Number): RemoteMatrix3x3 =
            RemoteMatrix3x3(
                scale.hasConstantValue,
                { creationState ->
                    Utils.idFromNan(
                        creationState.document.matrixExpression(
                            scale.getFloatIdForCreationState(creationState),
                            MatrixOperations.SCALE_X,
                        )
                    )
                },
            )

        /**
         * Creates a [RemoteMatrix3x3] that scales along the Y-axis.
         *
         * @param scale The scaling factor.
         */
        public fun createScaleY(scale: Number): RemoteMatrix3x3 =
            RemoteMatrix3x3(
                scale.hasConstantValue,
                { creationState ->
                    Utils.idFromNan(
                        creationState.document.matrixExpression(
                            scale.getFloatIdForCreationState(creationState),
                            MatrixOperations.SCALE_Y,
                        )
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
            angle: Number,
            centerX: Number,
            centerY: Number,
        ): RemoteMatrix3x3 =
            RemoteMatrix3x3(
                angle.hasConstantValue && centerX.hasConstantValue && centerY.hasConstantValue,
                { creationState ->
                    Utils.idFromNan(
                        creationState.document.matrixExpression(
                            angle.getFloatIdForCreationState(creationState),
                            centerX.getFloatIdForCreationState(creationState),
                            centerY.getFloatIdForCreationState(creationState),
                            MatrixOperations.ROT_PZ,
                        )
                    )
                },
            )
    }
}
