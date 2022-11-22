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

package androidx.emoji2.emojipicker.samples

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.emoji2.emojipicker.EmojiPickerView
import androidx.emoji2.emojipicker.EmojiViewItem

class MainActivity : AppCompatActivity() {
    companion object {
        internal const val LOG_TAG = "SAMPLE_APP"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)

        val view = findViewById<EmojiPickerView>(R.id.emoji_picker)
        view.setOnEmojiPickedListener { emoji: EmojiViewItem ->
            Log.d(LOG_TAG, String.format("emoji: %s", emoji.emoji))
            Log.d(LOG_TAG, String.format("variants: %s", emoji.variants.toString()))
        }
    }
}