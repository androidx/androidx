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

package androidx.emoji2.emojipicker.samples

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ToggleButton
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.emoji2.emojipicker.EmojiPickerView
import androidx.emoji2.emojipicker.EmojiViewItem

class ComposeActivity : ComponentActivity() {
    private lateinit var context: Context
    private lateinit var mainLayout: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        context = this
        setContent { EmojiPicker() }
    }

    @Composable
    private fun EmojiPicker() {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                val view =
                    LayoutInflater.from(context)
                        .inflate(R.layout.main, /* root= */ null, /* attachToRoot= */ false)
                val emojiPickerView = view.findViewById<EmojiPickerView>(R.id.emoji_picker)
                emojiPickerView.setOnEmojiPickedListener(this::updateEditText)
                view.findViewById<ToggleButton>(R.id.toggle_button).setOnCheckedChangeListener {
                    _,
                    isChecked ->
                    if (isChecked) {
                        emojiPickerView.emojiGridColumns = 8
                        emojiPickerView.emojiGridRows = 8.3f
                    } else {
                        emojiPickerView.emojiGridColumns = 9
                        emojiPickerView.emojiGridRows = 15f
                    }
                }
                view.findViewById<Button>(R.id.button).visibility = View.GONE
                val activityButton = view.findViewById<ToggleButton>(R.id.activity_button)
                activityButton.isChecked = true
                activityButton.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        val intent = Intent(this, ComposeActivity::class.java)
                        startActivity(intent)
                    } else {
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                    }
                }
                view
            },
            update = { view -> mainLayout = view }
        )
    }

    private fun updateEditText(emojiViewItem: EmojiViewItem) {
        val editText = mainLayout.findViewById<EditText>(R.id.edit_text)
        editText.append(emojiViewItem.emoji)
    }
}
