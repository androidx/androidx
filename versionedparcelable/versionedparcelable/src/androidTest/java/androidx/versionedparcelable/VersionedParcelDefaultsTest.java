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

package androidx.versionedparcelable;

import static androidx.versionedparcelable.ParcelUtils.fromInputStream;
import static androidx.versionedparcelable.ParcelUtils.fromParcelable;
import static androidx.versionedparcelable.ParcelUtils.toOutputStream;
import static androidx.versionedparcelable.ParcelUtils.toParcelable;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.util.Size;
import android.util.SizeF;
import android.util.SparseBooleanArray;

import androidx.collection.ArrayMap;
import androidx.collection.ArraySet;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RunWith(Parameterized.class)
@SmallTest
public class VersionedParcelDefaultsTest {

    @Parameterized.Parameters
    public static Iterable<? extends Object[]> data() {
        return Arrays.asList(new Object[][]{{false}, {true}});
    }

    private boolean mUseStream;

    public VersionedParcelDefaultsTest(boolean useStream) {
        mUseStream = useStream;
    }

    private DefaultParcelImpl parcelCopy(DefaultParcelImpl obj) {
        if (mUseStream) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            toOutputStream(obj, outputStream);
            byte[] buf = outputStream.toByteArray();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(buf);
            return fromInputStream(inputStream);
        } else {
            Parcel p = Parcel.obtain();
            p.writeParcelable(toParcelable(obj), 0);
            p.setDataPosition(0);
            return fromParcelable(p.readParcelable(getClass().getClassLoader()));
        }
    }

    @Test
    public void testCustomParcelCallbacks() {
        DefaultParcelImpl obj = new DefaultParcelImpl();
        assertFalse(obj.mPreParcelled);
        assertFalse(obj.mPostParcelled);

        DefaultParcelImpl other = parcelCopy(obj);
        assertTrue(obj.mPreParcelled);
        assertTrue(other.mPostParcelled);
    }

    @Test
    public void testInts() {
        DefaultParcelImpl obj = new DefaultParcelImpl();
        obj.mInt = 42;
        DefaultParcelImpl other = parcelCopy(obj);
        assertEquals(obj.mInt, other.mInt);
    }

    @Test
    public void testMultipleFields() {
        DefaultParcelImpl obj = new DefaultParcelImpl();
        obj.mBoolean = true;
        obj.mFloat = 42;
        obj.mDouble = 15;
        obj.mLong = 68;
        obj.mString = "my_string_123";
        DefaultParcelImpl other = parcelCopy(obj);
        assertEquals(obj.mBoolean, other.mBoolean);
        assertEquals(obj.mFloat, other.mFloat, .01f);
        assertEquals(obj.mDouble, other.mDouble, .01f);
        assertEquals(obj.mLong, other.mLong);
        assertEquals(obj.mString, other.mString);
    }

    @Test
    public void testBoolean() {
        DefaultParcelImpl obj = new DefaultParcelImpl();
        obj.mBoolean = true;
        DefaultParcelImpl other = parcelCopy(obj);
        assertEquals(obj.mBoolean, other.mBoolean);
    }

    @Test
    public void testFloat() {
        DefaultParcelImpl obj = new DefaultParcelImpl();
        obj.mFloat = 42;
        DefaultParcelImpl other = parcelCopy(obj);
        assertEquals(obj.mFloat, other.mFloat, .01f);
    }

    @Test
    public void testDouble() {
        DefaultParcelImpl obj = new DefaultParcelImpl();
        obj.mDouble = 42;
        DefaultParcelImpl other = parcelCopy(obj);
        assertEquals(obj.mDouble, other.mDouble, .01f);
    }

    @Test
    public void testLong() {
        DefaultParcelImpl obj = new DefaultParcelImpl();
        obj.mLong = 42;
        DefaultParcelImpl other = parcelCopy(obj);
        assertEquals(obj.mLong, other.mLong);
    }

    @Test
    public void testString() {
        DefaultParcelImpl obj = new DefaultParcelImpl();
        obj.mString = "42";
        DefaultParcelImpl other = parcelCopy(obj);
        assertEquals(obj.mString, other.mString);
    }

    @Test
    public void testBinder() {
        if (mUseStream) {
            return;
        }
        DefaultParcelImpl obj = new DefaultParcelImpl();
        obj.mBinder = new Binder();
        DefaultParcelImpl other = parcelCopy(obj);
        assertEquals(obj.mBinder, other.mBinder);
    }

    @Test
    public void testByteArray() {
        DefaultParcelImpl obj = new DefaultParcelImpl();
        obj.mByteArray = new byte[]{4, 2};
        DefaultParcelImpl other = parcelCopy(obj);
        assertArrayEquals(obj.mByteArray, other.mByteArray);
    }

    @Test
    public void testByte() {
        DefaultParcelImpl obj = new DefaultParcelImpl();
        obj.mByte = (byte) 42;
        DefaultParcelImpl other = parcelCopy(obj);
        assertEquals(obj.mByte, other.mByte);
    }

    @Test
    public void testBundle() {
        DefaultParcelImpl obj = new DefaultParcelImpl();
        obj.mBundle = new Bundle();
        obj.mBundle.putInt("my_string", 42);
        obj.mBundle.putString("my_int", "42");
        DefaultParcelImpl other = parcelCopy(obj);
        assertEquals(42, other.mBundle.getInt("my_string"));
        assertEquals("42", other.mBundle.getString("my_int"));
    }

    @Test
    public void testBooleanArray() {
        DefaultParcelImpl obj = new DefaultParcelImpl();
        obj.mBoolArray = new boolean[]{true, false, true};
        DefaultParcelImpl other = parcelCopy(obj);
        assertArrayEquals(obj.mBoolArray, other.mBoolArray);
    }

    @Test
    public void testCharArray() {
        DefaultParcelImpl obj = new DefaultParcelImpl();
        obj.mCharArray = new char[]{'a', 'Z'};
        DefaultParcelImpl other = parcelCopy(obj);
        assertArrayEquals(obj.mCharArray, other.mCharArray);
    }

    @Test
    public void testIntArray() {
        DefaultParcelImpl obj = new DefaultParcelImpl();
        obj.mIntArray = new int[]{42, 24, 16};
        DefaultParcelImpl other = parcelCopy(obj);
        assertArrayEquals(obj.mIntArray, other.mIntArray);
    }

    @Test
    public void testLongArray() {
        DefaultParcelImpl obj = new DefaultParcelImpl();
        obj.mLongArray = new long[]{1000L, 1312};
        DefaultParcelImpl other = parcelCopy(obj);
        assertArrayEquals(obj.mLongArray, other.mLongArray);
    }

    @Test
    public void testFloatArray() {
        DefaultParcelImpl obj = new DefaultParcelImpl();
        obj.mFloatArray = new float[]{1.5f, 2.5f};
        DefaultParcelImpl other = parcelCopy(obj);
        assertArrayEquals(obj.mFloatArray, other.mFloatArray, .01f);
    }

    @Test
    public void testDoubleArray() {
        DefaultParcelImpl obj = new DefaultParcelImpl();
        obj.mDoubleArray = new double[]{1.5, 2.5, 3.5};
        DefaultParcelImpl other = parcelCopy(obj);
        assertArrayEquals(obj.mDoubleArray, other.mDoubleArray, .01);
    }

    @Test
    public void testBinderArray() {
        if (mUseStream) {
            return;
        }
        DefaultParcelImpl obj = new DefaultParcelImpl();
        obj.mBinderArray = new IBinder[]{new Binder(), new Binder()};
        DefaultParcelImpl other = parcelCopy(obj);
        assertArrayEquals(obj.mBinderArray, other.mBinderArray);
    }

    @Test
    public void testException() {
        DefaultParcelImpl obj = new DefaultParcelImpl();
        obj.mException = new IllegalArgumentException();
        DefaultParcelImpl other = parcelCopy(obj);
        assertNotNull(other.mException);
        assertEquals(obj.mException.getClass(), other.mException.getClass());
    }

    @Test
    public void testSize() {
        // No Size until Lollipop.
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) return;
        DefaultParcelImpl obj = new DefaultParcelImpl();
        obj.mSize = new Size(4, 2);
        DefaultParcelImpl other = parcelCopy(obj);
        assertEquals(obj.mSize, other.mSize);
    }

    @Test
    public void testSizeF() {
        // No Size until Lollipop.
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) return;
        DefaultParcelImpl obj = new DefaultParcelImpl();
        obj.mSizeF = new SizeF(4.2f, 5);
        DefaultParcelImpl other = parcelCopy(obj);
        assertEquals(obj.mSizeF, other.mSizeF);
    }

    @Test
    public void testSparseBooleanArray() {
        DefaultParcelImpl obj = new DefaultParcelImpl();
        obj.mSparseBooleanArray = new SparseBooleanArray();
        obj.mSparseBooleanArray.put(42, true);
        obj.mSparseBooleanArray.put(15, true);
        obj.mSparseBooleanArray.put(23, false);
        DefaultParcelImpl other = parcelCopy(obj);
        assertEquivalent(obj.mSparseBooleanArray, other.mSparseBooleanArray);
    }

    private void assertEquivalent(SparseBooleanArray first, SparseBooleanArray second) {
        assertEquals(first.size(), second.size());
        for (int i = 0; i < first.size(); i++) {
            assertTrue(second.indexOfKey(first.keyAt(i)) >= 0);
            assertEquals(first.valueAt(i), second.get(first.keyAt(i)));
        }
    }

    @Test
    public void testParcelable() {
        if (mUseStream) {
            return;
        }
        DefaultParcelImpl obj = new DefaultParcelImpl();
        obj.mParcelable = new Intent("my.action.ACTION")
                .addCategory("has.a.CATEGORY")
                .setData(Uri.parse("something://authority/with/some/stuff"));
        DefaultParcelImpl other = parcelCopy(obj);
        assertEquals(obj.mParcelable.toString(), other.mParcelable.toString());
    }

    @Test
    public void testStringList() {
        DefaultParcelImpl obj = new DefaultParcelImpl();
        obj.mStringList = Arrays.asList("string_1", "42");
        DefaultParcelImpl other = parcelCopy(obj);
        assertEquals(obj.mStringList, other.mStringList);
    }

    @Test
    public void testStringSet() {
        DefaultParcelImpl obj = new DefaultParcelImpl();
        obj.mStringSet = new ArraySet<>(Arrays.asList("string_1", "42"));
        DefaultParcelImpl other = parcelCopy(obj);
        assertEquals(obj.mStringSet, other.mStringSet);
    }

    @Test
    public void testBinderList() {
        if (mUseStream) {
            return;
        }
        DefaultParcelImpl obj = new DefaultParcelImpl();
        obj.mBinderList = Arrays.asList((IBinder) new Binder(), new Binder());
        DefaultParcelImpl other = parcelCopy(obj);
        assertEquals(obj.mBinderList, other.mBinderList);
    }

    @Test
    public void testBundleTypes() {
        DefaultParcelImpl obj = new DefaultParcelImpl();
        obj.mBundle = new Bundle();
        Bundle subBundle = new Bundle();
        subBundle.putString("sub_bundle_string", "a_string");
        obj.mBundle.putBundle("sub_bundle", subBundle);
        obj.mBundle.putStringArray("string_array", new String[]{"1", "2", "3"});
        obj.mBundle.putBoolean("bool", true);
        obj.mBundle.putBooleanArray("bool_array", new boolean[]{true, false, true});
        obj.mBundle.putDouble("double", 4.2f);
        obj.mBundle.putDoubleArray("double_array", new double[]{4.2f, 1.2f});
        obj.mBundle.putIntArray("int_array", new int[]{4, 1, 15});
        obj.mBundle.putLong("long", 123456789L);
        obj.mBundle.putLongArray("long_array", new long[]{1, 2, 3, 4, 5});
        obj.mBundle.putFloat("float", 1.234f);
        obj.mBundle.putFloatArray("float_array", new float[]{1.2f, 3.4f, 5.6f});

        DefaultParcelImpl other = parcelCopy(obj);

        assertEquals("a_string",
                other.mBundle.getBundle("sub_bundle").getString("sub_bundle_string"));
        assertArrayEquals(new String[]{"1", "2", "3"},
                other.mBundle.getStringArray("string_array"));
        assertEquals(true, other.mBundle.getBoolean("bool"));
        assertArrayEquals(new boolean[]{true, false, true},
                other.mBundle.getBooleanArray("bool_array"));
        assertEquals(4.2f, other.mBundle.getDouble("double"), .01f);
        assertArrayEquals(new double[]{4.2f, 1.2f}, other.mBundle.getDoubleArray("double_array"),
                .01f);
        assertArrayEquals(new int[]{4, 1, 15}, other.mBundle.getIntArray("int_array"));
        assertEquals(123456789L, other.mBundle.getLong("long"));
        assertArrayEquals(new long[]{1, 2, 3, 4, 5}, other.mBundle.getLongArray("long_array"));

        assertEquals(1.234f, other.mBundle.getFloat("float"), .01f);
        float[] floatArray = other.mBundle.getFloatArray("float_array");
        assertEquals(3, floatArray.length);
        assertEquals(1.2f, floatArray[0], .01f);
        assertEquals(3.4f, floatArray[1], .01f);
        assertEquals(5.6f, floatArray[2], .01f);
    }

    @Test
    public void testIntList() {
        DefaultParcelImpl obj = new DefaultParcelImpl();
        obj.mIntList = Arrays.asList(1, 2);
        DefaultParcelImpl other = parcelCopy(obj);
        assertEquals(obj.mIntList, other.mIntList);
    }

    @Test
    public void testFloatList() {
        DefaultParcelImpl obj = new DefaultParcelImpl();
        obj.mFloatList = Arrays.asList(1.f, 2.f);
        DefaultParcelImpl other = parcelCopy(obj);
        assertEquals(obj.mFloatList, other.mFloatList);
    }

    @Test
    public void testStringFloatMap() {
        DefaultParcelImpl obj = new DefaultParcelImpl();
        Map<String, Float> arrayMap = new ArrayMap<>();
        arrayMap.put("one", 1f);
        arrayMap.put("two", 1f);
        arrayMap.put("three", 3f);

        obj.mStringFloatMap = arrayMap;
        DefaultParcelImpl other = parcelCopy(obj);
        assertEquals(obj.mStringFloatMap, other.mStringFloatMap);
    }

    @Test
    public void testStringFloatMap_empty() {
        DefaultParcelImpl obj = new DefaultParcelImpl();
        Map<String, Float> arrayMap = new ArrayMap<>();
        obj.mStringFloatMap = arrayMap;
        DefaultParcelImpl other = parcelCopy(obj);
        assertEquals(obj.mStringFloatMap, other.mStringFloatMap);
        assertTrue(other.mStringFloatMap.isEmpty());
    }

    @Test
    public void testStringFloatMap_null() {
        DefaultParcelImpl obj = new DefaultParcelImpl();
        obj.mStringFloatMap = null;
        DefaultParcelImpl other = parcelCopy(obj);
        assertNull(other.mStringFloatMap);
    }

    @VersionedParcelize(allowSerialization = true,
            ignoreParcelables = true,
            isCustom = true,
            deprecatedIds = {5, 14})
    public static class DefaultParcelImpl extends CustomVersionedParcelable {

        @ParcelField(value = 1, defaultValue = "5")
        public int mIntField = 5;

        @NonParcelField
        public int mNonSerializedField;

        @ParcelField(value = 2, defaultValue = "false")
        public boolean mBoolean = false;
        @ParcelField(value = 3, defaultValue = "3")
        public int mInt = 3;
        @ParcelField(value = 4, defaultValue = "4")
        public long mLong = 4;
        @ParcelField(value = 50, defaultValue = "1.2f")
        public float mFloat = 1.2f;
        @ParcelField(value = 6, defaultValue = "1.2")
        public double mDouble = 1.2;
        @ParcelField(value = 7, defaultValue = "Some string")
        public String mString = "Some string";
        @ParcelField(8)
        public IBinder mBinder;
        @ParcelField(9)
        public byte[] mByteArray;
        @ParcelField(10)
        public Bundle mBundle;
        @ParcelField(value = 12, defaultValue = "new boolean[0]")
        public boolean[] mBoolArray = new boolean[0];
        @ParcelField(value = 13, defaultValue = "new char[]{2, 5}")
        public char[] mCharArray = new char[]{2, 5};
        @ParcelField(140)
        public int[] mIntArray;
        @ParcelField(15)
        public long[] mLongArray;
        @ParcelField(16)
        public float[] mFloatArray;
        @ParcelField(17)
        public double[] mDoubleArray;
        @ParcelField(18)
        public IBinder[] mBinderArray = new IBinder[0];
        @ParcelField(19)
        public Exception mException;
        @ParcelField(20)
        public byte mByte;
        @ParcelField(value = 21)
        public Size mSize;
        @ParcelField(22)
        public SizeF mSizeF;
        @ParcelField(23)
        public String[] mStringArray;
        @ParcelField(24)
        public SparseBooleanArray mSparseBooleanArray;
        @ParcelField(25)
        public Intent mParcelable;
        @ParcelField(26)
        public List<String> mStringList;
        @ParcelField(27)
        public List<IBinder> mBinderList;
        @ParcelField(28)
        public Set<String> mStringSet;
        @ParcelField(29)
        public List<Integer> mIntList;
        @ParcelField(30)
        public List<Float> mFloatList;
        @ParcelField(31)
        public Map<String, Float> mStringFloatMap;

        @NonParcelField
        private boolean mPreParcelled;
        @NonParcelField
        private boolean mPostParcelled;

        @Override
        public void onPreParceling(boolean isStream) {
            mPreParcelled = true;
        }

        @Override
        public void onPostParceling() {
            mPostParcelled = true;
        }
    }
}
