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
import androidx.room.RawQuery
import androidx.room.RoomWarnings
import androidx.room.Transaction
import androidx.room.TypeConverters
import androidx.room.Update
import androidx.room.integration.kotlintestapp.vo.AnswerConverter
import androidx.room.integration.kotlintestapp.vo.Author
import androidx.room.integration.kotlintestapp.vo.Book
import androidx.room.integration.kotlintestapp.vo.BookAuthor
import androidx.room.integration.kotlintestapp.vo.BookWithJavaEntity
import androidx.room.integration.kotlintestapp.vo.BookWithPublisher
import androidx.room.integration.kotlintestapp.vo.DateConverter
import androidx.room.integration.kotlintestapp.vo.Lang
import androidx.room.integration.kotlintestapp.vo.MiniBook
import androidx.room.integration.kotlintestapp.vo.Publisher
import androidx.room.integration.kotlintestapp.vo.PublisherWithBookSales
import androidx.room.integration.kotlintestapp.vo.PublisherWithBooks
import androidx.sqlite.db.SupportSQLiteQuery
import com.google.common.base.Optional
import com.google.common.util.concurrent.ListenableFuture
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Single
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import java.util.Date

@Dao
@TypeConverters(DateConverter::class, AnswerConverter::class)
interface BooksDao {

    @Insert
    fun addPublishers(vararg publishers: Publisher): List<Long>

    @Insert
    fun addPublishersSingle(vararg publishers: Publisher): Single<List<Long>>

    @Insert
    fun addPublishersCompletable(vararg publishers: Publisher): Completable

    @Insert
    fun addPublishersMaybe(vararg publishers: Publisher): Maybe<List<Long>>

    @Insert
    fun addPublisherSingle(publisher: Publisher): Single<Long>

    @Insert
    fun addPublisherCompletable(publisher: Publisher): Completable

    @Insert
    fun addPublisherMaybe(publisher: Publisher): Maybe<Long>

    @Insert
    fun addPublisherSuspend(publisher: Publisher)

    @Delete
    fun deletePublishers(vararg publishers: Publisher)

    @Delete
    fun deletePublishersSingle(vararg publishers: Publisher): Single<Int>

    @Delete
    fun deletePublishersCompletable(vararg publishers: Publisher): Completable

    @Delete
    fun deletePublishersMaybe(vararg publishers: Publisher): Maybe<Int>

    @Delete
    fun deletePublishersCount(vararg publishers: Publisher): Int

    @Update
    fun updatePublishers(vararg publishers: Publisher)

    @Update
    fun updatePublishersCompletable(vararg publishers: Publisher): Completable

    @Update
    fun updatePublishersMaybe(vararg publishers: Publisher): Maybe<Int>

    @Update
    fun updatePublishersSingle(vararg publishers: Publisher): Single<Int>

    @Update
    fun updatePublishersCount(vararg publishers: Publisher): Int

    @Insert
    fun addAuthors(vararg authors: Author)

    @Insert
    suspend fun addAuthorsSuspend(vararg authors: Author)

    @Query("SELECT * FROM author WHERE authorId = :authorId")
    fun getAuthor(authorId: String): Author

    @Insert
    fun addBooks(vararg books: Book)

    @Insert(entity = Book::class)
    fun addMiniBook(miniBook: MiniBook)

    @Insert
    fun addBookAuthors(vararg bookAuthors: BookAuthor)

    @Query("SELECT * FROM book WHERE bookId = :bookId")
    fun getBook(bookId: String): Book

    @Query("SELECT * FROM book WHERE bookId = :bookId")
    suspend fun getBookSuspend(bookId: String): Book

    @Query("SELECT * FROM book")
    suspend fun getBooksSuspend(): List<Book>

    @Query("UPDATE book SET salesCnt = salesCnt + 1 WHERE bookId = :bookId")
    fun increaseBookSales(bookId: String)

    @Query("UPDATE book SET salesCnt = salesCnt + 1 WHERE bookId = :bookId")
    suspend fun increaseBookSalesSuspend(bookId: String)

    @Query("UPDATE book SET salesCnt = salesCnt + 1 WHERE bookId = :bookId")
    fun increaseBookSalesSingle(bookId: String): Single<Int>

    @Query("UPDATE book SET salesCnt = salesCnt + 1 WHERE bookId = :bookId")
    fun increaseBookSalesMaybe(bookId: String): Maybe<Int>

    @Query("UPDATE book SET salesCnt = salesCnt + 1 WHERE bookId = :bookId")
    fun increaseBookSalesCompletable(bookId: String): Completable

    @Query("UPDATE book SET salesCnt = salesCnt + 1 WHERE bookId = :bookId")
    fun increaseBookSalesFuture(bookId: String): ListenableFuture<Int>

    @Query("UPDATE book SET salesCnt = salesCnt + 1 WHERE bookId = :bookId")
    fun increaseBookSalesVoidFuture(bookId: String): ListenableFuture<Void>

    @Query("DELETE FROM book WHERE salesCnt = 0")
    fun deleteUnsoldBooks(): Int

    @Query("DELETE FROM book WHERE salesCnt = 0")
    suspend fun deleteUnsoldBooksSuspend(): Int

    @Query("DELETE FROM book WHERE salesCnt = 0")
    fun deleteUnsoldBooksSingle(): Single<Int>

    @Query("DELETE FROM book WHERE salesCnt = 0")
    fun deleteUnsoldBooksMaybe(): Maybe<Int>

    @Query("DELETE FROM book WHERE salesCnt = 0")
    fun deleteUnsoldBooksCompletable(): Completable

    @Query("DELETE FROM book WHERE salesCnt = 0")
    fun deleteUnsoldBooksFuture(): ListenableFuture<Int>

    @Query("DELETE FROM book WHERE salesCnt = 0")
    fun deleteUnsoldBooksVoidFuture(): ListenableFuture<Void>

    @Query("DELETE FROM book WHERE bookId IN (:bookIds)")
    fun deleteBookWithIds(vararg bookIds: String)

    @Query("DELETE FROM book WHERE bookId IN (:bookIds)")
    suspend fun deleteBookWithIdsSuspend(vararg bookIds: String)

    @Query("DELETE FROM book WHERE bookId IN (:bookIds)")
    fun deleteBookWithIdsSingle(vararg bookIds: String): Single<Int>

    @Query("DELETE FROM book WHERE bookId IN (:bookIds)")
    fun deleteBookWithIdsMaybe(vararg bookIds: String): Maybe<Int>

    @Query("DELETE FROM book WHERE bookId IN (:bookIds)")
    fun deleteBookWithIdsCompletable(vararg bookIds: String): Completable

    @Query("DELETE FROM book WHERE bookId IN (:bookIds)")
    fun deleteBookWithIdsFuture(vararg bookIds: String): ListenableFuture<Int>

    @Query("INSERT INTO publisher (publisherId, name) VALUES (:id, :name)")
    fun insertPublisherVoid(id: String, name: String)

    @Query("INSERT INTO publisher (publisherId, name) VALUES (:id, :name)")
    fun insertPublisher(id: String, name: String): Long

    @Query("INSERT INTO publisher (publisherId, name) VALUES (:id, :name)")
    suspend fun insertPublisherSuspend(id: String, name: String): Long

    @Query("INSERT INTO publisher (publisherId, name) VALUES (:id, :name)")
    fun insertPublisherSingle(id: String, name: String): Single<Long>

    @Query("INSERT INTO publisher (publisherId, name) VALUES (:id, :name)")
    fun insertPublisherMaybe(id: String, name: String): Maybe<Long>

    @Query("INSERT INTO publisher (publisherId, name) VALUES (:id, :name)")
    fun insertPublisherCompletable(id: String, name: String): Completable

    @Query("INSERT INTO publisher (publisherId, name) VALUES (:id, :name)")
    fun insertPublisherFuture(id: String, name: String): ListenableFuture<Long>

    @Transaction
    @Query("SELECT * FROM book WHERE salesCnt > :count")
    suspend fun getBooksWithMinSalesCountSuspend(count: Int): List<Book>

    @RawQuery
    suspend fun getBookWithRawQuerySuspend(query: SupportSQLiteQuery): Book

    @Insert
    suspend fun insertBookSuspend(book: Book)

    @Insert
    suspend fun insertBookWithResultSuspend(book: Book): Long

    @Insert
    suspend fun insertBooksWithResultSuspend(vararg book: Book): List<Long>

    @Delete
    suspend fun deleteBookSuspend(book: Book)

    @Delete
    suspend fun deleteBookWithResultSuspend(book: Book): Int

    @Update
    suspend fun updateBookSuspend(book: Book)

    @Update
    suspend fun updateBookWithResultSuspend(book: Book): Int

    @Query(
        """SELECT * FROM book WHERE
            bookId IN(:bookIds)
            order by bookId DESC"""
    )
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

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query(
        "SELECT * FROM book INNER JOIN publisher " +
            "ON book.bookPublisherId = publisher.publisherId "
    )
    fun getBooksWithPublisher(): List<BookWithPublisher>

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query(
        "SELECT * FROM book INNER JOIN publisher " +
            "ON book.bookPublisherId = publisher.publisherId "
    )
    fun getBooksWithPublisherLiveData(): LiveData<List<BookWithPublisher>>

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query(
        "SELECT * FROM book INNER JOIN publisher " +
            "ON book.bookPublisherId = publisher.publisherId "
    )
    fun getBooksWithPublisherFlowable(): Flowable<List<BookWithPublisher>>

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query(
        "SELECT * FROM book INNER JOIN publisher " +
            "ON book.bookPublisherId = publisher.publisherId "
    )
    fun getBooksWithPublisherListenableFuture(): ListenableFuture<List<BookWithPublisher>>

    @Transaction
    @Query("SELECT * FROM publisher WHERE publisherId = :publisherId")
    fun getPublisherWithBooks(publisherId: String): PublisherWithBooks

    @Transaction
    @Query("SELECT * FROM publisher WHERE publisherId = :publisherId")
    fun getPublisherWithBookSales(publisherId: String): PublisherWithBookSales

    @Transaction
    @Query("SELECT * FROM publisher WHERE publisherId = :publisherId")
    fun getPublisherWithBooksLiveData(publisherId: String): LiveData<PublisherWithBooks>

    @Transaction
    @Query("SELECT * FROM publisher WHERE publisherId = :publisherId")
    fun getPublisherWithBooksFlowable(publisherId: String): Flowable<PublisherWithBooks>

    @Query("UPDATE book SET title = :title WHERE bookId = :bookId")
    fun updateBookTitle(bookId: String, title: String?)

    @Query("SELECT * FROM book WHERE languages & :langs != 0 ORDER BY bookId ASC")
    @TypeConverters(Lang::class)
    fun findByLanguages(langs: Set<Lang>): List<Book>

    // see: b/78199923 just a compilation test to ensure we can generate proper code.
    @Transaction
    @Query("SELECT * FROM book WHERE bookId = :bookId")
    fun getWithJavaEntities(bookId: String): BookWithJavaEntity

    @Transaction
    fun deleteAndAddPublisher(
        oldPublisher: Publisher,
        newPublisher: Publisher,
        fail: Boolean = false
    ) {
        deletePublishers(oldPublisher)
        if (fail) {
            throw RuntimeException()
        }
        addPublishers(newPublisher)
    }

    @Transaction
    fun getDefaultBook() = getBook("DEFAULT_ID")

    @Query("SELECT * FROM Publisher")
    fun getPublishers(): List<Publisher>

    @Query("SELECT * FROM Publisher WHERE publisherId = :publisherId")
    fun getPublisher(publisherId: String): Publisher

    @Query("SELECT * FROM Publisher WHERE _rowid_ = :rowid")
    fun getPublisher(rowid: Long): Publisher

    @Query("SELECT dateOfBirth FROM author")
    suspend fun getAllAuthorsDateOfBirth(): List<Date>

    @Query("SELECT dateOfBirth FROM author WHERE authorId = :authorId")
    suspend fun getAuthorDateOfBirths(authorId: String): Date

    @Query("SELECT * FROM author WHERE dateOfBirth IN (:dates)")
    fun getAuthorsWithBirthDatesList(dates: List<Date>): List<Author>

    @Query("SELECT * FROM author WHERE dateOfBirth IN (:dates)")
    fun getAuthorsWithBirthDatesVararg(vararg dates: Date): List<Author>

    // see: b/123767877, suspend function with inner class as parameter issues.
    @Query("SELECT 0 FROM book WHERE bookId = :param")
    suspend fun getZero(param: AnswerConverter.Answer): Int

    // see: b/123767877, suspend function with inner class as parameter issues.
    @Query("SELECT 'YES' FROM book")
    suspend fun getAnswer(): AnswerConverter.Answer

    @Transaction
    suspend fun insertBookAndAuthorSuspend(book: Book, author: Author) {
        addBooks(book)
        addAuthors(author)
    }

    @Query("SELECT * FROM book WHERE salesCnt = :count")
    suspend fun getBooksSalesCountSuspend(count: Int): List<Book>

    @Transaction
    suspend fun deleteBooksWithZeroSales(): List<Book> {
        val books = getBooksSalesCountSuspend(0)
        deleteBookWithIds(*books.map { it.bookId }.toTypedArray())
        return books
    }

    @Transaction
    suspend fun addAuthorPublisherBooks(author: Author, publisher: Publisher, vararg books: Book) {
        addAuthorsSuspend(author)
        addPublisherSuspend(publisher)
        for (book in books) {
            insertBookSuspend(book)
        }
    }

    @Query("SELECT * FROM book")
    fun getBooksFlow(): Flow<List<Book>>

    @Transaction
    @Query("SELECT * FROM book")
    fun getBooksFlowInTransaction(): Flow<List<Book>>

    @Query("SELECT * FROM book WHERE bookId = :id")
    fun getOneBooksFlow(id: String): Flow<Book?>

    fun addAndRemovePublisher(thePublisher: Publisher) {
        addPublishers(thePublisher)
        deletePublishers(thePublisher)
    }

    fun concreteFunction() = ""

    fun concreteVoidFunction() {
    }

    fun concreteUnitFunction() {
    }

    fun concreteFunctionWithParams(num: Int, text: String) = "$num - $text"

    suspend fun concreteSuspendFunction() = ""

    suspend fun concreteVoidSuspendFunction() {
    }

    suspend fun concreteSuspendFunctionWithParams(num: Int, text: String) = "$num - $text"

    @Transaction
    fun functionWithSuspendFunctionalParam(
        input: Book,
        action: suspend (input: Book) -> Book
    ): Book = runBlocking { action(input) }

    @Transaction
    suspend fun suspendFunctionWithSuspendFunctionalParam(
        input: Book,
        action: suspend (input: Book) -> Book
    ): Book = action(input)

    // This is a private method to validate b/194706278
    private fun getNullAuthor(): Author? = null
}
