/*
 * Copyright (C) 2021 The Android Open Source Project
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

package androidx.car.app.sample.showcase.common.screens.templatelayouts;

import static android.text.Spanned.SPAN_INCLUSIVE_INCLUSIVE;

import static androidx.car.app.CarToast.LENGTH_LONG;

import android.graphics.Color;
import android.net.Uri;
import android.text.SpannableStringBuilder;

import androidx.activity.OnBackPressedCallback;
import androidx.car.app.CarContext;
import androidx.car.app.CarToast;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.CarColor;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.ForegroundCarColorSpan;
import androidx.car.app.model.Header;
import androidx.car.app.model.InputCallback;
import androidx.car.app.model.MessageTemplate;
import androidx.car.app.model.ParkedOnlyOnClickListener;
import androidx.car.app.model.Template;
import androidx.car.app.model.signin.InputSignInMethod;
import androidx.car.app.model.signin.PinSignInMethod;
import androidx.car.app.model.signin.ProviderSignInMethod;
import androidx.car.app.model.signin.QRCodeSignInMethod;
import androidx.car.app.model.signin.SignInTemplate;
import androidx.car.app.sample.showcase.common.R;
import androidx.car.app.sample.showcase.common.common.SpannableStringBuilderAnnotationExtensions;
import androidx.car.app.sample.showcase.common.screens.templatelayouts.messagetemplates.LongMessageTemplateDemoScreen;
import androidx.car.app.versioning.CarAppApiLevels;
import androidx.core.graphics.drawable.IconCompat;

import kotlin.Unit;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** A screen that demonstrates the sign-in template. */
public class SignInTemplateDemoScreen extends Screen {
    private static final String EMAIL_REGEXP = "^(.+)@(.+)$";
    private static final String EXPECTED_PASSWORD = "password";
    private static final int MIN_USERNAME_LENGTH = 5;
    private final CharSequence mAdditionalText;
    private final Action mProviderSignInAction;
    private final Action mPinSignInAction;
    private final Action mQRCodeSignInAction;
    // package private to avoid synthetic accessor
    State mState = State.USERNAME;
    @Nullable String mLastErrorMessage; // last displayed error message
    @Nullable String mErrorMessage;

    @Nullable String mUsername;

    public SignInTemplateDemoScreen(@NonNull CarContext carContext) {
        super(carContext);

        // Handle back pressed events manually, as we use them to navigate between templates within
        // the same screen.
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                mErrorMessage = "";
                if (mState == State.USERNAME || mState == State.SIGNED_IN) {
                    getScreenManager().pop();
                } else {
                    mState = State.USERNAME;
                    invalidate();
                }
            }
        };
        carContext.getOnBackPressedDispatcher().addCallback(this, callback);

        SpannableStringBuilder additionalText =
                SpannableStringBuilderAnnotationExtensions.getSpannableStringBuilder(
                        getCarContext(), R.string.additional_text);
        SpannableStringBuilderAnnotationExtensions.addSpanToAnnotatedPosition(
                additionalText,
                "link",
                "terms_of_service",
                () -> {
                    getScreenManager().push(new LongMessageTemplateDemoScreen(getCarContext()));
                    return Unit.INSTANCE;
                }
        );
        mAdditionalText = additionalText;

        mProviderSignInAction = new Action.Builder()
                .setTitle(getCarContext().getString(R.string.google_sign_in))
                .setOnClickListener(ParkedOnlyOnClickListener.create(() -> {
                    mState = State.PROVIDER;
                    invalidate();
                }))
                .build();

        mPinSignInAction = new Action.Builder()
                .setTitle(getCarContext().getString(R.string.use_pin))
                .setOnClickListener(ParkedOnlyOnClickListener.create(() -> {
                    mState = State.PIN;
                    invalidate();
                }))
                .build();

        mQRCodeSignInAction = new Action.Builder()
                .setTitle(getCarContext().getString(R.string.qr_code))
                .setOnClickListener(ParkedOnlyOnClickListener.create(() -> {
                    mState = State.QR_CODE;
                    invalidate();
                }))
                .build();
    }

    @Override
    public @NonNull Template onGetTemplate() {
        if (getCarContext().getCarAppApiLevel() < CarAppApiLevels.LEVEL_2) {
            return new MessageTemplate.Builder(
                    getCarContext().getString(R.string.sign_in_template_not_supported_text))
                    .setHeader(new Header.Builder().setTitle(getCarContext().getString(
                                    R.string.sign_in_template_not_supported_title))
                            .setStartHeaderAction(Action.BACK).build())
                    .build();
        }
        switch (mState) {
            case USERNAME:
                return getUsernameSignInTemplate();
            case PASSWORD:
                return getPasswordSignInTemplate();
            case PIN:
                return getPinSignInTemplate();
            case PROVIDER:
                return getProviderSignInTemplate();
            case QR_CODE:
                return getQRCodeSignInTemplate();
            case SIGNED_IN:
                return getSignInCompletedMessageTemplate();
        }
        throw new IllegalStateException("Invalid state: " + mState);
    }

    private Template getUsernameSignInTemplate() {
        InputCallback listener = new InputCallback() {
            @Override
            public void onInputSubmitted(@NonNull String text) {
                if (mState == State.USERNAME) {
                    mUsername = text;
                    submitUsername();
                }
            }

            @Override
            public void onInputTextChanged(@NonNull String text) {
                // This callback demonstrates how to use handle the text changed event.
                // In this case, we check that the user name doesn't exceed a certain length.
                if (mState == State.USERNAME) {
                    mUsername = text;
                    mErrorMessage = validateUsername();

                    // Invalidate the template (and hence possibly update the error message) only
                    // if clearing up the error string, or if the error is changing.
                    if ((mLastErrorMessage != null && !mLastErrorMessage.isEmpty())
                            && (mErrorMessage.isEmpty()
                            || !mLastErrorMessage.equals(mErrorMessage))) {
                        invalidate();
                    }
                }
            }
        };

        InputSignInMethod.Builder builder = new InputSignInMethod.Builder(listener)
                .setHint(getCarContext().getString(R.string.email_hint))
                .setKeyboardType(InputSignInMethod.KEYBOARD_EMAIL);
        if (mErrorMessage != null) {
            builder.setErrorMessage(mErrorMessage);
            mLastErrorMessage = mErrorMessage;
        }
        if (mUsername != null) {
            builder.setDefaultValue(mUsername);
        }
        InputSignInMethod signInMethod = builder.build();

        return new SignInTemplate.Builder(signInMethod)
                .addAction(mProviderSignInAction)
                .addAction(getCarContext().getCarAppApiLevel() > CarAppApiLevels.LEVEL_3
                        ? mQRCodeSignInAction : mPinSignInAction)
                .setTitle(getCarContext().getString(R.string.sign_in_title))
                .setInstructions(getCarContext().getString(R.string.sign_in_instructions))
                .setHeaderAction(Action.BACK)
                .setAdditionalText(mAdditionalText)
                .build();
    }

    /**
     * Validates the currently entered user name and returns an error message string if invalid,
     * or an empty string otherwise.
     */
    String validateUsername() {
        if (mUsername == null || mUsername.length() < MIN_USERNAME_LENGTH) {
            return getCarContext().getString(R.string.invalid_length_error_msg,
                    Integer.toString(MIN_USERNAME_LENGTH));
        } else if (!mUsername.matches(EMAIL_REGEXP)) {
            return getCarContext().getString(R.string.invalid_email_error_msg);
        } else {
            return "";
        }
    }

    /**
     * Moves to the password screen if the user name currently entered is valid, or displays
     * an error message otherwise.
     */
    void submitUsername() {
        mErrorMessage = validateUsername();

        boolean isError = !mErrorMessage.isEmpty();
        if (!isError) {
            // If there's no error, go to the password screen.
            mState = State.PASSWORD;
        }

        // Invalidate the template so that we either display an error, or go to the password screen.
        invalidate();
    }

    private Template getPasswordSignInTemplate() {
        InputCallback callback = new InputCallback() {
            @Override
            public void onInputSubmitted(@NonNull String text) {
                // Mocked password validation
                if (!EXPECTED_PASSWORD.equals(text)) {
                    mErrorMessage = getCarContext().getString(R.string.invalid_password_error_msg);
                } else {
                    mErrorMessage = "";
                    mState = State.SIGNED_IN;
                }
                invalidate();
            }
        };
        InputSignInMethod.Builder builder = new InputSignInMethod.Builder(callback)
                .setHint(getCarContext().getString(R.string.password_hint))
                .setInputType(InputSignInMethod.INPUT_TYPE_PASSWORD);
        if (mErrorMessage != null) {
            builder.setErrorMessage(mErrorMessage);
        }
        InputSignInMethod signInMethod = builder.build();

        return new SignInTemplate.Builder(signInMethod)
                .addAction(mProviderSignInAction)
                .addAction(getCarContext().getCarAppApiLevel() > CarAppApiLevels.LEVEL_3
                        ? mQRCodeSignInAction : mPinSignInAction)
                .setTitle(getCarContext().getString(R.string.sign_in_title))
                .setInstructions(
                        getCarContext().getString(R.string.password_sign_in_instruction_prefix)
                                + ": " + mUsername)
                .setHeaderAction(Action.BACK)
                .setAdditionalText(mAdditionalText)
                .build();
    }

    private Template getPinSignInTemplate() {
        PinSignInMethod pinSignInMethod = new PinSignInMethod("123456789ABC");
        return new SignInTemplate.Builder(pinSignInMethod)
                .setTitle(getCarContext().getString(R.string.sign_in_title))
                .setInstructions(getCarContext().getString(R.string.pin_sign_in_instruction))
                .setHeaderAction(Action.BACK)
                .setAdditionalText(mAdditionalText)
                .build();
    }

    private Template getQRCodeSignInTemplate() {
        QRCodeSignInMethod qrCodeSignInMethod = new QRCodeSignInMethod(Uri.parse("https://www"
                + ".youtube.com/watch?v=dQw4w9WgXcQ"));
        return new SignInTemplate.Builder(qrCodeSignInMethod)
                .setTitle(getCarContext().getString(R.string.qr_code_sign_in_title))
                .setHeaderAction(Action.BACK)
                .setAdditionalText(mAdditionalText)
                .addAction(mPinSignInAction)
                .addAction(mProviderSignInAction)
                .build();
    }

    private Template getProviderSignInTemplate() {
        IconCompat providerIcon = IconCompat.createWithResource(getCarContext(),
                R.drawable.ic_googleg);
        CarColor noTint = CarColor.createCustom(Color.TRANSPARENT, Color.TRANSPARENT);

        SpannableStringBuilder title = new SpannableStringBuilder()
                .append(
                        getCarContext().getString(R.string.sign_in_with_google_title),
                        ForegroundCarColorSpan.create(
                                CarColor.createCustom(Color.BLACK, Color.BLACK)),
                        SPAN_INCLUSIVE_INCLUSIVE
                );

        ProviderSignInMethod providerSignInMethod = new ProviderSignInMethod(
                new Action.Builder()
                        .setTitle(title)
                        .setBackgroundColor(CarColor.createCustom(Color.WHITE, Color.WHITE))
                        .setIcon(new CarIcon.Builder(providerIcon)
                                .setTint(noTint)
                                .build())
                        .setOnClickListener(ParkedOnlyOnClickListener.create(
                                this::performSignInWithGoogleFlow)).build());

        return new SignInTemplate.Builder(providerSignInMethod)
                .setTitle(getCarContext().getString(R.string.sign_in_title))
                .setInstructions(getCarContext().getString(R.string.provider_sign_in_instruction))
                .setHeaderAction(Action.BACK)
                .setAdditionalText(mAdditionalText)
                .build();
    }

    private void performSignInWithGoogleFlow() {
        // This is here for demonstration purposes, if the APK is not signed with a signature
        // that has been registered for sign in with Google flow, the sign in will fail at runtime.

//        Bundle extras = new Bundle(1);
//        extras.putBinder(BINDER_KEY, new SignInWithGoogleActivity.OnSignInComplete() {
//            @Override
//            public void onSignInComplete(@Nullable GoogleSignInAccount account) {
//                if (account == null) {
//                    CarToast.makeText(getCarContext(), "Error signing in", LENGTH_LONG).show();
//                    return;
//                }
//
//                // Use the account
//                CarToast.makeText(getCarContext(),
//                        account.getGivenName() + " signed in", LENGTH_LONG).show();
//            }
//        });
//        getCarContext().startActivity(
//                new Intent()
//                        .setClass(getCarContext(), SignInWithGoogleActivity.class)
//                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//                        .putExtras(extras));
        CarToast.makeText(getCarContext(),
                        getCarContext().getString(R.string.sign_in_with_google_toast_msg),
                        LENGTH_LONG)
                .show();
    }

    private MessageTemplate getSignInCompletedMessageTemplate() {
        return new MessageTemplate.Builder(
                getCarContext().getString(R.string.sign_in_complete_text))
                .setHeader(new Header.Builder().setStartHeaderAction(Action.BACK)
                        .setTitle(getCarContext().getString(R.string.sign_in_complete_title))
                        .build())
                .addAction(new Action.Builder()
                        .setTitle(getCarContext().getString(R.string.sign_out_action_title))
                        .setOnClickListener(() -> {
                            mState = State.USERNAME;
                            invalidate();
                        })
                        .build())
                .build();
    }

    private enum State {
        USERNAME,
        PASSWORD,
        PIN,
        PROVIDER,
        QR_CODE,
        SIGNED_IN,
    }

}
