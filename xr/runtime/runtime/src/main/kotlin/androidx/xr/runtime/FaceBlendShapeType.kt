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

package androidx.xr.runtime

import androidx.annotation.RestrictTo

/** Represents the type of face blend shape. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public enum class FaceBlendShapeType {
    /** Left brow lowerer. */
    BROW_LOWERER_LEFT,
    /** Right brow lowerer. */
    BROW_LOWERER_RIGHT,
    /** Left cheek puff. */
    CHEEK_PUFF_LEFT,
    /** Right cheek puff. */
    CHEEK_PUFF_RIGHT,
    /** Left cheek raiser. */
    CHEEK_RAISER_LEFT,
    /** Right cheek raiser. */
    CHEEK_RAISER_RIGHT,
    /** Left cheek suck. */
    CHEEK_SUCK_LEFT,
    /** Right cheek suck. */
    CHEEK_SUCK_RIGHT,
    /** Bottom chin raiser. */
    CHIN_RAISER_B,
    /** Top chin raiser. */
    CHIN_RAISER_T,
    /** Left dimpler. */
    DIMPLER_LEFT,
    /** Right dimpler. */
    DIMPLER_RIGHT,
    /** Left eyes closed. */
    EYES_CLOSED_LEFT,
    /** Right eyes closed. */
    EYES_CLOSED_RIGHT,
    /** Left eyes look down. */
    EYES_LOOK_DOWN_LEFT,
    /** Right eyes look down. */
    EYES_LOOK_DOWN_RIGHT,
    /** Left eyes look left. */
    EYES_LOOK_LEFT_LEFT,
    /** Right eyes look left. */
    EYES_LOOK_LEFT_RIGHT,
    /** Left eyes look right. */
    EYES_LOOK_RIGHT_LEFT,
    /** Right eyes look right. */
    EYES_LOOK_RIGHT_RIGHT,
    /** Left eyes look up. */
    EYES_LOOK_UP_LEFT,
    /** Right eyes look up. */
    EYES_LOOK_UP_RIGHT,
    /** Left inner brow raiser. */
    INNER_BROW_RAISER_LEFT,
    /** Right inner brow raiser. */
    INNER_BROW_RAISER_RIGHT,
    /** Jaw drop. */
    JAW_DROP,
    /** Jaw sideways left. */
    JAW_SIDEWAYS_LEFT,
    /** Jaw sideways right. */
    JAW_SIDEWAYS_RIGHT,
    /** Jaw thrust. */
    JAW_THRUST,
    /** Left lid tightener. */
    LID_TIGHTENER_LEFT,
    /** Right lid tightener. */
    LID_TIGHTENER_RIGHT,
    /** Left lip corner depressor. */
    LIP_CORNER_DEPRESSOR_LEFT,
    /** Right lip corner depressor. */
    LIP_CORNER_DEPRESSOR_RIGHT,
    /** Left lip corner puller. */
    LIP_CORNER_PULLER_LEFT,
    /** Right lip corner puller. */
    LIP_CORNER_PULLER_RIGHT,
    /** Left bottom lip funneler. */
    LIP_FUNNELER_LB,
    /** Left top lip funneler. */
    LIP_FUNNELER_LT,
    /** Right bottom lip funneler. */
    LIP_FUNNELER_RB,
    /** Right top lip funneler. */
    LIP_FUNNELER_RT,
    /** Left lip pressor. */
    LIP_PRESSOR_LEFT,
    /** Right lip pressor. */
    LIP_PRESSOR_RIGHT,
    /** Left lip pucker. */
    LIP_PUCKER_LEFT,
    /** Right lip pucker. */
    LIP_PUCKER_RIGHT,
    /** Left lip stretcher. */
    LIP_STRETCHER_LEFT,
    /** Right lip stretcher. */
    LIP_STRETCHER_RIGHT,
    /** Left bottom lip suck. */
    LIP_SUCK_LB,
    /** Left top lip suck. */
    LIP_SUCK_LT,
    /** Right bottom lip suck. */
    LIP_SUCK_RB,
    /** Right top lip suck. */
    LIP_SUCK_RT,
    /** Left lip tightener. */
    LIP_TIGHTENER_LEFT,
    /** Right lip tightener. */
    LIP_TIGHTENER_RIGHT,
    /** Lips toward. */
    LIPS_TOWARD,
    /** Left lower lip depressor. */
    LOWER_LIP_DEPRESSOR_LEFT,
    /** Right lower lip depressor. */
    LOWER_LIP_DEPRESSOR_RIGHT,
    /** Mouth left. */
    MOUTH_LEFT,
    /** Mouth right. */
    MOUTH_RIGHT,
    /** Left nose wrinkler. */
    NOSE_WRINKLER_LEFT,
    /** Right nose wrinkler. */
    NOSE_WRINKLER_RIGHT,
    /** Left outer brow raiser. */
    OUTER_BROW_RAISER_LEFT,
    /** Right outer brow raiser. */
    OUTER_BROW_RAISER_RIGHT,
    /** Left upper lid raiser. */
    UPPER_LID_RAISER_LEFT,
    /** Right upper lid raiser. */
    UPPER_LID_RAISER_RIGHT,
    /** Left upper lip raiser. */
    UPPER_LIP_RAISER_LEFT,
    /** Right upper lip raiser. */
    UPPER_LIP_RAISER_RIGHT,
    /** Tongue out. */
    TONGUE_OUT,
    /** Tongue left. */
    TONGUE_LEFT,
    /** Tongue right. */
    TONGUE_RIGHT,
    /** Tongue up. */
    TONGUE_UP,
    /** Tongue down. */
    TONGUE_DOWN,
}
