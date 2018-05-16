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

package androidx.safeparcelable;

import static androidx.safeparcelable.ParcelUtils.fromInputStream;
import static androidx.safeparcelable.ParcelUtils.fromParcelable;
import static androidx.safeparcelable.ParcelUtils.toOutputStream;
import static androidx.safeparcelable.ParcelUtils.toParcelable;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.support.test.filters.SmallTest;
import android.util.Size;
import android.util.SizeF;
import android.util.SparseBooleanArray;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;

@RunWith(Parameterized.class)
@SmallTest
public class SafeParcelIntegTest {

    @Parameterized.Parameters
    public static Iterable<? extends Object[]> data() {
        return Arrays.asList(new Object[][]{{false}, {true}});
    }

    private boolean mUseStream;

    public SafeParcelIntegTest(boolean useStream) {
        mUseStream = useStream;
    }

    private ParcelizableImpl parcelCopy(ParcelizableImpl obj) {
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
    public void testInts() {
        ParcelizableImpl obj = new ParcelizableImpl();
        obj.mInt = 42;
        ParcelizableImpl other = parcelCopy(obj);
        assertEquals(obj.mInt, other.mInt);
    }


    @Test
    public void testMultipleFields() {
        ParcelizableImpl obj = new ParcelizableImpl();
        obj.mBoolean = true;
        obj.mFloat = 42;
        obj.mDouble = 15;
        obj.mLong = 68;
        obj.mString = "my_string_123";
        ParcelizableImpl other = parcelCopy(obj);
        assertEquals(obj.mBoolean, other.mBoolean);
        assertEquals(obj.mFloat, other.mFloat, .01f);
        assertEquals(obj.mDouble, other.mDouble, .01f);
        assertEquals(obj.mLong, other.mLong);
        assertEquals(obj.mString, other.mString);
    }

    @Test
    public void testBoolean() {
        ParcelizableImpl obj = new ParcelizableImpl();
        obj.mBoolean = true;
        ParcelizableImpl other = parcelCopy(obj);
        assertEquals(obj.mBoolean, other.mBoolean);
    }

    @Test
    public void testFloat() {
        ParcelizableImpl obj = new ParcelizableImpl();
        obj.mFloat = 42;
        ParcelizableImpl other = parcelCopy(obj);
        assertEquals(obj.mFloat, other.mFloat, .01f);
    }

    @Test
    public void testDouble() {
        ParcelizableImpl obj = new ParcelizableImpl();
        obj.mDouble = 42;
        ParcelizableImpl other = parcelCopy(obj);
        assertEquals(obj.mDouble, other.mDouble, .01f);
    }

    @Test
    public void testLong() {
        ParcelizableImpl obj = new ParcelizableImpl();
        obj.mLong = 42;
        ParcelizableImpl other = parcelCopy(obj);
        assertEquals(obj.mLong, other.mLong);
    }

    @Test
    public void testString() {
        ParcelizableImpl obj = new ParcelizableImpl();
        obj.mString = "42";
        ParcelizableImpl other = parcelCopy(obj);
        assertEquals(obj.mString, other.mString);
    }

    @Test
    public void testBinder() {
        if (mUseStream) {
            return;
        }
        ParcelizableImpl obj = new ParcelizableImpl();
        obj.mBinder = new Binder();
        ParcelizableImpl other = parcelCopy(obj);
        assertEquals(obj.mBinder, other.mBinder);
    }

    @Test
    public void testByteArray() {
        ParcelizableImpl obj = new ParcelizableImpl();
        obj.mByteArray = new byte[]{4, 2};
        ParcelizableImpl other = parcelCopy(obj);
        assertArrayEquals(obj.mByteArray, other.mByteArray);
    }

    @Test
    public void testByte() {
        ParcelizableImpl obj = new ParcelizableImpl();
        obj.mByte = (byte) 42;
        ParcelizableImpl other = parcelCopy(obj);
        assertEquals(obj.mByte, other.mByte);
    }

    @Test
    public void testBundle() {
        ParcelizableImpl obj = new ParcelizableImpl();
        obj.mBundle = new Bundle();
        obj.mBundle.putInt("my_string", 42);
        obj.mBundle.putString("my_int", "42");
        ParcelizableImpl other = parcelCopy(obj);
        assertEquals(42, other.mBundle.getInt("my_string"));
        assertEquals("42", other.mBundle.getString("my_int"));
    }

    @Test
    public void testBooleanArray() {
        ParcelizableImpl obj = new ParcelizableImpl();
        obj.mBoolArray = new boolean[]{true, false, true};
        ParcelizableImpl other = parcelCopy(obj);
        assertArrayEquals(obj.mBoolArray, other.mBoolArray);
    }

    @Test
    public void testCharArray() {
        ParcelizableImpl obj = new ParcelizableImpl();
        obj.mCharArray = new char[]{'a', 'Z'};
        ParcelizableImpl other = parcelCopy(obj);
        assertArrayEquals(obj.mCharArray, other.mCharArray);
    }

    @Test
    public void testIntArray() {
        ParcelizableImpl obj = new ParcelizableImpl();
        obj.mIntArray = new int[]{42, 24, 16};
        ParcelizableImpl other = parcelCopy(obj);
        assertArrayEquals(obj.mIntArray, other.mIntArray);
    }

    @Test
    public void testLongArray() {
        ParcelizableImpl obj = new ParcelizableImpl();
        obj.mLongArray = new long[]{1000L, 1312};
        ParcelizableImpl other = parcelCopy(obj);
        assertArrayEquals(obj.mLongArray, other.mLongArray);
    }

    @Test
    public void testFloatArray() {
        ParcelizableImpl obj = new ParcelizableImpl();
        obj.mFloatArray = new float[]{1.5f, 2.5f};
        ParcelizableImpl other = parcelCopy(obj);
        assertArrayEquals(obj.mFloatArray, other.mFloatArray, .01f);
    }

    @Test
    public void testDoubleArray() {
        ParcelizableImpl obj = new ParcelizableImpl();
        obj.mDoubleArray = new double[]{1.5, 2.5, 3.5};
        ParcelizableImpl other = parcelCopy(obj);
        assertArrayEquals(obj.mDoubleArray, other.mDoubleArray, .01);
    }

    @Test
    public void testBinderArray() {
        if (mUseStream) {
            return;
        }
        ParcelizableImpl obj = new ParcelizableImpl();
        obj.mBinderArray = new IBinder[]{new Binder(), new Binder()};
        ParcelizableImpl other = parcelCopy(obj);
        assertArrayEquals(obj.mBinderArray, other.mBinderArray);
    }

    @Test
    public void testException() {
        ParcelizableImpl obj = new ParcelizableImpl();
        obj.mException = new IllegalArgumentException();
        ParcelizableImpl other = parcelCopy(obj);
        assertEquals(obj.mException.getClass(), other.mException.getClass());
    }

    @Test
    public void testSize() {
        ParcelizableImpl obj = new ParcelizableImpl();
        obj.mSize = new Size(4, 2);
        ParcelizableImpl other = parcelCopy(obj);
        assertEquals(obj.mSize, other.mSize);
    }

    @Test
    public void testSizeF() {
        ParcelizableImpl obj = new ParcelizableImpl();
        obj.mSizeF = new SizeF(4.2f, 5);
        ParcelizableImpl other = parcelCopy(obj);
        assertEquals(obj.mSizeF, other.mSizeF);
    }

    @Test
    public void testSparseBooleanArray() {
        ParcelizableImpl obj = new ParcelizableImpl();
        obj.mSparseBooleanArray = new SparseBooleanArray();
        obj.mSparseBooleanArray.put(42, true);
        obj.mSparseBooleanArray.put(15, true);
        obj.mSparseBooleanArray.put(23, false);
        ParcelizableImpl other = parcelCopy(obj);
        assertEquals(obj.mSparseBooleanArray, other.mSparseBooleanArray);
    }

    @Test
    public void testParcelable() {
        if (mUseStream) {
            return;
        }
        ParcelizableImpl obj = new ParcelizableImpl();
        obj.mParcelable = new Intent("my.action.ACTION")
                .addCategory("has.a.CATEGORY")
                .setData(Uri.parse("something://authority/with/some/stuff"));
        ParcelizableImpl other = parcelCopy(obj);
        assertEquals(obj.mParcelable.toString(), other.mParcelable.toString());
    }

    @Test
    public void testStringList() {
        ParcelizableImpl obj = new ParcelizableImpl();
        obj.mStringList = Arrays.asList("string_1", "42");
        ParcelizableImpl other = parcelCopy(obj);
        assertEquals(obj.mStringList, other.mStringList);
    }

    @Test
    public void testBinderList() {
        if (mUseStream) {
            return;
        }
        ParcelizableImpl obj = new ParcelizableImpl();
        obj.mBinderList = Arrays.asList((IBinder) new Binder(), new Binder());
        ParcelizableImpl other = parcelCopy(obj);
        assertEquals(obj.mBinderList, other.mBinderList);
    }

    @Test
    public void testBundleTypes() {
        ParcelizableImpl obj = new ParcelizableImpl();
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

        ParcelizableImpl other = parcelCopy(obj);

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

    @SafeParcelize(allowSerialization = true,
            ignoreParcelables = true,
            deprecatedIds = {5, 14})
    public static class ParcelizableImpl implements SafeParcelable {

        @ParcelField(1)
        public int mIntField;

        @NonParcelField
        public int mNonSerializedField;

        @ParcelField(2)
        public boolean mBoolean;
        @ParcelField(3)
        public int mInt;
        @ParcelField(4)
        public long mLong;
        @ParcelField(50)
        public float mFloat;
        @ParcelField(6)
        public double mDouble;
        @ParcelField(7)
        public String mString;
        @ParcelField(8)
        public IBinder mBinder;
        @ParcelField(9)
        public byte[] mByteArray;
        @ParcelField(10)
        public Bundle mBundle;
        @ParcelField(12)
        public boolean[] mBoolArray;
        @ParcelField(13)
        public char[] mCharArray;
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
        @ParcelField(21)
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
    }
}
