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

package androidx.xr.arcore

/**
 * Represents the type of face blend shape.
 *
 * A blend shape (also known as a morph target) is a deformation of a 3D model used to represent
 * different facial expressions or poses. Each `FaceBlendShapeType` corresponds to a specific facial
 * movement or deformation, and its intensity can be weighted to create a wide range of expressions.
 */
public class FaceBlendShapeType private constructor(private val value: Int) {

    public companion object {
        /** Left brow lowerer. */
        @JvmField public val BROW_LOWERER_LEFT: FaceBlendShapeType = FaceBlendShapeType(0)
        /** Right brow lowerer. */
        @JvmField public val BROW_LOWERER_RIGHT: FaceBlendShapeType = FaceBlendShapeType(1)
        /** Left cheek puff. */
        @JvmField public val CHEEK_PUFF_LEFT: FaceBlendShapeType = FaceBlendShapeType(2)
        /** Right cheek puff. */
        @JvmField public val CHEEK_PUFF_RIGHT: FaceBlendShapeType = FaceBlendShapeType(3)
        /** Left cheek raiser. */
        @JvmField public val CHEEK_RAISER_LEFT: FaceBlendShapeType = FaceBlendShapeType(4)
        /** Right cheek raiser. */
        @JvmField public val CHEEK_RAISER_RIGHT: FaceBlendShapeType = FaceBlendShapeType(5)
        /** Left cheek suck. */
        @JvmField public val CHEEK_SUCK_LEFT: FaceBlendShapeType = FaceBlendShapeType(6)
        /** Right cheek suck. */
        @JvmField public val CHEEK_SUCK_RIGHT: FaceBlendShapeType = FaceBlendShapeType(7)
        /** Bottom chin raiser. */
        @JvmField public val CHIN_RAISER_BOTTOM: FaceBlendShapeType = FaceBlendShapeType(8)
        /** Top chin raiser. */
        @JvmField public val CHIN_RAISER_TOP: FaceBlendShapeType = FaceBlendShapeType(9)
        /** Left dimpler. */
        @JvmField public val DIMPLER_LEFT: FaceBlendShapeType = FaceBlendShapeType(10)
        /** Right dimpler. */
        @JvmField public val DIMPLER_RIGHT: FaceBlendShapeType = FaceBlendShapeType(11)
        /** Left eyes closed. */
        @JvmField public val EYES_CLOSED_LEFT: FaceBlendShapeType = FaceBlendShapeType(12)
        /** Right eyes closed. */
        @JvmField public val EYES_CLOSED_RIGHT: FaceBlendShapeType = FaceBlendShapeType(13)
        /** Left eyes look down. */
        @JvmField public val EYES_LOOK_DOWN_LEFT: FaceBlendShapeType = FaceBlendShapeType(14)
        /** Right eyes look down. */
        @JvmField public val EYES_LOOK_DOWN_RIGHT: FaceBlendShapeType = FaceBlendShapeType(15)
        /** Left eyes look left. */
        @JvmField public val EYES_LOOK_LEFT_LEFT: FaceBlendShapeType = FaceBlendShapeType(16)
        /** Right eyes look left. */
        @JvmField public val EYES_LOOK_LEFT_RIGHT: FaceBlendShapeType = FaceBlendShapeType(17)
        /** Left eyes look right. */
        @JvmField public val EYES_LOOK_RIGHT_LEFT: FaceBlendShapeType = FaceBlendShapeType(18)
        /** Right eyes look right. */
        @JvmField public val EYES_LOOK_RIGHT_RIGHT: FaceBlendShapeType = FaceBlendShapeType(19)
        /** Left eyes look up. */
        @JvmField public val EYES_LOOK_UP_LEFT: FaceBlendShapeType = FaceBlendShapeType(20)
        /** Right eyes look up. */
        @JvmField public val EYES_LOOK_UP_RIGHT: FaceBlendShapeType = FaceBlendShapeType(21)
        /** Left inner brow raiser. */
        @JvmField public val INNER_BROW_RAISER_LEFT: FaceBlendShapeType = FaceBlendShapeType(22)
        /** Right inner brow raiser. */
        @JvmField public val INNER_BROW_RAISER_RIGHT: FaceBlendShapeType = FaceBlendShapeType(23)
        /** Jaw drop. */
        @JvmField public val JAW_DROP: FaceBlendShapeType = FaceBlendShapeType(24)
        /** Jaw sideways left. */
        @JvmField public val JAW_SIDEWAYS_LEFT: FaceBlendShapeType = FaceBlendShapeType(25)
        /** Jaw sideways right. */
        @JvmField public val JAW_SIDEWAYS_RIGHT: FaceBlendShapeType = FaceBlendShapeType(26)
        /** Jaw thrust. */
        @JvmField public val JAW_THRUST: FaceBlendShapeType = FaceBlendShapeType(27)
        /** Left lid tightener. */
        @JvmField public val LID_TIGHTENER_LEFT: FaceBlendShapeType = FaceBlendShapeType(28)
        /** Right lid tightener. */
        @JvmField public val LID_TIGHTENER_RIGHT: FaceBlendShapeType = FaceBlendShapeType(29)
        /** Left lip corner depressor. */
        @JvmField public val LIP_CORNER_DEPRESSOR_LEFT: FaceBlendShapeType = FaceBlendShapeType(30)
        /** Right lip corner depressor. */
        @JvmField public val LIP_CORNER_DEPRESSOR_RIGHT: FaceBlendShapeType = FaceBlendShapeType(31)
        /** Left lip corner puller. */
        @JvmField public val LIP_CORNER_PULLER_LEFT: FaceBlendShapeType = FaceBlendShapeType(32)
        /** Right lip corner puller. */
        @JvmField public val LIP_CORNER_PULLER_RIGHT: FaceBlendShapeType = FaceBlendShapeType(33)
        /** Left bottom lip funneler. */
        @JvmField public val LIP_FUNNELER_LEFT_BOTTOM: FaceBlendShapeType = FaceBlendShapeType(34)
        /** Left top lip funneler. */
        @JvmField public val LIP_FUNNELER_LEFT_TOP: FaceBlendShapeType = FaceBlendShapeType(35)
        /** Right bottom lip funneler. */
        @JvmField public val LIP_FUNNELER_RIGHT_BOTTOM: FaceBlendShapeType = FaceBlendShapeType(36)
        /** Right top lip funneler. */
        @JvmField public val LIP_FUNNELER_RIGHT_TOP: FaceBlendShapeType = FaceBlendShapeType(37)
        /** Left lip pressor. */
        @JvmField public val LIP_PRESSOR_LEFT: FaceBlendShapeType = FaceBlendShapeType(38)
        /** Right lip pressor. */
        @JvmField public val LIP_PRESSOR_RIGHT: FaceBlendShapeType = FaceBlendShapeType(39)
        /** Left lip pucker. */
        @JvmField public val LIP_PUCKER_LEFT: FaceBlendShapeType = FaceBlendShapeType(40)
        /** Right lip pucker. */
        @JvmField public val LIP_PUCKER_RIGHT: FaceBlendShapeType = FaceBlendShapeType(41)
        /** Left lip stretcher. */
        @JvmField public val LIP_STRETCHER_LEFT: FaceBlendShapeType = FaceBlendShapeType(42)
        /** Right lip stretcher. */
        @JvmField public val LIP_STRETCHER_RIGHT: FaceBlendShapeType = FaceBlendShapeType(43)
        /** Left bottom lip suck. */
        @JvmField public val LIP_SUCK_LEFT_BOTTOM: FaceBlendShapeType = FaceBlendShapeType(44)
        /** Left top lip suck. */
        @JvmField public val LIP_SUCK_LEFT_TOP: FaceBlendShapeType = FaceBlendShapeType(45)
        /** Right bottom lip suck. */
        @JvmField public val LIP_SUCK_RIGHT_BOTTOM: FaceBlendShapeType = FaceBlendShapeType(46)
        /** Right top lip suck. */
        @JvmField public val LIP_SUCK_RIGHT_TOP: FaceBlendShapeType = FaceBlendShapeType(47)
        /** Left lip tightener. */
        @JvmField public val LIP_TIGHTENER_LEFT: FaceBlendShapeType = FaceBlendShapeType(48)
        /** Right lip tightener. */
        @JvmField public val LIP_TIGHTENER_RIGHT: FaceBlendShapeType = FaceBlendShapeType(49)
        /** Lips toward. */
        @JvmField public val LIPS_TOWARD: FaceBlendShapeType = FaceBlendShapeType(50)
        /** Left lower lip depressor. */
        @JvmField public val LOWER_LIP_DEPRESSOR_LEFT: FaceBlendShapeType = FaceBlendShapeType(51)
        /** Right lower lip depressor. */
        @JvmField public val LOWER_LIP_DEPRESSOR_RIGHT: FaceBlendShapeType = FaceBlendShapeType(52)
        /** Mouth left. */
        @JvmField public val MOUTH_LEFT: FaceBlendShapeType = FaceBlendShapeType(53)
        /** Mouth right. */
        @JvmField public val MOUTH_RIGHT: FaceBlendShapeType = FaceBlendShapeType(54)
        /** Left nose wrinkler. */
        @JvmField public val NOSE_WRINKLER_LEFT: FaceBlendShapeType = FaceBlendShapeType(55)
        /** Right nose wrinkler. */
        @JvmField public val NOSE_WRINKLER_RIGHT: FaceBlendShapeType = FaceBlendShapeType(56)
        /** Left outer brow raiser. */
        @JvmField public val OUTER_BROW_RAISER_LEFT: FaceBlendShapeType = FaceBlendShapeType(57)
        /** Right outer brow raiser. */
        @JvmField public val OUTER_BROW_RAISER_RIGHT: FaceBlendShapeType = FaceBlendShapeType(58)
        /** Left upper lid raiser. */
        @JvmField public val UPPER_LID_RAISER_LEFT: FaceBlendShapeType = FaceBlendShapeType(59)
        /** Right upper lid raiser. */
        @JvmField public val UPPER_LID_RAISER_RIGHT: FaceBlendShapeType = FaceBlendShapeType(60)
        /** Left upper lip raiser. */
        @JvmField public val UPPER_LIP_RAISER_LEFT: FaceBlendShapeType = FaceBlendShapeType(61)
        /** Right upper lip raiser. */
        @JvmField public val UPPER_LIP_RAISER_RIGHT: FaceBlendShapeType = FaceBlendShapeType(62)
        /** Tongue out. */
        @JvmField public val TONGUE_OUT: FaceBlendShapeType = FaceBlendShapeType(63)
        /** Tongue left. */
        @JvmField public val TONGUE_LEFT: FaceBlendShapeType = FaceBlendShapeType(64)
        /** Tongue right. */
        @JvmField public val TONGUE_RIGHT: FaceBlendShapeType = FaceBlendShapeType(65)
        /** Tongue up. */
        @JvmField public val TONGUE_UP: FaceBlendShapeType = FaceBlendShapeType(66)
        /** Tongue down. */
        @JvmField public val TONGUE_DOWN: FaceBlendShapeType = FaceBlendShapeType(67)
    }
}
