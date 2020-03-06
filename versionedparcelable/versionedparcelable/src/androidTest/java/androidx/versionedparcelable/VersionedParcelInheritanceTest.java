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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;

@RunWith(Parameterized.class)
@SmallTest
public class VersionedParcelInheritanceTest {

    @Parameterized.Parameters
    public static Iterable<? extends Object[]> data() {
        return Arrays.asList(new Object[][]{{false}, {true}});
    }

    private boolean mUseStream;

    public VersionedParcelInheritanceTest(boolean useStream) {
        mUseStream = useStream;
    }

    private VersionedParcelable parcelCopy(VersionedParcelable obj) {
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
    public void testInheritance() {
        ParcelizableSubImpl obj = new ParcelizableSubImpl();
        obj.mSubField = "42";
        obj.mBaseField = 42;

        ParcelizableSubImpl other = (ParcelizableSubImpl) parcelCopy(obj);
        assertEquals(obj.mSubField, other.mSubField);
        assertEquals(obj.mBaseField, other.mBaseField);
    }

    @Test
    public void testInheritance_withInnerClass() {
        BuildableParcelizableSubImpl obj = new BuildableParcelizableSubImpl();
        obj.mBaseField = 42;

        BuildableParcelizableSubImpl other = (BuildableParcelizableSubImpl) parcelCopy(obj);
        assertEquals(obj.mBaseField, other.mBaseField);
    }

    @VersionedParcelize(allowSerialization = true,
            ignoreParcelables = true)
    public static class ParcelizableSubImpl extends ParcelizableImplBase {
        @ParcelField(2)
        public String mSubField;

        @NonParcelField
        public int mSubNonParcelField;
    }

    @VersionedParcelize(allowSerialization = true,
            ignoreParcelables = true)
    public static class ParcelizableImplBase implements VersionedParcelable {
        @ParcelField(1)
        public int mBaseField;

        @NonParcelField
        public String mBaseNonParcelField;
    }

    @VersionedParcelize(allowSerialization = true,
            ignoreParcelables = true)
    public static class BuildableParcelizableSubImpl extends BuildableParcelizableImplBase {
        public static class Builder extends BuildableParcelizableImplBase.Builder {
        }
    }

    @VersionedParcelize(allowSerialization = true,
            ignoreParcelables = true)
    public static class BuildableParcelizableImplBase implements VersionedParcelable {
        @ParcelField(1)
        int mBaseField;

        public static class Builder {
            int mBaseField;
        }
    }
}
