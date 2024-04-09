/*
 * Copyright 2024 The Android Open Source Project
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
 * Propositions for [Class] subjects.
 */
class ClassSubject internal constructor(
    actual: Class<*>?,
    metadata: FailureMetadata = FailureMetadata(),
) : Subject<Class<*>>(actual, metadata = metadata, typeDescriptionOverride = null) {

    /**
     * Fails if this class or interface is not the same as or a subclass or subinterface of, the
     * given class or interface.
     */
    fun isAssignableTo(clazz: Class<*>) {
        if (!clazz.isAssignableFrom(requireNonNull(actual))) {
            failWithActual("Expected to be assignable to", clazz.getName())
        }
    }
}
