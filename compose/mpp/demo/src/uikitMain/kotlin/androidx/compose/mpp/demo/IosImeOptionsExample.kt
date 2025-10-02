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

package androidx.compose.mpp.demo

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Text
import androidx.compose.mpp.demo.textfield.android.demoTextFieldModifiers
import androidx.compose.mpp.demo.textfield.android.fontSize8
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PlatformImeOptions
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.readValue
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSSelectorFromString
import platform.UIKit.NSLayoutConstraint
import platform.UIKit.UIButton
import platform.UIKit.UIColor
import platform.UIKit.UIControlEventTouchUpInside
import platform.UIKit.UIControlStateNormal
import platform.UIKit.UIInputView
import platform.UIKit.UIInputViewAudioFeedbackProtocol
import platform.UIKit.UIInputViewStyle
import platform.UIKit.UIKeyboardAppearanceDark
import platform.UIKit.UIKeyboardAppearanceDefault
import platform.UIKit.UIKeyboardAppearanceLight
import platform.UIKit.UIKeyboardTypeDecimalPad
import platform.UIKit.UIKeyboardTypeDefault
import platform.UIKit.UIKeyboardTypeEmailAddress
import platform.UIKit.UIKeyboardTypeNamePhonePad
import platform.UIKit.UIKeyboardTypeNumberPad
import platform.UIKit.UIKeyboardTypePhonePad
import platform.UIKit.UIKeyboardTypeTwitter
import platform.UIKit.UIKeyboardTypeURL
import platform.UIKit.UIKeyboardTypeWebSearch
import platform.UIKit.UIReturnKeyType
import platform.UIKit.UITextAutocapitalizationType
import platform.UIKit.UITextAutocorrectionType
import platform.UIKit.UIWritingToolsBehaviorComplete
import platform.UIKit.UIWritingToolsBehaviorDefault
import platform.UIKit.UIWritingToolsBehaviorLimited
import platform.UIKit.UIWritingToolsBehaviorNone
import platform.UIKit.constraintEqualToSystemSpacingBelowAnchor

private val keyboardTypes = listOf(
    "Default" to UIKeyboardTypeDefault,
    "Email" to UIKeyboardTypeEmailAddress,
    "Number" to UIKeyboardTypeNumberPad,
    "Phone" to UIKeyboardTypePhonePad,
    "Name" to UIKeyboardTypeNamePhonePad,
    "URL" to UIKeyboardTypeURL,
    "Decimal" to UIKeyboardTypeDecimalPad,
    "Twitter" to UIKeyboardTypeTwitter,
    "WebSearch" to UIKeyboardTypeWebSearch,
    "None" to null,
)

private val keyboardAppearances = listOf(
    "Default" to UIKeyboardAppearanceDefault,
    "Light" to UIKeyboardAppearanceLight,
    "Dark" to UIKeyboardAppearanceDark,
)

private val returnKeyTypes = listOf(
    "Default" to UIReturnKeyType.UIReturnKeyDefault,
    "Go" to UIReturnKeyType.UIReturnKeyGo,
    "Join" to UIReturnKeyType.UIReturnKeyJoin,
    "Next" to UIReturnKeyType.UIReturnKeyNext,
    "Route" to UIReturnKeyType.UIReturnKeyRoute,
    "Search" to UIReturnKeyType.UIReturnKeySearch,
    "Done" to UIReturnKeyType.UIReturnKeyDone,
    "Emergency Call" to UIReturnKeyType.UIReturnKeyEmergencyCall,
    "Null" to null
)

private val autocorrectionTypes = listOf(
    "Default" to UITextAutocorrectionType.UITextAutocorrectionTypeDefault,
    "No" to UITextAutocorrectionType.UITextAutocorrectionTypeNo,
    "Yes" to UITextAutocorrectionType.UITextAutocorrectionTypeYes,
)

private val autocapitalizationTypes = listOf(
    "None" to UITextAutocapitalizationType.UITextAutocapitalizationTypeNone,
    "Sentences" to UITextAutocapitalizationType.UITextAutocapitalizationTypeSentences,
    "Words" to UITextAutocapitalizationType.UITextAutocapitalizationTypeWords,
    "All Characters" to UITextAutocapitalizationType.UITextAutocapitalizationTypeAllCharacters,
    "Null" to null
)

private val writingToolsBehaviorTypes = listOf(
    "None" to UIWritingToolsBehaviorNone,
    "Default" to UIWritingToolsBehaviorDefault,
    "Limited" to UIWritingToolsBehaviorLimited,
    "Complete" to UIWritingToolsBehaviorComplete
)

@OptIn(ExperimentalComposeUiApi::class)
private val IosImeOptionsKeyboardTypeExample = Screen.Example("Keyboard Type") {
    LazyColumn {
        items(keyboardTypes) {
            Item(it.first, PlatformImeOptions { keyboardType(it.second) })
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
private val IosImeOptionsKeyboardAppearanceExample = Screen.Example("Keyboard Appearance") {
    LazyColumn {
        items(keyboardAppearances) {
            Item(it.first, PlatformImeOptions { keyboardAppearance(it.second) })
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
private val IosImeOptionsReturnKeyTypeExample = Screen.Example("Return Key Type") {
    LazyColumn {
        items(returnKeyTypes) {
            Item(it.first, PlatformImeOptions { returnKeyType(it.second) })
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
private val IosImeOptionsIsSecureTextEntryExample = Screen.Example("Is Secure Text Entry") {
    LazyColumn {
        item { Item("Is Secure", PlatformImeOptions { isSecureTextEntry(true) }) }
        item { Item("Is Not Secure", PlatformImeOptions { isSecureTextEntry(false) }) }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
private val IosImeOptionsEnablesReturnKeyTypeAutomaticallyExample = Screen.Example("Enables Return Key Type Automatically") {
    LazyColumn {
        item {
            Item(
                "Enables Return Key Type Automatically",
                PlatformImeOptions { enablesReturnKeyAutomatically(true) }
            )
        }
        item {
            Item(
                "Doesn't Enable Return Key Type Automatically",
                PlatformImeOptions { enablesReturnKeyAutomatically(false) }
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
private val IosImeOptionsAutocapitalizationTypeExample = Screen.Example("Autocapitalization Type") {
    LazyColumn {
        items(autocapitalizationTypes) {
            Item(it.first, PlatformImeOptions { autocapitalizationType(it.second) })
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
private val IosImeOptionsAutocorrectionTypeExample = Screen.Example("Autocapitalization Type") {
    LazyColumn {
        items(autocorrectionTypes) {
            Item(it.first, PlatformImeOptions { autocorrectionType(it.second) })
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
private val IosImeOptionsInputViewExample = Screen.Example("Input View") {
    Column {
        Item("Null Input View", PlatformImeOptions { inputView(null) })

        val randomCharacterInput = rememberSaveable { mutableStateOf("") }
        Item(
            "Random Character Input View",
            options = PlatformImeOptions { inputView(ActionInputView("Random Character", action = { randomCharacterInput.value += ('a'..'z').random().toString() })) },
            value = randomCharacterInput.value,
            onValueChange = { randomCharacterInput.value = it }
        )

        val keyboardController = LocalSoftwareKeyboardController.current
        Item(
            title = "Action Input Accessory View",
            options = PlatformImeOptions { inputAccessoryView(ActionButton("Hide", action = { keyboardController?.hide() })) }
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
private val IosImeOptionsWritingToolsBehaviorExample = Screen.Example("Writing Tools Behavior") {
    LazyColumn {
        items(writingToolsBehaviorTypes) {
            Item(it.first, PlatformImeOptions { writingToolsBehavior(it.second) })
        }
    }
}

val IosImeOptionsExample = Screen.Selection(
    "iOS Platform IME Options",
    IosImeOptionsKeyboardTypeExample,
    IosImeOptionsKeyboardAppearanceExample,
    IosImeOptionsReturnKeyTypeExample,
    IosImeOptionsIsSecureTextEntryExample,
    IosImeOptionsEnablesReturnKeyTypeAutomaticallyExample,
    IosImeOptionsAutocapitalizationTypeExample,
    IosImeOptionsAutocorrectionTypeExample,
    IosImeOptionsInputViewExample,
    IosImeOptionsWritingToolsBehaviorExample
)

@Composable
private fun Item(
    title: String,
    options: PlatformImeOptions
) {
    val state = rememberSaveable { mutableStateOf("") }
    Text(title)
    EditLine(options, value = state.value, onValueChange = { state.value = it })
}

@Composable
private fun Item(
    title: String,
    options: PlatformImeOptions,
    value: String,
    onValueChange: (String) -> Unit
) {
    Text(title)
    EditLine(options, value = value, onValueChange = onValueChange)
}

@Composable
private fun EditLine(
    options: PlatformImeOptions,
    value: String,
    onValueChange: (String) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    BasicTextField(
        modifier = demoTextFieldModifiers,
        value = value,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            platformImeOptions = options
        ),
        keyboardActions = KeyboardActions { keyboardController?.hide() },
        onValueChange = onValueChange,
        textStyle = TextStyle(fontSize = fontSize8),
    )
}

private class ActionButton(
    title: String,
    val action: () -> Unit
): UIButton(frame = CGRectZero.readValue()) {
    init {
        setTitle(title, forState = UIControlStateNormal)
        backgroundColor = UIColor.redColor
        translatesAutoresizingMaskIntoConstraints = false
        addTarget(this, NSSelectorFromString(::handleTap.name), UIControlEventTouchUpInside)
    }

    @ObjCAction
    fun handleTap() = action()
}

private class ActionInputView(
    title: String,
    val action: () -> Unit
) : UIInputView(frame = CGRectMake(0.0, 0.0, 0.0, 300.0), inputViewStyle = UIInputViewStyle.UIInputViewStyleKeyboard), UIInputViewAudioFeedbackProtocol {
    init {
        val button = ActionButton(title, action)
        addSubview(button)

        NSLayoutConstraint.activateConstraints(listOf(
            button.topAnchor.constraintEqualToSystemSpacingBelowAnchor(safeAreaLayoutGuide.topAnchor, multiplier = 1.0),
            button.bottomAnchor.constraintEqualToAnchor(safeAreaLayoutGuide.bottomAnchor, constant = -10.0),
            button.leftAnchor.constraintEqualToAnchor(leftAnchor, constant = 10.0),
            button.rightAnchor.constraintEqualToAnchor(rightAnchor, constant = -10.0),
        ))
    }

    override fun enableInputClicksWhenVisible(): Boolean = true
}