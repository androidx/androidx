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

package androidx.security.crypto;

import android.security.keystore.KeyInfo;

import androidx.annotation.NonNull;
import androidx.security.SecureConfig;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;

/**
 * A class that provides simpler access to the AndroidKeyStore including commonly used utilities.
 */
public class SecureKeyStore {

    private static final String TAG = "SecureKeyStore";

    private SecureConfig mSecureConfig;

    @NonNull
    public static SecureKeyStore getDefault() {
        return new SecureKeyStore(SecureConfig.getDefault());
    }

    /**
     * Gets an instance of SecureKeyStore based on the provided SecureConfig.
     *
     * @param secureConfig The SecureConfig to use the KeyStore.
     * @return A SecureKeyStore that has been configured.
     */
    @NonNull
    public static SecureKeyStore getInstance(@NonNull SecureConfig secureConfig) {
        return new SecureKeyStore(secureConfig);
    }

    private SecureKeyStore(@NonNull SecureConfig secureConfig) {
        this.mSecureConfig = secureConfig;
    }

    /**
     * Checks to see if the specified key exists in the AndroidKeyStore
     *
     * @param keyAlias The name of the generated SecretKey to save into the AndroidKeyStore.
     * @return true if the key is stored in secure hardware
     */
    public boolean keyExists(@NonNull String keyAlias) {
        boolean exists = false;
        try {
            KeyStore keyStore = KeyStore.getInstance(mSecureConfig.getAndroidKeyStore());
            keyStore.load(null);
            exists = keyStore.containsAlias(keyAlias);
            /*Certificate cert = keyStore.getCertificate(keyAlias);
            if (cert != null) {
                exists = cert.getPublicKey() != null;
            }*/
        } catch (GeneralSecurityException ex) {
            throw new SecurityException(ex);
        } catch (IOException ex) {
            throw new SecurityException(ex);
        }
        return exists;
    }


    /**
     * Delete a key from the specified keystore.
     *
     * @param keyAlias The key to delete from the KeyStore
     */
    public void deleteKey(@NonNull String keyAlias) {
        try {
            KeyStore keyStore = KeyStore.getInstance(mSecureConfig.getAndroidKeyStore());
            keyStore.load(null);
            keyStore.deleteEntry(keyAlias);
        } catch (GeneralSecurityException ex) {
            throw new SecurityException(ex);
        } catch (IOException ex) {
            throw new SecurityException(ex);
        }
    }

    /**
     * Checks to see if the specified key is stored in secure hardware
     *
     * @param keyAlias The name of the generated SecretKey to save into the AndroidKeyStore.
     * @return true if the key is stored in secure hardware
     */
    public boolean checkKeyInsideSecureHardware(@NonNull String keyAlias) {
        boolean inHardware = false;
        try {
            KeyStore keyStore = KeyStore.getInstance(mSecureConfig.getAndroidKeyStore());
            keyStore.load(null);
            SecretKey key = (SecretKey) keyStore.getKey(keyAlias, null);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(key.getAlgorithm(),
                    mSecureConfig.getAndroidKeyStore());
            KeyInfo keyInfo;
            keyInfo = (KeyInfo) factory.getKeySpec(key, KeyInfo.class);
            inHardware = keyInfo.isInsideSecureHardware();
            return inHardware;
        } catch (GeneralSecurityException e) {
            return inHardware;
        } catch (IOException e) {
            return inHardware;
        }
    }

    /**
     * Checks to see if the specified private key is stored in secure hardware
     *
     * @param keyAlias The name of the generated SecretKey to save into the AndroidKeyStore.
     * @return true if the key is stored in secure hardware
     */
    public boolean checkKeyInsideSecureHardwareAsymmetric(@NonNull String keyAlias) {
        boolean inHardware = false;
        try {
            KeyStore keyStore = KeyStore.getInstance(mSecureConfig.getAndroidKeyStore());
            keyStore.load(null);
            PrivateKey privateKey = (PrivateKey) keyStore.getKey(keyAlias, null);

            KeyFactory factory = KeyFactory.getInstance(privateKey.getAlgorithm(),
                    mSecureConfig.getAndroidKeyStore());
            KeyInfo keyInfo;

            keyInfo = factory.getKeySpec(privateKey, KeyInfo.class);
            inHardware = keyInfo.isInsideSecureHardware();
            return inHardware;

        } catch (GeneralSecurityException e) {
            return inHardware;
        } catch (IOException e) {
            return inHardware;
        }
    }

}
