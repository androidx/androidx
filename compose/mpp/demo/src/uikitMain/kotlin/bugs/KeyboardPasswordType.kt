/*
 * Copyright 2023 The Android Open Source Project
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

package bugs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.mpp.demo.Screen
import androidx.compose.ui.text.input.KeyboardType

val KeyboardPasswordType = Screen.Example("KeyboardPasswordType") {
    //TODO: https://youtrack.jetbrains.com/issue/COMPOSE-319/iOS-Bug-password-TextField-changes-behavior-for-all-other-TextFieds
    // Need to uncomment code in textContentType() and isSecureTextEntry()
    Column {
        Text("Issue COMPOSE-319/iOS-Bug-password-TextField-changes-behavior-for-all-other-TextFieds")
        TextField(
            "Password", { },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        TextField("No options", { })
        TextField(
            "Ascii", { },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
        )
    }
}
