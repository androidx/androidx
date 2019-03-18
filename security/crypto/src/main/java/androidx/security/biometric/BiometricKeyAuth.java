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

package androidx.security.biometric;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricPrompt;
import androidx.fragment.app.FragmentActivity;
import androidx.security.crypto.SecureCipher;

import java.security.Signature;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import javax.crypto.Cipher;

/**
 * Class that handles authenticating Ciphers using Biometric Prompt.
 */
public class BiometricKeyAuth extends BiometricPrompt.AuthenticationCallback {

    private static final String TAG = "BiometricKeyAuth";

    private FragmentActivity mActivity;
    private SecureCipher.SecureAuthListener mSecureAuthListener;
    private CountDownLatch mCountDownLatch = null;
    private BiometricKeyAuthCallback mBiometricKeyAuthCallback;

    /**
     * @param activity The activity to use a parent
     * @param callback Callback to reply with when complete.
     */
    public BiometricKeyAuth(@NonNull FragmentActivity activity,
            @NonNull BiometricKeyAuthCallback callback) {
        this.mActivity = activity;
        this.mBiometricKeyAuthCallback = callback;
    }

    @Override
    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
        super.onAuthenticationSucceeded(result);
        mBiometricKeyAuthCallback.onAuthenticationSucceeded();
        Log.i(TAG, "Fingerprint success!");
        mSecureAuthListener.authComplete(BiometricKeyAuthCallback.BiometricStatus.SUCCESS);
        mCountDownLatch.countDown();
    }

    @Override
    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
        super.onAuthenticationError(errorCode, errString);
        mBiometricKeyAuthCallback.onMessage(String.valueOf(errString));
        mBiometricKeyAuthCallback.onAuthenticationError(errorCode, errString);
        mSecureAuthListener.authComplete(BiometricKeyAuthCallback.BiometricStatus.FAILED);
        mCountDownLatch.countDown();
    }

    @Override
    public void onAuthenticationFailed() {
        super.onAuthenticationFailed();
        mBiometricKeyAuthCallback.onAuthenticationFailed();
        mSecureAuthListener.authComplete(BiometricKeyAuthCallback.BiometricStatus.FAILED);
        mCountDownLatch.countDown();
    }

    /**
     * Authenticates a key, via the Cipher it's used with
     *
     * @param cipher The cipher to authenticate
     * @param promptInfo The prompt info for the auth fragment
     * @param listener the listener to call back when complete
     */
    public void authenticateKey(@NonNull Cipher cipher,
            @NonNull BiometricPrompt.PromptInfo promptInfo,
            @NonNull SecureCipher.SecureAuthListener listener) {
        authenticateKeyObject(cipher, promptInfo, listener);
    }

    /**
     * Authenticates a key, via the Cipher it's used with
     *
     * @param signature The signature to authenticate
     * @param promptInfo The prompt info for the auth fragment
     * @param listener the listener to call back when complete
     */
    public void authenticateKey(@NonNull Signature signature,
            @NonNull BiometricPrompt.PromptInfo promptInfo,
            @NonNull SecureCipher.SecureAuthListener listener) {
        authenticateKeyObject(signature, promptInfo, listener);
    }

    private void authenticateKeyObject(Object crypto,
            BiometricPrompt.PromptInfo promptInfo,
            SecureCipher.SecureAuthListener listener) {
        mCountDownLatch = new CountDownLatch(1);
        mSecureAuthListener = listener;

        BiometricPrompt prompt = new BiometricPrompt(mActivity,
                Executors.newSingleThreadExecutor(),
                this);
        BiometricPrompt.CryptoObject cryptoObject;
        if (crypto instanceof Cipher) {
            cryptoObject = new BiometricPrompt.CryptoObject((Cipher) crypto);
        } else { /*if(crypto instanceof Signature) {*/
            cryptoObject = new BiometricPrompt.CryptoObject((Signature) crypto);
        }
        prompt.authenticate(promptInfo, cryptoObject);
        try {
            mCountDownLatch.await();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Authenticates a key, via the Cipher it's used with. Provides a default implementation of
     * PromptInfo.
     *
     * @param cipher The cipher to authenticate
     * @param listener the listener to call back when complete
     */
    public void authenticateKey(@NonNull Cipher cipher,
            @NonNull SecureCipher.SecureAuthListener listener) {
        authenticateKeyObject(cipher, listener);
    }

    /**
     * Authenticates a key, via the Signature it's used with. Provides a default implementation of
     * PromptInfo.
     *
     * @param signature The signature to authenticate
     * @param listener the listener to call back when complete
     */
    public void authenticateKey(@NonNull Signature signature,
            @NonNull SecureCipher.SecureAuthListener listener) {
        authenticateKeyObject(signature, listener);
    }

    private void authenticateKeyObject(Object crypto,
            @NonNull SecureCipher.SecureAuthListener listener) {
        authenticateKeyObject(crypto, new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Please Auth for key usage.")
                .setSubtitle("Key used for encrypting files")
                .setDescription("User authentication required to access key.")
                .setNegativeButtonText("Cancel")
                .build(), listener);
    }

}
