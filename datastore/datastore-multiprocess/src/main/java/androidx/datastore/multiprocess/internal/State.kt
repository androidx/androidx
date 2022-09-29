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

package androidx.datastore.multiprocess

/**
 * Represents the current state of the DataStore.
 */
internal sealed class State<T>

internal object UnInitialized : State<Any>()

/**
 * A read from disk has succeeded, value represents the current on disk state.
 */
internal class Data<T>(val value: T, val hashCode: Int, val version: Int) : State<T>() {
    fun checkHashCode() {
        check(value.hashCode() == hashCode) {
            "Data in DataStore was mutated but DataStore is only compatible with Immutable types."
        }
    }
}

/**
 * A read from disk has failed. ReadException is the exception that was thrown.
 */
internal class ReadException<T>(val readException: Throwable) : State<T>()

/**
 * The scope has been cancelled. This DataStore cannot process any new reads or writes.
 */
internal class Final<T>(val finalException: Throwable) : State<T>()