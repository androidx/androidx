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

package androidx.navigation.common.lint

import androidx.navigation.lint.common.CompatKotlinAndBytecodeStub
import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestFiles
import java.util.Locale

/**
 * Workaround for b/371463741
 *
 * Once the new lint common structure is merged, we should remove this file and return to copying
 * KotlinAndBytecodeStub helper from compose.lint.test.stubs into the new
 * navigation:navigation-lint:common-test (does not exist currently) and use the
 * KotlinAndBytecodeStub from common-test instead.
 */
fun CompatKotlinAndBytecodeStub.toTestKotlinAndBytecodeStub(): KotlinAndBytecodeStub {
    val filenameWithoutExtension = filename.substringBefore(".").lowercase(Locale.ROOT)
    val kotlin = kotlin(source).to("$filepath/$filename")
    val bytecodeStub =
        TestFiles.bytecode("libs/$filenameWithoutExtension.jar", kotlin, checksum, *bytecode)
    return KotlinAndBytecodeStub(kotlin, bytecodeStub)
}

fun CompatKotlinAndBytecodeStub.toTestBytecodeStub(): TestFile =
    toTestKotlinAndBytecodeStub().bytecode

class KotlinAndBytecodeStub(val kotlin: TestFile, val bytecode: TestFile)

val NAV_DEEP_LINK = androidx.navigation.lint.common.NAV_DEEP_LINK.toTestBytecodeStub()
