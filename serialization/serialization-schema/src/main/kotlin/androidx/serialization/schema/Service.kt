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

package androidx.serialization.schema

/** A bindable service, usually derived from a Java or Kotlin interface. */
interface Service : ComplexType {
    /** The descriptor, usually a class name, used for [android.os.IBinder.interfaceDescriptor]. */
    val descriptor: String

    /** The actions of the service */
    val actions: Collection<Action>

    /** Alias for [actions]. */
    override val members: Collection<Action>
        get() = actions

    /** An interface action, usually derived from a [androidx.serialization.Action] annotation. */
    interface Action : ComplexType.Member {
        /** The mode used for this action. */
        val mode: Mode

        /** Optional request message type. */
        val request: Message?

        /** Optional response message type. */
        val response: Message?

        /** The mode used for the binder transaction */
        enum class Mode {
            /** Default blocking mode. */
            BLOCKING,

            /** One-way async mode using [android.os.IBinder.FLAG_ONEWAY]. */
            ONE_WAY
        }
    }
}
