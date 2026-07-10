/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.binarycompatibilityvalidator

import com.google.common.truth.Truth.assertThat
import kotlin.test.Test
import org.jetbrains.kotlin.library.abi.ExperimentalLibraryAbiReader

@OptIn(ExperimentalLibraryAbiReader::class)
class MergedKlibDumpParserTest {

    @Test
    fun mergedKlibDumpParser_splitsMergedDumpsIntoIndividualTargets() {
        val parsed = MergedKlibDumpParser(mergedDumpText).parse()
        assertThat(parsed.keys).containsExactly("jvm", "iosArm64", "linuxX64")
    }

    @Test
    fun mergedKlibDumpParser_addsDeclarationsToCorrectTargets() {
        val parsed = MergedKlibDumpParser(mergedDumpText).parse()
        val jvm = parsed["jvm"]!!
        val ios = parsed["iosArm64"]!!
        val linux = parsed["linuxX64"]!!

        assertThat(jvm.topLevelDeclarations.declarations).hasSize(1)
        assertThat(ios.topLevelDeclarations.declarations).hasSize(2)
        assertThat(linux.topLevelDeclarations.declarations).hasSize(2)
        assertThat(jvm.topLevelDeclarations.declarations.single().qualifiedName.toString())
            .isEqualTo("example/commonFun")
        assertThat(ios.topLevelDeclarations.declarations.map() { it.qualifiedName.toString() })
            .containsExactly("example/commonFun", "example/IosOnly")
        assertThat(linux.topLevelDeclarations.declarations.map() { it.qualifiedName.toString() })
            .containsExactly("example/commonFun", "example/LinuxOnly")
    }

    @Test
    fun mergedKlibDumpParser_addsDeclarationsToCorrectTargetsWhenUsingAlias() {
        val parsed = MergedKlibDumpParser(mergedDumpWithAliasText).parse()
        val jvm = parsed["jvm"]!!
        val ios = parsed["iosArm64"]!!
        val mac = parsed["macosArm64"]!!
        val linux = parsed["linuxX64"]!!

        assertThat(jvm.topLevelDeclarations.declarations).hasSize(1)
        assertThat(linux.topLevelDeclarations.declarations).hasSize(1)
        assertThat(ios.topLevelDeclarations.declarations).hasSize(2)
        assertThat(mac.topLevelDeclarations.declarations).hasSize(2)
        assertThat(jvm.topLevelDeclarations.declarations.single().qualifiedName.toString())
            .isEqualTo("example/commonFun")
        assertThat(linux.topLevelDeclarations.declarations.single().qualifiedName.toString())
            .isEqualTo("example/commonFun")
        assertThat(ios.topLevelDeclarations.declarations.map() { it.qualifiedName.toString() })
            .containsExactly("example/commonFun", "example/AppleOnly")
        assertThat(mac.topLevelDeclarations.declarations.map() { it.qualifiedName.toString() })
            .containsExactly("example/commonFun", "example/AppleOnly")
    }

    @Test
    fun mergedKlibDumpParser_usesTargetNameNotCustomNameOrCombination() {
        val dumpTextWithCustomTargetName =
            """
            // Klib ABI Dump
            // Targets: [linuxX64.linuxx64Stubs]
            // Rendering settings:
            // - Signature version: 2
            // - Show manifest properties: true
            // - Show declarations: true

            // Library unique name: <io.github.kotlin:library>
            final fun example/commonFun(): kotlin/Int // example/commonFun|commonFun(){}[0]

            """
                .trimIndent()
        val parsed = MergedKlibDumpParser(dumpTextWithCustomTargetName).parse()
        assertThat(parsed.keys.single()).isEqualTo("linuxX64")
    }
}

private val mergedDumpText =
    """
    // Klib ABI Dump
    // Targets: [jvm, iosArm64, linuxX64]
    // Rendering settings:
    // - Signature version: 2
    // - Show manifest properties: true
    // - Show declarations: true

    // Library unique name: <io.github.kotlin:library>
    final fun example/commonFun(): kotlin/Int // example/commonFun|commonFun(){}[0]

    // Targets: [iosArm64]
    final class example/IosOnly { // example/IosOnly|null[0]
        constructor <init>() // example/IosOnly.<init>|<init>(){}[0]

        final fun iosFun(): kotlin/String // example/IosOnly.iosFun|iosFun(){}[0]
    }

    // Targets: [linuxX64]
    final class example/LinuxOnly { // example/LinuxOnly|null[0]
        constructor <init>() // example/LinuxOnly.<init>|<init>(){}[0]

        final fun linuxFun(): kotlin/Int // example/LinuxOnly.kotlinFun|linuxFun(){}[0]
    }
    """
        .trimIndent()

private val mergedDumpWithAliasText =
    """
    // Klib ABI Dump
    // Targets: [jvm, iosArm64, macosArm64, linuxX64]
    // Alias: apple => [iosArm64, macosArm64]
    // Rendering settings:
    // - Signature version: 2
    // - Show manifest properties: true
    // - Show declarations: true

    // Library unique name: <io.github.kotlin:library>
    final fun example/commonFun(): kotlin/Int // example/commonFun|commonFun(){}[0]

    // Targets: [apple]
    final class example/AppleOnly { // example/AppleOnly|null[0]
        constructor <init>() // example/AppleOnly.<init>|<init>(){}[0]

        final fun appleFun(): kotlin/Int // example/AppleOnly.appleFun|appleFun(){}[0]
    }
    """
        .trimIndent()
