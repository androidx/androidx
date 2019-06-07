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

import android.annotation.SuppressLint;
import android.content.Context;
import android.security.keystore.KeyProperties;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.biometric.BiometricPrompt;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECPoint;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyAgreement;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import co.nstant.in.cbor.CborBuilder;
import co.nstant.in.cbor.CborDecoder;
import co.nstant.in.cbor.CborEncoder;
import co.nstant.in.cbor.CborException;
import co.nstant.in.cbor.builder.MapBuilder;
import co.nstant.in.cbor.model.Array;
import co.nstant.in.cbor.model.ByteString;
import co.nstant.in.cbor.model.DataItem;
import co.nstant.in.cbor.model.Map;
import co.nstant.in.cbor.model.UnicodeString;

class SoftwareIdentityCredential extends IdentityCredential {

    private static final String TAG = "SWIdentityCredential";
    private String mCredentialName;

    private Context mContext;

    private CredentialData mData;

    private KeyPair mEphemeralKeyPair = null;

    private SecretKey mSecretKey = null;
    private SecretKey mReaderSecretKey = null;

    private int mEphemeralCounter;
    private int mReadersExpectedEphemeralCounter;
    private byte[] mAuthKeyAssociatedData = null;
    private PrivateKey mAuthKey = null;
    private BiometricPrompt.CryptoObject mCryptoObject = null;

    private PublicKey mReaderEphemeralPublicKey = null;
    private byte[] mSessionTranscript = null;

    SoftwareIdentityCredential(Context context, String credentialName,
            @IdentityCredentialStore.Ciphersuite int cipherSuite)
            throws CipherSuiteNotSupportedException {
        if (cipherSuite
                != IdentityCredentialStore.CIPHERSUITE_ECDHE_HKDF_ECDSA_WITH_AES_256_GCM_SHA256) {
            throw new CipherSuiteNotSupportedException("Unsupported Cipher Suite");
        }
        mContext = context;
        mCredentialName = credentialName;
    }

    boolean loadData() {
        mData = CredentialData.loadCredentialData(mContext, mCredentialName);
        if (mData == null) {
            return false;
        }
        return true;
    }

    static byte[] delete(Context context, String credentialName) {
        return CredentialData.delete(context, credentialName);
    }

    // This only extracts the requested namespaces, not DocType or RequestInfo. We
    // can do this later if it's needed.
    private static HashMap<String, Collection<String>> parseRequestMessage(
            @Nullable byte[] requestMessage) {
        HashMap<String, Collection<String>> result = new HashMap<>();

        if (requestMessage == null) {
            return result;
        }

        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(requestMessage);
            List<DataItem> dataItems = new CborDecoder(bais).decode();
            if (dataItems.size() != 1) {
                throw new RuntimeException("Expected 1 item, found " + dataItems.size());
            }
            if (!(dataItems.get(0) instanceof Map)) {
                throw new RuntimeException("Item is not a map");
            }
            Map map = (Map) dataItems.get(0);

            DataItem nameSpaces = map.get(new UnicodeString("nameSpaces"));
            if (!(nameSpaces instanceof Map)) {
                throw new RuntimeException(
                        "nameSpaces entry not found or not map");
            }

            for (DataItem keyItem : ((Map) nameSpaces).getKeys()) {
                if (!(keyItem instanceof UnicodeString)) {
                    throw new RuntimeException(
                            "Key item in NameSpaces map not UnicodeString");
                }
                String nameSpace = ((UnicodeString) keyItem).getString();
                ArrayList<String> names = new ArrayList<>();

                DataItem valueItem = ((Map) nameSpaces).get(keyItem);
                if (!(valueItem instanceof Map)) {
                    throw new RuntimeException(
                            "Value item in NameSpaces map not Map");
                }
                for (DataItem item : ((Map) valueItem).getKeys()) {
                    if (!(item instanceof UnicodeString)) {
                        throw new RuntimeException(
                                "Item in nameSpaces array not UnicodeString");
                    }
                    names.add(((UnicodeString) item).getString());
                    // TODO: check that value is a boolean
                }
                result.put(nameSpace, names);
            }

        } catch (CborException e) {
            throw new RuntimeException("Error decoding request message", e);
        }
        return result;
    }

    @SuppressLint("NewApi")
    @Override
    public @NonNull KeyPair createEphemeralKeyPair() {
        if (mEphemeralKeyPair == null) {
            try {
                KeyPairGenerator kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC);
                ECGenParameterSpec ecSpec = new ECGenParameterSpec("prime256v1");
                kpg.initialize(ecSpec);
                mEphemeralKeyPair = kpg.generateKeyPair();
            } catch (NoSuchAlgorithmException
                    | InvalidAlgorithmParameterException e) {
                throw new RuntimeException("Error generating ephemeral key", e);
            }
        }
        return mEphemeralKeyPair;
    }

    @Override
    public void setReaderEphemeralPublicKey(@NonNull PublicKey readerEphemeralPublicKey) {
        mReaderEphemeralPublicKey = readerEphemeralPublicKey;
    }

    @Override
    public void setSessionTranscript(@NonNull byte[] sessionTranscript) {
        if (mSessionTranscript != null) {
            throw new RuntimeException("SessionTranscript already set");
        }
        mSessionTranscript = sessionTranscript.clone();
    }

    private void ensureSessionEncryptionKey() {
        if (mSecretKey != null) {
            return;
        }
        if (mReaderEphemeralPublicKey == null) {
            throw new RuntimeException("Reader ephemeral key not set");
        }
        if (mSessionTranscript == null) {
            throw new RuntimeException("Session transcript not set");
        }
        try {
            KeyAgreement ka = KeyAgreement.getInstance("ECDH");
            ka.init(mEphemeralKeyPair.getPrivate());
            ka.doPhase(mReaderEphemeralPublicKey, true);
            byte[] sharedSecret = ka.generateSecret();

            byte[] sessionTranscriptBytes =
                    Util.prependSemanticTagForEncodedCbor(mSessionTranscript);
            byte[] sharedSecretWithSessionTranscriptBytes =
                    Util.concatArrays(sharedSecret, sessionTranscriptBytes);

            byte[] salt = new byte[1];
            byte[] info = new byte[0];

            salt[0] = 0x01;
            byte[] derivedKey = Util.computeHkdf("HmacSha256",
                    sharedSecretWithSessionTranscriptBytes, salt, info, 32);
            mSecretKey = new SecretKeySpec(derivedKey, "AES");

            salt[0] = 0x00;
            derivedKey = Util.computeHkdf("HmacSha256", sharedSecretWithSessionTranscriptBytes,
                    salt, info, 32);
            mReaderSecretKey = new SecretKeySpec(derivedKey, "AES");

            mEphemeralCounter = 1;
            mReadersExpectedEphemeralCounter = 1;

        } catch (InvalidKeyException
                | NoSuchAlgorithmException e) {
            throw new RuntimeException("Error performing key agreement", e);
        }
    }

    @Override
    public @NonNull
    byte[] encryptMessageToReader(@NonNull byte[] messagePlaintext) {
        ensureSessionEncryptionKey();
        byte[] messageCiphertextAndAuthTag = null;
        try {
            ByteBuffer iv = ByteBuffer.allocate(12);
            iv.putInt(0, 0x00000000);
            iv.putInt(4, 0x00000001);
            iv.putInt(8, mEphemeralCounter);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec encryptionParameterSpec = new GCMParameterSpec(128, iv.array());
            cipher.init(Cipher.ENCRYPT_MODE, mSecretKey, encryptionParameterSpec);
            messageCiphertextAndAuthTag = cipher.doFinal(messagePlaintext);
        } catch (BadPaddingException
                | IllegalBlockSizeException
                | NoSuchPaddingException
                | InvalidKeyException
                | NoSuchAlgorithmException
                | InvalidAlgorithmParameterException e) {
            throw new RuntimeException("Error encrypting message", e);
        }
        mEphemeralCounter += 1;
        return messageCiphertextAndAuthTag;
    }

    @Override
    public @NonNull
    byte[] decryptMessageFromReader(@NonNull byte[] messageCiphertext)
            throws MessageDecryptionException {
        ensureSessionEncryptionKey();
        ByteBuffer iv = ByteBuffer.allocate(12);
        iv.putInt(0, 0x00000000);
        iv.putInt(4, 0x00000000);
        iv.putInt(8, mReadersExpectedEphemeralCounter);
        byte[] plainText = null;
        try {
            final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, mReaderSecretKey, new GCMParameterSpec(128,
                    iv.array()));
            plainText = cipher.doFinal(messageCiphertext);
        } catch (BadPaddingException
                | IllegalBlockSizeException
                | InvalidAlgorithmParameterException
                | InvalidKeyException
                | NoSuchAlgorithmException
                | NoSuchPaddingException e) {
            throw new MessageDecryptionException("Error decrypting message", e);
        }
        mReadersExpectedEphemeralCounter += 1;
        return plainText;
    }

    @Override
    public @NonNull
    Collection<X509Certificate> getCredentialKeyCertificateChain() {
        return mData.getCredentialKeyCertificateChain();
    }

    private void ensureCryptoObject() {
        String aliasForCryptoObject = mData.getPerReaderSessionKeyAlias();
        if (aliasForCryptoObject.isEmpty()) {
            // This happens if there are no ACPs with user-auth.
            return;
        }
        try {
            KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);
            KeyStore.Entry entry = ks.getEntry(aliasForCryptoObject, null);
            SecretKey perReaderSessionKey = ((KeyStore.SecretKeyEntry) entry).getSecretKey();
            Cipher perReaderSessionCipher = Cipher.getInstance("AES/GCM/NoPadding");
            perReaderSessionCipher.init(Cipher.ENCRYPT_MODE, perReaderSessionKey);
            mCryptoObject = new BiometricPrompt.CryptoObject(perReaderSessionCipher);
        } catch (CertificateException
                | NoSuchPaddingException
                | InvalidKeyException
                | IOException
                | NoSuchAlgorithmException
                | KeyStoreException
                | UnrecoverableEntryException e) {
            throw new RuntimeException("Error creating Cipher for perReaderSessionKey", e);
        }
    }

    private void ensureAuthKey() throws NoAuthenticationKeyAvailableException {
        if (mAuthKey != null) {
            return;
        }

        Pair<PrivateKey, byte[]> keyAndStaticData = mData.selectAuthenticationKey(
                mAllowUsingExhaustedKeys);
        if (keyAndStaticData == null) {
            throw new NoAuthenticationKeyAvailableException(
                    "No authentication key available for signing");
        }
        mAuthKey = keyAndStaticData.first;
        mAuthKeyAssociatedData = keyAndStaticData.second;
    }

    boolean mAllowUsingExhaustedKeys = true;

    @Override
    public void setAllowUsingExhaustedKeys(boolean allowUsingExhaustedKeys) {
        mAllowUsingExhaustedKeys = allowUsingExhaustedKeys;
    }

    @Override
    @Nullable
    public BiometricPrompt.CryptoObject getCryptoObject() {
        ensureCryptoObject();
        return mCryptoObject;
    }

    private boolean hasEphemeralKeyInDeviceEngagement(@NonNull byte[] sessionTranscript) {
        if (mEphemeralKeyPair == null) {
            return false;
        }
        // The place to search for X and Y is in the DeviceEngagementBytes which is
        // the first bstr in the SessionTranscript array.
        ByteArrayInputStream bais = new ByteArrayInputStream(sessionTranscript);
        List<DataItem> dataItems = null;
        try {
            dataItems = new CborDecoder(bais).decode();
        } catch (CborException e) {
            Log.e(TAG, "Error parsing SessionTranscript CBOR");
            return false;
        }
        if (dataItems.size() != 1
                || !(dataItems.get(0) instanceof Array)
                || ((Array) dataItems.get(0)).getDataItems().size() != 2) {
            Log.e(TAG, "SessionTranscript is not an array of length 2");
            return false;
        }
        if (!(((Array) dataItems.get(0)).getDataItems().get(0) instanceof ByteString)) {
            Log.e(TAG, "First element of SessionTranscript array is not a bstr");
            return false;
        }
        byte[] deviceEngagementBytes =
                ((ByteString) ((Array) dataItems.get(0)).getDataItems().get(0)).getBytes();

        ECPoint w = ((ECPublicKey) mEphemeralKeyPair.getPublic()).getW();
        // X and Y are always positive so for interop we remove any leading zeroes
        // inserted by the BigInteger encoder.
        byte[] x = Util.stripLeadingZeroes(w.getAffineX().toByteArray());
        byte[] y = Util.stripLeadingZeroes(w.getAffineY().toByteArray());

        if (!Util.hasSubByteArray(deviceEngagementBytes, x)
                && !Util.hasSubByteArray(deviceEngagementBytes, y)) {
            return false;
        }
        return true;
    }


    private boolean mDidUserAuthResult = false;

    private boolean mDidUserAuthAlreadyCalled = false;

    // Returns true if the user authenticated using the cryptoObject we gave the
    // app in the getCryptoObject() method.
    //
    private boolean didUserAuth() {
        if (!mDidUserAuthAlreadyCalled) {
            mDidUserAuthResult = didUserAuthNoCache();
            mDidUserAuthAlreadyCalled = true;
        }
        return mDidUserAuthResult;
    }

    private boolean didUserAuthNoCache() {
        if (mCryptoObject == null) {
            // Certainly didn't auth since they didn't even get a cryptoObject (or no ACPs
            // are using user-auth).
            return false;
        }
        try {
            Cipher cipher = mCryptoObject.getCipher();
            byte[] clearText = new byte[16];
            // We don't care about the cipherText, only whether the key is unlocked.
            cipher.doFinal(clearText);
        } catch (IllegalBlockSizeException
                | BadPaddingException e) {
            // If we get here, it's because the user didn't auth.
            return false;
        }
        return true;
    }


    @NonNull
    @Override
    public ResultData getEntries(
            @Nullable byte[] requestMessage,
            @NonNull java.util.Map<String, Collection<String>> entriesToRequest,
            @Nullable byte[] readerSignature)
            throws NoAuthenticationKeyAvailableException,
            InvalidReaderSignatureException, InvalidRequestMessageException,
            EphemeralPublicKeyNotFoundException {

        if (mSessionTranscript != null && !hasEphemeralKeyInDeviceEngagement(mSessionTranscript)) {
            throw new EphemeralPublicKeyNotFoundException(
                    "Did not find ephemeral public key X and Y coordinates in SessionTranscript "
                            + "(make sure leading zeroes are not used)");
        }

        HashMap<String, Collection<String>> requestMessageMap = parseRequestMessage(requestMessage);

        // Check reader signature, if requested.
        Collection<X509Certificate> readerCertChain = null;
        if (readerSignature != null) {
            if (mSessionTranscript == null) {
                throw new InvalidReaderSignatureException(
                        "readerSignature non-null but sessionTranscript was null");
            }
            if (requestMessage == null) {
                throw new InvalidReaderSignatureException(
                        "readerSignature non-null but requestMessage was null");
            }

            try {
                readerCertChain = Util.coseSign1GetX5Chain(readerSignature);
            } catch (CertificateException e) {
                throw new InvalidReaderSignatureException("Error getting x5chain element", e);
            }
            if (readerCertChain.size() < 1) {
                throw new InvalidReaderSignatureException("No x5chain element in reader signature");
            }
            if (!Util.validateCertificateChain(readerCertChain)) {
                throw new InvalidReaderSignatureException("Error validating certificate chain");
            }
            PublicKey readerTopmostPublicKey = readerCertChain.iterator().next().getPublicKey();

            byte[] readerAuthentication = Util.buildReaderAuthenticationCbor(
                    mSessionTranscript,
                    requestMessage);
            byte[] readerAuthenticationBytes =
                    Util.prependSemanticTagForEncodedCbor(readerAuthentication);
            try {
                if (!Util.coseSign1CheckSignature(
                        readerSignature,
                        readerAuthenticationBytes,
                        readerTopmostPublicKey)) {
                    throw new InvalidReaderSignatureException("Reader signature check failed");
                }
            } catch (NoSuchAlgorithmException | InvalidKeyException e) {
                throw new InvalidReaderSignatureException("Error checking reader signature", e);
            }
        }

        SimpleResultData.Builder resultBuilder = new SimpleResultData.Builder();

        CborBuilder deviceNameSpaceBuilder = new CborBuilder();
        MapBuilder<CborBuilder> deviceNameSpacesMapBuilder = deviceNameSpaceBuilder.addMap();


        retrieveValues(requestMessage,
                requestMessageMap,
                readerCertChain,
                entriesToRequest,
                resultBuilder,
                deviceNameSpacesMapBuilder);

        ByteArrayOutputStream adBaos = new ByteArrayOutputStream();
        CborEncoder adEncoder = new CborEncoder(adBaos);
        DataItem deviceNameSpace = deviceNameSpaceBuilder.build().get(0);
        try {
            adEncoder.encode(deviceNameSpace);
        } catch (CborException e) {
            throw new RuntimeException("Error encoding deviceNameSpace", e);
        }
        byte[] authenticatedData = adBaos.toByteArray();
        resultBuilder.setAuthenticatedData(authenticatedData);

        // If the sessionTranscript is available, create the ECDSA signature
        // so the reader can authenticate the DeviceNamespaces CBOR. Also
        // return the staticAuthenticationData associated with the key chosen
        // to be used for signing.
        //
        // Unfortunately we can't do MACing because Android Keystore doesn't
        // implement ECDH. So we resort to ECSDA signing instead.
        if (mSessionTranscript != null) {
            ensureAuthKey();
            resultBuilder.setStaticAuthenticationData(mAuthKeyAssociatedData);

            byte[] deviceAuthentication = Util.buildDeviceAuthenticationCbor(
                    mData.getDocType(),
                    mSessionTranscript,
                    authenticatedData);

            byte[] deviceAuthenticationBytes =
                    Util.prependSemanticTagForEncodedCbor(deviceAuthentication);

            try {
                Signature authKeySignature = Signature.getInstance("SHA256withECDSA");
                authKeySignature.initSign(mAuthKey);
                resultBuilder.setEcdsaSignature(
                        Util.coseSign1Sign(authKeySignature,
                                null,
                                deviceAuthenticationBytes,
                                null));
            } catch (NoSuchAlgorithmException
                    | InvalidKeyException
                    | CertificateEncodingException e) {
                throw new RuntimeException("Error signing DeviceAuthentication CBOR", e);
            }
        }

        return resultBuilder.build();
    }

    private void retrieveValues(
            byte[] requestMessage,
            HashMap<String, Collection<String>> requestMessageMap,
            Collection<X509Certificate> readerCertChain,
            java.util.Map<String, Collection<String>> entriesToRequest,
            SimpleResultData.Builder resultBuilder,
            MapBuilder<CborBuilder> deviceNameSpacesMapBuilder) {

        for (String namespaceName : entriesToRequest.keySet()) {
            Collection<String> entriesToRequestInNamespace = entriesToRequest.get(namespaceName);

            PersonalizationData.NamespaceData loadedNamespace = mData.lookupNamespaceData(
                    namespaceName);

            Collection<String> requestMessageNamespace = requestMessageMap.get(namespaceName);

            retrieveValuesForNamespace(resultBuilder,
                    deviceNameSpacesMapBuilder,
                    entriesToRequestInNamespace,
                    requestMessage,
                    requestMessageNamespace,
                    readerCertChain,
                    namespaceName,
                    loadedNamespace);
        }
    }

    private void retrieveValuesForNamespace(
            SimpleResultData.Builder resultBuilder,
            MapBuilder<CborBuilder> deviceNameSpacesMapBuilder,
            Collection<String> entriesToRequestInNamespace,
            byte[] requestMessage,
            Collection<String> requestMessageNamespace,
            Collection<X509Certificate> readerCertChain,
            String namespaceName,
            PersonalizationData.NamespaceData loadedNamespace) {
        MapBuilder<MapBuilder<CborBuilder>> deviceNamespaceBuilder = null;

        for (String requestedEntryName : entriesToRequestInNamespace) {

            byte[] value = null;
            if (loadedNamespace != null) {
                value = loadedNamespace.getEntryValue(requestedEntryName);
            }

            if (value == null) {
                resultBuilder.addErrorStatus(namespaceName,
                        requestedEntryName,
                        ResultData.STATUS_NO_SUCH_ENTRY);
                continue;
            }

            if (requestMessage != null) {
                if (requestMessageNamespace == null
                        || !requestMessageNamespace.contains(requestedEntryName)) {
                    resultBuilder.addErrorStatus(namespaceName,
                            requestedEntryName,
                            ResultData.STATUS_NOT_IN_REQUEST_MESSAGE);
                    continue;
                }
            }

            Collection<AccessControlProfileId> accessControlProfileIds =
                    loadedNamespace.getAccessControlProfileIds(requestedEntryName);

            @ResultData.Status
            int status = checkAccess(accessControlProfileIds, readerCertChain);
            if (status != ResultData.STATUS_OK) {
                resultBuilder.addErrorStatus(namespaceName, requestedEntryName, status);
                continue;
            }

            resultBuilder.addEntry(namespaceName, requestedEntryName, value);
            if (deviceNamespaceBuilder == null) {
                deviceNamespaceBuilder = deviceNameSpacesMapBuilder.putMap(
                        namespaceName);
            }
            DataItem dataItem = Util.cborToDataItem(value);
            deviceNamespaceBuilder.put(new UnicodeString(requestedEntryName), dataItem);
        }
    }

    @ResultData.Status
    private int checkAccessSingleProfile(AccessControlProfile profile,
            Collection<X509Certificate> readerCertChain) {

        if (profile.isUserAuthenticationRequired()) {
            if (!mData.checkUserAuthentication(profile.getAccessControlProfileId(),
                    didUserAuth())) {
                return ResultData.STATUS_USER_AUTHENTICATION_FAILED;
            }
        }

        X509Certificate profileCert = profile.getReaderCertificate();
        if (profileCert != null) {
            if (readerCertChain == null) {
                return ResultData.STATUS_READER_AUTHENTICATION_FAILED;
            }

            // Need to check if the cert required by the profile is in the given chain.
            boolean foundMatchingCert = false;
            byte[] profilePublicKeyEncoded = profileCert.getPublicKey().getEncoded();
            for (X509Certificate readerCert : readerCertChain) {
                byte[] readerCertPublicKeyEncoded = readerCert.getPublicKey().getEncoded();
                if (Arrays.equals(profilePublicKeyEncoded, readerCertPublicKeyEncoded)) {
                    foundMatchingCert = true;
                    break;
                }
            }
            if (!foundMatchingCert) {
                return ResultData.STATUS_READER_AUTHENTICATION_FAILED;
            }
        }

        // Neither user auth nor reader auth required. This means access is always granted.
        return ResultData.STATUS_OK;
    }

    @ResultData.Status
    private int checkAccess(Collection<AccessControlProfileId> accessControlProfileIds,
            Collection<X509Certificate> readerCertChain) {
        // Access is granted if at least one of the profiles grants access.
        //
        // If an item is configured without any profiles, access is denied.
        //
        @ResultData.Status int lastStatus = ResultData.STATUS_NO_ACCESS_CONTROL_PROFILES;

        for (AccessControlProfileId id : accessControlProfileIds) {
            AccessControlProfile profile = mData.getAccessControlProfile(id);
            lastStatus = checkAccessSingleProfile(profile, readerCertChain);
            if (lastStatus == ResultData.STATUS_OK) {
                return lastStatus;
            }
        }
        return lastStatus;
    }

    @Override
    public void setAvailableAuthenticationKeys(int keyCount, int maxUsesPerKey) {
        mData.setAvailableAuthenticationKeys(keyCount, maxUsesPerKey);
    }

    @Override
    public @NonNull
    Collection<X509Certificate> getAuthKeysNeedingCertification() {
        return mData.getAuthKeysNeedingCertification();
    }

    @Override
    public void storeStaticAuthenticationData(@NonNull X509Certificate authenticationKey,
            @NonNull byte[] staticAuthData) throws UnknownAuthenticationKeyException {
        mData.storeStaticAuthenticationData(authenticationKey, staticAuthData);
    }

    @Override
    public @NonNull
    int[] getAuthenticationDataUsageCount() {
        return mData.getAuthKeyUseCounts();
    }
}
