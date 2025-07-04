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

package androidx.core.telecom.reference

object Constants {
    // Notification/Intent Extra Key
    const val EXTRA_CALL_ID = "EXTRA_CALL_ID"

    // Notification Actions (keep existing definitions or move them here)
    const val ACTION_ANSWER_CALL = "ACTION_ANSWER_CALL"
    const val ACTION_DECLINE_CALL = "ACTION_DECLINE_CALL"
    const val ACTION_HANGUP_CALL = "ACTION_HANGUP_CALL"
    const val ACTION_ANSWER_AND_SHOW_UI =
        "androidx.core.telecom.reference.ACTION_ANSWER_AND_SHOW_UI"

    // Intent Actions
    const val ACTION_NEW_INCOMING_CALL: String = "androidx.core.telecom.reference.NEW_INCOMING_CALL"

    // Intent Extras
    const val EXTRA_SIMULATED_NUMBER: String = "simulated_number"
    const val EXTRA_REMOTE_USER_NAME: String = "name"
    const val EXTRA_IS_VIDEO: String = "isVideo"

    // Deep Link Base URI
    const val DEEP_LINK_BASE_URI = "androidx.core.telecom.reference://"
}
