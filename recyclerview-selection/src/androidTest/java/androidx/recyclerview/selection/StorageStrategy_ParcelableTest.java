/*
 * Copyright 2017 The Android Open Source Project
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

package androidx.recyclerview.selection;

import static org.junit.Assert.assertEquals;

import android.net.Uri;
import android.os.Bundle;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.recyclerview.selection.testing.Bundles;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
@SmallTest
public final class StorageStrategy_ParcelableTest {

    private static final List<Uri> sData;
    static {
        sData = new ArrayList<>(5);
        sData.add(Uri.parse("content://cheese0"));
        sData.add(Uri.parse("content://cheese1"));
        sData.add(Uri.parse("content://cheese2"));
        sData.add(Uri.parse("content://cheese3"));
        sData.add(Uri.parse("content://cheese4"));
    }

    private StorageStrategy<Uri> mStorage;

    @Before
    public void setUp() {
        mStorage = StorageStrategy.createParcelableStorage(Uri.class);
    }

    @Test
    public void testReadWrite() {

        MutableSelection<Uri> orig = new MutableSelection<>();
        orig.add(sData.get(2));
        orig.add(sData.get(3));
        orig.add(sData.get(4));

        Bundle parceled = Bundles.forceParceling(mStorage.asBundle(orig));
        Selection<Uri> restored = mStorage.asSelection(parceled);

        assertEquals(orig, restored);
    }
}
