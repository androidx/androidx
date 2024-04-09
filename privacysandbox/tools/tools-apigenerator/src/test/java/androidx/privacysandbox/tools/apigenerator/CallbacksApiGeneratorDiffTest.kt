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

package androidx.privacysandbox.tools.apigenerator

import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class CallbacksApiGeneratorDiffTest : AbstractApiGeneratorDiffTest() {
    override val subdirectoryName = "callbacks"
    override val relativePathsToExpectedAidlClasses = listOf(
        "com/sdkwithcallbacks/IMyInterface.java",
        "com/sdkwithcallbacks/ISdkCallback.java",
        "com/sdkwithcallbacks/ISdkService.java",
        "com/sdkwithcallbacks/ParcelableResponse.java",
        "com/sdkwithcallbacks/ParcelableMyEnum.java",
        "com/sdkwithcallbacks/IMyUiInterface.java",
        "com/sdkwithcallbacks/IMyUiInterfaceCoreLibInfoAndBinderWrapper.java",
        "com/sdkwithcallbacks/PrivacySandboxThrowableParcel.java",
        "com/sdkwithcallbacks/IResponseTransactionCallback.java",
        "com/sdkwithcallbacks/ICancellationSignal.java",
        "com/sdkwithcallbacks/ParcelableStackFrame.java",
    )
}
