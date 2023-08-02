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

package androidx.profileinstaller.integration.profileverification.target.no_initializer

import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import androidx.profileinstaller.ProfileVerifier
import java.util.concurrent.Executors

class SampleActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onResume() {
        super.onResume()
        Executors.newSingleThreadExecutor().submit {
            val result = ProfileVerifier.writeProfileVerification(this)
            runOnUiThread {
                findViewById<TextView>(R.id.txtNotice).text = """
                    Profile installed: ${result.profileInstallResultCode}
                    Has reference profile: ${result.isCompiledWithProfile}
                    Has current profile: ${result.hasProfileEnqueuedForCompilation()}
                """.trimIndent()
            }
        }
    }
}
