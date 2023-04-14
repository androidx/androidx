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

package androidx.core.os;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.content.Intent;
import android.content.pm.Signature;
import android.graphics.Rect;
import android.os.BadParcelableException;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.SparseArray;

import androidx.annotation.RequiresApi;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.HashMap;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class ParcelCompatTest {
    @Test
    public void readWriteBoolean() {
        Parcel p = Parcel.obtain();
        ParcelCompat.writeBoolean(p, true);
        ParcelCompat.writeBoolean(p, false);

        p.setDataPosition(0);
        assertTrue(ParcelCompat.readBoolean(p));
        assertFalse(ParcelCompat.readBoolean(p));
    }

    @Test
    public void readParcelable() {
        Rect r = new Rect(0, 0, 10, 10);
        Parcel p = Parcel.obtain();
        p.writeParcelable(r, 0);

        p.setDataPosition(0);
        Rect r2 = ParcelCompat.readParcelable(p, Rect.class.getClassLoader(), Rect.class);
        assertEquals(r, r2);

        p.setDataPosition(0);
        assertThrows(BadParcelableException.class, () -> ParcelCompat.readParcelable(p,
                Rect.class.getClassLoader(), Intent.class));

        p.setDataPosition(0);
        p.writeParcelable((Rect) null, 0);
        p.setDataPosition(0);
        Rect r3 = ParcelCompat.readParcelable(p, Rect.class.getClassLoader(), Rect.class);
        assertEquals(null, r3);
    }

    @Test
    public void readArray() {
        Parcel p = Parcel.obtain();

        Signature[] s = {new Signature("1234"),
                null,
                new Signature("abcd")};
        p.writeArray(s);

        p.setDataPosition(0);
        Object[] objects = ParcelCompat.readArray(p, Signature.class.getClassLoader(),
                Signature.class);
        assertArrayEquals(s, objects);
        p.setDataPosition(0);

        p.recycle();
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    @Test
    public void readSparseArray() {
        Parcel p = Parcel.obtain();

        SparseArray<Signature> s = new SparseArray<>();
        s.put(0, new Signature("1234567890abcdef"));
        s.put(2, null);
        s.put(3, new Signature("abcdef1234567890"));
        p.writeSparseArray(s);

        p.setDataPosition(0);
        SparseArray<Signature> s1 = ParcelCompat.readSparseArray(p,
                Signature.class.getClassLoader(), Signature.class);
        assertEquals(s.size(), s1.size());
        for (int index = 0; index < s.size(); index++) {
            int key = s.keyAt(index);
            assertEquals(s.valueAt(index), s1.get(key));
        }

        p.recycle();
    }

    @Test
    public void readList() {
        Parcel p = Parcel.obtain();
        ArrayList<Signature> s = new ArrayList<>();
        ArrayList<Signature> s2 = new ArrayList<>();
        s.add(new Signature("1234567890abcdef"));
        s.add(new Signature("abcdef1234567890"));

        p.writeList(s);
        p.setDataPosition(0);
        ParcelCompat.readList(p, s2, Signature.class.getClassLoader(), Signature.class);
        assertEquals(2, s2.size());
        for (int i = 0; i < s2.size(); i++) {
            assertEquals(s.get(i), s2.get(i));
        }
        p.recycle();
    }

    @Test
    public void readArrayList() {
        Parcel p = Parcel.obtain();

        ArrayList<Signature> s = new ArrayList<>();
        s.add(new Signature("1234567890abcdef"));
        s.add(null);
        s.add(new Signature("abcdef1234567890"));

        p.writeList(s);
        p.setDataPosition(0);
        ArrayList<Signature> s1 = ParcelCompat.readArrayList(p, Signature.class.getClassLoader(),
                Signature.class);
        assertEquals(s, s1);

        p.recycle();
    }

    @Test
    public void readMap() {
        Parcel p = Parcel.obtain();
        ClassLoader loader = getClass().getClassLoader();
        HashMap<String, Signature> map = new HashMap<>();
        HashMap<String, Signature> map2 = new HashMap<>();

        map.put("key1", new Signature("abcd"));
        map.put("key2", new Signature("ABCD"));
        p.writeMap(map);
        p.setDataPosition(0);
        ParcelCompat.readMap(p, map2, Signature.class.getClassLoader(), String.class,
                Signature.class);
        assertEquals(map, map2);

        p.recycle();
    }

    @Test
    public void readHashMap() {
        Parcel p = Parcel.obtain();
        ClassLoader loader = getClass().getClassLoader();
        HashMap<String, Signature> map = new HashMap<>();
        HashMap<String, Signature> map2 = new HashMap<>();

        map.put("key1", new Signature("abcd"));
        map.put("key2", new Signature("ABCD"));
        p.writeMap(map);
        p.setDataPosition(0);
        map2 = ParcelCompat.readHashMap(p, loader, String.class, Signature.class);
        assertEquals(map, map2);

        p.recycle();
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    @Test
    public void readParcelableCreator() {
        final String signatureString  = "1234567890abcdef";
        Signature s = new Signature(signatureString);

        Parcel p = Parcel.obtain();
        p.writeParcelableCreator(s);
        p.setDataPosition(0);
        assertSame(Signature.CREATOR, ParcelCompat.readParcelableCreator(p,
                Signature.class.getClassLoader(), Signature.class));

        p.setDataPosition(0);
        p.recycle();
    }

    @Test
    public void readParcelableArray() {
        Parcel p = Parcel.obtain();
        Signature[] s = {new Signature("1234"),
                null,
                new Signature("abcd")
        };
        p.writeParcelableArray(s, 0);
        p.setDataPosition(0);
        Parcelable[] s1 = ParcelCompat.readParcelableArray(p, Signature.class.getClassLoader(),
                Signature.class);
        assertArrayEquals(s, s1);
        assertEquals(Signature[].class, s1.getClass());

        p.setDataPosition(0);
        Parcelable[] s2 = ParcelCompat.readParcelableArray(p, Parcelable.class.getClassLoader(),
                Parcelable.class);
        assertArrayEquals(s, s2);
        assertEquals(Parcelable[].class, s2.getClass());

        p.setDataPosition(0);
        assertThrows(BadParcelableException.class, () -> ParcelCompat.readParcelableArray(p,
                Signature.class.getClassLoader(), Intent.class));

        p.recycle();
    }

    @Test
    public void readParcelableArrayTyped_postU() {
        if (!BuildCompat.isAtLeastU()) return;
        Parcel p = Parcel.obtain();
        Signature[] s = {new Signature("1234"),
                null,
                new Signature("abcd")
        };
        p.writeParcelableArray(s, 0);
        p.setDataPosition(0);
        Parcelable[] s1 = ParcelCompat.readParcelableArrayTyped(p, Signature.class.getClassLoader(),
                Signature.class);
        assertArrayEquals(s, s1);
        assertEquals(Signature[].class, s1.getClass());

        p.setDataPosition(0);
        assertThrows(BadParcelableException.class, () -> ParcelCompat.readParcelableArrayTyped(p,
                Signature.class.getClassLoader(), Intent.class));

        p.recycle();
    }

    @Test
    public void readParcelableArrayTyped_preU() {
        if (BuildCompat.isAtLeastU()) return;
        Parcel p = Parcel.obtain();
        Signature[] s = {new Signature("1234"),
                null,
                new Signature("abcd")
        };
        p.writeParcelableArray(s, 0);
        p.setDataPosition(0);
        Parcelable[] s1 = ParcelCompat.readParcelableArrayTyped(p, Signature.class.getClassLoader(),
                Signature.class);
        assertArrayEquals(s, s1);
        assertNotEquals(Signature[].class, s1.getClass());

        // Type not checked pre-U
        p.setDataPosition(0);
        s1 = ParcelCompat.readParcelableArrayTyped(p, Signature.class.getClassLoader(),
                Intent.class);
        assertArrayEquals(s, s1);

        p.recycle();
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    public void readParcelableList() {
        final Parcel p = Parcel.obtain();
        ArrayList<Signature> list = new ArrayList<>();
        ArrayList<Signature> list1 = new ArrayList<>();
        list.add(new Signature("1234"));
        list.add(new Signature("4321"));
        p.writeParcelableList(list, 0);
        p.setDataPosition(0);
        ParcelCompat.readParcelableList(p, list1, Signature.class.getClassLoader(),
                Signature.class);
        assertEquals(list, list1);
        p.recycle();
    }

    @Test
    public void readSerializable2Arg() {
        String s = "Hello World";
        Parcel p = Parcel.obtain();
        p.writeSerializable(s);

        p.setDataPosition(0);
        String s2 = ParcelCompat.readSerializable(p, String.class.getClassLoader(), String.class);
        assertEquals(s, s2);
    }
}
