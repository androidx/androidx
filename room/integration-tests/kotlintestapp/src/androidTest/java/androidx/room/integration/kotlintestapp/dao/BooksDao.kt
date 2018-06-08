/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.room.integration.kotlintestapp.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.TypeConverters
import androidx.room.integration.kotlintestapp.vo.Author
import androidx.room.integration.kotlintestapp.vo.Book
import androidx.room.integration.kotlintestapp.vo.BookAuthor
import androidx.room.integration.kotlintestapp.vo.BookWithPublisher
import androidx.room.integration.kotlintestapp.vo.Lang
import androidx.room.integration.kotlintestapp.vo.Publisher
import androidx.room.integration.kotlintestapp.vo.PublisherWithBookSales
import androidx.room.integration.kotlintestapp.vo.PublisherWithBooks
import androidx.room.integration.kotlintestapp.vo.BookWithJavaEntity
import com.google.common.base.Optional
import com.google.common.util.concurrent.ListenableFuture
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Single

@Dao
interface BooksDao {

    @Insert
    fun addPublishers(vararg publishers: Publisher)

    @Delete
    fun deletePublishers(vararg publishers: Publisher)

    @Insert
    fun addAuthors(vararg authors: Author)

    @Query("SELECT * FROM author WHERE authorId = :authorId")
    fun getAuthor(authorId: String): Author

    @Insert
    fun addBooks(vararg books: Book)

    @Insert
    fun addBookAuthors(vararg bookAuthors: BookAuthor)

    @Query("SELECT * FROM book WHERE bookId = :bookId")
    fun getBook(bookId: String): Book

    @Query("""SELECT * FROM book WHERE
            bookId IN(:bookIds)
            order by bookId DESC""")
    fun getBooksMultiLineQuery(bookIds: List<String>): List<Book>

    @Query("SELECT * FROM book WHERE bookId = :bookId")
    fun getBookLiveData(bookId: String): LiveData<Book>

    @Query("SELECT * FROM book WHERE bookId = :bookId")
    fun getBookFlowable(bookId: String): Flowable<Book>

    @Query("SELECT * FROM book WHERE bookId = :bookId")
    fun getBookJavaOptional(bookId: String): java.util.Optional<Book>

    @Query("SELECT * FROM book WHERE bookId = :bookId")
    fun getBookListenableFuture(bookId: String): ListenableFuture<Book>

    @Query("SELECT * FROM book WHERE bookId = :bookId")
    fun getBookOptional(bookId: String): Optional<Book>

    @Query("SELECT * FROM book WHERE bookId = :bookId")
    fun getBookOptionalFlowable(bookId: String): Flowable<Optional<Book>>

    @Query("SELECT * FROM book WHERE bookId = :bookId")
    fun getBookOptionalListenableFuture(bookId: String): ListenableFuture<Optional<Book>>

    @Query("SELECT * FROM book WHERE bookId = :bookId")
    fun getBookSingle(bookId: String): Single<Book>

    @Query("SELECT * FROM book WHERE bookId = :bookId")
    fun getBookMaybe(bookId: String): Maybe<Book>

    @Query("SELECT * FROM book INNER JOIN publisher " +
            "ON book.bookPublisherId = publisher.publisherId ")
    fun getBooksWithPublisher(): List<BookWithPublisher>

    @Query("SELECT * FROM book INNER JOIN publisher " +
            "ON book.bookPublisherId = publisher.publisherId ")
    fun getBooksWithPublisherLiveData(): LiveData<List<BookWithPublisher>>

    @Query("SELECT * FROM book INNER JOIN publisher " +
            "ON book.bookPublisherId = publisher.publisherId ")
    fun getBooksWithPublisherFlowable(): Flowable<List<BookWithPublisher>>

    @Query("SELECT * FROM book INNER JOIN publisher " +
            "ON book.bookPublisherId = publisher.publisherId ")
    fun getBooksWithPublisherListenableFuture(): ListenableFuture<List<BookWithPublisher>>

    @Query("SELECT * FROM publisher WHERE publisherId = :publisherId")
    fun getPublisherWithBooks(publisherId: String): PublisherWithBooks

    @Query("SELECT * FROM publisher WHERE publisherId = :publisherId")
    fun getPublisherWithBookSales(publisherId: String): PublisherWithBookSales

    @Query("SELECT * FROM publisher WHERE publisherId = :publisherId")
    fun getPublisherWithBooksLiveData(publisherId: String): LiveData<PublisherWithBooks>

    @Query("SELECT * FROM publisher WHERE publisherId = :publisherId")
    fun getPublisherWithBooksFlowable(publisherId: String): Flowable<PublisherWithBooks>

    @Query("UPDATE book SET title = :title WHERE bookId = :bookId")
    fun updateBookTitle(bookId: String, title: String?)

    @Query("SELECT * FROM book WHERE languages & :langs != 0 ORDER BY bookId ASC")
    @TypeConverters(Lang::class)
    fun findByLanguages(langs: Set<Lang>): List<Book>

    // see: b/78199923 just a compilation test to ensure we can generate proper code.
    @Query("SELECT * FROM book WHERE bookId = :bookId")
    fun getWithJavaEntities(bookId: String): BookWithJavaEntity

    @Transaction
    fun deleteAndAddPublisher(oldPublisher: Publisher, newPublisher: Publisher,
            fail: Boolean = false) {
        deletePublishers(oldPublisher)
        if (fail) {
            throw RuntimeException()
        }
        addPublishers(newPublisher)
    }

    @Query("SELECT * FROM Publisher")
    fun getPublishers(): List<Publisher>
}
