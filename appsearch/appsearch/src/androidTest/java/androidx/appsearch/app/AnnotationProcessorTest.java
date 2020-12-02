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
// @exportToFramework:skipFile()
package androidx.appsearch.app;

import static androidx.appsearch.app.AppSearchSchema.PropertyConfig.INDEXING_TYPE_PREFIXES;
import static androidx.appsearch.app.AppSearchSchema.PropertyConfig.TOKENIZER_TYPE_PLAIN;
import static androidx.appsearch.app.util.AppSearchTestUtils.checkIsBatchResultSuccess;
import static androidx.appsearch.app.util.AppSearchTestUtils.checkIsResultSuccess;
import static androidx.appsearch.app.util.AppSearchTestUtils.convertSearchResultsToDocuments;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;

import androidx.appsearch.annotation.AppSearchDocument;
import androidx.appsearch.localstorage.LocalStorage;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class AnnotationProcessorTest {
    private AppSearchSession mSession;

    @Before
    public void setUp() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        mSession = checkIsResultSuccess(LocalStorage.createSearchSession(
                new LocalStorage.SearchContext.Builder(context)
                        .setDatabaseName("testDb1").build()));

        // Remove all documents from any instances that may have been created in the tests.
        checkIsResultSuccess(
                mSession.setSchema(new SetSchemaRequest.Builder().setForceOverride(true).build()));
    }

    @AppSearchDocument
    static class Card {
        @AppSearchDocument.Uri String mUri;
        @AppSearchDocument.Property
                (indexingType = INDEXING_TYPE_PREFIXES, tokenizerType = TOKENIZER_TYPE_PLAIN)
        String mString;        // 3a

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Card)) {
                return false;
            }
            Card otherCard = (Card) other;
            assertThat(otherCard.mUri).isEqualTo(this.mUri);
            return true;
        }
    }

    @AppSearchDocument
    static class Gift {
        @AppSearchDocument.Uri String mUri;

        // Collections
        @AppSearchDocument.Property Collection<Long> mCollectLong;         // 1a
        @AppSearchDocument.Property Collection<Integer> mCollectInteger;   // 1a
        @AppSearchDocument.Property Collection<Double> mCollectDouble;     // 1a
        @AppSearchDocument.Property Collection<Float> mCollectFloat;       // 1a
        @AppSearchDocument.Property Collection<Boolean> mCollectBoolean;   // 1a
        @AppSearchDocument.Property Collection<byte[]> mCollectByteArr;    // 1a
        @AppSearchDocument.Property Collection<String> mCollectString;     // 1b
        @AppSearchDocument.Property Collection<Card> mCollectCard;         // 1c

        // Arrays
        @AppSearchDocument.Property Long[] mArrBoxLong;         // 2a
        @AppSearchDocument.Property long[] mArrUnboxLong;       // 2b
        @AppSearchDocument.Property Integer[] mArrBoxInteger;   // 2a
        @AppSearchDocument.Property int[] mArrUnboxInt;         // 2a
        @AppSearchDocument.Property Double[] mArrBoxDouble;     // 2a
        @AppSearchDocument.Property double[] mArrUnboxDouble;   // 2b
        @AppSearchDocument.Property Float[] mArrBoxFloat;       // 2a
        @AppSearchDocument.Property float[] mArrUnboxFloat;     // 2a
        @AppSearchDocument.Property Boolean[] mArrBoxBoolean;   // 2a
        @AppSearchDocument.Property boolean[] mArrUnboxBoolean; // 2b
        @AppSearchDocument.Property byte[][] mArrUnboxByteArr;  // 2b
        @AppSearchDocument.Property Byte[] mBoxByteArr;         // 2a
        @AppSearchDocument.Property String[] mArrString;        // 2b
        @AppSearchDocument.Property Card[] mArrCard;            // 2c

        // Single values
        @AppSearchDocument.Property String mString;        // 3a
        @AppSearchDocument.Property Long mBoxLong;         // 3a
        @AppSearchDocument.Property long mUnboxLong;       // 3b
        @AppSearchDocument.Property Integer mBoxInteger;   // 3a
        @AppSearchDocument.Property int mUnboxInt;         // 3b
        @AppSearchDocument.Property Double mBoxDouble;     // 3a
        @AppSearchDocument.Property double mUnboxDouble;   // 3b
        @AppSearchDocument.Property Float mBoxFloat;       // 3a
        @AppSearchDocument.Property float mUnboxFloat;     // 3b
        @AppSearchDocument.Property Boolean mBoxBoolean;   // 3a
        @AppSearchDocument.Property boolean mUnboxBoolean; // 3b
        @AppSearchDocument.Property byte[] mUnboxByteArr;  // 3a
        @AppSearchDocument.Property Card mCard;            // 3c

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Gift)) {
                return false;
            }
            Gift otherGift = (Gift) other;
            assertThat(otherGift.mUri).isEqualTo(this.mUri);
            assertThat(otherGift.mArrBoxBoolean).isEqualTo(this.mArrBoxBoolean);
            assertThat(otherGift.mArrBoxDouble).isEqualTo(this.mArrBoxDouble);
            assertThat(otherGift.mArrBoxFloat).isEqualTo(this.mArrBoxFloat);
            assertThat(otherGift.mArrBoxLong).isEqualTo(this.mArrBoxLong);
            assertThat(otherGift.mArrBoxInteger).isEqualTo(this.mArrBoxInteger);
            assertThat(otherGift.mArrString).isEqualTo(this.mArrString);
            assertThat(otherGift.mBoxByteArr).isEqualTo(this.mBoxByteArr);
            assertThat(otherGift.mArrUnboxBoolean).isEqualTo(this.mArrUnboxBoolean);
            assertThat(otherGift.mArrUnboxByteArr).isEqualTo(this.mArrUnboxByteArr);
            assertThat(otherGift.mArrUnboxDouble).isEqualTo(this.mArrUnboxDouble);
            assertThat(otherGift.mArrUnboxFloat).isEqualTo(this.mArrUnboxFloat);
            assertThat(otherGift.mArrUnboxLong).isEqualTo(this.mArrUnboxLong);
            assertThat(otherGift.mArrUnboxInt).isEqualTo(this.mArrUnboxInt);
            assertThat(otherGift.mArrCard).isEqualTo(this.mArrCard);

            assertThat(otherGift.mCollectLong).isEqualTo(this.mCollectLong);
            assertThat(otherGift.mCollectInteger).isEqualTo(this.mCollectInteger);
            assertThat(otherGift.mCollectBoolean).isEqualTo(this.mCollectBoolean);
            assertThat(otherGift.mCollectString).isEqualTo(this.mCollectString);
            assertThat(otherGift.mCollectDouble).isEqualTo(this.mCollectDouble);
            assertThat(otherGift.mCollectFloat).isEqualTo(this.mCollectFloat);
            assertThat(otherGift.mCollectCard).isEqualTo(this.mCollectCard);
            checkCollectByteArr(otherGift.mCollectByteArr, this.mCollectByteArr);

            assertThat(otherGift.mString).isEqualTo(this.mString);
            assertThat(otherGift.mBoxLong).isEqualTo(this.mBoxLong);
            assertThat(otherGift.mUnboxLong).isEqualTo(this.mUnboxLong);
            assertThat(otherGift.mBoxInteger).isEqualTo(this.mBoxInteger);
            assertThat(otherGift.mUnboxInt).isEqualTo(this.mUnboxInt);
            assertThat(otherGift.mBoxDouble).isEqualTo(this.mBoxDouble);
            assertThat(otherGift.mUnboxDouble).isEqualTo(this.mUnboxDouble);
            assertThat(otherGift.mBoxFloat).isEqualTo(this.mBoxFloat);
            assertThat(otherGift.mUnboxFloat).isEqualTo(this.mUnboxFloat);
            assertThat(otherGift.mBoxBoolean).isEqualTo(this.mBoxBoolean);
            assertThat(otherGift.mUnboxBoolean).isEqualTo(this.mUnboxBoolean);
            assertThat(otherGift.mUnboxByteArr).isEqualTo(this.mUnboxByteArr);
            assertThat(otherGift.mCard).isEqualTo(this.mCard);
            return true;
        }

        void checkCollectByteArr(Collection<byte[]> first, Collection<byte[]> second) {
            if (first == null && second == null) {
                return;
            }
            assertThat(first).isNotNull();
            assertThat(second).isNotNull();
            assertThat(first.toArray()).isEqualTo(second.toArray());
        }
    }

    @Test
    public void testAnnotationProcessor() throws Exception {
        //TODO(b/156296904) add test for int, float, GenericDocument, and class with
        // @AppSearchDocument annotation
        checkIsResultSuccess(mSession.setSchema(
                new SetSchemaRequest.Builder().addDataClass(Card.class, Gift.class).build()));

        // Create a Gift object and assign values.
        Gift inputDataClass = new Gift();
        inputDataClass.mUri = "gift.uri";

        inputDataClass.mArrBoxBoolean = new Boolean[]{true, false};
        inputDataClass.mArrBoxDouble = new Double[]{0.0, 1.0};
        inputDataClass.mArrBoxFloat = new Float[]{2.0F, 3.0F};
        inputDataClass.mArrBoxInteger = new Integer[]{4, 5};
        inputDataClass.mArrBoxLong = new Long[]{6L, 7L};
        inputDataClass.mArrString = new String[]{"cat", "dog"};
        inputDataClass.mBoxByteArr = new Byte[]{8, 9};
        inputDataClass.mArrUnboxBoolean = new boolean[]{false, true};
        inputDataClass.mArrUnboxByteArr = new byte[][]{{0, 1}, {2, 3}};
        inputDataClass.mArrUnboxDouble = new double[]{1.0, 0.0};
        inputDataClass.mArrUnboxFloat = new float[]{3.0f, 2.0f};
        inputDataClass.mArrUnboxInt = new int[]{5, 4};
        inputDataClass.mArrUnboxLong = new long[]{7, 6};

        Card card1 = new Card();
        card1.mUri = "card.uri1";
        Card card2 = new Card();
        card2.mUri = "card.uri2";
        inputDataClass.mArrCard = new Card[]{card2, card2};

        inputDataClass.mCollectLong = Arrays.asList(inputDataClass.mArrBoxLong);
        inputDataClass.mCollectInteger = Arrays.asList(inputDataClass.mArrBoxInteger);
        inputDataClass.mCollectBoolean = Arrays.asList(inputDataClass.mArrBoxBoolean);
        inputDataClass.mCollectString = Arrays.asList(inputDataClass.mArrString);
        inputDataClass.mCollectDouble = Arrays.asList(inputDataClass.mArrBoxDouble);
        inputDataClass.mCollectFloat = Arrays.asList(inputDataClass.mArrBoxFloat);
        inputDataClass.mCollectByteArr = Arrays.asList(inputDataClass.mArrUnboxByteArr);
        inputDataClass.mCollectCard = Arrays.asList(card2, card2);

        inputDataClass.mString = "String";
        inputDataClass.mBoxLong = 1L;
        inputDataClass.mUnboxLong = 2L;
        inputDataClass.mBoxInteger = 3;
        inputDataClass.mUnboxInt = 4;
        inputDataClass.mBoxDouble = 5.0;
        inputDataClass.mUnboxDouble = 6.0;
        inputDataClass.mBoxFloat = 7.0F;
        inputDataClass.mUnboxFloat = 8.0f;
        inputDataClass.mBoxBoolean = true;
        inputDataClass.mUnboxBoolean = false;
        inputDataClass.mUnboxByteArr = new byte[]{1, 2, 3};
        inputDataClass.mCard = card1;

        // Index the Gift document and query it.
        checkIsBatchResultSuccess(mSession.putDocuments(
                new PutDocumentsRequest.Builder().addDataClass(inputDataClass).build()));
        SearchResults searchResults = mSession.query("", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(1);

        // Create DataClassFactory for Gift.
        DataClassFactoryRegistry registry = DataClassFactoryRegistry.getInstance();
        DataClassFactory<Gift> factory = registry.getOrCreateFactory(Gift.class);

        // Convert GenericDocument to Gift and check values.
        Gift outputDataClass = factory.fromGenericDocument(documents.get((0)));
        assertThat(outputDataClass).isEqualTo(inputDataClass);
    }

    @Test
    public void testAnnotationProcessor_queryByType() throws Exception {
        checkIsResultSuccess(mSession.setSchema(
                new SetSchemaRequest.Builder()
                        .addDataClass(Card.class, Gift.class)
                        .addSchema(AppSearchEmail.SCHEMA).build()));

        // Create documents and index them
        Gift inputDataClass1 = new Gift();
        inputDataClass1.mUri = "gift.uri1";
        Gift inputDataClass2 = new Gift();
        inputDataClass2.mUri = "gift.uri2";
        AppSearchEmail email1 =
                new AppSearchEmail.Builder("uri3")
                        .setNamespace("namespace")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mSession.putDocuments(
                new PutDocumentsRequest.Builder()
                        .addDataClass(inputDataClass1, inputDataClass2)
                        .addGenericDocument(email1).build()));

        // Query the documents by it's schema type.
        SearchResults searchResults = mSession.query("",
                new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                        .addSchemaType("Gift", AppSearchEmail.SCHEMA_TYPE)
                        .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(3);

        // Query the documents by it's class.
        searchResults = mSession.query("",
                new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                        .addSchemaByDataClass(Gift.class)
                        .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(2);

        // Query the documents by schema type and class mix.
        searchResults = mSession.query("",
                new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                        .addSchemaType(AppSearchEmail.SCHEMA_TYPE)
                        .addSchemaByDataClass(Gift.class)
                        .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(3);
    }
}
