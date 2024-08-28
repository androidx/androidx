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

package androidx.privacysandbox.ui.integration.sdkproviderutils

// TODO(b/346965982) : Move to SdkApi file
class SdkApiConstants {
    companion object {
        annotation class AdType {
            companion object {
                const val BASIC_NON_WEBVIEW = 0
                const val BASIC_WEBVIEW = 1
                const val WEBVIEW_FROM_LOCAL_ASSETS = 2
                const val NON_WEBVIEW_VIDEO = 3
            }
        }

        annotation class MediationOption {
            companion object {
                const val NON_MEDIATED = 0
                const val SDK_RUNTIME_MEDIATEE = 1
                const val SDK_RUNTIME_MEDIATEE_WITH_OVERLAY = 2
                const val IN_APP_MEDIATEE = 3
            }
        }
    }
}
