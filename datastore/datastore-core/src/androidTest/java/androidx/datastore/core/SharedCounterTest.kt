/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.datastore.core

import androidx.test.filters.MediumTest
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.io.IOException
import kotlin.collections.MutableSet
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@ExperimentalCoroutinesApi
@MediumTest
@RunWith(JUnit4::class)
class SharedCounterTest {

    companion object {
        init {
            SharedCounter.loadLib()
        }
    }

    @get:Rule
    val tempFolder = TemporaryFolder()
    private lateinit var testFile: File

    @Before
    fun setup() {
        testFile = tempFolder.newFile()
    }

    @Test
    fun testCreate_success_enableMlock() {
        val counter: SharedCounter = SharedCounter.create { testFile }
        assertThat(counter).isNotNull()
    }

    @Test
    fun testCreate_success_disableMlock() {
        val counter: SharedCounter = SharedCounter.create(false) { testFile }
        assertThat(counter).isNotNull()
    }

    @Test
    fun testCreate_failure() {
        val tempFile = tempFolder.newFile()
        tempFile.setReadable(false)
        assertThrows(IOException::class.java) {
            SharedCounter.create {
                tempFile
            }
        }
    }

    @Test
    fun testGetValue() {
        val counter: SharedCounter = SharedCounter.create(false) { testFile }
        assertThat(counter.getValue()).isEqualTo(0)
    }

    @Test
    fun testIncrementAndGet() {
        val counter: SharedCounter = SharedCounter.create(false) { testFile }
        for (count in 1..100) {
            assertThat(counter.incrementAndGetValue()).isEqualTo(count)
        }
    }

    @Test
    fun testIncrementInParallel() = runTest {
        val counter: SharedCounter = SharedCounter.create(false) { testFile }
        val valueToAdd = 100
        val numCoroutines = 10
        val numbers: MutableSet<Int> = mutableSetOf()
        val deferred = async {
            repeat(numCoroutines) {
                launch {
                    repeat(valueToAdd) {
                        assertThat(numbers.add(counter.incrementAndGetValue())).isTrue()
                    }
                }
            }
        }
        deferred.await()
        assertThat(counter.getValue()).isEqualTo(numCoroutines * valueToAdd)
        for (num in 1..(numCoroutines * valueToAdd)) {
            assertThat(numbers).contains(num)
        }
    }

    @Test
    fun testManyInstancesWithMlockDisabled() = runTest {
        // More than 16
        val numCoroutines = 5000
        val counters = mutableListOf<SharedCounter>()
        val deferred = async {
            repeat(numCoroutines) {
                val tempFile = tempFolder.newFile()
                val counter = SharedCounter.create(false) { tempFile }
                assertThat(counter.getValue()).isEqualTo(0)
                counters.add(counter)
            }
        }
        deferred.await()
    }
}