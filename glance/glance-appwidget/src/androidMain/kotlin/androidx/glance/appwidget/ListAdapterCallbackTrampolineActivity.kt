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

package androidx.glance.appwidget

import android.app.Activity
import android.content.Intent
import android.os.Bundle

/**
 * Trampoline activity for handling click interactions of list adapter items that invoke action
 * callbacks.
 */
@Suppress("ForbiddenSuperClass")
internal class ListAdapterCallbackTrampolineActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intent?.getParcelableExtra<Intent>(ActionIntentKey)
            ?.let { sendBroadcast(it) }
            ?: error("List adapter activity trampoline invoked without specifying target intent.")
        finish()
    }

    companion object {
        private const val ActionIntentKey = "UPDATE_CONTENT_ACTION_INTENT"

        internal fun Intent.putBroadcastIntentExtra(actionIntent: Intent) =
            putExtra(ActionIntentKey, actionIntent)
    }
}
