/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.kruth;

import static androidx.kruth.FailureAssertionsKt.assertFailureValue;
import static androidx.kruth.Kruth.assertThat;

import com.google.common.collect.ImmutableSet;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for comparisons between various integral types.
 *
 * @author David Saff
 * @author Christian Gruber
 * @author Kurt Alfred Kluever
 */
@RunWith(JUnit4.class)
public class NumericComparisonTest {

    @Rule
    public ExpectFailure expectFailure = new ExpectFailure();

    @SuppressWarnings("TruthSelfEquals")
    @Test
    public void testPrimitivesVsBoxedPrimitivesVsObject_int() {
        int int42 = 42;
        Integer integer42 = 42;
        Object object42 = (Object) 42;

        assertThat(int42).isEqualTo(int42);
        assertThat(integer42).isEqualTo(int42);
        assertThat(object42).isEqualTo(int42);

        assertThat(int42).isEqualTo(integer42);
        assertThat(integer42).isEqualTo(integer42);
        assertThat(object42).isEqualTo(integer42);

        assertThat(int42).isEqualTo(object42);
        assertThat(integer42).isEqualTo(object42);
        assertThat(object42).isEqualTo(object42);
    }

    @SuppressWarnings("TruthSelfEquals")
    @Test
    public void testPrimitivesVsBoxedPrimitivesVsObject_long() {
        long longPrim42 = 42;
        Long long42 = (long) 42;
        Object object42 = (Object) 42L;

        assertThat(longPrim42).isEqualTo(longPrim42);
        assertThat(long42).isEqualTo(longPrim42);
        assertThat(object42).isEqualTo(longPrim42);

        assertThat(longPrim42).isEqualTo(long42);
        assertThat(long42).isEqualTo(long42);
        assertThat(object42).isEqualTo(long42);

        assertThat(longPrim42).isEqualTo(object42);
        assertThat(long42).isEqualTo(object42);
        assertThat(object42).isEqualTo(object42);
    }

    @Test
    public void testAllCombinations_pass() {
        assertThat(42).isEqualTo(42L);
        assertThat(42).isEqualTo(Long.valueOf(42L));
        assertThat(Integer.valueOf(42)).isEqualTo(42L);
        assertThat(Integer.valueOf(42)).isEqualTo(Long.valueOf(42L));
        assertThat(42L).isEqualTo(42);
        assertThat(42L).isEqualTo(Integer.valueOf(42));
        assertThat(Long.valueOf(42L)).isEqualTo(42);
        assertThat(Long.valueOf(42L)).isEqualTo(Integer.valueOf(42));

        assertThat(42).isEqualTo(42);
        assertThat(42).isEqualTo(Integer.valueOf(42));
        assertThat(Integer.valueOf(42)).isEqualTo(42);
        assertThat(Integer.valueOf(42)).isEqualTo(Integer.valueOf(42));
        assertThat(42L).isEqualTo(42L);
        assertThat(42L).isEqualTo(Long.valueOf(42L));
        assertThat(Long.valueOf(42L)).isEqualTo(42L);
        assertThat(Long.valueOf(42L)).isEqualTo(Long.valueOf(42L));
    }

    @Test
    public void testNumericTypeWithSameValue_shouldBeEqual_int_long() {
        expectFailureWhenTestingThat(42).isNotEqualTo(42L);
    }

    @Test
    public void testNumericTypeWithSameValue_shouldBeEqual_int_int() {
        expectFailureWhenTestingThat(42).isNotEqualTo(42);
    }

    @Test
    public void testNumericPrimitiveTypes_isNotEqual_shouldFail_intToChar() {
        expectFailureWhenTestingThat(42).isNotEqualTo((char) 42);
        // 42 in ASCII is '*'
        assertFailureValue(expectFailure, "expected not to be", "*");
        assertFailureValue(expectFailure, "but was; string representation of actual value", "42");
    }

    @Test
    public void testNumericPrimitiveTypes_isNotEqual_shouldFail_charToInt() {
        // Uses Object overload rather than Integer.
        expectFailure.whenTesting().that((char) 42).isNotEqualTo(42);
        // 42 in ASCII is '*'
        assertFailureValue(expectFailure, "expected not to be", "42");
        assertFailureValue(expectFailure, "but was; string representation of actual value", "*");
    }

    private static final Subject.Factory<Subject<Object>, Object> DEFAULT_SUBJECT_FACTORY =
            Subject::new;

    private static void expectFailure(
            ExpectFailure.SimpleSubjectBuilderCallback<Subject<Object>, Object> callback) {
        AssertionError unused = ExpectFailure.expectFailureAbout(DEFAULT_SUBJECT_FACTORY, callback);
    }

    @Test
    public void testNumericPrimitiveTypes() {
        byte byte42 = (byte) 42;
        short short42 = (short) 42;
        char char42 = (char) 42;
        int int42 = 42;
        long long42 = (long) 42;

        ImmutableSet<Object> fortyTwos =
                ImmutableSet.<Object>of(byte42, short42, char42, int42, long42);
        for (Object actual : fortyTwos) {
            for (Object expected : fortyTwos) {
                assertThat(actual).isEqualTo(expected);
            }
        }

        ImmutableSet<Object> fortyTwosNoChar = ImmutableSet.<Object>of(byte42, short42, int42,
                long42);
        for (Object actual : fortyTwosNoChar) {
            for (Object expected : fortyTwosNoChar) {
                ExpectFailure.SimpleSubjectBuilderCallback<Subject<Object>, Object> actualFirst =
                        expect -> expect.that(actual).isNotEqualTo(expected);
                ExpectFailure.SimpleSubjectBuilderCallback<Subject<Object>, Object> expectedFirst =
                        expect -> expect.that(expected).isNotEqualTo(actual);
                expectFailure(actualFirst);
                expectFailure(expectedFirst);
            }
        }

        byte byte41 = (byte) 41;
        short short41 = (short) 41;
        char char41 = (char) 41;
        int int41 = 41;
        long long41 = (long) 41;

        ImmutableSet<Object> fortyOnes =
                ImmutableSet.<Object>of(byte41, short41, char41, int41, long41);

        for (Object first : fortyTwos) {
            for (Object second : fortyOnes) {
                assertThat(first).isNotEqualTo(second);
                assertThat(second).isNotEqualTo(first);
            }
        }
    }

    private IntegerSubject expectFailureWhenTestingThat(Integer actual) {
        return expectFailure.whenTesting().that(actual);
    }
}