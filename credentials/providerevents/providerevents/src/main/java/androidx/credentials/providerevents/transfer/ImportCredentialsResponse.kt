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

package androidx.credentials.providerevents.transfer

import android.os.Bundle

/**
 * A success response from requesting import.
 *
 * @property responseJson the credential response json according to the
 *   [Fido Credential Exchange Format](https://fidoalliance.org/specs/cx/cxf-v1.0-rd-20250313.html)
 */
public class ImportCredentialsResponse(public val responseJson: String) {
    public companion object {
        /**
         * Wraps the response class into a bundle. The responseJson itself isn't written to bundle
         * because it can potentially exceed the binder size limit. However, any other current or
         * future parameters of the response will be included as part of the bundle. To share the
         * credentials,
         * [File Provider](https://developer.android.com/reference/androidx/core/content/FileProvider)
         * can be used to share the credentials to another app.
         */
        @JvmStatic public fun toBundle(response: ImportCredentialsResponse): Bundle = Bundle()
    }
}
