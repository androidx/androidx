/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.work;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class DataTest {
    private static final String KEY1 = "key1";
    private static final String KEY2 = "key2";

    @Test
    public void testSize_noArguments() {
        Data data = new Data.Builder().build();
        assertThat(data.size(), is(0));
    }

    @Test
    public void testSize_hasArguments() {
        Data data = new Data.Builder().putBoolean(KEY1, true).build();
        assertThat(data.size(), is(1));
    }

    @Test
    public void testSerializeEmpty() {
        Data data = Data.EMPTY;

        byte[] byteArray = Data.toByteArray(data);
        Data restoredData = Data.fromByteArray(byteArray);

        assertThat(restoredData, is(data));
    }

    @Test
    public void testSerializeString() {
        String expectedValue1 = "value1";
        String expectedValue2 = "value2";
        Data data = new Data.Builder()
                .putString(KEY1, expectedValue1)
                .putString(KEY2, expectedValue2)
                .build();

        byte[] byteArray = Data.toByteArray(data);
        Data restoredData = Data.fromByteArray(byteArray);

        assertThat(restoredData, is(data));
    }

    @Test
    public void testSerializeIntArray() {
        int[] expectedValue1 = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        int[] expectedValue2 = new int[]{10, 9, 8, 7, 6, 5, 4, 3, 2, 1};
        Data data = new Data.Builder()
                .putIntArray(KEY1, expectedValue1)
                .putIntArray(KEY2, expectedValue2)
                .build();

        byte[] byteArray = Data.toByteArray(data);
        Data restoredData = Data.fromByteArray(byteArray);

        assertThat(restoredData, is(notNullValue()));
        assertThat(restoredData.size(), is(2));
        assertThat(restoredData.getIntArray(KEY1), is(equalTo(expectedValue1)));
        assertThat(restoredData.getIntArray(KEY2), is(equalTo(expectedValue2)));
    }

    @Test
    public void testSerializePastMaxSize() {
        int[] payload = new int[Data.MAX_DATA_BYTES + 1];
        Data data = new Data.Builder().putIntArray("payload", payload).build();
        boolean caughtIllegalStateException = false;
        try {
            Data.toByteArray(data);
        } catch (IllegalStateException e) {
            caughtIllegalStateException = true;
        } finally {
            assertThat(caughtIllegalStateException, is(true));
        }
    }

    @Test
    public void testDeserializePastMaxSize() {
        byte[] payload = new byte[Data.MAX_DATA_BYTES + 1];
        boolean caughtIllegalStateException = false;
        try {
            Data.fromByteArray(payload);
        } catch (IllegalStateException e) {
            caughtIllegalStateException = true;
        } finally {
            assertThat(caughtIllegalStateException, is(true));
        }
    }

    @Test
    public void testPutAll() {
        Map<String, Object> map = new HashMap<>();
        map.put("int", 1);
        map.put("float", 99f);
        map.put("String", "two");
        map.put("long array", new long[] { 1L, 2L, 3L });
        map.put("null", null);
        Data.Builder dataBuilder = new Data.Builder();
        dataBuilder.putAll(map);
        Data data = dataBuilder.build();
        assertThat(data.getInt("int", 0), is(1));
        assertThat(data.getFloat("float", 0f), is(99f));
        assertThat(data.getString("String"), is("two"));
        long[] longArray = data.getLongArray("long array");
        assertThat(longArray, is(notNullValue()));
        assertThat(longArray.length, is(3));
        assertThat(longArray[0], is(1L));
        assertThat(longArray[1], is(2L));
        assertThat(longArray[2], is(3L));
        assertThat(data.getString("null"), is(nullValue()));
    }

    @Test
    public void testPutAllWithInvalidTypes() {
        Map<String, Object> map = new HashMap<>();
        map.put("key", new Object());
        boolean caughtIllegalArgumentException = false;
        try {
            new Data.Builder().putAll(map);
        } catch (IllegalArgumentException e) {
            caughtIllegalArgumentException = true;
        }
        assertThat(caughtIllegalArgumentException, is(true));
    }
}
