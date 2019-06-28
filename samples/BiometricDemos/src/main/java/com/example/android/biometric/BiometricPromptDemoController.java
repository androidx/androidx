/*
 * Copyright 2019 The Android Open Source Project
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

package com.example.android.biometric;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.concurrent.Executor;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

/**
 * Controller for the biometric prompt demo app. Coordinates the logic of initializing buttons,
 * generating the info and crypto objects for the biometric prompt, and launching the prompt itself.
 */
abstract class BiometricPromptDemoController {

    private static final String TAG = "bio_prompt_demo_control";

    private static final String KEY_COUNTER = "saved_counter";

    private static final String DEFAULT_KEY_NAME = "default_key";

    private static final String BIOMETRIC_SUCCESS_MESSAGE = "BIOMETRIC_SUCCESS_MESSAGE";
    private static final String BIOMETRIC_ERROR_HW_UNAVAILABLE_MESSAGE =
            "BIOMETRIC_ERROR_HW_UNAVAILABLE";
    private static final String BIOMETRIC_ERROR_NONE_ENROLLED_MESSAGE =
            "BIOMETRIC_ERROR_NONE_ENROLLED";
    private static final String BIOMETRIC_ERROR_NO_HARDWARE =
            "BIOMETRIC_ERROR_NO_HARDWARE";
    private static final String BIOMETRIC_ERROR_UNKNOWN = "Error unknown return result";

    private static final int MODE_NONE = 0;
    private static final int MODE_PERSIST_ACROSS_CONFIGURATION_CHANGES = 1;
    private static final int MODE_CANCEL_ON_CONFIGURATION_CHANGE = 2;
    private static final int MODE_CANCEL_AFTER_THREE_FAILURES = 3;

    private Handler mHandler = new Handler(Looper.getMainLooper());
    private KeyStore mKeyStore;
    private int mCounter;
    private int mNumberFailedAttempts;

    Button mCreateKeysButton;
    Button mAuthenticateButton;
    Button mCanAuthenticateButton;
    CheckBox mUseCryptoCheckbox;
    CheckBox mConfirmationRequiredCheckbox;
    CheckBox mDeviceCredentialAllowedCheckbox;
    RadioGroup mRadioGroup;

    BiometricPrompt mBiometricPrompt;

    final Executor mExecutor = runnable -> mHandler.post(runnable);
    final BiometricPrompt.AuthenticationCallback mAuthenticationCallback =
            new BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationError(int err, @NonNull CharSequence message) {
                    Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
                    Log.v(TAG, "Error " + err + ": " + message);
                    mNumberFailedAttempts = 0;
                }

                @Override
                public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                    Toast.makeText(getApplicationContext(), "Yay, Crypto: "
                            + result.getCryptoObject(), Toast.LENGTH_SHORT).show();
                    mNumberFailedAttempts = 0;

                    if (result.getCryptoObject() != null) {
                        Cipher cipher = result.getCryptoObject().getCipher();
                        try {
                            byte[] encrypted = cipher.doFinal(
                                    "hello".getBytes(Charset.defaultCharset()));
                            String message = Arrays.toString(
                                    Base64.encode(encrypted, 0 /* flags */));
                            Toast.makeText(getApplicationContext(), "Message: " + message,
                                    Toast.LENGTH_SHORT).show();
                        } catch (BadPaddingException | IllegalBlockSizeException e) {
                            Toast.makeText(getApplicationContext(), "Error encrypting",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                }

                @Override
                public void onAuthenticationFailed() {
                    Toast.makeText(getApplicationContext(), "failed", Toast.LENGTH_SHORT).show();
                    Log.v(TAG, "onAuthenticationFailed");
                    mNumberFailedAttempts++;

                    // Cancel authentication after 3 failed attempts to test the cancel() method.
                    if (getMode() == MODE_CANCEL_AFTER_THREE_FAILURES
                            && mNumberFailedAttempts == 3) {
                        mBiometricPrompt.cancelAuthentication();
                    }
                }
            };

    abstract Context getApplicationContext();

    abstract void onResume();

    /** Sets up button callbacks and other state for the biometric prompt demo controller. */
    void init(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mCounter = savedInstanceState.getInt(KEY_COUNTER);
        }

        try {
            mKeyStore = KeyStore.getInstance("AndroidKeyStore");
        } catch (java.security.KeyStoreException e) {
            throw new RuntimeException("Failed to get an instance of KeyStore", e);
        }

        if (!useCrypto()) {
            mCreateKeysButton.setVisibility(View.GONE);
        } else {
            mCreateKeysButton.setVisibility(View.VISIBLE);
        }
        mUseCryptoCheckbox.setOnClickListener(this::onCheckboxClicked);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mCreateKeysButton.setOnClickListener(v -> enableBiometricWithCrypto());
        }
        mAuthenticateButton.setOnClickListener(v -> startAuthentication());

        mCanAuthenticateButton.setOnClickListener(v -> {
            BiometricManager biometricManager = BiometricManager.from(getApplicationContext());
            String message;
            switch (biometricManager.canAuthenticate()) {
                case BiometricManager.BIOMETRIC_SUCCESS:
                    message = BIOMETRIC_SUCCESS_MESSAGE;
                    break;
                case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                    message = BIOMETRIC_ERROR_HW_UNAVAILABLE_MESSAGE;
                    break;
                case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                    message = BIOMETRIC_ERROR_NONE_ENROLLED_MESSAGE;
                    break;
                case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                    message = BIOMETRIC_ERROR_NO_HARDWARE;
                    break;
                default:
                    message = BIOMETRIC_ERROR_UNKNOWN;
            }
            Toast.makeText(getApplicationContext(), "canAuthenticate : " + message,
                    Toast.LENGTH_SHORT).show();
        });


        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            mDeviceCredentialAllowedCheckbox.setEnabled(false);
            mDeviceCredentialAllowedCheckbox.setChecked(false);
        }
    }

    void onPause() {
        if (getMode() == MODE_CANCEL_ON_CONFIGURATION_CHANGE) {
            mBiometricPrompt.cancelAuthentication();
        }
    }

    void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt(KEY_COUNTER, mCounter);
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private void enableBiometricWithCrypto() {
        // Create the key, this is usually done when the user allows Biometric
        // authentication in the app. This key is invalidated by new biometric enrollment
        // (done in createKey())
        try {
            createKey(DEFAULT_KEY_NAME, true);
        } catch (RuntimeException e) {
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void startAuthentication() {
        if (getMode() == MODE_NONE) {
            Toast.makeText(getApplicationContext(), "Select a test first", Toast.LENGTH_SHORT)
                    .show();
            return;
        }

        // Build the biometric prompt info
        BiometricPrompt.PromptInfo.Builder builder = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Title " + mCounter)
                .setSubtitle("Subtitle " + mCounter)
                .setDescription(
                        "Lorem ipsum dolor sit amet, consecte etur adipisicing elit. "
                                + mCounter)
                .setConfirmationRequired(mConfirmationRequiredCheckbox.isChecked());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (mDeviceCredentialAllowedCheckbox.isChecked()) {
                builder.setDeviceCredentialAllowed(true);
            } else {
                builder.setNegativeButtonText("Negative Button " + mCounter);
            }
        } else {
            builder.setNegativeButtonText("Negative Button " + mCounter);
        }
        BiometricPrompt.PromptInfo info = builder.build();
        mCounter++;

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M && useCrypto()) {
            try {
                // Initialize the cipher. The cipher will be unlocked by KeyStore after the user has
                // authenticated via biometrics.
                Cipher defaultCipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/"
                        + KeyProperties.BLOCK_MODE_CBC + "/"
                        + KeyProperties.ENCRYPTION_PADDING_PKCS7);

                if (initCipher(defaultCipher, DEFAULT_KEY_NAME)) {
                    BiometricPrompt.CryptoObject crypto =
                            new BiometricPrompt.CryptoObject(
                                    defaultCipher);

                    // Show the biometric prompt.
                    mBiometricPrompt.authenticate(info, crypto);
                }
            } catch (RuntimeException e) {
                Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
                throw new RuntimeException("Failed to get an instance of Cipher", e);
            }
        } else {
            // Show the biometric prompt.
            mBiometricPrompt.authenticate(info);
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private boolean initCipher(Cipher cipher, String keyName) {
        try {
            mKeyStore.load(null);
            SecretKey key = (SecretKey) mKeyStore.getKey(keyName, null);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return true;
        } catch (KeyStoreException | CertificateException | UnrecoverableKeyException | IOException
                | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to init Cipher", e);
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private void createKey(String keyName, boolean invalidatedByBiometricEnrollment) {
        KeyGenerator keyGenerator;

        try {
            keyGenerator = KeyGenerator
                    .getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new RuntimeException("Failed to get an instance of KeyGenerator", e);
        }

        // The enrolling flow for fingerprint. This is where you ask the user to set up fingerprint
        // for your flow. Use of keys is necessary if you need to know if the set of
        // enrolled fingerprints has changed.
        try {
            mKeyStore.load(null);

            // Set the alias of the entry in Android KeyStore where the key will appear
            // and the constrains (purposes) in the constructor of the Builder
            KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(
                    keyName,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    // Require the user to authenticate with a fingerprint to authorize every use
                    // of the key
                    .setUserAuthenticationRequired(true)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7);

            // This is a workaround to avoid crashes on devices whose API level is < 24
            // because KeyGenParameterSpec.Builder#setInvalidatedByBiometricEnrollment is only
            // visible on API level 24+.
            // Ideally there should be a compat library for KeyGenParameterSpec.Builder but
            // which isn't available yet.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // Invalidate the keys if a new biometric has been enrolled.
                builder.setInvalidatedByBiometricEnrollment(invalidatedByBiometricEnrollment);
            }
            keyGenerator.init(builder.build());
            keyGenerator.generateKey();
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException
                | CertificateException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean useCrypto() {
        return mUseCryptoCheckbox.isChecked();
    }

    /**
     * Callback when the checkbox is clicked.
     */
    private void onCheckboxClicked(View view) {
        final boolean checked = ((CheckBox) view).isChecked();

        switch (view.getId()) {
            case R.id.checkbox_use_crypto:
                mCreateKeysButton.setVisibility(checked ? View.VISIBLE : View.GONE);
                break;
        }
    }

    /**
     * @return The currently selected configuration.
     */
    private int getMode() {
        int id = mRadioGroup.getCheckedRadioButtonId();
        switch (id) {
            case R.id.radio_persist_across_configuration_changes:
                return MODE_PERSIST_ACROSS_CONFIGURATION_CHANGES;
            case R.id.radio_cancel_on_configuration_change:
                return MODE_CANCEL_ON_CONFIGURATION_CHANGE;
            case R.id.radio_cancel_after_three_failures:
                return MODE_CANCEL_AFTER_THREE_FAILURES;
            default:
                return MODE_NONE;
        }
    }
}
