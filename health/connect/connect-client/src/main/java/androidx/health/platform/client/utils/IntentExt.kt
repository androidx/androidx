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

@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.health.platform.client.utils

import android.content.Intent
import android.os.Bundle
import androidx.annotation.RestrictTo
import androidx.health.platform.client.proto.AbstractMessageLite

fun Intent.putProtoMessages(name: String, messages: Collection<AbstractMessageLite<*, *>>): Intent =
    putByteArraysExtra(name = name, byteArrays = messages.map { it.toByteArray() })

fun Intent.putByteArraysExtra(name: String, byteArrays: Collection<ByteArray>): Intent =
    putExtra(
        name,
        Bundle(byteArrays.size).apply {
            byteArrays.forEachIndexed { index, bytes ->
                putByteArray(index.toString(), bytes)
            }
        },
    )

fun <T : AbstractMessageLite<*, *>> Intent.getProtoMessages(
    name: String,
    parser: (ByteArray) -> T,
): List<T>? =
    getByteArraysExtra(name = name)?.map(parser)

fun Intent.getByteArraysExtra(name: String): List<ByteArray>? =
    getBundleExtra(name)?.let { bundle ->
        List(bundle.size()) { index ->
            requireNotNull(bundle.getByteArray(index.toString()))
        }
    }
