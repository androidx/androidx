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

import static androidx.versionedparcelable.ParcelUtils.toParcelable;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.os.BadParcelableException;
import android.os.Bundle;
import android.os.Parcel;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class ParcelUtilsTest {

    @Test
    public void testBundlingWithoutCrash() {
        Bundle b = new Bundle();
        b.putString("pre_existing_arg", "my_string");
        ParcelUtils.putVersionedParcelable(b, "myKey", new ParcelUtilsParcelable());

        Parcel p = Parcel.obtain();
        p.writeParcelable(b, 0);
        p.setDataPosition(0);

        Bundle after = p.readParcelable(Bundle.class.getClassLoader());
        after.setClassLoader(null);
        assertEquals("my_string", after.getString("pre_existing_arg"));

        assertNotNull(ParcelUtils.getVersionedParcelable(after, "myKey"));
    }

    @Test(expected = BadParcelableException.class)
    public void testBundlingExpectedCrash() {
        Bundle b = new Bundle();
        b.putString("pre_existing_arg", "my_string");
        b.putParcelable("myKey", toParcelable(new ParcelUtilsParcelable()));

        Parcel p = Parcel.obtain();
        p.writeParcelable(b, 0);
        p.setDataPosition(0);

        Bundle after = p.readParcelable(Bundle.class.getClassLoader());
        after.setClassLoader(null);
        after.getString("pre_existing_arg");
    }

    @Test
    public void getAndPutVersionedParcelable_null() {
        Bundle bundle = new Bundle();
        ParcelUtils.putVersionedParcelable(bundle, "key", null);

        VersionedParcelable result = ParcelUtils.getVersionedParcelable(bundle, "key");

        assertThat(result).isNull();
    }

    @Test
    public void putVersionedParcelableNullClearsKey() {
        Bundle bundle = new Bundle();

        ParcelUtils.putVersionedParcelable(bundle, "key", new ParcelUtilsParcelable());
        assertThat(bundle.getBundle("key")).isNotNull();

        ParcelUtils.putVersionedParcelable(bundle, "key", null);
        assertThat(bundle.getBundle("key")).isNull();
    }

    @VersionedParcelize
    public static class ParcelUtilsParcelable implements VersionedParcelable {
        @ParcelField(1)
        public int mField;
    }
}
