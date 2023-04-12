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

package androidx.privacysandbox.tools.apicompiler

import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class WithoutRuntimeLibrarySdkApiCompilerDiffTest : AbstractApiCompilerDiffTest() {
    override val subdirectoryName = "withoutruntimelibrarysdk"
    override val extraProcessorOptions = mapOf("skip_sdk_runtime_compat_library" to "true")
    override val relativePathsToExpectedAidlClasses = listOf(
        "com/mysdk/ICancellationSignal.java",
        "com/mysdk/IWithoutRuntimeLibrarySdk.java",
        "com/mysdk/IStringTransactionCallback.java",
        "com/mysdk/ParcelableStackFrame.java",
        "com/mysdk/PrivacySandboxThrowableParcel.java",
    )
}