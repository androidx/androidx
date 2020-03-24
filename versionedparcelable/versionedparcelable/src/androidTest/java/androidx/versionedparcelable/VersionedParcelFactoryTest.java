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
import static org.junit.Assert.assertTrue;

import android.os.Parcel;

import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;

@RunWith(Parameterized.class)
@SmallTest
public class VersionedParcelFactoryTest {
    private static boolean sHasGotten;

    @Parameterized.Parameters
    public static Iterable<? extends Object[]> data() {
        return Arrays.asList(new Object[][]{{false}, {true}});
    }

    private boolean mUseStream;

    public VersionedParcelFactoryTest(boolean useStream) {
        mUseStream = useStream;
    }

    private FactoryParcelImpl parcelCopy(FactoryParcelImpl obj) {
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
        sHasGotten = false;
        FactoryParcelImpl obj = new FactoryParcelImpl();
        obj.mInt = 42;
        FactoryParcelImpl other = parcelCopy(obj);
        assertEquals(obj.mInt, other.mInt);
        assertTrue(sHasGotten);
    }

    @VersionedParcelize(allowSerialization = true,
            factory = FactoryParcelImplFactory.class)
    public static class FactoryParcelImpl extends CustomVersionedParcelable {

        @ParcelField(1)
        public int mInt;

        private FactoryParcelImpl() {

        }
    }

    public static class FactoryParcelImplFactory {

        public FactoryParcelImpl get() {
            sHasGotten = true;
            return new FactoryParcelImpl();
        }
    }
}
