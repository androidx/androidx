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

import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.ProviderException;
import java.util.Arrays;

import javax.crypto.KeyGenerator;

/**
 * Convenient methods to create and obtain master keys in Android Keystore.
 *
 * <p>The master keys are used to encrypt data encryption keys for encrypting files and preferences.
 *
 * @deprecated Use {@link MasterKey.Builder} to work with master keys.
 */
@Deprecated
public final class MasterKeys {
    private MasterKeys() {
    }

    static final String MASTER_KEY_ALIAS = MasterKey.DEFAULT_MASTER_KEY_ALIAS;
    static final int KEY_SIZE = MasterKey.DEFAULT_AES_GCM_MASTER_KEY_SIZE;

    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";

    @NonNull
    @RequiresApi(Build.VERSION_CODES.M)
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
    @RequiresApi(Build.VERSION_CODES.M)
    @SuppressWarnings("SameParameterValue")
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
     * Creates or gets the master key provided
     *
     * The encryption scheme is required fields to ensure that the type of
     * encryption used is clear to developers.
     *
     * @param keyGenParameterSpec The key encryption scheme
     * @return The key alias for the master key
     */
    @NonNull
    @RequiresApi(Build.VERSION_CODES.M)
    public static String getOrCreate(
            @NonNull KeyGenParameterSpec keyGenParameterSpec)
            throws GeneralSecurityException, IOException {
        validate(keyGenParameterSpec);
        if (!MasterKeys.keyExists(keyGenParameterSpec.getKeystoreAlias())) {
            generateKey(keyGenParameterSpec);
        }
        return keyGenParameterSpec.getKeystoreAlias();
    }

    @VisibleForTesting
    @RequiresApi(Build.VERSION_CODES.M)
    static void validate(KeyGenParameterSpec spec) {
        if (spec.getKeySize() != KEY_SIZE) {
            throw new IllegalArgumentException(
                    "invalid key size, want " + KEY_SIZE + " bits got " + spec.getKeySize()
                            + " bits");
        }
        if (!Arrays.equals(spec.getBlockModes(), new String[]{KeyProperties.BLOCK_MODE_GCM})) {
            throw new IllegalArgumentException(
                    "invalid block mode, want " + KeyProperties.BLOCK_MODE_GCM + " got "
                            + Arrays.toString(spec.getBlockModes()));
        }
        if (spec.getPurposes() != (KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)) {
            throw new IllegalArgumentException(
                    "invalid purposes mode, want PURPOSE_ENCRYPT | PURPOSE_DECRYPT got "
                            + spec.getPurposes());
        }
        if (!Arrays.equals(spec.getEncryptionPaddings(), new String[]
                {KeyProperties.ENCRYPTION_PADDING_NONE})) {
            throw new IllegalArgumentException(
                    "invalid padding mode, want " + KeyProperties.ENCRYPTION_PADDING_NONE + " got "
                            + Arrays.toString(spec.getEncryptionPaddings()));
        }
        if (spec.isUserAuthenticationRequired()
                && spec.getUserAuthenticationValidityDurationSeconds() < 1) {
            throw new IllegalArgumentException(
                    "per-operation authentication is not supported "
                            + "(UserAuthenticationValidityDurationSeconds must be >0)");
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private static void generateKey(@NonNull KeyGenParameterSpec keyGenParameterSpec)
            throws GeneralSecurityException {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    ANDROID_KEYSTORE);
            keyGenerator.init(keyGenParameterSpec);
            keyGenerator.generateKey();
        } catch (ProviderException providerException) {
            // Android 10 (API 29) throws a ProviderException under certain circumstances. Wrap
            // that as a GeneralSecurityException so it's more consistent across API levels.
            throw new GeneralSecurityException(providerException.getMessage(), providerException);
        }
    }

    private static boolean keyExists(@NonNull String keyAlias)
            throws GeneralSecurityException, IOException {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
        keyStore.load(null);
        return keyStore.containsAlias(keyAlias);
    }

}
