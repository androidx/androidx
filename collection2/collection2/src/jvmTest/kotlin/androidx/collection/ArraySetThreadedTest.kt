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

package androidx.collection

import org.junit.Test
import java.util.ConcurrentModificationException
import kotlin.test.AfterTest
import kotlin.test.fail

class ArraySetThreadedTest {
    var set: ArraySet<String>? = ArraySet()

    @AfterTest
    fun cleanUp() {
        set = null
    }

    @Test
    fun testConcurrentModificationException() {
        val testDurMs = 10000
        println("Starting ArraySet concurrency test")
        Thread {
            var i = 0
            val s = set
            while (s != null) {
                try {
                    s.add(String.format("key %d", i++))
                } catch (e: ArrayIndexOutOfBoundsException) {
                    fail(cause = e)
                } catch (e: ClassCastException) {
                    fail(cause = e)
                } catch (e: ConcurrentModificationException) {
                    println(
                        "[successfully caught CME at put #$i size=${s.size}]"
                    )
                }
            }
        }.start()
        for (i in 0 until testDurMs / 100) {
            try {
                if (set != null && set!!.size % 4 == 0) {
                    set?.clear()
                }
            } catch (e: ArrayIndexOutOfBoundsException) {
                fail(e.message)
            } catch (e: ClassCastException) {
                fail(e.message)
            } catch (e: ConcurrentModificationException) {
                println(
                    "[successfully caught CME at clear #$i size=${set?.size}]"
                )
            }
        }
    }
}