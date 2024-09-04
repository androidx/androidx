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

package androidx.room.integration.kotlintestapp.test

import androidx.kruth.assertThat
import androidx.lifecycle.LiveData
import androidx.lifecycle.asFlow
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.integration.kotlintestapp.assumeKsp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.ListenableFuture
import io.reactivex.Flowable
import io.reactivex.Observable
import java.util.Optional
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * This test matters in KSP specifically where we might use primitive adapter for non-null java
 * primitives.
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class BoxedNonNullTypesTest {
    lateinit var db: MyDb

    @Before
    fun init() {
        db =
            Room.inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    MyDb::class.java
                )
                .build()
    }

    @Test
    fun list() {
        db.myDao().insert(MyEntity(3))
        assertThat(db.myDao().getAsList()).containsExactly(3L)
    }

    @Test
    fun list_nullable() {
        assumeKsp()
        db.myDao().insert(MyNullableEntity(null), MyNullableEntity(3L))
        assertThat(db.myDao().getAsNullableList()).containsExactly(null, 3L)
    }

    @Test
    fun immutableList() {
        db.myDao().insert(MyEntity(4))
        assertThat(db.myDao().getAsImmutableList()).containsExactly(4L)
    }

    @Test
    @SdkSuppress(minSdkVersion = 24)
    fun javaOptional() {
        assertThat(db.myDao().getAsJavaOptional()).isEqualTo(Optional.empty<Long>())
        db.myDao().insert(MyEntity(5))
        assertThat(db.myDao().getAsJavaOptional()).isEqualTo(Optional.of(5L))
    }

    @Test
    fun guavaOptional() {
        assertThat(db.myDao().getAsGuavaOptional())
            .isEqualTo(com.google.common.base.Optional.absent<Long>())
        db.myDao().insert(MyEntity(6))
        assertThat(db.myDao().getAsGuavaOptional())
            .isEqualTo(com.google.common.base.Optional.of(6L))
    }

    @Test
    fun getAsLiveData() =
        runBlocking<Unit> {
            db.myDao().insert(MyEntity(7))
            assertThat(db.myDao().getAsLiveData().asFlow().first()).isEqualTo(7L)
        }

    @Test
    fun getAsLiveData_nullable() =
        runBlocking<Unit> {
            assumeKsp()
            db.myDao().insert(MyNullableEntity(null))
            assertThat(db.myDao().getAsNullableLiveData().asFlow().first()).isNull()
        }

    @Test
    fun getAsFlow() =
        runBlocking<Unit> {
            db.myDao().insert(MyEntity(8))
            assertThat(db.myDao().getAsFlow().first()).isEqualTo(8L)
        }

    @Test
    fun getAsFlow_nullable() =
        runBlocking<Unit> {
            assumeKsp()
            db.myDao().insert(MyNullableEntity(null))
            assertThat(db.myDao().getAsNullableFlow().first()).isNull()
        }

    @Test
    fun getAsRx2Observable() {
        db.myDao().insert(MyEntity(9))
        assertThat(db.myDao().getAsRx2Observable().blockingFirst()).isEqualTo(9L)
    }

    @Test // repro for: b/211822920
    fun getAsRx2ObservableUnknownNullabilityInCursor() {
        db.myDao().insert(MyEntity(9))
        assertThat(db.myDao().getAsRx2ObservableUnknownTypeInCursor().blockingFirst()).isEqualTo(9L)
    }

    @Test
    fun getAsRx2Flowable() {
        db.myDao().insert(MyEntity(10))
        assertThat(db.myDao().getAsRx2Flowable().blockingFirst()).isEqualTo(10L)
    }

    @Test
    fun getAsRx3Observable() {
        db.myDao().insert(MyEntity(11))
        assertThat(db.myDao().getAsRx3Observable().blockingFirst()).isEqualTo(11L)
    }

    @Test
    fun getAsRx3Flowable() {
        db.myDao().insert(MyEntity(12))
        assertThat(db.myDao().getAsRx3Flowable().blockingFirst()).isEqualTo(12L)
    }

    @Test
    fun getAsListenableFuture() {
        db.myDao().insert(MyEntity(13))
        assertThat(db.myDao().getAsListenableFuture().get()).isEqualTo(13L)
    }

    @Test
    fun getAsListenableFuture_nullable() {
        assumeKsp()
        db.myDao().insert(MyNullableEntity(null))
        assertThat(db.myDao().getAsNullableListenableFuture().get()).isEqualTo(null)
    }

    @Entity
    data class MyEntity(
        val value: Long,
        @PrimaryKey(autoGenerate = true) val id: Int = 0,
    )

    @Entity
    data class MyNullableEntity(
        val value: Long?,
        @PrimaryKey(autoGenerate = true) val id: Int = 0,
    )

    @Database(
        entities = [MyEntity::class, MyNullableEntity::class],
        version = 1,
        exportSchema = false
    )
    abstract class MyDb : RoomDatabase() {
        abstract fun myDao(): MyDao
    }

    @Dao
    interface MyDao {
        @Query("SELECT value FROM MyEntity") fun getAsList(): List<Long>

        @Suppress("ROOM_UNNECESSARY_NULLABILITY_IN_DAO_RETURN_TYPE")
        @Query("SELECT value FROM MyNullableEntity")
        fun getAsNullableList(): List<Long?>

        // immutable list does not allow nulls, hence no nullable test for it
        @Query("SELECT value FROM MyEntity") fun getAsImmutableList(): ImmutableList<Long>

        @Query("SELECT value FROM MyEntity LIMIT 1") fun getAsJavaOptional(): Optional<Long>

        @Query("SELECT value FROM MyEntity LIMIT 1")
        fun getAsGuavaOptional(): com.google.common.base.Optional<Long>

        @Query("SELECT value FROM MyEntity LIMIT 1") fun getAsLiveData(): LiveData<Long>

        @Query("SELECT value FROM MyNullableEntity LIMIT 1")
        fun getAsNullableLiveData(): LiveData<Long?>

        @Query("SELECT value FROM MyEntity LIMIT 1") fun getAsFlow(): Flow<Long>

        @Query("SELECT value FROM MyNullableEntity LIMIT 1") fun getAsNullableFlow(): Flow<Long?>

        @Query("SELECT value FROM MyEntity LIMIT 1") fun getAsRx2Observable(): Observable<Long>

        @Query("SELECT max(value) FROM MyEntity")
        fun getAsRx2ObservableUnknownTypeInCursor(): Observable<Long>

        @Query("SELECT value FROM MyEntity LIMIT 1") fun getAsRx2Flowable(): Flowable<Long>

        @Query("SELECT value FROM MyEntity LIMIT 1")
        fun getAsRx3Observable(): io.reactivex.rxjava3.core.Observable<Long>

        @Query("SELECT value FROM MyEntity LIMIT 1")
        fun getAsRx3Flowable(): io.reactivex.rxjava3.core.Flowable<Long>

        @Query("SELECT value FROM MyEntity LIMIT 1")
        fun getAsListenableFuture(): ListenableFuture<Long>

        @Query("SELECT value FROM MyNullableEntity LIMIT 1")
        fun getAsNullableListenableFuture(): ListenableFuture<Long?>

        @Insert fun insert(vararg entities: MyEntity)

        @Insert fun insert(vararg entities: MyNullableEntity)
    }
}
