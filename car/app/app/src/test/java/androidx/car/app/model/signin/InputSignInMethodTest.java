/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.car.app.model.signin;

import static androidx.car.app.model.signin.InputSignInMethod.INPUT_TYPE_DEFAULT;
import static androidx.car.app.model.signin.InputSignInMethod.INPUT_TYPE_PASSWORD;
import static androidx.car.app.model.signin.InputSignInMethod.KEYBOARD_DEFAULT;
import static androidx.car.app.model.signin.InputSignInMethod.KEYBOARD_EMAIL;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import androidx.car.app.OnDoneCallback;
import androidx.car.app.model.InputCallback;
import androidx.car.app.model.InputCallbackDelegate;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

/** Tests for {@link InputSignInMethod}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class InputSignInMethodTest {
    @Rule
    public final MockitoRule mockito = MockitoJUnit.rule();

    @Mock
    InputCallback mCallback;

    @Test
    public void create_defaultValues() {
        InputSignInMethod signIn = new InputSignInMethod.Builder(mCallback).build();

        assertThat(signIn.getInputType()).isEqualTo(INPUT_TYPE_DEFAULT);
        assertThat(signIn.getKeyboardType()).isEqualTo(KEYBOARD_DEFAULT);
        assertThat(signIn.getHint()).isNull();
        assertThat(signIn.getErrorMessage()).isNull();
        assertThat(signIn.isShowKeyboardByDefault()).isFalse();
    }

    @Test
    public void inputSubmittedCallback() {
        InputSignInMethod signIn = new InputSignInMethod.Builder(mCallback).build();

        InputCallbackDelegate delegate = signIn.getInputCallbackDelegate();
        OnDoneCallback onDoneCallback = mock(OnDoneCallback.class);
        delegate.sendInputSubmitted("ABC", onDoneCallback);

        verify(mCallback).onInputSubmitted("ABC");
        verify(onDoneCallback).onSuccess(null);
    }

    @Test
    public void inputTextChangedCallback() {
        InputSignInMethod signIn = new InputSignInMethod.Builder(mCallback).build();

        InputCallbackDelegate delegate = signIn.getInputCallbackDelegate();
        OnDoneCallback onDoneCallback = mock(OnDoneCallback.class);
        delegate.sendInputTextChanged("ABC", onDoneCallback);

        verify(mCallback).onInputTextChanged("ABC");
        verify(onDoneCallback).onSuccess(null);
    }

    @Test
    public void create_withInputType() {
        InputSignInMethod signIn = new InputSignInMethod.Builder(mCallback)
                .setInputType(INPUT_TYPE_PASSWORD)
                .build();

        assertThat(signIn.getInputType()).isEqualTo(INPUT_TYPE_PASSWORD);
    }

    @Test
    public void create_withKeyboardType() {
        InputSignInMethod signIn = new InputSignInMethod.Builder(mCallback)
                .setKeyboardType(KEYBOARD_EMAIL)
                .build();

        assertThat(signIn.getKeyboardType()).isEqualTo(KEYBOARD_EMAIL);
    }

    @Test
    public void create_wtihPrompt() {
        InputSignInMethod signIn = new InputSignInMethod.Builder(mCallback)
                .setHint("Signin")
                .build();

        assertThat(signIn.getHint().toString()).isEqualTo("Signin");
    }

    @Test
    public void create_withMessage() {
        InputSignInMethod signIn = new InputSignInMethod.Builder(mCallback)
                .setErrorMessage("error")
                .build();

        assertThat(signIn.getErrorMessage().toString()).isEqualTo("error");
    }

    @Test
    public void create_showKeyboard() {
        InputSignInMethod signIn = new InputSignInMethod.Builder(mCallback)
                .setShowKeyboardByDefault(true)
                .build();

        assertThat(signIn.isShowKeyboardByDefault()).isTrue();
    }

    @Test
    public void equals() {
        int inputType = INPUT_TYPE_PASSWORD;
        int keyboardType = KEYBOARD_EMAIL;
        String message = "error";
        String instructions = "sign";

        InputSignInMethod signIn = new InputSignInMethod.Builder(mCallback)
                .setInputType(inputType)
                .setKeyboardType(keyboardType)
                .setHint(instructions)
                .setErrorMessage(message)
                .setShowKeyboardByDefault(true)
                .build();

        assertThat(signIn)
                .isEqualTo(new InputSignInMethod.Builder(mCallback)
                        .setInputType(inputType)
                        .setKeyboardType(keyboardType)
                        .setHint(instructions)
                        .setErrorMessage(message)
                        .setShowKeyboardByDefault(true)
                        .build());
    }

    @Test
    public void notEquals_differentInputType() {
        int keyboardType = KEYBOARD_EMAIL;
        String message = "error";
        String instructions = "sign";

        InputSignInMethod signIn = new InputSignInMethod.Builder(mCallback)
                .setInputType(INPUT_TYPE_PASSWORD)
                .setKeyboardType(keyboardType)
                .setHint(instructions)
                .setErrorMessage(message)
                .setShowKeyboardByDefault(true)
                .build();

        assertThat(signIn)
                .isNotEqualTo(new InputSignInMethod.Builder(mCallback)
                        .setInputType(INPUT_TYPE_DEFAULT)
                        .setKeyboardType(keyboardType)
                        .setHint(instructions)
                        .setErrorMessage(message)
                        .setShowKeyboardByDefault(true)
                        .build());
    }

    @Test
    public void notEquals_differentKeyboardType() {
        int inputType = INPUT_TYPE_PASSWORD;
        String message = "error";
        String instructions = "sign";

        InputSignInMethod signIn = new InputSignInMethod.Builder(mCallback)
                .setInputType(inputType)
                .setKeyboardType(KEYBOARD_EMAIL)
                .setHint(instructions)
                .setErrorMessage(message)
                .setShowKeyboardByDefault(true)
                .build();

        assertThat(signIn)
                .isNotEqualTo(new InputSignInMethod.Builder(mCallback)
                        .setInputType(inputType)
                        .setKeyboardType(KEYBOARD_DEFAULT)
                        .setHint(instructions)
                        .setErrorMessage(message)
                        .setShowKeyboardByDefault(true)
                        .build());
    }

    @Test
    public void notEquals_differentInstructions() {
        int inputType = INPUT_TYPE_PASSWORD;
        int keyboardType = KEYBOARD_EMAIL;
        String message = "error";

        InputSignInMethod signIn = new InputSignInMethod.Builder(mCallback)
                .setInputType(inputType)
                .setKeyboardType(keyboardType)
                .setHint("signin")
                .setErrorMessage(message)
                .setShowKeyboardByDefault(true)
                .build();

        assertThat(signIn)
                .isNotEqualTo(new InputSignInMethod.Builder(mCallback)
                        .setInputType(inputType)
                        .setKeyboardType(keyboardType)
                        .setHint("sign2")
                        .setErrorMessage(message)
                        .setShowKeyboardByDefault(true)
                        .build());
    }

    @Test
    public void notEquals_differentMessage() {
        int inputType = INPUT_TYPE_PASSWORD;
        int keyboardType = KEYBOARD_EMAIL;
        String instructions = "sign";

        InputSignInMethod signIn = new InputSignInMethod.Builder(mCallback)
                .setInputType(inputType)
                .setKeyboardType(keyboardType)
                .setHint(instructions)
                .setErrorMessage("error")
                .setShowKeyboardByDefault(true)
                .build();

        assertThat(signIn)
                .isNotEqualTo(new InputSignInMethod.Builder(mCallback)
                        .setInputType(inputType)
                        .setKeyboardType(keyboardType)
                        .setHint(instructions)
                        .setErrorMessage("error2")
                        .setShowKeyboardByDefault(true)
                        .build());
    }

    @Test
    public void notEquals_differentShowKeyboard() {
        int inputType = INPUT_TYPE_PASSWORD;
        int keyboardType = KEYBOARD_EMAIL;
        String message = "error";
        String instructions = "sign";

        InputSignInMethod signIn = new InputSignInMethod.Builder(mCallback)
                .setInputType(inputType)
                .setKeyboardType(keyboardType)
                .setHint(instructions)
                .setErrorMessage(message)
                .setShowKeyboardByDefault(true)
                .build();

        assertThat(signIn)
                .isNotEqualTo(new InputSignInMethod.Builder(mCallback)
                        .setInputType(inputType)
                        .setKeyboardType(keyboardType)
                        .setHint(instructions)
                        .setErrorMessage(message)
                        .setShowKeyboardByDefault(false)
                        .build());
    }
}
