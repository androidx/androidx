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

package androidx.compose.foundation.text.input.internal

import android.content.Context
import android.graphics.Typeface
import androidx.compose.ui.text.font.AndroidFont
import androidx.compose.ui.text.font.FontLoadingStrategy.Companion.Async
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.google.common.truth.Truth
import kotlinx.coroutines.CompletableDeferred

// Copied the necessary parts from ui-text package to test Async font loading.
@Suppress("MemberVisibilityCanBePrivate") // visible for testing
class AsyncTestTypefaceLoader : AndroidFont.TypefaceLoader {
    private val callbackLock = Object()
    @Volatile
    private var asyncLoadCallback: ((AndroidFont) -> Unit)? = null
    private val requests =
        mutableMapOf<AsyncFauxFont, MutableList<CompletableDeferred<Typeface?>>>()
    internal val completedAsyncRequests = mutableListOf<AsyncFauxFont>()

    override fun loadBlocking(context: Context, font: AndroidFont): Typeface? {
        error("unsupported operation")
    }

    override suspend fun awaitLoad(context: Context, font: AndroidFont): Typeface? {
        val result = when (font) {
            is AsyncFauxFont -> {
                val deferred = CompletableDeferred<Typeface?>()
                val list = requests.getOrPut(font) { mutableListOf() }
                list.add(deferred)
                synchronized(callbackLock) {
                    asyncLoadCallback?.invoke(font)
                }
                deferred.await()
            }
            else -> null
        }
        return result
    }

    fun completeOne(font: AsyncFauxFont, typeface: Typeface?) {
        Truth.assertThat(requests).containsKey(font)
        val requestList = requests[font]!!
        requestList.removeAt(0).complete(typeface)
        completedAsyncRequests.add(font)
    }
}

class AsyncFauxFont(
    typefaceLoader: AsyncTestTypefaceLoader,
    override val weight: FontWeight = FontWeight.Normal,
    override val style: FontStyle = FontStyle.Normal,
    private val name: String = "AsyncFauxFont"
) : AndroidFont(Async, typefaceLoader, FontVariation.Settings(weight, style)) {
    override fun toString(): String {
        return "$name[$weight, $style]"
    }
}
