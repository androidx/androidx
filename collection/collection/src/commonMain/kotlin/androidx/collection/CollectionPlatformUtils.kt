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

package androidx.collection

/**
 * Internal utils for handling implementation differences for the various targets of Collections.
 */
internal expect object CollectionPlatformUtils {

    /**
     * IndexOutOfBoundsException is the nearest kotlin common ancestor for the native and jvm
     * specific implementations of ArrayIndexOutOfBoundsException.  Actuals should throw an
     * exception specific to their target platform.
     */
    internal inline fun createIndexOutOfBoundsException(): IndexOutOfBoundsException
}
