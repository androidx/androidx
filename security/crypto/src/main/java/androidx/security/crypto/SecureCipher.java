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

package androidx.security.crypto;

import android.os.Build;
import android.security.keystore.KeyProperties;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.security.SecureConfig;
import androidx.security.biometric.BiometricKeyAuthCallback;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.MGF1ParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;

/**
 * Class that helps encrypt and decrypt data.
 */
public class SecureCipher {

    private static final String TAG = "SecureCipher";
    private static final int FILE_ENCODING_VERSION = 1;

    private SecureConfig mSecureConfig;

    /**
     * Listener for encryption that requires biometric prompt
     */
    public interface SecureAuthListener {
        /**
         * @param status the status of auth
         */
        void authComplete(@NonNull BiometricKeyAuthCallback.BiometricStatus status);
    }

    /**
     * Listener for encrypting symmetric data
     */
    public interface SecureSymmetricEncryptionListener {
        /**
         * @param cipherText the encrypted cipher text
         * @param iv the initialization vector used
         */
        void encryptionComplete(@NonNull byte[] cipherText, @NonNull byte[] iv);
    }

    /**
     * Listener for encrypting asymmetric data
     */
    public interface SecureAsymmetricEncryptionListener {
        /**
         * @param cipherText the encrypted cipher text
         */
        void encryptionComplete(@NonNull byte[] cipherText);
    }

    /**
     * The listener for decryption
     */
    public interface SecureDecryptionListener {
        /**
         * @param clearText the decrypted text
         */
        void decryptionComplete(@NonNull byte[] clearText);
    }

    /**
     * The listener for signing data
     */
    public interface SecureSignListener {
        /**
         * @param signature the signature
         */
        void signComplete(@NonNull byte[] signature);
    }

    /**
     * @return The secure cipher
     */
    @NonNull
    public static SecureCipher getDefault() {
        return new SecureCipher(SecureConfig.getDefault());
    }

    /**
     * @param biometricKeyAuthCallback The callback to use when authenticating a key
     * @return the cipher using biometric prompt
     */
    @NonNull
    public static SecureCipher getDefault(
            @NonNull BiometricKeyAuthCallback biometricKeyAuthCallback) {
        SecureConfig secureConfig = SecureConfig.getDefault();
        secureConfig.setSymmetricRequireUserAuth(true);
        secureConfig.setAsymmetricRequireUserAuth(true);
        secureConfig.setBiometricKeyAuthCallback(biometricKeyAuthCallback);
        return new SecureCipher(secureConfig);
    }

    /**
     * Gets an instance using the specified configuration
     *
     * @param secureConfig the config
     * @return the secure cipher
     */
    @NonNull
    public static SecureCipher getInstance(@NonNull SecureConfig secureConfig) {
        return new SecureCipher(secureConfig);
    }


    public SecureCipher(@NonNull SecureConfig secureConfig) {
        this.mSecureConfig = secureConfig;
    }

    /**
     * The encoding mType for files
     */
    public enum SecureFileEncodingType {
        SYMMETRIC(0),
        ASYMMETRIC(1),
        EPHEMERAL(2),
        NOT_ENCRYPTED(1000);

        private final int mType;

        SecureFileEncodingType(int type) {
            this.mType = type;
        }

        /**
         * @return the id
         */
        public int getType() {
            return this.mType;
        }

        /**
         * @param id the id
         * @return the encoding mType
         */
        @NonNull
        public static SecureFileEncodingType fromId(int id) {
            switch (id) {
                case 0:
                    return SYMMETRIC;
                case 1:
                    return ASYMMETRIC;
                case 2:
                    return EPHEMERAL;
            }
            return NOT_ENCRYPTED;
        }

    }


    /**
     * Encrypts data with an existing key alias from the AndroidKeyStore.
     *
     * @param keyAlias  The name of the existing SecretKey to retrieve from the AndroidKeyStore.
     * @param clearData The unencrypted data to encrypt
     */
    public void encrypt(@NonNull String keyAlias,
            @NonNull final byte[] clearData,
            @NonNull final SecureSymmetricEncryptionListener callback) {
        try {
            KeyStore keyStore = KeyStore.getInstance(mSecureConfig.getAndroidKeyStore());
            keyStore.load(null);
            SecretKey key = (SecretKey) keyStore.getKey(keyAlias, null);
            final Cipher cipher = Cipher.getInstance(
                    mSecureConfig.getSymmetricCipherTransformation());
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] iv = cipher.getIV();
            if (mSecureConfig.getSymmetricRequireUserAuthEnabled()) {
                mSecureConfig.getBiometricKeyAuthCallback().authenticateKey(cipher,
                        new SecureAuthListener() {
                            public void authComplete(
                                    BiometricKeyAuthCallback.BiometricStatus status) {

                                switch (status) {
                                    case SUCCESS:
                                        try {
                                            callback.encryptionComplete(cipher.doFinal(clearData),
                                                    cipher.getIV());
                                        } catch (GeneralSecurityException e) {
                                            e.printStackTrace();
                                        }
                                        break;
                                    default:
                                        Log.i(TAG, "Failure");
                                        callback.encryptionComplete(null, null);
                                }
                            }
                        });
            } else {
                callback.encryptionComplete(cipher.doFinal(clearData), cipher.getIV());
            }
        } catch (GeneralSecurityException ex) {
            throw new SecurityException(ex);
        } catch (IOException ex) {
            throw new SecurityException(ex);
        }
    }

    /**
     * Signs data based on the specific key that has been generated
     *
     * Uses SecureConfig.getSignatureAlgorithm for algorithm type
     *
     * @param keyAlias The key to use for signing
     * @param clearData The data to sign
     * @param callback The listener to call back with the signature
     */
    public void sign(@NonNull String keyAlias,
            @NonNull final byte[] clearData,
            @NonNull final SecureSignListener callback) {
        byte[] dataSignature = new byte[0];
        try {
            KeyStore keyStore = KeyStore.getInstance(mSecureConfig.getAndroidKeyStore());
            keyStore.load(null);
            PrivateKey key = (PrivateKey) keyStore.getKey(keyAlias, null);
            final Signature signature = Signature.getInstance(
                    mSecureConfig.getSignatureAlgorithm());
            signature.initSign(key);
            signature.update(clearData);
            if (mSecureConfig.getAsymmetricRequireUserAuthEnabled()) {
                mSecureConfig.getBiometricKeyAuthCallback().authenticateKey(signature,
                        new SecureAuthListener() {
                            public void authComplete(
                                    BiometricKeyAuthCallback.BiometricStatus status) {
                                Log.i(TAG, "Finished success auth!");
                                switch (status) {
                                    case SUCCESS:
                                        try {
                                            byte[] sig = signature.sign();
                                            Log.i(TAG, "Signed " + clearData.length
                                                    + " bytes");
                                            callback.signComplete(sig);

                                        } catch (GeneralSecurityException e) {
                                            e.printStackTrace();
                                        }
                                        break;
                                    default:
                                        Log.i(TAG, "Failure");
                                        callback.signComplete(null);
                                }
                            }
                        });
            } else {
                dataSignature = signature.sign();
                callback.signComplete(dataSignature);
            }
        } catch (GeneralSecurityException ex) {
            ex.printStackTrace();
            //Log.e(TAG, ex.getMessage());
        } catch (IOException ex) {
            Log.e(TAG, ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Verifies signed data
     *
     * Uses SecureConfig.getSignatureAlgorithm for algorithm type
     *
     * @param keyAlias The key to use for signing
     * @param clearData The signed data
     * @param signature The signature
     * @return true if the provided signature is valid, false otherwise
     */
    public boolean verify(@NonNull String keyAlias,
            @NonNull final byte[] clearData,
            @NonNull final byte[] signature) {
        try {
            KeyStore keyStore = KeyStore.getInstance(mSecureConfig.getAndroidKeyStore());
            keyStore.load(null);
            PublicKey publicKey = keyStore.getCertificate(keyAlias).getPublicKey();
            final Signature signatureObject = Signature.getInstance(
                    mSecureConfig.getSignatureAlgorithm());
            signatureObject.initVerify(publicKey);
            signatureObject.update(clearData);
            return signatureObject.verify(signature);
        } catch (GeneralSecurityException ex) {
            ex.printStackTrace();
            //Log.e(TAG, ex.getMessage());
        } catch (IOException ex) {
            Log.e(TAG, ex.getMessage());
            ex.printStackTrace();
        }
        return false;
    }

    /**
     * Encrypts data with a public key from the cert in the AndroidKeyStore.
     *
     * @param keyAlias  The name of the existing KeyPair to retrieve the
     *                  PublicKey from the AndroidKeyStore.
     * @param clearData The unencrypted data to encrypt
     */
    public void encryptAsymmetric(@NonNull String keyAlias,
            @NonNull byte[] clearData, @NonNull SecureAsymmetricEncryptionListener callback) {
        try {
            KeyStore keyStore = KeyStore.getInstance(mSecureConfig.getAndroidKeyStore());
            keyStore.load(null);
            PublicKey publicKey = keyStore.getCertificate(keyAlias).getPublicKey();
            Cipher cipher = Cipher.getInstance(
                    mSecureConfig.getAsymmetricCipherTransformation());
            // Check to see if there are other padding types with a complex config.
            if (mSecureConfig.getAsymmetricPaddings().equals(
                    KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)) {
                cipher.init(Cipher.ENCRYPT_MODE, publicKey,
                        new OAEPParameterSpec("SHA-256",
                        "MGF1", new MGF1ParameterSpec("SHA-1"),
                                PSource.PSpecified.DEFAULT));
            } else {
                cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            }
            byte[] clearText = cipher.doFinal(clearData);
            callback.encryptionComplete(clearText);
        } catch (GeneralSecurityException ex) {
            throw new SecurityException(ex);
        } catch (IOException ex) {
            throw new SecurityException(ex);
        }
    }

    /**
     * Encrypts data using an Ephemeral key, destroying any trace of the key from the Cipher used.
     *
     * @param ephemeralSecretKey The generated Ephemeral key
     * @param clearData          The unencrypted data to encrypt
     * @return A Pair of byte[]'s, first is the encrypted data, second is the IV
     * (initialization vector)
     * used to encrypt which is required for decryption
     */
    @NonNull
    public Pair<byte[], byte[]> encryptEphemeralData(
            @NonNull EphemeralSecretKey ephemeralSecretKey, @NonNull byte[] clearData) {
        try {
            SecureRandom secureRandom = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                secureRandom = SecureRandom.getInstanceStrong();
            } else {
                secureRandom = new SecureRandom();
            }
            byte[] iv = new byte[SecureConfig.AES_IV_SIZE_BYTES];
            secureRandom.nextBytes(iv);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(
                    mSecureConfig.getSymmetricGcmTagLength(), iv);
            final Cipher cipher = Cipher.getInstance(
                    mSecureConfig.getSymmetricCipherTransformation());
            cipher.init(Cipher.ENCRYPT_MODE, ephemeralSecretKey, parameterSpec);
            byte[] encryptedData = cipher.doFinal(clearData);
            ephemeralSecretKey.destroyCipherKey(cipher, Cipher.ENCRYPT_MODE);
            return new Pair<>(encryptedData, iv);
        } catch (GeneralSecurityException ex) {
            throw new SecurityException(ex);
        }
    }

    /**
     * Decrypts a previously encrypted byte[]
     * <p>
     * Destroys all traces of the key data in the Cipher.
     *
     * @param ephemeralSecretKey   The generated Ephemeral key
     * @param encryptedData        The byte[] of encrypted data
     * @param initializationVector The IV of which the encrypted data was encrypted with
     * @return The byte[] of data that has been decrypted
     */
    @NonNull
    public byte[] decryptEphemeralData(@NonNull EphemeralSecretKey ephemeralSecretKey,
            @NonNull byte[] encryptedData, @NonNull byte[] initializationVector) {
        try {
            final Cipher cipher = Cipher.getInstance(
                    mSecureConfig.getSymmetricCipherTransformation());
            cipher.init(Cipher.DECRYPT_MODE, ephemeralSecretKey,
                    new GCMParameterSpec(
                            mSecureConfig.getSymmetricGcmTagLength(), initializationVector));
            byte[] decryptedData = cipher.doFinal(encryptedData);
            ephemeralSecretKey.destroyCipherKey(cipher, Cipher.DECRYPT_MODE);
            return decryptedData;
        } catch (GeneralSecurityException ex) {
            throw new SecurityException(ex);
        }
    }

    /**
     * Decrypts a previously encrypted byte[]
     *
     * @param keyAlias             The name of the existing SecretKey to retrieve from the
     *                             AndroidKeyStore.
     * @param encryptedData        The byte[] of encrypted data
     * @param initializationVector The IV of which the encrypted data was encrypted with
     */
    public void decrypt(@NonNull String keyAlias,
            @NonNull final byte[] encryptedData, @NonNull byte[] initializationVector,
            @NonNull final SecureDecryptionListener callback) {
        byte[] decryptedData = new byte[0];
        try {
            KeyStore keyStore = KeyStore.getInstance(mSecureConfig.getAndroidKeyStore());
            keyStore.load(null);
            Key key = keyStore.getKey(keyAlias, null);
            final Cipher cipher = Cipher.getInstance(
                    mSecureConfig.getSymmetricCipherTransformation());
            GCMParameterSpec spec = new GCMParameterSpec(
                    mSecureConfig.getSymmetricGcmTagLength(), initializationVector);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);
            if (mSecureConfig.getSymmetricRequireUserAuthEnabled()) {
                mSecureConfig.getBiometricKeyAuthCallback().authenticateKey(cipher,
                        new SecureAuthListener() {
                            public void authComplete(
                                    BiometricKeyAuthCallback.BiometricStatus status) {
                                switch (status) {
                                    case SUCCESS:
                                        try {
                                            callback.decryptionComplete(
                                                    cipher.doFinal(encryptedData));
                                        } catch (GeneralSecurityException e) {
                                            e.printStackTrace();
                                        }
                                        break;
                                    default:
                                        Log.i(TAG, "Failure");
                                        callback.decryptionComplete(null);
                                }
                            }
                        });
            } else {
                callback.decryptionComplete(cipher.doFinal(encryptedData));
            }
        } catch (GeneralSecurityException ex) {
            throw new SecurityException(ex);
        } catch (IOException ex) {
            throw new SecurityException(ex);
        }
    }

    /**
     * Decrypts a previously encrypted byte[] with the PrivateKey
     *
     * @param keyAlias      The name of the existing KeyPair to retrieve from the AndroidKeyStore.
     * @param encryptedData The byte[] of encrypted data
     */
    public void decryptAsymmetric(@NonNull String keyAlias,
            @NonNull final byte[] encryptedData,
            @NonNull final SecureDecryptionListener callback) {
        byte[] decryptedData = new byte[0];
        try {
            KeyStore keyStore = KeyStore.getInstance(mSecureConfig.getAndroidKeyStore());
            keyStore.load(null);
            PrivateKey key = (PrivateKey) keyStore.getKey(keyAlias, null);
            final Cipher cipher = Cipher.getInstance(
                    mSecureConfig.getAsymmetricCipherTransformation());
            if (mSecureConfig.getAsymmetricPaddings().equals(
                    KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)) {
                cipher.init(Cipher.DECRYPT_MODE, key, new OAEPParameterSpec("SHA-256",
                        "MGF1",
                        new MGF1ParameterSpec("SHA-1"), PSource.PSpecified.DEFAULT));
            } else {
                cipher.init(Cipher.DECRYPT_MODE, key);
            }
            if (mSecureConfig.getAsymmetricRequireUserAuthEnabled()) {
                mSecureConfig.getBiometricKeyAuthCallback().authenticateKey(cipher,
                        new SecureAuthListener() {
                            public void authComplete(
                                    BiometricKeyAuthCallback.BiometricStatus status) {
                                Log.i(TAG, "Finished success auth!");
                                switch (status) {
                                    case SUCCESS:
                                        try {
                                            byte[] clearData = cipher.doFinal(encryptedData);
                                            Log.i(TAG, "Decrypted " + new String(clearData));
                                            callback.decryptionComplete(clearData);

                                        } catch (GeneralSecurityException e) {
                                            e.printStackTrace();
                                        }
                                        break;
                                    default:
                                        Log.i(TAG, "Failure");
                                        callback.decryptionComplete(null);
                                }
                            }
                        });
            } else {
                decryptedData = cipher.doFinal(encryptedData);
                callback.decryptionComplete(decryptedData);
            }
        } catch (GeneralSecurityException ex) {
            ex.printStackTrace();
            //Log.e(TAG, ex.getMessage());
        } catch (IOException ex) {
            Log.e(TAG, ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * @param keyPairAlias
     * @param encryptedKey
     * @param cipherText
     * @param iv
     * @return
     */
    @NonNull
    public byte[] encodeEphemeralData(@NonNull byte[] keyPairAlias, @NonNull byte[] encryptedKey,
            @NonNull byte[] cipherText, @NonNull byte[] iv) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(((Integer.SIZE / 8) * 5) + iv.length
                + keyPairAlias.length + encryptedKey.length + cipherText.length);
        byteBuffer.putInt(SecureFileEncodingType.EPHEMERAL.getType());
        byteBuffer.putInt(FILE_ENCODING_VERSION);
        byteBuffer.putInt(encryptedKey.length);
        byteBuffer.put(encryptedKey);
        byteBuffer.putInt(iv.length);
        byteBuffer.put(iv);
        byteBuffer.putInt(keyPairAlias.length);
        byteBuffer.put(keyPairAlias);
        byteBuffer.put(cipherText);
        return byteBuffer.array();
    }

    /**
     * @param keyAlias
     * @param cipherText
     * @param iv
     * @return
     */
    @NonNull
    public byte[] encodeSymmetricData(@NonNull byte[] keyAlias, @NonNull byte[] cipherText,
            @NonNull byte[] iv) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(((Integer.SIZE / 8) * 4) + iv.length
                + keyAlias.length + cipherText.length);
        byteBuffer.putInt(SecureFileEncodingType.SYMMETRIC.getType());
        byteBuffer.putInt(FILE_ENCODING_VERSION);
        byteBuffer.putInt(iv.length);
        byteBuffer.put(iv);
        byteBuffer.putInt(keyAlias.length);
        byteBuffer.put(keyAlias);
        byteBuffer.put(cipherText);
        return byteBuffer.array();
    }

    /**
     * @param keyPairAlias
     * @param cipherText
     * @return
     */
    @NonNull
    public byte[] encodeAsymmetricData(@NonNull byte[] keyPairAlias,
            @NonNull byte[] cipherText) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(((Integer.SIZE / 8) * 3)
                + keyPairAlias.length + cipherText.length);
        byteBuffer.putInt(SecureFileEncodingType.ASYMMETRIC.getType());
        byteBuffer.putInt(FILE_ENCODING_VERSION);
        byteBuffer.putInt(keyPairAlias.length);
        byteBuffer.put(keyPairAlias);
        byteBuffer.put(cipherText);
        return byteBuffer.array();
    }

    /**
     * @param encodedCipherText
     * @param callback
     */
    @SuppressWarnings("fallthrough")
    public void decryptEncodedData(@NonNull byte[] encodedCipherText,
            @NonNull final SecureDecryptionListener callback) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(encodedCipherText);
        int encodingTypeVal = byteBuffer.getInt();
        SecureFileEncodingType encodingType = SecureFileEncodingType.fromId(encodingTypeVal);
        int encodingVersion = byteBuffer.getInt();
        byte[] encodedEphKey = null;
        byte[] iv = null;
        String keyAlias = null;
        byte[] cipherText = null;

        switch (encodingType) {
            case EPHEMERAL:
                int encodedEphKeyLength = byteBuffer.getInt();
                encodedEphKey = new byte[encodedEphKeyLength];
                byteBuffer.get(encodedEphKey);
                // fall through
            case SYMMETRIC:
                int ivLength = byteBuffer.getInt();
                iv = new byte[ivLength];
                byteBuffer.get(iv);
                // fall through
            case ASYMMETRIC:
                int keyAliasLength = byteBuffer.getInt();
                byte[] keyAliasBytes = new byte[keyAliasLength];
                byteBuffer.get(keyAliasBytes);
                keyAlias = new String(keyAliasBytes);
                cipherText = new byte[byteBuffer.remaining()];
                byteBuffer.get(cipherText);
                break;
            case NOT_ENCRYPTED:
                throw new SecurityException("Cannot determine file mType.");
        }
        switch (encodingType) {
            case EPHEMERAL:
                final byte[] ephemeralCipherText = cipherText;
                final byte[] ephemeralIv = iv;
                decryptAsymmetric(keyAlias, encodedEphKey,
                        new SecureDecryptionListener() {
                            @java.lang.Override
                            public void decryptionComplete(byte[] clearText) {
                                EphemeralSecretKey ephemeralSecretKey =
                                        new EphemeralSecretKey(clearText);
                                byte[] decrypted = decryptEphemeralData(
                                        ephemeralSecretKey,
                                        ephemeralCipherText, ephemeralIv);
                                callback.decryptionComplete(decrypted);
                                ephemeralSecretKey.destroy();
                            }
                        });

                break;
            case SYMMETRIC:
                decrypt(
                        keyAlias,
                        cipherText, iv, callback);
                break;
            case ASYMMETRIC:
                decryptAsymmetric(
                        keyAlias,
                        cipherText, callback);
                break;
            case NOT_ENCRYPTED:
                throw new SecurityException("File not encrypted.");
        }
    }

}
