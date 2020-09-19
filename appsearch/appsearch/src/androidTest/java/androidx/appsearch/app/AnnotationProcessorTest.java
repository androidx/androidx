/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.appsearch.app;

import static androidx.appsearch.app.AppSearchTestUtils.checkIsBatchResultSuccess;
import static androidx.appsearch.app.AppSearchTestUtils.checkIsResultSuccess;
import static androidx.appsearch.app.AppSearchTestUtils.doQuery;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;

import androidx.appsearch.annotation.AppSearchDocument;
import androidx.appsearch.localbackend.LocalBackend;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class AnnotationProcessorTest {
    private AppSearchManager mAppSearchManager;

    @Before
    public void setUp() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        LocalBackend backend = LocalBackend.getInstance(context).getResultValue();
        mAppSearchManager = checkIsResultSuccess(new AppSearchManager.Builder()
                .setDatabaseName("testDb").setBackend(backend).build());

        // Remove all documents from any instances that may have been created in the tests.
        backend.resetAllDatabases().getResultValue();
    }

    @AppSearchDocument
    static class Gift {
        @AppSearchDocument.Uri String mUri;

        // Collections
        @AppSearchDocument.Property Collection<Long> mLongCollection;         // 1a
//        @AppSearchDocument.Property Collection<Integer> mCollectInteger;   // 1a
        @AppSearchDocument.Property Collection<Double> mCollectDouble;     // 1a
//        @AppSearchDocument.Property Collection<Float> mCollectFloat;       // 1a
        @AppSearchDocument.Property Collection<Boolean> mCollectBoolean;   // 1a
        @AppSearchDocument.Property Collection<byte[]> mCollectByteArr;    // 1a
        @AppSearchDocument.Property Collection<String> mCollectString;     // 1b
//        @AppSearchDocument.Property Collection<Gift> mCollectGift;         // 1c

        // Arrays
        @AppSearchDocument.Property Long[] mArrBoxLong;         // 2a
        @AppSearchDocument.Property long[] mArrUnboxLong;       // 2b
//        @AppSearchDocument.Property Integer[] mArrBoxInteger;   // 2a
//        @AppSearchDocument.Property int[] mArrUnboxInt;         // 2a
        @AppSearchDocument.Property Double[] mArrBoxDouble;     // 2a
        @AppSearchDocument.Property double[] mArrUnboxDouble;   // 2b
//        @AppSearchDocument.Property Float[] mArrBoxFloat;       // 2a
//        @AppSearchDocument.Property float[] mArrUnboxFloat;     // 2a
        @AppSearchDocument.Property Boolean[] mArrBoxBoolean;   // 2a
        @AppSearchDocument.Property boolean[] mArrUnboxBoolean; // 2b
        @AppSearchDocument.Property byte[][] mArrUnboxByteArr;  // 2b
        @AppSearchDocument.Property Byte[] mBoxByteArr;         // 2a
        @AppSearchDocument.Property String[] mArrString;        // 2b
//        @AppSearchDocument.Property Gift[] mArrGift;            // 2c

        // Single values
        @AppSearchDocument.Property String mString;        // 3a
        @AppSearchDocument.Property Long mBoxLong;         // 3a
        @AppSearchDocument.Property long mUnboxLong;       // 3b
//        @AppSearchDocument.Property Integer mBoxInteger;   // 3a
//        @AppSearchDocument.Property int mUnboxInt;         // 3b
        @AppSearchDocument.Property Double mBoxDouble;     // 3a
        @AppSearchDocument.Property double mUnboxDouble;   // 3b
//        @AppSearchDocument.Property Float mBoxFloat;       // 3a
//        @AppSearchDocument.Property float mUnboxFloat;     // 3b
        @AppSearchDocument.Property Boolean mBoxBoolean;   // 3a
        @AppSearchDocument.Property boolean mUnboxBoolean; // 3b
        @AppSearchDocument.Property byte[] mUnboxByteArr;  // 3a
//        @AppSearchDocument.Property Gift mGift;            // 3c
    }

    @Test
    public void testAnnotationProcessor() throws Exception {
        //TODO(b/156296904) add test for int, float, GenericDocument, and class with
        // @AppSearchDocument annotation
        checkIsResultSuccess(mAppSearchManager.setSchema(
                new AppSearchManager.SetSchemaRequest.Builder()
                        .addDataClass(Gift.class)
                        .build()));

        // Create a Gift object and assign values.
        Gift inputDataClass = new Gift();
        inputDataClass.mUri = "gift.uri";

        inputDataClass.mArrBoxBoolean = new Boolean[]{true, false};
        inputDataClass.mArrBoxDouble = new Double[]{0.0, 1.0};
        inputDataClass.mArrBoxLong = new Long[]{6L, 7L};
        inputDataClass.mArrString = new String[]{"cat", "dog"};
        inputDataClass.mBoxByteArr = new Byte[]{8, 9};
        inputDataClass.mArrUnboxBoolean = new boolean[]{false, true};
        inputDataClass.mArrUnboxByteArr = new byte[][]{{0, 1}, {2, 3}};
        inputDataClass.mArrUnboxDouble = new double[]{1.0, 0.0};
        inputDataClass.mArrUnboxLong = new long[]{7, 6};

        inputDataClass.mLongCollection = Arrays.asList(inputDataClass.mArrBoxLong);
        inputDataClass.mCollectBoolean = Arrays.asList(inputDataClass.mArrBoxBoolean);
        inputDataClass.mCollectString = Arrays.asList(inputDataClass.mArrString);
        inputDataClass.mCollectDouble = Arrays.asList(inputDataClass.mArrBoxDouble);
        inputDataClass.mCollectByteArr = Arrays.asList(inputDataClass.mArrUnboxByteArr);

        inputDataClass.mString = "String";
        inputDataClass.mBoxLong = 1L;
        inputDataClass.mUnboxLong = 2L;
        inputDataClass.mBoxDouble = 5.0;
        inputDataClass.mUnboxDouble = 6.0;
        inputDataClass.mBoxBoolean = true;
        inputDataClass.mUnboxBoolean = false;
        inputDataClass.mUnboxByteArr = new byte[]{1, 2, 3};

        // Index the Gift document and query it.
        checkIsBatchResultSuccess(mAppSearchManager.putDocuments(
                new AppSearchManager.PutDocumentsRequest.Builder()
                        .addDataClass(inputDataClass).build()));
        List<GenericDocument> searchResults = doQuery(mAppSearchManager, "");
        assertThat(searchResults).hasSize(1);

        // Create DataClassFactory for Gift.
        DataClassFactoryRegistry registry = DataClassFactoryRegistry.getInstance();
        DataClassFactory<Gift> factory = registry.getOrCreateFactory(Gift.class);

        // Convert GenericDocument to Gift and check values.
        Gift outputDataClass = factory.fromGenericDocument(searchResults.get((0)));
        assertThat(outputDataClass.mArrBoxBoolean).isEqualTo(inputDataClass.mArrBoxBoolean);
        assertThat(outputDataClass.mArrBoxDouble).isEqualTo(inputDataClass.mArrBoxDouble);
        assertThat(outputDataClass.mArrBoxLong).isEqualTo(inputDataClass.mArrBoxLong);
        assertThat(outputDataClass.mArrString).isEqualTo(inputDataClass.mArrString);
        assertThat(outputDataClass.mBoxByteArr).isEqualTo(inputDataClass.mBoxByteArr);
        assertThat(outputDataClass.mArrUnboxBoolean).isEqualTo(inputDataClass.mArrUnboxBoolean);
        assertThat(outputDataClass.mArrUnboxByteArr).isEqualTo(inputDataClass.mArrUnboxByteArr);
        assertThat(outputDataClass.mArrUnboxDouble).isEqualTo(inputDataClass.mArrUnboxDouble);
        assertThat(outputDataClass.mArrUnboxLong).isEqualTo(inputDataClass.mArrUnboxLong);

        assertThat(outputDataClass.mLongCollection).isEqualTo(inputDataClass.mLongCollection);
        assertThat(outputDataClass.mCollectBoolean).isEqualTo(inputDataClass.mCollectBoolean);
        assertThat(outputDataClass.mCollectString).isEqualTo(inputDataClass.mCollectString);
        assertThat(outputDataClass.mCollectDouble).isEqualTo(inputDataClass.mCollectDouble);
        assertThat(outputDataClass.mCollectByteArr.toArray()).isEqualTo(
                inputDataClass.mArrUnboxByteArr);

        assertThat(outputDataClass.mString).isEqualTo(inputDataClass.mString);
        assertThat(outputDataClass.mBoxLong).isEqualTo(inputDataClass.mBoxLong);
        assertThat(outputDataClass.mUnboxLong).isEqualTo(inputDataClass.mUnboxLong);
        assertThat(outputDataClass.mBoxDouble).isEqualTo(inputDataClass.mBoxDouble);
        assertThat(outputDataClass.mUnboxDouble).isEqualTo(inputDataClass.mUnboxDouble);
        assertThat(outputDataClass.mBoxBoolean).isEqualTo(inputDataClass.mBoxBoolean);
        assertThat(outputDataClass.mUnboxBoolean).isEqualTo(inputDataClass.mUnboxBoolean);
        assertThat(outputDataClass.mUnboxByteArr).isEqualTo(inputDataClass.mUnboxByteArr);
    }
}
