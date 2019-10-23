/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.serialization

import kotlin.annotation.AnnotationRetention.BINARY
import kotlin.annotation.AnnotationTarget.FUNCTION

/**
 * Marks a function as an action in a service interface.
 *
 * Applying this annotation to an interface function marks the interface as a service and the
 * function as an action. A service interface may be used as a [Field] in a message class with a
 * parcel representation and by itself with Android's [android.os.IBinder] inter-process
 * communication API.
 *
 * ```kotlin
 * interface MyService {
 *     @Action(1)
 *     fun myAction(request: MyRequestMessage): MyResponseMessage
 *
 *     @Action(2, oneWay = true)
 *     fun myOneWayAction(request: MyOneWayRequestMessage)
 * }
 * ```
 *
 * To ensure that services have an extensible schema, actions may only produce or consume message
 * classes from the same compilation unit. Each action function must either take zero arguments
 * or one request message class argument and return [Unit] or a response message class.
 *
 * Service interfaces may contain a mix of abstract functions and functions or properties with
 * default implementations, but all abstract members must be action functions with this
 * annotation applied. Service interfaces may extend interfaces with no abstract functions or
 * properties or other service interfaces in the same compilation unit.
 *
 * @property id Integer ID of this action.
 *
 * Valid action IDs are between 1 and 16,777,215 inclusive
 * ([android.os.IBinder.FIRST_CALL_TRANSACTION] through
 * [android.os.IBinder.LAST_CALL_TRANSACTION]).
 *
 * Action IDs must be unique within a service interface, including any actions inherited from
 * another service interface, but may be reused between unrelated service interfaces.
 *
 * To reserve action IDs for future use or to prevent unintentional reuse of removed field IDs,
 * apply the [Reserved] annotation to the service interface.
 *
 * @property oneWay True if this action is one-way.
 *
 * One-way actions do not wait for the action to complete on the remote process and cannot throw
 * exceptions or return a result. Functions with a one-way action annotation are required to return
 * [Unit]. To receive a result from a one-way action asynchronously, consider creating a callback
 * service and passing an instance of it in the request message.
 *
 * For more details on one-way actions, see [android.os.IBinder.FLAG_ONEWAY].
 */
@Retention(BINARY)
@Target(FUNCTION)
annotation class Action(
    @get:JvmName("value")
    val id: Int,
    val oneWay: Boolean = false
)
