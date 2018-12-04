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

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.RestrictTo;
import androidx.fragment.app.Fragment;

import java.util.concurrent.Executor;

/**
 * A fragment that wraps the BiometricPrompt and has the ability to continue authentication across
 * device configuration changes. This class is not meant to be preserved after process death; for
 * security reasons, the BiometricPromptCompat will automatically stop authentication when the
 * activity is no longer in the foreground.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@TargetApi(28)
@SuppressLint("SyntheticAccessor")
public class BiometricFragment extends Fragment {

    private static final String TAG = "BiometricFragment";

    // Re-set by the application, through BiometricPromptCompat upon orientation changes.
    Executor mClientExecutor;
    DialogInterface.OnClickListener mClientNegativeButtonListener;
    BiometricPrompt.AuthenticationCallback mClientAuthenticationCallback;

    // Set once and retained.
    private BiometricPrompt.CryptoObject mCryptoObject;
    private CharSequence mNegativeButtonText;

    // Created once and retained.
    private boolean mShowing;
    private android.hardware.biometrics.BiometricPrompt mBiometricPrompt;
    private CancellationSignal mCancellationSignal;
    // Do not rely on the application's executor when calling into the framework's code.
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final Executor mExecutor = new Executor() {
        @Override
        public void execute(Runnable runnable) {
            mHandler.post(runnable);
        }
    };

    // Also created once and retained.
    private final android.hardware.biometrics.BiometricPrompt.AuthenticationCallback
            mAuthenticationCallback =
            new android.hardware.biometrics.BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationError(final int errorCode,
                        final CharSequence errString) {
                    mClientExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            mClientAuthenticationCallback
                                    .onAuthenticationError(errorCode, errString);
                        }
                    });
                    cleanup();
                }

                @Override
                public void onAuthenticationHelp(final int helpCode,
                        final CharSequence helpString) {
                    // Don't forward the result to the client, since the dialog takes care of it.
                }

                @Override
                public void onAuthenticationSucceeded(
                        final android.hardware.biometrics.BiometricPrompt.AuthenticationResult
                                result) {
                    mClientExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            mClientAuthenticationCallback.onAuthenticationSucceeded(
                                    new BiometricPrompt.AuthenticationResult(
                                            unwrapCryptoObject(result.getCryptoObject())));
                        }
                    });
                    cleanup();
                }

                @Override
                public void onAuthenticationFailed() {
                    mClientExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            mClientAuthenticationCallback.onAuthenticationFailed();
                        }
                    });
                }
            };

    // Also created once and retained.
    private DialogInterface.OnClickListener mNegativeButtonListener =
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mClientNegativeButtonListener.onClick(dialog, which);
                }
            };

    /**
     * Creates a new instance of the {@link BiometricFragment}.
     * @param bundle
     * @return
     */
    public static BiometricFragment newInstance(Bundle bundle) {
        BiometricFragment biometricFragment = new BiometricFragment();
        biometricFragment.setArguments(bundle);
        return biometricFragment;
    }

    /**
     * Sets the client's callback. This should be done whenever the lifecycle changes (orientation
     * changes).
     * @param executor
     * @param onClickListener
     * @param authenticationCallback
     */
    protected void setCallbacks(Executor executor, DialogInterface.OnClickListener onClickListener,
            BiometricPrompt.AuthenticationCallback authenticationCallback) {
        mClientExecutor = executor;
        mClientNegativeButtonListener = onClickListener;
        mClientAuthenticationCallback = authenticationCallback;
    }

    /**
     * Sets the crypto object to be associated with the authentication. Should be called before
     * adding the fragment to guarantee that it's ready in onCreate().
     * @param crypto
     */
    protected void setCryptoObject(BiometricPrompt.CryptoObject crypto) {
        mCryptoObject = crypto;
    }

    /**
     * Cancel the authentication.
     */
    protected void cancel() {
        if (mCancellationSignal != null) {
            mCancellationSignal.cancel();
        }
        cleanup();
    }

    /**
     * Remove the fragment so that resources can be freed.
     */
    void cleanup() {
        mShowing = false;
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().beginTransaction().detach(this)
                    .commitAllowingStateLoss();
        }
    }

    protected CharSequence getNegativeButtonText() {
        return mNegativeButtonText;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        Bundle bundle = getArguments();

        mNegativeButtonText = bundle.getCharSequence(BiometricPrompt.KEY_NEGATIVE_TEXT);

        mBiometricPrompt = new android.hardware.biometrics.BiometricPrompt.Builder(getContext())
                .setTitle(bundle.getCharSequence(BiometricPrompt.KEY_TITLE))
                .setSubtitle(bundle.getCharSequence(BiometricPrompt.KEY_SUBTITLE))
                .setDescription(bundle.getCharSequence(BiometricPrompt.KEY_DESCRIPTION))
                .setNegativeButton(bundle.getCharSequence(BiometricPrompt.KEY_NEGATIVE_TEXT),
                        mClientExecutor, mNegativeButtonListener)
                .build();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // Start the actual authentication when the fragment is attached.
        if (!mShowing) {
            mCancellationSignal = new CancellationSignal();
            if (mCryptoObject == null) {
                mBiometricPrompt.authenticate(mCancellationSignal, mExecutor,
                        mAuthenticationCallback);
            } else {
                mBiometricPrompt.authenticate(wrapCryptoObject(mCryptoObject), mCancellationSignal,
                        mExecutor, mAuthenticationCallback);
            }
        }
        mShowing = true;
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    static BiometricPrompt.CryptoObject unwrapCryptoObject(
            android.hardware.biometrics.BiometricPrompt.CryptoObject cryptoObject) {
        if (cryptoObject == null) {
            return null;
        } else if (cryptoObject.getCipher() != null) {
            return new BiometricPrompt.CryptoObject(cryptoObject.getCipher());
        } else if (cryptoObject.getSignature() != null) {
            return new BiometricPrompt.CryptoObject(cryptoObject.getSignature());
        } else if (cryptoObject.getMac() != null) {
            return new BiometricPrompt.CryptoObject(cryptoObject.getMac());
        } else {
            return null;
        }
    }

    static android.hardware.biometrics.BiometricPrompt.CryptoObject wrapCryptoObject(
            BiometricPrompt.CryptoObject cryptoObject) {
        if (cryptoObject == null) {
            return null;
        } else if (cryptoObject.getCipher() != null) {
            return new android.hardware.biometrics.BiometricPrompt.CryptoObject(
                    cryptoObject.getCipher());
        } else if (cryptoObject.getSignature() != null) {
            return new android.hardware.biometrics.BiometricPrompt.CryptoObject(
                    cryptoObject.getSignature());
        } else if (cryptoObject.getMac() != null) {
            return new android.hardware.biometrics.BiometricPrompt.CryptoObject(
                    cryptoObject.getMac());
        } else {
            return null;
        }
    }
}
