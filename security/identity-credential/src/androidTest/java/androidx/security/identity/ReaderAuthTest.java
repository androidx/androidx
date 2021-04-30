/*
 * Copyright 2021 The Android Open Source Project
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

import static androidx.security.identity.IdentityCredentialStore.CIPHERSUITE_ECDHE_HKDF_ECDSA_WITH_AES_256_GCM_SHA256;
import static androidx.security.identity.ResultData.STATUS_NOT_IN_REQUEST_MESSAGE;
import static androidx.security.identity.ResultData.STATUS_OK;
import static androidx.security.identity.ResultData.STATUS_READER_AUTHENTICATION_FAILED;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import co.nstant.in.cbor.CborBuilder;
import co.nstant.in.cbor.CborException;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class ReaderAuthTest {
    private static final String TAG = "ReaderAuthTest";


    static KeyPair createReaderKey(String readerKeyAlias, boolean createCaKey)
            throws InvalidAlgorithmParameterException, NoSuchProviderException,
            NoSuchAlgorithmException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore");
        KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(
                readerKeyAlias,
                KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY)
                .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512);
        kpg.initialize(builder.build());
        return kpg.generateKeyPair();
    }

    @SuppressWarnings("deprecation")
    @Test
    public void readerAuth()
            throws IdentityCredentialException, CborException, InvalidAlgorithmParameterException,
            KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException,
            NoSuchProviderException, InvalidKeyException, SignatureException {

        // We create two reader keys - 'A' and 'B' - and then generate certificates for each of
        // them, signed by a third key 'C'. We then provision a document with four elements where
        // each element is configured to be accessible only by 'A', 'B', ('A' or 'B'), and 'C'
        // respectively. The names of each element reflect this:
        //
        //  - "Accessible by A"
        //  - "Accessible by B"
        //  - "Accessible by A or B"
        //  - "Accessible by C"
        //
        // We then try reading from the credential in the following cases
        //
        //  - Request signed by A and presenting certChain {certA}
        //    - this should return the following data elements:
        //      - "Accessible by A"
        //      - "Accessible by A or B"
        //
        //  - Request signed by A and presenting certChain {certA_SignedBy_certC, certC}
        //    - this should return the following data elements:
        //      - "Accessible by A"
        //      - "Accessible by A or B"
        //      - "Accessible by C"
        //
        //  - Request signed by B and presenting certChain {certB}
        //    - this should return the following data elements:
        //      - "Accessible by B"
        //      - "Accessible by A or B"
        //
        //  - Request signed by B and presenting certChain {certB_SignedBy_certC, certC}
        //    - this should return the following data elements:
        //      - "Accessible by B"
        //      - "Accessible by A or B"
        //      - "Accessible by C"
        //
        //  - Reader presenting an invalid certificate chain
        //
        // We test all this in the following.
        //


        // Generate keys and certificate chains.
        KeyPair readerKeyPairA = createReaderKey("readerKeyA", false);
        KeyPair readerKeyPairB = createReaderKey("readerKeyB", false);
        KeyPair readerKeyPairC = createReaderKey("readerKeyC", true);

        KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
        ks.load(null);
        X509Certificate certA = (X509Certificate) ks.getCertificate("readerKeyA");
        X509Certificate certB = (X509Certificate) ks.getCertificate("readerKeyB");
        X509Certificate certA_SignedBy_certC = Util.signPublicKeyWithPrivateKey("readerKeyA",
                "readerKeyC");
        X509Certificate certB_SignedBy_certC = Util.signPublicKeyWithPrivateKey("readerKeyB",
                "readerKeyC");
        X509Certificate certC = (X509Certificate) ks.getCertificate("readerKeyC");

        Collection<X509Certificate> certChainForA = new ArrayList<>();
        certChainForA.add(certA);
        Collection<X509Certificate> certChainForAwithC = new ArrayList<>();
        certChainForAwithC.add(certA_SignedBy_certC);
        certChainForAwithC.add(certC);

        Collection<X509Certificate> certChainForB = new ArrayList<>();
        certChainForB.add(certB);
        Collection<X509Certificate> certChainForBwithC = new ArrayList<>();
        certChainForBwithC.add(certB_SignedBy_certC);
        certChainForBwithC.add(certC);

        // Provision the credential.
        Context appContext = androidx.test.InstrumentationRegistry.getTargetContext();
        IdentityCredentialStore store = Util.getIdentityCredentialStore(appContext);

        String credentialName = "readerAuthTestCredential";
        store.deleteCredentialByName(credentialName);
        WritableIdentityCredential wc = store.createCredential(credentialName,
                "org.iso.18013-5.2019.mdl");

        // Profile 0 (reader A authentication)
        AccessControlProfile readerAProfile =
                new AccessControlProfile.Builder(new AccessControlProfileId(0))
                        .setReaderCertificate(certA)
                        .setUserAuthenticationRequired(false)
                        .build();
        // Profile 1 (reader B authentication)
        AccessControlProfile readerBProfile =
                new AccessControlProfile.Builder(new AccessControlProfileId(1))
                        .setReaderCertificate(certB)
                        .setUserAuthenticationRequired(false)
                        .build();
        // Profile 2 (reader C authentication)
        AccessControlProfile readerCProfile =
                new AccessControlProfile.Builder(new AccessControlProfileId(2))
                        .setReaderCertificate(certC)
                        .setUserAuthenticationRequired(false)
                        .build();
        Collection<AccessControlProfileId> idsReaderAuthA =
                new ArrayList<AccessControlProfileId>();
        idsReaderAuthA.add(new AccessControlProfileId(0));
        Collection<AccessControlProfileId> idsReaderAuthB =
                new ArrayList<AccessControlProfileId>();
        idsReaderAuthB.add(new AccessControlProfileId(1));
        Collection<AccessControlProfileId> idsReaderAuthAorB =
                new ArrayList<AccessControlProfileId>();
        idsReaderAuthAorB.add(new AccessControlProfileId(0));
        idsReaderAuthAorB.add(new AccessControlProfileId(1));
        Collection<AccessControlProfileId> idsReaderAuthC =
                new ArrayList<AccessControlProfileId>();
        idsReaderAuthC.add(new AccessControlProfileId(2));
        String mdlNs = "org.iso.18013-5.2019";
        PersonalizationData personalizationData =
                new PersonalizationData.Builder()
                        .addAccessControlProfile(readerAProfile)
                        .addAccessControlProfile(readerBProfile)
                        .addAccessControlProfile(readerCProfile)
                        .putEntry(mdlNs, "Accessible to A", idsReaderAuthA,
                                Util.cborEncodeString("foo"))
                        .putEntry(mdlNs, "Accessible to B", idsReaderAuthB,
                                Util.cborEncodeString("bar"))
                        .putEntry(mdlNs, "Accessible to A or B", idsReaderAuthAorB,
                                Util.cborEncodeString("baz"))
                        .putEntry(mdlNs, "Accessible to C", idsReaderAuthC,
                                Util.cborEncodeString("bat"))
                        .build();
        byte[] proofOfProvisioningSignature = wc.personalize(personalizationData);
        byte[] proofOfProvisioning =
                Util.coseSign1GetData(Util.cborDecode(proofOfProvisioningSignature));

        String pretty = Util.cborPrettyPrint(proofOfProvisioning);
        pretty = Util.replaceLine(pretty, 6, "      'readerCertificate' : [] // Removed");
        pretty = Util.replaceLine(pretty, 10, "      'readerCertificate' : [] // Removed");
        pretty = Util.replaceLine(pretty, 14, "      'readerCertificate' : [] // Removed");
        assertEquals("[\n"
                + "  'ProofOfProvisioning',\n"
                + "  'org.iso.18013-5.2019.mdl',\n"
                + "  [\n"
                + "    {\n"
                + "      'id' : 0,\n"
                + "      'readerCertificate' : [] // Removed\n"
                + "    },\n"
                + "    {\n"
                + "      'id' : 1,\n"
                + "      'readerCertificate' : [] // Removed\n"
                + "    },\n"
                + "    {\n"
                + "      'id' : 2,\n"
                + "      'readerCertificate' : [] // Removed\n"
                + "    }\n"
                + "  ],\n"
                + "  {\n"
                + "    'org.iso.18013-5.2019' : [\n"
                + "      {\n"
                + "        'name' : 'Accessible to A',\n"
                + "        'value' : 'foo',\n"
                + "        'accessControlProfiles' : [0]\n"
                + "      },\n"
                + "      {\n"
                + "        'name' : 'Accessible to B',\n"
                + "        'value' : 'bar',\n"
                + "        'accessControlProfiles' : [1]\n"
                + "      },\n"
                + "      {\n"
                + "        'name' : 'Accessible to A or B',\n"
                + "        'value' : 'baz',\n"
                + "        'accessControlProfiles' : [0, 1]\n"
                + "      },\n"
                + "      {\n"
                + "        'name' : 'Accessible to C',\n"
                + "        'value' : 'bat',\n"
                + "        'accessControlProfiles' : [2]\n"
                + "      }\n"
                + "    ]\n"
                + "  },\n"
                + "  false\n"
                + "]", pretty);


        // Get the credential we'll be reading from and provision it with a sufficient number
        // of dynamic auth keys
        IdentityCredential credential = store.getCredentialByName(credentialName,
                CIPHERSUITE_ECDHE_HKDF_ECDSA_WITH_AES_256_GCM_SHA256);
        assertNotNull(credential);
        credential.setAvailableAuthenticationKeys(1, 10);
        Collection<X509Certificate> dynAuthKeyCerts = credential.getAuthKeysNeedingCertification();
        credential.storeStaticAuthenticationData(dynAuthKeyCerts.iterator().next(), new byte[0]);
        KeyPair eKeyPair = credential.createEphemeralKeyPair();

        Collection<String> entryNames;
        Collection<String> resultNamespaces;
        ResultData rd;

        // Create the request message which will be signed by the reader.
        Map<String, Collection<String>> entriesToRequest = new LinkedHashMap<>();
        entriesToRequest.put("org.iso.18013-5.2019",
                Arrays.asList("Accessible to A",
                        "Accessible to B",
                        "Accessible to A or B",
                        "Accessible to C"));
        byte[] requestMessage = Util.createItemsRequest(entriesToRequest,
                "org.iso.18013-5.2019.mdl");

        // Signing with A and presenting cert chain {certA}.
        //
        rd = retrieveForReader(credential, eKeyPair, readerKeyPairA, certChainForA, requestMessage,
                entriesToRequest);
        resultNamespaces = rd.getNamespaces();
        assertEquals(1, resultNamespaces.size());
        assertEquals("org.iso.18013-5.2019", resultNamespaces.iterator().next());
        entryNames = rd.getRetrievedEntryNames("org.iso.18013-5.2019");
        assertEquals(2, entryNames.size());
        assertTrue(entryNames.contains("Accessible to A"));
        assertTrue(entryNames.contains("Accessible to A or B"));
        assertEquals(STATUS_OK, rd.getStatus("org.iso.18013-5.2019", "Accessible to A"));
        assertEquals(STATUS_OK, rd.getStatus("org.iso.18013-5.2019", "Accessible to A or B"));
        assertEquals(STATUS_READER_AUTHENTICATION_FAILED, rd.getStatus("org.iso.18013-5.2019",
                "Accessible to B"));
        assertEquals(STATUS_READER_AUTHENTICATION_FAILED, rd.getStatus("org.iso.18013-5.2019",
                "Accessible to C"));

        // Signing with A and presenting cert chain {certA} and providing a requestMessage
        // that doesn't request "Accessible to A or B" in the signed |requestMessage| but
        // includes it in |entriesToRequest| (which is not signed)... should result
        // in requesting "Accessible to A or B" failing with NOT_IN_REQUEST_MESSAGE
        // and "Accessible to B" and "Accessible to C" failing with
        // READER_AUTHENTICATION_FAILED.
        //
        Map<String, Collection<String>> entriesToRequest2 = new LinkedHashMap<>();
        entriesToRequest2.put("org.iso.18013-5.2019",
                Arrays.asList("Accessible to A",
                        "Accessible to B",
                        "Accessible to C"));
        byte[] requestMessage2 = Util.createItemsRequest(entriesToRequest2,
                "org.iso.18013-5.2019.mdl");
        credential = store.getCredentialByName(credentialName,
                CIPHERSUITE_ECDHE_HKDF_ECDSA_WITH_AES_256_GCM_SHA256);
        eKeyPair = credential.createEphemeralKeyPair();
        rd = retrieveForReader(credential, eKeyPair, readerKeyPairA, certChainForA,
                requestMessage2, entriesToRequest);
        resultNamespaces = rd.getNamespaces();
        assertEquals(1, resultNamespaces.size());
        assertEquals("org.iso.18013-5.2019", resultNamespaces.iterator().next());
        entryNames = rd.getRetrievedEntryNames("org.iso.18013-5.2019");
        assertEquals(1, entryNames.size());
        assertTrue(entryNames.contains("Accessible to A"));
        assertEquals(STATUS_OK, rd.getStatus("org.iso.18013-5.2019", "Accessible to A"));
        assertEquals(STATUS_NOT_IN_REQUEST_MESSAGE,
                rd.getStatus("org.iso.18013-5.2019", "Accessible to A or B"));
        assertEquals(STATUS_READER_AUTHENTICATION_FAILED,
                rd.getStatus("org.iso.18013-5.2019", "Accessible to B"));
        assertEquals(STATUS_READER_AUTHENTICATION_FAILED,
                rd.getStatus("org.iso.18013-5.2019", "Accessible to C"));

        // Signing with A and presenting cert chain {certA_SignedBy_certC, certC}.
        //
        credential = store.getCredentialByName(credentialName,
                CIPHERSUITE_ECDHE_HKDF_ECDSA_WITH_AES_256_GCM_SHA256);
        eKeyPair = credential.createEphemeralKeyPair();
        rd = retrieveForReader(credential, eKeyPair, readerKeyPairA, certChainForAwithC,
                requestMessage, entriesToRequest);
        resultNamespaces = rd.getNamespaces();
        assertEquals(1, resultNamespaces.size());
        assertEquals("org.iso.18013-5.2019", resultNamespaces.iterator().next());
        entryNames = rd.getRetrievedEntryNames("org.iso.18013-5.2019");
        assertEquals(3, entryNames.size());
        assertTrue(entryNames.contains("Accessible to A"));
        assertTrue(entryNames.contains("Accessible to A or B"));
        assertTrue(entryNames.contains("Accessible to C"));

        // Signing with B and presenting cert chain {certB}.
        //
        credential = store.getCredentialByName(credentialName,
                CIPHERSUITE_ECDHE_HKDF_ECDSA_WITH_AES_256_GCM_SHA256);
        eKeyPair = credential.createEphemeralKeyPair();
        rd = retrieveForReader(credential, eKeyPair, readerKeyPairB, certChainForB, requestMessage,
                entriesToRequest);
        resultNamespaces = rd.getNamespaces();
        assertEquals(1, resultNamespaces.size());
        assertEquals("org.iso.18013-5.2019", resultNamespaces.iterator().next());
        entryNames = rd.getRetrievedEntryNames("org.iso.18013-5.2019");
        assertEquals(2, entryNames.size());
        assertTrue(entryNames.contains("Accessible to B"));
        assertTrue(entryNames.contains("Accessible to A or B"));

        // Signing with B and presenting cert chain {certB_SignedBy_certC, certC}.
        //
        credential = store.getCredentialByName(credentialName,
                CIPHERSUITE_ECDHE_HKDF_ECDSA_WITH_AES_256_GCM_SHA256);
        eKeyPair = credential.createEphemeralKeyPair();
        rd = retrieveForReader(credential, eKeyPair, readerKeyPairB, certChainForBwithC,
                requestMessage, entriesToRequest);
        resultNamespaces = rd.getNamespaces();
        assertEquals(1, resultNamespaces.size());
        assertEquals("org.iso.18013-5.2019", resultNamespaces.iterator().next());
        entryNames = rd.getRetrievedEntryNames("org.iso.18013-5.2019");
        assertEquals(3, entryNames.size());
        assertTrue(entryNames.contains("Accessible to B"));
        assertTrue(entryNames.contains("Accessible to A or B"));
        assertTrue(entryNames.contains("Accessible to C"));

        // Signing with B and presenting invalid cert chain {certB, certC} should fail
        // because certB is not signed by certC.
        try {
            credential = store.getCredentialByName(credentialName,
                    CIPHERSUITE_ECDHE_HKDF_ECDSA_WITH_AES_256_GCM_SHA256);
            eKeyPair = credential.createEphemeralKeyPair();
            Collection<X509Certificate> certChain = new ArrayList<>();
            certChain.add(certB);
            certChain.add(certC);
            retrieveForReader(credential, eKeyPair, readerKeyPairB, certChain, requestMessage,
                    entriesToRequest);
            assertTrue(false);
        } catch (InvalidReaderSignatureException e) {
            // Do nothing, this is the expected exception...
        }

        // No request message should result in returning zero data elements - they're
        // all protected by reader authentication.
        //
        credential = store.getCredentialByName(credentialName,
                CIPHERSUITE_ECDHE_HKDF_ECDSA_WITH_AES_256_GCM_SHA256);
        eKeyPair = credential.createEphemeralKeyPair();
        rd = credential.getEntries(
                null,
                entriesToRequest,
                null);
        resultNamespaces = rd.getNamespaces();
        assertEquals(1, resultNamespaces.size());
        assertEquals("org.iso.18013-5.2019", resultNamespaces.iterator().next());
        entryNames = rd.getRetrievedEntryNames("org.iso.18013-5.2019");
        assertEquals(0, entryNames.size());
        assertEquals(STATUS_READER_AUTHENTICATION_FAILED,
                rd.getStatus("org.iso.18013-5.2019", "Accessible to A"));
        assertEquals(STATUS_READER_AUTHENTICATION_FAILED,
                rd.getStatus("org.iso.18013-5.2019", "Accessible to A or B"));
        assertEquals(STATUS_READER_AUTHENTICATION_FAILED,
                rd.getStatus("org.iso.18013-5.2019", "Accessible to B"));
        assertEquals(STATUS_READER_AUTHENTICATION_FAILED,
                rd.getStatus("org.iso.18013-5.2019", "Accessible to C"));
    }

    private ResultData retrieveForReader(
            IdentityCredential credential,
            KeyPair ephemeralKeyPair,
            KeyPair readerKeyToSignWith,
            Collection<X509Certificate> readerCertificateChainToPresent,
            byte[] requestMessage,
            Map<String, Collection<String>> entriesToRequest)
            throws IdentityCredentialException, CborException, InvalidAlgorithmParameterException,
            KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException,
            NoSuchProviderException, InvalidKeyException, SignatureException {

        byte[] sessionTranscript = Util.buildSessionTranscript(ephemeralKeyPair);

        // Finally, create the structure that the reader signs, and sign it.

        byte[] readerAuthentication = Util.cborEncode(new CborBuilder()
                .addArray()
                .add("ReaderAuthentication")
                .add(Util.cborDecode(sessionTranscript))
                .add(Util.cborBuildTaggedByteString(requestMessage))
                .end()
                .build().get(0));
        byte[] readerAuthenticationBytes =
                Util.cborEncode(Util.cborBuildTaggedByteString((readerAuthentication)));
        byte[] readerSignature = Util.cborEncode(
                Util.coseSign1Sign(readerKeyToSignWith.getPrivate(),
                null, // payload
                readerAuthenticationBytes, // detached content
                readerCertificateChainToPresent)); // certificate-chain

        // Now issue the request.
        credential.setSessionTranscript(sessionTranscript);
        ResultData result = credential.getEntries(
                requestMessage,
                entriesToRequest,
                readerSignature);
        return result;
    }

}
