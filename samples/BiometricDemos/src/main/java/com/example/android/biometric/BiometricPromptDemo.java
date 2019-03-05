/*
 * Copyright (C) 2013 The Android Open Source Project
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

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.biometric.BiometricPrompt;
import androidx.fragment.app.FragmentActivity;

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
 * Demo for BiometricPrompt, demonstrating: 1) Use of biometrics without a crypto object. 2) Use
 * of biometrics with a crypto object, where the keys become invalid after new biometric enrollment.
 * 3) Use of biometric prompt (compat) that persists through orientation changes. 4) Use of
 * biometric prompt (compat) that doesn't persist through orientation changes. 5) Cancellation of
 * the authentication attempt.
 */
public class BiometricPromptDemo extends FragmentActivity {

    private static final String TAG = "BiometricPromptDemo";

    private static final String KEY_SAVED_USE_CRYPTO = "saved_use_crypto_state";
    private static final String KEY_SAVED_RADIO = "saved_radio_state";
    private static final String KEY_SAVED_FAILURES = "saved_failures_state";
    private static final String KEY_COUNTER = "saved_counter";

    private static final String DEFAULT_KEY_NAME = "default_key";

    private static final int MODE_NONE = 0;
    private static final int MODE_PERSIST_ACROSS_CONFIGURATION_CHANGES = 1;
    private static final int MODE_CANCEL_ON_CONFIGURATION_CHANGE = 2;
    private static final int MODE_CANCEL_AFTER_THREE_FAILURES = 3;

    private int mMode = MODE_NONE;
    private boolean mUseCrypto;

    private Handler mHandler = new Handler(Looper.getMainLooper());
    private KeyStore mKeyStore;
    private BiometricPrompt mBiometricPrompt;

    private int mCounter;
    private int mNumberFailedAttempts;

    private final Executor mExecutor = (runnable) -> {
        mHandler.post(runnable);
    };

    private final BiometricPrompt.AuthenticationCallback mAuthenticationCallback =
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
                    byte[] encrypted = cipher.doFinal("hello".getBytes(Charset.defaultCharset()));
                    Toast.makeText(getApplicationContext(), "Message: "
                            + Arrays.toString(Base64.encode(encrypted, 0 /* flags */)),
                            Toast.LENGTH_SHORT).show();
                } catch (BadPaddingException | IllegalBlockSizeException e) {
                    Toast.makeText(getApplicationContext(), "Error encrypting", Toast.LENGTH_SHORT)
                            .show();
                }
            }
        }

        @Override
        public void onAuthenticationFailed() {
            Toast.makeText(getApplicationContext(), "failed", Toast.LENGTH_SHORT).show();
            Log.v(TAG, "onAuthenticationFailed");
            mNumberFailedAttempts++;

            // Cancel authentication after 3 failed attempts to test the cancel() method.
            if (mMode == MODE_CANCEL_AFTER_THREE_FAILURES && mNumberFailedAttempts == 3) {
                mBiometricPrompt.cancelAuthentication();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mUseCrypto = savedInstanceState.getBoolean(KEY_SAVED_USE_CRYPTO);
            mMode = savedInstanceState.getInt(KEY_SAVED_RADIO);
            mNumberFailedAttempts = savedInstanceState.getInt(KEY_SAVED_FAILURES);
            mCounter = savedInstanceState.getInt(KEY_COUNTER);
        }

        setContentView(R.layout.fragment_activity);

        final Button buttonCreateKeys;
        buttonCreateKeys = findViewById(R.id.button_enable_biometric_with_crypto);
        final Button buttonAuthenticate;
        buttonAuthenticate = findViewById(R.id.button_authenticate);

        try {
            mKeyStore = KeyStore.getInstance("AndroidKeyStore");
        } catch (java.security.KeyStoreException e) {
            throw new RuntimeException("Failed to get an instance of KeyStore", e);
        }

        if (!mUseCrypto) {
            buttonCreateKeys.setVisibility(View.GONE);
        } else {
            buttonCreateKeys.setVisibility(View.VISIBLE);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            buttonCreateKeys.setOnClickListener(v -> enableBiometricWithCrypto());
        }
        buttonAuthenticate.setOnClickListener(v -> startAuthentication());
    }

    @Override
    protected void onPause() {
        if (mMode == MODE_CANCEL_ON_CONFIGURATION_CHANGE) {
            mBiometricPrompt.cancelAuthentication();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Developers should (re)create the BiometricPrompt every time the application is resumed.
        // This is necessary because it is possible for the executor and callback to be GC'd.
        // Instantiating the prompt here allows the library to handle things such as configuration
        // changes.
        mBiometricPrompt = new BiometricPrompt(this /* fragmentActivity */, mExecutor,
                mAuthenticationCallback);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_SAVED_USE_CRYPTO, mUseCrypto);
        outState.putInt(KEY_SAVED_RADIO, mMode);
        outState.putInt(KEY_SAVED_FAILURES, mNumberFailedAttempts);
        outState.putInt(KEY_COUNTER, mCounter);
    }

    /**
     * Callback when the radio buttons are clicked.
     * @param view
     */
    public void onRadioButtonClicked(View view) {
        mNumberFailedAttempts = 0;

        switch(view.getId()) {
            case R.id.radio_persist_across_configuration_changes:
                mMode = MODE_PERSIST_ACROSS_CONFIGURATION_CHANGES;
                break;
            case R.id.radio_cancel_on_configuration_change:
                mMode = MODE_CANCEL_ON_CONFIGURATION_CHANGE;
                break;
            case R.id.radio_cancel_after_three_failures:
                mMode = MODE_CANCEL_AFTER_THREE_FAILURES;
                break;
        }
    }

    /**
     * Callback when the checkbox is clicked.
     * @param view
     */
    public void onCheckboxClicked(View view) {
        boolean checked = ((CheckBox) view).isChecked();

        switch(view.getId()) {
            case R.id.checkbox_use_crypto:
                if (checked) {
                    findViewById(R.id.button_enable_biometric_with_crypto)
                            .setVisibility(View.VISIBLE);
                } else {
                    findViewById(R.id.button_enable_biometric_with_crypto)
                            .setVisibility(View.GONE);
                }
                mUseCrypto = checked;
                break;
        }
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
        if (mMode == MODE_NONE) {
            Toast.makeText(getApplicationContext(), "Select a test first", Toast.LENGTH_SHORT)
                    .show();
            return;
        }

        // Build the biometric prompt info
        BiometricPrompt.PromptInfo info =
                new BiometricPrompt.PromptInfo.Builder()
                        .setTitle("Title " + mCounter)
                        .setSubtitle("Subtitle " + mCounter)
                        .setDescription(
                                "Lorem ipsum dolor sit amet, consecte etur adipisicing elit. "
                                        + mCounter)
                        .setNegativeButtonText("Negative Button " + mCounter)
                        .build();
        mCounter++;

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M && mUseCrypto) {
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
        } catch (KeyPermanentlyInvalidatedException e) {
            throw new RuntimeException("Key permanently invalidated", e);
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
}
