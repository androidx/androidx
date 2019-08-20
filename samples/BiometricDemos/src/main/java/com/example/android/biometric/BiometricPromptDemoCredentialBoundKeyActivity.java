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

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.biometric.BiometricPrompt;
import androidx.fragment.app.FragmentActivity;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
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
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

/**
 * Demo activity that shows how BiometricPrompt can be used with credential bound secret keys.
 */
@RequiresApi(api = Build.VERSION_CODES.M)
public class BiometricPromptDemoCredentialBoundKeyActivity extends FragmentActivity {

    private static final String TAG = "bio_prompt_demo_control";
    private static final String LOG_BUNDLE = "log";
    private static final String KEY_NAME = "demo_key";
    private static final String PAYLOAD = "hello";

    // Specifies the duration for which the key can be used after the last user authentication.
    // Only affects the keys generated with setUserAuthenticationValidityDurationSeconds(int).
    private static final int VALIDITY_DURATION = 5;

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final Executor mExecutor = mHandler::post;
    private BiometricPrompt mBiometricPrompt;
    private TextView mLogTextView;

    private void log(String s) {
        Log.d(TAG, s);
        mLogTextView.append(s + '\n');
    }

    private void log(String s, Throwable tr) {
        Log.e(TAG, "", tr);
        mLogTextView.append(s + '\n' + tr.toString() + '\n');
    }

    private final BiometricPrompt.PromptInfo mPromptInfo = new BiometricPrompt.PromptInfo.Builder()
            .setTitle("Title")
            .setSubtitle("Subtitle")
            .setDescription("Description")
            .setDeviceCredentialAllowed(true)
            .build();

    private final BiometricPrompt.AuthenticationCallback mAuthenticationCallback =
            new BiometricPrompt.AuthenticationCallback() {

                @Override
                public void onAuthenticationError(int err, @NonNull CharSequence message) {
                    log("onAuthenticationError " + err + ": " + message);
                }

                @Override
                public void onAuthenticationSucceeded(
                        @NonNull BiometricPrompt.AuthenticationResult result) {
                    log("onAuthenticationSucceeded");
                }

                @Override
                public void onAuthenticationFailed() {
                    log("onAuthenticationFailed");
                    mBiometricPrompt.cancelAuthentication();
                }
            };

    private View.OnClickListener mGenerateKeyListener = view -> {
        try {
            BiometricPromptDemoSecretKeyHelper.generateCredentialBoundKey(KEY_NAME,
                    VALIDITY_DURATION);
            log("Generated a key with a " + VALIDITY_DURATION + " second authentication validity");
        } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException
                | NoSuchProviderException e) {
            log("Failed to generate key", e);
        }
    };

    private View.OnClickListener mUnlockKeyListener = view -> {
        mBiometricPrompt.authenticate(mPromptInfo);
        log("Started authentication");
    };

    private View.OnClickListener mUseKeyListener = view -> {
        Cipher cipher = null;
        try {
            cipher = BiometricPromptDemoSecretKeyHelper.getCipher();
        } catch (NoSuchPaddingException | NoSuchAlgorithmException e) {
            log("Failed to get cipher", e);
        }

        SecretKey secretKey = null;
        try {
            secretKey = BiometricPromptDemoSecretKeyHelper.getSecretKey(KEY_NAME);
        } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException
                | UnrecoverableKeyException e) {
            log("Failed to get secret key", e);
        }

        if (cipher != null && secretKey != null) {
            try {
                cipher.init(Cipher.ENCRYPT_MODE, secretKey);
                byte[] encrypted = cipher.doFinal(PAYLOAD.getBytes(Charset.defaultCharset()));
                log("Test payload: " + PAYLOAD);
                log("Encrypted payload: " + Arrays.toString(encrypted));
            } catch (InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
                log("Failed to encrypt", e);
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.biometric_prompt_demo_credential_bound_key);

        mLogTextView = findViewById(R.id.text_view_log);
        mBiometricPrompt = new BiometricPrompt(this, mExecutor, mAuthenticationCallback);

        ((TextView) findViewById(R.id.text_view_activity_description)).setText(
                R.string.label_credential_bound_key_activity_description);

        if (savedInstanceState != null) {
            mLogTextView.setText(savedInstanceState.getCharSequence(LOG_BUNDLE));
        }

        findViewById(R.id.button_generate_key).setOnClickListener(mGenerateKeyListener);
        findViewById(R.id.button_unlock_key).setOnClickListener(mUnlockKeyListener);
        findViewById(R.id.button_use_key).setOnClickListener(mUseKeyListener);
        findViewById(R.id.button_clear_log).setOnClickListener(v -> mLogTextView.setText(""));
    }

    @Override
    protected void onPause() {
        mBiometricPrompt.cancelAuthentication();
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putCharSequence(LOG_BUNDLE, mLogTextView.getText());
    }
}
