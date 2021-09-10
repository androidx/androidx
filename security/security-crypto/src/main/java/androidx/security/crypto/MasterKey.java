/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.security.crypto;

import static android.security.keystore.KeyProperties.AUTH_BIOMETRIC_STRONG;
import static android.security.keystore.KeyProperties.AUTH_DEVICE_CREDENTIAL;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import androidx.annotation.DoNotInline;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

/**
 * Wrapper for a master key used in the library.
 *
 * On Android M (API 23) and above, this is class references a key that's stored in the
 * Android Keystore. On Android L (API 21, 22), there isn't a master key.
 */
public final class MasterKey {
    static final String KEYSTORE_PATH_URI = "android-keystore://";

    /**
     * The default master key alias.
     */
    public static final String DEFAULT_MASTER_KEY_ALIAS = "_androidx_security_master_key_";

    /**
     * The default and recommended size for the master key.
     */
    public static final int DEFAULT_AES_GCM_MASTER_KEY_SIZE = 256;

    private static final int DEFAULT_AUTHENTICATION_VALIDITY_DURATION_SECONDS = 5 * 60;

    @NonNull
    private final String mKeyAlias;
    @Nullable
    private final KeyGenParameterSpec mKeyGenParameterSpec;

    /**
     * Algorithm/Cipher choices used for the master key.
     */
    public enum KeyScheme {
        AES256_GCM
    }

    /**
     * The default validity period for authentication in seconds.
     */
    @SuppressLint("MethodNameUnits")
    public static int getDefaultAuthenticationValidityDurationSeconds() {
        return DEFAULT_AUTHENTICATION_VALIDITY_DURATION_SECONDS;
    }

    /* package */ MasterKey(@NonNull String keyAlias, @Nullable Object keyGenParameterSpec) {
        mKeyAlias = keyAlias;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mKeyGenParameterSpec = (KeyGenParameterSpec) keyGenParameterSpec;
        } else {
            mKeyGenParameterSpec = null;
        }
    }

    /**
     * Checks if this key is backed by the Android Keystore.
     *
     * @return {@code true} if the key is in Android Keystore, {@code false} otherwise. This
     * method always returns false when called on Android Lollipop (API 21 and 22).
     */
    public boolean isKeyStoreBacked() {
        // Keystore is not used prior to Android M (API 23)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return false;
        }

        try {
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            return keyStore.containsAlias(mKeyAlias);
        } catch (KeyStoreException | CertificateException
                | NoSuchAlgorithmException | IOException ignored) {
            return false;
        }
    }

    /**
     * Gets whether user authentication is required to use this key.
     *
     * This method always returns {@code false} on Android L (API 21 + 22).
     */
    public boolean isUserAuthenticationRequired() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return false;
        }
        return mKeyGenParameterSpec != null
                && Api23Impl.isUserAuthenticationRequired(mKeyGenParameterSpec);
    }

    /**
     * Gets the duration in seconds that the key is unlocked for following user authentication.
     *
     * The value returned for this method is only meaningful on Android M+ (API 23) when
     * {@link #isUserAuthenticationRequired()} returns {@code true}.
     *
     * @return The duration the key is unlocked for in seconds.
     */
    @SuppressLint("MethodNameUnits")
    public int getUserAuthenticationValidityDurationSeconds() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return 0;
        }
        return mKeyGenParameterSpec == null ? 0 :
                Api23Impl.getUserAuthenticationValidityDurationSeconds(mKeyGenParameterSpec);
    }

    /**
     * Gets whether the key is backed by strong box.
     */
    public boolean isStrongBoxBacked() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P || mKeyGenParameterSpec == null) {
            return false;
        }
        return Api28Impl.isStrongBoxBacked(mKeyGenParameterSpec);
    }

    @NonNull
    @Override
    public String toString() {
        return "MasterKey{keyAlias=" + mKeyAlias
                + ", isKeyStoreBacked=" + isKeyStoreBacked()
                + "}";
    }

    @NonNull
    /* package */ String getKeyAlias() {
        return mKeyAlias;
    }

    /**
     * Builder for generating a {@link MasterKey}.
     */
    public static final class Builder {
        @NonNull
        final String mKeyAlias;

        @Nullable
        KeyGenParameterSpec mKeyGenParameterSpec;
        @Nullable
        KeyScheme mKeyScheme;

        boolean mAuthenticationRequired;
        int mUserAuthenticationValidityDurationSeconds;
        boolean mRequestStrongBoxBacked;

        final Context mContext;

        /**
         * Creates a builder for a {@link MasterKey} using the default alias of
         * {@link #DEFAULT_MASTER_KEY_ALIAS}.
         *
         * @param context The context to use with this master key.
         */
        public Builder(@NonNull Context context) {
            this(context, MasterKey.DEFAULT_MASTER_KEY_ALIAS);
        }

        /**
         * Creates a builder for a {@link MasterKey}.
         *
         * @param context The context to use with this master key.
         */
        public Builder(@NonNull Context context, @NonNull String keyAlias) {
            mContext = context.getApplicationContext();
            mKeyAlias = keyAlias;
        }

        /**
         * Sets a {@link KeyScheme} to be used for the master key.
         * This uses a default {@link KeyGenParameterSpec} associated with the provided
         * {@code KeyScheme}.
         * NOTE: Either this method OR {@link #setKeyGenParameterSpec} should be used to set
         * the parameters to use for building the master key. Calling either function after
         * the other will throw an {@link IllegalArgumentException}.
         *
         * @param keyScheme The KeyScheme to use.
         * @return This builder.
         */
        @NonNull
        public Builder setKeyScheme(@NonNull KeyScheme keyScheme) {
            switch (keyScheme) {
                case AES256_GCM:
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (mKeyGenParameterSpec != null) {
                            throw new IllegalArgumentException("KeyScheme set after setting a "
                                    + "KeyGenParamSpec");
                        }
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported scheme: " + keyScheme);
            }
            mKeyScheme = keyScheme;
            return this;
        }

        /**
         * When used with {@link #setKeyScheme(KeyScheme)}, sets that the built master key should
         * require the user to authenticate before it's unlocked, probably using the
         * androidx.biometric library.
         *
         * This method sets the validity duration of the key to
         * {@link #getDefaultAuthenticationValidityDurationSeconds()}.
         *
         * @param authenticationRequired Whether user authentication should be required to use
         *                               the key.
         * @return This builder.
         */
        @NonNull
        public Builder setUserAuthenticationRequired(boolean authenticationRequired) {
            return setUserAuthenticationRequired(authenticationRequired,
                    getDefaultAuthenticationValidityDurationSeconds());
        }

        /**
         * When used with {@link #setKeyScheme(KeyScheme)}, sets that the built master key should
         * require the user to authenticate before it's unlocked, probably using the
         * androidx.biometric library, and that the key should remain unlocked for the provided
         * duration.
         *
         * @param authenticationRequired                    Whether user authentication should be
         *                                                  required to use the key.
         * @param userAuthenticationValidityDurationSeconds Duration in seconds that the key
         *                                                  should remain unlocked following user
         *                                                  authentication.
         * @return This builder.
         */
        @NonNull
        public Builder setUserAuthenticationRequired(boolean authenticationRequired,
                @IntRange(from = 1) int userAuthenticationValidityDurationSeconds) {
            mAuthenticationRequired = authenticationRequired;
            mUserAuthenticationValidityDurationSeconds = userAuthenticationValidityDurationSeconds;
            return this;
        }

        /**
         * Sets whether or not to request this key is strong box backed. This setting is only
         * applicable on {@link Build.VERSION_CODES#P} and above, and only on devices that
         * support Strongbox.
         *
         * @param requestStrongBoxBacked Whether to request to use strongbox
         * @return This builder.
         */
        @NonNull
        public Builder setRequestStrongBoxBacked(boolean requestStrongBoxBacked) {
            mRequestStrongBoxBacked = requestStrongBoxBacked;
            return this;
        }

        /**
         * Sets a custom {@link KeyGenParameterSpec} to use as the basis of the master key.
         * NOTE: Either this method OR {@link #setKeyScheme(KeyScheme)} should be used to set
         * the parameters to use for building the master key. Calling either function after
         * the other will throw an {@link IllegalArgumentException}.
         *
         * @param keyGenParameterSpec The key spec to use.
         * @return This builder.
         */
        @NonNull
        @RequiresApi(Build.VERSION_CODES.M)
        public Builder setKeyGenParameterSpec(@NonNull KeyGenParameterSpec keyGenParameterSpec) {
            if (mKeyScheme != null) {
                throw new IllegalArgumentException("KeyGenParamSpec set after setting a "
                        + "KeyScheme");
            }
            if (!mKeyAlias.equals(Api23Impl.getKeystoreAlias(keyGenParameterSpec))) {
                throw new IllegalArgumentException("KeyGenParamSpec's key alias does not match "
                        + "provided alias (" + mKeyAlias + " vs "
                        + Api23Impl.getKeystoreAlias(keyGenParameterSpec));
            }
            mKeyGenParameterSpec = keyGenParameterSpec;
            return this;
        }

        /**
         * Builds a {@link MasterKey} from this builder.
         *
         * @return The master key.
         */
        @NonNull
        public MasterKey build() throws GeneralSecurityException, IOException {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return Api23Impl.build(this);
            } else {
                return new MasterKey(mKeyAlias, null);
            }
        }

        @RequiresApi(23)
        static class Api23Impl {
            private Api23Impl() {
                // This class is not instantiable.
            }

            @DoNotInline
            static String getKeystoreAlias(KeyGenParameterSpec keyGenParameterSpec) {
                return keyGenParameterSpec.getKeystoreAlias();
            }

            @SuppressWarnings("deprecation")
            static MasterKey build(Builder builder) throws GeneralSecurityException, IOException {
                if (builder.mKeyScheme == null && builder.mKeyGenParameterSpec == null) {
                    throw new IllegalArgumentException("build() called before "
                            + "setKeyGenParameterSpec or setKeyScheme.");
                }

                if (builder.mKeyScheme == KeyScheme.AES256_GCM) {
                    KeyGenParameterSpec.Builder keyGenBuilder = new KeyGenParameterSpec.Builder(
                            builder.mKeyAlias,
                            KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                            .setKeySize(DEFAULT_AES_GCM_MASTER_KEY_SIZE);

                    if (builder.mAuthenticationRequired) {
                        keyGenBuilder.setUserAuthenticationRequired(true);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            Api30Impl.setUserAuthenticationParameters(keyGenBuilder,
                                    builder.mUserAuthenticationValidityDurationSeconds,
                                    AUTH_DEVICE_CREDENTIAL | AUTH_BIOMETRIC_STRONG);
                        } else {
                            keyGenBuilder.setUserAuthenticationValidityDurationSeconds(
                                    builder.mUserAuthenticationValidityDurationSeconds);
                        }
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                            && builder.mRequestStrongBoxBacked) {
                        if (builder.mContext.getPackageManager().hasSystemFeature(
                                PackageManager.FEATURE_STRONGBOX_KEYSTORE)) {
                            Api28Impl.setIsStrongBoxBacked(keyGenBuilder);
                        }
                    }

                    builder.mKeyGenParameterSpec = keyGenBuilder.build();
                }
                if (builder.mKeyGenParameterSpec == null) {
                    // This really should not happen.
                    throw new NullPointerException(
                            "KeyGenParameterSpec was null after build() check");
                }

                String keyAlias = MasterKeys.getOrCreate(builder.mKeyGenParameterSpec);
                return new MasterKey(keyAlias, builder.mKeyGenParameterSpec);
            }

            @RequiresApi(28)
            static class Api28Impl {
                private Api28Impl() {
                    // This class is not instantiable.
                }

                @DoNotInline
                static void setIsStrongBoxBacked(KeyGenParameterSpec.Builder builder) {
                    builder.setIsStrongBoxBacked(true);
                }
            }

            @RequiresApi(30)
            static class Api30Impl {
                private Api30Impl() {
                    // This class is not instantiable.
                }

                @DoNotInline
                static void setUserAuthenticationParameters(KeyGenParameterSpec.Builder builder,
                        int timeout,
                        int type) {
                    builder.setUserAuthenticationParameters(timeout, type);
                }

            }
        }
    }

    @RequiresApi(23)
    static class Api23Impl {
        private Api23Impl() {
            // This class is not instantiable.
        }

        @DoNotInline
        static boolean isUserAuthenticationRequired(KeyGenParameterSpec keyGenParameterSpec) {
            return keyGenParameterSpec.isUserAuthenticationRequired();
        }

        @DoNotInline
        static int getUserAuthenticationValidityDurationSeconds(
                KeyGenParameterSpec keyGenParameterSpec) {
            return keyGenParameterSpec.getUserAuthenticationValidityDurationSeconds();
        }
    }

    @RequiresApi(28)
    static class Api28Impl {
        private Api28Impl() {
            // This class is not instantiable.
        }

        @DoNotInline
        static boolean isStrongBoxBacked(KeyGenParameterSpec keyGenParameterSpec) {
            return keyGenParameterSpec.isStrongBoxBacked();
        }

    }
}
