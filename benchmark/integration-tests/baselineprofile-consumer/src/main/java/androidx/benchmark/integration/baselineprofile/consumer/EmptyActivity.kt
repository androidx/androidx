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

package androidx.benchmark.integration.baselineprofile.consumer

import android.os.Bundle
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.profileinstaller.ProfileVerifier
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.ExecutorService
import javax.inject.Inject

@AndroidEntryPoint
class EmptyActivity : ComponentActivity() {

    @Inject
    lateinit var executor: ExecutorService

    @Inject
    lateinit var compilationStatusFuture: ListenableFuture<ProfileVerifier.CompilationStatus>

    @Inject
    lateinit var someObject: SomeObject

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onResume() {
        super.onResume()
        executor.submit {
            val result = compilationStatusFuture.get()
            runOnUiThread {
                findViewById<TextView>(R.id.txtNotice).text = """
                    Profile installed: ${result.profileInstallResultCode}
                    Has reference profile: ${result.isCompiledWithProfile}
                    Has current profile: ${result.hasProfileEnqueuedForCompilation()}
                    Build type: ${someObject.buildType}
                """.trimIndent()
            }
        }
    }
}
