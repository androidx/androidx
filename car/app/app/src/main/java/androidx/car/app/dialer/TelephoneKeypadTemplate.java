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
package androidx.car.app.dialer;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.car.app.model.constraints.ActionsConstraints.ACTION_CONSTRAINTS_TELEPHONE_KEYPAD_HEADER;
import static androidx.car.app.model.constraints.ActionsConstraints.ACTION_CONSTRAINTS_TELEPHONE_KEYPAD_PRIMARY;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.util.Objects.requireNonNull;

import android.annotation.SuppressLint;

import androidx.annotation.IntDef;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.annotations.KeepFields;
import androidx.car.app.model.Action;
import androidx.car.app.model.CarText;
import androidx.car.app.model.Header;
import androidx.car.app.model.InputCallback;
import androidx.car.app.model.InputCallbackDelegate;
import androidx.car.app.model.InputCallbackDelegateImpl;
import androidx.car.app.model.OnClickListener;
import androidx.car.app.model.Template;
import androidx.car.app.model.constraints.CarTextConstraints;

import com.google.errorprone.annotations.CanIgnoreReturnValue;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A template that allows a user to dial a phone number which includes a keypad, a primary button,
 * and a place for the user to see their dialed number.
 */
@ExperimentalCarApi
@KeepFields
@CarProtocol
public class TelephoneKeypadTemplate implements Template {
    /** The `0` key in the keypad. */
    public static final int KEY_ZERO = 0;

    /** The `1` key in the keypad. */
    public static final int KEY_ONE = 1;

    /** The `2` key in the keypad. */
    public static final int KEY_TWO = 2;

    /** The `3` key in the keypad. */
    public static final int KEY_THREE = 3;

    /** The `4` key in the keypad. */
    public static final int KEY_FOUR = 4;

    /** The `5` key in the keypad. */
    public static final int KEY_FIVE = 5;

    /** The `6` key in the keypad. */
    public static final int KEY_SIX = 6;

    /** The `7` key in the keypad. */
    public static final int KEY_SEVEN = 7;

    /** The `8` key in the keypad. */
    public static final int KEY_EIGHT = 8;

    /** The `9` key in the keypad. */
    public static final int KEY_NINE = 9;

    /** The `*` key in the keypad. */
    public static final int KEY_STAR = 10;

    /** The `#` key in the keypad. */
    public static final int KEY_POUND = 11;

    @Nullable private final Header mHeader;
    @Nullable private final String mPhoneNumber;
    @Nullable private final Action mPrimaryAction;
    @Nullable private final TelephoneKeypadCallbackDelegate mTelephoneKeypadCallbackDelegate;
    @Nullable private final InputCallbackDelegate mPhoneNumberChangedDelegate;
    private final Map<Integer, CarText> mKeySecondaryTexts;

    /** Default empty constructor for the serializer. */
    @SuppressWarnings("assignment.type.incompatible")
    @VisibleForTesting
    public TelephoneKeypadTemplate() {
        mHeader = null;
        mPhoneNumber = null;
        mPrimaryAction = null;
        mTelephoneKeypadCallbackDelegate = null;
        mPhoneNumberChangedDelegate = null;
        mKeySecondaryTexts = new HashMap<>(0);
    }

    private TelephoneKeypadTemplate(Builder builder) {
        mHeader = builder.mHeader;
        mPhoneNumber = builder.mPhoneNumber;
        mPrimaryAction = builder.mPrimaryAction;
        mTelephoneKeypadCallbackDelegate = builder.mTelephoneKeypadCallback;
        mPhoneNumberChangedDelegate = builder.mPhoneNumberChangedDelegate;
        mKeySecondaryTexts = builder.mKeySecondaryTexts;
    }

    /** Returns the {@link Header} to show in this template. */
    @Nullable
    public Header getHeader() {
        return mHeader;
    }

    /**
     * Gets the phone number to show in the "dialed number" field which can be edited before
     * calling.
     *
     * @see TelephoneKeypadTemplate.Builder#setPhoneNumber(String)
     */
    @Nullable
    public String getPhoneNumber() {
        return mPhoneNumber;
    }

    /**
     * Returns the primary action button. This is usually the call button, but the app can set this
     * arbitrarily.
     */
    @Nullable
    public Action getPrimaryAction() {
        return mPrimaryAction;
    }

    /** Returns the delegate that is used to fire key events back to the client app. */
    @Nullable
    public TelephoneKeypadCallbackDelegate getTelephoneKeypadCallbackDelegate() {
        return mTelephoneKeypadCallbackDelegate;
    }

    /**
     * Returns the delegate that is used to fire dialed number change events back to the client app.
     */
    @Nullable
    public InputCallbackDelegate getPhoneNumberChangedDelegate() {
        return mPhoneNumberChangedDelegate;
    }

    /** Returns the mapping of key -> text for the keypad button's secondary text. */
    @NonNull
    public Map<Integer, CarText> getKeySecondaryTexts() {
        return mKeySecondaryTexts;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof TelephoneKeypadTemplate)) {
            return false;
        }
        TelephoneKeypadTemplate that = (TelephoneKeypadTemplate) other;
        return Objects.equals(mHeader, that.mHeader)
                && Objects.equals(mPhoneNumber, that.mPhoneNumber)
                && Objects.equals(mPrimaryAction, that.mPrimaryAction)
                // There's no way to compare a callback, so only compare if it's present
                && ((mTelephoneKeypadCallbackDelegate == null)
                        == (that.mTelephoneKeypadCallbackDelegate == null))
                && ((mPhoneNumberChangedDelegate == null)
                        == (that.mPhoneNumberChangedDelegate == null))
                && mKeySecondaryTexts.size() == that.mKeySecondaryTexts.size()
                && mKeySecondaryTexts.entrySet().containsAll(that.mKeySecondaryTexts.entrySet());
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                mHeader,
                mPhoneNumber,
                mPrimaryAction,
                mTelephoneKeypadCallbackDelegate == null,
                mPhoneNumberChangedDelegate == null,
                mKeySecondaryTexts);
    }

    @NonNull
    @Override
    public String toString() {
        return "TelephoneKeypadTemplate { header: "
                + mHeader
                + "; phone number: "
                + mPhoneNumber
                + "; primaryAction: "
                + mPrimaryAction
                + "; secondaryTextMap: "
                + mKeySecondaryTexts
                + " }";
    }

    @IntDef({
        KEY_ZERO, KEY_ONE, KEY_TWO, KEY_THREE, KEY_FOUR, KEY_FIVE, KEY_SIX, KEY_SEVEN, KEY_EIGHT,
        KEY_NINE, KEY_STAR, KEY_POUND
    })
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(LIBRARY)
    @Target({FIELD, PARAMETER, METHOD})
    public @interface KeypadKey {}

    /** An interface for clients to receive events about the phone number changing. */
    public interface PhoneNumberChangeListener {
        /**
         * Called when the number that's being dialed by the user changes for any reason. This is
         * usually associated with a button press which will also trigger an event to {@link
         * TelephoneKeypadCallback}; however, keys like "backspace" will update this number number
         * without firing an event to the keypad listener.
         */
        void onPhoneNumberChanged(@NonNull String number);
    }

    /** A builder for {@link TelephoneKeypadTemplate}. */
    public static final class Builder {
        @Nullable private final Action mPrimaryAction;
        private final InputCallbackDelegate mPhoneNumberChangedDelegate;
        private final Map<Integer, CarText> mKeySecondaryTexts = new HashMap<>(13);
        @Nullable private Header mHeader;
        @Nullable private String mPhoneNumber;
        @Nullable private TelephoneKeypadCallbackDelegate mTelephoneKeypadCallback;

        /**
         * Creates a builder for a {@link TelephoneKeypadTemplate}.
         *
         * <p>{@code primaryAction} is usually the "call" button, but can be set arbitrarily by the
         * app (eg. it can be a hangup button when the user is in a call). This action must include
         * an icon,must not have text, and may define a custom color (subject to contrast
         * requirements). This action is subject to the constraints defined in
         * {@link androidx.car.app.model.constraints.ActionsConstraints#ACTION_CONSTRAINTS_TELEPHONE_KEYPAD_PRIMARY}.
         *
         * <p>The app must keep track of the dialed phone number via {@code numberChangeListener}
         * because the primary action's {@link Action.Builder#setOnClickListener(OnClickListener)}
         * does not provide the fully dialed phone number.
         */
        @SuppressLint("ExecutorRegistration")
        public Builder(
                @NonNull Action primaryAction,
                @NonNull PhoneNumberChangeListener numberChangeListener) {
            ACTION_CONSTRAINTS_TELEPHONE_KEYPAD_PRIMARY.validateOrThrow(
                    Collections.singletonList(primaryAction));
            mPrimaryAction = primaryAction;
            mPhoneNumberChangedDelegate =
                    InputCallbackDelegateImpl.create(
                            new InputCallback() {
                                @Override
                                public void onInputTextChanged(@NonNull String text) {
                                    numberChangeListener.onPhoneNumberChanged(text);
                                }
                            });
        }

        /**
         * Create a Builder for a {@link TelephoneKeypadTemplate} with values from the given
         * template.
         */
        public Builder(@NonNull TelephoneKeypadTemplate template) {
            mHeader = template.getHeader();
            mPhoneNumber = template.getPhoneNumber();
            mPrimaryAction = template.getPrimaryAction();
            mTelephoneKeypadCallback = template.getTelephoneKeypadCallbackDelegate();
            mPhoneNumberChangedDelegate = requireNonNull(template.mPhoneNumberChangedDelegate);
            mKeySecondaryTexts.putAll(template.getKeySecondaryTexts());
        }

        /** Sets the header to show in the template.
         *
         * <p>The start action of the header, if present, is subject to the constraints defined in
         * {@link androidx.car.app.model.constraints.ActionsConstraints#ACTION_CONSTRAINTS_TELEPHONE_KEYPAD_HEADER}.
         */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setHeader(@Nullable Header header) {
            this.mHeader = header;
            return this;
        }

        /**
         * Sets the phone number to show in the dialer which can be edited before calling.
         *
         * <p>If a phone number is being edited in an existing instance of this template the value
         * will be reset to the phone number in the new template.
         */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setPhoneNumber(@Nullable String phoneNumber) {
            this.mPhoneNumber = phoneNumber;
            return this;
        }

        /** Sets the callback for the keypad. See {@link TelephoneKeypadCallback}. */
        @SuppressLint({"MissingGetterMatchingBuilder", "ExecutorRegistration"})
        @CanIgnoreReturnValue
        @NonNull
        public Builder setTelephoneKeypadCallback(@Nullable TelephoneKeypadCallback callback) {
            if (callback == null) {
                mTelephoneKeypadCallback = null;
            } else {
                mTelephoneKeypadCallback = TelephoneKeypadCallbackDelegateImpl.create(callback);
            }
            return this;
        }

        /**
         * Sets the secondary text underneath the numbers/symbols of the keypad, overwriting any
         * text that already exists for the given {@code key}. Both icon and text is allowed (for
         * example, showing a voicemail icon underneath the 1 key to denote that pressing and
         * holding that key will automatically call voicemail). The {@code secondaryText} is subject
         * to the constraints defined in {@link CarTextConstraints#TEXT_AND_ICON}.
         *
         * <p>Throws an {@link IllegalArgumentException} for any invalid keys.
         *
         * @see #setKeySecondaryTexts(Map) to mass set button secondary text
         */
        @CanIgnoreReturnValue
        @NonNull
        public Builder addKeySecondaryText(@KeypadKey int key, @NonNull CarText secondaryText) {
            if (key != KEY_ZERO
                    && key != KEY_ONE
                    && key != KEY_TWO
                    && key != KEY_THREE
                    && key != KEY_FOUR
                    && key != KEY_FIVE
                    && key != KEY_SIX
                    && key != KEY_SEVEN
                    && key != KEY_EIGHT
                    && key != KEY_NINE
                    && key != KEY_STAR
                    && key != KEY_POUND) {
                throw new IllegalArgumentException("Invalid key int \"" + key + "\"");
            }
            CarTextConstraints.TEXT_AND_ICON.validateOrThrow(secondaryText);
            mKeySecondaryTexts.put(key, secondaryText);
            return this;
        }

        /**
         * Sets all the given button's {@code secondaryTexts}, overwriting existing values, but not
         * clearing any existing values (eg. if KEY_ONE was already added and the map passed in only
         * included TWO, the result would be both ONE and TWO). Throws an {@link
         * IllegalArgumentException} for any invalid keys.
         *
         * @see #addKeySecondaryText(int, CarText) to set individual buttons
         */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setKeySecondaryTexts(@NonNull Map<Integer, CarText> secondaryTexts) {
            for (Map.Entry<Integer, CarText> entry : secondaryTexts.entrySet()) {
                addKeySecondaryText(entry.getKey(), entry.getValue());
            }
            return this;
        }

        /** Builds a {@link TelephoneKeypadTemplate} */
        @NonNull
        public TelephoneKeypadTemplate build() {
            if (mHeader != null) {
                Action startHeaderAction = mHeader.getStartHeaderAction();
                if (startHeaderAction != null) {
                    ACTION_CONSTRAINTS_TELEPHONE_KEYPAD_HEADER.validateOrThrow(
                            Collections.singletonList(startHeaderAction));
                }
            }

            return new TelephoneKeypadTemplate(this);
        }
    }
}
