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

package androidx.browser.trusted;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests for {@link Token}.
 */
@RunWith(JUnit4.class)
@SmallTest
public class TokenContentsTest {
    private static final String PACKAGE1 = "com.package.one";
    private static final String PACKAGE2 = "com.package.two";
    private static final byte[] FINGERPRINT1 = { 0x0, 0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x7 };
    private static final byte[] FINGERPRINT2 = { 0x8, 0x9, 0xa, 0xb, 0xc, 0xd, 0xe, 0xf };

    @Test
    public void equality() throws IOException {
        TokenContents token1 = TokenContents.create(PACKAGE1, asList(FINGERPRINT1));
        TokenContents token2 = TokenContents.create(PACKAGE1, asList(FINGERPRINT1));

        equals(token1, token2);
    }

    @Test
    public void equality_fingerprintOrder() throws IOException {
        TokenContents token1 = TokenContents.create(PACKAGE1, asList(FINGERPRINT1, FINGERPRINT2));
        TokenContents token2 = TokenContents.create(PACKAGE1, asList(FINGERPRINT2, FINGERPRINT1));

        equals(token1, token2);
    }

    @Test
    public void nonEquality_packageName() throws IOException {
        TokenContents token1 = TokenContents.create(PACKAGE1, asList(FINGERPRINT1));
        TokenContents token2 = TokenContents.create(PACKAGE2, asList(FINGERPRINT1));

        assertNotEquals(token1, token2);
    }

    @Test
    public void nonEquality_fingerprint() throws IOException {
        TokenContents token1 = TokenContents.create(PACKAGE1, asList(FINGERPRINT1));
        TokenContents token2 = TokenContents.create(PACKAGE1, asList(FINGERPRINT2));

        assertNotEquals(token1, token2);
    }

    @Test
    public void nonEquality_fingerprintNumber() throws IOException {
        TokenContents token1 = TokenContents.create(PACKAGE1, asList(FINGERPRINT1));
        TokenContents token2 = TokenContents.create(PACKAGE1, asList(FINGERPRINT1, FINGERPRINT2));

        assertNotEquals(token1, token2);
    }

    @Test
    public void serialization_equality() throws IOException {
        TokenContents token1 = TokenContents.create(PACKAGE1, asList(FINGERPRINT1, FINGERPRINT2));
        TokenContents token2 = TokenContents.deserialize(token1.serialize());

        equals(token1, token2);
    }

    @Test
    public void serialization_fingerprint() throws IOException {
        TokenContents token1 = TokenContents.create(PACKAGE1, asList(FINGERPRINT1));

        TokenContents token2 = TokenContents.create(PACKAGE1, asList(FINGERPRINT2));
        TokenContents token3 = TokenContents.deserialize(token2.serialize());

        assertNotEquals(token1, token3);
    }

    @Test
    public void serialization_packageName() throws IOException {
        TokenContents token1 = TokenContents.create(PACKAGE1, asList(FINGERPRINT1));

        TokenContents token2 = TokenContents.create(PACKAGE2, asList(FINGERPRINT1));
        TokenContents token3 = TokenContents.deserialize(token2.serialize());

        assertNotEquals(token1, token3);
    }

    @Test
    public void deserializes_packageName() throws IOException {
        TokenContents token1 = TokenContents.create(PACKAGE1, asList(FINGERPRINT1));

        assertEquals(PACKAGE1, TokenContents.deserialize(token1.serialize()).getPackageName());
    }

    @Test
    public void deserializes_fingerprints() throws IOException {
        TokenContents token1 = TokenContents.create(PACKAGE1, asList(FINGERPRINT1, FINGERPRINT2));

        TokenContents token2 = TokenContents.deserialize(token1.serialize());

        assertEquals(2, token2.getFingerprintCount());
        assertArrayEquals(token1.getFingerprint(0), token2.getFingerprint(0));
        assertArrayEquals(token1.getFingerprint(1), token2.getFingerprint(1));
    }

    private static List<byte[]> asList(byte[] ...arrays) {
        List<byte[]> list = new ArrayList<>();
        for (byte[] array: arrays) {
            list.add(array);
        }
        return list;
    }

    private static void equals(TokenContents token1, TokenContents token2) {
        assertEquals(token1, token2);
        assertEquals(token1.hashCode(), token2.hashCode());
    }
}
