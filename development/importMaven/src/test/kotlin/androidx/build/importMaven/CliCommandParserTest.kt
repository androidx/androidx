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
package androidx.build.importMaven

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

class CliCommandParserTest {
    @Test
    fun artifactCoordinatesViaArguments() {
        runCommand<ImportArtifact>("foo:bar:123") { cmd ->
            assertThat(
                cmd.artifacts()
            ).containsExactly("foo:bar:123")
        }
    }

    @Test
    fun multipleArtifactCoordinatesViaArguments() {
        runCommand<ImportArtifact>("foo:123", "bar:2.3.4") { cmd ->
            assertThat(
                cmd.artifacts()
            ).containsExactly("foo:123", "bar:2.3.4")
        }
    }

    @Test
    fun mixedArtifactAndArgumentInputs() {
        runCommand<ImportArtifact>("--artifacts", "foo:123,foo:345", "bar:2.3.4") { cmd ->
            assertThat(
                cmd.artifacts()
            ).containsExactly("foo:123", "foo:345", "bar:2.3.4")
        }
    }

    @Test
    fun artifactCoordinatesAsCommaSeparatedArguments() {
        runCommand<ImportArtifact>("foo:bar,bar:baz") { cmd ->
            assertThat(
                cmd.artifacts()
            ).containsExactly(
                "foo:bar", "bar:baz"
            )
        }
    }

    @Test
    fun importArtifactParameters() {
        val allSupportedImportMavenBaseArguments = listOf(
            "--verbose",
            "--androidx-build-id", "123",
            "--metalava-build-id", "345",
            "--support-repo-folder", "support/repo/path",
            "--allow-jetbrains-dev",
            "--redownload",
            "--repositories", "http://a.com,http://b.com",
            "--clean-local-repo",
            "--explicitly-fetch-inherited-dependencies"
        )
        val validateCommonArguments = { cmd: BaseImportMavenCommand ->
            assertThat(
                cmd.androidXBuildId
            ).isEqualTo(123)
            assertThat(
                cmd.verbose
            ).isTrue()
            assertThat(
                cmd.redownload
            ).isTrue()
            assertThat(
                cmd.metalavaBuildId
            ).isEqualTo(345)
            assertThat(
                cmd.supportRepoFolder
            ).isEqualTo("support/repo/path")
            assertThat(
                cmd.allowJetbrainsDev
            ).isTrue()
            assertThat(
                cmd.repositories
            ).containsExactly(
                "http://a.com", "http://b.com"
            )
            assertThat(
                cmd.cleanLocalRepo
            ).isTrue()
            assertThat(
                cmd.explicitlyFetchInheritedDependencies
            ).isTrue()
        }
        val importArtifactArgs = allSupportedImportMavenBaseArguments + listOf(
            "foo:bar",
            "foo2:bar2",
            "--artifacts",
            "bar:baz,bar2:baz2"
        )
        runCommand<BaseImportMavenCommand>(
            *importArtifactArgs.toTypedArray()
        ) { cmd ->
            assertThat(
                cmd.artifacts()
            ).containsExactly(
                "foo:bar", "foo2:bar2", "bar:baz", "bar2:baz2"
            )
            validateCommonArguments(cmd)
        }
        val importTomlArgs = listOf("import-toml") + allSupportedImportMavenBaseArguments
        runCommand<ImportToml>(
            *importTomlArgs.toTypedArray()
        ) { cmd ->
            validateCommonArguments(cmd)
        }
    }

    @Test
    fun noArguments() {
        val result = kotlin.runCatching {
            runCommand<ImportArtifact>() { cmd ->
                cmd.artifacts()
            }
        }
        assertThat(
            result.exceptionOrNull()
        ).hasMessageThat().contains(
            "Missing artifact coordinates"
        )
    }

    @Test
    fun importToml() {
        runCommand<ImportToml>("import-toml") { cmd ->
            assertThat(cmd.tomlFile).isNull()
        }
    }

    @Test
    fun importKonanArtifacts_missingKotlinVersion() {
        val exception = runInvalidCommand<ImportKonanBinariesCommand>("import-konan-binaries")
        assertThat(exception).hasMessageThat().contains(
            "Missing option \"--konan-compiler-version\""
        )
    }

    private inline fun <reified T : BaseCommand> runInvalidCommand(
        vararg args: String
    ): Throwable {
        val result = kotlin.runCatching {
            runCommand<T>(*args) { _ -> }
        }
        val exception = result.exceptionOrNull()
        assertWithMessage("expected the commmand to fail")
            .that(exception)
            .isNotNull()
        return exception!!
    }

    private inline fun <reified T : BaseCommand> runCommand(
        vararg args: String,
        crossinline block: (T) -> Unit
    ) = createCliCommands().also {
        var intercepted = false
        it.intercept { context ->
            if (context.invokedSubcommand == null) {
                val cmd = context.command
                if (T::class.isInstance(cmd)) {
                    block(cmd as T)
                } else {
                    throw AssertionError(
                        """
                    Expected to invoke command type of ${T::class} but invoked ${cmd::class}
                """.trimIndent()
                    )
                }
                intercepted = true
            }
        }
        it.parse(argv = args.toList(), parentContext = null)
        assertWithMessage(
            "Expected to intercept execution"
        ).that(
            intercepted
        ).isTrue()
    }
}