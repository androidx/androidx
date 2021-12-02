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

package androidx.security.identity;

import android.content.Context;
import android.icu.util.Calendar;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.AtomicFile;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

import co.nstant.in.cbor.CborBuilder;
import co.nstant.in.cbor.CborDecoder;
import co.nstant.in.cbor.CborEncoder;
import co.nstant.in.cbor.CborException;
import co.nstant.in.cbor.builder.ArrayBuilder;
import co.nstant.in.cbor.builder.MapBuilder;
import co.nstant.in.cbor.model.Array;
import co.nstant.in.cbor.model.ByteString;
import co.nstant.in.cbor.model.DataItem;
import co.nstant.in.cbor.model.Number;
import co.nstant.in.cbor.model.UnicodeString;
import co.nstant.in.cbor.model.UnsignedInteger;

/**
 * Internal routines used to manage credential data.
 */
class CredentialData {
    private static final String TAG = "CredentialData";

    private Context mContext;
    private String mCredentialName;

    private String mDocType = "";
    private String mCredentialKeyAlias = "";
    private Collection<X509Certificate> mCertificateChain = null;
    private byte[] mProofOfProvisioningSha256 = null;
    private AbstractList<AccessControlProfile> mAccessControlProfiles = new ArrayList<>();
    private AbstractMap<Integer, AccessControlProfile> mProfileIdToAcpMap = new HashMap<>();
    private AbstractList<PersonalizationData.NamespaceData> mNamespaceDatas = new ArrayList<>();

    private int mAuthKeyCount = 0;
    private int mAuthMaxUsesPerKey = 1;

    // The alias for the key that must be unlocked by user auth for every reader session.
    //
    // Is non-empty if and only if there is at least one ACP requiring user-auth.
    private String mPerReaderSessionKeyAlias = "";

    // A map from ACP id to key alias.
    //
    // This is for ACPs with positive timeouts.
    private AbstractMap<Integer, String> mAcpTimeoutKeyAliases;

    // The data for each authentication key, this is always mAuthKeyCount items.
    private AbstractList<AuthKeyData> mAuthKeyDatas = new ArrayList<>();

    private CredentialData(Context context, String credentialName) {
        mContext = context;
        mCredentialName = credentialName;
    }

    /**
     * Deletes KeyStore keys no longer needed when updating a credential (every KeyStore key
     * except for CredentialKey).
     */
    void deleteKeysForReplacement() {
        KeyStore ks;
        try {
            ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);
        } catch (CertificateException
                | IOException
                | NoSuchAlgorithmException
                | KeyStoreException e) {
            throw new RuntimeException("Error loading keystore", e);
        }

        // Nuke all keys except for CredentialKey.
        try {
            if (!mPerReaderSessionKeyAlias.isEmpty()) {
                ks.deleteEntry(mPerReaderSessionKeyAlias);
            }
            for (String alias : mAcpTimeoutKeyAliases.values()) {
                ks.deleteEntry(alias);
            }
            for (AuthKeyData authKeyData : mAuthKeyDatas) {
                if (!authKeyData.mAlias.isEmpty()) {
                    ks.deleteEntry(authKeyData.mAlias);
                }
                if (!authKeyData.mPendingAlias.isEmpty()) {
                    ks.deleteEntry(authKeyData.mPendingAlias);
                }
            }
        } catch (KeyStoreException e) {
            throw new RuntimeException("Error deleting key", e);
        }
    }

    /**
     * Creates a new {@link CredentialData} with the given name and saves it to disk.
     *
     * <p>The created data will be configured with zero authentication keys and max one use per
     * key and can later be loaded via the {@link #loadCredentialData(Context, String)}
     * method and deleted by calling {@link #delete(Context, String, byte[])}.
     *
     * <p>An auth-bound key will be created for each access control profile with
     * user-authentication.
     *
     * @param context             the context.
     * @param credentialName      the name of the credential.
     * @param credentialKeyAlias  the alias of the credential key which must have already been
     *                            created.
     * @param certificateChain    the certificate chain for the credential key.
     * @param personalizationData the data for the credential.
     * @param isReplacement       set to true if this replaces an existing credential
     * @return a new @{link CredentialData} object
     */
    static CredentialData createCredentialData(Context context,
            String docType,
            String credentialName,
            String credentialKeyAlias,
            Collection<X509Certificate> certificateChain,
            PersonalizationData personalizationData,
            byte[] proofOfProvisioningSha256,
            boolean isReplacement) {
        if (!isReplacement) {
            if (credentialAlreadyExists(context, credentialName)) {
                throw new RuntimeException("Credential with given name already exists");
            }
        }

        CredentialData data = new CredentialData(context, credentialName);
        data.mDocType = docType;
        data.mCredentialKeyAlias = credentialKeyAlias;
        data.mCertificateChain = certificateChain;
        data.mProofOfProvisioningSha256 = proofOfProvisioningSha256;
        data.mAccessControlProfiles = new ArrayList<>();
        data.mProfileIdToAcpMap = new HashMap<>();
        for (AccessControlProfile item : personalizationData.getAccessControlProfiles()) {
            data.mAccessControlProfiles.add(item);
            data.mProfileIdToAcpMap.put(item.getAccessControlProfileId().getId(), item);
        }
        data.mNamespaceDatas = new ArrayList<>();
        data.mNamespaceDatas.addAll(personalizationData.getNamespaceDatas());

        data.mAcpTimeoutKeyAliases = new HashMap<>();
        for (AccessControlProfile profile : personalizationData.getAccessControlProfiles()) {
            boolean isAuthRequired = profile.isUserAuthenticationRequired();
            long timeoutSeconds = profile.getUserAuthenticationTimeout();
            if (isAuthRequired) {
                // Always make sure the per-reader-session key exists since this is what we're
                // going to be handing out a Cipher for when returning a CryptoObject at
                // presentation time.
                ensurePerReaderSessionKey(credentialName, data);

                ensureAcpTimoutKeyForProfile(credentialName, data, profile, timeoutSeconds);
            }
        }

        data.createDataEncryptionKey();

        data.saveToDisk();
        return data;
    }

    static boolean credentialAlreadyExists(Context context, String credentialName) {
        String filename = getFilenameForCredentialData(credentialName);
        AtomicFile file = new AtomicFile(context.getFileStreamPath(filename));
        try {
            file.openRead();
        } catch (FileNotFoundException e) {
            return false;
        }
        return true;
    }

    @SuppressWarnings("deprecation")  // setUserAuthenticationValidityDurationSeconds()
    private static void ensurePerReaderSessionKey(String credentialName,
            CredentialData data) {
        if (!data.mPerReaderSessionKeyAlias.isEmpty()) {
            return;
        }
        data.mPerReaderSessionKeyAlias = getAcpKeyAliasFromCredentialName(credentialName);
        try {
            KeyGenerator kg = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");

            KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(
                    data.mPerReaderSessionKeyAlias,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(128)
                    // Can't use setUserAuthenticationParameters() since we need to run
                    // on API level 24.
                    .setUserAuthenticationRequired(true)
                    .setUserAuthenticationValidityDurationSeconds(-1); // Auth for every use
            kg.init(builder.build());
            kg.generateKey();
        } catch (InvalidAlgorithmParameterException
                | NoSuchAlgorithmException
                | NoSuchProviderException e) {
            throw new RuntimeException("Error creating ACP auth-bound key", e);
        }
    }

    @SuppressWarnings("deprecation")  // setUserAuthenticationValidityDurationSeconds()
    private static void ensureAcpTimoutKeyForProfile(String credentialName, CredentialData data,
            AccessControlProfile profile, long timeoutMilliSeconds) {
        if (timeoutMilliSeconds > 0) {
            int profileId = profile.getAccessControlProfileId().getId();
            String acpAlias = getAcpTimeoutKeyAliasFromCredentialName(credentialName,
                    profileId);
            try {
                final int timeoutSeconds = (int) (timeoutMilliSeconds / 1000);
                KeyGenerator kg = KeyGenerator.getInstance(
                        KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
                KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(
                        acpAlias,
                        KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setUserAuthenticationRequired(true)
                        // Can't use setUserAuthenticationParameters() since we need to run
                        // on API level 24.
                        .setUserAuthenticationValidityDurationSeconds(timeoutSeconds)
                        .setKeySize(128);
                kg.init(builder.build());
                kg.generateKey();
            } catch (InvalidAlgorithmParameterException
                    | NoSuchAlgorithmException
                    | NoSuchProviderException e) {
                throw new RuntimeException("Error creating ACP auth-bound timeout key", e);
            }
            data.mAcpTimeoutKeyAliases.put(profileId, acpAlias);
        }
    }

    /**
     * Loads a {@link CredentialData} object previously created with
     * {@link #createCredentialData(Context, String, String, String, Collection,
     * PersonalizationData, byte[])}.
     *
     * @param context        the application context
     * @param credentialName the name of the credential.
     * @return a new {@link CredentialData} object or {@code null} if an error occurred.
     */
    static CredentialData loadCredentialData(Context context, String credentialName) {
        CredentialData data = new CredentialData(context, credentialName);
        String dataKeyAlias = getDataKeyAliasFromCredentialName(credentialName);
        if (!data.loadFromDisk(dataKeyAlias)) {
            return null;
        }
        return data;
    }

    static String escapeCredentialName(String componentName, String credentialName) {
        try {
            return "identity_credential_" + componentName + "_"
                    + URLEncoder.encode(credentialName, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Unexpected UnsupportedEncodingException", e);
        }
    }

    static String getFilenameForCredentialData(String credentialName) {
        return escapeCredentialName("data", credentialName);
    }

    static String getAliasFromCredentialName(String credentialName) {
        return escapeCredentialName("credkey", credentialName);
    }

    static String getDataKeyAliasFromCredentialName(String credentialName) {
        return escapeCredentialName("datakey", credentialName);
    }

    static String getAcpTimeoutKeyAliasFromCredentialName(String credentialName, int acpProfileId) {
        return escapeCredentialName("acp_timeout_for_id" + acpProfileId, credentialName);
    }

    static String getAcpKeyAliasFromCredentialName(String credentialName) {
        return escapeCredentialName("acp", credentialName);
    }

    PrivateKey getCredentialKeyPrivate() {
        KeyStore ks;
        KeyStore.Entry entry;
        try {
            ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);
            entry = ks.getEntry(mCredentialKeyAlias, null);
        } catch (CertificateException
                | IOException
                | NoSuchAlgorithmException
                | KeyStoreException
                | UnrecoverableEntryException e) {
            throw new RuntimeException("Error loading keystore", e);
        }
        return ((KeyStore.PrivateKeyEntry) entry).getPrivateKey();
    }

    // Returns COSE_Sign1 with payload set to ProofOfOwnership
    @NonNull byte[] proveOwnership(@NonNull byte[] challenge) {
        PrivateKey key = getCredentialKeyPrivate();

        CborBuilder signedDataBuilder = new CborBuilder();
        signedDataBuilder.addArray()
                .add("ProofOfOwnership")
                .add(mDocType)
                .add(challenge)
                .add(false);
        byte[] signatureBytes;
        try {
            ByteArrayOutputStream dtsBaos = new ByteArrayOutputStream();
            CborEncoder dtsEncoder = new CborEncoder(dtsBaos);
            dtsEncoder.encode(signedDataBuilder.build().get(0));
            byte[] dataToSign = dtsBaos.toByteArray();

            signatureBytes = Util.cborEncode(Util.coseSign1Sign(key,
                    dataToSign,
                    null,
                    null));
        } catch (NoSuchAlgorithmException
                | InvalidKeyException
                | CertificateEncodingException
                | CborException e) {
            throw new RuntimeException("Error building ProofOfOwnership", e);
        }
        return signatureBytes;
    }

    // Returns COSE_Sign1 with payload set to ProofOfDeletion
    static byte[] buildProofOfDeletionSignature(String docType, PrivateKey key, byte[] challenge) {

        CborBuilder signedDataBuilder = new CborBuilder();
        ArrayBuilder<CborBuilder> arrayBuilder = signedDataBuilder.addArray();
        arrayBuilder.add("ProofOfDeletion")
                .add(docType);
        if (challenge != null) {
            arrayBuilder.add(challenge);
        }
        arrayBuilder.add(false);

        byte[] signatureBytes;
        try {
            ByteArrayOutputStream dtsBaos = new ByteArrayOutputStream();
            CborEncoder dtsEncoder = new CborEncoder(dtsBaos);
            dtsEncoder.encode(signedDataBuilder.build().get(0));
            byte[] dataToSign = dtsBaos.toByteArray();

            signatureBytes = Util.cborEncode(Util.coseSign1Sign(key,
                    dataToSign,
                    null,
                    null));
        } catch (NoSuchAlgorithmException
                | InvalidKeyException
                | CertificateEncodingException
                | CborException e) {
            throw new RuntimeException("Error building ProofOfDeletion", e);
        }
        return signatureBytes;
    }

    static byte[] delete(Context context, String credentialName, byte[] challenge) {
        String filename = getFilenameForCredentialData(credentialName);
        AtomicFile file = new AtomicFile(context.getFileStreamPath(filename));
        try {
            file.openRead();
        } catch (FileNotFoundException e) {
            return null;
        }

        CredentialData data = new CredentialData(context, credentialName);
        String dataKeyAlias = getDataKeyAliasFromCredentialName(credentialName);
        try {
            data.loadFromDisk(dataKeyAlias);
        } catch (RuntimeException e) {
            Log.e(TAG, "Error parsing file on disk (old version?). Deleting anyway.");
            file.delete();
            return null;
        }

        KeyStore ks;
        KeyStore.Entry entry;
        try {
            ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);
            entry = ks.getEntry(data.mCredentialKeyAlias, null);
        } catch (CertificateException
                | IOException
                | NoSuchAlgorithmException
                | KeyStoreException
                | UnrecoverableEntryException e) {
            throw new RuntimeException("Error loading keystore", e);
        }

        byte[] signature = buildProofOfDeletionSignature(data.mDocType,
                ((KeyStore.PrivateKeyEntry) entry).getPrivateKey(), challenge);

        file.delete();

        // Nuke all keys.
        try {
            ks.deleteEntry(data.mCredentialKeyAlias);
            if (!data.mPerReaderSessionKeyAlias.isEmpty()) {
                ks.deleteEntry(data.mPerReaderSessionKeyAlias);
            }
            for (String alias : data.mAcpTimeoutKeyAliases.values()) {
                ks.deleteEntry(alias);
            }
            for (AuthKeyData authKeyData : data.mAuthKeyDatas) {
                if (!authKeyData.mAlias.isEmpty()) {
                    ks.deleteEntry(authKeyData.mAlias);
                }
                if (!authKeyData.mPendingAlias.isEmpty()) {
                    ks.deleteEntry(authKeyData.mPendingAlias);
                }
            }
        } catch (KeyStoreException e) {
            throw new RuntimeException("Error deleting key", e);
        }

        return signature;
    }

    private void createDataEncryptionKey() {
        // TODO: it could maybe be nice to encrypt data with the appropriate auth-bound
        //  key (the one associated with the ACP with the longest timeout), if it doesn't
        //  have a no-auth ACP.
        try {
            String dataKeyAlias = getDataKeyAliasFromCredentialName(mCredentialName);
            KeyGenerator kg = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(
                    dataKeyAlias,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(128);
            kg.init(builder.build());
            kg.generateKey();
        } catch (InvalidAlgorithmParameterException
                | NoSuchAlgorithmException
                | NoSuchProviderException e) {
            throw new RuntimeException("Error creating data encryption key", e);
        }
    }

    private void saveToDisk() {
        CborBuilder builder = new CborBuilder();
        MapBuilder<CborBuilder> map = builder.addMap();

        saveToDiskBasic(map);
        saveToDiskAuthDatas(map);
        saveToDiskACPs(map);
        saveToDiskNamespaceDatas(map);
        saveToDiskAuthKeys(map);

        byte[] cleartextDataToSaveBytes = saveToDiskEncode(builder);

        byte[] dataToSaveBytes = saveToDiskEncrypt(cleartextDataToSaveBytes);

        String filename = getFilenameForCredentialData(mCredentialName);
        AtomicFile file = new AtomicFile(mContext.getFileStreamPath(filename));
        FileOutputStream outputStream = null;
        try {
            outputStream = file.startWrite();
            outputStream.write(dataToSaveBytes);
            outputStream.close();
            file.finishWrite(outputStream);
        } catch (IOException e) {
            if (outputStream != null) {
                file.failWrite(outputStream);
            }
            throw new RuntimeException("Error writing data", e);
        }
    }

    private byte[] saveToDiskEncode(CborBuilder map) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        CborEncoder encoder = new CborEncoder(baos);
        try {
            encoder.encode(map.build());
        } catch (CborException e) {
            throw new RuntimeException("Error encoding data", e);
        }
        return baos.toByteArray();
    }

    private byte[] saveToDiskEncrypt(byte[] cleartextDataToSaveBytes) {
        byte[] dataToSaveBytes;
        try {
            KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);
            String dataKeyAlias = getDataKeyAliasFromCredentialName(mCredentialName);

            KeyStore.Entry entry = ks.getEntry(dataKeyAlias, null);
            SecretKey secretKey = ((KeyStore.SecretKeyEntry) entry).getSecretKey();
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);

            byte[] cipherText = cipher.doFinal(
                    cleartextDataToSaveBytes); // This includes the auth tag
            ByteBuffer byteBuffer = ByteBuffer.allocate(12 + cipherText.length);
            byteBuffer.put(cipher.getIV());
            byteBuffer.put(cipherText);
            dataToSaveBytes = byteBuffer.array();
        } catch (NoSuchPaddingException
                | BadPaddingException
                | NoSuchAlgorithmException
                | CertificateException
                | InvalidKeyException
                | UnrecoverableEntryException
                | IOException
                | IllegalBlockSizeException
                | KeyStoreException e) {
            throw new RuntimeException("Error encrypting CBOR for saving to disk", e);
        }
        return dataToSaveBytes;
    }

    private void saveToDiskAuthKeys(MapBuilder<CborBuilder> map) {
        map.put("perReaderSessionKeyAlias", mPerReaderSessionKeyAlias);
        MapBuilder<MapBuilder<CborBuilder>> acpTimeoutKeyMapBuilder = map.putMap(
                "acpTimeoutKeyMap");
        for (Map.Entry<Integer, String> entry : mAcpTimeoutKeyAliases.entrySet()) {
            int profileId = entry.getKey();
            String acpAlias = entry.getValue();
            acpTimeoutKeyMapBuilder.put(new UnsignedInteger(profileId),
                    new UnicodeString(acpAlias));
        }
    }

    private void saveToDiskNamespaceDatas(MapBuilder<CborBuilder> map) {
        MapBuilder<MapBuilder<CborBuilder>> ensArrayBuilder = map.putMap(
                "namespaceDatas");
        for (PersonalizationData.NamespaceData namespaceData : mNamespaceDatas) {
            ensArrayBuilder.put(new UnicodeString(namespaceData.getNamespaceName()),
                    Util.namespaceDataToCbor(namespaceData));
        }
    }

    private void saveToDiskACPs(MapBuilder<CborBuilder> map) {
        ArrayBuilder<MapBuilder<CborBuilder>> acpArrayBuilder = map.putArray(
                "accessControlProfiles");
        for (AccessControlProfile profile : mAccessControlProfiles) {
            acpArrayBuilder.add(Util.accessControlProfileToCbor(profile));
        }
    }

    private void saveToDiskAuthDatas(MapBuilder<CborBuilder> map) {
        ArrayBuilder<MapBuilder<CborBuilder>> authKeyDataArrayBuilder = map.putArray(
                "authKeyDatas");
        for (AuthKeyData data : mAuthKeyDatas) {
            long expirationDateMillis = Long.MAX_VALUE;
            if (data.mExpirationDate != null) {
                expirationDateMillis = data.mExpirationDate.getTimeInMillis();
            }
            authKeyDataArrayBuilder.addMap()
                    .put("alias", data.mAlias)
                    .put("useCount", data.mUseCount)
                    .put("certificate", data.mCertificate)
                    .put("staticAuthenticationData", data.mStaticAuthenticationData)
                    .put("pendingAlias", data.mPendingAlias)
                    .put("pendingCertificate", data.mPendingCertificate)
                    .put("expirationDateMillis", expirationDateMillis)
                    .end();
        }
    }

    private void saveToDiskBasic(MapBuilder<CborBuilder> map) {
        map.put("docType", mDocType);
        map.put("credentialKeyAlias", mCredentialKeyAlias);
        ArrayBuilder<MapBuilder<CborBuilder>> credentialKeyCertChainBuilder =
                map.putArray("credentialKeyCertChain");
        for (X509Certificate certificate : mCertificateChain) {
            try {
                credentialKeyCertChainBuilder.add(certificate.getEncoded());
            } catch (CertificateEncodingException e) {
                throw new RuntimeException("Error encoding certificate", e);
            }
        }
        map.put("proofOfProvisioningSha256", mProofOfProvisioningSha256);
        map.put("authKeyCount", mAuthKeyCount);
        map.put("authKeyMaxUses", mAuthMaxUsesPerKey);
    }

    private boolean loadFromDisk(String dataKeyAlias) {
        String filename = getFilenameForCredentialData(mCredentialName);
        byte[] encryptedFileData = new byte[0];
        try {
            AtomicFile file = new AtomicFile(mContext.getFileStreamPath(filename));
            encryptedFileData = file.readFully();
        } catch (Exception e) {
            return false;
        }

        byte[] fileData = loadFromDiskDecrypt(dataKeyAlias, encryptedFileData);

        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(fileData);
            List<DataItem> dataItems = new CborDecoder(bais).decode();
            if (dataItems.size() != 1) {
                throw new RuntimeException("Expected 1 item, found " + dataItems.size());
            }
            if (!(dataItems.get(0) instanceof co.nstant.in.cbor.model.Map)) {
                throw new RuntimeException("Item is not a map");
            }
            co.nstant.in.cbor.model.Map map = (co.nstant.in.cbor.model.Map) dataItems.get(0);

            loadBasic(map);
            loadCredentialKeyCertChain(map);
            loadProofOfProvisioningSha256(map);
            loadAccessControlProfiles(map);
            loadNamespaceDatas(map);
            loadAuthKey(map);

        } catch (CborException e) {
            throw new RuntimeException("Error decoding data", e);
        }
        return true;
    }

    private byte[] loadFromDiskDecrypt(String dataKeyAlias, byte[] encryptedFileData) {
        byte[] fileData = null;
        try {
            KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);
            KeyStore.Entry entry = ks.getEntry(dataKeyAlias, null);
            SecretKey secretKey = ((KeyStore.SecretKeyEntry) entry).getSecretKey();

            if (encryptedFileData.length < 12) {
                throw new RuntimeException("Encrypted CBOR on disk is too small");
            }
            ByteBuffer byteBuffer = ByteBuffer.wrap(encryptedFileData);
            byte[] iv = new byte[12];
            byteBuffer.get(iv);
            byte[] cipherText = new byte[encryptedFileData.length - 12];
            byteBuffer.get(cipherText);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(128, iv));
            fileData = cipher.doFinal(cipherText);
        } catch (InvalidAlgorithmParameterException
                | NoSuchPaddingException
                | BadPaddingException
                | NoSuchAlgorithmException
                | CertificateException
                | InvalidKeyException
                | IOException
                | IllegalBlockSizeException
                | UnrecoverableEntryException
                | KeyStoreException e) {
            throw new RuntimeException("Error decrypting CBOR", e);
        }
        return fileData;
    }

    private void loadBasic(co.nstant.in.cbor.model.Map map) {
        mDocType = ((UnicodeString) map.get(new UnicodeString("docType"))).getString();
        mCredentialKeyAlias = ((UnicodeString) map.get(
                new UnicodeString("credentialKeyAlias"))).getString();
    }

    private void loadAuthKey(co.nstant.in.cbor.model.Map map) {
        mPerReaderSessionKeyAlias = ((UnicodeString) map.get(
                new UnicodeString("perReaderSessionKeyAlias"))).getString();

        DataItem userAuthKeyAliases = map.get(new UnicodeString("acpTimeoutKeyMap"));
        if (!(userAuthKeyAliases instanceof co.nstant.in.cbor.model.Map)) {
            throw new RuntimeException("acpTimeoutKeyMap not found or not map");
        }
        mAcpTimeoutKeyAliases = new HashMap<>();
        for (DataItem key : ((co.nstant.in.cbor.model.Map) userAuthKeyAliases).getKeys()) {
            if (!(key instanceof UnsignedInteger)) {
                throw new RuntimeException(
                        "Key in acpTimeoutKeyMap is not an integer");
            }
            int profileId = ((UnsignedInteger) key).getValue().intValue();
            DataItem item = ((co.nstant.in.cbor.model.Map) userAuthKeyAliases).get(key);
            if (!(item instanceof UnicodeString)) {
                throw new RuntimeException(
                        "Item in acpTimeoutKeyMap is not a string");
            }
            String acpAlias = ((UnicodeString) item).getString();
            mAcpTimeoutKeyAliases.put(profileId, acpAlias);
        }

        mAuthKeyCount = ((Number) map.get(
                new UnicodeString("authKeyCount"))).getValue().intValue();
        mAuthMaxUsesPerKey = ((Number) map.get(
                new UnicodeString("authKeyMaxUses"))).getValue().intValue();

        DataItem authKeyDatas = map.get(new UnicodeString("authKeyDatas"));
        if (!(authKeyDatas instanceof Array)) {
            throw new RuntimeException("authKeyDatas not found or not array");
        }
        mAuthKeyDatas = new ArrayList<AuthKeyData>();
        for (DataItem item : ((Array) authKeyDatas).getDataItems()) {
            AuthKeyData data = new AuthKeyData();

            co.nstant.in.cbor.model.Map im = (co.nstant.in.cbor.model.Map) item;

            data.mAlias = ((UnicodeString) im.get(new UnicodeString("alias"))).getString();
            data.mUseCount = ((Number) im.get(new UnicodeString("useCount"))).getValue().intValue();
            data.mCertificate = ((ByteString) im.get(new UnicodeString("certificate"))).getBytes();
            data.mStaticAuthenticationData = ((ByteString) im.get(
                    new UnicodeString("staticAuthenticationData"))).getBytes();
            data.mPendingAlias = ((UnicodeString) im.get(
                    new UnicodeString("pendingAlias"))).getString();
            data.mPendingCertificate = ((ByteString) im.get(
                    new UnicodeString("pendingCertificate"))).getBytes();

            // expirationDateMillis was added in a later release, may not be present
            long expirationDateMillis = Long.MAX_VALUE;
            DataItem expirationDateMillisItem = im.get(new UnicodeString("expirationDateMillis"));
            if (expirationDateMillisItem != null) {
                if (!(expirationDateMillisItem instanceof Number)) {
                    throw new RuntimeException("expirationDateMillis not a number");
                }
                expirationDateMillis = ((Number) expirationDateMillisItem).getValue().longValue();
            }
            Calendar expirationDate = Calendar.getInstance();
            expirationDate.setTimeInMillis(expirationDateMillis);
            data.mExpirationDate = expirationDate;

            mAuthKeyDatas.add(data);
        }
    }

    private void loadNamespaceDatas(co.nstant.in.cbor.model.Map map) {
        DataItem namespaceDatas = map.get(new UnicodeString("namespaceDatas"));
        if (!(namespaceDatas instanceof co.nstant.in.cbor.model.Map)) {
            throw new RuntimeException("namespaceDatas not found or not map");
        }
        mNamespaceDatas = new ArrayList<PersonalizationData.NamespaceData>();
        for (DataItem key : ((co.nstant.in.cbor.model.Map) namespaceDatas).getKeys()) {
            if (!(key instanceof UnicodeString)) {
                throw new RuntimeException("Key in namespaceDatas is not a string");
            }
            String namespaceName = ((UnicodeString) key).getString();
            DataItem item = ((co.nstant.in.cbor.model.Map) namespaceDatas).get(key);
            mNamespaceDatas.add(Util.namespaceDataFromCbor(namespaceName, item));
        }
    }

    private void loadAccessControlProfiles(co.nstant.in.cbor.model.Map map) {
        DataItem accessControlProfiles = map.get(new UnicodeString("accessControlProfiles"));
        if (!(accessControlProfiles instanceof Array)) {
            throw new RuntimeException(
                    "accessControlProfiles not found or not array");
        }
        mAccessControlProfiles = new ArrayList<AccessControlProfile>();
        mProfileIdToAcpMap = new HashMap<Integer, AccessControlProfile>();
        for (DataItem item : ((Array) accessControlProfiles).getDataItems()) {
            AccessControlProfile profile = Util.accessControlProfileFromCbor(item);
            mAccessControlProfiles.add(profile);
            mProfileIdToAcpMap.put(profile.getAccessControlProfileId().getId(), profile);
        }
    }

    private void loadProofOfProvisioningSha256(co.nstant.in.cbor.model.Map map) {
        DataItem proofOfProvisioningSha256 = map.get(
                new UnicodeString("proofOfProvisioningSha256"));
        if (!(proofOfProvisioningSha256 instanceof ByteString)) {
            throw new RuntimeException(
                    "proofOfProvisioningSha256 not found or not bstr");
        }
        mProofOfProvisioningSha256 = ((ByteString) proofOfProvisioningSha256).getBytes();
    }

    private void loadCredentialKeyCertChain(co.nstant.in.cbor.model.Map map) {
        DataItem credentialKeyCertChain = map.get(new UnicodeString("credentialKeyCertChain"));
        if (!(credentialKeyCertChain instanceof Array)) {
            throw new RuntimeException(
                    "credentialKeyCertChain not found or not array");
        }
        mCertificateChain = new ArrayList<>();
        for (DataItem item : ((Array) credentialKeyCertChain).getDataItems()) {
            byte[] encodedCert = ((ByteString) item).getBytes();
            try {
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                ByteArrayInputStream certBais = new ByteArrayInputStream(encodedCert);
                mCertificateChain.add((X509Certificate) cf.generateCertificate(certBais));
            } catch (CertificateException e) {
                throw new RuntimeException("Error decoding certificate blob", e);
            }
        }
    }

    Collection<AccessControlProfile> getAccessControlProfiles() {
        return mAccessControlProfiles;
    }

    Collection<PersonalizationData.NamespaceData> getNamespaceDatas() {
        return mNamespaceDatas;
    }

    PersonalizationData.NamespaceData lookupNamespaceData(String nameSpace) {
        // TODO: This might be slow, maybe build map at load/build time
        for (PersonalizationData.NamespaceData namespaceData : mNamespaceDatas) {
            if (namespaceData.getNamespaceName().equals(nameSpace)) {
                return namespaceData;
            }
        }
        return null;
    }

    String getCredentialKeyAlias() {
        return mCredentialKeyAlias;
    }

    String getPerReaderSessionKeyAlias() {
        return mPerReaderSessionKeyAlias;
    }

    int getAuthKeyCount() {
        return mAuthKeyCount;
    }

    int getAuthMaxUsesPerKey() {
        return mAuthMaxUsesPerKey;
    }

    int[] getAuthKeyUseCounts() {
        int[] result = new int[mAuthKeyCount];
        int n = 0;
        for (AuthKeyData data : mAuthKeyDatas) {
            result[n++] = data.mUseCount;
        }
        return result;
    }

    void setAvailableAuthenticationKeys(int keyCount, int maxUsesPerKey) {
        int prevAuthKeyCount = mAuthKeyCount;
        mAuthKeyCount = keyCount;
        mAuthMaxUsesPerKey = maxUsesPerKey;

        if (prevAuthKeyCount < mAuthKeyCount) {
            // Added non-zero number of auth keys...
            for (int n = prevAuthKeyCount; n < mAuthKeyCount; n++) {
                mAuthKeyDatas.add(new AuthKeyData());
            }
        } else if (prevAuthKeyCount > mAuthKeyCount) {
            KeyStore ks = null;
            try {
                ks = KeyStore.getInstance("AndroidKeyStore");
                ks.load(null);
            } catch (CertificateException
                    | IOException
                    | NoSuchAlgorithmException
                    | KeyStoreException e) {
                throw new RuntimeException("Error loading keystore", e);
            }

            int numKeysToDelete = prevAuthKeyCount - mAuthKeyCount;
            // Removed non-zero number of auth keys. For now we just delete
            // the keys at the beginning... (an optimization could be to instead
            // delete the keys with the biggest use count).
            for (int n = 0; n < numKeysToDelete; n++) {
                AuthKeyData data = mAuthKeyDatas.get(0);
                if (!data.mAlias.isEmpty()) {
                    try {
                        if (ks.containsAlias(data.mAlias)) {
                            ks.deleteEntry(data.mAlias);
                        }
                    } catch (KeyStoreException e) {
                        throw new RuntimeException(
                                "Error deleting auth key with mAlias " + data.mAlias, e);
                    }
                }
                if (!data.mPendingAlias.isEmpty()) {
                    try {
                        if (ks.containsAlias(data.mPendingAlias)) {
                            ks.deleteEntry(data.mPendingAlias);
                        }
                    } catch (KeyStoreException e) {
                        throw new RuntimeException(
                                "Error deleting auth key with mPendingAlias " + data.mPendingAlias,
                                e);
                    }
                }
                mAuthKeyDatas.remove(0);
            }
        }
        saveToDisk();
    }

    Collection<X509Certificate> getAuthKeysNeedingCertification() {
        KeyStore ks = null;
        try {
            ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);
        } catch (CertificateException
                | IOException
                | NoSuchAlgorithmException
                | KeyStoreException e) {
            throw new RuntimeException("Error loading keystore", e);
        }

        ArrayList<X509Certificate> certificates = new ArrayList<X509Certificate>();

        Calendar now = Calendar.getInstance();

        // Determine which keys need certification (or re-certification) and generate
        // keys and X.509 certs for these and mark them as pending.
        for (int n = 0; n < mAuthKeyCount; n++) {
            AuthKeyData data = mAuthKeyDatas.get(n);

            boolean keyExceededUseCount = (data.mUseCount >= mAuthMaxUsesPerKey);
            boolean keyBeyondExpirationDate = false;
            if (data.mExpirationDate != null) {
                keyBeyondExpirationDate = now.after(data.mExpirationDate);
            }
            boolean newKeyNeeded =
                    data.mAlias.isEmpty() || keyExceededUseCount || keyBeyondExpirationDate;
            boolean certificationPending = !data.mPendingAlias.isEmpty();

            if (newKeyNeeded && !certificationPending) {
                try {
                    // Calculate name to use and be careful to avoid collisions when
                    // re-certifying an already populated slot.
                    String aliasForAuthKey = mCredentialKeyAlias + String.format("_auth_%d", n);
                    if (aliasForAuthKey.equals(data.mAlias)) {
                        aliasForAuthKey = aliasForAuthKey + "_";
                    }

                    KeyPairGenerator kpg = KeyPairGenerator.getInstance(
                            KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore");
                    KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(
                            aliasForAuthKey,
                            KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY)
                            .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512);
                    kpg.initialize(builder.build());
                    kpg.generateKeyPair();

                    X509Certificate certificate = Util.generateAuthenticationKeyCert(
                            aliasForAuthKey, mCredentialKeyAlias, mProofOfProvisioningSha256);

                    data.mPendingAlias = aliasForAuthKey;
                    data.mPendingCertificate = certificate.getEncoded();
                    certificationPending = true;
                } catch (InvalidAlgorithmParameterException
                        | NoSuchAlgorithmException
                        | NoSuchProviderException
                        | CertificateEncodingException e) {
                    throw new RuntimeException("Error creating auth key", e);
                }
            }

            if (certificationPending) {
                try {
                    CertificateFactory cf = CertificateFactory.getInstance("X.509");
                    ByteArrayInputStream bais = new ByteArrayInputStream(data.mPendingCertificate);
                    certificates.add((X509Certificate) cf.generateCertificate(bais));
                } catch (CertificateException e) {
                    throw new RuntimeException(
                            "Error creating certificate for auth key", e);
                }
            }
        }

        saveToDisk();

        return certificates;
    }

    void storeStaticAuthenticationData(X509Certificate authenticationKey,
            Calendar expirationDate,
            byte[] staticAuthData)
            throws UnknownAuthenticationKeyException {
        AuthKeyData dataForAuthKey = null;
        CertificateFactory cf = null;
        try {
            cf = CertificateFactory.getInstance("X.509");

            for (AuthKeyData data : mAuthKeyDatas) {
                if (data.mPendingCertificate.length > 0) {
                    ByteArrayInputStream bais = new ByteArrayInputStream(data.mPendingCertificate);
                    X509Certificate certificate = (X509Certificate) cf.generateCertificate(bais);
                    if (certificate.equals(authenticationKey)) {
                        dataForAuthKey = data;
                        break;
                    }
                }
            }
        } catch (CertificateException e) {
            throw new RuntimeException("Error encoding certificate", e);
        }

        if (dataForAuthKey == null) {
            throw new UnknownAuthenticationKeyException("No such authentication key");
        }

        // Delete old key, if set.
        if (!dataForAuthKey.mAlias.isEmpty()) {
            KeyStore ks = null;
            try {
                ks = KeyStore.getInstance("AndroidKeyStore");
                ks.load(null);
                if (ks.containsAlias(dataForAuthKey.mAlias)) {
                    ks.deleteEntry(dataForAuthKey.mAlias);
                }
            } catch (CertificateException
                    | IOException
                    | NoSuchAlgorithmException
                    | KeyStoreException e) {
                throw new RuntimeException("Error deleting old authentication key", e);
            }
        }
        dataForAuthKey.mAlias = dataForAuthKey.mPendingAlias;
        dataForAuthKey.mCertificate = dataForAuthKey.mPendingCertificate;
        dataForAuthKey.mStaticAuthenticationData = staticAuthData;
        dataForAuthKey.mUseCount = 0;
        dataForAuthKey.mPendingAlias = "";
        dataForAuthKey.mPendingCertificate = new byte[0];
        dataForAuthKey.mExpirationDate = expirationDate;
        saveToDisk();
    }

    /**
     * Selects an authentication key to use.
     *
     * The victim is picked simply by choosing the key with the smallest use count. (This may
     * change in the future.)
     *
     * The use count of the returned authentication key will be increased by one.
     *
     * If no key could be found {@code null} is returned.
     *
     * @param allowUsingExhaustedKeys If {@code true}, allow using an authentication key which
     *                                use count has been exceeded if no other key is available.
     * @param allowUsingExpiredKeys If {@code true}, allow using an authentication key which
     *                              is expired.
     * @return A pair containing the authentication key and its associated static authentication
     * data or {@code null} if no key could be found.
     */
    Pair<PrivateKey, byte[]> selectAuthenticationKey(boolean allowUsingExhaustedKeys,
            boolean allowUsingExpiredKeys) {

        // First try to find a un-expired key..
        Pair<PrivateKey, byte[]> keyAndStaticData =
                selectAuthenticationKeyHelper(allowUsingExhaustedKeys, false);
        if (keyAndStaticData != null) {
            return keyAndStaticData;
        }
        // Nope, try to see if there's an expired key (if allowed)
        if (!allowUsingExpiredKeys) {
            return null;
        }
        return selectAuthenticationKeyHelper(allowUsingExhaustedKeys, true);
    }

    Pair<PrivateKey, byte[]> selectAuthenticationKeyHelper(boolean allowUsingExhaustedKeys,
            boolean allowUsingExpiredKeys) {
        AuthKeyData candidate = null;

        Calendar now = Calendar.getInstance();

        for (int n = 0; n < mAuthKeyCount; n++) {
            AuthKeyData data = mAuthKeyDatas.get(n);
            if (!data.mAlias.isEmpty()) {
                if (data.mExpirationDate != null) {
                    if (now.after(data.mExpirationDate)) {
                        // expired...
                        if (!allowUsingExpiredKeys) {
                            continue;
                        }
                    }
                }

                if (candidate == null || data.mUseCount < candidate.mUseCount) {
                    candidate = data;
                }
            }
        }

        if (candidate == null) {
            return null;
        }

        // We've found the key with lowest use count... so if this is beyond maximum uses
        // so are all the other ones. So fail if we're not allowed to use exhausted keys.
        if (candidate.mUseCount >= mAuthMaxUsesPerKey && !allowUsingExhaustedKeys) {
            return null;
        }

        KeyStore.Entry entry = null;
        try {
            KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);
            entry = ks.getEntry(candidate.mAlias, null);
        } catch (CertificateException
                | IOException
                | NoSuchAlgorithmException
                | KeyStoreException
                | UnrecoverableEntryException e) {
            throw new RuntimeException("Error loading keystore", e);
        }

        Pair<PrivateKey, byte[]> result = new Pair<>(
                ((KeyStore.PrivateKeyEntry) entry).getPrivateKey(),
                candidate.mStaticAuthenticationData);

        candidate.mUseCount += 1;
        saveToDisk();

        return result;
    }

    String getDocType() {
        return mDocType;
    }

    Collection<X509Certificate> getCredentialKeyCertificateChain() {
        return mCertificateChain;
    }

    AccessControlProfile getAccessControlProfile(AccessControlProfileId profileId) {
        AccessControlProfile profile = mProfileIdToAcpMap.get(profileId.getId());
        if (profile == null) {
            throw new RuntimeException("No profile with id " + profileId.getId());
        }
        return profile;
    }

    private boolean checkUserAuthenticationTimeout(String acpAlias) {
        // Unfortunately there are no APIs to tell if a key needs user authentication to work so
        // we check if the key is available by simply trying to encrypt some data.
        try {
            KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);
            KeyStore.Entry entry = ks.getEntry(acpAlias, null);
            SecretKey secretKey = ((KeyStore.SecretKeyEntry) entry).getSecretKey();

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] clearText = {0x01, 0x02};
            cipher.doFinal(clearText);
            // We don't care about the cipherText, only whether the key is unlocked.
        } catch (NoSuchPaddingException
                | BadPaddingException
                | NoSuchAlgorithmException
                | CertificateException
                | InvalidKeyException
                | IOException
                | IllegalBlockSizeException
                | UnrecoverableEntryException
                | KeyStoreException e) {
            // If this fails, it probably means authentication is needed... (there's no
            // specific exception for that in API level 23, unfortunately.)
            return false;
        }
        return true;
    }

    boolean checkUserAuthentication(AccessControlProfileId accessControlProfileId,
            boolean didUserAuth) {
        AccessControlProfile profile = getAccessControlProfile(accessControlProfileId);
        if (profile.getUserAuthenticationTimeout() == 0) {
            return didUserAuth;
        }
        String acpAlias = mAcpTimeoutKeyAliases.get(accessControlProfileId.getId());
        if (acpAlias == null) {
            throw new RuntimeException(
                    "No key alias for ACP with ID " + accessControlProfileId.getId());
        }
        return checkUserAuthenticationTimeout(acpAlias);
    }

    // Note that a dynamic authentication key may have two Android Keystore keys associated with
    // it.. the obvious one is for a previously certificated key. This key may possibly have an
    // use-count which is already exhausted. The other one is for a key yet pending certification.
    //
    // Why is it implemented this way? Because we never want selectAuthenticationKey() to fail.
    // That is, it's better to use a key with an exhausted use-count (slightly bad for user privacy
    // in terms of linkability between multiple presentations) than the user not being able to
    // present their credential at all...
    private static class AuthKeyData {
        // The mAlias for the key in Android Keystore. Is set to the empty string if the key has not
        // yet been created. This is set to the empty string if no key has been certified.
        String mAlias = "";
        // The X509 certificate for the key. This is empty if mAlias is empty.
        byte[] mCertificate = new byte[0];
        // The static authentication data, as set by the application as part of certification. This
        // is empty if mAlias is empty.
        byte[] mStaticAuthenticationData = new byte[0];
        // The number of times a key has been used.
        int mUseCount = 0;
        // The alias for a key pending certification. Once a key has been certified - by
        // calling storeStaticAuthenticationData() - and is no longer pending, both mPendingAlias
        // and mPendingCertificate will be set to the empty string and empty byte array.
        String mPendingAlias = "";
        // The X509 certificate for a key pending certification.
        byte[] mPendingCertificate = new byte[0];

        Calendar mExpirationDate = null;

        AuthKeyData() {
        }
    }
}
