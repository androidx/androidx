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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import android.content.Context;
import android.icu.util.Calendar;
import android.os.SystemClock;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.crypto.SecretKey;

import co.nstant.in.cbor.CborBuilder;
import co.nstant.in.cbor.CborEncoder;
import co.nstant.in.cbor.CborException;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class DynamicAuthTest {
    private static final String TAG = "DynamicAuthTest";

    @SuppressWarnings("deprecation")
    @Test
    public void checkAuthKey() throws Exception {
        Context appContext = androidx.test.InstrumentationRegistry.getTargetContext();
        IdentityCredentialStore store = Util.getIdentityCredentialStore(appContext);

        String credentialName = "test";

        store.deleteCredentialByName(credentialName);

        WritableIdentityCredential wc = store.createCredential(credentialName,
                "org.iso.18013-5.2019.mdl");

        byte[] challenge = "TheChallenge".getBytes();
        Collection<X509Certificate> certChain = wc.getCredentialKeyCertificateChain(challenge);
        // Profile 0 (no authentication)
        AccessControlProfile noAuthProfile =
                new AccessControlProfile.Builder(new AccessControlProfileId(0))
                        .setUserAuthenticationRequired(false)
                        .build();
        Collection<AccessControlProfileId> idsNoAuth = new ArrayList<AccessControlProfileId>();
        idsNoAuth.add(new AccessControlProfileId(0));
        String mdlNs = "org.iso.18013-5.2019";
        PersonalizationData personalizationData =
                new PersonalizationData.Builder()
                        .addAccessControlProfile(noAuthProfile)
                        .putEntry(mdlNs, "First name", idsNoAuth, Util.cborEncodeString("Alan"))
                        .putEntry(mdlNs, "Last name", idsNoAuth, Util.cborEncodeString("Turing"))
                        .build();
        byte[] proofOfProvisioningSignature = wc.personalize(personalizationData);
        byte[] proofOfProvisioning =
                Util.coseSign1GetData(Util.cborDecode(proofOfProvisioningSignature));
        byte[] proofOfProvisioningSha256 =
                MessageDigest.getInstance("SHA256").digest(proofOfProvisioning);

        IdentityCredential credential = store.getCredentialByName(credentialName,
                IdentityCredentialStore.CIPHERSUITE_ECDHE_HKDF_ECDSA_WITH_AES_256_GCM_SHA256);
        assertNotNull(credential);
        credential.setAvailableAuthenticationKeys(5, 3);
        assertArrayEquals(
                new int[]{0, 0, 0, 0, 0},
                credential.getAuthenticationDataUsageCount());

        Collection<X509Certificate> certificates = null;
        certificates = credential.getAuthKeysNeedingCertification();
        assertEquals(5, certificates.size());

        X509Certificate cert = (X509Certificate) certificates.toArray()[0];

        //  - serialNumber: INTEGER 1 (fixed value: same on all certs).
        assertEquals(1, cert.getSerialNumber().intValue());

        //  - issuer: CN shall be set to "Android Identity Credential Key". (fixed value:
        //    same on all certs)
        assertEquals("CN=Android Identity Credential Key",
                cert.getIssuerX500Principal().getName());

        //  - subject: CN shall be set to "Android Identity Credential Authentication Key". (fixed
        //    value: same on all certs)
        assertEquals("CN=Android Identity Credential Authentication Key",
                cert.getSubjectX500Principal().getName());

        //  - validity: should be from current time and one year in the future (365 days).
        Date now = new Date();

        // Allow for 10 seconds drift to account for the time drift and loss of precision
        // when encoding into ASN.1
        //
        long diffMilliSecs = now.getTime() - cert.getNotBefore().getTime();
        final long allowDriftMilliSecs = 10 * 1000;
        assertTrue(-allowDriftMilliSecs <= diffMilliSecs && diffMilliSecs <= allowDriftMilliSecs);

        final long kMilliSecsInOneYear = 365L * 24 * 60 * 60 * 1000;
        diffMilliSecs =
                cert.getNotBefore().getTime() + kMilliSecsInOneYear - cert.getNotAfter().getTime();
        assertTrue(-allowDriftMilliSecs <= diffMilliSecs && diffMilliSecs <= allowDriftMilliSecs);

        // The extension must be there if the underlying hardware says it
        // supports updating the credential.
        //
        if (store.getCapabilities().isUpdateSupported()) {
            byte[] icExtension = cert.getExtensionValue("1.3.6.1.4.1.11129.2.1.26");
            assertNotNull(icExtension);
            assertArrayEquals(proofOfProvisioningSha256, Util.getPopSha256FromAuthKeyCert(cert));
        }

        // ... and we're done. Clean up after ourselves.
        store.deleteCredentialByName(credentialName);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void dynamicAuthTest() throws Exception {
        Context appContext = androidx.test.InstrumentationRegistry.getTargetContext();
        IdentityCredentialStore store = Util.getIdentityCredentialStore(appContext);

        String credentialName = "test";

        store.deleteCredentialByName(credentialName);
        Collection<X509Certificate> certChain = ProvisioningTest.createCredential(store,
                credentialName);

        IdentityCredential credential = store.getCredentialByName(credentialName,
                IdentityCredentialStore.CIPHERSUITE_ECDHE_HKDF_ECDSA_WITH_AES_256_GCM_SHA256);
        assertNotNull(credential);
        assertArrayEquals(new int[0], credential.getAuthenticationDataUsageCount());

        credential.setAvailableAuthenticationKeys(5, 3);
        assertArrayEquals(
                new int[]{0, 0, 0, 0, 0},
                credential.getAuthenticationDataUsageCount());

        // Getting data without device authentication should work even in the case where we haven't
        // provisioned any authentication keys. Check that.
        Map<String, Collection<String>> entriesToRequest = new LinkedHashMap<>();
        entriesToRequest.put("org.iso.18013-5.2019", Arrays.asList("First name", "Last name"));
        // no setSessionTranscript() call indicates Device Authentication not requested.
        ResultData rd = credential.getEntries(
                Util.createItemsRequest(entriesToRequest, null),
                entriesToRequest,
                null);
        byte[] resultCbor = rd.getAuthenticatedData();
        try {
            String pretty = Util.cborPrettyPrint(Util.canonicalizeCbor(resultCbor));
            assertEquals("{\n"
                            + "  'org.iso.18013-5.2019' : {\n"
                            + "    'Last name' : 'Turing',\n"
                            + "    'First name' : 'Alan'\n"
                            + "  }\n"
                            + "}",
                    pretty);
        } catch (CborException e) {
            e.printStackTrace();
            assertTrue(false);
        }

        KeyPair readerEphemeralKeyPair = Util.createEphemeralKeyPair();

        credential = store.getCredentialByName(credentialName,
                IdentityCredentialStore.CIPHERSUITE_ECDHE_HKDF_ECDSA_WITH_AES_256_GCM_SHA256);
        KeyPair ephemeralKeyPair = credential.createEphemeralKeyPair();
        credential.setReaderEphemeralPublicKey(readerEphemeralKeyPair.getPublic());

        byte[] sessionTranscript = Util.buildSessionTranscript(ephemeralKeyPair);
        // Then check that getEntries() throw NoAuthenticationKeyAvailableException (_even_ when
        // allowing using exhausted keys).
        try {
            credential.setSessionTranscript(sessionTranscript);
            rd = credential.getEntries(
                    Util.createItemsRequest(entriesToRequest, null),
                    entriesToRequest,
                    null);
            assertTrue(false);
        } catch (NoAuthenticationKeyAvailableException e) {
            // This is the expected path...
        } catch (IdentityCredentialException e) {
            e.printStackTrace();
            assertTrue(false);
        }

        // Get auth keys needing certification. This should be all of them. Note that
        // this forces the creation of the authentication keys in the HAL.
        Collection<X509Certificate> certificates = null;
        certificates = credential.getAuthKeysNeedingCertification();
        assertEquals(5, certificates.size());

        // Do it one more time to check that an auth key is still pending even
        // when the corresponding key has been created.
        Collection<X509Certificate> certificates2 = null;
        certificates2 = credential.getAuthKeysNeedingCertification();
        assertArrayEquals(certificates.toArray(), certificates2.toArray());

        // Now set auth data for the *first* key (this is the act of certifying the key) and check
        // that one less key now needs certification.
        X509Certificate key0Cert = certificates.iterator().next();

        // Check key0Cert is signed by CredentialKey.
        try {
            key0Cert.verify(certChain.iterator().next().getPublicKey());
        } catch (CertificateException
                | InvalidKeyException
                | NoSuchAlgorithmException
                | NoSuchProviderException
                | SignatureException e) {
            e.printStackTrace();
            assertTrue(false);
        }

        try {
            credential.storeStaticAuthenticationData(key0Cert, new byte[]{42, 43, 44});
            certificates = credential.getAuthKeysNeedingCertification();
        } catch (IdentityCredentialException e) {
            e.printStackTrace();
            assertTrue(false);
        }
        assertEquals(4, certificates.size());

        // Now certify the *last* key.
        X509Certificate key4Cert = new ArrayList<X509Certificate>(certificates).get(
                certificates.size() - 1);
        try {
            key4Cert.verify(certChain.iterator().next().getPublicKey());
        } catch (CertificateException
                | InvalidKeyException
                | NoSuchAlgorithmException
                | NoSuchProviderException
                | SignatureException e) {
            e.printStackTrace();
            assertTrue(false);
        }

        try {
            credential.storeStaticAuthenticationData(key4Cert, new byte[]{43, 44, 45});
            certificates = credential.getAuthKeysNeedingCertification();
        } catch (IdentityCredentialException e) {
            e.printStackTrace();
            assertTrue(false);
        }
        assertEquals(3, certificates.size());

        // Now that we've provisioned authentication keys, presentation will no longer fail with
        // NoAuthenticationKeyAvailableException ... So now we can try a sessionTranscript without
        // the ephemeral public key that was created in the Secure Area and check it fails with
        // EphemeralPublicKeyNotFoundException instead...
        credential = store.getCredentialByName(credentialName,
                IdentityCredentialStore.CIPHERSUITE_ECDHE_HKDF_ECDSA_WITH_AES_256_GCM_SHA256);
        ByteArrayOutputStream stBaos = new ByteArrayOutputStream();
        try {
            new CborEncoder(stBaos).encode(new CborBuilder()
                    .addArray()
                    .add(new byte[]{0x01, 0x02})  // The device engagement structure, encoded
                    .add(new byte[]{0x03, 0x04})  // Reader ephemeral public key, encoded
                    .end()
                    .build());
        } catch (CborException e) {
            e.printStackTrace();
            assertTrue(false);
        }
        /* TODO
        byte[] wrongSessionTranscript = stBaos.toByteArray();
        try {
            credential.setSessionTranscript(wrongSessionTranscript);
            rd = credential.getEntries(
                    Util.createItemsRequest(entriesToRequest, null),
                    entriesToRequest,
                    null);
            assertTrue(false);
        } catch (EphemeralPublicKeyNotFoundException e) {
            // This is the expected path...
        } catch (IdentityCredentialException e) {
            e.printStackTrace();
            assertTrue(false);
        }
        */

        // Now use one of the keys...
        entriesToRequest = new LinkedHashMap<>();
        entriesToRequest.put("org.iso.18013-5.2019",
                Arrays.asList("First name",
                        "Last name",
                        "Home address",
                        "Birth date",
                        "Cryptanalyst",
                        "Portrait image",
                        "Height"));
        credential = store.getCredentialByName(credentialName,
                IdentityCredentialStore.CIPHERSUITE_ECDHE_HKDF_ECDSA_WITH_AES_256_GCM_SHA256);
        ephemeralKeyPair = credential.createEphemeralKeyPair();
        credential.setReaderEphemeralPublicKey(readerEphemeralKeyPair.getPublic());
        sessionTranscript = Util.buildSessionTranscript(ephemeralKeyPair);
        credential.setSessionTranscript(sessionTranscript);
        rd = credential.getEntries(
                Util.createItemsRequest(entriesToRequest, null),
                entriesToRequest,
                null);
        resultCbor = rd.getAuthenticatedData();
        try {
            String pretty = Util.cborPrettyPrint(Util.canonicalizeCbor(resultCbor));
            assertEquals("{\n"
                            + "  'org.iso.18013-5.2019' : {\n"
                            + "    'Height' : 180,\n"
                            + "    'Last name' : 'Turing',\n"
                            + "    'Birth date' : '19120623',\n"
                            + "    'First name' : 'Alan',\n"
                            + "    'Cryptanalyst' : true,\n"
                            + "    'Home address' : 'Maida Vale, London, England',\n"
                            + "    'Portrait image' : [0x01, 0x02]\n"
                            + "  }\n"
                            + "}",
                    pretty);
        } catch (CborException e) {
            e.printStackTrace();
            assertTrue(false);
        }

        byte[] deviceAuthentication = Util.cborEncode(new CborBuilder()
                .addArray()
                .add("DeviceAuthentication")
                .add(Util.cborDecode(sessionTranscript))
                .add("org.iso.18013-5.2019.mdl")
                .add(Util.cborBuildTaggedByteString(resultCbor))
                .end()
                .build().get(0));

        byte[] deviceAuthenticationBytes =
                Util.cborEncode(Util.cborBuildTaggedByteString((deviceAuthentication)));

        byte[] mac = rd.getMessageAuthenticationCode();
        byte[] signature = rd.getEcdsaSignature();
        assertTrue(mac != null || signature != null);
        if (mac != null) {
            // Calculate the MAC by deriving the key using ECDH and HKDF.
            SecretKey eMacKey = Util.calcEMacKeyForReader(
                    key0Cert.getPublicKey(),
                    readerEphemeralKeyPair.getPrivate(),
                    sessionTranscript);
            byte[] expectedMac = Util.cborEncode(Util.coseMac0(
                    eMacKey,
                    new byte[0],                  // payload
                    deviceAuthenticationBytes));  // detached content

            // Then compare it with what the TA produced.
            assertArrayEquals(expectedMac, mac);
        } else {
            assertTrue(Util.coseSign1CheckSignature(
                    Util.cborDecode(signature),
                    deviceAuthenticationBytes,
                    key0Cert.getPublicKey()));
        }

        // Check that key0's static auth data is returned and that this
        // key has an increased use-count.
        assertArrayEquals(new byte[]{42, 43, 44}, rd.getStaticAuthenticationData());
        assertArrayEquals(new int[]{1, 0, 0, 0, 0}, credential.getAuthenticationDataUsageCount());


        // Now do this one more time.... this time key4 should have been used. Check this by
        // inspecting use-counts and the static authentication data.
        credential = store.getCredentialByName(credentialName,
                IdentityCredentialStore.CIPHERSUITE_ECDHE_HKDF_ECDSA_WITH_AES_256_GCM_SHA256);
        ephemeralKeyPair = credential.createEphemeralKeyPair();
        credential.setReaderEphemeralPublicKey(readerEphemeralKeyPair.getPublic());
        sessionTranscript = Util.buildSessionTranscript(ephemeralKeyPair);
        credential.setSessionTranscript(sessionTranscript);
        rd = credential.getEntries(
                Util.createItemsRequest(entriesToRequest, null),
                entriesToRequest,
                null);
        assertArrayEquals(new byte[]{43, 44, 45}, rd.getStaticAuthenticationData());
        assertArrayEquals(new int[]{1, 0, 0, 0, 1}, credential.getAuthenticationDataUsageCount());

        deviceAuthentication = Util.cborEncode(new CborBuilder()
                .addArray()
                .add("DeviceAuthentication")
                .add(Util.cborDecode(sessionTranscript))
                .add("org.iso.18013-5.2019.mdl")
                .add(Util.cborBuildTaggedByteString(resultCbor))
                .end()
                .build().get(0));

        deviceAuthenticationBytes =
                Util.cborEncode(Util.cborBuildTaggedByteString((deviceAuthentication)));

        // Verify Signature or MAC was made with key4.
        mac = rd.getMessageAuthenticationCode();
        signature = rd.getEcdsaSignature();
        assertTrue(mac != null || signature != null);
        if (mac != null) {
            SecretKey eMacKey = Util.calcEMacKeyForReader(
                    key4Cert.getPublicKey(),
                    readerEphemeralKeyPair.getPrivate(),
                    sessionTranscript);
            byte[] expectedMac = Util.cborEncode(Util.coseMac0(eMacKey,
                    new byte[0],                 // payload
                    deviceAuthenticationBytes));  // detached content
            assertArrayEquals(expectedMac, mac);
        } else {
            assertTrue(Util.coseSign1CheckSignature(
                    Util.cborDecode(signature),
                    deviceAuthenticationBytes,
                    key4Cert.getPublicKey()));
        }

        // And again.... this time key0 should have been used. Check it.
        credential = store.getCredentialByName(credentialName,
                IdentityCredentialStore.CIPHERSUITE_ECDHE_HKDF_ECDSA_WITH_AES_256_GCM_SHA256);
        ephemeralKeyPair = credential.createEphemeralKeyPair();
        credential.setReaderEphemeralPublicKey(readerEphemeralKeyPair.getPublic());
        sessionTranscript = Util.buildSessionTranscript(ephemeralKeyPair);
        credential.setSessionTranscript(sessionTranscript);
        rd = credential.getEntries(
                Util.createItemsRequest(entriesToRequest, null),
                entriesToRequest,
                null);
        assertArrayEquals(new byte[]{42, 43, 44}, rd.getStaticAuthenticationData());
        assertArrayEquals(new int[]{2, 0, 0, 0, 1}, credential.getAuthenticationDataUsageCount());

        // And again.... this time key4 should have been used. Check it.
        credential = store.getCredentialByName(credentialName,
                IdentityCredentialStore.CIPHERSUITE_ECDHE_HKDF_ECDSA_WITH_AES_256_GCM_SHA256);
        ephemeralKeyPair = credential.createEphemeralKeyPair();
        credential.setReaderEphemeralPublicKey(readerEphemeralKeyPair.getPublic());
        sessionTranscript = Util.buildSessionTranscript(ephemeralKeyPair);
        credential.setSessionTranscript(sessionTranscript);
        rd = credential.getEntries(
                Util.createItemsRequest(entriesToRequest, null),
                entriesToRequest,
                null);
        assertArrayEquals(new byte[]{43, 44, 45}, rd.getStaticAuthenticationData());
        assertArrayEquals(new int[]{2, 0, 0, 0, 2}, credential.getAuthenticationDataUsageCount());

        // We configured each key to have three uses only. So we have two more presentations
        // to go until we run out... first, check that only three keys need certifications
        certificates = credential.getAuthKeysNeedingCertification();
        assertEquals(3, certificates.size());

        // Then exhaust the two we've already configured.
        for (int n = 0; n < 2; n++) {
            credential = store.getCredentialByName(credentialName,
                IdentityCredentialStore.CIPHERSUITE_ECDHE_HKDF_ECDSA_WITH_AES_256_GCM_SHA256);
            ephemeralKeyPair = credential.createEphemeralKeyPair();
            credential.setReaderEphemeralPublicKey(readerEphemeralKeyPair.getPublic());
            sessionTranscript = Util.buildSessionTranscript(ephemeralKeyPair);
            credential.setSessionTranscript(sessionTranscript);
            rd = credential.getEntries(
                    Util.createItemsRequest(entriesToRequest, null),
                    entriesToRequest,
                    null);
            assertNotNull(rd);
        }
        assertArrayEquals(new int[]{3, 0, 0, 0, 3}, credential.getAuthenticationDataUsageCount());

        // Now we should have five certs needing certification.
        certificates = credential.getAuthKeysNeedingCertification();
        assertEquals(5, certificates.size());

        // We still have the two keys which have been exhausted.
        assertArrayEquals(new int[]{3, 0, 0, 0, 3}, credential.getAuthenticationDataUsageCount());

        // Check that we fail when running out of presentations (and explicitly don't allow
        // exhausting keys).
        try {
            credential = store.getCredentialByName(credentialName,
                IdentityCredentialStore.CIPHERSUITE_ECDHE_HKDF_ECDSA_WITH_AES_256_GCM_SHA256);
            ephemeralKeyPair = credential.createEphemeralKeyPair();
            credential.setReaderEphemeralPublicKey(readerEphemeralKeyPair.getPublic());
            sessionTranscript = Util.buildSessionTranscript(ephemeralKeyPair);
            credential.setAllowUsingExhaustedKeys(false);
            credential.setSessionTranscript(sessionTranscript);
            rd = credential.getEntries(
                    Util.createItemsRequest(entriesToRequest, null),
                    entriesToRequest,
                    null);
            assertTrue(false);
        } catch (IdentityCredentialException e) {
            assertTrue(e instanceof NoAuthenticationKeyAvailableException);
        }
        assertArrayEquals(new int[]{3, 0, 0, 0, 3}, credential.getAuthenticationDataUsageCount());

        // Now try with allowing using auth keys already exhausted... this should work!
        try {
            credential = store.getCredentialByName(credentialName,
                IdentityCredentialStore.CIPHERSUITE_ECDHE_HKDF_ECDSA_WITH_AES_256_GCM_SHA256);
            ephemeralKeyPair = credential.createEphemeralKeyPair();
            credential.setReaderEphemeralPublicKey(readerEphemeralKeyPair.getPublic());
            sessionTranscript = Util.buildSessionTranscript(ephemeralKeyPair);
            credential.setSessionTranscript(sessionTranscript);
            rd = credential.getEntries(
                    Util.createItemsRequest(entriesToRequest, null),
                    entriesToRequest,
                    null);
        } catch (IdentityCredentialException e) {
            e.printStackTrace();
            assertTrue(false);
        }
        assertArrayEquals(new int[]{4, 0, 0, 0, 3}, credential.getAuthenticationDataUsageCount());

        // Check that replenishing works...
        certificates = credential.getAuthKeysNeedingCertification();
        assertEquals(5, certificates.size());
        X509Certificate keyNewCert = certificates.iterator().next();
        try {
            credential.storeStaticAuthenticationData(keyNewCert, new byte[]{10, 11, 12});
            certificates = credential.getAuthKeysNeedingCertification();
        } catch (IdentityCredentialException e) {
            e.printStackTrace();
            assertTrue(false);
        }
        assertEquals(4, certificates.size());
        assertArrayEquals(new int[]{0, 0, 0, 0, 3}, credential.getAuthenticationDataUsageCount());
        credential = store.getCredentialByName(credentialName,
                IdentityCredentialStore.CIPHERSUITE_ECDHE_HKDF_ECDSA_WITH_AES_256_GCM_SHA256);
        ephemeralKeyPair = credential.createEphemeralKeyPair();
        credential.setReaderEphemeralPublicKey(readerEphemeralKeyPair.getPublic());
        sessionTranscript = Util.buildSessionTranscript(ephemeralKeyPair);
        credential.setSessionTranscript(sessionTranscript);
        rd = credential.getEntries(
                Util.createItemsRequest(entriesToRequest, null),
                entriesToRequest,
                null);
        assertArrayEquals(new byte[]{10, 11, 12}, rd.getStaticAuthenticationData());
        assertArrayEquals(new int[]{1, 0, 0, 0, 3}, credential.getAuthenticationDataUsageCount());

        deviceAuthentication = Util.cborEncode(new CborBuilder()
                .addArray()
                .add("DeviceAuthentication")
                .add(Util.cborDecode(sessionTranscript))
                .add("org.iso.18013-5.2019.mdl")
                .add(Util.cborBuildTaggedByteString(resultCbor))
                .end()
                .build().get(0));

        deviceAuthenticationBytes =
                Util.cborEncode(Util.cborBuildTaggedByteString((deviceAuthentication)));

        mac = rd.getMessageAuthenticationCode();
        signature = rd.getEcdsaSignature();
        assertTrue(mac != null || signature != null);
        if (mac != null) {
            SecretKey eMacKey = Util.calcEMacKeyForReader(
                    keyNewCert.getPublicKey(),
                    readerEphemeralKeyPair.getPrivate(),
                    sessionTranscript);
            byte[] expectedMac = Util.cborEncode(Util.coseMac0(eMacKey,
                    new byte[0],                  // payload
                    deviceAuthenticationBytes));  // detached content
            assertArrayEquals(expectedMac, rd.getMessageAuthenticationCode());
        } else {
            assertTrue(Util.coseSign1CheckSignature(
                    Util.cborDecode(signature),
                    deviceAuthenticationBytes,
                    keyNewCert.getPublicKey()));
        }

        // ... and we're done. Clean up after ourselves.
        store.deleteCredentialByName(credentialName);
    }

    // TODO: test storeStaticAuthenticationData() throwing UnknownAuthenticationKeyException
    // on an unknown auth key

    @SuppressWarnings("deprecation")
    @Test
    public void dynamicAuthWithExpirationTest() throws Exception {
        Context appContext = androidx.test.InstrumentationRegistry.getTargetContext();
        IdentityCredentialStore store = Util.getIdentityCredentialStore(appContext);
        assumeTrue(store.getCapabilities().isStaticAuthenticationDataExpirationSupported());

        String credentialName = "test";

        store.deleteCredentialByName(credentialName);
        Collection<X509Certificate> certChain = ProvisioningTest.createCredential(store,
                credentialName);

        IdentityCredential credential = store.getCredentialByName(credentialName,
                IdentityCredentialStore.CIPHERSUITE_ECDHE_HKDF_ECDSA_WITH_AES_256_GCM_SHA256);
        assertNotNull(credential);

        credential.setAvailableAuthenticationKeys(3, 5);

        Collection<X509Certificate> certificates = null;
        certificates = credential.getAuthKeysNeedingCertification();
        assertEquals(3, certificates.size());

        // Endorse an auth-key but set expiration to 10 seconds in the future.
        //
        Calendar now = Calendar.getInstance();
        Calendar tenSecondsFromNow = Calendar.getInstance();
        tenSecondsFromNow.add(Calendar.SECOND, 10);
        try {
            X509Certificate key0Cert = certificates.iterator().next();
            credential.storeStaticAuthenticationData(key0Cert,
                    tenSecondsFromNow,
                    new byte[]{52, 53, 44});
            certificates = credential.getAuthKeysNeedingCertification();
        } catch (IdentityCredentialException e) {
            e.printStackTrace();
            assertTrue(false);
        }
        assertEquals(2, certificates.size());
        assertArrayEquals(
                new int[]{0, 0, 0},
                credential.getAuthenticationDataUsageCount());
        // Check that presentation works.
        try {
            IdentityCredential tc = store.getCredentialByName(credentialName,
                    IdentityCredentialStore.CIPHERSUITE_ECDHE_HKDF_ECDSA_WITH_AES_256_GCM_SHA256);
            KeyPair ekp = tc.createEphemeralKeyPair();
            KeyPair rekp = Util.createEphemeralKeyPair();
            tc.setReaderEphemeralPublicKey(rekp.getPublic());
            tc.setSessionTranscript(Util.buildSessionTranscript(ekp));
            Map<String, Collection<String>> etr = new LinkedHashMap<>();
            etr.put("org.iso.18013-5.2019", Arrays.asList("First name", "Last name"));
            ResultData rd = tc.getEntries(
                    Util.createItemsRequest(etr, null),
                    etr,
                    null);
        } catch (IdentityCredentialException e) {
            e.printStackTrace();
            assertTrue(false);
        }
        credential = store.getCredentialByName(credentialName,
                IdentityCredentialStore.CIPHERSUITE_ECDHE_HKDF_ECDSA_WITH_AES_256_GCM_SHA256);
        assertArrayEquals(
                new int[]{1, 0, 0},
                credential.getAuthenticationDataUsageCount());

        SystemClock.sleep(11 * 1000);

        certificates = credential.getAuthKeysNeedingCertification();
        assertEquals(3, certificates.size());

        // Check that presentation now fails..
        try {
            IdentityCredential tc = store.getCredentialByName(credentialName,
                    IdentityCredentialStore.CIPHERSUITE_ECDHE_HKDF_ECDSA_WITH_AES_256_GCM_SHA256);
            KeyPair ekp = tc.createEphemeralKeyPair();
            KeyPair rekp = Util.createEphemeralKeyPair();
            tc.setReaderEphemeralPublicKey(rekp.getPublic());
            tc.setSessionTranscript(Util.buildSessionTranscript(ekp));
            Map<String, Collection<String>> etr = new LinkedHashMap<>();
            etr.put("org.iso.18013-5.2019", Arrays.asList("First name", "Last name"));
            ResultData rd = tc.getEntries(
                    Util.createItemsRequest(etr, null),
                    etr,
                    null);
            assertTrue(false);
        } catch (NoAuthenticationKeyAvailableException e) {
            // This is the expected path...
        } catch (IdentityCredentialException e) {
            e.printStackTrace();
            assertTrue(false);
        }
        credential = store.getCredentialByName(credentialName,
                IdentityCredentialStore.CIPHERSUITE_ECDHE_HKDF_ECDSA_WITH_AES_256_GCM_SHA256);
        assertArrayEquals(
                new int[]{1, 0, 0},
                credential.getAuthenticationDataUsageCount());

        // Check that it works if we use setAllowUsingExpiredKeys(true)
        try {
            IdentityCredential tc = store.getCredentialByName(credentialName,
                    IdentityCredentialStore.CIPHERSUITE_ECDHE_HKDF_ECDSA_WITH_AES_256_GCM_SHA256);
            tc.setAllowUsingExpiredKeys(true);   // <-- this is the call that makes the difference!
            KeyPair ekp = tc.createEphemeralKeyPair();
            KeyPair rekp = Util.createEphemeralKeyPair();
            tc.setReaderEphemeralPublicKey(rekp.getPublic());
            tc.setSessionTranscript(Util.buildSessionTranscript(ekp));
            Map<String, Collection<String>> etr = new LinkedHashMap<>();
            etr.put("org.iso.18013-5.2019", Arrays.asList("First name", "Last name"));
            ResultData rd = tc.getEntries(
                    Util.createItemsRequest(etr, null),
                    etr,
                    null);
        } catch (IdentityCredentialException e) {
            e.printStackTrace();
            assertTrue(false);
        }
        credential = store.getCredentialByName(credentialName,
                IdentityCredentialStore.CIPHERSUITE_ECDHE_HKDF_ECDSA_WITH_AES_256_GCM_SHA256);
        assertArrayEquals(
                new int[]{2, 0, 0},
                credential.getAuthenticationDataUsageCount());

        // ... and we're done. Clean up after ourselves.
        store.deleteCredentialByName(credentialName);
    }

}
