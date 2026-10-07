/*
 * Copyright (C) 2026 The Android Open Source Project
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

package androidx.test.backup.host

import com.android.adblib.AdbSession
import java.io.File
import java.lang.reflect.Method
import java.nio.file.Path
import java.util.Properties
import java.util.function.Function
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.rules.TemporaryFolder
import org.mockito.Mockito.any
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class BackupRestoreExtensionTest {

    @Suppress("UNUSED_PARAMETER")
    private fun dummyMethod(device: BackupRestoreController, other: String) {}

    @Suppress("UNUSED_PARAMETER")
    private fun migrationMethod(
        source: BackupRestoreController,
        dir: Path,
        target: BackupRestoreController,
    ) {}

    @Suppress("UNUSED_PARAMETER")
    private fun deviceAfterDirMethod(dir: Path, device: BackupRestoreController) {}

    @BackupRestoreConfig(applicationId = "com.example.outer") class ConfiguredTestClass

    class UnconfiguredTestClass

    @get:Rule val tempFolder = TemporaryFolder()

    private val rootStore = FakeStore()
    private val context = contextWithRootStore(rootStore)
    private val device = FakeAdbDevice()

    /** The base APK paths of the packages installed on [device], by package name. */
    private val installedPackages = mutableMapOf<String, String>()

    @Test
    fun testSupportsParameter() {
        val extension = BackupRestoreExtension()
        val mockParameterContext = mock(ParameterContext::class.java)
        val mockExtensionContext = mock(ExtensionContext::class.java)

        val method =
            BackupRestoreExtensionTest::class
                .java
                .getDeclaredMethod(
                    "dummyMethod",
                    BackupRestoreController::class.java,
                    String::class.java,
                )
        val parameters = method.parameters
        val deviceParameter = parameters[0]
        val otherParameter = parameters[1]

        // Test when parameter is BackupRestoreController
        `when`(mockParameterContext.parameter).thenReturn(deviceParameter)
        assertTrue(extension.supportsParameter(mockParameterContext, mockExtensionContext))

        // Test when parameter is something else
        `when`(mockParameterContext.parameter).thenReturn(otherParameter)
        assertFalse(extension.supportsParameter(mockParameterContext, mockExtensionContext))
    }

    @Test
    fun controllersSeparatedByAnotherParameterGetConsecutiveDevices() {
        val method =
            BackupRestoreExtensionTest::class
                .java
                .getDeclaredMethod(
                    "migrationMethod",
                    BackupRestoreController::class.java,
                    Path::class.java,
                    BackupRestoreController::class.java,
                )
        val extension = BackupRestoreExtension()

        assertEquals(0, extension.deviceIndexOf(parameterContext(method, 0)))
        assertEquals(1, extension.deviceIndexOf(parameterContext(method, 2)))
    }

    @Test
    fun aControllerAfterAnotherParameterGetsTheFirstDevice() {
        val method =
            BackupRestoreExtensionTest::class
                .java
                .getDeclaredMethod(
                    "deviceAfterDirMethod",
                    Path::class.java,
                    BackupRestoreController::class.java,
                )

        assertEquals(0, BackupRestoreExtension().deviceIndexOf(parameterContext(method, 1)))
    }

    @Test
    fun aTestClassWithoutConfigurationTargetsTheTestedApp() {
        val extension = BackupRestoreExtension()
        val suiteFile = suitePropertiesFile(TESTED_APPLICATION_ID to "com.example.tested")

        assertEquals(
            "com.example.tested",
            extension.applicationIdFor(
                UnconfiguredTestClass::class.java,
                extension.suiteProperties(suiteFile.path),
            ),
        )
    }

    @Test
    fun theConfiguredApplicationIdTakesPrecedenceOverTheTestedApp() {
        val extension = BackupRestoreExtension()
        val suiteFile = suitePropertiesFile(TESTED_APPLICATION_ID to "com.example.tested")
        val suiteProperties = extension.suiteProperties(suiteFile.path)

        assertEquals(
            "com.example.outer",
            extension.applicationIdFor(ConfiguredTestClass::class.java, suiteProperties),
        )
    }

    @Test
    fun theTestedAppCanComeFromASystemProperty() {
        val previous = System.setProperty(TESTED_APPLICATION_ID, "com.example.property")
        try {
            assertEquals(
                "com.example.property",
                BackupRestoreExtension()
                    .applicationIdFor(UnconfiguredTestClass::class.java, Properties()),
            )
        } finally {
            if (previous == null) {
                System.clearProperty(TESTED_APPLICATION_ID)
            } else {
                System.setProperty(TESTED_APPLICATION_ID, previous)
            }
        }
    }

    @Test
    fun aTestClassWithoutConfigurationOutsideOfATestSuiteFails() {
        val error =
            assertFailsWith<IllegalStateException> {
                BackupRestoreExtension()
                    .applicationIdFor(UnconfiguredTestClass::class.java, Properties())
            }

        assertContains(error.message.orEmpty(), UnconfiguredTestClass::class.java.name)
    }

    /** Each test class gets its own extension instance, but they all share one session. */
    @Test
    fun standaloneExtensionsShareOneSessionUntilTheTestRunEnds() {
        val session = mock(AdbSession::class.java)
        var sessionsCreated = 0
        val createSession = {
            sessionsCreated++
            session
        }
        val first = BackupRestoreExtension(isStandalone = true, createSession)
        val second = BackupRestoreExtension(isStandalone = true, createSession)

        assertSame(session, first.sessionFor(context))
        assertSame(session, second.sessionFor(context))
        assertEquals(1, sessionsCreated)
        verify(session, never()).close()

        rootStore.closeAll()

        verify(session).close()
    }

    @Test
    fun aStandaloneSessionThatFailsToCloseDoesNotFailTheTestRun() {
        val session = mock(AdbSession::class.java)
        doThrow(IllegalStateException("Session already closed")).`when`(session).close()
        BackupRestoreExtension(isStandalone = true) { session }.sessionFor(context)

        rootStore.closeAll()

        verify(session).close()
    }

    @Test
    fun aSharedSessionIsLeftOpenForItsOwner() {
        val session = mock(AdbSession::class.java)

        assertSame(session, BackupRestoreExtension(session).sessionFor(context))
        rootStore.closeAll()

        verify(session, never()).close()
    }

    @Test
    fun installOnceInstallsTheApksOnTheFirstResolution() = runBlocking {
        simulatePackageManager()

        BackupRestoreExtension().installOnce(context, controller(), APKS)

        assertEquals(2, pmInstalls())
    }

    /** Every test class has its own extension, and every resolution its own controller. */
    @Test
    fun installOnceDoesNotReinstallTheApksWhileTheAppIsUnchanged() = runBlocking {
        simulatePackageManager()
        val extension = BackupRestoreExtension()
        extension.installOnce(context, controller(), APKS)

        extension.installOnce(context, controller(), APKS)
        BackupRestoreExtension().installOnce(context, controller(), APKS)

        assertEquals(2, pmInstalls())
    }

    @Test
    fun installOnceReinstallsTheApksAfterATestInstalledTheAppAgain() = runBlocking {
        simulatePackageManager()
        val extension = BackupRestoreExtension()
        extension.installOnce(context, controller(), APKS)
        controller().installApk(Path.of("app-v1.apk"))

        extension.installOnce(context, controller(), APKS)

        assertEquals(5, pmInstalls())
    }

    @Test
    fun installOnceReinstallsTheApksAfterATestUninstalledTheApp() = runBlocking {
        simulatePackageManager()
        val extension = BackupRestoreExtension()
        extension.installOnce(context, controller(), APKS)
        installedPackages.remove(PACKAGE)

        extension.installOnce(context, controller(), APKS)

        assertEquals(4, pmInstalls())
    }

    @Test
    fun installOnceReinstallsTheApksAfterATestInstalledTheTestApkAgain() = runBlocking {
        simulatePackageManager()
        val extension = BackupRestoreExtension()
        extension.installOnce(context, controller(), APKS)
        installedPackages[TEST_PACKAGE] = "/data/app/~~other==/$TEST_PACKAGE==/base.apk"

        extension.installOnce(context, controller(), APKS)

        assertEquals(4, pmInstalls())
    }

    @Test
    fun installOnceInstallsOtherApksForTheSameApp() = runBlocking {
        simulatePackageManager()
        val extension = BackupRestoreExtension()
        extension.installOnce(context, controller(), APKS)

        extension.installOnce(context, controller(), listOf(File("app-release.apk")))

        assertEquals(3, pmInstalls())
    }

    @Test
    fun installOnceRunsNoCommandWithoutApks() = runBlocking {
        BackupRestoreExtension().installOnce(context, controller(), emptyList())

        assertEquals(emptyList(), device.commands)
    }

    @Test
    fun apkFilesInListsTheApksAndTheApksInDirectories() {
        val apk = tempFolder.newFile("app.apk")
        val dir = tempFolder.newFolder("test")
        val apkInDir = File(dir, "app-test.apk").apply { createNewFile() }
        File(dir, "output-metadata.json").createNewFile()
        val missing = File(tempFolder.root, "missing.apk")
        val paths = listOf(apk, dir, missing).joinToString(File.pathSeparator)

        assertEquals(listOf(apk, apkInDir), BackupRestoreExtension().apkFilesIn(paths))
        assertEquals(emptyList(), BackupRestoreExtension().apkFilesIn(null))
    }

    @Test
    fun suiteApksListsTheTestedApksThenTheTestApks() {
        val testedApk = tempFolder.newFile("app-debug.apk")
        val testApk = tempFolder.newFile("app-debug-backupTest.apk")
        val extension = BackupRestoreExtension()
        val suiteFile =
            suitePropertiesFile(
                "com.android.agp.test.TEST_APKS" to testApk.path,
                "com.android.agp.test.TESTED_APKS" to testedApk.path,
            )

        assertEquals(
            listOf(testedApk, testApk),
            extension.suiteApks(extension.suiteProperties(suiteFile.path)),
        )
    }

    @Test
    fun thereAreNoSuitePropertiesOutsideOfATestSuite() {
        val extension = BackupRestoreExtension()

        assertTrue(extension.suiteProperties(null).isEmpty)
        assertTrue(extension.suiteProperties("").isEmpty)
        assertTrue(extension.suiteProperties(File(tempFolder.root, "missing").path).isEmpty)
    }

    @Test
    fun unreadableSuitePropertiesAreIgnored() {
        val suiteFile = tempFolder.newFile("input-parameters.properties")
        suiteFile.writeText("$TESTED_APPLICATION_ID=com.example.tested\nmalformed=\\uXYZW\n")

        assertTrue(BackupRestoreExtension().suiteProperties(suiteFile.path).isEmpty)
    }

    /** Writes [inputs] to a properties file, as an Android Gradle Plugin test suite does. */
    private fun suitePropertiesFile(vararg inputs: Pair<String, String>): File {
        val properties = Properties()
        inputs.forEach { (name, value) -> properties.setProperty(name, value) }
        val file = tempFolder.newFile("input-parameters.properties")
        file.writer(Charsets.UTF_8).use { properties.store(it, "Input properties for test engine") }
        return file
    }

    private fun controller() = BackupRestoreControllerImpl(device.session, FAKE_SERIAL, 34, PACKAGE)

    /**
     * Makes [device] install every APK, and list [installedPackages] for `pm list packages -f`.
     * Like the package manager, it gives the packages new paths on every install. It does not tell
     * the APKs apart, so every install moves both the app and its test APK.
     */
    private fun simulatePackageManager() {
        device.onShell { command ->
            when {
                command.startsWith("pm install ") -> {
                    for (pkg in listOf(PACKAGE, TEST_PACKAGE)) {
                        installedPackages[pkg] = "/data/app/~~${pmInstalls()}==/$pkg==/base.apk"
                    }
                    shellOutput("Success\n")
                }
                command == "pm list packages -f $PACKAGE" ->
                    shellOutput(
                        installedPackages.entries.joinToString("") { (pkg, path) ->
                            "package:$path=$pkg\n"
                        }
                    )
                else -> null
            }
        }
    }

    private fun pmInstalls() = device.commands.count { it.startsWith("pm install ") }

    /**
     * An [ExtensionContext.Store] that closes its [CloseableResource]s on [closeAll], as JUnit does
     * when the context that owns the store ends.
     */
    private class FakeStore : ExtensionContext.Store {
        private val values = LinkedHashMap<Any, Any?>()

        /** Closes the stored resources in reverse order of insertion, like JUnit. */
        fun closeAll() {
            values.values.reversed().filterIsInstance<CloseableResource>().forEach { it.close() }
        }

        override fun get(key: Any): Any? = values[key]

        override fun <V> get(key: Any, requiredType: Class<V>): V = requiredType.cast(values[key])

        override fun <K, V> getOrComputeIfAbsent(key: K, defaultCreator: Function<K, V>): Any? =
            values.getOrPut(key as Any) { defaultCreator.apply(key) }

        override fun <K, V> getOrComputeIfAbsent(
            key: K,
            defaultCreator: Function<K, V>,
            requiredType: Class<V>,
        ): V = requiredType.cast(getOrComputeIfAbsent(key, defaultCreator))

        override fun put(key: Any, value: Any?) {
            values[key] = value
        }

        override fun remove(key: Any): Any? = values.remove(key)

        override fun <V> remove(key: Any, requiredType: Class<V>): V =
            requiredType.cast(values.remove(key))
    }

    private companion object {
        const val PACKAGE = "com.example.app"

        /** The package of the test APK, which AGP names after the app. */
        const val TEST_PACKAGE = "$PACKAGE.test"

        /** Test suite input with the application ID of the tested app. */
        const val TESTED_APPLICATION_ID = "com.android.junit.engine.tested.application.id"

        /** The APKs of a test suite: the app, then the test APK. */
        val APKS = listOf(File("app-debug.apk"), File("app-debug-backupTest.apk"))

        /** Returns a root [ExtensionContext] whose store, in any namespace, is [store]. */
        fun contextWithRootStore(store: ExtensionContext.Store): ExtensionContext {
            val context = mock(ExtensionContext::class.java)
            `when`(context.root).thenReturn(context)
            `when`(context.getStore(any())).thenReturn(store)
            return context
        }

        /** Returns the [ParameterContext] of the parameter of [method] at [index]. */
        fun parameterContext(method: Method, index: Int): ParameterContext {
            val context = mock(ParameterContext::class.java)
            `when`(context.parameter).thenReturn(method.parameters[index])
            `when`(context.index).thenReturn(index)
            return context
        }
    }
}
