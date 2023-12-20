/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.credentials.webauthn

import android.os.Build
import android.util.Base64
import androidx.annotation.RestrictTo
import androidx.credentials.provider.CallingAppInfo

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class WebAuthnUtils {
  companion object {
    fun b64Decode(str: String): ByteArray {
      return Base64.decode(str, Base64.NO_PADDING or Base64.NO_WRAP or Base64.URL_SAFE)
    }

    fun b64Encode(data: ByteArray): String {
      return Base64.encodeToString(data, Base64.NO_PADDING or Base64.NO_WRAP or Base64.URL_SAFE)
    }

    fun appInfoToOrigin(info: CallingAppInfo): String {
      if (Build.VERSION.SDK_INT >= 28) {
        return WebAuthnUtilsApi28.appInfoToOrigin(info)
      }
      return ""
    }
  }
}
