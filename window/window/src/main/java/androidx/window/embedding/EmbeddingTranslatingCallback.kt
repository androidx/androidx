/*
 * Copyright 2021 The Android Open Source Project
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
package androidx.window.embedding

import android.annotation.SuppressLint
import androidx.window.core.ExperimentalWindowApi
import androidx.window.embedding.EmbeddingInterfaceCompat.EmbeddingCallbackInterface
import java.util.function.Consumer

@SuppressLint("NewApi") // The callback is only used with extensions on SDK levels 30+.
@ExperimentalWindowApi
internal class EmbeddingTranslatingCallback(
    private val callback: EmbeddingCallbackInterface,
    private val adapter: EmbeddingAdapter
) : Consumer<List<androidx.window.extensions.embedding.SplitInfo>> {
    override fun accept(splitInfoList: List<androidx.window.extensions.embedding.SplitInfo>) {
        callback.onSplitInfoChanged(adapter.translate(splitInfoList))
    }
}
