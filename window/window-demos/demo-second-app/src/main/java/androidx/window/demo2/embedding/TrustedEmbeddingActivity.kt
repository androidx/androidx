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

package androidx.window.demo2.embedding

import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import androidx.window.demo2.R

/**
 * Activity that can be embedded by a process with a known certificate. See
 * `android:allowUntrustedActivityEmbedding` in AndroidManifest. Activity can be launched from the
 * split demos in window-samples/demos.
 */
class TrustedEmbeddingActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_embedded)
        findViewById<TextView>(R.id.detail_text_view).text =
            getString(R.string.trusted_embedding_activity_detail)
    }
}
