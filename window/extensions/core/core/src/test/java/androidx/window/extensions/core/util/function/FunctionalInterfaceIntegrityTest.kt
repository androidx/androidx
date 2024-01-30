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

package androidx.window.extensions.core.util.function

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Verifies the integrity of functional interface defined in extensions core. */
class FunctionalInterfaceIntegrityTest {
    /**
     * Verifies that [Consumer] has exactly one method to prevent compatibility issue if we add
     * more methods to [Consumer]
     */
    @Test
    fun testConsumerHasOnlyOneMethod() {
        val testConsumer = Consumer<Int> { }
        val consumerClass = testConsumer.javaClass
        assertTrue(Consumer::class.java.isInstance(testConsumer))
        assertEquals(1, consumerClass.declaredMethods.size)
    }

    /**
     * Verifies that [Predicate] has exactly one method to prevent compatibility issue if we add
     * more methods to [Predicate]
     */
    @Test
    fun testPredicateHasOnlyOneMethod() {
        val testPredicate = Predicate<Int> { true }
        val predicateClass = testPredicate.javaClass
        assertTrue(Predicate::class.java.isInstance(testPredicate))
        assertEquals(1, predicateClass.declaredMethods.size)
    }

    /**
     * Verifies that [Function] has exactly one method to prevent compatibility issue if we add
     * more methods to [Function]
     */
    @Test
    fun testFunctionHasOnlyOneMethod() {
        val testFunction = Function<Int, Float> { num -> num.toFloat() }
        val functionClass = testFunction.javaClass
        assertTrue(Function::class.java.isInstance(testFunction))
        assertEquals(1, functionClass.declaredMethods.size)
    }
}
