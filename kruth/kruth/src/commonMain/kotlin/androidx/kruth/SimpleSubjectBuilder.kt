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

/**
 * In a fluent assertion chain, exposes the most common [SimpleSubjectBuilder.that] method,
 * which accepts a value under test and returns a [Subject].
 *
 * For more information about the methods in this class, see
 * [this FAQ entry](https://truth.dev/faq#full-chain).
 *
 * **For people extending Kruth**
 *
 * You won't extend this type. When you write a custom subject, see
 * [our doc on extensions](https://truth.dev/extension). It explains where [Subject.Factory] fits
 * into the process.
 */
class SimpleSubjectBuilder<out S : Subject<T>, T> internal constructor(
    private val metadata: FailureMetadata = FailureMetadata(),
    private val subjectFactory: Subject.Factory<S, T>,
) {

    fun that(actual: T): S =
        subjectFactory.createSubject(metadata, actual)
}
