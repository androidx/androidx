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

import androidx.privacysandbox.tools.testing.CompilationTestHelper.assertThat
import androidx.privacysandbox.tools.testing.loadSourcesFromDirectory
import java.io.File
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/** Test the Privacy Sandbox API Compiler with an SDK that uses all available features. */
@RunWith(JUnit4::class)
class FullFeaturedSdkTest {

    @Test
    fun compileServiceInterface_ok() {
        val inputTestDataDir = File("src/test/test-data/fullfeaturedsdk/input")
        val outputTestDataDir = File("src/test/test-data/fullfeaturedsdk/output")
        val inputSources = loadSourcesFromDirectory(inputTestDataDir)
        val expectedKotlinSources = loadSourcesFromDirectory(outputTestDataDir)

        val result = compileWithPrivacySandboxKspCompiler(
            inputSources,
            platformStubs = PlatformStubs.API_33,
            extraProcessorOptions = mapOf("skip_sdk_runtime_compat_library" to "true")
        )
        assertThat(result).succeeds()

        val expectedAidlFilepath = listOf(
            "com/mysdk/ICancellationSignal.java",
            "com/mysdk/IMyCallback.java",
            "com/mysdk/IMyInterface.java",
            "com/mysdk/IMyInterfaceTransactionCallback.java",
            "com/mysdk/IMySdk.java",
            "com/mysdk/IMySecondInterface.java",
            "com/mysdk/IMySecondInterfaceTransactionCallback.java",
            "com/mysdk/IResponseTransactionCallback.java",
            "com/mysdk/IStringTransactionCallback.java",
            "com/mysdk/IUnitTransactionCallback.java",
            "com/mysdk/IListResponseTransactionCallback.java",
            "com/mysdk/IListIntTransactionCallback.java",
            "com/mysdk/IListLongTransactionCallback.java",
            "com/mysdk/IListDoubleTransactionCallback.java",
            "com/mysdk/IListStringTransactionCallback.java",
            "com/mysdk/IListBooleanTransactionCallback.java",
            "com/mysdk/IListFloatTransactionCallback.java",
            "com/mysdk/IListCharTransactionCallback.java",
            "com/mysdk/IListShortTransactionCallback.java",
            "com/mysdk/ParcelableRequest.java",
            "com/mysdk/ParcelableResponse.java",
            "com/mysdk/ParcelableStackFrame.java",
            "com/mysdk/ParcelableInnerValue.java",
            "com/mysdk/PrivacySandboxThrowableParcel.java",
        )
        assertThat(result).hasAllExpectedGeneratedSourceFilesAndContent(
            expectedKotlinSources,
            expectedAidlFilepath
        )
    }
}