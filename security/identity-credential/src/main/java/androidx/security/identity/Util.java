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
import android.icu.text.DateFormat;
import android.icu.text.SimpleDateFormat;
import android.icu.util.Calendar;
import android.icu.util.GregorianCalendar;
import android.icu.util.TimeZone;
import android.security.keystore.KeyProperties;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECPoint;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;

import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import co.nstant.in.cbor.CborBuilder;
import co.nstant.in.cbor.CborDecoder;
import co.nstant.in.cbor.CborEncoder;
import co.nstant.in.cbor.CborException;
import co.nstant.in.cbor.builder.ArrayBuilder;
import co.nstant.in.cbor.builder.MapBuilder;
import co.nstant.in.cbor.model.AbstractFloat;
import co.nstant.in.cbor.model.Array;
import co.nstant.in.cbor.model.ByteString;
import co.nstant.in.cbor.model.DataItem;
import co.nstant.in.cbor.model.DoublePrecisionFloat;
import co.nstant.in.cbor.model.MajorType;
import co.nstant.in.cbor.model.Map;
import co.nstant.in.cbor.model.NegativeInteger;
import co.nstant.in.cbor.model.Number;
import co.nstant.in.cbor.model.SimpleValue;
import co.nstant.in.cbor.model.SimpleValueType;
import co.nstant.in.cbor.model.SpecialType;
import co.nstant.in.cbor.model.UnicodeString;
import co.nstant.in.cbor.model.UnsignedInteger;

/**
 * @hide
 */
class Util {
    private static final String TAG = "Util";

    // Not called.
    private Util() {}

    public static byte[] fromHex(String stringWithHex) {
        int stringLength = stringWithHex.length();
        if ((stringLength & 1) != 0) {
            throw new IllegalArgumentException("Invalid length of hex string");
        }
        int numBytes = stringLength / 2;
        byte[] data = new byte[numBytes];
        for (int n = 0; n < numBytes; n++) {
            data[n] = (byte) ((Character.digit(stringWithHex.charAt(n * 2), 16) << 4)
                    + Character.digit(stringWithHex.charAt(n * 2 + 1), 16));
        }
        return data;
    }

    public static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int n = 0; n < bytes.length; n++) {
            byte b = bytes[n];
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    static void hexdump(String name, byte[] data) {
        int n, m, o;
        StringBuilder sb = new StringBuilder();
        Formatter fmt = new Formatter(sb);
        for (n = 0; n < data.length; n += 16) {
            fmt.format("%04x  ", n);
            for (m = 0; m < 16 && n + m < data.length; m++) {
                fmt.format("%02x ", data[n + m]);
            }
            for (o = m; o < 16; o++) {
                sb.append("   ");
            }
            sb.append(" ");
            for (m = 0; m < 16 && n + m < data.length; m++) {
                int c = data[n + m] & 0xff;
                fmt.format("%c", Character.isISOControl(c) ? '.' : c);
            }
            sb.append("\n");
        }
        sb.append("\n");
        Log.e(TAG, name + ": dumping " + data.length + " bytes\n" + fmt.toString());
    }


    public static byte[] cborEncode(DataItem dataItem) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            new CborEncoder(baos).encode(dataItem);
        } catch (CborException e) {
            // This should never happen and we don't want cborEncode() to throw since that
            // would complicate all callers. Log it instead.
            throw new IllegalStateException("Unexpected failure encoding data", e);
        }
        return baos.toByteArray();
    }

    static byte[] cborEncodeBoolean(boolean value) {
        return cborEncode(new CborBuilder().add(value).build().get(0));
    }

    static byte[] cborEncodeString(@NonNull String value) {
        return cborEncode(new CborBuilder().add(value).build().get(0));
    }

    static byte[] cborEncodeNumber(long value) {
        return cborEncode(new CborBuilder().add(value).build().get(0));
    }

    static byte[] cborEncodeBytestring(@NonNull byte[] value) {
        return cborEncode(new CborBuilder().add(value).build().get(0));
    }

    static byte[] cborEncodeDateTime(@NonNull Calendar calendar) {
        return cborEncode(cborBuildDateTime(calendar));
    }

    // Returns #6.0(tstr) where tstr is the ISO 8601 encoding of the
    // given point in time.
    //
    public static DataItem cborBuildDateTime(@NonNull Calendar calendar) {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        if (calendar.isSet(Calendar.MILLISECOND) && calendar.get(Calendar.MILLISECOND) != 0) {
            df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        }
        df.setTimeZone(calendar.getTimeZone());
        Date val = calendar.getTime();
        String dateString = df.format(val);
        DataItem dataItem = new UnicodeString(dateString);
        dataItem.setTag(0);
        return dataItem;
    }

    public static DataItem cborDecode(byte[] encodedBytes) {
        ByteArrayInputStream bais = new ByteArrayInputStream(encodedBytes);
        List<DataItem> dataItems = null;
        try {
            dataItems = new CborDecoder(bais).decode();
        } catch (CborException e) {
            throw new IllegalArgumentException("Error decoding CBOR", e);
        }
        if (dataItems.size() != 1) {
            throw new IllegalArgumentException("Unexpected number of items, expected 1 got "
                    + dataItems.size());
        }
        return dataItems.get(0);
    }

    static boolean cborDecodeBoolean(@NonNull byte[] data) {
        SimpleValue simple = (SimpleValue) cborDecode(data);
        return simple.getSimpleValueType() == SimpleValueType.TRUE;
    }

    static String cborDecodeString(@NonNull byte[] data) {
        return ((co.nstant.in.cbor.model.UnicodeString) cborDecode(data)).getString();
    }

    static long cborDecodeLong(@NonNull byte[] data) {
        return ((co.nstant.in.cbor.model.Number) cborDecode(data)).getValue().longValue();
    }

    static byte[] cborDecodeByteString(@NonNull byte[] data) {
        return ((co.nstant.in.cbor.model.ByteString) cborDecode(data)).getBytes();
    }

    static Calendar cborDecodeDateTime(@NonNull byte[] data) {
        DataItem di = cborDecode(data);
        if (!(di instanceof co.nstant.in.cbor.model.UnicodeString)) {
            throw new IllegalArgumentException("Passed in data is not a Unicode-string");
        }
        if (!di.hasTag() || di.getTag().getValue() != 0) {
            throw new IllegalArgumentException("Passed in data is not tagged with tag 0");
        }
        String dateString = ((co.nstant.in.cbor.model.UnicodeString) di).getString();

        // Manually parse the timezone
        TimeZone parsedTz = TimeZone.getTimeZone("UTC");
        java.util.TimeZone parsedTz2 = java.util.TimeZone.getTimeZone("UTC");
        if (!dateString.endsWith("Z")) {
            String timeZoneSubstr = dateString.substring(dateString.length() - 6);
            parsedTz = TimeZone.getTimeZone("GMT" + timeZoneSubstr);
            parsedTz2 = java.util.TimeZone.getTimeZone("GMT" + timeZoneSubstr);
        }

        java.text.DateFormat df = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS",
                Locale.US);
        df.setTimeZone(parsedTz2);
        Date date = null;
        try {
            date = df.parse(dateString);
        } catch (ParseException e) {
            // Try again, this time without the milliseconds
            df = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            df.setTimeZone(parsedTz2);
            try {
                date = df.parse(dateString);
            } catch (ParseException e2) {
                throw new RuntimeException("Error parsing string", e2);
            }
        }

        Calendar c = new GregorianCalendar();
        c.clear();
        c.setTimeZone(parsedTz);
        c.setTime(date);
        return c;
    }

    static DataItem namespaceDataToCbor(PersonalizationData.NamespaceData entryNamespace) {
        CborBuilder entryBuilder = new CborBuilder();
        ArrayBuilder<CborBuilder> entryArrayBuilder = entryBuilder.addArray();
        for (String entryName : entryNamespace.getEntryNames()) {
            byte[] entryValue = entryNamespace.getEntryValue(entryName);
            Collection<AccessControlProfileId> accessControlProfileIds =
                    entryNamespace.getAccessControlProfileIds(
                            entryName);

            CborBuilder accessControlProfileIdsBuilder = new CborBuilder();
            ArrayBuilder<CborBuilder> accessControlProfileIdsArrayBuilder =
                    accessControlProfileIdsBuilder.addArray();
            for (AccessControlProfileId id : accessControlProfileIds) {
                accessControlProfileIdsArrayBuilder.add(id.getId());
            }

            MapBuilder<ArrayBuilder<CborBuilder>> entryMapBuilder = entryArrayBuilder.addMap();
            entryMapBuilder.put("name", entryName);
            entryMapBuilder.put(new UnicodeString("accessControlProfiles"),
                    accessControlProfileIdsBuilder.build().get(0));
            entryMapBuilder.put(new UnicodeString("value"), cborDecode(entryValue));
        }
        return entryBuilder.build().get(0);
    }

    public static PersonalizationData.NamespaceData namespaceDataFromCbor(String namespaceName,
            DataItem dataItem) {
        if (!(dataItem instanceof Array)) {
            throw new IllegalArgumentException("Item is not an Array");
        }
        Array array = (Array) dataItem;

        PersonalizationData.Builder builder = new PersonalizationData.Builder();

        for (DataItem item : array.getDataItems()) {
            if (!(item instanceof co.nstant.in.cbor.model.Map)) {
                throw new IllegalArgumentException("Item is not a map");
            }
            co.nstant.in.cbor.model.Map map = (co.nstant.in.cbor.model.Map) item;

            String name = ((UnicodeString) map.get(new UnicodeString("name"))).getString();

            Collection<AccessControlProfileId> accessControlProfileIds = new ArrayList<>();
            co.nstant.in.cbor.model.Array accessControlProfileArray =
                    (co.nstant.in.cbor.model.Array) map.get(
                            new UnicodeString("accessControlProfiles"));
            for (DataItem acpIdItem : accessControlProfileArray.getDataItems()) {
                accessControlProfileIds.add(
                        new AccessControlProfileId(((Number) acpIdItem).getValue().intValue()));
            }

            DataItem cborValue = map.get(new UnicodeString("value"));
            byte[] data = cborEncode(cborValue);
            builder.putEntry(namespaceName, name, accessControlProfileIds, data);
        }

        return builder.build().getNamespaceData(namespaceName);
    }

    public static AccessControlProfile accessControlProfileFromCbor(DataItem item) {
        if (!(item instanceof co.nstant.in.cbor.model.Map)) {
            throw new IllegalArgumentException("Item is not a map");
        }
        Map map = (Map) item;

        int accessControlProfileId = ((Number) map.get(
                new UnicodeString("id"))).getValue().intValue();
        AccessControlProfile.Builder builder = new AccessControlProfile.Builder(
                new AccessControlProfileId(accessControlProfileId));

        item = map.get(new UnicodeString("readerCertificate"));
        if (item != null) {
            byte[] rcBytes = ((ByteString) item).getBytes();
            CertificateFactory certFactory = null;
            try {
                certFactory = CertificateFactory.getInstance("X.509");
                builder.setReaderCertificate((X509Certificate) certFactory.generateCertificate(
                        new ByteArrayInputStream(rcBytes)));
            } catch (CertificateException e) {
                throw new IllegalArgumentException("Error decoding readerCertificate", e);
            }
        }

        builder.setUserAuthenticationRequired(false);
        item = map.get(new UnicodeString("capabilityType"));
        if (item != null) {
            // TODO: deal with -1 as per entryNamespaceToCbor()
            builder.setUserAuthenticationRequired(true);
            item = map.get(new UnicodeString("timeout"));
            builder.setUserAuthenticationTimeout(
                    item == null ? 0 : ((Number) item).getValue().intValue());
        }
        return builder.build();
    }

    static DataItem accessControlProfileToCbor(AccessControlProfile accessControlProfile) {
        CborBuilder cborBuilder = new CborBuilder();
        MapBuilder<CborBuilder> mapBuilder = cborBuilder.addMap();

        mapBuilder.put("id", accessControlProfile.getAccessControlProfileId().getId());
        X509Certificate readerCertificate = accessControlProfile.getReaderCertificate();
        if (readerCertificate != null) {
            try {
                mapBuilder.put("readerCertificate", readerCertificate.getEncoded());
            } catch (CertificateEncodingException e) {
                throw new IllegalStateException("Error encoding reader mCertificate", e);
            }
        }
        if (accessControlProfile.isUserAuthenticationRequired()) {
            mapBuilder.put("capabilityType", 1); // TODO: what value to put here?
            long timeout = accessControlProfile.getUserAuthenticationTimeout();
            if (timeout != 0) {
                mapBuilder.put("timeout", timeout);
            }
        }
        return cborBuilder.build().get(0);
    }

    static @NonNull X509Certificate generateAuthenticationKeyCert(String authKeyAlias,
            String credentialKeyAlias,
            byte[] proofOfProvisioningSha256) {
        KeyStore ks = null;
        try {
            ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);

            X509Certificate selfSignedCert = (X509Certificate) ks.getCertificate(authKeyAlias);
            PublicKey publicKey = selfSignedCert.getPublicKey();

            PrivateKey privateKey = ((KeyStore.PrivateKeyEntry) ks.getEntry(credentialKeyAlias,
                    null)).getPrivateKey();

            X500Name issuer = new X500Name("CN=Android Identity Credential Key");
            X500Name subject = new X500Name("CN=Android Identity Credential Authentication Key");

            Date now = new Date();
            final long kMilliSecsInOneYear = 365L * 24 * 60 * 60 * 1000;
            Date expirationDate = new Date(now.getTime() + kMilliSecsInOneYear);
            BigInteger serial = new BigInteger("1");
            JcaX509v3CertificateBuilder builder =
                    new JcaX509v3CertificateBuilder(issuer,
                            serial,
                            now,
                            expirationDate,
                            subject,
                            publicKey);

            if (proofOfProvisioningSha256 != null) {
                byte[] encodedProofOfBinding = cborEncode(new CborBuilder()
                        .addArray()
                        .add("ProofOfBinding")
                        .add(proofOfProvisioningSha256)
                        .end()
                        .build().get(0));
                builder.addExtension(new ASN1ObjectIdentifier("1.3.6.1.4.1.11129.2.1.26"), false,
                        encodedProofOfBinding);
            }

            ContentSigner signer = new JcaContentSignerBuilder("SHA256withECDSA").build(privateKey);
            byte[] encodedCert = builder.build(signer).getEncoded();

            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            ByteArrayInputStream bais = new ByteArrayInputStream(encodedCert);
            X509Certificate result = (X509Certificate) cf.generateCertificate(bais);
            return result;
        } catch (Exception e) {
            throw new IllegalStateException("Error signing public key with private key", e);
        }
    }

    /**
     * Helper function to check if a given certificate chain is valid.
     *
     * NOTE NOTE NOTE: We only check that the certificates in the chain sign each other. We
     * <em>specifically</em> don't check that each certificate is also a CA certificate.
     *
     * @param certificateChain the chain to validate.
     * @return <code>true</code> if valid, <code>false</code> otherwise.
     */
    public static boolean validateCertificateChain(Collection<X509Certificate> certificateChain) {
        // First check that each certificate signs the previous one...
        X509Certificate prevCertificate = null;
        for (X509Certificate certificate : certificateChain) {
            if (prevCertificate != null) {
                // We're not the leaf certificate...
                //
                // Check the previous certificate was signed by this one.
                try {
                    prevCertificate.verify(certificate.getPublicKey());
                } catch (CertificateException
                        | InvalidKeyException
                        | NoSuchAlgorithmException
                        | NoSuchProviderException
                        | SignatureException e) {
                    return false;
                }
            } else {
                // we're the leaf certificate so we're not signing anything nor
                // do we need to be e.g. a CA certificate.
            }
            prevCertificate = certificate;
        }
        return true;
    }

    /**
     * Computes an HKDF.
     *
     * This is based on https://github.com/google/tink/blob/master/java/src/main/java/com/google
     * /crypto/tink/subtle/Hkdf.java
     * which is also Copyright (c) Google and also licensed under the Apache 2 license.
     *
     * @param macAlgorithm the MAC algorithm used for computing the Hkdf. I.e., "HMACSHA1" or
     *                     "HMACSHA256".
     * @param ikm          the input keying material.
     * @param salt         optional salt. A possibly non-secret random value. If no salt is
     *                     provided (i.e. if
     *                     salt has length 0) then an array of 0s of the same size as the hash
     *                     digest is used as salt.
     * @param info         optional context and application specific information.
     * @param size         The length of the generated pseudorandom string in bytes. The maximal
     *                     size is
     *                     255.DigestSize, where DigestSize is the size of the underlying HMAC.
     * @return size pseudorandom bytes.
     */
    static byte[] computeHkdf(
            String macAlgorithm, final byte[] ikm, final byte[] salt, final byte[] info, int size) {
        Mac mac = null;
        try {
            mac = Mac.getInstance(macAlgorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("No such algorithm: " + macAlgorithm, e);
        }
        if (size > 255 * mac.getMacLength()) {
            throw new IllegalArgumentException("size too large");
        }
        try {
            if (salt == null || salt.length == 0) {
                // According to RFC 5869, Section 2.2 the salt is optional. If no salt is provided
                // then HKDF uses a salt that is an array of zeros of the same length as the hash
                // digest.
                mac.init(new SecretKeySpec(new byte[mac.getMacLength()], macAlgorithm));
            } else {
                mac.init(new SecretKeySpec(salt, macAlgorithm));
            }
            byte[] prk = mac.doFinal(ikm);
            byte[] result = new byte[size];
            int ctr = 1;
            int pos = 0;
            mac.init(new SecretKeySpec(prk, macAlgorithm));
            byte[] digest = new byte[0];
            while (true) {
                mac.update(digest);
                mac.update(info);
                mac.update((byte) ctr);
                digest = mac.doFinal();
                if (pos + digest.length < size) {
                    System.arraycopy(digest, 0, result, pos, digest.length);
                    pos += digest.length;
                    ctr++;
                } else {
                    System.arraycopy(digest, 0, result, pos, size - pos);
                    break;
                }
            }
            return result;
        } catch (InvalidKeyException e) {
            throw new IllegalStateException("Error MACing", e);
        }
    }

    private static byte[] coseBuildToBeSigned(byte[] encodedProtectedHeaders,
            byte[] payload,
            byte[] detachedContent) {
        CborBuilder sigStructure = new CborBuilder();
        ArrayBuilder<CborBuilder> array = sigStructure.addArray();

        array.add("Signature1");
        array.add(encodedProtectedHeaders);

        // We currently don't support Externally Supplied Data (RFC 8152 section 4.3)
        // so external_aad is the empty bstr
        byte[] emptyExternalAad = new byte[0];
        array.add(emptyExternalAad);

        // Next field is the payload, independently of how it's transported (RFC
        // 8152 section 4.4). Since our API specifies only one of |data| and
        // |detachedContent| can be non-empty, it's simply just the non-empty one.
        if (payload != null && payload.length > 0) {
            array.add(payload);
        } else {
            array.add(detachedContent);
        }
        array.end();
        return cborEncode(sigStructure.build().get(0));
    }

    private static final int COSE_LABEL_ALG = 1;
    private static final int COSE_LABEL_X5CHAIN = 33;  // temporary identifier

    // From "COSE Algorithms" registry
    private static final int COSE_ALG_ECDSA_256 = -7;
    private static final int COSE_ALG_HMAC_256_256 = 5;

    private static byte[] signatureDerToCose(byte[] signature) {
        if (signature.length > 128) {
            throw new IllegalArgumentException(
                    "Unexpected length " + signature.length + ", expected less than 128");
        }
        if (signature[0] != 0x30) {
            throw new IllegalArgumentException("Unexpected first byte " + signature[0]
                    + ", expected 0x30");
        }
        if ((signature[1] & 0x80) != 0x00) {
            throw new IllegalArgumentException(
                    "Unexpected second byte " + signature[1] + ", bit 7 shouldn't be set");
        }
        int rOffset = 2;
        int rSize = signature[rOffset + 1];
        byte[] rBytes = stripLeadingZeroes(
                Arrays.copyOfRange(signature, rOffset + 2, rOffset + rSize + 2));

        int sOffset = rOffset + 2 + rSize;
        int sSize = signature[sOffset + 1];
        byte[] sBytes = stripLeadingZeroes(
                Arrays.copyOfRange(signature, sOffset + 2, sOffset + sSize + 2));

        if (rBytes.length > 32) {
            throw new IllegalArgumentException(
                    "rBytes.length is " + rBytes.length + " which is > 32");
        }
        if (sBytes.length > 32) {
            throw new IllegalArgumentException(
                    "sBytes.length is " + sBytes.length + " which is > 32");
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            for (int n = 0; n < 32 - rBytes.length; n++) {
                baos.write(0x00);
            }
            baos.write(rBytes);
            for (int n = 0; n < 32 - sBytes.length; n++) {
                baos.write(0x00);
            }
            baos.write(sBytes);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return baos.toByteArray();
    }

    // Adds leading 0x00 if the first encoded byte MSB is set.
    private static byte[] encodePositiveBigInteger(BigInteger i) {
        byte[] bytes = i.toByteArray();
        if ((bytes[0] & 0x80) != 0) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                baos.write(0x00);
                baos.write(bytes);
            } catch (IOException e) {
                throw new IllegalStateException("Failed writing data", e);
            }
            bytes = baos.toByteArray();
        }
        return bytes;
    }

    private static byte[] signatureCoseToDer(byte[] signature) {
        if (signature.length != 64) {
            throw new IllegalArgumentException(
                    "signature.length is " + signature.length + ", expected 64");
        }
        // r and s are always positive and may use all 256 bits so use the constructor which
        // parses them as unsigned.
        BigInteger r = new BigInteger(1, Arrays.copyOfRange(signature, 0, 32));
        BigInteger s = new BigInteger(1, Arrays.copyOfRange(signature, 32, 64));
        byte[] rBytes = encodePositiveBigInteger(r);
        byte[] sBytes = encodePositiveBigInteger(s);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            baos.write(0x30);
            baos.write(2 + rBytes.length + 2 + sBytes.length);
            baos.write(0x02);
            baos.write(rBytes.length);
            baos.write(rBytes);
            baos.write(0x02);
            baos.write(sBytes.length);
            baos.write(sBytes);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return baos.toByteArray();
    }

    public static DataItem coseSign1Sign(Signature s,
            @Nullable byte[] data,
            byte[] detachedContent,
            @Nullable Collection<X509Certificate> certificateChain)
            throws CertificateEncodingException {

        int dataLen = (data != null ? data.length : 0);
        int detachedContentLen = (detachedContent != null ? detachedContent.length : 0);
        if (dataLen > 0 && detachedContentLen > 0) {
            throw new IllegalArgumentException("data and detachedContent cannot both be non-empty");
        }

        CborBuilder protectedHeaders = new CborBuilder();
        MapBuilder<CborBuilder> protectedHeadersMap = protectedHeaders.addMap();
        protectedHeadersMap.put(COSE_LABEL_ALG, COSE_ALG_ECDSA_256);
        byte[] protectedHeadersBytes = cborEncode(protectedHeaders.build().get(0));

        byte[] toBeSigned = coseBuildToBeSigned(protectedHeadersBytes, data, detachedContent);

        byte[] coseSignature = null;
        try {
            s.update(toBeSigned);
            byte[] derSignature = s.sign();
            coseSignature = signatureDerToCose(derSignature);
        } catch (SignatureException e) {
            throw new IllegalStateException("Error signing data", e);
        }

        CborBuilder builder = new CborBuilder();
        ArrayBuilder<CborBuilder> array = builder.addArray();
        array.add(protectedHeadersBytes);
        MapBuilder<ArrayBuilder<CborBuilder>> unprotectedHeaders = array.addMap();
        if (certificateChain != null && certificateChain.size() > 0) {
            if (certificateChain.size() == 1) {
                X509Certificate cert = certificateChain.iterator().next();
                unprotectedHeaders.put(COSE_LABEL_X5CHAIN, cert.getEncoded());
            } else {
                ArrayBuilder<MapBuilder<ArrayBuilder<CborBuilder>>> x5chainsArray =
                        unprotectedHeaders.putArray(COSE_LABEL_X5CHAIN);
                for (X509Certificate cert : certificateChain) {
                    x5chainsArray.add(cert.getEncoded());
                }
            }
        }
        if (data == null || data.length == 0) {
            array.add(new SimpleValue(SimpleValueType.NULL));
        } else {
            array.add(data);
        }
        array.add(coseSignature);

        return builder.build().get(0);
    }

    public static DataItem coseSign1Sign(PrivateKey key,
            @Nullable byte[] data,
            byte[] additionalData,
            @Nullable Collection<X509Certificate> certificateChain)
            throws NoSuchAlgorithmException, InvalidKeyException, CertificateEncodingException {

        Signature s = Signature.getInstance("SHA256withECDSA");
        s.initSign(key);
        return coseSign1Sign(s, data, additionalData, certificateChain);
    }

    public static boolean coseSign1CheckSignature(DataItem coseSign1,
            byte[] detachedContent, PublicKey publicKey)  {
        if (coseSign1.getMajorType() != MajorType.ARRAY) {
            throw new IllegalArgumentException("Data item is not an array");
        }
        List<DataItem> items = ((co.nstant.in.cbor.model.Array) coseSign1).getDataItems();
        if (items.size() < 4) {
            throw new IllegalArgumentException("Expected at least four items in COSE_Sign1 array");
        }
        if (items.get(0).getMajorType() != MajorType.BYTE_STRING) {
            throw new IllegalArgumentException("Item 0 (protected headers) is not a byte-string");
        }
        byte[] encodedProtectedHeaders = ((co.nstant.in.cbor.model.ByteString) items.get(
                0)).getBytes();
        byte[] payload = new byte[0];
        if (items.get(2).getMajorType() == MajorType.SPECIAL) {
            if (((co.nstant.in.cbor.model.Special) items.get(2)).getSpecialType()
                    != SpecialType.SIMPLE_VALUE) {
                throw new IllegalArgumentException(
                        "Item 2 (payload) is a special but not a simple value");
            }
            SimpleValue simple = (co.nstant.in.cbor.model.SimpleValue) items.get(2);
            if (simple.getSimpleValueType() != SimpleValueType.NULL) {
                throw new IllegalArgumentException(
                        "Item 2 (payload) is a simple but not the value null");
            }
        } else if (items.get(2).getMajorType() == MajorType.BYTE_STRING) {
            payload = ((co.nstant.in.cbor.model.ByteString) items.get(2)).getBytes();
        } else {
            throw new IllegalArgumentException("Item 2 (payload) is not nil or byte-string");
        }
        if (items.get(3).getMajorType() != MajorType.BYTE_STRING) {
            throw new IllegalArgumentException("Item 3 (signature) is not a byte-string");
        }
        byte[] coseSignature = ((co.nstant.in.cbor.model.ByteString) items.get(3)).getBytes();

        byte[] derSignature = signatureCoseToDer(coseSignature);

        int dataLen = payload.length;
        int detachedContentLen = (detachedContent != null ? detachedContent.length : 0);
        if (dataLen > 0 && detachedContentLen > 0) {
            throw new IllegalArgumentException("data and detachedContent cannot both be non-empty");
        }

        byte[] toBeSigned = Util.coseBuildToBeSigned(encodedProtectedHeaders, payload,
                detachedContent);

        try {
            Signature verifier = Signature.getInstance("SHA256withECDSA");
            verifier.initVerify(publicKey);
            verifier.update(toBeSigned);
            return verifier.verify(derSignature);
        } catch (SignatureException | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Error verifying signature", e);
        }
    }

    private static byte[] coseBuildToBeMACed(byte[] encodedProtectedHeaders,
            byte[] payload,
            byte[] detachedContent) {
        CborBuilder macStructure = new CborBuilder();
        ArrayBuilder<CborBuilder> array = macStructure.addArray();

        array.add("MAC0");
        array.add(encodedProtectedHeaders);

        // We currently don't support Externally Supplied Data (RFC 8152 section 4.3)
        // so external_aad is the empty bstr
        byte[] emptyExternalAad = new byte[0];
        array.add(emptyExternalAad);

        // Next field is the payload, independently of how it's transported (RFC
        // 8152 section 4.4). Since our API specifies only one of |data| and
        // |detachedContent| can be non-empty, it's simply just the non-empty one.
        if (payload != null && payload.length > 0) {
            array.add(payload);
        } else {
            array.add(detachedContent);
        }

        return cborEncode(macStructure.build().get(0));
    }

    public static DataItem coseMac0(SecretKey key,
            @Nullable byte[] data,
            byte[] detachedContent) {

        int dataLen = (data != null ? data.length : 0);
        int detachedContentLen = (detachedContent != null ? detachedContent.length : 0);
        if (dataLen > 0 && detachedContentLen > 0) {
            throw new IllegalArgumentException("data and detachedContent cannot both be non-empty");
        }

        CborBuilder protectedHeaders = new CborBuilder();
        MapBuilder<CborBuilder> protectedHeadersMap = protectedHeaders.addMap();
        protectedHeadersMap.put(COSE_LABEL_ALG, COSE_ALG_HMAC_256_256);
        byte[] protectedHeadersBytes = cborEncode(protectedHeaders.build().get(0));

        byte[] toBeMACed = coseBuildToBeMACed(protectedHeadersBytes, data, detachedContent);

        byte[] mac;
        try {
            Mac m = Mac.getInstance("HmacSHA256");
            m.init(key);
            m.update(toBeMACed);
            mac = m.doFinal();
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Unexpected error", e);
        }

        CborBuilder builder = new CborBuilder();
        ArrayBuilder<CborBuilder> array = builder.addArray();
        array.add(protectedHeadersBytes);
        /* MapBuilder<ArrayBuilder<CborBuilder>> unprotectedHeaders = */ array.addMap();
        if (data == null || data.length == 0) {
            array.add(new SimpleValue(SimpleValueType.NULL));
        } else {
            array.add(data);
        }
        array.add(mac);

        return builder.build().get(0);
    }

    public static byte[] coseMac0GetTag(DataItem coseMac0) {
        if (!(coseMac0 instanceof Array)) {
            throw new IllegalArgumentException("coseMac0 is not an array");
        }
        List<DataItem> items = ((Array) coseMac0).getDataItems();
        if (items.size() < 4) {
            throw new IllegalArgumentException("coseMac0 have less than 4 elements");
        }
        DataItem tagItem = items.get(3);
        if (!(tagItem instanceof ByteString)) {
            throw new IllegalArgumentException("tag in coseMac0 is not a ByteString");
        }
        return ((ByteString) tagItem).getBytes();
    }


    // Brute-force but good enough since users will only pass relatively small amounts of data.
    static boolean hasSubByteArray(byte[] haystack, byte[] needle) {
        int n = 0;
        while (needle.length + n <= haystack.length) {
            boolean found = true;
            for (int m = 0; m < needle.length; m++) {
                if (needle[m] != haystack[n + m]) {
                    found = false;
                    break;
                }
            }
            if (found) {
                return true;
            }
            n++;
        }
        return false;
    }

    static byte[] stripLeadingZeroes(byte[] value) {
        int n = 0;
        while (n < value.length && value[n] == 0) {
            n++;
        }
        int newLen = value.length - n;
        byte[] ret = new byte[newLen];
        int m = 0;
        while (n < value.length) {
            ret[m++] = value[n++];
        }
        return ret;
    }

    static final int CBOR_SEMANTIC_TAG_ENCODED_CBOR = 24;

    // Returns #6.24(bstr) of the given already encoded CBOR
    //
    public static @NonNull DataItem cborBuildTaggedByteString(@NonNull byte[] encodedCbor) {
        DataItem item = new ByteString(encodedCbor);
        item.setTag(CBOR_SEMANTIC_TAG_ENCODED_CBOR);
        return item;
    }

    // For a #6.24(bstr), extracts the bytes and decodes it and returns
    // the decoded CBOR as a DataItem.
    //
    public static DataItem cborExtractTaggedAndEncodedCbor(DataItem item) {
        if (item == null || !(item instanceof ByteString)) {
            throw new IllegalArgumentException("Item is not a ByteString");
        }
        if (!item.hasTag() || item.getTag().getValue() != CBOR_SEMANTIC_TAG_ENCODED_CBOR) {
            throw new IllegalArgumentException("ByteString is not tagged with tag 24");
        }
        byte[] encodedCbor = ((ByteString) item).getBytes();
        DataItem embeddedItem = cborDecode(encodedCbor);
        return embeddedItem;
    }

    // Returns the empty byte-array if no data is included in the structure.
    //
    public static byte[] coseSign1GetData(DataItem coseSign1) {
        if (coseSign1.getMajorType() != MajorType.ARRAY) {
            throw new IllegalArgumentException("Data item is not an array");
        }
        List<DataItem> items = ((co.nstant.in.cbor.model.Array) coseSign1).getDataItems();
        if (items.size() < 4) {
            throw new IllegalArgumentException("Expected at least four items in COSE_Sign1 array");
        }
        byte[] payload = new byte[0];
        if (items.get(2).getMajorType() == MajorType.SPECIAL) {
            if (((co.nstant.in.cbor.model.Special) items.get(2)).getSpecialType()
                    != SpecialType.SIMPLE_VALUE) {
                throw new IllegalArgumentException(
                        "Item 2 (payload) is a special but not a simple value");
            }
            SimpleValue simple = (co.nstant.in.cbor.model.SimpleValue) items.get(2);
            if (simple.getSimpleValueType() != SimpleValueType.NULL) {
                throw new IllegalArgumentException(
                        "Item 2 (payload) is a simple but not the value null");
            }
        } else if (items.get(2).getMajorType() == MajorType.BYTE_STRING) {
            payload = ((co.nstant.in.cbor.model.ByteString) items.get(2)).getBytes();
        } else {
            throw new IllegalArgumentException("Item 2 (payload) is not nil or byte-string");
        }
        return payload;
    }

    // Returns the empty collection if no x5chain is included in the structure.
    //
    // Throws exception if the given bytes aren't valid COSE_Sign1.
    //
    public static Collection<X509Certificate> coseSign1GetX5Chain(DataItem coseSign1) {
        ArrayList<X509Certificate> ret = new ArrayList<>();
        if (coseSign1.getMajorType() != MajorType.ARRAY) {
            throw new IllegalArgumentException("Data item is not an array");
        }
        List<DataItem> items = ((co.nstant.in.cbor.model.Array) coseSign1).getDataItems();
        if (items.size() < 4) {
            throw new IllegalArgumentException("Expected at least four items in COSE_Sign1 array");
        }
        if (items.get(1).getMajorType() != MajorType.MAP) {
            throw new IllegalArgumentException("Item 1 (unprotected headers) is not a map");
        }
        co.nstant.in.cbor.model.Map map = (co.nstant.in.cbor.model.Map) items.get(1);
        DataItem x5chainItem = map.get(new UnsignedInteger(COSE_LABEL_X5CHAIN));
        if (x5chainItem != null) {
            try {
                CertificateFactory factory = CertificateFactory.getInstance("X.509");
                if (x5chainItem instanceof ByteString) {
                    ByteArrayInputStream certBais = new ByteArrayInputStream(
                            ((ByteString) x5chainItem).getBytes());
                    ret.add((X509Certificate) factory.generateCertificate(certBais));
                } else if (x5chainItem instanceof Array) {
                    for (DataItem certItem : ((Array) x5chainItem).getDataItems()) {
                        if (!(certItem instanceof ByteString)) {
                            throw new IllegalArgumentException(
                                    "Unexpected type for array item in x5chain value");
                        }
                        ByteArrayInputStream certBais = new ByteArrayInputStream(
                                ((ByteString) certItem).getBytes());
                        ret.add((X509Certificate) factory.generateCertificate(certBais));
                    }
                } else {
                    throw new IllegalArgumentException("Unexpected type for x5chain value");
                }
            } catch (CertificateException e) {
                throw new IllegalArgumentException("Unexpected error", e);
            }
        }
        return ret;
    }

    public static SecretKey calcEMacKeyForReader(PublicKey authenticationPublicKey,
            PrivateKey ephemeralReaderPrivateKey,
            byte[] encodedSessionTranscript) {
        try {
            KeyAgreement ka = KeyAgreement.getInstance("ECDH");
            ka.init(ephemeralReaderPrivateKey);
            ka.doPhase(authenticationPublicKey, true);
            byte[] sharedSecret = ka.generateSecret();

            byte[] sessionTranscriptBytes =
                    Util.cborEncode(Util.cborBuildTaggedByteString(encodedSessionTranscript));

            byte[] salt = MessageDigest.getInstance("SHA-256").digest(sessionTranscriptBytes);
            byte[] info = new byte[] {'E', 'M', 'a', 'c', 'K', 'e', 'y'};
            byte[] derivedKey = computeHkdf("HmacSha256", sharedSecret, salt, info, 32);

            SecretKey secretKey = new SecretKeySpec(derivedKey, "");
            return secretKey;
        } catch (InvalidKeyException
                | NoSuchAlgorithmException e) {
            throw new IllegalStateException("Error performing key agreement", e);
        }
    }


    public static String cborPrettyPrint(DataItem dataItem) {
        StringBuilder sb = new StringBuilder();
        cborPrettyPrintDataItem(sb, 0, dataItem);
        return sb.toString();
    }

    public static String cborPrettyPrint(byte[] encodedBytes) {
        StringBuilder sb = new StringBuilder();

        ByteArrayInputStream bais = new ByteArrayInputStream(encodedBytes);
        List<DataItem> dataItems = null;
        try {
            dataItems = new CborDecoder(bais).decode();
        } catch (CborException e) {
            throw new IllegalStateException(e);
        }
        int count = 0;
        for (DataItem dataItem : dataItems) {
            if (count > 0) {
                sb.append(",\n");
            }
            cborPrettyPrintDataItem(sb, 0, dataItem);
            count++;
        }

        return sb.toString();
    }

    // Returns true iff all elements in |items| are not compound (e.g. an array or a map).
    private static boolean cborAreAllDataItemsNonCompound(List<DataItem> items) {
        for (DataItem item : items) {
            switch (item.getMajorType()) {
                case ARRAY:
                case MAP:
                    return false;
                default:
                    // Do nothing
                    break;
            }
        }
        return true;
    }

    private static void cborPrettyPrintDataItem(StringBuilder sb, int indent, DataItem dataItem) {
        StringBuilder indentBuilder = new StringBuilder();
        for (int n = 0; n < indent; n++) {
            indentBuilder.append(' ');
        }
        String indentString = indentBuilder.toString();

        if (dataItem.hasTag()) {
            sb.append(String.format("tag %d ", dataItem.getTag().getValue()));
        }

        switch (dataItem.getMajorType()) {
            case INVALID:
                // TODO: throw
                sb.append("<invalid>");
                break;
            case UNSIGNED_INTEGER: {
                // Major type 0: an unsigned integer.
                BigInteger value = ((UnsignedInteger) dataItem).getValue();
                sb.append(value);
            }
            break;
            case NEGATIVE_INTEGER: {
                // Major type 1: a negative integer.
                BigInteger value = ((NegativeInteger) dataItem).getValue();
                sb.append(value);
            }
            break;
            case BYTE_STRING: {
                // Major type 2: a byte string.
                byte[] value = ((ByteString) dataItem).getBytes();
                sb.append("[");
                int count = 0;
                for (byte b : value) {
                    if (count > 0) {
                        sb.append(", ");
                    }
                    sb.append(String.format("0x%02x", b));
                    count++;
                }
                sb.append("]");
            }
            break;
            case UNICODE_STRING: {
                // Major type 3: string of Unicode characters that is encoded as UTF-8 [RFC3629].
                String value = ((UnicodeString) dataItem).getString();
                // TODO: escape ' in |value|
                sb.append("'" + value + "'");
            }
            break;
            case ARRAY: {
                // Major type 4: an array of data items.
                List<DataItem> items = ((co.nstant.in.cbor.model.Array) dataItem).getDataItems();
                if (items.size() == 0) {
                    sb.append("[]");
                } else if (cborAreAllDataItemsNonCompound(items)) {
                    // The case where everything fits on one line.
                    sb.append("[");
                    int count = 0;
                    for (DataItem item : items) {
                        cborPrettyPrintDataItem(sb, indent, item);
                        if (++count < items.size()) {
                            sb.append(", ");
                        }
                    }
                    sb.append("]");
                } else {
                    sb.append("[\n" + indentString);
                    int count = 0;
                    for (DataItem item : items) {
                        sb.append("  ");
                        cborPrettyPrintDataItem(sb, indent + 2, item);
                        if (++count < items.size()) {
                            sb.append(",");
                        }
                        sb.append("\n" + indentString);
                    }
                    sb.append("]");
                }
            }
            break;
            case MAP: {
                // Major type 5: a map of pairs of data items.
                Collection<DataItem> keys = ((co.nstant.in.cbor.model.Map) dataItem).getKeys();
                if (keys.size() == 0) {
                    sb.append("{}");
                } else {
                    sb.append("{\n" + indentString);
                    int count = 0;
                    for (DataItem key : keys) {
                        sb.append("  ");
                        DataItem value = ((co.nstant.in.cbor.model.Map) dataItem).get(key);
                        cborPrettyPrintDataItem(sb, indent + 2, key);
                        sb.append(" : ");
                        cborPrettyPrintDataItem(sb, indent + 2, value);
                        if (++count < keys.size()) {
                            sb.append(",");
                        }
                        sb.append("\n" + indentString);
                    }
                    sb.append("}");
                }
            }
            break;
            case TAG:
                // Major type 6: optional semantic tagging of other major types
                //
                // We never encounter this one since it's automatically handled via the
                // DataItem that is tagged.
                throw new IllegalStateException("Semantic tag data item not expected");

            case SPECIAL:
                // Major type 7: floating point numbers and simple data types that need no
                // content, as well as the "break" stop code.
                if (dataItem instanceof SimpleValue) {
                    switch (((SimpleValue) dataItem).getSimpleValueType()) {
                        case FALSE:
                            sb.append("false");
                            break;
                        case TRUE:
                            sb.append("true");
                            break;
                        case NULL:
                            sb.append("null");
                            break;
                        case UNDEFINED:
                            sb.append("undefined");
                            break;
                        case RESERVED:
                            sb.append("reserved");
                            break;
                        case UNALLOCATED:
                            sb.append("unallocated");
                            break;
                    }
                } else if (dataItem instanceof DoublePrecisionFloat) {
                    DecimalFormat df = new DecimalFormat("0",
                            DecimalFormatSymbols.getInstance(Locale.ENGLISH));
                    df.setMaximumFractionDigits(340);
                    sb.append(df.format(((DoublePrecisionFloat) dataItem).getValue()));
                } else if (dataItem instanceof AbstractFloat) {
                    DecimalFormat df = new DecimalFormat("0",
                            DecimalFormatSymbols.getInstance(Locale.ENGLISH));
                    df.setMaximumFractionDigits(340);
                    sb.append(df.format(((AbstractFloat) dataItem).getValue()));
                } else {
                    sb.append("break");
                }
                break;
        }
    }

    static byte[] canonicalizeCbor(byte[] encodedCbor) throws CborException {
        ByteArrayInputStream bais = new ByteArrayInputStream(encodedCbor);
        List<DataItem> dataItems = new CborDecoder(bais).decode();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (DataItem dataItem : dataItems) {
            CborEncoder encoder = new CborEncoder(baos);
            encoder.encode(dataItem);
        }
        return baos.toByteArray();
    }

    public static String replaceLine(String text, int lineNumber, String replacementLine) {
        @SuppressWarnings("StringSplitter")
        String[] lines = text.split("\n");
        int numLines = lines.length;
        if (lineNumber < 0) {
            lineNumber = numLines - -lineNumber;
        }
        StringBuilder sb = new StringBuilder();
        for (int n = 0; n < numLines; n++) {
            if (n == lineNumber) {
                sb.append(replacementLine);
            } else {
                sb.append(lines[n]);
            }
            // Only add terminating newline if passed-in string ends in a newline.
            if (n == numLines - 1) {
                if (text.endsWith("\n")) {
                    sb.append('\n');
                }
            } else {
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    // This returns a SessionTranscript which satisfy the requirement
    // that the uncompressed X and Y coordinates of the public key for the
    // mDL's ephemeral key-pair appear somewhere in the encoded
    // DeviceEngagement.
    static byte[] buildSessionTranscript(KeyPair ephemeralKeyPair) {
        // Make the coordinates appear in an already encoded bstr - this
        // mimics how the mDL COSE_Key appear as encoded data inside the
        // encoded DeviceEngagement
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ECPoint w = ((ECPublicKey) ephemeralKeyPair.getPublic()).getW();
            // X and Y are always positive so for interop we remove any leading zeroes
            // inserted by the BigInteger encoder.
            byte[] x = stripLeadingZeroes(w.getAffineX().toByteArray());
            byte[] y = stripLeadingZeroes(w.getAffineY().toByteArray());
            baos.write(new byte[]{42});
            baos.write(x);
            baos.write(y);
            baos.write(new byte[]{43, 44});
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        byte[] blobWithCoords = baos.toByteArray();

        DataItem encodedDeviceEngagementItem = cborBuildTaggedByteString(
                cborEncode(new CborBuilder()
                        .addArray()
                        .add(blobWithCoords)
                        .end()
                        .build().get(0)));
        DataItem encodedEReaderKeyItem =
                cborBuildTaggedByteString(Util.cborEncodeString("doesn't matter"));

        baos = new ByteArrayOutputStream();
        try {
            new CborEncoder(baos).encode(new CborBuilder()
                    .addArray()
                    .add(encodedDeviceEngagementItem)
                    .add(encodedEReaderKeyItem)
                    .end()
                    .build());
        } catch (CborException e) {
            e.printStackTrace();
            return null;
        }
        return baos.toByteArray();
    }

    static IdentityCredentialStore getIdentityCredentialStore(@NonNull Context context) {
        // We generally want to run all tests against the software implementation since
        // hardware-based implementations are already tested against CTS and VTS and the bulk
        // of the code in the Jetpack is the software implementation. This also helps avoid
        // whatever bugs or flakiness that may exist in hardware implementations.
        //
        // Occasionally it's useful for a developer to test that the hardware-backed paths
        // (HardwareIdentityCredentialStore + friends) work as intended. This can be done by
        // uncommenting the line below and making sure it runs on a device with the appropriate
        // hardware support.
        //
        // See b/164480361 for more discussion.
        //
        //return IdentityCredentialStore.getHardwareInstance(context);
        return IdentityCredentialStore.getSoftwareInstance(context);
    }

        /*
Certificate:
    Data:
        Version: 3 (0x2)
        Serial Number: 1 (0x1)
    Signature Algorithm: ecdsa-with-SHA256
        Issuer: CN=fake
        Validity
            Not Before: Jan  1 00:00:00 1970 GMT
            Not After : Jan  1 00:00:00 2048 GMT
        Subject: CN=fake
        Subject Public Key Info:
            Public Key Algorithm: id-ecPublicKey
                Public-Key: (256 bit)
                00000000  04 9b 60 70 8a 99 b6 bf  e3 b8 17 02 9e 93 eb 48  |..`p...........H|
                00000010  23 b9 39 89 d1 00 bf a0  0f d0 2f bd 6b 11 bc d1  |#.9......./.k...|
                00000020  19 53 54 28 31 00 f5 49  db 31 fb 9f 7d 99 bf 23  |.ST(1..I.1..}..#|
                00000030  fb 92 04 6b 23 63 55 98  ad 24 d2 68 c4 83 bf 99  |...k#cU..$.h....|
                00000040  62                                                |b|
    Signature Algorithm: ecdsa-with-SHA256
         30:45:02:20:67:ad:d1:34:ed:a5:68:3f:5b:33:ee:b3:18:a2:
         eb:03:61:74:0f:21:64:4a:a3:2e:82:b3:92:5c:21:0f:88:3f:
         02:21:00:b7:38:5c:9b:f2:9c:b1:27:86:37:44:df:eb:4a:b2:
         6c:11:9a:c1:ff:b2:80:95:ce:fc:5f:26:b4:20:6e:9b:0d
     */
    static @NonNull X509Certificate signPublicKeyWithPrivateKey(String keyToSignAlias,
            String keyToSignWithAlias) {

        KeyStore ks = null;
        try {
            ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);

            /* First note that KeyStore.getCertificate() returns a self-signed X.509 certificate
             * for the key in question. As per RFC 5280, section 4.1 an X.509 certificate has the
             * following structure:
             *
             *   Certificate  ::=  SEQUENCE  {
             *        tbsCertificate       TBSCertificate,
             *        signatureAlgorithm   AlgorithmIdentifier,
             *        signatureValue       BIT STRING  }
             *
             * Conveniently, the X509Certificate class has a getTBSCertificate() method which
             * returns the tbsCertificate blob. So all we need to do is just sign that and build
             * signatureAlgorithm and signatureValue and combine it with tbsCertificate. We don't
             * need a full-blown ASN.1/DER encoder to do this.
             */
            X509Certificate selfSignedCert = (X509Certificate) ks.getCertificate(keyToSignAlias);
            byte[] tbsCertificate = selfSignedCert.getTBSCertificate();

            KeyStore.Entry keyToSignWithEntry = ks.getEntry(keyToSignWithAlias, null);
            Signature s = Signature.getInstance("SHA256withECDSA");
            s.initSign(((KeyStore.PrivateKeyEntry) keyToSignWithEntry).getPrivateKey());
            s.update(tbsCertificate);
            byte[] signatureValue = s.sign();

            /* The DER encoding for a SEQUENCE of length 128-65536 - the length is updated below.
             *
             * We assume - and test for below - that the final length is always going to be in
             * this range. This is a sound assumption given we're using 256-bit EC keys.
             */
            byte[] sequence = new byte[]{
                    0x30, (byte) 0x82, 0x00, 0x00
            };

            /* The DER encoding for the ECDSA with SHA-256 signature algorithm:
             *
             *   SEQUENCE (1 elem)
             *      OBJECT IDENTIFIER 1.2.840.10045.4.3.2 ecdsaWithSHA256 (ANSI X9.62 ECDSA
             *      algorithm with SHA256)
             */
            byte[] signatureAlgorithm = new byte[]{
                    0x30, 0x0a, 0x06, 0x08, 0x2a, (byte) 0x86, 0x48, (byte) 0xce, 0x3d, 0x04, 0x03,
                    0x02
            };

            /* The DER encoding for a BIT STRING with one element - the length is updated below.
             *
             * We assume the length of signatureValue is always going to be less than 128. This
             * assumption works since we know ecdsaWithSHA256 signatures are always 69, 70, or
             * 71 bytes long when DER encoded.
             */
            byte[] bitStringForSignature = new byte[]{0x03, 0x00, 0x00};

            // Calculate sequence length and set it in |sequence|.
            int sequenceLength = tbsCertificate.length
                    + signatureAlgorithm.length
                    + bitStringForSignature.length
                    + signatureValue.length;
            if (sequenceLength < 128 || sequenceLength > 65535) {
                throw new Exception("Unexpected sequenceLength " + sequenceLength);
            }
            sequence[2] = (byte) (sequenceLength >> 8);
            sequence[3] = (byte) (sequenceLength & 0xff);

            // Calculate signatureValue length and set it in |bitStringForSignature|.
            int signatureValueLength = signatureValue.length + 1;
            if (signatureValueLength >= 128) {
                throw new Exception("Unexpected signatureValueLength " + signatureValueLength);
            }
            bitStringForSignature[1] = (byte) signatureValueLength;

            // Finally concatenate everything together.
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(sequence);
            baos.write(tbsCertificate);
            baos.write(signatureAlgorithm);
            baos.write(bitStringForSignature);
            baos.write(signatureValue);
            byte[] resultingCertBytes = baos.toByteArray();

            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            ByteArrayInputStream bais = new ByteArrayInputStream(resultingCertBytes);
            X509Certificate result = (X509Certificate) cf.generateCertificate(bais);
            return result;
        } catch (Exception e) {
            throw new IllegalStateException("Error signing public key with private key", e);
        }
    }

    /*
     * Helper function to create a CBOR data for requesting data items. The IntentToRetain
     * value will be set to false for all elements.
     *
     * <p>The returned CBOR data conforms to the following CDDL schema:</p>
     *
     * <pre>
     *   ItemsRequest = {
     *     ? "docType" : DocType,
     *     "nameSpaces" : NameSpaces,
     *     ? "RequestInfo" : {* tstr => any} ; Additional info the reader wants to provide
     *   }
     *
     *   NameSpaces = {
     *     + NameSpace => DataElements     ; Requested data elements for each NameSpace
     *   }
     *
     *   DataElements = {
     *     + DataElement => IntentToRetain
     *   }
     *
     *   DocType = tstr
     *
     *   DataElement = tstr
     *   IntentToRetain = bool
     *   NameSpace = tstr
     * </pre>
     *
     * @param entriesToRequest       The entries to request, organized as a map of namespace
     *                               names with each value being a collection of data elements
     *                               in the given namespace.
     * @param docType                  The document type or {@code null} if there is no document
     *                                 type.
     * @return CBOR data conforming to the CDDL mentioned above.
     */
    static @NonNull byte[] createItemsRequest(
            @NonNull java.util.Map<String, Collection<String>> entriesToRequest,
            @Nullable String docType) {
        CborBuilder builder = new CborBuilder();
        MapBuilder<CborBuilder> mapBuilder = builder.addMap();
        if (docType != null) {
            mapBuilder.put("docType", docType);
        }

        MapBuilder<MapBuilder<CborBuilder>> nsMapBuilder = mapBuilder.putMap("nameSpaces");
        for (String namespaceName : entriesToRequest.keySet()) {
            Collection<String> entryNames = entriesToRequest.get(namespaceName);
            MapBuilder<MapBuilder<MapBuilder<CborBuilder>>> entryNameMapBuilder =
                    nsMapBuilder.putMap(namespaceName);
            for (String entryName : entryNames) {
                entryNameMapBuilder.put(entryName, false);
            }
        }
        return cborEncode(builder.build().get(0));
    }

    static KeyPair createEphemeralKeyPair() {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC);
            ECGenParameterSpec ecSpec = new ECGenParameterSpec("prime256v1");
            kpg.initialize(ecSpec);
            KeyPair keyPair = kpg.generateKeyPair();
            return keyPair;
        } catch (NoSuchAlgorithmException
                | InvalidAlgorithmParameterException e) {
            throw new IllegalStateException("Error generating ephemeral key-pair", e);
        }
    }

    static byte[] getPopSha256FromAuthKeyCert(X509Certificate cert) {
        byte[] octetString = cert.getExtensionValue("1.3.6.1.4.1.11129.2.1.26");
        if (octetString == null) {
            return null;
        }
        try {
            ASN1InputStream asn1InputStream = new ASN1InputStream(octetString);
            byte[] cborBytes = ((ASN1OctetString) asn1InputStream.readObject()).getOctets();

            ByteArrayInputStream bais = new ByteArrayInputStream(cborBytes);
            List<DataItem> dataItems = new CborDecoder(bais).decode();
            if (dataItems.size() != 1) {
                throw new IllegalArgumentException("Expected 1 item, found " + dataItems.size());
            }
            if (!(dataItems.get(0) instanceof Array)) {
                throw new IllegalArgumentException("Item is not a map");
            }
            Array array = (Array) dataItems.get(0);
            List<DataItem> items = array.getDataItems();
            if (items.size() < 2) {
                throw new IllegalArgumentException(
                        "Expected at least 2 array items, found " + items.size());
            }
            if (!(items.get(0) instanceof UnicodeString)) {
                throw new IllegalArgumentException("First array item is not a string");
            }
            String id = ((UnicodeString) items.get(0)).getString();
            if (!id.equals("ProofOfBinding")) {
                throw new IllegalArgumentException("Expected ProofOfBinding, got " + id);
            }
            if (!(items.get(1) instanceof ByteString)) {
                throw new IllegalArgumentException("Second array item is not a bytestring");
            }
            byte[] popSha256 = ((ByteString) items.get(1)).getBytes();
            if (popSha256.length != 32) {
                throw new IllegalArgumentException(
                        "Expected bstr to be 32 bytes, it is " + popSha256.length);
            }
            return popSha256;
        } catch (IOException e) {
            throw new IllegalArgumentException("Error decoding extension data", e);
        } catch (CborException e) {
            throw new IllegalArgumentException("Error decoding data", e);
        }
    }
}
