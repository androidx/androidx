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

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import static java.util.Objects.requireNonNull;

import android.annotation.SuppressLint;
import android.os.Looper;

import androidx.annotation.IntDef;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.car.app.annotations.RequiresCarApi;
import androidx.car.app.model.CarText;
import androidx.car.app.model.InputCallback;
import androidx.car.app.model.InputCallbackDelegate;
import androidx.car.app.model.InputCallbackDelegateImpl;
import androidx.car.app.model.OnInputCompletedDelegate;
import androidx.car.app.model.OnInputCompletedDelegateImpl;
import androidx.car.app.model.OnInputCompletedListener;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

/**
 * A {@link SignInTemplate.SignInMethod} that presents an input box for the user to enter their
 * credentials.
 *
 * <p>For example, this can be used to request a username, a password or an activation code.
 */
@RequiresCarApi(2)
public final class InputSignInMethod implements SignInTemplate.SignInMethod {
    /**
     * The type of input represented by the {@link InputSignInMethod} instance.
     *
     * @hide
     */
    @RestrictTo(LIBRARY)
    @IntDef(
            value = {
                    INPUT_TYPE_DEFAULT,
                    INPUT_TYPE_PASSWORD,
            })
    @Retention(RetentionPolicy.SOURCE)
    public @interface InputType {
    }

    /**
     * Default input where the text is shown as it is typed.
     */
    public static final int INPUT_TYPE_DEFAULT = 1;

    /**
     * Input where the text is hidden as it is typed.
     */
    public static final int INPUT_TYPE_PASSWORD = 2;

    /**
     * The type of keyboard to be displayed while the user is interacting with this input.
     *
     * @hide
     */
    @RestrictTo(LIBRARY)
    @IntDef(
            value = {
                    KEYBOARD_DEFAULT,
                    KEYBOARD_EMAIL,
                    KEYBOARD_PHONE,
                    KEYBOARD_NUMBER,
            })
    @Retention(RetentionPolicy.SOURCE)
    public @interface KeyboardType {
    }

    /**
     * Default (full) keyboard.
     */
    public static final int KEYBOARD_DEFAULT = 1;

    /**
     * Keyboard optimized for typing an email address.
     */
    public static final int KEYBOARD_EMAIL = 2;

    /**
     * Keyboard optimized for typing a phone number.
     */
    public static final int KEYBOARD_PHONE = 3;

    /**
     * Keyboard optimized for typing numbers.
     */
    public static final int KEYBOARD_NUMBER = 4;

    @Keep
    @Nullable
    private final CarText mHint;
    @Keep
    @Nullable
    private final CarText mDefaultValue;
    @Keep
    @InputType
    private final int mInputType;
    @Keep
    @Nullable
    private final CarText mErrorMessage;
    @Keep
    @KeyboardType
    private final int mKeyboardType;
    @Keep
    @Nullable
    private final OnInputCompletedDelegate mOnInputCompletedDelegate;
    @Nullable
    private final InputCallbackDelegate mInputCallbackDelegate;
    @Keep
    private final boolean mShowKeyboardByDefault;

    /**
     * Returns the text explaining to the user what should be entered in this input box or
     * {@code null} if no hint is provided.
     *
     * @see Builder#setHint(CharSequence)
     */
    @Nullable
    public CarText getHint() {
        return mHint;
    }

    /**
     * Returns the default value for this input box or {@code null} if no value is provided.
     *
     * <p>For the {@link #INPUT_TYPE_PASSWORD} input type, this value will formatted to be hidden
     * to the user as well.
     *
     * @see Builder#setDefaultValue(String)
     */
    @Nullable
    public CarText getDefaultValue() {
        return mDefaultValue;
    }

    /**
     * Returns the input type, one of {@link #INPUT_TYPE_DEFAULT} or {@link #INPUT_TYPE_PASSWORD}
     */
    @InputType
    public int getInputType() {
        return mInputType;
    }

    /**
     * Returns an error message associated with the user input.
     *
     * <p>For example, this can be used to indicate formatting errors, wrong username or
     * password, or any other problem related to the user input.
     *
     * @see Builder#setErrorMessage(CharSequence)
     */
    @Nullable
    public CarText getErrorMessage() {
        return mErrorMessage;
    }

    /**
     * Returns the type of keyboard to be displayed when this input gets focused.
     *
     * @see Builder#setKeyboardType(int)
     */
    public int getKeyboardType() {
        return mKeyboardType;
    }

    /**
     * Returns the {@link OnInputCompletedDelegate} for input callbacks.
     *
     * @see Builder#Builder(OnInputCompletedListener)
     */
    @NonNull
    public OnInputCompletedDelegate getOnInputCompletedDelegate() {
        return requireNonNull(mOnInputCompletedDelegate);
    }

    /**
     * Returns the {@link InputCallbackDelegate} for input callbacks.
     *
     * @see Builder#Builder(InputCallback)
     */
    @RequiresCarApi(2)
    @NonNull
    public InputCallbackDelegate getInputCallbackDelegate() {
        return requireNonNull(mInputCallbackDelegate);
    }

    /**
     * Returns whether to show the keyboard by default or not.
     *
     * @see Builder#setShowKeyboardByDefault
     */
    public boolean isShowKeyboardByDefault() {
        return mShowKeyboardByDefault;
    }

    @NonNull
    @Override
    public String toString() {
        return "[inputType:" + mInputType + ", keyboardType: " + mKeyboardType + "]";
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof InputSignInMethod)) {
            return false;
        }

        InputSignInMethod that = (InputSignInMethod) other;
        return mInputType == that.mInputType
                && mKeyboardType == that.mKeyboardType
                && mShowKeyboardByDefault == that.mShowKeyboardByDefault
                && Objects.equals(mHint, that.mHint)
                && Objects.equals(mDefaultValue, that.mDefaultValue)
                && Objects.equals(mErrorMessage, that.mErrorMessage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mHint, mDefaultValue, mInputType, mErrorMessage, mKeyboardType,
                mShowKeyboardByDefault);
    }

    InputSignInMethod(Builder builder) {
        mHint = builder.mHint;
        mDefaultValue = builder.mDefaultValue;
        mInputType = builder.mInputType;
        mErrorMessage = builder.mErrorMessage;
        mKeyboardType = builder.mKeyboardType;
        mOnInputCompletedDelegate = builder.mOnInputCompletedDelegate;
        mInputCallbackDelegate = builder.mInputCallbackDelegate;
        mShowKeyboardByDefault = builder.mShowKeyboardByDefault;
    }

    /** Constructs an empty instance, used by serialization code. */
    private InputSignInMethod() {
        mHint = null;
        mDefaultValue = null;
        mInputType = INPUT_TYPE_DEFAULT;
        mErrorMessage = null;
        mKeyboardType = KEYBOARD_DEFAULT;
        mOnInputCompletedDelegate = null;
        mInputCallbackDelegate = null;
        mShowKeyboardByDefault = false;
    }

    /** A builder of {@link InputSignInMethod}. */
    public static final class Builder {
        @Nullable final OnInputCompletedDelegate mOnInputCompletedDelegate;
        @Nullable final InputCallbackDelegate mInputCallbackDelegate;
        @Nullable
        CarText mHint;
        @Nullable
        CarText mDefaultValue;
        int mInputType = INPUT_TYPE_DEFAULT;
        @Nullable
        CarText mErrorMessage;
        int mKeyboardType = KEYBOARD_DEFAULT;
        boolean mShowKeyboardByDefault;

        /**
         * Sets the text explaining to the user what should be entered in this input box.
         *
         * <p>Unless set with this method, the sign-in method will not show any hint.
         *
         * <p>Spans are supported in the input string.
         *
         * @throws NullPointerException if {@code hint} is {@code null}
         */
        // TODO(b/181569051): document supported span types.
        @NonNull
        public Builder setHint(@NonNull CharSequence hint) {
            mHint = CarText.create(requireNonNull(hint));
            return this;
        }

        /**
         * Sets the default value for this input.
         *
         * <p>Unless set with this method, the input box will not have a default value.
         *
         * <p>For {@link #INPUT_TYPE_PASSWORD} input types, in order to indicate that is not empty
         * it is recommended to use a special value rather the actual credential. Any user input
         * on a {@link #INPUT_TYPE_PASSWORD} input box will replace this default value instead of
         * appending to it.
         *
         * @throws NullPointerException if {@code defaultValue} is {@code null}
         */
        @NonNull
        public Builder setDefaultValue(@NonNull String defaultValue) {
            mDefaultValue = CarText.create(requireNonNull(defaultValue));
            return this;
        }

        /**
         * Sets the input type.
         *
         * <p>This must be one of {@link InputSignInMethod#INPUT_TYPE_DEFAULT} or
         * {@link InputSignInMethod#INPUT_TYPE_PASSWORD}
         *
         * <p>If not set, {@link InputSignInMethod#INPUT_TYPE_DEFAULT} will be assumed.
         *
         * @throws IllegalArgumentException if the provided input type is not supported
         */
        @NonNull
        public Builder setInputType(@InputType int inputType) {
            mInputType = validateInputType(inputType);
            return this;
        }

        /**
         * Sets the error message associated with this input box.
         *
         * <p>For example, this can be used to indicate formatting errors, wrong username or
         * password or any other problem related to the user input.
         *
         * <h4>Requirements</h4>
         *
         * Error messages can have only up to 2 lines of text, amd additional texts beyond the
         * second line may be truncated.
         *
         * <p>Spans are not supported in the input string and will be ignored.
         *
         * @throws NullPointerException if {@code message} is {@code null}
         */
        @NonNull
        public Builder setErrorMessage(@NonNull CharSequence message) {
            mErrorMessage = CarText.create(requireNonNull(message));
            return this;
        }

        /**
         * Sets the keyboard type to display when this input box gets focused.
         *
         * <p>This must be one of {@link #KEYBOARD_DEFAULT}, {@link #KEYBOARD_PHONE},
         * {@link #KEYBOARD_NUMBER}, or {@link #KEYBOARD_EMAIL}. A host might fall back
         * to {@link #KEYBOARD_DEFAULT} if they do not support a particular keyboard type.
         *
         * If not provided, {@link #KEYBOARD_DEFAULT} will be used.
         *
         * @throws IllegalArgumentException if the provided type is not supported
         */
        @NonNull
        public Builder setKeyboardType(@KeyboardType int keyboardType) {
            mKeyboardType = validateKeyboardType(keyboardType);
            return this;
        }

        /**
         * Sets whether keyboard should be opened by default when this template is presented.
         *
         * By default, keyboard will only be opened if the user focuses on the input box.
         */
        @NonNull
        public Builder setShowKeyboardByDefault(boolean showKeyboardByDefault) {
            mShowKeyboardByDefault = showKeyboardByDefault;
            return this;
        }

        /**
         * Builds an {@link InputSignInMethod} instance.
         */
        @NonNull
        public InputSignInMethod build() {
            return new InputSignInMethod(this);
        }

        /**
         * Returns an {@link InputSignInMethod.Builder} instance.
         *
         * <p>Note that the listener relates to UI events and will be executed on the main thread
         * using {@link Looper#getMainLooper()}.
         *
         * @param listener the {@link OnInputCompletedListener} to be notified of input events
         * @throws NullPointerException if {@code listener} is {@code null}
         */
        @SuppressLint("ExecutorRegistration")
        public Builder(@NonNull OnInputCompletedListener listener) {
            mInputCallbackDelegate = null;
            mOnInputCompletedDelegate = OnInputCompletedDelegateImpl.create(
                    requireNonNull(listener));
        }

        /**
         * Returns an {@link InputSignInMethod.Builder} instance.
         *
         * <p>Note that the listener relates to UI events and will be executed on the main thread
         * using {@link Looper#getMainLooper()}.
         *
         * @param listener the {@link InputCallbackDelegate} to be notified of input events
         * @throws NullPointerException if {@code listener} is {@code null}
         */
        @RequiresCarApi(2)
        @SuppressLint("ExecutorRegistration")
        public Builder(@NonNull InputCallback listener) {
            mOnInputCompletedDelegate = null;
            mInputCallbackDelegate = InputCallbackDelegateImpl.create(
                    requireNonNull(listener));
        }

        @KeyboardType
        private static int validateKeyboardType(@KeyboardType int keyboardType) {
            if (keyboardType != KEYBOARD_DEFAULT && keyboardType != KEYBOARD_EMAIL
                    && keyboardType != KEYBOARD_NUMBER && keyboardType != KEYBOARD_PHONE) {
                throw new IllegalArgumentException("Keyboard type is not supported: "
                        + keyboardType);
            }

            return keyboardType;
        }

        @InputType
        private static int validateInputType(@InputType int inputType) {
            if (inputType != INPUT_TYPE_DEFAULT && inputType != INPUT_TYPE_PASSWORD) {
                throw new IllegalArgumentException("Invalid input type: " + inputType);
            }

            return inputType;
        }
    }
}
