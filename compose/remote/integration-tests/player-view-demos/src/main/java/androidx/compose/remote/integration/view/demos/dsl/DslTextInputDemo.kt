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

package androidx.compose.remote.integration.view.demos.dsl

import androidx.compose.remote.creation.dsl.Modifier
import androidx.compose.remote.creation.dsl.RcColumnVerticalPositioning
import androidx.compose.remote.creation.dsl.RcDensityBehavior
import androidx.compose.remote.creation.dsl.RcFontWeight
import androidx.compose.remote.creation.dsl.RcHeaderTag
import androidx.compose.remote.creation.dsl.RcHorizontalPositioning
import androidx.compose.remote.creation.dsl.RcImeAction
import androidx.compose.remote.creation.dsl.RcInputFilter
import androidx.compose.remote.creation.dsl.RcKeyboardType
import androidx.compose.remote.creation.dsl.RcProfile
import androidx.compose.remote.creation.dsl.RcScope
import androidx.compose.remote.creation.dsl.RcText
import androidx.compose.remote.creation.dsl.background
import androidx.compose.remote.creation.dsl.border
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.fillMaxWidth
import androidx.compose.remote.creation.dsl.padding
import androidx.compose.remote.creation.dsl.rcColor
import androidx.compose.remote.creation.dsl.rsp
import androidx.compose.remote.creation.profile.RcPlatformProfiles

private const val BACKGROUND = 0xFF181A20
private const val FIELD_BACKGROUND = 0xFF262A33
private const val FIELD_BORDER = 0xFF3D4452
private const val LABEL_COLOR = 0xFF9AA4B2
private const val TEXT_COLOR = 0xFFFFFFFF
private const val ACCENT = 0xFF8AB4F8

/**
 * Text input demo: three editable fields (name, age, email) declared as [RcScope.EditableText] (a
 * text input layout wrapping a text). Tapping a field opens the platform editor over it; edits are
 * written back into the bound strings, which the summary text at the bottom is derived from.
 */
public fun dslTextInputDemo(): ByteArray {
    return createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX, isExperimental = true),
        RcHeaderTag.densityBehavior(RcDensityBehavior.Dp),
    ) {
        val name = remoteNamedText("name", "")
        val age = remoteNamedText("age", "")
        val email = remoteNamedText("email", "")
        val notes = remoteNamedText("notes", "")

        Column(
            modifier = Modifier.fillMaxSize().background(BACKGROUND.toInt()).padding(24f),
            horizontal = RcHorizontalPositioning.Start,
            vertical = RcColumnVerticalPositioning.Top,
        ) {
            Text(
                text = "Text input",
                fontSize = 32.rsp,
                fontWeight = RcFontWeight.Bold,
                color = TEXT_COLOR.toInt(),
                modifier = Modifier.padding(bottom = 24f),
            )

            field(
                label = "Name",
                value = name,
                hint = remoteText("Your name"),
                keyboardType = RcKeyboardType.Text,
                imeAction = RcImeAction.Next,
            )
            field(
                label = "Age",
                value = age,
                hint = remoteText("Digits only"),
                keyboardType = RcKeyboardType.Number,
                imeAction = RcImeAction.Next,
                filter = RcInputFilter.Digits,
                maxLength = 3,
            )
            field(
                label = "Email",
                value = email,
                hint = remoteText("name@example.com"),
                keyboardType = RcKeyboardType.Email,
                imeAction = RcImeAction.Done,
            )

            Text(
                text = "Notes (multi-line)",
                fontSize = 14.rsp,
                color = LABEL_COLOR.toInt(),
                modifier = Modifier.padding(top = 12f, bottom = 4f),
            )
            EditableText(
                text = notes,
                fontSize = 18.rsp,
                color = TEXT_COLOR.toInt(),
                maxLines = 4,
                modifier =
                    Modifier.fillMaxWidth()
                        .background(FIELD_BACKGROUND.toInt())
                        .border(1f, 8f, FIELD_BORDER.rcColor())
                        .padding(16f, 14f, 16f, 14f),
                keyboardType = RcKeyboardType.Text,
                singleLine = false,
                hint = remoteText("Anything else?"),
            )

            Text(
                text = "Summary",
                fontSize = 14.rsp,
                color = LABEL_COLOR.toInt(),
                modifier = Modifier.padding(top = 24f, bottom = 4f),
            )
            Text(
                text = "Hello " join (name + ", " + age + " years old (" + email + ")"),
                fontSize = 18.rsp,
                color = ACCENT.toInt(),
                maxLines = 3,
            )
        }
    }
}

private fun RcScope.field(
    label: String,
    value: RcText,
    hint: RcText,
    keyboardType: RcKeyboardType,
    imeAction: RcImeAction,
    filter: RcInputFilter = RcInputFilter.None,
    maxLength: Int = 0,
) {
    Text(
        text = label,
        fontSize = 14.rsp,
        color = LABEL_COLOR.toInt(),
        modifier = Modifier.padding(top = 12f, bottom = 4f),
    )
    EditableText(
        text = value,
        fontSize = 20.rsp,
        color = TEXT_COLOR.toInt(),
        modifier =
            Modifier.fillMaxWidth()
                .background(FIELD_BACKGROUND.toInt())
                .border(1f, 8f, FIELD_BORDER.rcColor())
                .padding(16f, 14f, 16f, 14f),
        keyboardType = keyboardType,
        imeAction = imeAction,
        singleLine = true,
        filter = filter,
        maxLength = maxLength,
        hint = hint,
    )
}
