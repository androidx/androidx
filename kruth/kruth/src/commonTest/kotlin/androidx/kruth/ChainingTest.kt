/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.kruth

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.asserter

class ChainingTest {

    @Test
    fun singleChain() {
        assertFailsWith<AssertionError> {
            ObjectSubject.assertThat("object1").isNotAnObject()
        }
    }

    @Test
    fun doubleChain() {
        assertFailsWith<AssertionError> {
            ObjectSubject.assertThat("object1")
                .chain("object2")
                .isNotAnObject()
        }
    }

    @Test
    fun tripleChain() {
        assertFailsWith<AssertionError> {
            ObjectSubject.assertThat("object1")
                .chain("object2")
                .chain("object3")
                .isNotAnObject()
        }
    }

    private class ObjectSubject(
        metadata: FailureMetadata = FailureMetadata(),
        actual: Any?,
    ) : Subject<Any>(actual = actual, metadata = metadata) {
        companion object {
            val FACTORY: Factory<ObjectSubject, Any> = Factory<ObjectSubject, Any> {
                    metadata, actual -> ObjectSubject(metadata, actual)
            }

            // entry point
            fun assertThat(obj: Any): ObjectSubject {
                return assertAbout(::ObjectSubject).that(obj)
            }
        }

        fun isNotAnObject() {
            asserter.fail("Expected be an Object.")
        }

        fun chain(actual: Any): ObjectSubject {
            return check().about(ObjectSubject.FACTORY).that(actual)
        }
    }
}
