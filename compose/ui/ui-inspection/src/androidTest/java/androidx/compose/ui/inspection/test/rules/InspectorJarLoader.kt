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

package androidx.compose.ui.inspection.test.rules

import androidx.inspection.InspectorEnvironment
import androidx.inspection.InspectorFactory
import androidx.inspection.testing.InspectorTester
import androidx.test.platform.app.InstrumentationRegistry
import dalvik.system.DexClassLoader
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipFile

object InspectorJarLoader {
    private var cachedFactory: InspectorFactory<*>? = null

    /**
     * Name of the native library built by `:compose:ui:ui-inspection`'s CMake build
     * (`src/main/cpp/CMakeLists.txt`) and packaged into `compose-ui-inspection.jar`. Loaded via
     * `System.loadLibrary("compose_inspection_jni")` in `LambdaLocation.kt`.
     */
    private const val NATIVE_LIB_NAME = "libcompose_inspection_jni.so"

    /** Name of the packaged inspector JAR. */
    private const val INSPECTOR_JAR_NAME = "compose-ui-inspection.jar"

    /**
     * Package name prefix of the integration test app that runs tests against the inspector JAR.
     */
    private const val JAR_TEST_APP_PACKAGE = "androidx.compose.ui.inspection.jartest"

    /**
     * Loads `ComposeLayoutInspectorFactory` dynamically from `compose-ui-inspection.jar` if the JAR
     * is present in the test APK's assets.
     *
     * In standard in-tree tests (`:compose:ui:ui-inspection`), inspector classes are compiled
     * directly into the test APK, and this function returns `null` so [InspectorTester] falls back
     * to standard [java.util.ServiceLoader] on the test APK classpath.
     *
     * In JAR integration tests
     * (`:compose:ui:ui-inspection:integration-tests:inspector-jar-testapp`),
     * `compose-ui-inspection.jar` is bundled as an asset. This function:
     * 1. Copies the JAR to [android.content.Context.getCodeCacheDir] and marks it read-only
     *    (required on Android 14+ for dynamic code loading security).
     * 2. Extracts `libcompose_inspection_jni.so` for the active device ABI so native JVMTI hooks
     *    work.
     * 3. Wraps the context classloader with a parent filter that throws [ClassNotFoundException]
     *    for all inspector implementation and shaded protobuf classes, forcing [DexClassLoader] to
     *    load them from `classes.dex` inside `compose-ui-inspection.jar` rather than the APK.
     * 4. Dynamically instantiates the factory to verify D8 dexing, dependency shadowing, and
     *    packaging.
     *
     * Returns the instantiated [InspectorFactory] override, or `null` if the asset is not present.
     */
    fun loadInspectorFactoryIfPresent(): InspectorFactory<*>? {
        cachedFactory?.let {
            return it
        }

        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.context
        val targetContext = instrumentation.targetContext
        val isJarTestApp = context.packageName.startsWith(JAR_TEST_APP_PACKAGE)

        val assetStream =
            try {
                context.assets.open(INSPECTOR_JAR_NAME)
            } catch (_: IOException) {
                try {
                    targetContext.assets.open(INSPECTOR_JAR_NAME)
                } catch (e: IOException) {
                    if (isJarTestApp) {
                        throw IllegalStateException(
                            "$INSPECTOR_JAR_NAME was not found in assets! " +
                                "Tests in inspector-jar-testapp must run against the inspector JAR.",
                            e,
                        )
                    }
                    null
                }
            } ?: return null

        val cacheDir = context.codeCacheDir ?: context.cacheDir
        val jarFile = File(cacheDir, INSPECTOR_JAR_NAME)
        if (jarFile.exists()) {
            jarFile.setWritable(true)
            jarFile.delete()
        }
        assetStream.use { input ->
            FileOutputStream(jarFile).use { output ->
                input.copyTo(output)
            }
        }
        jarFile.setReadOnly()

        var nativeLibDir: File? = null
        try {
            ZipFile(jarFile).use { zip ->
                for (abi in android.os.Build.SUPPORTED_ABIS) {
                    val entry = zip.getEntry("lib/$abi/$NATIVE_LIB_NAME")
                    if (entry != null) {
                        val libDir = File(cacheDir, "lib/$abi").apply { mkdirs() }
                        val soFile = File(libDir, NATIVE_LIB_NAME)
                        if (soFile.exists()) {
                            soFile.setWritable(true)
                            soFile.delete()
                        }
                        zip.getInputStream(entry).use { input ->
                            FileOutputStream(soFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                        soFile.setReadOnly()
                        nativeLibDir = libDir
                        break
                    }
                }
            }
        } catch (e: Exception) {
            throw IllegalStateException("Failed to extract $NATIVE_LIB_NAME from $jarFile", e)
        }

        val parentFilterClassLoader =
            object : ClassLoader(context.classLoader) {
                override fun loadClass(name: String?, resolve: Boolean): Class<*> {
                    if (
                        name != null &&
                            !name.startsWith("androidx.compose.ui.inspection.test.") &&
                            !name.startsWith("androidx.compose.ui.inspection.jartest.")
                    ) {
                        if (
                            name.startsWith("androidx.compose.ui.inspection.") ||
                                name.startsWith("layoutinspector.compose.inspection.") ||
                                name.startsWith("deps.ui.inspection.")
                        ) {
                            throw ClassNotFoundException(name)
                        }
                    }
                    return super.loadClass(name, resolve)
                }
            }

        val dexClassLoader =
            DexClassLoader(
                jarFile.absolutePath,
                cacheDir.absolutePath,
                nativeLibDir?.absolutePath,
                parentFilterClassLoader,
            )

        val factoryClass =
            dexClassLoader.loadClass("androidx.compose.ui.inspection.ComposeLayoutInspectorFactory")
        val factory = factoryClass.getDeclaredConstructor().newInstance() as InspectorFactory<*>
        cachedFactory = factory
        return factory
    }
}

/**
 * Creates an [InspectorTester] for inspection testing.
 *
 * If `compose-ui-inspection.jar` is packaged in assets (e.g. in the `inspector-jar-testapp`
 * module), this resolves the inspector factory dynamically from that JAR via
 * [InspectorJarLoader.loadInspectorFactoryIfPresent] and enforces that the JAR was loaded.
 *
 * Otherwise (in `:compose:ui:ui-inspection`), it returns an [InspectorTester] using default
 * [java.util.ServiceLoader] classpath lookup of in-tree compiled classes.
 */
suspend fun createInspectorTester(
    inspectorId: String = "layoutinspector.compose.inspection",
    environment: InspectorEnvironment? = null,
): InspectorTester {
    val factoryOverride = InspectorJarLoader.loadInspectorFactoryIfPresent()
    return InspectorTester(
        inspectorId = inspectorId,
        environment = environment,
        factoryOverride = factoryOverride,
    )
}
