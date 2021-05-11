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

import static androidx.appsearch.app.AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES;
import static androidx.appsearch.app.AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN;
import static androidx.appsearch.app.util.AppSearchTestUtils.checkIsBatchResultSuccess;
import static androidx.appsearch.app.util.AppSearchTestUtils.convertSearchResultsToDocuments;

import static com.google.common.truth.Truth.assertThat;

import androidx.annotation.NonNull;
import androidx.appsearch.annotation.Document;
import androidx.appsearch.app.util.AppSearchEmail;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public abstract class AnnotationProcessorTestBase {
    private AppSearchSession mSession;
    private static final String DB_NAME_1 = "";

    protected abstract ListenableFuture<AppSearchSession> createSearchSession(
            @NonNull String dbName);

    @Before
    public void setUp() throws Exception {
        mSession = createSearchSession(DB_NAME_1).get();

        // Cleanup whatever documents may still exist in these databases. This is needed in
        // addition to tearDown in case a test exited without completing properly.
        cleanup();
    }

    @After
    public void tearDown() throws Exception {
        // Cleanup whatever documents may still exist in these databases.
        cleanup();
    }

    private void cleanup() throws Exception {
        mSession.setSchema(new SetSchemaRequest.Builder().setForceOverride(true).build()).get();
    }

    @Document
    static class Card {
        @Document.Namespace
        String mNamespace;

        @Document.Id
        String mId;

        @Document.CreationTimestampMillis
        long mCreationTimestampMillis;

        @Document.StringProperty
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
            assertThat(otherCard.mId).isEqualTo(this.mId);
            return true;
        }
    }

    @Document
    static class Gift {
        @Document.Namespace
        String mNamespace;

        @Document.Id
        String mId;

        @Document.CreationTimestampMillis
        long mCreationTimestampMillis;

        // Collections
        @Document.LongProperty
        Collection<Long> mCollectLong;         // 1a
        @Document.LongProperty
        Collection<Integer> mCollectInteger;   // 1a
        @Document.DoubleProperty
        Collection<Double> mCollectDouble;     // 1a
        @Document.DoubleProperty
        Collection<Float> mCollectFloat;       // 1a
        @Document.BooleanProperty
        Collection<Boolean> mCollectBoolean;   // 1a
        @Document.BytesProperty
        Collection<byte[]> mCollectByteArr;    // 1a
        @Document.StringProperty
        Collection<String> mCollectString;     // 1b
        @Document.DocumentProperty
        Collection<Card> mCollectCard;         // 1c

        // Arrays
        @Document.LongProperty
        Long[] mArrBoxLong;         // 2a
        @Document.LongProperty
        long[] mArrUnboxLong;       // 2b
        @Document.LongProperty
        Integer[] mArrBoxInteger;   // 2a
        @Document.LongProperty
        int[] mArrUnboxInt;         // 2a
        @Document.DoubleProperty
        Double[] mArrBoxDouble;     // 2a
        @Document.DoubleProperty
        double[] mArrUnboxDouble;   // 2b
        @Document.DoubleProperty
        Float[] mArrBoxFloat;       // 2a
        @Document.DoubleProperty
        float[] mArrUnboxFloat;     // 2a
        @Document.BooleanProperty
        Boolean[] mArrBoxBoolean;   // 2a
        @Document.BooleanProperty
        boolean[] mArrUnboxBoolean; // 2b
        @Document.BytesProperty
        byte[][] mArrUnboxByteArr;  // 2b
        @Document.BytesProperty
        Byte[] mBoxByteArr;         // 2a
        @Document.StringProperty
        String[] mArrString;        // 2b
        @Document.DocumentProperty
        Card[] mArrCard;            // 2c

        // Single values
        @Document.StringProperty
        String mString;        // 3a
        @Document.LongProperty
        Long mBoxLong;         // 3a
        @Document.LongProperty
        long mUnboxLong;       // 3b
        @Document.LongProperty
        Integer mBoxInteger;   // 3a
        @Document.LongProperty
        int mUnboxInt;         // 3b
        @Document.DoubleProperty
        Double mBoxDouble;     // 3a
        @Document.DoubleProperty
        double mUnboxDouble;   // 3b
        @Document.DoubleProperty
        Float mBoxFloat;       // 3a
        @Document.DoubleProperty
        float mUnboxFloat;     // 3b
        @Document.BooleanProperty
        Boolean mBoxBoolean;   // 3a
        @Document.BooleanProperty
        boolean mUnboxBoolean; // 3b
        @Document.BytesProperty
        byte[] mUnboxByteArr;  // 3a
        @Document.DocumentProperty
        Card mCard;            // 3c

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Gift)) {
                return false;
            }
            Gift otherGift = (Gift) other;
            assertThat(otherGift.mNamespace).isEqualTo(this.mNamespace);
            assertThat(otherGift.mId).isEqualTo(this.mId);
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

        public static Gift createPopulatedGift() {
            Gift gift = new Gift();
            gift.mNamespace = "gift.namespace";
            gift.mId = "gift.id";

            gift.mArrBoxBoolean = new Boolean[]{true, false};
            gift.mArrBoxDouble = new Double[]{0.0, 1.0};
            gift.mArrBoxFloat = new Float[]{2.0F, 3.0F};
            gift.mArrBoxInteger = new Integer[]{4, 5};
            gift.mArrBoxLong = new Long[]{6L, 7L};
            gift.mArrString = new String[]{"cat", "dog"};
            gift.mBoxByteArr = new Byte[]{8, 9};
            gift.mArrUnboxBoolean = new boolean[]{false, true};
            gift.mArrUnboxByteArr = new byte[][]{{0, 1}, {2, 3}};
            gift.mArrUnboxDouble = new double[]{1.0, 0.0};
            gift.mArrUnboxFloat = new float[]{3.0f, 2.0f};
            gift.mArrUnboxInt = new int[]{5, 4};
            gift.mArrUnboxLong = new long[]{7, 6};

            Card card1 = new Card();
            card1.mNamespace = "card.namespace";
            card1.mId = "card.id1";
            Card card2 = new Card();
            card2.mNamespace = "card.namespace";
            card2.mId = "card.id2";
            gift.mArrCard = new Card[]{card2, card2};

            gift.mCollectLong = Arrays.asList(gift.mArrBoxLong);
            gift.mCollectInteger = Arrays.asList(gift.mArrBoxInteger);
            gift.mCollectBoolean = Arrays.asList(gift.mArrBoxBoolean);
            gift.mCollectString = Arrays.asList(gift.mArrString);
            gift.mCollectDouble = Arrays.asList(gift.mArrBoxDouble);
            gift.mCollectFloat = Arrays.asList(gift.mArrBoxFloat);
            gift.mCollectByteArr = Arrays.asList(gift.mArrUnboxByteArr);
            gift.mCollectCard = Arrays.asList(card2, card2);

            gift.mString = "String";
            gift.mBoxLong = 1L;
            gift.mUnboxLong = 2L;
            gift.mBoxInteger = 3;
            gift.mUnboxInt = 4;
            gift.mBoxDouble = 5.0;
            gift.mUnboxDouble = 6.0;
            gift.mBoxFloat = 7.0F;
            gift.mUnboxFloat = 8.0f;
            gift.mBoxBoolean = true;
            gift.mUnboxBoolean = false;
            gift.mUnboxByteArr = new byte[]{1, 2, 3};
            gift.mCard = card1;

            return gift;
        }
    }

    @Test
    public void testAnnotationProcessor() throws Exception {
        //TODO(b/156296904) add test for int, float, GenericDocument, and class with
        // @Document annotation
        mSession.setSchema(
                new SetSchemaRequest.Builder().addDocumentClasses(Card.class, Gift.class).build())
                .get();

        // Create a Gift object and assign values.
        Gift inputDocument = Gift.createPopulatedGift();

        // Index the Gift document and query it.
        checkIsBatchResultSuccess(mSession.put(
                new PutDocumentsRequest.Builder().addDocuments(inputDocument).build()));
        SearchResults searchResults = mSession.search("", new SearchSpec.Builder()
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(1);

        // Convert GenericDocument to Gift and check values.
        Gift outputDocument = documents.get(0).toDocumentClass(Gift.class);
        assertThat(outputDocument).isEqualTo(inputDocument);
    }

    @Test
    public void testAnnotationProcessor_queryByType() throws Exception {
        mSession.setSchema(
                new SetSchemaRequest.Builder()
                        .addDocumentClasses(Card.class, Gift.class)
                        .addSchemas(AppSearchEmail.SCHEMA).build())
                .get();

        // Create documents and index them
        Gift inputDocument1 = new Gift();
        inputDocument1.mNamespace = "gift.namespace";
        inputDocument1.mId = "gift.id1";
        Gift inputDocument2 = new Gift();
        inputDocument2.mNamespace = "gift.namespace";
        inputDocument2.mId = "gift.id2";
        AppSearchEmail email1 =
                new AppSearchEmail.Builder("namespace", "id3")
                        .setFrom("from@example.com")
                        .setTo("to1@example.com", "to2@example.com")
                        .setSubject("testPut example")
                        .setBody("This is the body of the testPut email")
                        .build();
        checkIsBatchResultSuccess(mSession.put(
                new PutDocumentsRequest.Builder()
                        .addDocuments(inputDocument1, inputDocument2)
                        .addGenericDocuments(email1).build()));

        // Query the documents by it's schema type.
        SearchResults searchResults = mSession.search("",
                new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                        .addFilterSchemas("Gift", AppSearchEmail.SCHEMA_TYPE)
                        .build());
        List<GenericDocument> documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(3);

        // Query the documents by it's class.
        searchResults = mSession.search("",
                new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                        .addFilterDocumentClasses(Gift.class)
                        .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(2);

        // Query the documents by schema type and class mix.
        searchResults = mSession.search("",
                new SearchSpec.Builder()
                        .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                        .addFilterSchemas(AppSearchEmail.SCHEMA_TYPE)
                        .addFilterDocumentClasses(Gift.class)
                        .build());
        documents = convertSearchResultsToDocuments(searchResults);
        assertThat(documents).hasSize(3);
    }

    @Test
    public void testGenericDocumentConversion() throws Exception {
        Gift inGift = Gift.createPopulatedGift();
        GenericDocument genericDocument1 = GenericDocument.fromDocumentClass(inGift);
        GenericDocument genericDocument2 = GenericDocument.fromDocumentClass(inGift);
        Gift outGift = genericDocument2.toDocumentClass(Gift.class);

        assertThat(inGift).isNotSameInstanceAs(outGift);
        assertThat(inGift).isEqualTo(outGift);
        assertThat(genericDocument1).isNotSameInstanceAs(genericDocument2);
        assertThat(genericDocument1).isEqualTo(genericDocument2);
    }
}
