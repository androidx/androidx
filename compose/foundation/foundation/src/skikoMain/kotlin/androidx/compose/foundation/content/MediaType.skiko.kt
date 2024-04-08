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

package androidx.compose.foundation.content

import androidx.compose.foundation.ExperimentalFoundationApi

// TODO https://youtrack.jetbrains.com/issue/COMPOSE-1263/Implement-Modifier.receiveContent

@ExperimentalFoundationApi
actual class MediaType internal constructor() {

    actual constructor(representation: String) : this()

    actual val representation: String = ""

    actual companion object {
        actual val Text: MediaType = MediaType()

        actual val PlainText: MediaType = MediaType()

        actual val HtmlText: MediaType = MediaType()

        actual val Image: MediaType = MediaType()

        actual val All: MediaType = MediaType()
    }

    override fun toString(): String {
        return "MediaType(" +
            "representation='$representation')"
    }
}
