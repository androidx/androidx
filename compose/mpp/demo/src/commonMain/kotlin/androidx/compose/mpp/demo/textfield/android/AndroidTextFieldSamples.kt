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

package androidx.compose.mpp.demo.textfield.android

import androidx.compose.mpp.demo.Screen

val AndroidTextFieldSamples = Screen.Selection(
    "Android TextField samples",
    /*
    //TODO: Basic input fields/simple editing RTL in LTR layout works incorrectly:
       caret doesn’t follow text and shows in a wrong position.
       Manual changing position of the caret leads to changing RTL to LTR editing.
     */
    Screen.Example("Basic input fields") { InputFieldDemo() },
    Screen.Example("Capitalization/AutoCorrect") { CapitalizationAutoCorrectDemo() },
    Screen.Example("Cursor configuration") { TextFieldCursorBlinkingDemo() },
    Screen.Selection(
        "Focus",
        /*
        TODO: Android TextField samples/Focus/Focus transition: focused words should underline
         */
        Screen.Example("Focus transition") { TextFieldFocusTransition() },
        Screen.Example("Focus keyboard interaction") { TextFieldFocusKeyboardInteraction() },
    ),
    Screen.Example("Full-screen field") { FullScreenTextFieldDemo() },
    Screen.Example("Ime Action") { ImeActionDemo() },
    /*
    TODO: Ime Action / Ime Single Line: Behavior differs from android,
     in multiline textfields return button works as default multiline textfield.
     In android multiline tf with ime action works as singleline (except default)
     */
    Screen.Example("Ime SingleLine") { ImeSingleLineDemo() },
    Screen.Example("Inside Dialog") { TextFieldsInDialogDemo() },
    Screen.Example("Inside scrollable") { TextFieldsInScrollableDemo() },
    Screen.Example("Keyboard Types") { KeyboardTypeDemo() },
    Screen.Example("Min/Max Lines") { BasicTextFieldMinMaxDemo() },
    Screen.Example("Reject Text Change") { RejectTextChangeDemo() },
    Screen.Example("Scrollable text fields") { ScrollableTextFieldDemo() },
    Screen.Example("Visual Transformation") { VisualTransformationDemo() },
    Screen.Example("TextFieldValue") { TextFieldValueDemo() },
    Screen.Example("Tail Following Text Field") { TailFollowingTextFieldDemo() },
    Screen.Example("Focus immediately") { FocusTextFieldImmediatelyDemo() },
    /*
    TODO: From-scratch textfield doesn't work at all.
     However, on android using this textfield leads to app crash
     */
    Screen.Example("Secondary input system") { PlatformTextInputAdapterDemo() },
    /*
    TODO: Textfield Focus - iOS doesnt support navigation between textfields via shift + arrow or tab.
      Singleline textfields work with tab
     */
    Screen.Example("TextField focus") { TextFieldFocusDemo() },
    Screen.Example("TextFieldBrush") { TextFieldBrush() },
)
