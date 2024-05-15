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

package androidx.compose.foundation.content

import androidx.compose.foundation.ExperimentalFoundationApi
import java.awt.datatransfer.DataFlavor

@ExperimentalFoundationApi
actual class MediaType internal constructor(val dataFlavor: DataFlavor) {

    actual constructor(representation: String) : this(DataFlavor(representation))

    actual val representation: String = dataFlavor.mimeType

    actual companion object {
        actual val Text: MediaType = MediaType(DataFlavor.stringFlavor)

        actual val PlainText: MediaType = MediaType(DataFlavor.getTextPlainUnicodeFlavor())

        actual val HtmlText: MediaType = MediaType(DataFlavor.allHtmlFlavor)

        actual val Image: MediaType = MediaType(DataFlavor.imageFlavor)

        actual val All: MediaType = MediaType(DataFlavor("*/*"))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MediaType) return false

        return dataFlavor == other.dataFlavor
    }

    override fun hashCode(): Int {
        return dataFlavor.hashCode()
    }

    override fun toString(): String {
        return "MediaType(" +
            "dataFlavor=$dataFlavor, " +
            "representation='$representation')"
    }
}
