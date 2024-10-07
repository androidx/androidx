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

package androidx.wear.protolayout.material3

import androidx.annotation.OptIn
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.LayoutElementBuilders.FontVariantProp
import androidx.wear.protolayout.LayoutElementBuilders.FontWeightProp
import androidx.wear.protolayout.expression.ProtoLayoutExperimental
import androidx.wear.protolayout.material3.Typography.ARC_MEDIUM
import androidx.wear.protolayout.material3.Typography.ARC_SMALL
import androidx.wear.protolayout.material3.Typography.BODY_EXTRA_SMALL
import androidx.wear.protolayout.material3.Typography.BODY_LARGE
import androidx.wear.protolayout.material3.Typography.BODY_MEDIUM
import androidx.wear.protolayout.material3.Typography.BODY_SMALL
import androidx.wear.protolayout.material3.Typography.DISPLAY_LARGE
import androidx.wear.protolayout.material3.Typography.DISPLAY_MEDIUM
import androidx.wear.protolayout.material3.Typography.DISPLAY_SMALL
import androidx.wear.protolayout.material3.Typography.LABEL_LARGE
import androidx.wear.protolayout.material3.Typography.LABEL_MEDIUM
import androidx.wear.protolayout.material3.Typography.LABEL_SMALL
import androidx.wear.protolayout.material3.Typography.NUMERAL_EXTRA_LARGE
import androidx.wear.protolayout.material3.Typography.NUMERAL_EXTRA_SMALL
import androidx.wear.protolayout.material3.Typography.NUMERAL_LARGE
import androidx.wear.protolayout.material3.Typography.NUMERAL_MEDIUM
import androidx.wear.protolayout.material3.Typography.NUMERAL_SMALL
import androidx.wear.protolayout.material3.Typography.TITLE_LARGE
import androidx.wear.protolayout.material3.Typography.TITLE_MEDIUM
import androidx.wear.protolayout.material3.Typography.TITLE_SMALL
import androidx.wear.protolayout.material3.Typography.TypographyToken

/**
 * Helper object for determining additional font specifics that should be used for each Typography.
 */
// TODO: b/372068167 - Address the diff here.
internal object TypographyFontSelection {
    /**
     * Returns the [FontVariantProp] for the given typography which is used to select font when
     * system font is not used.
     */
    @OptIn(ProtoLayoutExperimental::class)
    fun getFontVariant(@TypographyToken typographyToken: Int): FontVariantProp =
        FontVariantProp.Builder()
            .setValue(
                when (typographyToken) {
                    ARC_MEDIUM,
                    ARC_SMALL,
                    BODY_EXTRA_SMALL,
                    BODY_LARGE,
                    BODY_MEDIUM,
                    BODY_SMALL,
                    LABEL_LARGE,
                    LABEL_MEDIUM,
                    LABEL_SMALL -> LayoutElementBuilders.FONT_VARIANT_BODY
                    DISPLAY_LARGE,
                    DISPLAY_MEDIUM,
                    DISPLAY_SMALL,
                    NUMERAL_EXTRA_LARGE,
                    NUMERAL_EXTRA_SMALL,
                    NUMERAL_LARGE,
                    NUMERAL_MEDIUM,
                    NUMERAL_SMALL,
                    TITLE_LARGE,
                    TITLE_MEDIUM,
                    TITLE_SMALL -> LayoutElementBuilders.FONT_VARIANT_TITLE
                    else ->
                        throw IllegalArgumentException("Typography $typographyToken does not exit.")
                }
            )
            .build()

    /** Returns the [FontWeightProp] for the given typography. */
    @OptIn(ProtoLayoutExperimental::class)
    fun getFontWeight(@TypographyToken typographyToken: Int): FontWeightProp =
        FontWeightProp.Builder()
            .setValue(
                when (typographyToken) {
                    DISPLAY_SMALL,
                    TITLE_MEDIUM,
                    TITLE_SMALL,
                    LABEL_LARGE,
                    LABEL_MEDIUM,
                    LABEL_SMALL,
                    BODY_EXTRA_SMALL,
                    BODY_SMALL,
                    ARC_MEDIUM,
                    ARC_SMALL,
                    NUMERAL_EXTRA_LARGE,
                    NUMERAL_LARGE,
                    NUMERAL_MEDIUM,
                    NUMERAL_SMALL,
                    NUMERAL_EXTRA_SMALL -> LayoutElementBuilders.FONT_WEIGHT_MEDIUM
                    DISPLAY_LARGE,
                    DISPLAY_MEDIUM,
                    TITLE_LARGE,
                    BODY_LARGE,
                    BODY_MEDIUM -> LayoutElementBuilders.FONT_WEIGHT_NORMAL
                    else ->
                        throw IllegalArgumentException("Typography $typographyToken does not exit.")
                }
            )
            .build()

    /** Returns whether the given typography should be scaled with the user font size changing. */
    fun getFontScalability(@TypographyToken typographyToken: Int): Boolean =
        when (typographyToken) {
            TITLE_MEDIUM,
            TITLE_SMALL,
            LABEL_MEDIUM,
            LABEL_SMALL,
            BODY_EXTRA_SMALL,
            BODY_LARGE,
            BODY_MEDIUM,
            BODY_SMALL,
            ARC_MEDIUM,
            ARC_SMALL -> true
            DISPLAY_LARGE,
            DISPLAY_MEDIUM,
            DISPLAY_SMALL,
            TITLE_LARGE,
            LABEL_LARGE,
            NUMERAL_EXTRA_LARGE,
            NUMERAL_EXTRA_SMALL,
            NUMERAL_LARGE,
            NUMERAL_MEDIUM,
            NUMERAL_SMALL -> false
            else -> throw IllegalArgumentException("Typography $typographyToken does not exit.")
        }
}
