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

import static androidx.security.crypto.MasterKey.KEYSTORE_PATH_URI;

import static java.nio.charset.StandardCharsets.UTF_8;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArraySet;

import com.google.crypto.tink.Aead;
import com.google.crypto.tink.DeterministicAead;
import com.google.crypto.tink.KeyTemplate;
import com.google.crypto.tink.KeyTemplates;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.aead.AeadConfig;
import com.google.crypto.tink.daead.DeterministicAeadConfig;
import com.google.crypto.tink.integration.android.AndroidKeysetManager;
import com.google.crypto.tink.subtle.Base64;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An implementation of {@link SharedPreferences} that encrypts keys and values.
 * <br />
 * <br />
 * <b>WARNING</b>: The preference file should not be backed up with Auto Backup. When restoring the
 * file it is likely the key used to encrypt it will no longer be present. You should exclude all
 * <code>EncryptedSharedPreference</code>s from backup using
 * <a href="https://developer.android.com/guide/topics/data/autobackup#IncludingFiles">backup rules</a>.
 * <br />
 * <br />
 * Basic use of the class:
 *
 * <pre>
 *  MasterKey masterKey = new MasterKey.Builder(context)
 *      .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
 *      .build();
 *
 *  SharedPreferences sharedPreferences = EncryptedSharedPreferences.create(
 *      context,
 *      "secret_shared_prefs",
 *      masterKey,
 *      EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
 *      EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
 *  );
 *
 *  // use the shared preferences and editor as you normally would
 *  SharedPreferences.Editor editor = sharedPreferences.edit();
 * </pre>
 */
public final class EncryptedSharedPreferences implements SharedPreferences {

    private static final String KEY_KEYSET_ALIAS =
            "__androidx_security_crypto_encrypted_prefs_key_keyset__";
    private static final String VALUE_KEYSET_ALIAS =
            "__androidx_security_crypto_encrypted_prefs_value_keyset__";

    private static final String NULL_VALUE = "__NULL__";

    final SharedPreferences mSharedPreferences;
    final CopyOnWriteArrayList<OnSharedPreferenceChangeListener> mListeners;
    final String mFileName;
    final String mMasterKeyAlias;

    final Aead mValueAead;
    final DeterministicAead mKeyDeterministicAead;

    EncryptedSharedPreferences(@NonNull String name,
            @NonNull String masterKeyAlias,
            @NonNull SharedPreferences sharedPreferences,
            @NonNull Aead aead,
            @NonNull DeterministicAead deterministicAead) {
        mFileName = name;
        mSharedPreferences = sharedPreferences;
        mMasterKeyAlias = masterKeyAlias;
        mValueAead = aead;
        mKeyDeterministicAead = deterministicAead;
        mListeners = new CopyOnWriteArrayList<>();
    }

    /**
     * Opens an instance of encrypted SharedPreferences
     *
     * @param fileName                  The name of the file to open; can not contain path
     *                                  separators.
     * @param masterKey                 The master key to use.
     * @param prefKeyEncryptionScheme   The scheme to use for encrypting keys.
     * @param prefValueEncryptionScheme The scheme to use for encrypting values.
     * @return The SharedPreferences instance that encrypts all data.
     * @throws GeneralSecurityException when a bad master key or keyset has been attempted
     * @throws IOException              when fileName can not be used
     */
    @NonNull
    public static SharedPreferences create(@NonNull Context context,
            @NonNull String fileName,
            @NonNull MasterKey masterKey,
            @NonNull PrefKeyEncryptionScheme prefKeyEncryptionScheme,
            @NonNull PrefValueEncryptionScheme prefValueEncryptionScheme)
            throws GeneralSecurityException, IOException {
        return create(fileName, masterKey.getKeyAlias(), context,
                prefKeyEncryptionScheme, prefValueEncryptionScheme);
    }

    /**
     * Opens an instance of encrypted SharedPreferences
     *
     * <p>If the <code>masterKeyAlias</code> used here is for a key that is not yet created, this
     * method will not be thread safe. Use the alternate signature that is not deprecated for
     * multi-threaded contexts.
     *
     * @deprecated Use {@link #create(Context, String, MasterKey,
     * PrefKeyEncryptionScheme, PrefValueEncryptionScheme)} instead.
     * @param fileName                  The name of the file to open; can not contain path
     *                                  separators.
     * @param masterKeyAlias            The alias of the master key to use.
     * @param context                   The context to use to open the preferences file.
     * @param prefKeyEncryptionScheme   The scheme to use for encrypting keys.
     * @param prefValueEncryptionScheme The scheme to use for encrypting values.
     * @return The SharedPreferences instance that encrypts all data.
     * @throws GeneralSecurityException when a bad master key or keyset has been attempted
     * @throws IOException              when fileName can not be used
     */
    @Deprecated
    @NonNull
    public static SharedPreferences create(@NonNull String fileName,
            @NonNull String masterKeyAlias,
            @NonNull Context context,
            @NonNull PrefKeyEncryptionScheme prefKeyEncryptionScheme,
            @NonNull PrefValueEncryptionScheme prefValueEncryptionScheme)
            throws GeneralSecurityException, IOException {
        DeterministicAeadConfig.register();
        AeadConfig.register();

        final Context applicationContext = context.getApplicationContext();
        KeysetHandle daeadKeysetHandle = new AndroidKeysetManager.Builder()
                .withKeyTemplate(prefKeyEncryptionScheme.getKeyTemplate())
                .withSharedPref(applicationContext, KEY_KEYSET_ALIAS, fileName)
                .withMasterKeyUri(KEYSTORE_PATH_URI + masterKeyAlias)
                .build().getKeysetHandle();
        KeysetHandle aeadKeysetHandle = new AndroidKeysetManager.Builder()
                .withKeyTemplate(prefValueEncryptionScheme.getKeyTemplate())
                .withSharedPref(applicationContext, VALUE_KEYSET_ALIAS, fileName)
                .withMasterKeyUri(KEYSTORE_PATH_URI + masterKeyAlias)
                .build().getKeysetHandle();

        DeterministicAead daead = daeadKeysetHandle.getPrimitive(DeterministicAead.class);
        Aead aead = aeadKeysetHandle.getPrimitive(Aead.class);

        return new EncryptedSharedPreferences(fileName, masterKeyAlias,
                applicationContext.getSharedPreferences(fileName, Context.MODE_PRIVATE), aead,
                daead);
    }

    /**
     * The encryption scheme to encrypt keys.
     */
    public enum PrefKeyEncryptionScheme {
        /**
         * Pref keys are encrypted deterministically with AES256-SIV-CMAC (RFC 5297).
         *
         * <p>For more information please see the Tink documentation:
         *
         * <p><a href="https://google.github.io/tink/javadoc/tink/1.7.0/com/google/crypto/tink/daead/AesSivKeyManager.html">AesSivKeyManager</a>.aes256SivTemplate()
         */
        AES256_SIV("AES256_SIV");

        private final String mDeterministicAeadKeyTemplateName;

        PrefKeyEncryptionScheme(String deterministicAeadKeyTemplateName) {
            mDeterministicAeadKeyTemplateName = deterministicAeadKeyTemplateName;
        }

        KeyTemplate getKeyTemplate() throws GeneralSecurityException {
            return KeyTemplates.get(mDeterministicAeadKeyTemplateName);
        }
    }

    /**
     * The encryption scheme to encrypt values.
     */
    public enum PrefValueEncryptionScheme {
        /**
         * Pref values are encrypted with AES256-GCM. The associated data is the encrypted pref key.
         *
         * <p>For more information please see the Tink documentation:
         *
         * <p><a href="https://google.github.io/tink/javadoc/tink/1.7.0/com/google/crypto/tink/aead/AesGcmKeyManager.html">AesGcmKeyManager</a>.aes256GcmTemplate()
         */
        AES256_GCM("AES256_GCM");

        private final String mAeadKeyTemplateName;

        PrefValueEncryptionScheme(String aeadKeyTemplateName) {
            mAeadKeyTemplateName = aeadKeyTemplateName;
        }

        KeyTemplate getKeyTemplate() throws GeneralSecurityException {
            return KeyTemplates.get(mAeadKeyTemplateName);
        }
    }

    private static final class Editor implements SharedPreferences.Editor {
        private final EncryptedSharedPreferences mEncryptedSharedPreferences;
        private final SharedPreferences.Editor mEditor;
        private final List<String> mKeysChanged;
        private final AtomicBoolean mClearRequested = new AtomicBoolean(false);

        Editor(EncryptedSharedPreferences encryptedSharedPreferences,
                SharedPreferences.Editor editor) {
            mEncryptedSharedPreferences = encryptedSharedPreferences;
            mEditor = editor;
            mKeysChanged = new CopyOnWriteArrayList<>();
        }

        @Override
        @NonNull
        public SharedPreferences.Editor putString(@Nullable String key, @Nullable String value) {
            if (value == null) {
                value = NULL_VALUE;
            }
            byte[] stringBytes = value.getBytes(UTF_8);
            int stringByteLength = stringBytes.length;
            ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES + Integer.BYTES
                    + stringByteLength);
            buffer.putInt(EncryptedType.STRING.getId());
            buffer.putInt(stringByteLength);
            buffer.put(stringBytes);
            putEncryptedObject(key, buffer.array());
            return this;
        }

        @Override
        @NonNull
        public SharedPreferences.Editor putStringSet(@Nullable String key,
                @Nullable Set<String> values) {
            if (values == null) {
                values = new ArraySet<>();
                values.add(NULL_VALUE);
            }
            List<byte[]> byteValues = new ArrayList<>(values.size());
            int totalBytes = values.size() * Integer.BYTES;
            for (String strValue : values) {
                byte[] byteValue = strValue.getBytes(UTF_8);
                byteValues.add(byteValue);
                totalBytes += byteValue.length;
            }
            totalBytes += Integer.BYTES;
            ByteBuffer buffer = ByteBuffer.allocate(totalBytes);
            buffer.putInt(EncryptedType.STRING_SET.getId());
            for (byte[] bytes : byteValues) {
                buffer.putInt(bytes.length);
                buffer.put(bytes);
            }
            putEncryptedObject(key, buffer.array());
            return this;
        }

        @Override
        @NonNull
        public SharedPreferences.Editor putInt(@Nullable String key, int value) {
            ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES + Integer.BYTES);
            buffer.putInt(EncryptedType.INT.getId());
            buffer.putInt(value);
            putEncryptedObject(key, buffer.array());
            return this;
        }

        @Override
        @NonNull
        public SharedPreferences.Editor putLong(@Nullable String key, long value) {
            ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES + Long.BYTES);
            buffer.putInt(EncryptedType.LONG.getId());
            buffer.putLong(value);
            putEncryptedObject(key, buffer.array());
            return this;
        }

        @Override
        @NonNull
        public SharedPreferences.Editor putFloat(@Nullable String key, float value) {
            ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES + Float.BYTES);
            buffer.putInt(EncryptedType.FLOAT.getId());
            buffer.putFloat(value);
            putEncryptedObject(key, buffer.array());
            return this;
        }

        @Override
        @NonNull
        public SharedPreferences.Editor putBoolean(@Nullable String key, boolean value) {
            ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES + Byte.BYTES);
            buffer.putInt(EncryptedType.BOOLEAN.getId());
            buffer.put(value ? (byte) 1 : (byte) 0);
            putEncryptedObject(key, buffer.array());
            return this;
        }

        @Override
        @NonNull
        public SharedPreferences.Editor remove(@Nullable String key) {
            if (mEncryptedSharedPreferences.isReservedKey(key)) {
                throw new SecurityException(key + " is a reserved key for the encryption keyset.");
            }
            mEditor.remove(mEncryptedSharedPreferences.encryptKey(key));
            mKeysChanged.add(key);
            return this;
        }

        @Override
        @NonNull
        public SharedPreferences.Editor clear() {
            // Set the flag to clear on commit, this operation happens first on commit.
            // Cannot use underlying clear operation, it will remove the keysets and
            // break the editor.
            mClearRequested.set(true);
            return this;
        }

        @Override
        public boolean commit() {
            clearKeysIfNeeded();
            try {
                return mEditor.commit();
            } finally {
                notifyListeners();
                mKeysChanged.clear();
            }
        }

        @Override
        public void apply() {
            clearKeysIfNeeded();
            mEditor.apply();
            notifyListeners();
            mKeysChanged.clear();
        }

        private void clearKeysIfNeeded() {
            // Call "clear" first as per the documentation, remove all keys that haven't
            // been modified in this editor.
            if (mClearRequested.getAndSet(false)) {
                for (String key : mEncryptedSharedPreferences.getAll().keySet()) {
                    if (!mKeysChanged.contains(key)
                            && !mEncryptedSharedPreferences.isReservedKey(key)) {
                        mEditor.remove(mEncryptedSharedPreferences.encryptKey(key));
                    }
                }
            }
        }

        private void putEncryptedObject(String key, byte[] value) {
            if (mEncryptedSharedPreferences.isReservedKey(key)) {
                throw new SecurityException(key + " is a reserved key for the encryption keyset.");
            }
            mKeysChanged.add(key);
            if (key == null) {
                key = NULL_VALUE;
            }
            try {
                Pair<String, String> encryptedPair = mEncryptedSharedPreferences
                        .encryptKeyValuePair(key, value);
                mEditor.putString(encryptedPair.first, encryptedPair.second);
            } catch (GeneralSecurityException ex) {
                throw new SecurityException("Could not encrypt data: " + ex.getMessage(), ex);
            }
        }

        private void notifyListeners() {
            for (OnSharedPreferenceChangeListener listener :
                    mEncryptedSharedPreferences.mListeners) {
                for (String key : mKeysChanged) {
                    listener.onSharedPreferenceChanged(mEncryptedSharedPreferences, key);
                }
            }
        }
    }

    // SharedPreferences methods

    @Override
    @NonNull
    public Map<String, ?> getAll() {
        Map<String, ? super Object> allEntries = new HashMap<>();
        for (Map.Entry<String, ?> entry : mSharedPreferences.getAll().entrySet()) {
            if (!isReservedKey(entry.getKey())) {
                String decryptedKey = decryptKey(entry.getKey());
                allEntries.put(decryptedKey,
                        getDecryptedObject(decryptedKey));
            }
        }
        return allEntries;
    }

    @Nullable
    @Override
    public String getString(@Nullable String key, @Nullable String defValue) {
        Object value = getDecryptedObject(key);
        return (value instanceof String ? (String) value : defValue);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public Set<String> getStringSet(@Nullable String key, @Nullable Set<String> defValues) {
        Set<String> returnValues;
        Object value = getDecryptedObject(key);
        if (value instanceof Set) {
            returnValues = (Set<String>) value;
        } else {
            returnValues = new ArraySet<>();
        }
        return returnValues.size() > 0 ? returnValues : defValues;
    }

    @Override
    public int getInt(@Nullable String key, int defValue) {
        Object value = getDecryptedObject(key);
        return (value instanceof Integer ? (Integer) value : defValue);
    }

    @Override
    public long getLong(@Nullable String key, long defValue) {
        Object value = getDecryptedObject(key);
        return (value instanceof Long ? (Long) value : defValue);
    }

    @Override
    public float getFloat(@Nullable String key, float defValue) {
        Object value = getDecryptedObject(key);
        return (value instanceof Float ? (Float) value : defValue);
    }

    @Override
    public boolean getBoolean(@Nullable String key, boolean defValue) {
        Object value = getDecryptedObject(key);
        return (value instanceof Boolean ? (Boolean) value : defValue);
    }

    @Override
    public boolean contains(@Nullable String key) {
        if (isReservedKey(key)) {
            throw new SecurityException(key + " is a reserved key for the encryption keyset.");
        }
        String encryptedKey = encryptKey(key);
        return mSharedPreferences.contains(encryptedKey);
    }

    @Override
    @NonNull
    public SharedPreferences.Editor edit() {
        return new Editor(this, mSharedPreferences.edit());
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(
            @NonNull OnSharedPreferenceChangeListener listener) {
        mListeners.add(listener);
    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(
            @NonNull OnSharedPreferenceChangeListener listener) {
        mListeners.remove(listener);
    }

    /**
     * Internal enum to set the type of encrypted data.
     */
    private enum EncryptedType {
        STRING(0),
        STRING_SET(1),
        INT(2),
        LONG(3),
        FLOAT(4),
        BOOLEAN(5);

        private final int mId;

        EncryptedType(int id) {
            mId = id;
        }

        public int getId() {
            return mId;
        }

        @Nullable
        public static EncryptedType fromId(int id) {
            switch (id) {
                case 0:
                    return STRING;
                case 1:
                    return STRING_SET;
                case 2:
                    return INT;
                case 3:
                    return LONG;
                case 4:
                    return FLOAT;
                case 5:
                    return BOOLEAN;
            }
            return null;
        }
    }

    private Object getDecryptedObject(String key) throws SecurityException {
        if (isReservedKey(key)) {
            throw new SecurityException(key + " is a reserved key for the encryption keyset.");
        }
        if (key == null) {
            key = NULL_VALUE;
        }

        try {
            String encryptedKey = encryptKey(key);
            String encryptedValue = mSharedPreferences.getString(encryptedKey, null);
            if (encryptedValue == null) {
                return null;
            }

            byte[] cipherText = Base64.decode(encryptedValue, Base64.DEFAULT);
            byte[] value = mValueAead.decrypt(cipherText, encryptedKey.getBytes(UTF_8));
            ByteBuffer buffer = ByteBuffer.wrap(value);
            buffer.position(0);
            int typeId = buffer.getInt();
            EncryptedType type = EncryptedType.fromId(typeId);
            if (type == null) {
                throw new SecurityException("Unknown type ID for encrypted pref value: " + typeId);
            }

            switch (type) {
                case STRING:
                    int stringLength = buffer.getInt();
                    ByteBuffer stringSlice = buffer.slice();
                    buffer.limit(stringLength);

                    String stringValue = UTF_8.decode(stringSlice).toString();
                    if (stringValue.equals(NULL_VALUE)) {
                        return null;
                    }

                    return stringValue;
                case INT:
                    return buffer.getInt();
                case LONG:
                    return buffer.getLong();
                case FLOAT:
                    return buffer.getFloat();
                case BOOLEAN:
                    return buffer.get() != (byte) 0;
                case STRING_SET:
                    ArraySet<String> stringSet = new ArraySet<>();

                    while (buffer.hasRemaining()) {
                        int subStringLength = buffer.getInt();
                        ByteBuffer subStringSlice = buffer.slice();
                        subStringSlice.limit(subStringLength);
                        buffer.position(buffer.position() + subStringLength);
                        stringSet.add(UTF_8.decode(subStringSlice).toString());
                    }

                    if (stringSet.size() == 1 && NULL_VALUE.equals(stringSet.valueAt(0))) {
                        return null;
                    }

                    return stringSet;
                default:
                    throw new SecurityException("Unhandled type for encrypted pref value: " + type);
            }
        } catch (GeneralSecurityException ex) {
            throw new SecurityException("Could not decrypt value. " + ex.getMessage(), ex);
        }
    }

    String encryptKey(String key) {
        if (key == null) {
            key = NULL_VALUE;
        }
        try {
            byte[] encryptedKeyBytes = mKeyDeterministicAead.encryptDeterministically(
                    key.getBytes(UTF_8),
                    mFileName.getBytes());
            return Base64.encode(encryptedKeyBytes);
        } catch (GeneralSecurityException ex) {
            throw new SecurityException("Could not encrypt key. " + ex.getMessage(), ex);
        }
    }

    String decryptKey(String encryptedKey) {
        try {
            byte[] clearText = mKeyDeterministicAead.decryptDeterministically(
                    Base64.decode(encryptedKey, Base64.DEFAULT),
                    mFileName.getBytes());
            String key = new String(clearText, UTF_8);
            if (key.equals(NULL_VALUE)) {
                key = null;
            }
            return key;
        } catch (GeneralSecurityException ex) {
            throw new SecurityException("Could not decrypt key. " + ex.getMessage(), ex);
        }
    }


    /**
     * Check usage of the key and value keysets.
     *
     * @param key the plain text key
     */
    boolean isReservedKey(String key) {
        return KEY_KEYSET_ALIAS.equals(key) || VALUE_KEYSET_ALIAS.equals(key);
    }

    Pair<String, String> encryptKeyValuePair(String key, byte[] value)
            throws GeneralSecurityException {
        String encryptedKey = encryptKey(key);
        byte[] cipherText = mValueAead.encrypt(value, encryptedKey.getBytes(UTF_8));
        return new Pair<>(encryptedKey, Base64.encode(cipherText));
    }

}
