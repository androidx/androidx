/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.foundation.text.test

import android.content.Context
import androidx.emoji2.bundled.BundledEmojiCompatConfig
import androidx.emoji2.text.EmojiCompat
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executor

internal fun withEmojiCompat(context: Context, enabled: Boolean = true, block: () -> Unit) {
    if (!enabled) {
        block()
        return
    }

    try {
        val synchronousExecutor = Executor { runnable -> runnable.run() }
        EmojiCompat.init(BundledEmojiCompatConfig(context, synchronousExecutor))
        assertThat(EmojiCompat.get().loadState).isEqualTo(EmojiCompat.LOAD_STATE_SUCCEEDED)
        block()
    } finally {
        EmojiCompat.reset(null)
    }
}
