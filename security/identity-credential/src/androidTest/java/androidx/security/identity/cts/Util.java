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

package androidx.security.identity.cts;

import android.content.Context;
import android.security.keystore.KeyProperties;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.security.identity.IdentityCredentialStore;
import androidx.security.identity.ResultData;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
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
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Formatter;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

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
import co.nstant.in.cbor.model.NegativeInteger;
import co.nstant.in.cbor.model.SimpleValue;
import co.nstant.in.cbor.model.SimpleValueType;
import co.nstant.in.cbor.model.SpecialType;
import co.nstant.in.cbor.model.UnicodeString;
import co.nstant.in.cbor.model.UnsignedInteger;

class Util {
    private static final String TAG = "Util";

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
        //return IdentityCredentialStore.getHardwareIdentityCredentialStore(context);
        return IdentityCredentialStore.getSoftwareIdentityCredentialStore(context);
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


    static String cborPrettyPrint(byte[] encodedBytes) throws CborException {
        StringBuilder sb = new StringBuilder();

        ByteArrayInputStream bais = new ByteArrayInputStream(encodedBytes);
        List<DataItem> dataItems = new CborDecoder(bais).decode();
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
    static boolean cborAreAllDataItemsNonCompound(List<DataItem> items) {
        for (DataItem item : items) {
            switch (item.getMajorType()) {
                case ARRAY:
                case MAP:
                    return false;
            }
        }
        return true;
    }

    static void cborPrettyPrintDataItem(StringBuilder sb, int indent, DataItem dataItem) {
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
                throw new RuntimeException("Semantic tag data item not expected");

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

    public static byte[] encodeCbor(List<DataItem> dataItems) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        CborEncoder encoder = new CborEncoder(baos);
        try {
            encoder.encode(dataItems);
        } catch (CborException e) {
            throw new RuntimeException("Error encoding data", e);
        }
        return baos.toByteArray();
    }

    public static byte[] coseBuildToBeSigned(byte[] encodedProtectedHeaders,
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
        return encodeCbor(sigStructure.build());
    }

    private static final int COSE_LABEL_ALG = 1;
    private static final int COSE_LABEL_X5CHAIN = 33;  // temporary identifier

    // From "COSE Algorithms" registry
    private static final int COSE_ALG_ECDSA_256 = -7;
    private static final int COSE_ALG_HMAC_256_256 = 5;

    private static byte[] signatureDerToCose(byte[] signature) {
        if (signature.length > 128) {
            throw new RuntimeException(
                    "Unexpected length " + signature.length + ", expected less than 128");
        }
        if (signature[0] != 0x30) {
            throw new RuntimeException("Unexpected first byte " + signature[0] + ", expected 0x30");
        }
        if ((signature[1] & 0x80) != 0x00) {
            throw new RuntimeException(
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
            throw new RuntimeException("rBytes.length is " + rBytes.length + " which is > 32");
        }
        if (sBytes.length > 32) {
            throw new RuntimeException("sBytes.length is " + sBytes.length + " which is > 32");
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
                e.printStackTrace();
                throw new RuntimeException("Failed writing data", e);
            }
            bytes = baos.toByteArray();
        }
        return bytes;
    }

    private static byte[] signatureCoseToDer(byte[] signature) {
        if (signature.length != 64) {
            throw new RuntimeException("signature.length is " + signature.length + ", expected 64");
        }
        BigInteger r = new BigInteger(Arrays.copyOfRange(signature, 0, 32));
        BigInteger s = new BigInteger(Arrays.copyOfRange(signature, 32, 64));
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

    public static byte[] coseSign1Sign(PrivateKey key,
            @Nullable byte[] data,
            byte[] detachedContent,
            @Nullable Collection<X509Certificate> certificateChain)
            throws NoSuchAlgorithmException, InvalidKeyException, CertificateEncodingException {

        int dataLen = (data != null ? data.length : 0);
        int detachedContentLen = (detachedContent != null ? detachedContent.length : 0);
        if (dataLen > 0 && detachedContentLen > 0) {
            throw new RuntimeException("data and detachedContent cannot both be non-empty");
        }

        CborBuilder protectedHeaders = new CborBuilder();
        MapBuilder<CborBuilder> protectedHeadersMap = protectedHeaders.addMap();
        protectedHeadersMap.put(COSE_LABEL_ALG, COSE_ALG_ECDSA_256);
        byte[] protectedHeadersBytes = encodeCbor(protectedHeaders.build());

        byte[] toBeSigned = coseBuildToBeSigned(protectedHeadersBytes, data, detachedContent);

        byte[] coseSignature = null;
        try {
            Signature s = Signature.getInstance("SHA256withECDSA");
            s.initSign(key);
            s.update(toBeSigned);
            byte[] derSignature = s.sign();
            coseSignature = signatureDerToCose(derSignature);
        } catch (SignatureException e) {
            throw new RuntimeException("Error signing data");
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

        return encodeCbor(builder.build());
    }

    public static boolean coseSign1CheckSignature(byte[] signatureCose1,
            byte[] detachedContent,
            PublicKey publicKey) throws NoSuchAlgorithmException, InvalidKeyException {
        ByteArrayInputStream bais = new ByteArrayInputStream(signatureCose1);
        List<DataItem> dataItems = null;
        try {
            dataItems = new CborDecoder(bais).decode();
        } catch (CborException e) {
            throw new RuntimeException("Given signature is not valid CBOR", e);
        }
        if (dataItems.size() != 1) {
            throw new RuntimeException("Expected just one data item");
        }
        DataItem dataItem = dataItems.get(0);
        if (dataItem.getMajorType() != MajorType.ARRAY) {
            throw new RuntimeException("Data item is not an array");
        }
        List<DataItem> items = ((co.nstant.in.cbor.model.Array) dataItem).getDataItems();
        if (items.size() < 4) {
            throw new RuntimeException("Expected at least four items in COSE_Sign1 array");
        }
        if (items.get(0).getMajorType() != MajorType.BYTE_STRING) {
            throw new RuntimeException("Item 0 (protected headers) is not a byte-string");
        }
        byte[] encodedProtectedHeaders = ((co.nstant.in.cbor.model.ByteString) items.get(
                0)).getBytes();
        byte[] payload = new byte[0];
        if (items.get(2).getMajorType() == MajorType.SPECIAL) {
            if (((co.nstant.in.cbor.model.Special) items.get(2)).getSpecialType()
                    != SpecialType.SIMPLE_VALUE) {
                throw new RuntimeException("Item 2 (payload) is a special but not a simple value");
            }
            SimpleValue simple = (co.nstant.in.cbor.model.SimpleValue) items.get(2);
            if (simple.getSimpleValueType() != SimpleValueType.NULL) {
                throw new RuntimeException("Item 2 (payload) is a simple but not the value null");
            }
        } else if (items.get(2).getMajorType() == MajorType.BYTE_STRING) {
            payload = ((co.nstant.in.cbor.model.ByteString) items.get(2)).getBytes();
        } else {
            throw new RuntimeException("Item 2 (payload) is not nil or byte-string");
        }
        if (items.get(3).getMajorType() != MajorType.BYTE_STRING) {
            throw new RuntimeException("Item 3 (signature) is not a byte-string");
        }
        byte[] coseSignature = ((co.nstant.in.cbor.model.ByteString) items.get(3)).getBytes();

        byte[] derSignature = signatureCoseToDer(coseSignature);

        int dataLen = payload.length;
        int detachedContentLen = (detachedContent != null ? detachedContent.length : 0);
        if (dataLen > 0 && detachedContentLen > 0) {
            throw new RuntimeException("data and detachedContent cannot both be non-empty");
        }

        byte[] toBeSigned = Util.coseBuildToBeSigned(encodedProtectedHeaders, payload,
                detachedContent);

        try {
            Signature verifier = Signature.getInstance("SHA256withECDSA");
            verifier.initVerify(publicKey);
            verifier.update(toBeSigned);
            return verifier.verify(derSignature);
        } catch (SignatureException e) {
            throw new RuntimeException("Error verifying signature");
        }
    }

    // Returns the empty byte-array if no data is included in the structure.
    //
    // Throws RuntimeException if the given bytes aren't valid COSE_Sign1.
    //
    public static byte[] coseSign1GetData(byte[] signatureCose1) {
        ByteArrayInputStream bais = new ByteArrayInputStream(signatureCose1);
        List<DataItem> dataItems = null;
        try {
            dataItems = new CborDecoder(bais).decode();
        } catch (CborException e) {
            throw new RuntimeException("Given signature is not valid CBOR", e);
        }
        if (dataItems.size() != 1) {
            throw new RuntimeException("Expected just one data item");
        }
        DataItem dataItem = dataItems.get(0);
        if (dataItem.getMajorType() != MajorType.ARRAY) {
            throw new RuntimeException("Data item is not an array");
        }
        List<DataItem> items = ((co.nstant.in.cbor.model.Array) dataItem).getDataItems();
        if (items.size() < 4) {
            throw new RuntimeException("Expected at least four items in COSE_Sign1 array");
        }
        byte[] payload = new byte[0];
        if (items.get(2).getMajorType() == MajorType.SPECIAL) {
            if (((co.nstant.in.cbor.model.Special) items.get(2)).getSpecialType()
                    != SpecialType.SIMPLE_VALUE) {
                throw new RuntimeException("Item 2 (payload) is a special but not a simple value");
            }
            SimpleValue simple = (co.nstant.in.cbor.model.SimpleValue) items.get(2);
            if (simple.getSimpleValueType() != SimpleValueType.NULL) {
                throw new RuntimeException("Item 2 (payload) is a simple but not the value null");
            }
        } else if (items.get(2).getMajorType() == MajorType.BYTE_STRING) {
            payload = ((co.nstant.in.cbor.model.ByteString) items.get(2)).getBytes();
        } else {
            throw new RuntimeException("Item 2 (payload) is not nil or byte-string");
        }
        return payload;
    }

    // Returns the empty collection if no x5chain is included in the structure.
    //
    // Throws RuntimeException if the given bytes aren't valid COSE_Sign1.
    //
    public static Collection<X509Certificate> coseSign1GetX5Chain(byte[] signatureCose1)
            throws CertificateException {
        LinkedList<X509Certificate> ret = new LinkedList<>();
        ByteArrayInputStream bais = new ByteArrayInputStream(signatureCose1);
        List<DataItem> dataItems = null;
        try {
            dataItems = new CborDecoder(bais).decode();
        } catch (CborException e) {
            throw new RuntimeException("Given signature is not valid CBOR", e);
        }
        if (dataItems.size() != 1) {
            throw new RuntimeException("Expected just one data item");
        }
        DataItem dataItem = dataItems.get(0);
        if (dataItem.getMajorType() != MajorType.ARRAY) {
            throw new RuntimeException("Data item is not an array");
        }
        List<DataItem> items = ((co.nstant.in.cbor.model.Array) dataItem).getDataItems();
        if (items.size() < 4) {
            throw new RuntimeException("Expected at least four items in COSE_Sign1 array");
        }
        if (items.get(1).getMajorType() != MajorType.MAP) {
            throw new RuntimeException("Item 1 (unprocted headers) is not a map");
        }
        co.nstant.in.cbor.model.Map map = (co.nstant.in.cbor.model.Map) items.get(1);
        DataItem x5chainItem = map.get(new UnsignedInteger(COSE_LABEL_X5CHAIN));
        if (x5chainItem != null) {
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            if (x5chainItem instanceof ByteString) {
                ByteArrayInputStream certBais = new ByteArrayInputStream(
                        ((ByteString) x5chainItem).getBytes());
                ret.add((X509Certificate) factory.generateCertificate(certBais));
            } else if (x5chainItem instanceof Array) {
                for (DataItem certItem : ((Array) x5chainItem).getDataItems()) {
                    if (!(certItem instanceof ByteString)) {
                        throw new RuntimeException(
                                "Unexpected type for array item in x5chain value");
                    }
                    ByteArrayInputStream certBais = new ByteArrayInputStream(
                            ((ByteString) certItem).getBytes());
                    ret.add((X509Certificate) factory.generateCertificate(certBais));
                }
            } else {
                throw new RuntimeException("Unexpected type for x5chain value");
            }
        }
        return ret;
    }

    public static byte[] coseBuildToBeMACed(byte[] encodedProtectedHeaders,
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

        return encodeCbor(macStructure.build());
    }

    public static byte[] coseMac0(SecretKey key,
            @Nullable byte[] data,
            byte[] detachedContent)
            throws NoSuchAlgorithmException, InvalidKeyException, CertificateEncodingException {

        int dataLen = (data != null ? data.length : 0);
        int detachedContentLen = (detachedContent != null ? detachedContent.length : 0);
        if (dataLen > 0 && detachedContentLen > 0) {
            throw new RuntimeException("data and detachedContent cannot both be non-empty");
        }

        CborBuilder protectedHeaders = new CborBuilder();
        MapBuilder<CborBuilder> protectedHeadersMap = protectedHeaders.addMap();
        protectedHeadersMap.put(COSE_LABEL_ALG, COSE_ALG_HMAC_256_256);
        byte[] protectedHeadersBytes = encodeCbor(protectedHeaders.build());

        byte[] toBeMACed = coseBuildToBeMACed(protectedHeadersBytes, data, detachedContent);

        byte[] mac = null;
        Mac m = Mac.getInstance("HmacSHA256");
        m.init(key);
        m.update(toBeMACed);
        mac = m.doFinal();

        CborBuilder builder = new CborBuilder();
        ArrayBuilder<CborBuilder> array = builder.addArray();
        array.add(protectedHeadersBytes);
        MapBuilder<ArrayBuilder<CborBuilder>> unprotectedHeaders = array.addMap();
        if (data == null || data.length == 0) {
            array.add(new SimpleValue(SimpleValueType.NULL));
        } else {
            array.add(data);
        }
        array.add(mac);

        return encodeCbor(builder.build());
    }

    public static String replaceLine(String text, int lineNumber, String replacementLine) {
        String[] lines = text.split("\n");
        int numLines = lines.length;
        if (lineNumber < 0) {
            lineNumber = numLines - (-lineNumber);
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
                if (text.endsWith(("\n"))) {
                    sb.append('\n');
                }
            } else {
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    static byte[] cborEncode(DataItem dataItem) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            new CborEncoder(baos).encode(dataItem);
        } catch (CborException e) {
            // This should never happen and we don't want cborEncode() to throw since that
            // would complicate all callers. Log it instead.
            e.printStackTrace();
            Log.e(TAG, "Error encoding DataItem");
        }
        return baos.toByteArray();
    }

    static byte[] cborEncodeBoolean(boolean value) {
        return cborEncode(new CborBuilder().add(value).build().get(0));
    }

    static byte[] cborEncodeBytestring(@NonNull byte[] value) {
        return cborEncode(new CborBuilder().add(value).build().get(0));
    }


    static byte[] cborEncodeString(@NonNull String value) {
        return cborEncode(new CborBuilder().add(value).build().get(0));
    }

    static byte[] cborEncodeInt(long value) {
        return cborEncode(new CborBuilder().add(value).build().get(0));
    }

    static String getStringEntry(ResultData data, String namespaceName, String name) {
        return Util.cborDecodeString(data.getEntry(namespaceName, name));
    }

    static boolean getBooleanEntry(ResultData data, String namespaceName, String name) {
        return Util.cborDecodeBoolean(data.getEntry(namespaceName, name));
    }

    static long getIntegerEntry(ResultData data, String namespaceName, String name) {
        return Util.cborDecodeInt(data.getEntry(namespaceName, name));
    }

    static byte[] getBytestringEntry(ResultData data, String namespaceName, String name) {
        return Util.cborDecodeBytestring(data.getEntry(namespaceName, name));
    }

    static final int CBOR_SEMANTIC_TAG_ENCODED_CBOR = 24;

    static byte[] cborEncodeCalendar(@NonNull Calendar calendar) {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        if (calendar.isSet(Calendar.MILLISECOND) && calendar.get(Calendar.MILLISECOND) != 0) {
            df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        }
        df.setTimeZone(calendar.getTimeZone());
        Date val = calendar.getTime();
        String dateString = df.format(val);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            new CborEncoder(baos).encode(new CborBuilder()
                    .addTag(0)
                    .add(dateString)
                    .build());
        } catch (CborException e) {
            // Should never happen and we don't want to complicate callers by throwing.
            e.printStackTrace();
            Log.e(TAG, "Error encoding Calendar");
        }
        byte[] data = baos.toByteArray();
        return data;
    }

    static DataItem cborToDataItem(byte[] data) {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        try {
            List<DataItem> dataItems = new CborDecoder(bais).decode();
            if (dataItems.size() != 1) {
                throw new RuntimeException("Expected 1 item, found " + dataItems.size());
            }
            return dataItems.get(0);
        } catch (CborException e) {
            throw new RuntimeException("Error decoding data", e);
        }
    }

    static boolean cborDecodeBoolean(@NonNull byte[] data) {
        return cborToDataItem(data) == SimpleValue.TRUE;
    }

    static String cborDecodeString(@NonNull byte[] data) {
        return ((co.nstant.in.cbor.model.UnicodeString) cborToDataItem(data)).getString();
    }

    static long cborDecodeInt(@NonNull byte[] data) {
        return ((co.nstant.in.cbor.model.Number) cborToDataItem(data)).getValue().longValue();
    }

    static byte[] cborDecodeBytestring(@NonNull byte[] data) {
        return ((co.nstant.in.cbor.model.ByteString) cborToDataItem(data)).getBytes();
    }

    static Calendar cborDecodeCalendar(@NonNull byte[] data) {
        DataItem di = cborToDataItem(data);
        if (!(di instanceof co.nstant.in.cbor.model.UnicodeString)) {
            throw new RuntimeException("Passed in data is not a Unicode-string");
        }
        if (!di.hasTag() || di.getTag().getValue() != 0) {
            throw new RuntimeException("Passed in data is not tagged with tag 0");
        }
        String dateString = ((co.nstant.in.cbor.model.UnicodeString) di).getString();

        // Manually parse the timezone
        TimeZone parsedTz = TimeZone.getTimeZone("UTC");
        if (!dateString.endsWith("Z")) {
            String timeZoneSubstr = dateString.substring(dateString.length() - 6);
            parsedTz = TimeZone.getTimeZone("GMT" + timeZoneSubstr);
        }

        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US);
        df.setTimeZone(parsedTz);
        Date date = null;
        try {
            date = df.parse(dateString);
        } catch (ParseException e) {
            // Try again, this time without the milliseconds
            df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            df.setTimeZone(parsedTz);
            try {
                date = df.parse(dateString);
            } catch (ParseException e2) {
                throw new RuntimeException("Error parsing string", e2);
            }
        }

        Calendar c = new GregorianCalendar();
        c.clear();
        c.setTimeZone(df.getTimeZone());
        c.setTime(date);
        return c;
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
            throw new RuntimeException("Error signing public key with private key", e);
        }
    }

    static byte[] prependSemanticTagForEncodedCbor(byte[] encodedCbor) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ByteString taggedBytestring = new ByteString(encodedCbor);
            taggedBytestring.setTag(CBOR_SEMANTIC_TAG_ENCODED_CBOR);
            new CborEncoder(baos).encode(taggedBytestring);
        } catch (CborException e) {
            throw new RuntimeException("Error encoding with semantic tag for CBOR encoding", e);
        }
        return baos.toByteArray();
    }

    static byte[] buildDeviceAuthenticationCbor(String docType,
            byte[] encodedSessionTranscript,
            byte[] encodedDeviceNameSpaces) {
        ByteArrayOutputStream daBaos = new ByteArrayOutputStream();
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(encodedSessionTranscript);
            List<DataItem> dataItems = null;
            dataItems = new CborDecoder(bais).decode();
            DataItem sessionTranscript = dataItems.get(0);
            ByteString deviceNameSpacesBytesItem = new ByteString(encodedDeviceNameSpaces);
            deviceNameSpacesBytesItem.setTag(CBOR_SEMANTIC_TAG_ENCODED_CBOR);
            new CborEncoder(daBaos).encode(new CborBuilder()
                    .addArray()
                    .add("DeviceAuthentication")
                    .add(sessionTranscript)
                    .add(docType)
                    .add(deviceNameSpacesBytesItem)
                    .end()
                    .build());
        } catch (CborException e) {
            throw new RuntimeException("Error encoding DeviceAuthentication", e);
        }
        return daBaos.toByteArray();
    }

    static byte[] buildReaderAuthenticationCbor(
            byte[] sessionTranscriptBytes,
            byte[] requestMessageBytes) {

        ByteArrayOutputStream daBaos = new ByteArrayOutputStream();
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(sessionTranscriptBytes);
            List<DataItem> dataItems = null;
            dataItems = new CborDecoder(bais).decode();
            DataItem sessionTranscript = dataItems.get(0);
            ByteString requestMessageBytesItem = new ByteString(requestMessageBytes);
            requestMessageBytesItem.setTag(CBOR_SEMANTIC_TAG_ENCODED_CBOR);
            new CborEncoder(daBaos).encode(new CborBuilder()
                    .addArray()
                    .add("ReaderAuthentication")
                    .add(sessionTranscript)
                    .add(requestMessageBytesItem)
                    .end()
                    .build());
        } catch (CborException e) {
            throw new RuntimeException("Error encoding ReaderAuthentication", e);
        }
        return daBaos.toByteArray();
    }

    static byte[] concatArrays(byte[] a, byte[] b) {
        byte[] ret = new byte[a.length + b.length];
        System.arraycopy(a, 0, ret, 0, a.length);
        System.arraycopy(b, 0, ret, a.length, b.length);
        return ret;
    }

    static SecretKey calcEMacKeyForReader(PublicKey authenticationPublicKey,
            PrivateKey ephemeralReaderPrivateKey,
            byte[] encodedSessionTranscript) {
        try {
            KeyAgreement ka = KeyAgreement.getInstance("ECDH");
            ka.init(ephemeralReaderPrivateKey);
            ka.doPhase(authenticationPublicKey, true);
            byte[] sharedSecret = ka.generateSecret();

            byte[] sessionTranscriptBytes =
                    Util.prependSemanticTagForEncodedCbor(encodedSessionTranscript);
            byte[] sharedSecretWithSessionTranscriptBytes =
                    Util.concatArrays(sharedSecret, sessionTranscriptBytes);

            byte[] salt = new byte[1];
            byte[] info = new byte[0];

            salt[0] = 0x00;
            byte[] derivedKey = Util.computeHkdf("HmacSha256",
                    sharedSecretWithSessionTranscriptBytes, salt, info, 32);
            SecretKey secretKey = new SecretKeySpec(derivedKey, "");
            return secretKey;
        } catch (InvalidKeyException
                | NoSuchAlgorithmException e) {
            throw new RuntimeException("Error performing key agreement", e);
        }
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
            throw new RuntimeException("No such algorithm: " + macAlgorithm, e);
        }
        if (size > 255 * mac.getMacLength()) {
            throw new RuntimeException("size too large");
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
            throw new RuntimeException("Error MACing", e);
        }
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

        baos = new ByteArrayOutputStream();
        try {
            new CborEncoder(baos).encode(new CborBuilder()
                    .addArray()
                    .add(blobWithCoords)
                    .end()
                    .build());
        } catch (CborException e) {
            e.printStackTrace();
            return null;
        }
        ByteString encodedDeviceEngagementItem = new ByteString(baos.toByteArray());
        ByteString encodedEReaderKeyItem = new ByteString(cborEncodeString("doesn't matter"));
        encodedDeviceEngagementItem.setTag(CBOR_SEMANTIC_TAG_ENCODED_CBOR);
        encodedEReaderKeyItem.setTag(CBOR_SEMANTIC_TAG_ENCODED_CBOR);

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
            @NonNull Map<String, Collection<String>> entriesToRequest,
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

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        CborEncoder encoder = new CborEncoder(baos);
        try {
            encoder.encode(builder.build());
        } catch (CborException e) {
            throw new RuntimeException("Error encoding CBOR", e);
        }
        return baos.toByteArray();
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
            throw new RuntimeException("Error generating ephemeral key-pair", e);
        }
    }
}
