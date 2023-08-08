/*
 * Copyright 2022 The Android Open Source Project
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

import android.content.Intent;
import android.content.pm.Signature;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.SparseArray;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import com.google.common.collect.Lists;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Objects;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class BundleCompatTest {

    @Test
    public void getParcelable() {
        Bundle bundle = new Bundle();
        bundle.putParcelable("parcelable", new Intent());
        parcelAndUnparcel(bundle);

        assertEquals(Intent.class, Objects.requireNonNull(
                BundleCompat.getParcelable(bundle, "parcelable", Intent.class)).getClass());
    }

    @Test
    public void getParcelable_returnsNullOnClassMismatch() {
        Bundle bundle = new Bundle();
        bundle.putParcelable("parcelable", new Intent());
        parcelAndUnparcel(bundle);

        assertNull(BundleCompat.getParcelable(bundle, "parcelable", Signature.class));
    }

    @Test
    @SdkSuppress(minSdkVersion = 33)
    public void getParcelableArray_post33() {
        if (Build.VERSION.SDK_INT < 34) return;
        Bundle bundle = new Bundle();
        bundle.putParcelableArray("array", new Intent[] { new Intent() });
        parcelAndUnparcel(bundle);

        assertEquals(Intent[].class, Objects.requireNonNull(
                BundleCompat.getParcelableArray(bundle, "array", Intent.class)).getClass());
    }

    @Test
    @SdkSuppress(minSdkVersion = 33)
    public void getParcelableArray_returnsNullOnClassMismatch_post33() {
        if (Build.VERSION.SDK_INT < 34) return;
        Bundle bundle = new Bundle();
        bundle.putParcelableArray("array", new Intent[] { new Intent() });
        parcelAndUnparcel(bundle);

        assertNull(BundleCompat.getParcelableArray(bundle, "array", Signature.class));
    }

    @Test
    @SdkSuppress(maxSdkVersion = 32)
    public void getParcelableArray_pre33() {
        if (Build.VERSION.SDK_INT >= 34) return;
        Bundle bundle = new Bundle();
        bundle.putParcelableArray("array", new Intent[] { new Intent() });
        parcelAndUnparcel(bundle);

        assertEquals(Parcelable[].class, Objects.requireNonNull(
                BundleCompat.getParcelableArray(bundle, "array", Intent.class)).getClass());

        assertNotEquals(Intent[].class, Objects.requireNonNull(
                BundleCompat.getParcelableArray(bundle, "array", Intent.class)).getClass());

        // We do not check clazz Pre-U
        assertEquals(Parcelable[].class, Objects.requireNonNull(
                BundleCompat.getParcelableArray(bundle, "array", Signature.class)).getClass());
    }

    @Test
    public void getParcelableArrayList() {
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList("array", Lists.newArrayList(new Intent()));
        parcelAndUnparcel(bundle);

        assertEquals(Intent.class, Objects.requireNonNull(
                        BundleCompat.getParcelableArrayList(bundle, "array", Intent.class))
                .get(0).getClass());
    }

    @Test
    @SdkSuppress(minSdkVersion = 34)
    public void getParcelableArrayList_returnsNullOnClassMismatch_post34() {
        if (Build.VERSION.SDK_INT < 34) return;
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList("array", Lists.newArrayList(new Intent()));
        parcelAndUnparcel(bundle);

        assertNull(BundleCompat.getParcelableArrayList(bundle, "array", Signature.class));
    }

    @Test
    @SdkSuppress(maxSdkVersion = 33)
    public void getParcelableArrayList_noTypeCheck_pre34() {
        if (Build.VERSION.SDK_INT >= 34) return;
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList("array", Lists.newArrayList(new Intent()));
        parcelAndUnparcel(bundle);

        Object intent = Objects.requireNonNull(
                        BundleCompat.getParcelableArrayList(bundle, "array", Signature.class))
                .get(0);
        assertEquals(Intent.class, intent.getClass());
    }

    @Test
    public void getSparseParcelableArray() {
        Bundle bundle = new Bundle();
        SparseArray<Intent> array = new SparseArray<>();
        array.put(0, new Intent());
        bundle.putSparseParcelableArray("array", array);
        parcelAndUnparcel(bundle);

        assertEquals(Intent.class, Objects.requireNonNull(
                        BundleCompat.getSparseParcelableArray(bundle, "array", Intent.class))
                .get(0).getClass());
    }

    @Test
    @SdkSuppress(minSdkVersion = 34)
    public void getSparseParcelableArray_returnsNullOnClassMismatch_post34() {
        if (Build.VERSION.SDK_INT < 34) return;
        Bundle bundle = new Bundle();
        SparseArray<Intent> array = new SparseArray<>();
        array.put(0, new Intent());
        bundle.putSparseParcelableArray("array", array);
        parcelAndUnparcel(bundle);

        assertNull(BundleCompat.getSparseParcelableArray(bundle, "array", Signature.class));
    }

    @Test
    @SdkSuppress(maxSdkVersion = 33)
    public void getSparseParcelableArray_noTypeCheck_pre34() {
        if (Build.VERSION.SDK_INT >= 34) return;
        Bundle bundle = new Bundle();
        SparseArray<Intent> array = new SparseArray<>();
        array.put(0, new Intent());
        bundle.putSparseParcelableArray("array", array);
        parcelAndUnparcel(bundle);

        Object intent = Objects.requireNonNull(
                        BundleCompat.getSparseParcelableArray(bundle, "array", Signature.class))
                .get(0);
        assertEquals(Intent.class, intent.getClass());
    }

    private void parcelAndUnparcel(Bundle bundle) {
        Parcel p = Parcel.obtain();
        bundle.writeToParcel(p, 0);
        p.setDataPosition(0);
        bundle.readFromParcel(p);
    }

    @Test
    public void getBinder() {
        IBinder binder = new Binder();
        Bundle bundle = new Bundle();
        BundleCompat.putBinder(bundle, "binder", binder);
        IBinder result = BundleCompat.getBinder(bundle, "binder");
        assertEquals(binder, result);
    }
}
