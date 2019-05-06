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

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Arrays;

import javax.crypto.KeyGenerator;

/**
 * Convenient methods to create and obtain master keys in Android Keystore.
 *
 * <p>The master keys are used to encrypt data encryption keys for encrypting files and preferences.
 *
 */
public final class MasterKeys {

    private static final int KEY_SIZE = 256;

    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    static final String KEYSTORE_PATH_URI = "android-keystore://";
    static final String MASTER_KEY_ALIAS = "_androidx_security_master_key_";

    @NonNull
    public static final KeyGenParameterSpec AES256_GCM_SPEC =
            createAES256GCMKeyGenParameterSpec(MASTER_KEY_ALIAS);

    /**
     * Provides a safe and easy to use KenGenParameterSpec with the settings.
     * Algorithm: AES
     * Block Mode: GCM
     * Padding: No Padding
     * Key Size: 256
     *
     * @param keyAlias The alias for the master key
     * @return The spec for the master key with the specified keyAlias
     */
    @NonNull
    private static KeyGenParameterSpec createAES256GCMKeyGenParameterSpec(
            @NonNull String keyAlias) {
        KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE);
        return builder.build();
    }

    /**
     * Provides a safe and easy to use KenGenParameterSpec with the settings with a default
     * key alias.
     *
     * Algorithm: AES
     * Block Mode: GCM
     * Padding: No Padding
     * Key Size: 256
     *
     * @return The spec for the master key with the default key alias
     */
    @NonNull
    private static KeyGenParameterSpec createAES256GCMKeyGenParameterSpec() {
        return createAES256GCMKeyGenParameterSpec(MASTER_KEY_ALIAS);
    }

    /**
     * Creates or gets the master key provided
     *
     * The encryption scheme is required fields to ensure that the type of
     * encryption used is clear to developers.
     *
     * @param keyGenParameterSpec The key encryption scheme
     * @return The key alias for the master key
     */
    @NonNull
    public static String getOrCreate(
            @NonNull KeyGenParameterSpec keyGenParameterSpec)
            throws GeneralSecurityException, IOException {
        validate(keyGenParameterSpec);
        if (!MasterKeys.keyExists(keyGenParameterSpec.getKeystoreAlias())) {
            generateKey(keyGenParameterSpec);
        }
        return keyGenParameterSpec.getKeystoreAlias();
    }

    private static void validate(KeyGenParameterSpec spec) {
        if (spec.getKeySize() != KEY_SIZE) {
            throw new IllegalArgumentException(
                    "invalid key size, want " + KEY_SIZE + " bits got " + spec.getKeySize()
                            + " bits");
        }
        if (spec.getBlockModes().equals(new String[] { KeyProperties.BLOCK_MODE_GCM })) {
            throw new IllegalArgumentException(
                    "invalid block mode, want " + KeyProperties.BLOCK_MODE_GCM + " got "
                            + Arrays.toString(spec.getBlockModes()));
        }
        if (spec.getPurposes() != (KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)) {
            throw new IllegalArgumentException(
                    "invalid purposes mode, want PURPOSE_ENCRYPT | PURPOSE_DECRYPT got "
                            + spec.getPurposes());
        }
        if (spec.getEncryptionPaddings().equals(new String[]
                { KeyProperties.ENCRYPTION_PADDING_NONE })) {
            throw new IllegalArgumentException(
                    "invalid padding mode, want " + KeyProperties.ENCRYPTION_PADDING_NONE + " got "
                            + Arrays.toString(spec.getEncryptionPaddings()));
        }
    }

    private static void generateKey(@NonNull KeyGenParameterSpec keyGenParameterSpec)
            throws GeneralSecurityException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE);
        keyGenerator.init(keyGenParameterSpec);
        keyGenerator.generateKey();
    }

    private static boolean keyExists(@NonNull String keyAlias)
            throws GeneralSecurityException, IOException {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
        keyStore.load(null);
        return keyStore.containsAlias(keyAlias);
    }

}
