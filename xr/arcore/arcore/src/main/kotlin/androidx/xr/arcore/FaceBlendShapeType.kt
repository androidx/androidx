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
        @JvmField
        public val FACE_BLEND_SHAPE_TYPE_BROW_LOWERER_LEFT: FaceBlendShapeType =
            FaceBlendShapeType(0)
        /** Right brow lowerer. */
        @JvmField
        public val FACE_BLEND_SHAPE_TYPE_BROW_LOWERER_RIGHT: FaceBlendShapeType =
            FaceBlendShapeType(1)
        /** Left cheek puff. */
        @JvmField
        public val FACE_BLEND_SHAPE_TYPE_CHEEK_PUFF_LEFT: FaceBlendShapeType = FaceBlendShapeType(2)
        /** Right cheek puff. */
        @JvmField
        public val FACE_BLEND_SHAPE_TYPE_CHEEK_PUFF_RIGHT: FaceBlendShapeType =
            FaceBlendShapeType(3)
        /** Left cheek raiser. */
        @JvmField
        public val FACE_BLEND_SHAPE_TYPE_CHEEK_RAISER_LEFT: FaceBlendShapeType =
            FaceBlendShapeType(4)
        /** Right cheek raiser. */
        @JvmField
        public val FACE_BLEND_SHAPE_TYPE_CHEEK_RAISER_RIGHT: FaceBlendShapeType =
            FaceBlendShapeType(5)
        /** Left cheek suck. */
        @JvmField
        public val FACE_BLEND_SHAPE_TYPE_CHEEK_SUCK_LEFT: FaceBlendShapeType = FaceBlendShapeType(6)
        /** Right cheek suck. */
        @JvmField
        public val FACE_BLEND_SHAPE_TYPE_CHEEK_SUCK_RIGHT: FaceBlendShapeType =
            FaceBlendShapeType(7)
        /** Bottom chin raiser. */
        @JvmField
        public val FACE_BLEND_SHAPE_TYPE_CHIN_RAISER_BOTTOM: FaceBlendShapeType =
            FaceBlendShapeType(8)
        /** Top chin raiser. */
        @JvmField
        public val FACE_BLEND_SHAPE_TYPE_CHIN_RAISER_TOP: FaceBlendShapeType = FaceBlendShapeType(9)
        /** Left dimpler. */
        @JvmField
        public val FACE_BLEND_SHAPE_TYPE_DIMPLER_LEFT: FaceBlendShapeType = FaceBlendShapeType(10)
        /** Right dimpler. */
        @JvmField
        public val FACE_BLEND_SHAPE_TYPE_DIMPLER_RIGHT: FaceBlendShapeType = FaceBlendShapeType(11)
        /** Left eyes closed. */
        @JvmField
        public val FACE_BLEND_SHAPE_TYPE_EYES_CLOSED_LEFT: FaceBlendShapeType =
            FaceBlendShapeType(12)
        /** Right eyes closed. */
        @JvmField
        public val FACE_BLEND_SHAPE_TYPE_EYES_CLOSED_RIGHT: FaceBlendShapeType =
            FaceBlendShapeType(13)
        /** Left eyes look down. */
        @JvmField
        public val FACE_BLEND_SHAPE_TYPE_EYES_LOOK_DOWN_LEFT: FaceBlendShapeType =
            FaceBlendShapeType(14)
        /** Right eyes look down. */
        @JvmField
        public val FACE_BLEND_SHAPE_TYPE_EYES_LOOK_DOWN_RIGHT: FaceBlendShapeType =
            FaceBlendShapeType(15)
        /** Left eyes look left. */
        @JvmField
        public val FACE_BLEND_SHAPE_TYPE_EYES_LOOK_LEFT_LEFT: FaceBlendShapeType =
            FaceBlendShapeType(16)
        /** Right eyes look left. */
        @JvmField
        public val FACE_BLEND_SHAPE_TYPE_EYES_LOOK_LEFT_RIGHT: FaceBlendShapeType =
            FaceBlendShapeType(17)
        /** Left eyes look right. */
        @JvmField
        public val FACE_BLEND_SHAPE_TYPE_EYES_LOOK_RIGHT_LEFT: FaceBlendShapeType =
            FaceBlendShapeType(18)
        /** Right eyes look right. */
        @JvmField
        public val FACE_BLEND_SHAPE_TYPE_EYES_LOOK_RIGHT_RIGHT: FaceBlendShapeType =
            FaceBlendShapeType(19)
        /** Left eyes look up. */
        @JvmField
        public val FACE_BLEND_SHAPE_TYPE_EYES_LOOK_UP_LEFT: FaceBlendShapeType =
            FaceBlendShapeType(20)
        /** Right eyes look up. */
        @JvmField
        public val FACE_BLEND_SHAPE_TYPE_EYES_LOOK_UP_RIGHT: FaceBlendShapeType =
            FaceBlendShapeType(21)
        /** Left inner brow raiser. */
        @JvmField
        public val FACE_BLEND_SHAPE_TYPE_INNER_BROW_RAISER_LEFT: FaceBlendShapeType =
            FaceBlendShapeType(22)
        /** Right inner brow raiser. */
        @JvmField
        public val FACE_BLEND_SHAPE_TYPE_INNER_BROW_RAISER_RIGHT: FaceBlendShapeType =
            FaceBlendShapeType(23)
        /** Jaw drop. */
        @JvmField
        public val FACE_BLEND_SHAPE_TYPE_JAW_DROP: FaceBlendShapeType = FaceBlendShapeType(24)
        /** Jaw sideways left. */
        @JvmField
        public val FACE_BLEND_SHAPE_TYPE_JAW_SIDEWAYS_LEFT: FaceBlendShapeType =
            FaceBlendShapeType(25)
        /** Jaw sideways right. */
        @JvmField
        public val FACE_BLEND_SHAPE_TYPE_JAW_SIDEWAYS_RIGHT: FaceBlendShapeType =
            FaceBlendShapeType(26)
        /** Jaw thrust. */
        @JvmField
        public val FACE_BLEND_SHAPE_TYPE_JAW_THRUST: FaceBlendShapeType = FaceBlendShapeType(27)
        /** Left lid tightener. */
        @JvmField
        public val FACE_BLEND_SHAPE_TYPE_LID_TIGHTENER_LEFT: FaceBlendShapeType =
            FaceBlendShapeType(28)
        /** Right lid tightener. */
        @JvmField
        public val FACE_BLEND_SHAPE_TYPE_LID_TIGHTENER_RIGHT: FaceBlendShapeType =
            FaceBlendShapeType(29)
        /** Left lip corner depressor. */
        @JvmField
        public val FACE_BLEND_SHAPE_TYPE_LIP_CORNER_DEPRESSOR_LEFT: FaceBlendShapeType =
            FaceBlendShapeType(30)
        /** Right lip corner depressor. */
        @JvmField
        public val FACE_BLEND_SHAPE_TYPE_LIP_CORNER_DEPRESSOR_RIGHT: FaceBlendShapeType =
            FaceBlendShapeType(31)
        /** Left lip corner puller. */
        @JvmField
        public val FACE_BLEND_SHAPE_TYPE_LIP_CORNER_PULLER_LEFT: FaceBlendShapeType =
            FaceBlendShapeType(32)
        /** Right lip corner puller. */
        @JvmField
        public val FACE_BLEND_SHAPE_TYPE_LIP_CORNER_PULLER_RIGHT: FaceBlendShapeType =
            FaceBlendShapeType(33)
        /** Left bottom lip funneler. */
        @JvmField
        public val FACE_BLEND_SHAPE_TYPE_LIP_FUNNELER_LEFT_BOTTOM: FaceBlendShapeType =
            FaceBlendShapeType(34)
        /** Left top lip funneler. */
        @JvmField
        public val FACE_BLEND_SHAPE_TYPE_LIP_FUNNELER_LEFT_TOP: FaceBlendShapeType =
            FaceBlendShapeType(35)
        /** Right bottom lip funneler. */
        @JvmField
        public val FACE_BLEND_SHAPE_TYPE_LIP_FUNNELER_RIGHT_BOTTOM: FaceBlendShapeType =
            FaceBlendShapeType(36)
        /** Right top lip funneler. */
        @JvmField
        public val FACE_BLEND_SHAPE_TYPE_LIP_FUNNELER_RIGHT_TOP: FaceBlendShapeType =
            FaceBlendShapeType(37)
        /** Left lip pressor. */
        @JvmField
        public val FACE_BLEND_SHAPE_TYPE_LIP_PRESSOR_LEFT: FaceBlendShapeType =
            FaceBlendShapeType(38)
        /** Right lip pressor. */
        @JvmField
        public val FACE_BLEND_SHAPE_TYPE_LIP_PRESSOR_RIGHT: FaceBlendShapeType =
            FaceBlendShapeType(39)
        /** Left lip pucker. */
        @JvmField
        public val FACE_BLEND_SHAPE_TYPE_LIP_PUCKER_LEFT: FaceBlendShapeType =
            FaceBlendShapeType(40)
        /** Right lip pucker. */
        @JvmField
        public val FACE_BLEND_SHAPE_TYPE_LIP_PUCKER_RIGHT: FaceBlendShapeType =
            FaceBlendShapeType(41)
        /** Left lip stretcher. */
        @JvmField
        public val FACE_BLEND_SHAPE_TYPE_LIP_STRETCHER_LEFT: FaceBlendShapeType =
            FaceBlendShapeType(42)
        /** Right lip stretcher. */
        @JvmField
        public val FACE_BLEND_SHAPE_TYPE_LIP_STRETCHER_RIGHT: FaceBlendShapeType =
            FaceBlendShapeType(43)
        /** Left bottom lip suck. */
        @JvmField
        public val FACE_BLEND_SHAPE_TYPE_LIP_SUCK_LEFT_BOTTOM: FaceBlendShapeType =
            FaceBlendShapeType(44)
        /** Left top lip suck. */
        @JvmField
        public val FACE_BLEND_SHAPE_TYPE_LIP_SUCK_LEFT_TOP: FaceBlendShapeType =
            FaceBlendShapeType(45)
        /** Right bottom lip suck. */
        @JvmField
        public val FACE_BLEND_SHAPE_TYPE_LIP_SUCK_RIGHT_BOTTOM: FaceBlendShapeType =
            FaceBlendShapeType(46)
        /** Right top lip suck. */
        @JvmField
        public val FACE_BLEND_SHAPE_TYPE_LIP_SUCK_RIGHT_TOP: FaceBlendShapeType =
            FaceBlendShapeType(47)
        /** Left lip tightener. */
        @JvmField
        public val FACE_BLEND_SHAPE_TYPE_LIP_TIGHTENER_LEFT: FaceBlendShapeType =
            FaceBlendShapeType(48)
        /** Right lip tightener. */
        @JvmField
        public val FACE_BLEND_SHAPE_TYPE_LIP_TIGHTENER_RIGHT: FaceBlendShapeType =
            FaceBlendShapeType(49)
        /** Lips toward. */
        @JvmField
        public val FACE_BLEND_SHAPE_TYPE_LIPS_TOWARD: FaceBlendShapeType = FaceBlendShapeType(50)
        /** Left lower lip depressor. */
        @JvmField
        public val FACE_BLEND_SHAPE_TYPE_LOWER_LIP_DEPRESSOR_LEFT: FaceBlendShapeType =
            FaceBlendShapeType(51)
        /** Right lower lip depressor. */
        @JvmField
        public val FACE_BLEND_SHAPE_TYPE_LOWER_LIP_DEPRESSOR_RIGHT: FaceBlendShapeType =
            FaceBlendShapeType(52)
        /** Mouth left. */
        @JvmField
        public val FACE_BLEND_SHAPE_TYPE_MOUTH_LEFT: FaceBlendShapeType = FaceBlendShapeType(53)
        /** Mouth right. */
        @JvmField
        public val FACE_BLEND_SHAPE_TYPE_MOUTH_RIGHT: FaceBlendShapeType = FaceBlendShapeType(54)
        /** Left nose wrinkler. */
        @JvmField
        public val FACE_BLEND_SHAPE_TYPE_NOSE_WRINKLER_LEFT: FaceBlendShapeType =
            FaceBlendShapeType(55)
        /** Right nose wrinkler. */
        @JvmField
        public val FACE_BLEND_SHAPE_TYPE_NOSE_WRINKLER_RIGHT: FaceBlendShapeType =
            FaceBlendShapeType(56)
        /** Left outer brow raiser. */
        @JvmField
        public val FACE_BLEND_SHAPE_TYPE_OUTER_BROW_RAISER_LEFT: FaceBlendShapeType =
            FaceBlendShapeType(57)
        /** Right outer brow raiser. */
        @JvmField
        public val FACE_BLEND_SHAPE_TYPE_OUTER_BROW_RAISER_RIGHT: FaceBlendShapeType =
            FaceBlendShapeType(58)
        /** Left upper lid raiser. */
        @JvmField
        public val FACE_BLEND_SHAPE_TYPE_UPPER_LID_RAISER_LEFT: FaceBlendShapeType =
            FaceBlendShapeType(59)
        /** Right upper lid raiser. */
        @JvmField
        public val FACE_BLEND_SHAPE_TYPE_UPPER_LID_RAISER_RIGHT: FaceBlendShapeType =
            FaceBlendShapeType(60)
        /** Left upper lip raiser. */
        @JvmField
        public val FACE_BLEND_SHAPE_TYPE_UPPER_LIP_RAISER_LEFT: FaceBlendShapeType =
            FaceBlendShapeType(61)
        /** Right upper lip raiser. */
        @JvmField
        public val FACE_BLEND_SHAPE_TYPE_UPPER_LIP_RAISER_RIGHT: FaceBlendShapeType =
            FaceBlendShapeType(62)
        /** Tongue out. */
        @JvmField
        public val FACE_BLEND_SHAPE_TYPE_TONGUE_OUT: FaceBlendShapeType = FaceBlendShapeType(63)
        /** Tongue left. */
        @JvmField
        public val FACE_BLEND_SHAPE_TYPE_TONGUE_LEFT: FaceBlendShapeType = FaceBlendShapeType(64)
        /** Tongue right. */
        @JvmField
        public val FACE_BLEND_SHAPE_TYPE_TONGUE_RIGHT: FaceBlendShapeType = FaceBlendShapeType(65)
        /** Tongue up. */
        @JvmField
        public val FACE_BLEND_SHAPE_TYPE_TONGUE_UP: FaceBlendShapeType = FaceBlendShapeType(66)
        /** Tongue down. */
        @JvmField
        public val FACE_BLEND_SHAPE_TYPE_TONGUE_DOWN: FaceBlendShapeType = FaceBlendShapeType(67)
    }

    override fun toString(): String {
        return when (value) {
            0 -> "BROW_LOWERER_LEFT"
            1 -> "BROW_LOWERER_RIGHT"
            2 -> "CHEEK_PUFF_LEFT"
            3 -> "CHEEK_PUFF_RIGHT"
            4 -> "CHEEK_RAISER_LEFT"
            5 -> "CHEEK_RAISER_RIGHT"
            6 -> "CHEEK_SUCK_LEFT"
            7 -> "CHEEK_SUCK_RIGHT"
            8 -> "CHIN_RAISER_BOTTOM"
            9 -> "CHIN_RAISER_TOP"
            10 -> "DIMPLER_LEFT"
            11 -> "DIMPLER_RIGHT"
            12 -> "EYES_CLOSED_LEFT"
            13 -> "EYES_CLOSED_RIGHT"
            14 -> "EYES_LOOK_DOWN_LEFT"
            15 -> "EYES_LOOK_DOWN_RIGHT"
            16 -> "EYES_LOOK_LEFT_LEFT"
            17 -> "EYES_LOOK_LEFT_RIGHT"
            18 -> "EYES_LOOK_RIGHT_LEFT"
            19 -> "EYES_LOOK_RIGHT_RIGHT"
            20 -> "EYES_LOOK_UP_LEFT"
            21 -> "EYES_LOOK_UP_RIGHT"
            22 -> "INNER_BROW_RAISER_LEFT"
            23 -> "INNER_BROW_RAISER_RIGHT"
            24 -> "JAW_DROP"
            25 -> "JAW_SIDEWAYS_LEFT"
            26 -> "JAW_SIDEWAYS_RIGHT"
            27 -> "JAW_THRUST"
            28 -> "LID_TIGHTENER_LEFT"
            29 -> "LID_TIGHTENER_RIGHT"
            30 -> "LIP_CORNER_DEPRESSOR_LEFT"
            31 -> "LIP_CORNER_DEPRESSOR_RIGHT"
            32 -> "LIP_CORNER_PULLER_LEFT"
            33 -> "LIP_CORNER_PULLER_RIGHT"
            34 -> "LIP_FUNNELER_LEFT_BOTTOM"
            35 -> "LIP_FUNNELER_LEFT_TOP"
            36 -> "LIP_FUNNELER_RIGHT_BOTTOM"
            37 -> "LIP_FUNNELER_RIGHT_TOP"
            38 -> "LIP_PRESSOR_LEFT"
            39 -> "LIP_PRESSOR_RIGHT"
            40 -> "LIP_PUCKER_LEFT"
            41 -> "LIP_PUCKER_RIGHT"
            42 -> "LIP_STRETCHER_LEFT"
            43 -> "LIP_STRETCHER_RIGHT"
            44 -> "LIP_SUCK_LEFT_BOTTOM"
            45 -> "LIP_SUCK_LEFT_TOP"
            46 -> "LIP_SUCK_RIGHT_BOTTOM"
            47 -> "LIP_SUCK_RIGHT_TOP"
            48 -> "LIP_TIGHTENER_LEFT"
            49 -> "LIP_TIGHTENER_RIGHT"
            50 -> "LIPS_TOWARD"
            51 -> "LOWER_LIP_DEPRESSOR_LEFT"
            52 -> "LOWER_LIP_DEPRESSOR_RIGHT"
            53 -> "MOUTH_LEFT"
            54 -> "MOUTH_RIGHT"
            55 -> "NOSE_WRINKLER_LEFT"
            56 -> "NOSE_WRINKLER_RIGHT"
            57 -> "OUTER_BROW_RAISER_LEFT"
            58 -> "OUTER_BROW_RAISER_RIGHT"
            59 -> "UPPER_LID_RAISER_LEFT"
            60 -> "UPPER_LID_RAISER_RIGHT"
            61 -> "UPPER_LIP_RAISER_LEFT"
            62 -> "UPPER_LIP_RAISER_RIGHT"
            63 -> "TONGUE_OUT"
            64 -> "TONGUE_LEFT"
            65 -> "TONGUE_RIGHT"
            66 -> "TONGUE_UP"
            67 -> "TONGUE_DOWN"
            else -> "UNKNOWN"
        }
    }
}
