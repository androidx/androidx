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
@file:Suppress("RestrictedApiAndroidX")

package androidx.wear.compose.remote.material3

import androidx.compose.remote.creation.compose.text.RemoteTextStyle
import androidx.wear.compose.material3.Typography

internal object RemoteTypographyTokens {
    private val defaultTypography = Typography()
    val DisplayLarge = RemoteTextStyle.fromTextStyle(defaultTypography.displayLarge)
    val DisplayMedium = RemoteTextStyle.fromTextStyle(defaultTypography.displayMedium)
    val DisplaySmall = RemoteTextStyle.fromTextStyle(defaultTypography.displaySmall)

    val TitleLarge = RemoteTextStyle.fromTextStyle(defaultTypography.titleLarge)
    val TitleMedium = RemoteTextStyle.fromTextStyle(defaultTypography.titleMedium)
    val TitleSmall = RemoteTextStyle.fromTextStyle(defaultTypography.titleSmall)

    val LabelLarge = RemoteTextStyle.fromTextStyle(defaultTypography.labelLarge)
    val LabelMedium = RemoteTextStyle.fromTextStyle(defaultTypography.labelMedium)
    val LabelSmall = RemoteTextStyle.fromTextStyle(defaultTypography.labelSmall)

    val BodyLarge = RemoteTextStyle.fromTextStyle(defaultTypography.bodyLarge)
    val BodyMedium = RemoteTextStyle.fromTextStyle(defaultTypography.bodyMedium)
    val BodySmall = RemoteTextStyle.fromTextStyle(defaultTypography.bodySmall)
    val BodyExtraSmall = RemoteTextStyle.fromTextStyle(defaultTypography.bodyExtraSmall)

    val NumeralExtraLarge = RemoteTextStyle.fromTextStyle(defaultTypography.numeralExtraLarge)
    val NumeralLarge = RemoteTextStyle.fromTextStyle(defaultTypography.numeralLarge)
    val NumeralMedium = RemoteTextStyle.fromTextStyle(defaultTypography.numeralMedium)
    val NumeralSmall = RemoteTextStyle.fromTextStyle(defaultTypography.numeralSmall)
    val NumeralExtraSmall = RemoteTextStyle.fromTextStyle(defaultTypography.numeralExtraSmall)
}
