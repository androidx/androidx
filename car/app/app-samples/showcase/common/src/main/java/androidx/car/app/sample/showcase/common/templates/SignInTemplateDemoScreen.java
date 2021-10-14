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

package androidx.car.app.sample.showcase.common.templates;

import static androidx.car.app.CarToast.LENGTH_LONG;

import android.graphics.Color;
import android.net.Uri;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.car.app.CarContext;
import androidx.car.app.CarToast;
import androidx.car.app.Screen;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.model.Action;
import androidx.car.app.model.CarColor;
import androidx.car.app.model.CarIcon;
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
import androidx.car.app.sample.showcase.common.common.Utils;
import androidx.car.app.versioning.CarAppApiLevels;
import androidx.core.graphics.drawable.IconCompat;

/** A screen that demonstrates the sign-in template. */
public class SignInTemplateDemoScreen extends Screen {
    private enum State {
        USERNAME,
        PASSWORD,
        PIN,
        PROVIDER,
        QR_CODE,
        SIGNED_IN,
    }

    private static final String EMAIL_REGEXP = "^(.+)@(.+)$";
    private static final String EXPECTED_PASSWORD = "password";
    private static final int MIN_USERNAME_LENGTH = 5;

    // package private to avoid synthetic accessor
    State mState = State.USERNAME;
    String mLastErrorMessage = ""; // last displayed error message
    String mErrorMessage = "";
    String mUsername = null;

    private final CharSequence mAdditionalText = Utils.clickable("Please review our terms of "
                    + "service", 18, 16,
            () -> getScreenManager().push(new LongMessageTemplateDemoScreen(getCarContext())));

    private final Action mProviderSignInAction = new Action.Builder()
            .setTitle("Google Sign-In")
            .setOnClickListener(ParkedOnlyOnClickListener.create(() -> {
                mState = State.PROVIDER;
                invalidate();
            }))
            .build();

    private final Action mPinSignInAction = new Action.Builder()
            .setTitle("Use PIN")
            .setOnClickListener(ParkedOnlyOnClickListener.create(() -> {
                mState = State.PIN;
                invalidate();
            }))
            .build();

    private final Action mQRCodeSignInAction = new Action.Builder()
            .setTitle("QR Code")
            .setOnClickListener(ParkedOnlyOnClickListener.create(() -> {
                mState = State.QR_CODE;
                invalidate();
            }))
            .build();

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
    }

    @NonNull
    @Override
    public Template onGetTemplate() {
        if (getCarContext().getCarAppApiLevel() < CarAppApiLevels.LEVEL_2) {
            return new MessageTemplate.Builder("Your host doesn't support Sign In template")
                    .setTitle("Incompatible host")
                    .setHeaderAction(Action.BACK)
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
                    if (!mLastErrorMessage.isEmpty()
                            && (mErrorMessage.isEmpty()
                            || !mLastErrorMessage.equals(mErrorMessage))) {
                        invalidate();
                    }
                }
            }
        };

        InputSignInMethod.Builder builder = new InputSignInMethod.Builder(listener)
                .setHint("Email")
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
                .setTitle("Sign in")
                .setInstructions("Enter your credentials")
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
            return "User name must be at least " + MIN_USERNAME_LENGTH + " characters "
                    + "long";
        } else if (!mUsername.matches(EMAIL_REGEXP)) {
            return "User name must be a valid email address";
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
        } else {
            // If there's an error, display a toast.
            CarToast.makeText(getCarContext(), "Please enter a valid user name",
                    LENGTH_LONG).show();
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
                    mErrorMessage = "Invalid password";
                } else {
                    mErrorMessage = "";
                    mState = State.SIGNED_IN;
                }
                invalidate();
            }
        };
        InputSignInMethod.Builder builder = new InputSignInMethod.Builder(callback)
                .setHint("Password")
                .setInputType(InputSignInMethod.INPUT_TYPE_PASSWORD);
        if (mErrorMessage != null) {
            builder.setErrorMessage(mErrorMessage);
        }
        InputSignInMethod signInMethod = builder.build();

        return new SignInTemplate.Builder(signInMethod)
                .addAction(mProviderSignInAction)
                .addAction(getCarContext().getCarAppApiLevel() > CarAppApiLevels.LEVEL_3
                        ? mQRCodeSignInAction : mPinSignInAction)
                .setTitle("Sign in")
                .setInstructions("Username: " + mUsername)
                .setHeaderAction(Action.BACK)
                .setAdditionalText(mAdditionalText)
                .build();
    }

    private Template getPinSignInTemplate() {
        PinSignInMethod pinSignInMethod = new PinSignInMethod("123456789ABC");
        return new SignInTemplate.Builder(pinSignInMethod)
                .setTitle("Sign in")
                .setInstructions("Type this PIN in your phone")
                .setHeaderAction(Action.BACK)
                .setAdditionalText(mAdditionalText)
                .build();
    }

    @OptIn(markerClass = ExperimentalCarApi.class)
    private Template getQRCodeSignInTemplate() {
        QRCodeSignInMethod qrCodeSignInMethod = new QRCodeSignInMethod(Uri.parse("https://www"
                + ".youtube.com/watch?v=dQw4w9WgXcQ"));
        return new SignInTemplate.Builder(qrCodeSignInMethod)
                .setTitle("Scan QR Code to sign in")
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

        ProviderSignInMethod providerSignInMethod = new ProviderSignInMethod(
                new Action.Builder()
                        .setTitle(Utils.colorize("Sign in with Google",
                                CarColor.createCustom(Color.BLACK, Color.BLACK), 0, 19))
                        .setBackgroundColor(CarColor.createCustom(Color.WHITE, Color.WHITE))
                        .setIcon(new CarIcon.Builder(providerIcon)
                                .setTint(noTint)
                                .build())
                        .setOnClickListener(ParkedOnlyOnClickListener.create(
                                this::performSignInWithGoogleFlow)).build());

        return new SignInTemplate.Builder(providerSignInMethod)
                .setTitle("Sign in")
                .setInstructions("Use this button to complete your Google sign-in")
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
        CarToast.makeText(getCarContext(), "Sign-in with Google starts here", LENGTH_LONG)
                .show();
    }

    private MessageTemplate getSignInCompletedMessageTemplate() {
        return new MessageTemplate.Builder("You are signed in!")
                .setTitle("Sign in completed")
                .setHeaderAction(Action.BACK)
                .addAction(new Action.Builder()
                        .setTitle("Sign out")
                        .setOnClickListener(() -> {
                            mState = State.USERNAME;
                            invalidate();
                        })
                        .build())
                .build();
    }

}
