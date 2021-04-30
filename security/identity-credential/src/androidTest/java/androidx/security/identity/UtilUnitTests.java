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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.icu.util.Calendar;
import android.icu.util.GregorianCalendar;
import android.icu.util.TimeZone;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.X509Certificate;
import java.util.LinkedList;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import co.nstant.in.cbor.CborBuilder;
import co.nstant.in.cbor.CborEncoder;
import co.nstant.in.cbor.CborException;
import co.nstant.in.cbor.builder.ArrayBuilder;
import co.nstant.in.cbor.model.ByteString;
import co.nstant.in.cbor.model.DataItem;
import co.nstant.in.cbor.model.DoublePrecisionFloat;
import co.nstant.in.cbor.model.HalfPrecisionFloat;
import co.nstant.in.cbor.model.NegativeInteger;
import co.nstant.in.cbor.model.SimpleValue;
import co.nstant.in.cbor.model.SimpleValueType;
import co.nstant.in.cbor.model.SinglePrecisionFloat;
import co.nstant.in.cbor.model.UnicodeString;
import co.nstant.in.cbor.model.UnsignedInteger;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class UtilUnitTests {
    @Test
    public void prettyPrintMultipleCompleteTypes() throws CborException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new CborEncoder(baos).encode(new CborBuilder()
                .add("text")                // add string
                .add(1234)                  // add integer
                .add(new byte[]{0x10})   // add byte array
                .addArray()                 // add array
                .add(1)
                .add("text")
                .end()
                .build());
        assertEquals("'text',\n"
                + "1234,\n"
                + "[0x10],\n"
                + "[1, 'text']", Util.cborPrettyPrint(baos.toByteArray()));
    }

    @Test
    public void prettyPrintString() throws CborException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new CborEncoder(baos).encode(new UnicodeString("foobar"));
        assertEquals("'foobar'", Util.cborPrettyPrint(baos.toByteArray()));
    }

    @Test
    public void prettyPrintBytestring() throws CborException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new CborEncoder(baos).encode(new ByteString(new byte[]{1, 2, 33, (byte) 254}));
        assertEquals("[0x01, 0x02, 0x21, 0xfe]", Util.cborPrettyPrint(baos.toByteArray()));
    }

    @Test
    public void prettyPrintUnsignedInteger() throws CborException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new CborEncoder(baos).encode(new UnsignedInteger(42));
        assertEquals("42", Util.cborPrettyPrint(baos.toByteArray()));
    }

    @Test
    public void prettyPrintNegativeInteger() throws CborException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new CborEncoder(baos).encode(new NegativeInteger(-42));
        assertEquals("-42", Util.cborPrettyPrint(baos.toByteArray()));
    }

    @Test
    public void prettyPrintDouble() throws CborException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new CborEncoder(baos).encode(new DoublePrecisionFloat(1.1));
        assertEquals("1.1", Util.cborPrettyPrint(baos.toByteArray()));

        baos = new ByteArrayOutputStream();
        new CborEncoder(baos).encode(new DoublePrecisionFloat(-42.0000000001));
        assertEquals("-42.0000000001", Util.cborPrettyPrint(baos.toByteArray()));

        baos = new ByteArrayOutputStream();
        new CborEncoder(baos).encode(new DoublePrecisionFloat(-5));
        assertEquals("-5", Util.cborPrettyPrint(baos.toByteArray()));
    }

    @Test
    public void prettyPrintFloat() throws CborException {
        ByteArrayOutputStream baos;

        // TODO: These two tests yield different results on different devices, disable for now
        /*
        baos = new ByteArrayOutputStream();
        new CborEncoder(baos).encode(new SinglePrecisionFloat(1.1f));
        assertEquals("1.100000023841858", Util.cborPrettyPrint(baos.toByteArray()));

        baos = new ByteArrayOutputStream();
        new CborEncoder(baos).encode(new SinglePrecisionFloat(-42.0001f));
        assertEquals("-42.000099182128906", Util.cborPrettyPrint(baos.toByteArray()));
        */

        baos = new ByteArrayOutputStream();
        new CborEncoder(baos).encode(new SinglePrecisionFloat(-5f));
        assertEquals("-5", Util.cborPrettyPrint(baos.toByteArray()));
    }

    @Test
    public void prettyPrintHalfFloat() throws CborException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new CborEncoder(baos).encode(new HalfPrecisionFloat(1.1f));
        assertEquals("1.099609375", Util.cborPrettyPrint(baos.toByteArray()));

        baos = new ByteArrayOutputStream();
        new CborEncoder(baos).encode(new HalfPrecisionFloat(-42.0001f));
        assertEquals("-42", Util.cborPrettyPrint(baos.toByteArray()));

        baos = new ByteArrayOutputStream();
        new CborEncoder(baos).encode(new HalfPrecisionFloat(-5f));
        assertEquals("-5", Util.cborPrettyPrint(baos.toByteArray()));
    }

    @Test
    public void prettyPrintFalse() throws CborException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new CborEncoder(baos).encode(new SimpleValue(SimpleValueType.FALSE));
        assertEquals("false", Util.cborPrettyPrint(baos.toByteArray()));
    }

    @Test
    public void prettyPrintTrue() throws CborException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new CborEncoder(baos).encode(new SimpleValue(SimpleValueType.TRUE));
        assertEquals("true", Util.cborPrettyPrint(baos.toByteArray()));
    }

    @Test
    public void prettyPrintNull() throws CborException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new CborEncoder(baos).encode(new SimpleValue(SimpleValueType.NULL));
        assertEquals("null", Util.cborPrettyPrint(baos.toByteArray()));
    }

    @Test
    public void prettyPrintUndefined() throws CborException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new CborEncoder(baos).encode(new SimpleValue(SimpleValueType.UNDEFINED));
        assertEquals("undefined", Util.cborPrettyPrint(baos.toByteArray()));
    }

    @Test
    public void prettyPrintTag() throws CborException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new CborEncoder(baos).encode(new CborBuilder()
                .addTag(0)
                .add("ABC")
                .build());
        byte[] data = baos.toByteArray();
        assertEquals("tag 0 'ABC'", Util.cborPrettyPrint(data));
    }

    @Test
    public void prettyPrintArrayNoCompounds() throws CborException {
        // If an array has no compound elements, no newlines are used.
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new CborEncoder(baos).encode(new CborBuilder()
                .addArray()                 // add array
                .add(1)
                .add("text")
                .add(new ByteString(new byte[]{1, 2, 3}))
                .end()
                .build());
        assertEquals("[1, 'text', [0x01, 0x02, 0x03]]", Util.cborPrettyPrint(baos.toByteArray()));
    }

    @Test
    public void prettyPrintArray() throws CborException {
        // This array contains a compound value so will use newlines
        CborBuilder array = new CborBuilder();
        ArrayBuilder<CborBuilder> arrayBuilder = array.addArray();
        arrayBuilder.add(2);
        arrayBuilder.add(3);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new CborEncoder(baos).encode(new CborBuilder()
                .addArray()                 // add array
                .add(1)
                .add("text")
                .add(new ByteString(new byte[]{1, 2, 3}))
                .add(array.build().get(0))
                .end()
                .build());
        assertEquals("[\n"
                + "  1,\n"
                + "  'text',\n"
                + "  [0x01, 0x02, 0x03],\n"
                + "  [2, 3]\n"
                + "]", Util.cborPrettyPrint(baos.toByteArray()));
    }

    @Test
    public void prettyPrintMap() throws CborException {
        // If an array has no compound elements, no newlines are used.
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new CborEncoder(baos).encode(new CborBuilder()
                .addMap()
                .put("Foo", 42)
                .put("Bar", "baz")
                .put(43, 44)
                .put(new UnicodeString("bstr"), new ByteString(new byte[]{1, 2, 3}))
                .put(new ByteString(new byte[]{1, 2, 3}), new UnicodeString("other way"))
                .end()
                .build());
        assertEquals("{\n"
                + "  43 : 44,\n"
                + "  [0x01, 0x02, 0x03] : 'other way',\n"
                + "  'Bar' : 'baz',\n"
                + "  'Foo' : 42,\n"
                + "  'bstr' : [0x01, 0x02, 0x03]\n"
                + "}", Util.cborPrettyPrint(baos.toByteArray()));
    }

    @Test
    public void cborEncodeDecode() {
        // TODO: add better coverage and check specific encoding etc.
        assertEquals(42, Util.cborDecodeLong(Util.cborEncodeNumber(42)));
        assertEquals(123456, Util.cborDecodeLong(Util.cborEncodeNumber(123456)));
        assertFalse(Util.cborDecodeBoolean(Util.cborEncodeBoolean(false)));
        assertTrue(Util.cborDecodeBoolean(Util.cborEncodeBoolean(true)));
    }

    @Test
    public void cborEncodeDecodeCalendar() throws CborException {
        GregorianCalendar c;
        byte[] data;

        c = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        c.clear();
        c.set(2019, Calendar.JULY, 8, 11, 51, 42);
        data = Util.cborEncodeDateTime(c);
        assertEquals("tag 0 '2019-07-08T11:51:42Z'", Util.cborPrettyPrint(data));
        assertEquals("tag 0 '2019-07-08T11:51:42Z'",
                Util.cborPrettyPrint(Util.cborEncodeDateTime(Util.cborDecodeDateTime(data))));
        assertEquals(0, c.compareTo(Util.cborDecodeDateTime(data)));

        c = new GregorianCalendar(TimeZone.getTimeZone("GMT-04:00"));
        c.clear();
        c.set(2019, Calendar.JULY, 8, 11, 51, 42);
        data = Util.cborEncodeDateTime(c);
        assertEquals("tag 0 '2019-07-08T11:51:42-04:00'", Util.cborPrettyPrint(data));
        assertEquals("tag 0 '2019-07-08T11:51:42-04:00'",
                Util.cborPrettyPrint(Util.cborEncodeDateTime(Util.cborDecodeDateTime(data))));
        assertEquals(0, c.compareTo(Util.cborDecodeDateTime(data)));

        c = new GregorianCalendar(TimeZone.getTimeZone("GMT-08:00"));
        c.clear();
        c.set(2019, Calendar.JULY, 8, 11, 51, 42);
        data = Util.cborEncodeDateTime(c);
        assertEquals("tag 0 '2019-07-08T11:51:42-08:00'", Util.cborPrettyPrint(data));
        assertEquals("tag 0 '2019-07-08T11:51:42-08:00'",
                Util.cborPrettyPrint(Util.cborEncodeDateTime(Util.cborDecodeDateTime(data))));
        assertEquals(0, c.compareTo(Util.cborDecodeDateTime(data)));

        c = new GregorianCalendar(TimeZone.getTimeZone("GMT+04:30"));
        c.clear();
        c.set(2019, Calendar.JULY, 8, 11, 51, 42);
        data = Util.cborEncodeDateTime(c);
        assertEquals("tag 0 '2019-07-08T11:51:42+04:30'", Util.cborPrettyPrint(data));
        assertEquals("tag 0 '2019-07-08T11:51:42+04:30'",
                Util.cborPrettyPrint(Util.cborEncodeDateTime(Util.cborDecodeDateTime(data))));
        assertEquals(0, c.compareTo(Util.cborDecodeDateTime(data)));
    }

    @Test
    public void cborCalendarMilliseconds() throws CborException {
        Calendar c = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        c.clear();
        c.set(2019, Calendar.JULY, 8, 11, 51, 42);
        c.set(Calendar.MILLISECOND, 123);
        byte[] data = Util.cborEncodeDateTime(c);
        assertEquals("tag 0 '2019-07-08T11:51:42.123Z'", Util.cborPrettyPrint(data));
        assertEquals("tag 0 '2019-07-08T11:51:42.123Z'",
                Util.cborPrettyPrint(Util.cborEncodeDateTime(Util.cborDecodeDateTime(data))));
        assertEquals(0, c.compareTo(Util.cborDecodeDateTime(data)));
    }

    @Test
    public void cborCalendarForeign() throws CborException {
        ByteArrayOutputStream baos;
        byte[] data;

        // milliseconds, non-standard format
        baos = new ByteArrayOutputStream();
        new CborEncoder(baos).encode(new CborBuilder()
                .addTag(0)
                .add("2019-07-08T11:51:42.25Z")
                .build());
        data = baos.toByteArray();
        assertEquals("tag 0 '2019-07-08T11:51:42.250Z'",
                Util.cborPrettyPrint(Util.cborEncodeDateTime(Util.cborDecodeDateTime(data))));

        // milliseconds set to 0
        baos = new ByteArrayOutputStream();
        new CborEncoder(baos).encode(new CborBuilder()
                .addTag(0)
                .add("2019-07-08T11:51:42.0Z")
                .build());
        data = baos.toByteArray();
        assertEquals("tag 0 '2019-07-08T11:51:42Z'",
                Util.cborPrettyPrint(Util.cborEncodeDateTime(Util.cborDecodeDateTime(data))));

        // we only support millisecond-precision
        baos = new ByteArrayOutputStream();
        new CborEncoder(baos).encode(new CborBuilder()
                .addTag(0)
                .add("2019-07-08T11:51:42.9876Z")
                .build());
        data = baos.toByteArray();
        assertEquals("tag 0 '2019-07-08T11:51:42.987Z'",
                Util.cborPrettyPrint(Util.cborEncodeDateTime(Util.cborDecodeDateTime(data))));

        // milliseconds and timezone
        baos = new ByteArrayOutputStream();
        new CborEncoder(baos).encode(new CborBuilder()
                .addTag(0)
                .add("2019-07-08T11:51:42.26-11:30")
                .build());
        data = baos.toByteArray();
        assertEquals("tag 0 '2019-07-08T11:51:42.260-11:30'",
                Util.cborPrettyPrint(Util.cborEncodeDateTime(Util.cborDecodeDateTime(data))));
    }

    private KeyPair coseGenerateKeyPair() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore");
        KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(
                "coseTestKeyPair",
                KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY)
                .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512);
        kpg.initialize(builder.build());
        return kpg.generateKeyPair();
    }

    @Test
    public void coseSignAndVerify() throws Exception {
        KeyPair keyPair = coseGenerateKeyPair();
        byte[] data = new byte[]{0x10, 0x11, 0x12, 0x13};
        byte[] detachedContent = new byte[]{};
        DataItem sig = Util.coseSign1Sign(keyPair.getPrivate(), data, detachedContent, null);
        assertTrue(Util.coseSign1CheckSignature(sig, detachedContent, keyPair.getPublic()));
        assertArrayEquals(data, Util.coseSign1GetData(sig));
        assertEquals(0, Util.coseSign1GetX5Chain(sig).size());
    }

    @Test
    public void coseSignAndVerifyDetachedContent() throws Exception {
        KeyPair keyPair = coseGenerateKeyPair();
        byte[] data = new byte[]{};
        byte[] detachedContent = new byte[]{0x20, 0x21, 0x22, 0x23, 0x24};
        DataItem sig = Util.coseSign1Sign(keyPair.getPrivate(), data, detachedContent, null);
        assertTrue(Util.coseSign1CheckSignature(sig, detachedContent, keyPair.getPublic()));
        assertArrayEquals(data, Util.coseSign1GetData(sig));
        assertEquals(0, Util.coseSign1GetX5Chain(sig).size());
    }

    @Test
    public void coseSignAndVerifySingleCertificate() throws Exception {
        KeyPair keyPair = coseGenerateKeyPair();
        byte[] data = new byte[]{};
        byte[] detachedContent = new byte[]{0x20, 0x21, 0x22, 0x23, 0x24};
        LinkedList<X509Certificate> certs = new LinkedList<X509Certificate>();
        certs.add(Util.signPublicKeyWithPrivateKey("coseTestKeyPair", "coseTestKeyPair"));
        DataItem sig = Util.coseSign1Sign(keyPair.getPrivate(), data, detachedContent, certs);
        assertTrue(Util.coseSign1CheckSignature(sig, detachedContent, keyPair.getPublic()));
        assertArrayEquals(data, Util.coseSign1GetData(sig));
        assertEquals(certs, Util.coseSign1GetX5Chain(sig));
    }

    @Test
    public void coseSignAndVerifyMultipleCertificates() throws Exception {
        KeyPair keyPair = coseGenerateKeyPair();
        byte[] data = new byte[]{};
        byte[] detachedContent = new byte[]{0x20, 0x21, 0x22, 0x23, 0x24};
        LinkedList<X509Certificate> certs = new LinkedList<X509Certificate>();
        certs.add(Util.signPublicKeyWithPrivateKey("coseTestKeyPair", "coseTestKeyPair"));
        certs.add(Util.signPublicKeyWithPrivateKey("coseTestKeyPair", "coseTestKeyPair"));
        certs.add(Util.signPublicKeyWithPrivateKey("coseTestKeyPair", "coseTestKeyPair"));
        DataItem sig = Util.coseSign1Sign(keyPair.getPrivate(), data, detachedContent, certs);
        assertTrue(Util.coseSign1CheckSignature(sig, detachedContent, keyPair.getPublic()));
        assertArrayEquals(data, Util.coseSign1GetData(sig));
        assertEquals(certs, Util.coseSign1GetX5Chain(sig));
    }

    @Test
    public void coseMac0() throws Exception {
        SecretKey secretKey = new SecretKeySpec(new byte[32], "");
        byte[] data = new byte[]{0x10, 0x11, 0x12, 0x13};
        byte[] detachedContent = new byte[]{};
        DataItem mac = Util.coseMac0(secretKey, data, detachedContent);
        assertEquals("[\n"
                + "  [0xa1, 0x01, 0x05],\n"
                + "  {},\n"
                + "  [0x10, 0x11, 0x12, 0x13],\n"
                + "  [0x6c, 0xec, 0xb5, 0x6a, 0xc9, 0x5c, 0xae, 0x3b, 0x41, 0x13, 0xde, 0xa4, "
                + "0xd8, 0x86, 0x5c, 0x28, 0x2c, 0xd5, 0xa5, 0x13, 0xff, 0x3b, 0xd1, 0xde, 0x70, "
                + "0x5e, 0xbb, 0xe2, 0x2d, 0x42, 0xbe, 0x53]\n"
                + "]", Util.cborPrettyPrint(mac));
    }

    @Test
    public void coseMac0DetachedContent() throws Exception {
        SecretKey secretKey = new SecretKeySpec(new byte[32], "");
        byte[] data = new byte[]{};
        byte[] detachedContent = new byte[]{0x10, 0x11, 0x12, 0x13};
        DataItem mac = Util.coseMac0(secretKey, data, detachedContent);
        // Same HMAC as in coseMac0 test, only difference is that payload is null.
        assertEquals("[\n"
                + "  [0xa1, 0x01, 0x05],\n"
                + "  {},\n"
                + "  null,\n"
                + "  [0x6c, 0xec, 0xb5, 0x6a, 0xc9, 0x5c, 0xae, 0x3b, 0x41, 0x13, 0xde, 0xa4, "
                + "0xd8, 0x86, 0x5c, 0x28, 0x2c, 0xd5, 0xa5, 0x13, 0xff, 0x3b, 0xd1, 0xde, 0x70, "
                + "0x5e, 0xbb, 0xe2, 0x2d, 0x42, 0xbe, 0x53]\n"
                + "]", Util.cborPrettyPrint(mac));
    }

    @Test
    public void replaceLineTest() {
        assertEquals("foo",
                Util.replaceLine("Hello World", 0, "foo"));
        assertEquals("foo\n",
                Util.replaceLine("Hello World\n", 0, "foo"));
        assertEquals("Hello World",
                Util.replaceLine("Hello World", 1, "foo"));
        assertEquals("Hello World\n",
                Util.replaceLine("Hello World\n", 1, "foo"));
        assertEquals("foo\ntwo\nthree",
                Util.replaceLine("one\ntwo\nthree", 0, "foo"));
        assertEquals("one\nfoo\nthree",
                Util.replaceLine("one\ntwo\nthree", 1, "foo"));
        assertEquals("one\ntwo\nfoo",
                Util.replaceLine("one\ntwo\nthree", 2, "foo"));
        assertEquals("one\ntwo\nfoo",
                Util.replaceLine("one\ntwo\nthree", -1, "foo"));
        assertEquals("one\ntwo\nthree\nfoo",
                Util.replaceLine("one\ntwo\nthree\nfour", -1, "foo"));
        assertEquals("one\ntwo\nfoo\nfour",
                Util.replaceLine("one\ntwo\nthree\nfour", -2, "foo"));
    }

}
