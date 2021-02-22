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

package androidx.testutils.mockito

import org.mockito.Answers
import org.mockito.MockSettings
import org.mockito.Mockito
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer

/**
 * [Answer] variant intended for [MockSettings.defaultAnswer] that logs the unmocked method
 * that was called, serializing the arguments used, to try and provide a more informative
 * error message.
 */
val ANSWER_THROWS = Answer {
    when (val name = it.method.name) {
        // Delegate to the actual toString, since that will probably not be mocked by a test
        "toString" -> Answers.CALLS_REAL_METHODS.answer(it)
        else -> {
            val arguments = it.arguments
                ?.takeUnless { it.isEmpty() }
                ?.mapIndexed { index, arg ->
                    try {
                        arg?.toString()
                    } catch (e: Exception) {
                        "toString[$index] threw ${e.message}"
                    }
                }
                ?.joinToString()
                ?: "no arguments"

            throw UnsupportedOperationException(
                "${it.mock::class.java.simpleName}#$name with $arguments should not be called"
            )
        }
    }
}

fun <Type : Any?> whenever(mock: Type, block: InvocationOnMock.() -> Type) =
    Mockito.`when`(mock).thenAnswer { block(it) }!!

/**
 * Spy an existing object and allow mocking within [block]. Once the method returns, the spied
 * instance is prepped to throw exceptions whenever an unmocked method is called. This can be
 * used to enforce that only specifically mocked methods are called, avoiding unexpected
 * results when the behavior under test adds code to call an unexpected method.
 */
inline fun <reified T> spyThrowOnUnmocked(value: T?, block: T.() -> Unit = {}): T {
    val swappingAnswer = object : Answer<Any?> {
        var delegate: Answer<*> = Answers.RETURNS_DEFAULTS

        override fun answer(invocation: InvocationOnMock?): Any? {
            return delegate.answer(invocation)
        }
    }

    val settings = Mockito.withSettings()
        .spiedInstance(value)
        .defaultAnswer(swappingAnswer)

    return Mockito.mock(T::class.java, settings)
        .apply(block)
        .also {
            // To allow Mockito.when() usage inside block, only swap to throwing afterwards
            swappingAnswer.delegate = ANSWER_THROWS
        }
}

/**
 * [Mockito.mock] equivalent of [spyThrowOnUnmocked] which doesn't spy an existing instance.
 */
inline fun <reified T> mockThrowOnUnmocked(block: T.() -> Unit = {}) =
    spyThrowOnUnmocked(null, block)