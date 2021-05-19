/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.appsearch.ktx

import android.content.Context
import androidx.appsearch.annotation.Document
import androidx.appsearch.app.AppSearchSchema
import androidx.appsearch.app.AppSearchSession
import androidx.appsearch.app.GenericDocument
import androidx.appsearch.app.PutDocumentsRequest
import androidx.appsearch.app.SearchResult
import androidx.appsearch.app.SearchResults
import androidx.appsearch.app.SearchSpec
import androidx.appsearch.app.SetSchemaRequest
import androidx.appsearch.localstorage.LocalStorage
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test

public class AnnotationProcessorKtTest {
    private companion object {
        private const val DB_NAME = ""
    }

    private lateinit var session: AppSearchSession

    @Before
    public fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        session = LocalStorage.createSearchSession(
            LocalStorage.SearchContext.Builder(context, DB_NAME).build()
        ).get()

        // Cleanup whatever documents may still exist in these databases. This is needed in
        // addition to tearDown in case a test exited without completing properly.
        cleanup()
    }

    @After
    public fun tearDown() {
        // Cleanup whatever documents may still exist in these databases.
        cleanup()
    }

    private fun cleanup() {
        session.setSchema(SetSchemaRequest.Builder().setForceOverride(true).build()).get()
    }

    @Document
    internal data class Card(
        @Document.Namespace
        val namespace: String,

        @Document.Id
        val id: String,

        @Document.CreationTimestampMillis
        val creationTimestampMillis: Long = 0L,

        @Document.StringProperty(
            indexingType = AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES,
            tokenizerType = AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN
        )
        val string: String? = null,
    )

    @Document
    internal data class Gift(
        @Document.Namespace
        val namespace: String,

        @Document.Id
        val id: String,

        @Document.CreationTimestampMillis
        val creationTimestampMillis: Long = 0L,

        // Collections
        @Document.LongProperty
        val collectLong: Collection<Long>,

        @Document.LongProperty
        val collectInteger: Collection<Int>,

        @Document.DoubleProperty
        val collectDouble: Collection<Double>,

        @Document.DoubleProperty
        val collectFloat: Collection<Float>,

        @Document.BooleanProperty
        val collectBoolean: Collection<Boolean>,

        @Document.BytesProperty
        val collectByteArr: Collection<ByteArray>,

        @Document.StringProperty
        val collectString: Collection<String>,

        @Document.DocumentProperty
        val collectCard: Collection<Card>,

        // Arrays
        @Document.LongProperty
        val arrBoxLong: Array<Long>,

        @Document.LongProperty
        val arrUnboxLong: LongArray,

        @Document.LongProperty
        val arrBoxInteger: Array<Int>,

        @Document.LongProperty
        val arrUnboxInt: IntArray,

        @Document.DoubleProperty
        val arrBoxDouble: Array<Double>,

        @Document.DoubleProperty
        val arrUnboxDouble: DoubleArray,

        @Document.DoubleProperty
        val arrBoxFloat: Array<Float>,

        @Document.DoubleProperty
        val arrUnboxFloat: FloatArray,

        @Document.BooleanProperty
        val arrBoxBoolean: Array<Boolean>,

        @Document.BooleanProperty
        val arrUnboxBoolean: BooleanArray,

        @Document.BytesProperty
        val arrUnboxByteArr: Array<ByteArray>,

        @Document.BytesProperty
        val boxByteArr: Array<Byte>,

        @Document.StringProperty
        val arrString: Array<String>,

        @Document.DocumentProperty
        val arrCard: Array<Card>,

        // Single values
        @Document.StringProperty
        val string: String,

        @Document.LongProperty
        val boxLong: Long,

        @Document.LongProperty
        val unboxLong: Long = 0,

        @Document.LongProperty
        val boxInteger: Int,

        @Document.LongProperty
        val unboxInt: Int = 0,

        @Document.DoubleProperty
        val boxDouble: Double,

        @Document.DoubleProperty
        val unboxDouble: Double = 0.0,

        @Document.DoubleProperty
        val boxFloat: Float,

        @Document.DoubleProperty
        val unboxFloat: Float = 0f,

        @Document.BooleanProperty
        val boxBoolean: Boolean,

        @Document.BooleanProperty
        val unboxBoolean: Boolean = false,

        @Document.BytesProperty
        val unboxByteArr: ByteArray,

        @Document.DocumentProperty
        val card: Card
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Gift

            if (namespace != other.namespace) return false
            if (id != other.id) return false
            if (collectLong != other.collectLong) return false
            if (collectInteger != other.collectInteger) return false
            if (collectDouble != other.collectDouble) return false
            if (collectFloat != other.collectFloat) return false
            if (collectBoolean != other.collectBoolean) return false
            // It's complicated to do a deep comparison of this, so we skip it
            // if (collectByteArr != other.collectByteArr) return false
            if (collectString != other.collectString) return false
            if (collectCard != other.collectCard) return false
            if (!arrBoxLong.contentEquals(other.arrBoxLong)) return false
            if (!arrUnboxLong.contentEquals(other.arrUnboxLong)) return false
            if (!arrBoxInteger.contentEquals(other.arrBoxInteger)) return false
            if (!arrUnboxInt.contentEquals(other.arrUnboxInt)) return false
            if (!arrBoxDouble.contentEquals(other.arrBoxDouble)) return false
            if (!arrUnboxDouble.contentEquals(other.arrUnboxDouble)) return false
            if (!arrBoxFloat.contentEquals(other.arrBoxFloat)) return false
            if (!arrUnboxFloat.contentEquals(other.arrUnboxFloat)) return false
            if (!arrBoxBoolean.contentEquals(other.arrBoxBoolean)) return false
            if (!arrUnboxBoolean.contentEquals(other.arrUnboxBoolean)) return false
            if (!arrUnboxByteArr.contentDeepEquals(other.arrUnboxByteArr)) return false
            if (!boxByteArr.contentEquals(other.boxByteArr)) return false
            if (!arrString.contentEquals(other.arrString)) return false
            if (!arrCard.contentEquals(other.arrCard)) return false
            if (string != other.string) return false
            if (boxLong != other.boxLong) return false
            if (unboxLong != other.unboxLong) return false
            if (boxInteger != other.boxInteger) return false
            if (unboxInt != other.unboxInt) return false
            if (boxDouble != other.boxDouble) return false
            if (unboxDouble != other.unboxDouble) return false
            if (boxFloat != other.boxFloat) return false
            if (unboxFloat != other.unboxFloat) return false
            if (boxBoolean != other.boxBoolean) return false
            if (unboxBoolean != other.unboxBoolean) return false
            if (!unboxByteArr.contentEquals(other.unboxByteArr)) return false
            if (card != other.card) return false

            return true
        }
    }

    @Test
    public fun testAnnotationProcessor() {
        session.setSchema(
            SetSchemaRequest.Builder()
                .addDocumentClasses(Card::class.java, Gift::class.java).build()
        ).get()

        // Create a Gift object and assign values.
        val inputDocument = createPopulatedGift()

        // Index the Gift document and query it.
        session.put(PutDocumentsRequest.Builder().addDocuments(inputDocument).build())
            .get().checkSuccess()
        val searchResults = session.search("", SearchSpec.Builder().build())
        val documents = convertSearchResultsToDocuments(searchResults)
        assertThat(documents).hasSize(1)

        // Convert GenericDocument to Gift and check values.
        val outputDocument = documents[0].toDocumentClass(Gift::class.java)
        assertThat(outputDocument).isEqualTo(inputDocument)
    }

    @Test
    public fun testGenericDocumentConversion() {
        val inGift = createPopulatedGift()
        val genericDocument1 = GenericDocument.fromDocumentClass(inGift)
        val genericDocument2 = GenericDocument.fromDocumentClass(inGift)
        val outGift = genericDocument2.toDocumentClass(Gift::class.java)
        assertThat(inGift).isNotSameInstanceAs(outGift)
        assertThat(inGift).isEqualTo(outGift)
        assertThat(genericDocument1).isNotSameInstanceAs(genericDocument2)
        assertThat(genericDocument1).isEqualTo(genericDocument2)
    }

    private fun createPopulatedGift(): Gift {
        val card1 = Card("card.namespace", "card.id1")
        val card2 = Card("card.namespace", "card.id2")
        return Gift(
            namespace = "gift.namespace",
            id = "gift.id",
            arrBoxBoolean = arrayOf(true, false),
            arrBoxDouble = arrayOf(0.0, 1.0),
            arrBoxFloat = arrayOf(2.0f, 3.0f),
            arrBoxInteger = arrayOf(4, 5),
            arrBoxLong = arrayOf(6L, 7L),
            arrString = arrayOf("cat", "dog"),
            boxByteArr = arrayOf(8, 9),
            arrUnboxBoolean = booleanArrayOf(false, true),
            arrUnboxByteArr = arrayOf(byteArrayOf(0, 1), byteArrayOf(2, 3)),
            arrUnboxDouble = doubleArrayOf(1.0, 0.0),
            arrUnboxFloat = floatArrayOf(3.0f, 2.0f),
            arrUnboxInt = intArrayOf(5, 4),
            arrUnboxLong = longArrayOf(7, 6),
            arrCard = arrayOf(card2, card2),
            collectLong = listOf(6L, 7L),
            collectInteger = listOf(4, 5),
            collectBoolean = listOf(false, true),
            collectString = listOf("cat", "dog"),
            collectDouble = listOf(0.0, 1.0),
            collectFloat = listOf(2.0f, 3.0f),
            collectByteArr = listOf(byteArrayOf(0, 1), byteArrayOf(2, 3)),
            collectCard = listOf(card2, card2),
            string = "String",
            boxLong = 1L,
            unboxLong = 2L,
            boxInteger = 3,
            unboxInt = 4,
            boxDouble = 5.0,
            unboxDouble = 6.0,
            boxFloat = 7.0f,
            unboxFloat = 8.0f,
            boxBoolean = true,
            unboxBoolean = false,
            unboxByteArr = byteArrayOf(1, 2, 3),
            card = card1
        )
    }

    private fun convertSearchResultsToDocuments(
        searchResults: SearchResults
    ): List<GenericDocument> {
        var page = searchResults.nextPage.get()
        val results = mutableListOf<SearchResult>()
        while (page.isNotEmpty()) {
            results.addAll(page)
            page = searchResults.nextPage.get()
        }
        return results.map { it.genericDocument }
    }
}
