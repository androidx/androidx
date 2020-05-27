/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.biometric;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import java.lang.annotation.Retention;
import java.security.Signature;
import java.util.concurrent.Executor;

import javax.crypto.Cipher;
import javax.crypto.Mac;

/**
 * A class that manages a system-provided biometric prompt. On devices running P and above, this
 * will show a system-provided authentication prompt, using a device's supported biometric
 * (fingerprint, iris, face, etc). On devices before P, this will show a dialog prompting for
 * fingerprint authentication. The prompt will persist across configuration changes unless
 * explicitly canceled by the client. For security reasons, the prompt will automatically dismiss
 * when the application is no longer in the foreground.
 *
 * To persist authentication across configuration changes, developers should (re)create the
 * BiometricPrompt every time the activity/fragment is created. Instantiating the library with a new
 * callback early in the fragment/activity lifecycle (e.g. onCreate) allows the ongoing authenticate
 * session's callbacks to be received by the new fragment/activity. Note that
 * {@link BiometricPrompt#cancelAuthentication()} should not be called, and
 * {@link BiometricPrompt#authenticate(PromptInfo)} or
 * {@link BiometricPrompt#authenticate(PromptInfo, CryptoObject)} does not need to be invoked after
 * the new activity/fragment is created, since we are keeping/continuing the same session.
 */
public class BiometricPrompt implements BiometricConstants {
    private static final String TAG = "BiometricPromptCompat";

    static final String BIOMETRIC_FRAGMENT_TAG = "BiometricFragment";

    static final String KEY_TITLE = "title";
    static final String KEY_SUBTITLE = "subtitle";
    static final String KEY_DESCRIPTION = "description";
    static final String KEY_NEGATIVE_TEXT = "negative_text";
    static final String KEY_REQUIRE_CONFIRMATION = "require_confirmation";
    static final String KEY_ALLOW_DEVICE_CREDENTIAL = "allow_device_credential";

    @Retention(SOURCE)
    @IntDef({ERROR_HW_UNAVAILABLE,
            ERROR_UNABLE_TO_PROCESS,
            ERROR_TIMEOUT,
            ERROR_NO_SPACE,
            ERROR_CANCELED,
            ERROR_LOCKOUT,
            ERROR_VENDOR,
            ERROR_LOCKOUT_PERMANENT,
            ERROR_USER_CANCELED,
            ERROR_NO_BIOMETRICS,
            ERROR_HW_NOT_PRESENT,
            ERROR_NEGATIVE_BUTTON,
            ERROR_NO_DEVICE_CREDENTIAL})
    private @interface BiometricError {
    }

    /**
     * A wrapper class for the crypto objects supported by BiometricPrompt. Currently the
     * framework supports {@link Signature}, {@link Cipher}, and {@link Mac} objects.
     */
    public static class CryptoObject {
        private final Signature mSignature;
        private final Cipher mCipher;
        private final Mac mMac;

        public CryptoObject(@NonNull Signature signature) {
            mSignature = signature;
            mCipher = null;
            mMac = null;
        }

        public CryptoObject(@NonNull Cipher cipher) {
            mCipher = cipher;
            mSignature = null;
            mMac = null;
        }

        public CryptoObject(@NonNull Mac mac) {
            mMac = mac;
            mCipher = null;
            mSignature = null;
        }

        /**
         * Get {@link Signature} object.
         *
         * @return {@link Signature} object or null if this doesn't contain one.
         */
        @Nullable
        public Signature getSignature() {
            return mSignature;
        }

        /**
         * Get {@link Cipher} object.
         *
         * @return {@link Cipher} object or null if this doesn't contain one.
         */
        @Nullable
        public Cipher getCipher() {
            return mCipher;
        }

        /**
         * Get {@link Mac} object.
         *
         * @return {@link Mac} object or null if this doesn't contain one.
         */
        @Nullable
        public Mac getMac() {
            return mMac;
        }
    }

    /**
     * Container for callback data from {@link #authenticate(PromptInfo)} and
     * {@link #authenticate(PromptInfo, CryptoObject)}.
     */
    public static class AuthenticationResult {
        private final CryptoObject mCryptoObject;

        /**
         *
         */
        AuthenticationResult(CryptoObject crypto) {
            mCryptoObject = crypto;
        }

        /**
         * Obtain the crypto object associated with this transaction
         *
         * @return crypto object provided to {@link #authenticate(PromptInfo, CryptoObject)}.
         */
        @Nullable
        public CryptoObject getCryptoObject() {
            return mCryptoObject;
        }
    }

    /**
     * Callback structure provided to {@link BiometricPrompt}. Users of {@link
     * BiometricPrompt} must provide an implementation of this for listening to
     * fingerprint events.
     */
    public abstract static class AuthenticationCallback {
        /**
         * Called when an unrecoverable error has been encountered and the operation is complete.
         * No further actions will be made on this object.
         *
         * @param errorCode An integer identifying the error message. The error message will usually
         *                  be one of the BIOMETRIC_ERROR constants.
         * @param errString A human-readable error string that can be shown on an UI
         */
        public void onAuthenticationError(@BiometricError int errorCode,
                @NonNull CharSequence errString) {
        }

        /**
         * Called when a biometric is recognized.
         *
         * @param result An object containing authentication-related data
         */
        public void onAuthenticationSucceeded(@NonNull AuthenticationResult result) {
        }

        /**
         * Called when a biometric is valid but not recognized.
         */

        public void onAuthenticationFailed() {
        }
    }

    /**
     * A class that contains a builder which returns the {@link PromptInfo} to be used in
     * {@link #authenticate(PromptInfo, CryptoObject)} and {@link #authenticate(PromptInfo)}.
     */
    public static class PromptInfo {

        /**
         * A builder that collects arguments to be shown on the system-provided biometric dialog.
         */
        public static class Builder {
            private final Bundle mBundle = new Bundle();

            /**
             * Required: Set the title to display.
             */
            @NonNull
            public Builder setTitle(@NonNull CharSequence title) {
                mBundle.putCharSequence(KEY_TITLE, title);
                return this;
            }

            /**
             * Optional: Set the subtitle to display.
             */
            @NonNull
            public Builder setSubtitle(@Nullable CharSequence subtitle) {
                mBundle.putCharSequence(KEY_SUBTITLE, subtitle);
                return this;
            }

            /**
             * Optional: Set the description to display.
             */
            @NonNull
            public Builder setDescription(@Nullable CharSequence description) {
                mBundle.putCharSequence(KEY_DESCRIPTION, description);
                return this;
            }

            /**
             * Required: Set the text for the negative button. This would typically be used as a
             * "Cancel" button, but may be also used to show an alternative method for
             * authentication, such as screen that asks for a backup password.
             */
            @NonNull
            public Builder setNegativeButtonText(@NonNull CharSequence text) {
                mBundle.putCharSequence(KEY_NEGATIVE_TEXT, text);
                return this;
            }

            /**
             * Optional: A hint to the system to require user confirmation after a biometric has
             * been authenticated. For example, implicit modalities like Face and
             * Iris authentication are passive, meaning they don't require an explicit user action
             * to complete. When set to 'false', the user action (e.g. pressing a button)
             * will not be required. BiometricPrompt will require confirmation by default.
             *
             * A typical use case for not requiring confirmation would be for low-risk transactions,
             * such as re-authenticating a recently authenticated application. A typical use case
             * for requiring confirmation would be for authorizing a purchase.
             *
             * Note that this is a hint to the system. The system may choose to ignore the flag. For
             * example, if the user disables implicit authentication in Settings, or if it does not
             * apply to a modality (e.g. Fingerprint). When ignored, the system will default to
             * requiring confirmation.
             *
             * This method only applies to Q and above.
             */
            @NonNull
            public Builder setConfirmationRequired(boolean requireConfirmation) {
                mBundle.putBoolean(KEY_REQUIRE_CONFIRMATION, requireConfirmation);
                return this;
            }

            /**
             * The user will first be prompted to authenticate with biometrics, but also given the
             * option to authenticate with their device PIN, pattern, or password. Developers should
             * first check {@link android.app.KeyguardManager#isDeviceSecure()} before enabling
             * this. If the device is not secure, {@link BiometricPrompt#ERROR_NO_DEVICE_CREDENTIAL}
             * will be returned in
             * {@link AuthenticationCallback#onAuthenticationError(int, CharSequence)}.
             *
             * <p>Note that {@link Builder#setNegativeButtonText(CharSequence)} should not be set
             * if this is set to true.
             *
             * <p>On versions P and below, once the device credential prompt is shown,
             * {@link #cancelAuthentication()} will not work, since the library internally launches
             * {@link android.app.KeyguardManager#createConfirmDeviceCredentialIntent(CharSequence,
             * CharSequence)}, which does not have a public API for cancellation.
             *
             * @param enable When true, the prompt will fall back to ask for the user's device
             *               credentials (PIN, pattern, or password).
             */
            @NonNull
            public Builder setDeviceCredentialAllowed(boolean enable) {
                mBundle.putBoolean(KEY_ALLOW_DEVICE_CREDENTIAL, enable);
                return this;
            }

            /**
             * Creates a {@link BiometricPrompt}.
             *
             * @return a {@link BiometricPrompt}
             * @throws IllegalArgumentException if any of the required fields are not set.
             */
            @NonNull
            public PromptInfo build() {
                final CharSequence title = mBundle.getCharSequence(KEY_TITLE);
                final CharSequence negative = mBundle.getCharSequence(KEY_NEGATIVE_TEXT);
                boolean allowDeviceCredential = mBundle.getBoolean(KEY_ALLOW_DEVICE_CREDENTIAL);

                if (TextUtils.isEmpty(title)) {
                    throw new IllegalArgumentException("Title must be set and non-empty");
                }
                if (TextUtils.isEmpty(negative) && !allowDeviceCredential) {
                    throw new IllegalArgumentException("Negative text must be set and non-empty");
                }
                if (!TextUtils.isEmpty(negative) && allowDeviceCredential) {
                    throw new IllegalArgumentException("Can't have both negative button behavior"
                            + " and device credential enabled");
                }
                return new PromptInfo(mBundle);
            }
        }

        private Bundle mBundle;

        PromptInfo(Bundle bundle) {
            mBundle = bundle;
        }

        Bundle getBundle() {
            return mBundle;
        }

        /**
         * @return See {@link Builder#setTitle(CharSequence)}.
         */
        @NonNull
        public CharSequence getTitle() {
            return mBundle.getCharSequence(KEY_TITLE);
        }

        /**
         * @return See {@link Builder#setSubtitle(CharSequence)}.
         */
        @Nullable
        public CharSequence getSubtitle() {
            return mBundle.getCharSequence(KEY_SUBTITLE);
        }

        /**
         * @return See {@link Builder#setDescription(CharSequence)}.
         */
        @Nullable
        public CharSequence getDescription() {
            return mBundle.getCharSequence(KEY_DESCRIPTION);
        }

        /**
         * @return See {@link Builder#setNegativeButtonText(CharSequence)}.
         */
        @NonNull
        public CharSequence getNegativeButtonText() {
            return mBundle.getCharSequence(KEY_NEGATIVE_TEXT);
        }

        /**
         * @return See {@link Builder#setConfirmationRequired(boolean)}.
         */
        public boolean isConfirmationRequired() {
            return mBundle.getBoolean(KEY_REQUIRE_CONFIRMATION);
        }

        /**
         * @return See {@link Builder#setDeviceCredentialAllowed(boolean)}.
         */
        public boolean isDeviceCredentialAllowed() {
            return mBundle.getBoolean(KEY_ALLOW_DEVICE_CREDENTIAL);
        }
    }

    // Fragment attached to the client activity that coordinates logic for the prompt.
    private BiometricFragment mBiometricFragment;

    /**
     * Constructs a {@link BiometricPrompt} which can be used to prompt the user for
     * authentication. The authentication prompt created by
     * {@link BiometricPrompt#authenticate(PromptInfo, CryptoObject)} and
     * {@link BiometricPrompt#authenticate(PromptInfo)} will persist across device
     * configuration changes by default. If authentication is in progress, re-creating
     * the {@link BiometricPrompt} can be used to update the {@link Executor} and
     * {@link AuthenticationCallback}. This should be used to update the
     * {@link AuthenticationCallback} after configuration changes.
     *
     * @param fragmentActivity A reference to the client's activity.
     * @param executor         An executor to handle callback events.
     * @param callback         An object to receive authentication events.
     */
    @SuppressLint("LambdaLast")
    @SuppressWarnings("ConstantConditions")
    public BiometricPrompt(
            @NonNull FragmentActivity fragmentActivity,
            @NonNull Executor executor,
            @NonNull AuthenticationCallback callback) {

        if (fragmentActivity == null) {
            throw new IllegalArgumentException("FragmentActivity must not be null");
        }
        if (executor == null) {
            throw new IllegalArgumentException("Executor must not be null");
        }
        if (callback == null) {
            throw new IllegalArgumentException("AuthenticationCallback must not be null");
        }

        mBiometricFragment = getBiometricFragment(fragmentActivity.getSupportFragmentManager());
        mBiometricFragment.setClientActivity(fragmentActivity);
        mBiometricFragment.setClientCallback(executor, callback);
    }

    /**
     * Constructs a {@link BiometricPrompt} which can be used to prompt the user for
     * authentication. The authentication prompt created by
     * {@link BiometricPrompt#authenticate(PromptInfo, CryptoObject)} and
     * {@link BiometricPrompt#authenticate(PromptInfo)} will persist across device
     * configuration changes by default. If authentication is in progress, re-creating
     * the {@link BiometricPrompt} can be used to update the {@link Executor} and
     * {@link AuthenticationCallback}. This should be used to update the
     * {@link AuthenticationCallback} after configuration changes.
     *
     * @param fragment A reference to the client's fragment.
     * @param executor An executor to handle callback events.
     * @param callback An object to receive authentication events.
     */
    @SuppressLint("LambdaLast")
    @SuppressWarnings("ConstantConditions")
    public BiometricPrompt(
            @NonNull Fragment fragment,
            @NonNull Executor executor,
            @NonNull AuthenticationCallback callback) {

        if (fragment == null) {
            throw new IllegalArgumentException("FragmentActivity must not be null");
        }
        if (executor == null) {
            throw new IllegalArgumentException("Executor must not be null");
        }
        if (callback == null) {
            throw new IllegalArgumentException("AuthenticationCallback must not be null");
        }

        mBiometricFragment = getBiometricFragment(fragment.getChildFragmentManager());
        mBiometricFragment.setClientFragment(fragment);
        mBiometricFragment.setClientCallback(executor, callback);
    }

    /**
     * Shows the biometric prompt. The prompt survives lifecycle changes by default. To cancel the
     * authentication, use {@link #cancelAuthentication()}.
     *
     * @param info   The information that will be displayed on the prompt. Create this object using
     *               {@link BiometricPrompt.PromptInfo.Builder}.
     * @param crypto The crypto object associated with the authentication.
     */
    @SuppressWarnings("ConstantConditions")
    public void authenticate(@NonNull PromptInfo info, @NonNull CryptoObject crypto) {
        if (info == null) {
            throw new IllegalArgumentException("PromptInfo can not be null");
        } else if (crypto == null) {
            throw new IllegalArgumentException("CryptoObject can not be null");
        } else if (info.getBundle().getBoolean(KEY_ALLOW_DEVICE_CREDENTIAL)) {
            throw new IllegalArgumentException("Device credential not supported with crypto");
        }

        if (mBiometricFragment == null) {
            Log.e(TAG, "Unable to authenticate; BiometricFragment was null");
        } else {
            mBiometricFragment.authenticate(info, crypto);
        }
    }

    /**
     * Shows the biometric prompt. The prompt survives lifecycle changes by default. To cancel the
     * authentication, use {@link #cancelAuthentication()}.
     *
     * @param info The information that will be displayed on the prompt. Create this object using
     *             {@link BiometricPrompt.PromptInfo.Builder}.
     */
    @SuppressWarnings("ConstantConditions")
    public void authenticate(@NonNull PromptInfo info) {
        if (info == null) {
            throw new IllegalArgumentException("PromptInfo can not be null");
        }

        if (mBiometricFragment == null) {
            Log.e(TAG, "Unable to authenticate; BiometricFragment was null");
        } else {
            mBiometricFragment.authenticate(info, null /* crypto */);
        }
    }

    /**
     * Cancels the biometric authentication, and dismisses the dialog upon confirmation from the
     * biometric service.
     *
     * <p>On P or below, calling this method when the device credential prompt is shown will NOT
     * work as expected. See {@link PromptInfo.Builder#setDeviceCredentialAllowed(boolean)} for more
     * details.
     */
    public void cancelAuthentication() {
        if (mBiometricFragment == null) {
            Log.e(TAG, "Unable to cancel authentication; BiometricFragment was null");
        } else {
            mBiometricFragment.cancel(BiometricFragment.USER_CANCELED_FROM_NONE);
        }
    }

    @NonNull
    private static BiometricFragment getBiometricFragment(FragmentManager fragmentManager) {
        final BiometricFragment biometricFragment =
                (BiometricFragment) fragmentManager.findFragmentByTag(BIOMETRIC_FRAGMENT_TAG);
        return biometricFragment != null ? biometricFragment : BiometricFragment.newInstance();
    }
}
