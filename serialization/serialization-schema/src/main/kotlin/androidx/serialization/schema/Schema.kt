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

package androidx.serialization.schema

/**
 * A schema consisting of messages, enums, and services.
 *
 * Clients who have subclassed [Message], [Enum], or [Service] may wish to subclass this class to
 * provide type safety around their subclassed types.
 *
 * @property messages Messages declared in the schema.
 * @property enums Enums declared in the schema.
 * @property services Services declared in the schema.
 */
open class Schema(
    open val messages: List<Message> = emptyList(),
    open val enums: List<Enum> = emptyList(),
    open val services: List<Service> = emptyList()
)