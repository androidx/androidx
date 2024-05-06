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

package androidx.core.telecom.test.VoipAppWithExtensions

import android.os.Build
import androidx.annotation.RequiresApi

/**
 * The same implementation of [VoipAppWithExtensionsControl], but just running in the local process
 * which is useful for cases where we need to test backwards compatibility (ConnectionService impl
 * needs to be in the same process)
 */
@RequiresApi(Build.VERSION_CODES.O)
class VoipAppWithExtensionsControlLocal : VoipAppWithExtensionsControl() {
    companion object {
        val CLASS_NAME: String? = VoipAppWithExtensionsControlLocal::class.java.canonicalName
    }

    override fun getClassName(): String? {
        return CLASS_NAME
    }
}
