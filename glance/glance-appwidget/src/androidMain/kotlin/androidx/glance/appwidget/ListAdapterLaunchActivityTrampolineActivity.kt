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
 * Trampoline activity for handling click interactions of list adapter items that start activities.
 * This trampoline is only used for device versions before [android.os.Build.VERSION_CODES.Q].
 */
@Suppress("ForbiddenSuperClass")
internal class ListAdapterLaunchActivityTrampolineActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intent?.getParcelableExtra<Intent>(ActionIntentKey)
            ?.let {
                // Copy flags from trampoline intent to action intent
                it.flags = it.flags or intent.flags
                startActivity(it)
            }
            ?: error("List adapter activity trampoline invoked without specifying target intent.")
        finish()
    }

    companion object {
        private const val ActionIntentKey = "ACTIVITY_ACTION_INTENT"

        internal fun Intent.putActivityIntentExtra(actionIntent: Intent) =
            putExtra(ActionIntentKey, actionIntent)
    }
}
