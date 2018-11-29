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

package androidx.room

import androidx.lifecycle.LiveData
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito
import java.util.concurrent.Callable

@RunWith(JUnit4::class)
class InvalidationLiveDataContainerTest {
    private lateinit var container: InvalidationLiveDataContainer

    @Before
    fun init() {
        container = InvalidationLiveDataContainer(
            Mockito.mock(RoomDatabase::class.java)
        )
    }

    @Test
    fun add() {
        val liveData = createLiveData()
        assertThat(container.mLiveDataSet, `is`(emptySet()))
        container.onActive(liveData)
        assertThat(container.mLiveDataSet, `is`(setOf(liveData)))
    }

    @Test
    fun add_twice() {
        val liveData = createLiveData()
        container.onActive(liveData)
        container.onActive(liveData)
        assertThat(container.mLiveDataSet, `is`(setOf(liveData)))
    }

    @Test
    fun remove() {
        val liveData = createLiveData()
        container.onActive(liveData)
        container.onInactive(liveData)
        assertThat(container.mLiveDataSet, `is`(emptySet()))
    }

    @Test
    fun remove_twice() {
        val liveData = createLiveData()
        container.onActive(liveData)
        container.onInactive(liveData)
        container.onInactive(liveData)
        assertThat(container.mLiveDataSet, `is`(emptySet()))
    }

    @Test
    fun addRemoveMultiple() {
        val ld1 = createLiveData()
        val ld2 = createLiveData()
        assertThat(container.mLiveDataSet, `is`(emptySet()))
        container.onActive(ld1)
        container.onActive(ld2)
        assertThat(container.mLiveDataSet, `is`(setOf(ld1, ld2)))
        container.onInactive(ld1)
        assertThat(container.mLiveDataSet, `is`(setOf(ld2)))
        container.onInactive(ld1) // intentional
        assertThat(container.mLiveDataSet, `is`(setOf(ld2)))
        container.onActive(ld1)
        assertThat(container.mLiveDataSet, `is`(setOf(ld1, ld2)))
        container.onActive(ld1) // intentional
        assertThat(container.mLiveDataSet, `is`(setOf(ld1, ld2)))
        container.onInactive(ld2)
        assertThat(container.mLiveDataSet, `is`(setOf(ld1)))
        container.onInactive(ld1)
        assertThat(container.mLiveDataSet, `is`(emptySet()))
        container.onActive(ld1)
        assertThat(container.mLiveDataSet, `is`(setOf(ld1)))
        container.onActive(ld2)
        assertThat(container.mLiveDataSet, `is`(setOf(ld1, ld2)))
    }

    private fun createLiveData(): LiveData<Any> {
        return container.create(
            arrayOf("a", "b"),
            createComputeFunction<Any>()
        ) as LiveData
    }

    private fun <T> createComputeFunction(): Callable<T> {
        return Callable<T> { null }
    }
}