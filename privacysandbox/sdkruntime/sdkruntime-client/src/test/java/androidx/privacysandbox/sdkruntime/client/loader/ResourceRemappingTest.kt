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

package androidx.privacysandbox.sdkruntime.client.loader

import androidx.privacysandbox.sdkruntime.client.config.ResourceRemappingConfig
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.compiler.TestCompilationArguments
import androidx.room.compiler.processing.util.compiler.compile
import com.google.common.truth.Truth.assertThat
import java.net.URLClassLoader
import kotlin.reflect.KClass
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ResourceRemappingTest {

    @field:Rule
    @JvmField
    val temporaryFolder = TemporaryFolder()

    @Test
    fun apply_whenNonNullConfig_updatePackageId() {
        val classLoader = compileAndLoad(
            Source.java(
                "RPackage", """
                   public class RPackage {
                        public static int packageId = 0;
                   }
                """
            )
        )

        ResourceRemapping.apply(
            classLoader,
            ResourceRemappingConfig(
                rPackageClassName = "RPackage",
                packageId = 42
            )
        )

        val rPackageClass = classLoader.loadClass("RPackage")
        val packageIdField = rPackageClass.getDeclaredField("packageId")
        val value = packageIdField.getInt(null)

        assertThat(value).isEqualTo(42)
    }

    @Test
    fun apply_whenNullConfig_doesntThrow() {
        val classLoader = compileAndLoad(
            Source.java(
                "AnotherClass", """
                   public class AnotherClass {
                   }
                """
            )
        )

        ResourceRemapping.apply(
            classLoader,
            remappingConfig = null
        )
    }

    @Test
    fun apply_whenNoRPackageClass_throwsClassNotFoundException() {
        val source = Source.java(
            "AnotherClass", """
                public class AnotherClass {
                }
                """
        )

        val config = ResourceRemappingConfig(
            rPackageClassName = "RPackage",
            packageId = 42
        )

        assertThrows(ClassNotFoundException::class, source, config)
    }

    @Test
    fun apply_whenNoPackageIdField_throwsNoSuchFieldException() {
        val source = Source.java(
            "RPackage", """
                   public class RPackage {
                   }
                """
        )

        val config = ResourceRemappingConfig(
            rPackageClassName = "RPackage",
            packageId = 42
        )

        assertThrows(NoSuchFieldException::class, source, config)
    }

    private fun assertThrows(
        expectedThrowable: KClass<out Exception>,
        source: Source,
        config: ResourceRemappingConfig
    ) {
        val classLoader = compileAndLoad(source)
        assertThrows(expectedThrowable.java) {
            ResourceRemapping.apply(
                classLoader,
                config
            )
        }
    }

    private fun compileAndLoad(source: Source): ClassLoader {
        val compilationResult = compile(
            temporaryFolder.root,
            TestCompilationArguments(
                sources = listOf(source),
            )
        )

        assertThat(compilationResult.success).isTrue()

        return URLClassLoader.newInstance(
            compilationResult.outputClasspath.map {
                it.toURI().toURL()
            }.toTypedArray(),
            /* parent = */ null
        )
    }
}
