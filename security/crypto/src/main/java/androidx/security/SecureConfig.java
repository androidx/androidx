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


package androidx.security;

import android.security.keystore.KeyProperties;

import androidx.annotation.NonNull;
import androidx.security.biometric.BiometricKeyAuthCallback;
import androidx.security.config.TrustAnchorOptions;

/**
 * Class that defines constants used by the library. Includes predefined configurations for:
 *
 * Default:
 * SecureConfig.getDefault() provides a good basic security configuration for encrypting data
 * both in transit and at rest.
 *
 * NIAP:
 * For government use, SecureConfig.getNiapConfig(biometric auth) which returns compliant security
 * settings for NIAP use cases.
 *
 *
 */
public class SecureConfig {

    public static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    public static final String ANDROID_CA_STORE = "AndroidCAStore";
    public static final int AES_IV_SIZE_BYTES = 16;
    public static final String SSL_TLS = "TLS";

    String mAndroidKeyStore;
    String mAndroidCAStore;
    String mKeystoreType;

    // Asymmetric Encryption Constants
    String mAsymmetricKeyPairAlgorithm;
    int mAsymmetricKeySize;
    String mAsymmetricCipherTransformation;
    String mAsymmetricBlockModes;
    String mAsymmetricPaddings;
    int mAsymmetricKeyPurposes;
    // Sets KeyGenBuilder#setUnlockedDeviceRequired to true, requires Android 9 Pie.
    boolean mAsymmetricSensitiveDataProtection;
    boolean mAsymmetricRequireUserAuth;
    int mAsymmetricRequireUserValiditySeconds;
    String mAsymmetricDigests;

    // Symmetric Encryption Constants
    String mSymmetricKeyAlgorithm;
    String mSymmetricBlockModes;
    String mSymmetricPaddings;
    int mSymmetricKeySize;
    int mSymmetricGcmTagLength;
    int mSymmetricKeyPurposes;
    String mSymmetricCipherTransformation;
    // Sets KeyGenBuilder#setUnlockedDeviceRequired to true, requires Android 9 Pie.
    boolean mSymmetricSensitiveDataProtection;
    boolean mSymmetricRequireUserAuth;
    int mSymmetricRequireUserValiditySeconds;
    private String mSymmetricDigests;

    // Certificate Constants
    String mCertPath;
    String mCertPathValidator;
    boolean mUseStrongSSLCiphers;
    String[] mStrongSSLCiphers;
    String[] mClientCertAlgorithms;
    TrustAnchorOptions mTrustAnchorOptions;

    BiometricKeyAuthCallback mBiometricKeyAuthCallback;

    String mSignatureAlgorithm;

    SecureConfig() {
    }

    /**
     * SecureConfig.Builder configures SecureConfig.
     */
    public static class Builder {

        public Builder() {
        }

        // Keystore Constants
        String mAndroidKeyStore;
        String mAndroidCAStore;
        String mKeystoreType;

        // Asymmetric Encryption Constants
        String mAsymmetricKeyPairAlgorithm;
        int mAsymmetricKeySize;
        String mAsymmetricCipherTransformation;
        int mAsymmetricKeyPurposes;
        String mAsymmetricBlockModes;
        String mAsymmetricPaddings;
        boolean mAsymmetricSensitiveDataProtection;
        boolean mAsymmetricRequireUserAuth;
        int mAsymmetricRequireUserValiditySeconds;

        /**
         * Sets the keystore type
         *
         * @param keystoreType the KeystoreType to set
         * @return
         */
        @NonNull
        public Builder forKeyStoreType(@NonNull String keystoreType) {
            this.mKeystoreType = keystoreType;
            return this;
        }

        /**
         * Sets the key pair algorithm.
         *
         * @param keyPairAlgorithm the key pair algorithm
         * @return The configured builder
         */
        @NonNull
        public Builder setAsymmetricKeyPairAlgorithm(@NonNull String keyPairAlgorithm) {
            this.mAsymmetricKeyPairAlgorithm = keyPairAlgorithm;
            return this;
        }

        /**
         * @param keySize
         * @return The configured builder
         */
        @NonNull
        public Builder setAsymmetricKeySize(int keySize) {
            this.mAsymmetricKeySize = keySize;
            return this;
        }

        /**
         * @param cipherTransformation
         * @return The configured builder
         */
        @NonNull
        public Builder setAsymmetricCipherTransformation(@NonNull String cipherTransformation) {
            this.mAsymmetricCipherTransformation = cipherTransformation;
            return this;
        }

        /**
         * @param purposes
         * @return The configured builder
         */
        @NonNull
        public Builder setAsymmetricKeyPurposes(int purposes) {
            this.mAsymmetricKeyPurposes = purposes;
            return this;
        }

        /**
         * @param blockModes
         * @return The configured builder
         */
        @NonNull
        public Builder setAsymmetricBlockModes(@NonNull String blockModes) {
            this.mAsymmetricBlockModes = blockModes;
            return this;
        }

        /**
         * @param paddings
         * @return The configured builder
         */
        @NonNull
        public Builder setAsymmetricPaddings(@NonNull String paddings) {
            this.mAsymmetricPaddings = paddings;
            return this;
        }

        /**
         * @param dataProtection
         * @return The configured builder
         */
        @NonNull
        public Builder setAsymmetricSensitiveDataProtection(boolean dataProtection) {
            this.mAsymmetricSensitiveDataProtection = dataProtection;
            return this;
        }

        /**
         * @param userAuth
         * @return The configured builder
         */
        @NonNull
        public Builder setAsymmetricRequireUserAuth(boolean userAuth) {
            this.mAsymmetricRequireUserAuth = userAuth;
            return this;
        }

        /**
         * @param authValiditySeconds
         * @return The configured builder
         */
        @NonNull
        public Builder setAsymmetricRequireUserValiditySeconds(int authValiditySeconds) {
            this.mAsymmetricRequireUserValiditySeconds = authValiditySeconds;
            return this;
        }

        // Symmetric Encryption Constants
        String mSymmetricKeyAlgorithm;
        String mSymmetricBlockModes;
        String mSymmetricPaddings;
        int mSymmetricKeySize;
        int mSymmetricGcmTagLength;
        int mSymmetricKeyPurposes;
        String mSymmetricCipherTransformation;
        boolean mSymmetricSensitiveDataProtection;
        boolean mSymmetricRequireUserAuth;
        int mSymmetricRequireUserValiditySeconds;

        /**
         * @param keyAlgorithm
         * @return The configured builder
         */
        @NonNull
        public Builder setSymmetricKeyAlgorithm(@NonNull String keyAlgorithm) {
            this.mSymmetricKeyAlgorithm = keyAlgorithm;
            return this;
        }

        /**
         * @param keySize
         * @return The configured builder
         */
        @NonNull
        public Builder setSymmetricKeySize(int keySize) {
            this.mSymmetricKeySize = keySize;
            return this;
        }

        /**
         * @param cipherTransformation
         * @return The configured builder
         */
        @NonNull
        public Builder setSymmetricCipherTransformation(@NonNull String cipherTransformation) {
            this.mSymmetricCipherTransformation = cipherTransformation;
            return this;
        }

        /**
         * @param purposes
         * @return The configured builder
         */
        @NonNull
        public Builder setSymmetricKeyPurposes(int purposes) {
            this.mSymmetricKeyPurposes = purposes;
            return this;
        }

        /**
         * @param gcmTagLength
         * @return The configured builder
         */
        @NonNull
        public Builder setSymmetricGcmTagLength(int gcmTagLength) {
            this.mSymmetricGcmTagLength = gcmTagLength;
            return this;
        }

        /**
         * @param blockModes
         * @return The configured builder
         */
        @NonNull
        public Builder setSymmetricBlockModes(@NonNull String blockModes) {
            this.mSymmetricBlockModes = blockModes;
            return this;
        }

        /**
         * @param paddings
         * @return The configured builder
         */
        @NonNull
        public Builder setSymmetricPaddings(@NonNull String paddings) {
            this.mSymmetricPaddings = paddings;
            return this;
        }

        /**
         * @param dataProtection
         * @return The configured builder
         */
        @NonNull
        public Builder setSymmetricSensitiveDataProtection(boolean dataProtection) {
            this.mSymmetricSensitiveDataProtection = dataProtection;
            return this;
        }

        /**
         * @param userAuth
         * @return The configured builder
         */
        @NonNull
        public Builder setSymmetricRequireUserAuth(boolean userAuth) {
            this.mSymmetricRequireUserAuth = userAuth;
            return this;
        }

        /**
         * @param authValiditySeconds
         * @return The configured builder
         */
        @NonNull
        public Builder setSymmetricRequireUserValiditySeconds(int authValiditySeconds) {
            this.mSymmetricRequireUserValiditySeconds = authValiditySeconds;
            return this;
        }

        // Certificate Constants
        String mCertPath;
        String mCertPathValidator;
        boolean mUseStrongSSLCiphers;
        String[] mStrongSSLCiphers;
        String[] mClientCertAlgorithms;
        TrustAnchorOptions mAnchorOptions;
        BiometricKeyAuthCallback mBiometricKeyAuthCallback;
        String mSignatureAlgorithm;

        /**
         * @param certPath
         * @return The configured builder
         */
        @NonNull
        public Builder setCertPath(@NonNull String certPath) {
            this.mCertPath = certPath;
            return this;
        }

        /**
         * @param certPathValidator
         * @return The configured builder
         */
        @NonNull
        public Builder setCertPathValidator(@NonNull String certPathValidator) {
            this.mCertPathValidator = certPathValidator;
            return this;
        }

        /**
         * @param strongSSLCiphers
         * @return The configured builder
         */
        @NonNull
        public Builder setUseStrongSSLCiphers(boolean strongSSLCiphers) {
            this.mUseStrongSSLCiphers = strongSSLCiphers;
            return this;
        }

        /**
         * @param strongSSLCiphers
         * @return The configured builder
         */
        @NonNull
        public Builder setStrongSSLCiphers(@NonNull String[] strongSSLCiphers) {
            this.mStrongSSLCiphers = strongSSLCiphers;
            return this;
        }

        /**
         * @param clientCertAlgorithms
         * @return The configured builder
         */
        @NonNull
        public Builder setClientCertAlgorithms(@NonNull String[] clientCertAlgorithms) {
            this.mClientCertAlgorithms = clientCertAlgorithms;
            return this;
        }

        /**
         * @param trustAnchorOptions
         * @return The configured builder
         */
        @NonNull
        public Builder setTrustAnchorOptions(@NonNull TrustAnchorOptions trustAnchorOptions) {
            this.mAnchorOptions = trustAnchorOptions;
            return this;
        }

        /**
         * @param biometricKeyAuthCallback
         * @return The configured builder
         */
        @NonNull
        public Builder setBiometricKeyAuthCallback(
                @NonNull BiometricKeyAuthCallback biometricKeyAuthCallback) {
            this.mBiometricKeyAuthCallback = biometricKeyAuthCallback;
            return this;
        }

        /**
         * @param signatureAlgorithm
         * @return The configured builder
         */
        @NonNull
        public Builder setSignatureAlgorithm(
                @NonNull String signatureAlgorithm) {
            this.mSignatureAlgorithm = signatureAlgorithm;
            return this;
        }



        /**
         * @return The configured builder
         */
        @NonNull
        public SecureConfig build() {
            SecureConfig secureConfig = new SecureConfig();
            secureConfig.mAndroidKeyStore = this.mAndroidKeyStore;
            secureConfig.mAndroidCAStore = this.mAndroidCAStore;
            secureConfig.mKeystoreType = this.mKeystoreType;

            secureConfig.mAsymmetricKeyPairAlgorithm = this.mAsymmetricKeyPairAlgorithm;
            secureConfig.mAsymmetricKeySize = this.mAsymmetricKeySize;
            secureConfig.mAsymmetricCipherTransformation = this.mAsymmetricCipherTransformation;
            secureConfig.mAsymmetricKeyPurposes = this.mAsymmetricKeyPurposes;
            secureConfig.mAsymmetricBlockModes = this.mAsymmetricBlockModes;
            secureConfig.mAsymmetricPaddings = this.mAsymmetricPaddings;
            secureConfig.mAsymmetricSensitiveDataProtection =
                    this.mAsymmetricSensitiveDataProtection;
            secureConfig.mAsymmetricRequireUserAuth = this.mAsymmetricRequireUserAuth;
            secureConfig.mAsymmetricRequireUserValiditySeconds =
                    this.mAsymmetricRequireUserValiditySeconds;

            secureConfig.mSymmetricKeyAlgorithm = this.mSymmetricKeyAlgorithm;
            secureConfig.mSymmetricBlockModes = this.mSymmetricBlockModes;
            secureConfig.mSymmetricPaddings = this.mSymmetricPaddings;
            secureConfig.mSymmetricKeySize = this.mSymmetricKeySize;
            secureConfig.mSymmetricGcmTagLength = this.mSymmetricGcmTagLength;
            secureConfig.mSymmetricKeyPurposes = this.mSymmetricKeyPurposes;
            secureConfig.mSymmetricCipherTransformation = this.mSymmetricCipherTransformation;
            secureConfig.mSymmetricSensitiveDataProtection =
                    this.mSymmetricSensitiveDataProtection;
            secureConfig.mSymmetricRequireUserAuth = this.mSymmetricRequireUserAuth;
            secureConfig.mSymmetricRequireUserValiditySeconds =
                    this.mSymmetricRequireUserValiditySeconds;

            secureConfig.mCertPath = this.mCertPath;
            secureConfig.mCertPathValidator = this.mCertPathValidator;
            secureConfig.mUseStrongSSLCiphers = this.mUseStrongSSLCiphers;
            secureConfig.mStrongSSLCiphers = this.mStrongSSLCiphers;
            secureConfig.mClientCertAlgorithms = this.mClientCertAlgorithms;
            secureConfig.mTrustAnchorOptions = this.mAnchorOptions;
            secureConfig.mBiometricKeyAuthCallback = this.mBiometricKeyAuthCallback;
            secureConfig.mSignatureAlgorithm = this.mSignatureAlgorithm;
            return secureConfig;
        }
    }

    /**
     * @return A NIAP compliant configuration.
     */
    @NonNull
    public static SecureConfig getNiapConfig() {
        return getNiapConfig(null);
    }

    /**
     * @return A default configuration with for consumer applications.
     */
    @NonNull
    public static SecureConfig getDefault() {
        SecureConfig.Builder builder = new SecureConfig.Builder();
        builder.mAndroidKeyStore = SecureConfig.ANDROID_KEYSTORE;
        builder.mAndroidCAStore = SecureConfig.ANDROID_CA_STORE;
        builder.mKeystoreType = "PKCS12";

        builder.mAsymmetricKeyPairAlgorithm = KeyProperties.KEY_ALGORITHM_RSA;
        builder.mAsymmetricKeySize = 2048;
        builder.mAsymmetricCipherTransformation = "RSA/ECB/PKCS1Padding";
        builder.mAsymmetricBlockModes = KeyProperties.BLOCK_MODE_ECB;
        builder.mAsymmetricPaddings = KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1;
        builder.mAsymmetricKeyPurposes = KeyProperties.PURPOSE_DECRYPT | KeyProperties.PURPOSE_SIGN;
        builder.mAsymmetricSensitiveDataProtection = false;
        builder.mAsymmetricRequireUserAuth = false;
        builder.mAsymmetricRequireUserValiditySeconds = -1;

        builder.mSymmetricKeyAlgorithm = KeyProperties.KEY_ALGORITHM_AES;
        builder.mSymmetricBlockModes = KeyProperties.BLOCK_MODE_GCM;
        builder.mSymmetricPaddings = KeyProperties.ENCRYPTION_PADDING_NONE;
        builder.mSymmetricKeySize = 256;
        builder.mSymmetricGcmTagLength = 128;
        builder.mSymmetricKeyPurposes =
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT;
        builder.mSymmetricCipherTransformation = "AES/GCM/NoPadding";
        builder.mSymmetricSensitiveDataProtection = false;
        builder.mSymmetricRequireUserAuth = false;
        builder.mSymmetricRequireUserValiditySeconds = -1;

        builder.mCertPath = "X.509";
        builder.mCertPathValidator = "PKIX";
        builder.mUseStrongSSLCiphers = false;
        builder.mStrongSSLCiphers = null;
        builder.mClientCertAlgorithms = new String[]{"RSA"};
        builder.mAnchorOptions = TrustAnchorOptions.USER_SYSTEM;
        builder.mBiometricKeyAuthCallback = null;
        builder.mSignatureAlgorithm = "SHA256withECDSA";

        return builder.build();
    }

    /**
     * Create a Niap compliant configuration
     *
     * -Insert Link to Spec
     *
     * @param biometricKeyAuthCallback
     * @return The NIAP compliant configuration
     */
    @NonNull
    public static SecureConfig getNiapConfig(
            @NonNull BiometricKeyAuthCallback biometricKeyAuthCallback) {
        SecureConfig.Builder builder = new SecureConfig.Builder();
        builder.mAndroidKeyStore = SecureConfig.ANDROID_KEYSTORE;
        builder.mAndroidCAStore = SecureConfig.ANDROID_CA_STORE;
        builder.mKeystoreType = "PKCS12";

        builder.mAsymmetricKeyPairAlgorithm = KeyProperties.KEY_ALGORITHM_RSA;
        builder.mAsymmetricKeySize = 4096;
        builder.mAsymmetricCipherTransformation = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
        builder.mAsymmetricBlockModes = KeyProperties.BLOCK_MODE_ECB;
        builder.mAsymmetricPaddings = KeyProperties.ENCRYPTION_PADDING_RSA_OAEP;
        builder.mAsymmetricKeyPurposes = KeyProperties.PURPOSE_DECRYPT | KeyProperties.PURPOSE_SIGN;
        builder.mAsymmetricSensitiveDataProtection = true;
        builder.mAsymmetricRequireUserAuth = true;
        builder.mAsymmetricRequireUserValiditySeconds = -1;

        builder.mSymmetricKeyAlgorithm = KeyProperties.KEY_ALGORITHM_AES;
        builder.mSymmetricBlockModes = KeyProperties.BLOCK_MODE_GCM;
        builder.mSymmetricPaddings = KeyProperties.ENCRYPTION_PADDING_NONE;
        builder.mSymmetricKeySize = 256;
        builder.mSymmetricGcmTagLength = 128;
        builder.mSymmetricKeyPurposes =
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT;
        builder.mSymmetricCipherTransformation = "AES/GCM/NoPadding";
        builder.mSymmetricSensitiveDataProtection = true;
        builder.mSymmetricRequireUserAuth = true;
        builder.mSymmetricRequireUserValiditySeconds = -1;

        builder.mCertPath = "X.509";
        builder.mCertPathValidator = "PKIX";
        builder.mUseStrongSSLCiphers = false;
        builder.mStrongSSLCiphers = new String[]{
                "TLS_RSA_WITH_AES_128_CBC_SHA256",
                "TLS_RSA_WITH_AES_256_CBC_SHA256",
                "TLS_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_RSA_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256",
                "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384",
                "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
                "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
                "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256"
        };
        builder.mClientCertAlgorithms = new String[]{"RSA"};
        builder.mAnchorOptions = TrustAnchorOptions.USER_SYSTEM;
        builder.mBiometricKeyAuthCallback = biometricKeyAuthCallback;
        builder.mSignatureAlgorithm = "SHA512withRSA/PSS";

        return builder.build();
    }

    /**
     * @return
     */
    @NonNull
    public String getAndroidKeyStore() {
        return mAndroidKeyStore;
    }

    public void setAndroidKeyStore(@NonNull String androidKeyStore) {
        this.mAndroidKeyStore = androidKeyStore;
    }

    /**
     * @return
     */
    @NonNull
    public String getAndroidCAStore() {
        return mAndroidCAStore;
    }

    public void setAndroidCAStore(@NonNull String androidCAStore) {
        this.mAndroidCAStore = androidCAStore;
    }

    /**
     * @return
     */
    @NonNull
    public String getKeystoreType() {
        return mKeystoreType;
    }

    /**
     * @param keystoreType
     */
    @NonNull
    public void setKeystoreType(@NonNull String keystoreType) {
        this.mKeystoreType = keystoreType;
    }

    /**
     * @return
     */
    @NonNull
    public String getAsymmetricKeyPairAlgorithm() {
        return mAsymmetricKeyPairAlgorithm;
    }

    /**
     * @param asymmetricKeyPairAlgorithm
     */
    public void setAsymmetricKeyPairAlgorithm(@NonNull String asymmetricKeyPairAlgorithm) {
        this.mAsymmetricKeyPairAlgorithm = asymmetricKeyPairAlgorithm;
    }

    /**
     * @return
     */
    public int getAsymmetricKeySize() {
        return mAsymmetricKeySize;
    }

    /**
     * @param asymmetricKeySize
     */
    public void setAsymmetricKeySize(int asymmetricKeySize) {
        this.mAsymmetricKeySize = asymmetricKeySize;
    }

    /**
     * @return
     */
    @NonNull
    public String getAsymmetricCipherTransformation() {
        return mAsymmetricCipherTransformation;
    }

    /**
     * @param asymmetricCipherTransformation
     */
    public void setAsymmetricCipherTransformation(@NonNull String asymmetricCipherTransformation) {
        this.mAsymmetricCipherTransformation = asymmetricCipherTransformation;
    }

    /**
     * @return
     */
    @NonNull
    public String getAsymmetricBlockModes() {
        return mAsymmetricBlockModes;
    }

    /**
     * @param asymmetricBlockModes
     */
    public void setAsymmetricBlockModes(@NonNull String asymmetricBlockModes) {
        this.mAsymmetricBlockModes = asymmetricBlockModes;
    }

    /**
     * @return
     */
    @NonNull
    public String getAsymmetricPaddings() {
        return mAsymmetricPaddings;
    }

    /**
     * @param asymmetricPaddings
     */
    public void setAsymmetricPaddings(@NonNull String asymmetricPaddings) {
        this.mAsymmetricPaddings = asymmetricPaddings;
    }

    /**
     * @return
     */
    public int getAsymmetricKeyPurposes() {
        return mAsymmetricKeyPurposes;
    }

    /**
     * @param asymmetricKeyPurposes
     */
    public void setAsymmetricKeyPurposes(int asymmetricKeyPurposes) {
        this.mAsymmetricKeyPurposes = asymmetricKeyPurposes;
    }

    /**
     * @return
     */
    public boolean getAsymmetricSensitiveDataProtectionEnabled() {
        return mAsymmetricSensitiveDataProtection;
    }

    /**
     * @param asymmetricSensitiveDataProtection
     */
    public void setAsymmetricSensitiveDataProtection(boolean asymmetricSensitiveDataProtection) {
        this.mAsymmetricSensitiveDataProtection = asymmetricSensitiveDataProtection;
    }

    /**
     * @return
     */
    public boolean getAsymmetricRequireUserAuthEnabled() {
        return mAsymmetricRequireUserAuth && mBiometricKeyAuthCallback != null;
    }

    /**
     * @param requireUserAuth
     */
    public void setAsymmetricRequireUserAuth(boolean requireUserAuth) {
        this.mAsymmetricRequireUserAuth = requireUserAuth;
    }

    /**
     * @return
     */
    public int getAsymmetricRequireUserValiditySeconds() {
        return this.mAsymmetricRequireUserValiditySeconds;
    }

    /**
     * @param userValiditySeconds
     */
    public void setAsymmetricRequireUserValiditySeconds(int userValiditySeconds) {
        this.mAsymmetricRequireUserValiditySeconds = userValiditySeconds;
    }

    /**
     * @return
     */
    @NonNull
    public String getSymmetricKeyAlgorithm() {
        return mSymmetricKeyAlgorithm;
    }

    /**
     * @param symmetricKeyAlgorithm
     */
    public void setSymmetricKeyAlgorithm(@NonNull String symmetricKeyAlgorithm) {
        this.mSymmetricKeyAlgorithm = symmetricKeyAlgorithm;
    }

    /**
     * @return
     */
    @NonNull
    public String getSymmetricBlockModes() {
        return mSymmetricBlockModes;
    }

    /**
     * @param symmetricBlockModes
     */
    public void setSymmetricBlockModes(@NonNull String symmetricBlockModes) {
        this.mSymmetricBlockModes = symmetricBlockModes;
    }

    /**
     * @return
     */
    @NonNull
    public String getSymmetricPaddings() {
        return mSymmetricPaddings;
    }

    /**
     * @param symmetricPaddings
     */
    public void setSymmetricPaddings(@NonNull String symmetricPaddings) {
        this.mSymmetricPaddings = symmetricPaddings;
    }

    /**
     * @return
     */
    public int getSymmetricKeySize() {
        return mSymmetricKeySize;
    }

    /**
     * @param symmetricKeySize
     */
    public void setSymmetricKeySize(int symmetricKeySize) {
        this.mSymmetricKeySize = symmetricKeySize;
    }

    /**
     * @return
     */
    public int getSymmetricGcmTagLength() {
        return mSymmetricGcmTagLength;
    }

    /**
     * @param symmetricGcmTagLength
     */
    public void setSymmetricGcmTagLength(int symmetricGcmTagLength) {
        this.mSymmetricGcmTagLength = symmetricGcmTagLength;
    }

    /**
     * @return
     */
    public int getSymmetricKeyPurposes() {
        return mSymmetricKeyPurposes;
    }

    /**
     * @param symmetricKeyPurposes
     */
    public void setSymmetricKeyPurposes(int symmetricKeyPurposes) {
        this.mSymmetricKeyPurposes = symmetricKeyPurposes;
    }

    /**
     * @return
     */
    @NonNull
    public String getSymmetricCipherTransformation() {
        return mSymmetricCipherTransformation;
    }

    /**
     * @param symmetricCipherTransformation
     */
    public void setSymmetricCipherTransformation(@NonNull String symmetricCipherTransformation) {
        this.mSymmetricCipherTransformation = symmetricCipherTransformation;
    }

    /**
     * @return
     */
    public boolean getSymmetricSensitiveDataProtectionEnabled() {
        return mSymmetricSensitiveDataProtection;
    }

    /**
     * @param symmetricSensitiveDataProtection
     */
    public void setSymmetricSensitiveDataProtection(boolean symmetricSensitiveDataProtection) {
        this.mSymmetricSensitiveDataProtection = symmetricSensitiveDataProtection;
    }

    public boolean getSymmetricRequireUserAuthEnabled() {
        return mSymmetricRequireUserAuth && mBiometricKeyAuthCallback != null;
    }

    /**
     * @param requireUserAuth
     */
    public void setSymmetricRequireUserAuth(boolean requireUserAuth) {
        this.mSymmetricRequireUserAuth = requireUserAuth;
    }

    /**
     * @return
     */
    public int getSymmetricRequireUserValiditySeconds() {
        return this.mSymmetricRequireUserValiditySeconds;
    }

    /**
     * @param userValiditySeconds
     */
    public void setSymmetricRequireUserValiditySeconds(int userValiditySeconds) {
        this.mSymmetricRequireUserValiditySeconds = userValiditySeconds;
    }

    /**
     * @return
     */
    @NonNull
    public String getCertPath() {
        return mCertPath;
    }

    public void setCertPath(@NonNull String certPath) {
        this.mCertPath = certPath;
    }

    /**
     * @return
     */
    @NonNull
    public String getCertPathValidator() {
        return mCertPathValidator;
    }

    /**
     * @param certPathValidator
     */
    public void setCertPathValidator(@NonNull String certPathValidator) {
        this.mCertPathValidator = certPathValidator;
    }

    /**
     * @return
     */
    public boolean getUseStrongSSLCiphersEnabled() {
        return mUseStrongSSLCiphers;
    }

    /**
     * @param useStrongSSLCiphers
     */
    public void setUseStrongSSLCiphers(boolean useStrongSSLCiphers) {
        this.mUseStrongSSLCiphers = useStrongSSLCiphers;
    }

    public boolean getUseStrongSSLCiphers() {
        return mUseStrongSSLCiphers;
    }

    /**
     * @return
     */
    @NonNull
    public String[] getStrongSSLCiphers() {
        return mStrongSSLCiphers;
    }

    /**
     * @param strongSSLCiphers
     */
    public void setStrongSSLCiphers(@NonNull String[] strongSSLCiphers) {
        this.mStrongSSLCiphers = strongSSLCiphers;
    }

    /**
     * @return
     */
    @NonNull
    public String[] getClientCertAlgorithms() {
        return mClientCertAlgorithms;
    }

    public void setClientCertAlgorithms(@NonNull String[] clientCertAlgorithms) {
        this.mClientCertAlgorithms = clientCertAlgorithms;
    }

    /**
     * @return
     */
    @NonNull
    public TrustAnchorOptions getTrustAnchorOptions() {
        return mTrustAnchorOptions;
    }

    /**
     * @param trustAnchorOptions
     */
    public void setTrustAnchorOptions(@NonNull TrustAnchorOptions trustAnchorOptions) {
        this.mTrustAnchorOptions = trustAnchorOptions;
    }

    /**
     * @return
     */
    @NonNull
    public BiometricKeyAuthCallback getBiometricKeyAuthCallback() {
        return mBiometricKeyAuthCallback;
    }

    /**
     * @param biometricKeyAuthCallback
     */
    public void setBiometricKeyAuthCallback(
            @NonNull BiometricKeyAuthCallback biometricKeyAuthCallback) {
        this.mBiometricKeyAuthCallback = biometricKeyAuthCallback;
    }

    /**
     * @return
     */
    @NonNull
    public String getSignatureAlgorithm() {
        return mSignatureAlgorithm;
    }

    /**
     * @param signatureAlgorithm
     */
    public void setSignatureAlgorithm(
            @NonNull String signatureAlgorithm) {
        this.mSignatureAlgorithm = signatureAlgorithm;
    }
}
