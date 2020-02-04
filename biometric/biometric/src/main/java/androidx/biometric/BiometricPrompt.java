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
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

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
@SuppressLint("SyntheticAccessor")
public class BiometricPrompt implements BiometricConstants {

    private static final String TAG = "BiometricPromptCompat";
    private static final boolean DEBUG = false;
    // In order to keep consistent behavior between versions, we need to send
    // FingerprintDialogFragment a message indicating whether or not to dismiss the UI instantly.
    private static final int DELAY_MILLIS = 500;
    // For debugging fingerprint dialog only. Must never be checked in as `true`.
    private static final boolean DEBUG_FORCE_FINGERPRINT = false;

    static final String DIALOG_FRAGMENT_TAG = "FingerprintDialogFragment";
    static final String FINGERPRINT_HELPER_FRAGMENT_TAG = "FingerprintHelperFragment";
    static final String BIOMETRIC_FRAGMENT_TAG = "BiometricFragment";
    static final String KEY_TITLE = "title";
    static final String KEY_SUBTITLE = "subtitle";
    static final String KEY_DESCRIPTION = "description";
    static final String KEY_NEGATIVE_TEXT = "negative_text";
    static final String KEY_REQUIRE_CONFIRMATION = "require_confirmation";
    static final String KEY_ALLOW_DEVICE_CREDENTIAL = "allow_device_credential";
    static final String KEY_HANDLING_DEVICE_CREDENTIAL_RESULT = "handling_device_credential_result";

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
             * A flag that is set to true when launching the prompt within the transparent
             * {@link DeviceCredentialHandlerActivity}. This lets us handle the result of {@link
             * android.app.KeyguardManager#createConfirmDeviceCredentialIntent(CharSequence,
             * CharSequence)} in order to allow device credentials for <= P.
             */
            @NonNull
            Builder setHandlingDeviceCredentialResult(boolean isHandling) {
                mBundle.putBoolean(KEY_HANDLING_DEVICE_CREDENTIAL_RESULT, isHandling);
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
                boolean handlingDeviceCredentialResult =
                        mBundle.getBoolean(KEY_HANDLING_DEVICE_CREDENTIAL_RESULT);

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
                if (handlingDeviceCredentialResult && !allowDeviceCredential) {
                    throw new IllegalArgumentException("Can't be handling device credential result"
                            + " without device credential enabled");
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

        /**
         * @return See {@link Builder#setHandlingDeviceCredentialResult(boolean)}.
         *
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        boolean isHandlingDeviceCredentialResult() {
            return mBundle.getBoolean(KEY_HANDLING_DEVICE_CREDENTIAL_RESULT);
        }
    }

    // Passed in from the client.
    private FragmentActivity mFragmentActivity;
    private Fragment mFragment;
    private final Executor mExecutor;
    private final AuthenticationCallback mAuthenticationCallback;

    // Created internally for devices before P.
    private FingerprintDialogFragment mFingerprintDialogFragment;
    private FingerprintHelperFragment mFingerprintHelperFragment;

    // Created internally for devices P and above.
    private BiometricFragment mBiometricFragment;

    // In Q, we must ignore the first onPause if setDeviceCredentialAllowed is true, since
    // the Q implementation launches ConfirmDeviceCredentialActivity which is an activity and
    // puts the client app onPause.
    private boolean mPausedOnce;

    // Whether this prompt is being hosted in DeviceCredentialHandlerActivity.
    private boolean mIsHandlingDeviceCredential;

    /**
     * A shim to interface with the framework API and simplify the support library's API.
     * The support library sends onAuthenticationError when the negative button is pressed.
     * Conveniently, the {@link FingerprintDialogFragment} also uses the
     * {@link DialogInterface.OnClickListener} for its buttons ;)
     */
    private final DialogInterface.OnClickListener mNegativeButtonListener =
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            if (canUseBiometricFragment() && mBiometricFragment != null) {
                                final CharSequence errorText =
                                        mBiometricFragment.getNegativeButtonText();
                                mAuthenticationCallback.onAuthenticationError(
                                        ERROR_NEGATIVE_BUTTON, errorText != null ? errorText : "");
                                mBiometricFragment.cleanup();
                            } else if (mFingerprintDialogFragment != null
                                    && mFingerprintHelperFragment != null) {
                                final CharSequence errorText =
                                        mFingerprintDialogFragment.getNegativeButtonText();
                                mAuthenticationCallback.onAuthenticationError(
                                        ERROR_NEGATIVE_BUTTON, errorText != null ? errorText : "");
                                mFingerprintHelperFragment.cancel(
                                        FingerprintHelperFragment
                                                .USER_CANCELED_FROM_NEGATIVE_BUTTON);
                            } else {
                                Log.e(TAG, "Negative button callback not run. Fragment was null.");
                            }
                        }
                    });
                }
            };

    /**
     * Observe the client's lifecycle. Keep authenticating across configuration changes, but
     * dismiss the prompt if the client goes into the background.
     */
    private final LifecycleObserver mLifecycleObserver = new LifecycleObserver() {
        @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        void onPause() {
            if (isChangingConfigurations()) {
                if (DEBUG) Log.v(TAG, "onPause() not run while configuration is changing.");
                return;
            }

            if (canUseBiometricFragment() && mBiometricFragment != null) {
                // TODO(b/123378871): Fix behavior in R and remove this workaround.
                // Ignore the first onPause if isDeviceCredentialAllowed is true, since
                // the Q implementation launches ConfirmDeviceCredentialActivity, which puts
                // the client app onPause. Implementations prior to Q instead launch
                // DeviceCredentialHandlerActivity, resulting in the same problem.
                if (mBiometricFragment.isDeviceCredentialAllowed()) {
                    if (!mPausedOnce) {
                        mPausedOnce = true;
                    } else {
                        mBiometricFragment.cancel();
                    }
                } else {
                    mBiometricFragment.cancel();
                }
            } else if (mFingerprintDialogFragment != null && mFingerprintHelperFragment != null) {
                dismissFingerprintFragments(mFingerprintDialogFragment, mFingerprintHelperFragment);
            }

            maybeResetHandlerBridge();
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
        void onResume() {
            mBiometricFragment = canUseBiometricFragment()
                    ? (BiometricFragment) getFragmentManager().findFragmentByTag(
                            BIOMETRIC_FRAGMENT_TAG)
                    : null;
            if (DEBUG && canUseBiometricFragment()) {
                Log.v(TAG, "BiometricFragment: " + mBiometricFragment);
            }
            if (canUseBiometricFragment() && mBiometricFragment != null) {
                mBiometricFragment.setCallbacks(mExecutor, mNegativeButtonListener,
                        mAuthenticationCallback);
            } else {
                mFingerprintDialogFragment =
                        (FingerprintDialogFragment) getFragmentManager().findFragmentByTag(
                                DIALOG_FRAGMENT_TAG);
                mFingerprintHelperFragment =
                        (FingerprintHelperFragment) getFragmentManager().findFragmentByTag(
                                FINGERPRINT_HELPER_FRAGMENT_TAG);

                if (DEBUG) Log.v(TAG, "FingerprintDialogFragment: " + mFingerprintDialogFragment);
                if (DEBUG) Log.v(TAG, "FingerprintHelperFragment: " + mFingerprintHelperFragment);
                if (mFingerprintDialogFragment != null) {
                    mFingerprintDialogFragment.setNegativeButtonListener(mNegativeButtonListener);
                }
                if (mFingerprintHelperFragment != null) {
                    mFingerprintHelperFragment.setCallback(mExecutor, mAuthenticationCallback);
                    if (mFingerprintDialogFragment != null) {
                        mFingerprintHelperFragment.setHandler(
                                mFingerprintDialogFragment.getHandler());
                    }
                }
            }

            maybeHandleDeviceCredentialResult();
            maybeInitHandlerBridge(false /* startIgnoringReset */);
        }
    };

    /**
     * Constructs a {@link BiometricPrompt} which can be used to prompt the user for
     * authentication. The authentication prompt created by
     * {@link BiometricPrompt#authenticate(PromptInfo, CryptoObject)} and
     * {@link BiometricPrompt#authenticate(PromptInfo)} will persist across device
     * configuration changes by default. If authentication is in progress, re-creating
     * the {@link BiometricPrompt} can be used to update the {@link Executor} and
     * {@link AuthenticationCallback}. This should be used to update the
     * {@link AuthenticationCallback} after configuration changes.
     * such as {@link FragmentActivity#onCreate(Bundle)}.
     *
     * @param fragmentActivity A reference to the client's activity.
     * @param executor         An executor to handle callback events.
     * @param callback         An object to receive authentication events.
     */
    @SuppressLint("LambdaLast")
    public BiometricPrompt(@NonNull FragmentActivity fragmentActivity,
            @NonNull Executor executor, @NonNull AuthenticationCallback callback) {
        if (fragmentActivity == null) {
            throw new IllegalArgumentException("FragmentActivity must not be null");
        }
        if (executor == null) {
            throw new IllegalArgumentException("Executor must not be null");
        }
        if (callback == null) {
            throw new IllegalArgumentException("AuthenticationCallback must not be null");
        }
        mFragmentActivity = fragmentActivity;
        mAuthenticationCallback = callback;
        mExecutor = executor;

        mFragmentActivity.getLifecycle().addObserver(mLifecycleObserver);
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
     * such as {@link Fragment#onCreate(Bundle)}.
     *
     * @param fragment A reference to the client's fragment.
     * @param executor An executor to handle callback events.
     * @param callback An object to receive authentication events.
     */
    @SuppressLint("LambdaLast")
    public BiometricPrompt(@NonNull Fragment fragment,
            @NonNull Executor executor, @NonNull AuthenticationCallback callback) {
        if (fragment == null) {
            throw new IllegalArgumentException("FragmentActivity must not be null");
        }
        if (executor == null) {
            throw new IllegalArgumentException("Executor must not be null");
        }
        if (callback == null) {
            throw new IllegalArgumentException("AuthenticationCallback must not be null");
        }
        mFragment = fragment;
        mAuthenticationCallback = callback;
        mExecutor = executor;

        mFragment.getLifecycle().addObserver(mLifecycleObserver);
    }

    /**
     * Shows the biometric prompt. The prompt survives lifecycle changes by default. To cancel the
     * authentication, use {@link #cancelAuthentication()}.
     *
     * @param info   The information that will be displayed on the prompt. Create this object using
     *               {@link BiometricPrompt.PromptInfo.Builder}.
     * @param crypto The crypto object associated with the authentication.
     */
    public void authenticate(@NonNull PromptInfo info, @NonNull CryptoObject crypto) {
        if (info == null) {
            throw new IllegalArgumentException("PromptInfo can not be null");
        } else if (crypto == null) {
            throw new IllegalArgumentException("CryptoObject can not be null");
        } else if (info.getBundle().getBoolean(KEY_ALLOW_DEVICE_CREDENTIAL)) {
            throw new IllegalArgumentException("Device credential not supported with crypto");
        }
        authenticateInternal(info, crypto);
    }

    /**
     * Shows the biometric prompt. The prompt survives lifecycle changes by default. To cancel the
     * authentication, use {@link #cancelAuthentication()}.
     *
     * @param info The information that will be displayed on the prompt. Create this object using
     *             {@link BiometricPrompt.PromptInfo.Builder}.
     */
    public void authenticate(@NonNull PromptInfo info) {
        if (info == null) {
            throw new IllegalArgumentException("PromptInfo can not be null");
        }
        authenticateInternal(info, null /* crypto */);
    }

    private void authenticateInternal(@NonNull PromptInfo info, @Nullable CryptoObject crypto) {
        mIsHandlingDeviceCredential = info.isHandlingDeviceCredentialResult();
        final FragmentActivity activity = getActivity();
        if (info.isDeviceCredentialAllowed() && Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            // Launch handler activity to support device credential on older versions.
            if (!mIsHandlingDeviceCredential) {
                launchDeviceCredentialHandler(info);
                return;
            }

            // Fall back to device credential immediately if no biometrics are enrolled.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (activity == null) {
                    Log.e(TAG, "Failed to authenticate with device credential. Activity was null.");
                    return;
                }

                final DeviceCredentialHandlerBridge bridge =
                        DeviceCredentialHandlerBridge.getInstanceIfNotNull();
                if (bridge == null) {
                    Log.e(TAG, "Failed to authenticate with device credential. Bridge was null.");
                    return;
                }

                if (!bridge.isConfirmingDeviceCredential()) {
                    final BiometricManager biometricManager = BiometricManager.from(activity);
                    if (biometricManager.canAuthenticate() != BiometricManager.BIOMETRIC_SUCCESS) {
                        DeviceCredentialLauncher.launchConfirmation(
                                TAG, activity, info.getBundle(), null /* onLaunch */);
                        return;
                    }
                }
            }
        }

        // Don't launch prompt if state has already been saved (potential for state loss).
        final FragmentManager fragmentManager = getFragmentManager();
        if (fragmentManager.isStateSaved()) {
            Log.w(TAG, "Not launching prompt. authenticate() called after onSaveInstanceState()");
            return;
        }

        final Bundle bundle = info.getBundle();
        mPausedOnce = false;

        // Force some devices to fall back to fingerprint in order to support strong (crypto) auth.
        final boolean shouldForceFingerprint = DEBUG_FORCE_FINGERPRINT
                || (activity != null && crypto != null
                        && DeviceConfig.shouldUseFingerprintForCrypto(
                                activity, Build.MANUFACTURER, Build.MODEL));

        if (!shouldForceFingerprint && canUseBiometricFragment()) {
            BiometricFragment biometricFragment =
                    (BiometricFragment) fragmentManager.findFragmentByTag(BIOMETRIC_FRAGMENT_TAG);
            if (biometricFragment != null) {
                mBiometricFragment = biometricFragment;
            } else {
                mBiometricFragment = BiometricFragment.newInstance();
            }
            mBiometricFragment.setCallbacks(mExecutor, mNegativeButtonListener,
                    mAuthenticationCallback);

            // Set the crypto object.
            mBiometricFragment.setCryptoObject(crypto);
            mBiometricFragment.setBundle(bundle);

            if (biometricFragment == null) {
                // If the fragment hasn't been added before, add it. It will also start the
                // authentication.
                fragmentManager.beginTransaction().add(mBiometricFragment, BIOMETRIC_FRAGMENT_TAG)
                        .commitAllowingStateLoss();
            } else if (mBiometricFragment.isDetached()) {
                // If it's been added before, just re-attach it.
                fragmentManager.beginTransaction().attach(mBiometricFragment)
                        .commitAllowingStateLoss();
            }
        } else {
            // Create the UI
            FingerprintDialogFragment fingerprintDialogFragment =
                    (FingerprintDialogFragment) fragmentManager.findFragmentByTag(
                            DIALOG_FRAGMENT_TAG);
            if (fingerprintDialogFragment != null) {
                mFingerprintDialogFragment = fingerprintDialogFragment;
            } else {
                mFingerprintDialogFragment = FingerprintDialogFragment.newInstance();
            }

            mFingerprintDialogFragment.setNegativeButtonListener(mNegativeButtonListener);
            mFingerprintDialogFragment.setBundle(bundle);

            if (activity != null
                    && !DeviceConfig.shouldHideFingerprintDialog(activity, Build.MODEL)) {
                if (fingerprintDialogFragment == null) {
                    mFingerprintDialogFragment.show(fragmentManager, DIALOG_FRAGMENT_TAG);
                } else if (mFingerprintDialogFragment.isDetached()) {
                    fragmentManager.beginTransaction().attach(mFingerprintDialogFragment)
                            .commitAllowingStateLoss();
                }
            }

            // Create the connection to FingerprintManager
            FingerprintHelperFragment fingerprintHelperFragment =
                    (FingerprintHelperFragment) fragmentManager.findFragmentByTag(
                            FINGERPRINT_HELPER_FRAGMENT_TAG);
            if (fingerprintHelperFragment != null) {
                mFingerprintHelperFragment = fingerprintHelperFragment;
            } else {
                mFingerprintHelperFragment = FingerprintHelperFragment.newInstance();
            }

            mFingerprintHelperFragment.setCallback(mExecutor, mAuthenticationCallback);
            final Handler fingerprintDialogHandler = mFingerprintDialogFragment.getHandler();
            mFingerprintHelperFragment.setHandler(fingerprintDialogHandler);
            mFingerprintHelperFragment.setCryptoObject(crypto);
            fingerprintDialogHandler.sendMessageDelayed(
                    fingerprintDialogHandler.obtainMessage(
                            FingerprintDialogFragment.MSG_DISPLAYED_FOR_500_MS), DELAY_MILLIS);

            if (fingerprintHelperFragment == null) {
                // If the fragment hasn't been added before, add it. It will also start the
                // authentication.
                fragmentManager.beginTransaction()
                        .add(mFingerprintHelperFragment, FINGERPRINT_HELPER_FRAGMENT_TAG)
                        .commitAllowingStateLoss();
            } else if (mFingerprintHelperFragment.isDetached()) {
                // If it's been added before, just re-attach it.
                fragmentManager.beginTransaction().attach(mFingerprintHelperFragment)
                        .commitAllowingStateLoss();
            }
        }

        // For the case when onResume() is being called right after authenticate,
        // we need to make sure that all fragment transactions have been committed.
        fragmentManager.executePendingTransactions();
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
        if (canUseBiometricFragment() && mBiometricFragment != null) {
            mBiometricFragment.cancel();

            // If we launched a device credential handler activity, also clean up its fragment.
            if (!mIsHandlingDeviceCredential) {
                final DeviceCredentialHandlerBridge bridge =
                        DeviceCredentialHandlerBridge.getInstanceIfNotNull();
                if (bridge != null && bridge.getBiometricFragment() != null) {
                    bridge.getBiometricFragment().cancel();
                }
            }
        } else {
            if (mFingerprintHelperFragment != null && mFingerprintDialogFragment != null) {
                dismissFingerprintFragments(mFingerprintDialogFragment, mFingerprintHelperFragment);
            }

            // If we launched a device credential handler activity, also clean up its fragment.
            if (!mIsHandlingDeviceCredential) {
                final DeviceCredentialHandlerBridge bridge =
                        DeviceCredentialHandlerBridge.getInstanceIfNotNull();
                if (bridge != null && bridge.getFingerprintDialogFragment() != null
                        && bridge.getFingerprintHelperFragment() != null) {
                    dismissFingerprintFragments(bridge.getFingerprintDialogFragment(),
                            bridge.getFingerprintHelperFragment());
                }
            }
        }
    }

    /**
     * Launches a copy of this prompt in a transparent {@link DeviceCredentialHandlerActivity}.
     * This allows that activity to intercept and handle activity results from {@link
     * android.app.KeyguardManager#createConfirmDeviceCredentialIntent(CharSequence, CharSequence)}.
     */
    private void launchDeviceCredentialHandler(PromptInfo info) {
        final FragmentActivity activity = getActivity();
        if (activity == null || activity.isFinishing()) {
            Log.w(TAG, "Failed to start handler activity. Parent activity was null or finishing.");
            return;
        }

        maybeInitHandlerBridge(true /* startIgnoringReset */);

        // Set the handling device credential flag so the new prompt knows not to launch another
        // instance of the handler activity.
        final Bundle infoBundle = info.getBundle();
        infoBundle.putBoolean(KEY_HANDLING_DEVICE_CREDENTIAL_RESULT, true);

        final Intent intent = new Intent(activity, DeviceCredentialHandlerActivity.class);
        intent.putExtra(DeviceCredentialHandlerActivity.EXTRA_PROMPT_INFO_BUNDLE, infoBundle);
        activity.startActivity(intent);
    }

    /**
     * Creates (if necessary) the singleton bridge used for communication between the client-hosted
     * prompt and one hosted by {@link DeviceCredentialHandlerActivity}, and initializes all of the
     * relevant data for the bridge.
     *
     * @param startIgnoringReset Whether the bridge should start ignoring calls to
     *                           {@link DeviceCredentialHandlerBridge#reset()} once initialized.
     */
    private void maybeInitHandlerBridge(boolean startIgnoringReset) {
        // Don't create bridge if DeviceCredentialHandlerActivity isn't needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return;
        }

        final DeviceCredentialHandlerBridge bridge = DeviceCredentialHandlerBridge.getInstance();
        if (mIsHandlingDeviceCredential) {
            if (canUseBiometricFragment() && mBiometricFragment != null) {
                bridge.setBiometricFragment(mBiometricFragment);
            } else if (mFingerprintDialogFragment != null && mFingerprintHelperFragment != null) {
                bridge.setFingerprintFragments(mFingerprintDialogFragment,
                        mFingerprintHelperFragment);
            }
        } else {
            // If hosted by the client, register the current activity theme to the bridge.
            final FragmentActivity activity = getActivity();
            if (activity != null) {
                try {
                    bridge.setClientThemeResId(activity.getPackageManager().getActivityInfo(
                            activity.getComponentName(), 0).getThemeResource());
                } catch (PackageManager.NameNotFoundException e) {
                    Log.e(TAG, "Failed to register client theme to bridge", e);
                }
            }
        }
        bridge.setCallbacks(mExecutor, mNegativeButtonListener, mAuthenticationCallback);

        if (startIgnoringReset) {
            bridge.startIgnoringReset();
        }
    }

    /**
     * Checks the handler bridge to see if we've received a result from the confirm device
     * credential Settings activity. If so, handles that result by calling the appropriate
     * authentication callback.
     */
    private void maybeHandleDeviceCredentialResult() {
        // Only handle result from the original (not handler-hosted) prompt.
        if (mIsHandlingDeviceCredential) {
            return;
        }

        final DeviceCredentialHandlerBridge bridge =
                DeviceCredentialHandlerBridge.getInstanceIfNotNull();
        if (bridge != null) {
            switch (bridge.getDeviceCredentialResult()) {
                case DeviceCredentialHandlerBridge.RESULT_SUCCESS:
                    // Device credential auth succeeded. This is incompatible with crypto.
                    mAuthenticationCallback.onAuthenticationSucceeded(
                            new BiometricPrompt.AuthenticationResult(null /* crypto */));
                    bridge.stopIgnoringReset();
                    bridge.reset();
                    break;

                case DeviceCredentialHandlerBridge.RESULT_ERROR:
                    // Device credential auth failed. Assume this is due to the user canceling.
                    final CharSequence errorMsg = getActivity() != null
                            ? getActivity().getString(R.string.generic_error_user_canceled) : "";
                    mAuthenticationCallback.onAuthenticationError(
                            BiometricConstants.ERROR_USER_CANCELED, errorMsg);
                    bridge.stopIgnoringReset();
                    bridge.reset();
                    break;
            }
        }
    }

    /** Cleans up the device credential handler bridge (if it exists) to avoid leaking memory. */
    private void maybeResetHandlerBridge() {
        final DeviceCredentialHandlerBridge bridge =
                DeviceCredentialHandlerBridge.getInstanceIfNotNull();
        if (bridge != null) {
            bridge.reset();
        }
    }

    /** Checks if the client is currently changing configurations (e.g., screen orientation). */
    private boolean isChangingConfigurations() {
        return getActivity() != null && getActivity().isChangingConfigurations();
    }

    /** Gets the client activity that is hosting the biometric prompt. */
    @Nullable
    private FragmentActivity getActivity() {
        return mFragmentActivity != null ? mFragmentActivity : mFragment.getActivity();
    }

    /**
     * Gets the appropriate fragment manager for the client. This is either the support fragment
     * manager for a client activity or the child fragment manager for a client fragment.
     */
    private FragmentManager getFragmentManager() {
        return mFragmentActivity != null ? mFragmentActivity.getSupportFragmentManager()
                : mFragment.getChildFragmentManager();
    }

    /**
     * @return true if the prompt can handle authentication via {@link BiometricFragment}, based
     * on API level, or false if it will do so via {@link FingerprintDialogFragment}.
     */
    private static boolean canUseBiometricFragment() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P;
    }

    /**
     * Dismisses the given {@link FingerprintDialogFragment} and {@link FingerprintHelperFragment},
     * both of which must be non-null.
     */
    private static void dismissFingerprintFragments(
            @NonNull FingerprintDialogFragment fingerprintDialogFragment,
            @NonNull FingerprintHelperFragment fingerprintHelperFragment) {
        fingerprintDialogFragment.dismissSafely();
        fingerprintHelperFragment.cancel(FingerprintHelperFragment.USER_CANCELED_FROM_NONE);
    }
}
