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

import androidx.test.backup.host.BackupRestoreExtension.DeviceRequest
import androidx.test.backup.host.BackupRestoreExtension.OnlineDevice
import com.android.adblib.AdbSession
import java.io.File
import java.io.IOException
import java.lang.reflect.Method
import java.nio.file.Files
import java.nio.file.Path
import java.util.Optional
import java.util.Properties
import java.util.function.Function
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.function.ThrowingConsumer
import org.junit.platform.commons.PreconditionViolationException
import org.junit.rules.TemporaryFolder
import org.mockito.Mockito.any
import org.mockito.Mockito.anyString
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.opentest4j.TestAbortedException

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

    @Suppress("UNUSED_PARAMETER")
    private fun rolesMethod(
        @Device(role = "source") source: BackupRestoreController,
        dir: Path,
        @Device(role = "target") target: BackupRestoreController,
    ) {}

    @Suppress("UNUSED_PARAMETER")
    private fun apiRangeMethod(
        @Device(minApi = 34) newer: BackupRestoreController,
        @Device(maxApi = 33) older: BackupRestoreController,
    ) {}

    @Suppress("UNUSED_PARAMETER")
    private fun apiOutsideRangeMethod(
        @Device(api = 33, minApi = 34) device: BackupRestoreController
    ) {}

    @Suppress("UNUSED_PARAMETER")
    private fun invertedRangeMethod(
        @Device(role = "target", minApi = 35, maxApi = 33) device: BackupRestoreController
    ) {}

    @Suppress("UNUSED_PARAMETER")
    private fun negativeApiMethod(@Device(minApi = -1) device: BackupRestoreController) {}

    @Isolation(IsolationPolicy.MANUAL) private fun manuallyIsolatedMethod() {}

    @Isolation(IsolationPolicy.AUTOMATIC) class AutomaticallyIsolatedTestClass

    @BackupRestoreConfig(applicationId = "com.example.outer")
    @Isolation(IsolationPolicy.MANUAL)
    class ConfiguredTestClass {
        inner class NestedTestClass

        @BackupRestoreConfig(applicationId = "com.example.nested")
        inner class ConfiguredNestedTestClass

        @BackupRestoreConfig(testApplicationId = "com.example.nested.tests")
        inner class TestApkOnlyNestedTestClass

        class StaticNestedClass
    }

    class UnconfiguredTestClass

    @BackupRestoreConfig class DefaultConfiguredTestClass

    @BackupRestoreConfig(applicationId = "com.example.app", testApplicationId = "com.example.tests")
    class TestApkConfiguredTestClass {
        inner class NestedTestClass

        @BackupRestoreConfig(applicationId = "com.example.nested")
        inner class AppOnlyNestedTestClass {
            inner class DeeperTestClass
        }

        @BackupRestoreConfig(
            applicationId = "com.example.nested",
            testApplicationId = "com.example.nested.tests",
        )
        inner class FullyConfiguredNestedTestClass
    }

    @BackupRestoreConfig(applicationId = PACKAGE)
    class ConstructorInjectedTestClass(val controller: BackupRestoreController)

    @get:Rule val tempFolder = TemporaryFolder()

    private val rootStore = FakeStore()
    private val stores = mutableListOf(rootStore)
    private val context = contextWithRootStore(rootStore)
    private val device = FakeAdbDevice()

    /** The base APK paths of the packages installed on [device], by package name. */
    private val installedPackages = mutableMapOf<String, String>()

    @After
    fun tearDown() {
        stores.asReversed().forEach { it.closeAll() }
        rootStore.storedValues.filterIsInstance<Path>().forEach {
            it.toFile().deleteRecursively()
        }
        ActiveCaptureContext.clearCurrentThread()
        assertEquals(0, HostLogManager.activeCollectorCountForTesting())
        assertFalse(HostLogManager.hasAttachedHandlersForTesting())
    }

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
    fun aNestedTestClassTakesTheAnnotationsOfItsEnclosingClass() {
        val extension = BackupRestoreExtension()
        val nested = ConfiguredTestClass.NestedTestClass::class.java

        assertEquals(
            "com.example.outer",
            extension.findClassAnnotation(nested, BackupRestoreConfig::class.java)?.applicationId,
        )
        assertEquals(
            IsolationPolicy.MANUAL,
            extension.findClassAnnotation(nested, Isolation::class.java)?.value,
        )
    }

    @Test
    fun aNestedTestClassCanDeclareItsOwnAnnotation() {
        val nested = ConfiguredTestClass.ConfiguredNestedTestClass::class.java

        assertEquals(
            "com.example.nested",
            BackupRestoreExtension()
                .findClassAnnotation(nested, BackupRestoreConfig::class.java)
                ?.applicationId,
        )
    }

    @Test
    fun aStaticNestedClassDoesNotTakeTheAnnotationsOfItsEnclosingClass() {
        val nested = ConfiguredTestClass.StaticNestedClass::class.java

        assertNull(BackupRestoreExtension().findClassAnnotation(nested, Isolation::class.java))
    }

    /**
     * JUnit resolves the parameters of a test class constructor and of a `@BeforeAll` method in the
     * context of the class, which has no test method.
     */
    @Test
    fun aControllerOutsideOfATestMethodTakesTheIsolationOfItsClass() {
        val extension = BackupRestoreExtension()

        assertEquals(
            IsolationPolicy.MANUAL,
            extension.isolationPolicyFor(classContext(ConfiguredTestClass::class.java)),
        )
        assertEquals(
            IsolationPolicy.AUTOMATIC,
            extension.isolationPolicyFor(classContext(UnconfiguredTestClass::class.java)),
        )
    }

    /** The two annotations disagree, and the one of the method is not the default. */
    @Test
    fun theIsolationOfATestMethodTakesPrecedenceOverThatOfItsClass() {
        val method =
            BackupRestoreExtensionTest::class.java.getDeclaredMethod("manuallyIsolatedMethod")
        val context = classContext(AutomaticallyIsolatedTestClass::class.java)
        `when`(context.testMethod).thenReturn(Optional.of(method))

        assertEquals(IsolationPolicy.MANUAL, BackupRestoreExtension().isolationPolicyFor(context))
    }

    @Test
    fun aTestClassWithoutConfigurationTargetsTheTestedApp() {
        val extension = BackupRestoreExtension()
        val suiteFile = suitePropertiesFile(TESTED_APPLICATION_ID to "com.example.tested")
        val suiteProperties = extension.suiteProperties(suiteFile.path)

        assertEquals(
            "com.example.tested",
            extension.applicationIdFor(UnconfiguredTestClass::class.java, suiteProperties),
        )
        assertEquals(
            "com.example.tested",
            extension.applicationIdFor(DefaultConfiguredTestClass::class.java, suiteProperties),
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
        assertEquals(
            "com.example.outer",
            extension.applicationIdFor(
                ConfiguredTestClass.NestedTestClass::class.java,
                suiteProperties,
            ),
        )
        assertEquals(
            "com.example.outer",
            extension.applicationIdFor(
                ConfiguredTestClass.TestApkOnlyNestedTestClass::class.java,
                suiteProperties,
            ),
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
        val extension = BackupRestoreExtension()
        for (testClass in
            listOf(UnconfiguredTestClass::class.java, DefaultConfiguredTestClass::class.java)) {
            val error =
                assertFailsWith<IllegalStateException> {
                    extension.applicationIdFor(testClass, Properties())
                }

            assertContains(error.message.orEmpty(), testClass.name)
        }
    }

    /** Each test class gets its own extension instance, but they all share one session. */
    @Test
    fun extensionsShareOneSessionUntilTheTestRunEnds() {
        val session = mock(AdbSession::class.java)
        var sessionsCreated = 0
        val createSession = {
            sessionsCreated++
            session
        }
        val first = BackupRestoreExtension(createSession)
        val second = BackupRestoreExtension(createSession)

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
        BackupRestoreExtension { session }.sessionFor(context)

        rootStore.closeAll()

        verify(session).close()
    }

    /** JUnit 5.13 and later close a stored [AutoCloseable], and warn about other resources. */
    @Test
    fun aStandaloneSessionCanBeClosedAsAnAutoCloseable() {
        val session = mock(AdbSession::class.java)
        BackupRestoreExtension { session }.sessionFor(context)

        val stored = rootStore.storedValues.single()
        assertIs<CloseableResource>(stored)
        assertIs<AutoCloseable>(stored).close()

        verify(session).close()
    }

    @Test
    fun aRoleIsBoundToTheDeviceThatItsPropertyNames() =
        withDeviceProperties(
            "$SERIAL_PROPERTY.source" to "emulator-5556",
            "$SERIAL_PROPERTY.target" to "emulator-5554",
            "${SERIAL_PROPERTY}s" to "emulator-5554,emulator-5556",
        ) {
            val extension = BackupRestoreExtension()

            // The binding takes precedence over the order of the parameters.
            assertEquals("emulator-5556", extension.requestedSerial(Device(role = "source"), 0))
            assertEquals("emulator-5554", extension.requestedSerial(Device(role = "target"), 1))
        }

    @Test
    fun aRoleWithoutABindingKeepsTheOrderOfTheParameters() =
        withDeviceProperties("${SERIAL_PROPERTY}s" to "emulator-5554,emulator-5556") {
            val extension = BackupRestoreExtension()

            assertEquals("emulator-5554", extension.requestedSerial(Device(role = "source"), 0))
            assertEquals("emulator-5556", extension.requestedSerial(Device(role = "target"), 1))
            assertEquals("", extension.requestedSerial(Device(role = "other"), 2))
        }

    @Test
    fun theBindingOfTheRoleTakesPrecedenceOverThatOfTheSerial() =
        withDeviceProperties(
            "$SERIAL_PROPERTY.source" to "emulator-5556",
            "$SERIAL_PROPERTY.primary" to "emulator-5554",
        ) {
            assertEquals(
                "emulator-5556",
                BackupRestoreExtension()
                    .requestedSerial(Device(role = "source", serial = "primary"), 0),
            )
        }

    /** A serial can also be a name that a property binds, or else it is a literal serial. */
    @Test
    fun aSerialIsBoundToTheDeviceThatItsPropertyNames() =
        withDeviceProperties("$SERIAL_PROPERTY.target" to "emulator-5556") {
            val extension = BackupRestoreExtension()

            assertEquals("emulator-5556", extension.requestedSerial(Device(serial = "target"), 0))
            assertEquals(
                "emulator-5554",
                extension.requestedSerial(Device(serial = "emulator-5554"), 0),
            )
        }

    @Test
    fun theGlobalSerialAppliesToEveryParameterWithoutABinding() =
        withDeviceProperties(SERIAL_PROPERTY to "emulator-5556") {
            val extension = BackupRestoreExtension()

            assertEquals("emulator-5556", extension.requestedSerial(null, 0))
            assertEquals("emulator-5556", extension.requestedSerial(Device(role = "target"), 1))
        }

    @Test
    fun theApiLevelsAreAssignedInTheOrderOfTheParameters() =
        withDeviceProperties("$DEVICE_PROPERTY.apis" to "34,35") {
            val extension = BackupRestoreExtension()

            assertEquals(34, extension.requestedApi(null, 0))
            assertEquals(35, extension.requestedApi(Device(api = 31), 1))
            assertEquals(31, extension.requestedApi(Device(api = 31), 2))
        }

    @Test
    fun theControllerParametersGetTheOnlineDevicesInTheOrderOfTheirSerialNumbers() {
        assertEquals(
            listOf(SERIAL_1, SERIAL_2),
            BackupRestoreExtension()
                .assignDevices(
                    listOf(DeviceRequest("", 0), DeviceRequest("", 0)),
                    listOf(OnlineDevice(SERIAL_2, 34), OnlineDevice(SERIAL_1, 34)),
                ),
        )
    }

    @Test
    fun aDeviceThatAParameterAsksForIsNotAssignedToAnotherParameter() {
        val extension = BackupRestoreExtension()
        val devices = listOf(OnlineDevice(SERIAL_1, 34), OnlineDevice(SERIAL_2, 34))

        assertEquals(
            listOf(SERIAL_2, SERIAL_1),
            extension.assignDevices(
                listOf(DeviceRequest(SERIAL_2, 0), DeviceRequest("", 0)),
                devices,
            ),
        )
        assertEquals(
            listOf(SERIAL_2, SERIAL_1),
            extension.assignDevices(
                listOf(DeviceRequest("", 0), DeviceRequest(SERIAL_1, 0)),
                devices,
            ),
        )
    }

    @Test
    fun aParameterDoesNotGetTheDeviceOfAnEarlierParameterWithAnApiLevel() {
        assertEquals(
            listOf(SERIAL_2, SERIAL_1),
            BackupRestoreExtension()
                .assignDevices(
                    listOf(DeviceRequest("", 34), DeviceRequest("", 0)),
                    listOf(OnlineDevice(SERIAL_1, 33), OnlineDevice(SERIAL_2, 34)),
                ),
        )
    }

    /**
     * An earlier parameter moves to another device that it matches, so a later one gets its own.
     */
    @Test
    fun parametersWithOverlappingApiLevelRangesGetDevicesOfTheirOwn() {
        assertEquals(
            listOf(SERIAL_2, SERIAL_1),
            BackupRestoreExtension()
                .assignDevices(
                    listOf(DeviceRequest("", 0, minApi = 33), DeviceRequest("", 0, minApi = 35)),
                    listOf(OnlineDevice(SERIAL_1, 35), OnlineDevice(SERIAL_2, 34)),
                ),
        )
    }

    @Test
    fun parametersShareTheFirstDeviceThatMatchesThemWhenTooFewDevicesMatch() {
        val extension = BackupRestoreExtension()
        val devices = listOf(OnlineDevice(SERIAL_1, 34))

        assertEquals(
            listOf(SERIAL_1, SERIAL_1),
            extension.assignDevices(listOf(DeviceRequest("", 0), DeviceRequest("", 0)), devices),
        )
        assertEquals(
            listOf(SERIAL_1, SERIAL_1),
            extension.assignDevices(
                listOf(DeviceRequest(SERIAL_1, 0), DeviceRequest("", 0)),
                devices,
            ),
        )
    }

    @Test
    fun aParameterThatNoOnlineDeviceMatchesGetsNoDevice() {
        assertEquals(
            listOf(null, null, null, SERIAL_1),
            BackupRestoreExtension()
                .assignDevices(
                    listOf(
                        DeviceRequest(SERIAL_2, 0),
                        DeviceRequest("", 35),
                        DeviceRequest(SERIAL_1, 35),
                        DeviceRequest("", 0),
                    ),
                    listOf(OnlineDevice(SERIAL_1, 34)),
                ),
        )
    }

    @Test
    fun theMessageForAParameterThatNoDeviceMatchesNamesItsRequestAndRole() {
        val extension = BackupRestoreExtension()
        val devices = listOf(OnlineDevice(SERIAL_1, 34), OnlineDevice(SERIAL_2, 33))

        assertEquals(
            "Could not find any online devices matching requirements: API=0, " +
                "Serial='emulator-5558' (role 'target'). " +
                "Available devices: [emulator-5554 (API 34), emulator-5556 (API 33)]",
            extension.noMatchingDeviceMessage(DeviceRequest("emulator-5558", 0), "target", devices),
        )
        assertEquals(
            "Could not find any online devices matching requirements: API=35, Serial=''. " +
                "Available devices: [emulator-5554 (API 34), emulator-5556 (API 33)]",
            extension.noMatchingDeviceMessage(DeviceRequest("", 35), "", devices),
        )
        assertEquals(
            "Could not find any online devices matching requirements: API=0, minApi=35, " +
                "maxApi=36, Serial=''. " +
                "Available devices: [emulator-5554 (API 34), emulator-5556 (API 33)]",
            extension.noMatchingDeviceMessage(DeviceRequest("", 0, 35, 36), "", devices),
        )
    }

    @Test
    fun aParameterGetsADeviceWithinItsApiLevelRange() {
        val extension = BackupRestoreExtension()
        val method =
            BackupRestoreExtensionTest::class
                .java
                .getDeclaredMethod(
                    "apiRangeMethod",
                    BackupRestoreController::class.java,
                    BackupRestoreController::class.java,
                )

        val requests = extension.deviceRequestsOf(method)

        assertEquals(
            listOf(DeviceRequest("", 0, minApi = 34), DeviceRequest("", 0, maxApi = 33)),
            requests,
        )
        assertEquals(
            listOf(SERIAL_2, SERIAL_1),
            extension.assignDevices(
                requests,
                listOf(OnlineDevice(SERIAL_1, 33), OnlineDevice(SERIAL_2, 35)),
            ),
        )
        assertEquals(
            listOf(null),
            extension.assignDevices(
                listOf(DeviceRequest("", 33, minApi = 34)),
                listOf(OnlineDevice(SERIAL_1, 33), OnlineDevice(SERIAL_2, 35)),
            ),
        )
    }

    /**
     * A system property replaces the API level of a parameter, within the range of the parameter.
     */
    @Test
    fun anApiLevelPropertyWithinTheRangeOfAParameterReplacesItsApiLevel() =
        withDeviceProperties("$DEVICE_PROPERTY.apis" to "35,33") {
            assertEquals(
                listOf(DeviceRequest("", 35, minApi = 34), DeviceRequest("", 33, maxApi = 33)),
                BackupRestoreExtension().deviceRequestsOf(controllerMethod("apiRangeMethod", 2)),
            )
        }

    /** CI can't silently run a parameter on a device that its range excludes. */
    @Test
    fun anApiLevelPropertyOutsideTheRangeOfAParameterFailsTheTest() =
        withDeviceProperties("$DEVICE_PROPERTY.api" to "33") {
            val e =
                assertFailsWith<IllegalArgumentException> {
                    BackupRestoreExtension().deviceRequestsOf(controllerMethod("apiRangeMethod", 2))
                }

            assertEquals(
                "The system property androidx.test.backup.device.api asks for API level 33, but " +
                    "the @Device of controller parameter 0 allows only API level 34 or higher. Use " +
                    "an API level in that range, or androidx.test.backup.device.apis for an API " +
                    "level per parameter.",
                e.message,
            )
        }

    /** As for [Device.api], 0 in an API level property means any API level in the range. */
    @Test
    fun anApiLevelPropertyOfZeroAsksForAnyApiLevel() =
        withDeviceProperties("$DEVICE_PROPERTY.apis" to "0,33") {
            assertEquals(
                listOf(DeviceRequest("", 0, minApi = 34), DeviceRequest("", 33, maxApi = 33)),
                BackupRestoreExtension().deviceRequestsOf(controllerMethod("apiRangeMethod", 2)),
            )
        }

    @Test
    fun aDeviceWithValuesThatNoDeviceCanMatchFailsTheTest() {
        val extension = BackupRestoreExtension()
        val messages =
            listOf("apiOutsideRangeMethod", "invertedRangeMethod", "negativeApiMethod").map {
                assertFailsWith<IllegalArgumentException> {
                        extension.deviceRequestsOf(controllerMethod(it, 1))
                    }
                    .message
            }

        assertEquals(
            listOf(
                "The @Device of controller parameter 0 asks for API level 33, but its minApi and " +
                    "maxApi allow only API level 34 or higher.",
                "The @Device of controller parameter 0 (role 'target') sets minApi to 35, above " +
                    "its maxApi of 33.",
                "The @Device of controller parameter 0 sets minApi to -1, but an API level can't " +
                    "be negative.",
            ),
            messages,
        )
    }

    /** Binding one role must not give the device of that role to the other one as well. */
    @Test
    fun aRoleWithoutABindingGetsAnotherDeviceThanABoundRole() =
        withDeviceProperties("$SERIAL_PROPERTY.source" to SERIAL_2) {
            val extension = BackupRestoreExtension()
            val method =
                BackupRestoreExtensionTest::class
                    .java
                    .getDeclaredMethod(
                        "rolesMethod",
                        BackupRestoreController::class.java,
                        Path::class.java,
                        BackupRestoreController::class.java,
                    )

            val requests = extension.deviceRequestsOf(method)

            assertEquals(listOf(DeviceRequest(SERIAL_2, 0), DeviceRequest("", 0)), requests)
            assertEquals(
                listOf(SERIAL_2, SERIAL_1),
                extension.assignDevices(
                    requests,
                    listOf(OnlineDevice(SERIAL_1, 34), OnlineDevice(SERIAL_2, 34)),
                ),
            )
        }

    @Test
    fun theTestApplicationIdComesFromTheConfigurationOfTheTestClass() {
        val extension = BackupRestoreExtension()

        assertEquals(
            "com.example.tests",
            extension.testApplicationIdFor(TestApkConfiguredTestClass::class.java),
        )
        assertEquals(
            "com.example.tests",
            extension.testApplicationIdFor(TestApkConfiguredTestClass.NestedTestClass::class.java),
        )
        assertEquals(
            "com.example.nested.tests",
            extension.testApplicationIdFor(
                TestApkConfiguredTestClass.FullyConfiguredNestedTestClass::class.java
            ),
        )
        assertEquals(
            "com.example.nested.tests",
            extension.testApplicationIdFor(
                ConfiguredTestClass.TestApkOnlyNestedTestClass::class.java
            ),
        )
        assertNull(extension.testApplicationIdFor(ConfiguredTestClass::class.java))
        assertNull(extension.testApplicationIdFor(UnconfiguredTestClass::class.java))
        assertNull(extension.testApplicationIdFor(DefaultConfiguredTestClass::class.java))
    }

    /** The test APK of an enclosing class was chosen for its app, so it isn't used for another. */
    @Test
    fun theTestApplicationIdOfAnEnclosingClassDoesNotApplyToAnotherApp() {
        val extension = BackupRestoreExtension()

        assertNull(
            extension.testApplicationIdFor(
                TestApkConfiguredTestClass.AppOnlyNestedTestClass::class.java
            )
        )
        assertNull(
            extension.testApplicationIdFor(
                TestApkConfiguredTestClass.AppOnlyNestedTestClass.DeeperTestClass::class.java
            )
        )
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

    @Test
    fun aTestThatDoesNotFailSavesNoDeviceLogs() = withDeviceProperties {
        val extension = BackupRestoreExtension { device.session }
        for (failure in listOf(null, TestAbortedException("Assumption failed"))) {
            val testClass = childContext(context, CLASS_ID)
            val test = childContext(testClass, TEST_ID, failure)
            extension.beforeAll(testClass)
            extension.track(testClass, controller())
            extension.beforeEach(test)
            extension.track(test, controller())

            extension.afterEach(test)
            extension.afterAll(testClass)
        }

        assertEquals(emptyList(), device.commands)
    }

    /** An extension registered in an instance field gets no callbacks for its test class. */
    @Test
    fun aFailedTestSavesTheLogsOfTheDevicesThatItUsed() = withResultsDir { resultsDir ->
        stubDeviceLogs("I Backup: restore failed\n")
        val failure = AssertionError("The restored data differs")
        val test = childContext(childContext(context, CLASS_ID), TEST_ID, failure)
        val extension = BackupRestoreExtension { device.session }
        extension.beforeEach(test)
        extension.track(test, controller())

        extension.afterEach(test)

        val detailId = extension.detailIdOf(TEST_ID)
        val logs = File(resultsDir, "suite-details/$detailId/logcat-$FAKE_SERIAL.txt")
        assertEquals("I Backup: restore failed\n", logs.readText())
        assertEquals("Device logs saved to $logs", failure.suppressed.single().toString())
    }

    @Test
    fun reflectiveJunitFilePublisherInvokesPublishFileTargetMethod() {
        val outDir = tempFolder.newFolder("reflective-publish").toPath()
        val parseMethod = FakeJunit512MediaType::class.java.getMethod("parse", String::class.java)
        val publishMethod =
            FakeJunit512Context::class
                .java
                .getMethod(
                    "publishFile",
                    String::class.java,
                    FakeJunit512MediaType::class.java,
                    ThrowingConsumer::class.java,
                )
        val fakeCtx = FakeJunit512Context(outDir)

        val written =
            ReflectiveJunitFilePublisher.invokePublishFile(
                target = fakeCtx,
                parseMediaType = parseMethod,
                publishFile = publishMethod,
                fileName = "host-log.txt",
                mediaType = "text/plain; charset=utf-8",
            ) { path ->
                Files.write(path, "hello attachment".toByteArray(Charsets.UTF_8))
            }
        assertNotNull(written)
        assertEquals("hello attachment", Files.readAllBytes(written).toString(Charsets.UTF_8))
        assertEquals("text/plain; charset=utf-8", fakeCtx.lastMediaType?.value)

        // Target exceptions unwrap InvocationTargetException cleanly.
        assertFailsWith<IOException> {
            ReflectiveJunitFilePublisher.invokePublishFile(
                target = fakeCtx,
                parseMediaType = parseMethod,
                publishFile = publishMethod,
                fileName = "broken.txt",
                mediaType = "text/plain; charset=utf-8",
            ) {
                throw IOException("disk full")
            }
        }
    }

    @Test
    fun reflectiveJunitFilePublisherPrefersApiMediaTypeWithExtensionFallback() {
        assertEquals(
            listOf(
                "org.junit.jupiter.api.MediaType",
                "org.junit.jupiter.api.extension.MediaType",
            ),
            ReflectiveJunitFilePublisher.MEDIA_TYPE_CLASS_NAMES,
        )

        val primaryBinding =
            ReflectiveJunitFilePublisher.resolveBinding(
                targetClass = FakeJunit512Context::class.java,
                mediaTypeClassNames =
                    listOf(
                        FakePrimaryMediaType::class.java.name,
                        FakeJunit512MediaType::class.java.name,
                    ),
            )
        assertNotNull(primaryBinding)
        assertEquals(FakePrimaryMediaType::class.java, primaryBinding.parseMediaType.declaringClass)

        val fallbackBinding =
            ReflectiveJunitFilePublisher.resolveBinding(
                targetClass = FakeJunit512Context::class.java,
                mediaTypeClassNames =
                    listOf(
                        "org.junit.jupiter.api.MissingMediaType",
                        FakeJunit512MediaType::class.java.name,
                    ),
            )
        assertNotNull(fallbackBinding)
        assertEquals(
            FakeJunit512MediaType::class.java,
            fallbackBinding.parseMediaType.declaringClass,
        )

        assertNull(
            ReflectiveJunitFilePublisher.resolveBinding(
                targetClass = FakeJunit512Context::class.java,
                mediaTypeClassNames = listOf("org.junit.jupiter.api.MissingMediaType"),
            )
        )
    }

    @Test
    fun aFailedTestSavesTheLogsOfTheDevicesThatItsClassShares() = withResultsDir { resultsDir ->
        stubDeviceLogs("I Backup: restore failed\n")
        val failure = AssertionError("The restored data differs")
        val testClass = childContext(context, CLASS_ID)
        val test = childContext(testClass, TEST_ID, failure)
        val extension = BackupRestoreExtension { device.session }
        extension.beforeAll(testClass)
        extension.track(testClass, controller())
        extension.beforeEach(test)
        extension.track(test, controller())

        extension.afterEach(test)
        extension.afterAll(testClass)

        assertEquals(1, device.commands.count { it.startsWith("logcat ") })
        val detailId = extension.detailIdOf(TEST_ID)
        val logs = File(resultsDir, "suite-details/$detailId/logcat-$FAKE_SERIAL.txt")
        assertEquals("Device logs saved to $logs", failure.suppressed.single().toString())
    }

    @Test
    fun aFailedTestClassSavesTheLogsOfItsDevices() = withResultsDir { resultsDir ->
        stubDeviceLogs("I Backup: restore failed\n")
        val failure = IllegalStateException("No online Android devices")
        val testClass = childContext(context, CLASS_ID, failure)
        val extension = BackupRestoreExtension { device.session }
        extension.beforeAll(testClass)
        extension.track(testClass, controller())

        extension.afterAll(testClass)

        val detailId = extension.detailIdOf(CLASS_ID)
        val logs = File(resultsDir, "suite-details/$detailId/logcat-$FAKE_SERIAL.txt")
        assertEquals("I Backup: restore failed\n", logs.readText())
        assertEquals("Device logs saved to $logs", failure.suppressed.single().toString())
    }

    @Test
    fun aFailedTestSavesNoDeviceLogsWhenTheyAreTurnedOff() =
        withDeviceProperties("$DEVICE_PROPERTY.logs.enabled" to "false") {
            val failure = AssertionError("The restored data differs")
            val test = childContext(childContext(context, CLASS_ID), TEST_ID, failure)
            val extension = BackupRestoreExtension { device.session }
            extension.beforeEach(test)
            extension.track(test, controller())

            extension.afterEach(test)

            assertEquals(emptyList(), device.commands)
            assertEquals(0, failure.suppressed.size)
        }

    @Test
    fun aFailureToSaveDeviceLogsKeepsTheFailureOfTheTest() = withResultsDir {
        // The device prints no time for `date`, so its logs can't be read.
        val failure = AssertionError("The restored data differs")
        val test = childContext(childContext(context, CLASS_ID), TEST_ID, failure)
        val extension = BackupRestoreExtension { device.session }
        extension.beforeEach(test)
        extension.track(test, controller())

        extension.afterEach(test)

        assertEquals(listOf("date +%s"), device.commands)
        assertEquals(0, failure.suppressed.size)
    }

    @Test
    fun resolveParameterResolvesControllerAndRecordsPrepareStep() = withResultsDir { resultsDir ->
        device.stubOnlineDevices(FAKE_SERIAL to 34)
        device.stubReadonlyProperties(mapOf("ro.product.model" to "Pixel 8"))
        device.onShell { cmd ->
            when (cmd) {
                "pm clear com.example.outer" -> shellOutput("Success\n")
                else -> null
            }
        }
        val extension = BackupRestoreExtension { device.session }
        val testClass = childContext(context, CLASS_ID)
        val test =
            childContext(
                testClass,
                TEST_ID,
                testClass = AutomaticallyIsolatedConfiguredClass::class.java,
            )
        val method =
            BackupRestoreExtensionTest::class
                .java
                .getDeclaredMethod(
                    "dummyMethod",
                    BackupRestoreController::class.java,
                    String::class.java,
                )
        extension.beforeAll(testClass)
        extension.beforeEach(test)

        val resolved = extension.resolveParameter(parameterContext(method, 0), test)
        assertIs<BackupRestoreControllerImpl>(resolved)
        assertEquals(FAKE_SERIAL, resolved.serialNumber)
        assertEquals(34, resolved.apiLevel)
        assertEquals("com.example.outer", resolved.applicationId)
        assertContains(device.commands, "pm clear com.example.outer")
        assertTrue(device.commands.none { it.startsWith("getprop") })

        extension.afterEach(test)
        extension.afterAll(testClass)

        val detailId = extension.detailIdOf(TEST_ID)
        val record =
            TestDetailRecord.fromJson(File(resultsDir, "suite-details/$detailId.json").readText())
        assertEquals(1, record.steps.size)
        assertNull(record.steps.single().n)
        assertEquals("Prepare", record.steps.single().name)
        assertEquals("PASSED", record.steps.single().status)
    }

    @Test
    fun resolveParameterPreconditionFailureRestoresParentThreadBinding() = withDeviceProperties {
        val extension = BackupRestoreExtension { device.session }
        val testClass = childContext(context, CLASS_ID)
        extension.beforeAll(testClass)
        val classBoundCapture = ActiveCaptureContext.currentThreadBound()

        // Device running API 30 fails MIN_REQUIRED_API (31) during constructor injection.
        device.stubOnlineDevices(FAKE_SERIAL to 30)
        val published = LinkedHashMap<String, String>()
        val test =
            createChildContext(
                parent = testClass,
                uniqueId = TEST_ID,
                stores = stores,
                reportSink = published,
                testClass = ConstructorInjectedTestClass::class.java,
            )
        val ctor = ConstructorInjectedTestClass::class.java.declaredConstructors.single()
        val paramCtx = mock(ParameterContext::class.java)
        `when`(paramCtx.parameter).thenReturn(ctor.parameters[0])
        `when`(paramCtx.index).thenReturn(0)

        assertFailsWith<IllegalStateException> { extension.resolveParameter(paramCtx, test) }

        assertEquals(
            BackupErrorCode.UNSUPPORTED_API_LEVEL.name,
            published[BackupReportKeys.ERROR_CODE],
        )
        assertSame(classBoundCapture, ActiveCaptureContext.currentThreadBound())
        extension.afterAll(testClass)
    }

    @Test
    fun constructorInjectionFailureFinalizesDetailRecordAndLogsOnStoreClose() =
        withResultsDir { resultsDir ->
            device.stubOnlineDevices(FAKE_SERIAL to 34)
            device.onShell { cmd ->
                when {
                    cmd == "pm clear $PACKAGE" -> shellOutput("Success\n")
                    cmd == "date +%s" -> shellOutput("1700000000\n")
                    cmd.startsWith("logcat ") -> shellOutput("E AppInit: constructor crash\n")
                    else -> null
                }
            }
            val extension = BackupRestoreExtension { device.session }
            val testClass = childContext(context, CLASS_ID)
            extension.beforeAll(testClass)
            val classBoundCapture = ActiveCaptureContext.currentThreadBound()

            var ctorFailure: Throwable? = null
            val test =
                createChildContext(
                    parent = testClass,
                    uniqueId = TEST_ID,
                    stores = stores,
                    failureProvider = { ctorFailure },
                    testClass = ConstructorInjectedTestClass::class.java,
                )
            val testStore = test.getStore(ExtensionContext.Namespace.GLOBAL) as FakeStore

            val ctor = ConstructorInjectedTestClass::class.java.declaredConstructors.single()
            val paramCtx = mock(ParameterContext::class.java)
            `when`(paramCtx.parameter).thenReturn(ctor.parameters[0])
            `when`(paramCtx.index).thenReturn(0)

            // JUnit resolves constructor parameter BEFORE beforeEach, then constructor throws,
            // skipping both beforeEach and afterEach and closing only the test's Store.
            extension.resolveParameter(paramCtx, test)
            val thrown = IllegalStateException("constructor body failed")
            ctorFailure = thrown
            testStore.closeAll()

            assertSame(classBoundCapture, ActiveCaptureContext.currentThreadBound())
            val detailId = extension.detailIdOf(TEST_ID)
            val detailFile = File(resultsDir, "suite-details/$detailId.json")
            assertTrue(detailFile.isFile)
            val record = TestDetailRecord.fromJson(detailFile.readText())
            assertEquals("Prepare", record.steps.single().name)
            assertFalse(record.steps.single().shared)
            val logcatFile = File(resultsDir, "suite-details/$detailId/logcat-$FAKE_SERIAL.txt")
            assertEquals("E AppInit: constructor crash\n", logcatFile.readText())
            assertContains(
                thrown.suppressed.map { it.toString() },
                "Device logs saved to $logcatFile",
            )

            extension.afterAll(testClass)
        }

    @Test
    fun perMethodConstructorFailureOnClassContextSavesUniqueDetailWithoutClassReportPollution() =
        withResultsDir { resultsDir ->
            device.stubOnlineDevices(FAKE_SERIAL to 34)
            device.onShell { cmd ->
                when {
                    cmd == "pm clear $PACKAGE" -> shellOutput("Failed\n", exitCode = 1)
                    cmd == "date +%s" -> shellOutput("1700000000\n")
                    cmd.startsWith("logcat ") -> shellOutput("E PmClear: failed in ctor\n")
                    else -> null
                }
            }
            val classPublished = LinkedHashMap<String, String>()
            val extension = BackupRestoreExtension { device.session }
            val classCtx =
                createChildContext(
                    parent = context,
                    uniqueId = CLASS_ID,
                    stores = stores,
                    reportSink = classPublished,
                    testClass = ConstructorInjectedTestClass::class.java,
                )
            extension.beforeAll(classCtx)
            val classBoundCapture = ActiveCaptureContext.currentThreadBound()

            val ctor = ConstructorInjectedTestClass::class.java.declaredConstructors.single()
            val paramCtx = mock(ParameterContext::class.java)
            `when`(paramCtx.parameter).thenReturn(ctor.parameters[0])
            `when`(paramCtx.index).thenReturn(0)

            // 1. Preparation failure inside PER_METHOD constructor resolution on ClassContext:
            // JUnit skips beforeEach and invokes afterEach(methodContext) with prepErr, which
            // claims the pending capture and publishes androidx.report.detailPath on methodContext.
            val prepErr =
                assertFailsWith<IOException> { extension.resolveParameter(paramCtx, classCtx) }
            assertSame(classBoundCapture, ActiveCaptureContext.currentThreadBound())
            val methodPublished = LinkedHashMap<String, String>()
            val methodCtx =
                createChildContext(
                    parent = classCtx,
                    uniqueId = TEST_ID,
                    stores = stores,
                    failureProvider = { prepErr },
                    reportSink = methodPublished,
                    testClass = ConstructorInjectedTestClass::class.java,
                )
            extension.afterEach(methodCtx)

            assertNull(classPublished[BackupReportKeys.DETAIL_PATH])
            val methodDetailId = extension.detailIdOf(TEST_ID)
            assertEquals(
                "suite-details/$methodDetailId.json",
                methodPublished[BackupReportKeys.DETAIL_PATH],
            )
            val methodRecord =
                TestDetailRecord.fromJson(
                    File(resultsDir, "suite-details/$methodDetailId.json").readText()
                )
            assertEquals("Prepare", methodRecord.steps.single().name)
            assertEquals("FAILED", methodRecord.steps.single().status)
            assertTrue(prepErr.suppressed.any { it.toString().startsWith("Device logs saved to ") })

            // 2. Preparation failure on ClassContext when afterEach never runs (unclaimed at
            // afterAll) still finalizes a unique _ctor_ detail JSON without polluting ClassContext.
            val unclaimedErr =
                assertFailsWith<IOException> { extension.resolveParameter(paramCtx, classCtx) }

            // 3. Precondition failure on ClassContext still publishes BackupRestore.errorCode,
            // without publishing a method-level androidx.report.detailPath onto ClassContext.
            device.stubOnlineDevices(FAKE_SERIAL to 30)
            assertFailsWith<IllegalStateException> {
                extension.resolveParameter(paramCtx, classCtx)
            }
            assertEquals(
                BackupErrorCode.UNSUPPORTED_API_LEVEL.name,
                classPublished[BackupReportKeys.ERROR_CODE],
            )
            assertNull(classPublished[BackupReportKeys.DETAIL_PATH])
            assertSame(classBoundCapture, ActiveCaptureContext.currentThreadBound())

            extension.afterAll(classCtx)
            val classDetailPrefix = "${extension.detailIdOf(CLASS_ID)}_ctor_"
            val ctorJson =
                File(resultsDir, "suite-details").listFiles().orEmpty().single {
                    it.name.startsWith(classDetailPrefix) && it.name.endsWith(".json")
                }
            val ctorRecord = TestDetailRecord.fromJson(ctorJson.readText())
            assertEquals("Prepare", ctorRecord.steps.single().name)
            assertEquals("FAILED", ctorRecord.steps.single().status)
            assertTrue(
                unclaimedErr.suppressed.any { it.toString().startsWith("Device logs saved to ") }
            )
        }

    @Test
    fun perMethodCtorBodyFailureClaimedInAfterEachAndNestedPerMethodCtorsMergeIntoLeaf() =
        withResultsDir { resultsDir ->
            device.stubOnlineDevices(FAKE_SERIAL to 34)
            device.onShell { cmd ->
                when {
                    cmd == "pm clear $PACKAGE" -> shellOutput("Success\n")
                    cmd == "date +%s" -> shellOutput("1700000000\n")
                    cmd.startsWith("logcat ") -> shellOutput("E CtorBody: crashed after param 0\n")
                    else -> null
                }
            }
            val extension = BackupRestoreExtension { device.session }
            val outerCtx =
                createChildContext(
                    parent = context,
                    uniqueId = CLASS_ID,
                    stores = stores,
                    testClass = OuterPerMethodCtorTestClass::class.java,
                )
            val outerCtor = OuterPerMethodCtorTestClass::class.java.declaredConstructors.single()
            val outerParamCtx = mock(ParameterContext::class.java)
            `when`(outerParamCtx.parameter).thenReturn(outerCtor.parameters[0])
            `when`(outerParamCtx.index).thenReturn(0)

            extension.beforeAll(outerCtx)

            // 1. Outer PER_METHOD constructor resolves param 0, then constructor body throws
            // before beforeEach runs; JUnit invokes afterEach(failMethodCtx) with ctorBodyErr.
            val failPublished = LinkedHashMap<String, String>()
            val ctorBodyErr = IllegalStateException("ctor body threw after param 0")
            val failTestId = "$CLASS_ID/[method:testOne()]"
            val failTestCtx =
                createChildContext(
                    parent = outerCtx,
                    uniqueId = failTestId,
                    stores = stores,
                    failureProvider = { ctorBodyErr },
                    reportSink = failPublished,
                    testClass = OuterPerMethodCtorTestClass::class.java,
                )
            extension.resolveParameter(outerParamCtx, outerCtx)
            extension.afterEach(failTestCtx)

            val failDetailId = extension.detailIdOf(failTestId)
            assertEquals(
                "suite-details/$failDetailId.json",
                failPublished[BackupReportKeys.DETAIL_PATH],
            )
            assertTrue(
                ctorBodyErr.suppressed.any { it.toString().startsWith("Device logs saved to ") }
            )

            // 2. @Nested inner class with PER_METHOD lifecycle where both outer and inner
            // constructors inject BackupRestoreController before beforeEach(leafCtx) runs.
            val nestedId = "$CLASS_ID/[nested-class:Inner]"
            val nestedCtx =
                createChildContext(
                    parent = outerCtx,
                    uniqueId = nestedId,
                    stores = stores,
                    testClass = OuterPerMethodCtorTestClass.Inner::class.java,
                )
            val innerCtor =
                OuterPerMethodCtorTestClass.Inner::class.java.declaredConstructors.single()
            val innerParamCtx = mock(ParameterContext::class.java)
            `when`(innerParamCtx.parameter).thenReturn(innerCtor.parameters[1])
            `when`(innerParamCtx.index).thenReturn(1)

            extension.beforeAll(nestedCtx)
            extension.resolveParameter(outerParamCtx, outerCtx)
            extension.resolveParameter(innerParamCtx, nestedCtx)

            val leafPublished = LinkedHashMap<String, String>()
            val leafId = "$nestedId/[method:nestedTest()]"
            val leafCtx =
                createChildContext(
                    parent = nestedCtx,
                    uniqueId = leafId,
                    stores = stores,
                    reportSink = leafPublished,
                    testClass = OuterPerMethodCtorTestClass.Inner::class.java,
                )
            extension.beforeEach(leafCtx)
            extension.afterEach(leafCtx)
            extension.afterAll(nestedCtx)
            extension.afterAll(outerCtx)

            val leafDetailId = extension.detailIdOf(leafId)
            assertEquals(
                "suite-details/$leafDetailId.json",
                leafPublished[BackupReportKeys.DETAIL_PATH],
            )
            val leafRecord =
                TestDetailRecord.fromJson(
                    File(resultsDir, "suite-details/$leafDetailId.json").readText()
                )
            assertEquals(listOf("Prepare", "Prepare"), leafRecord.steps.map { it.name })
            assertTrue(leafRecord.steps.none { it.shared })
            assertEquals(
                OuterPerMethodCtorTestClass.Inner::class.java.name,
                leafRecord.sourceLocation?.className,
            )
            assertTrue(
                File(resultsDir, "suite-details").listFiles().orEmpty().none {
                    "_ctor_" in it.name
                }
            )
        }

    @Test
    fun perMethodUnclaimedConstructorCaptureEvictsSilentlyWhenSuccessfulAndWritesDetailOnFailure() =
        withResultsDir { resultsDir ->
            var failPrepare = false
            device.stubOnlineDevices(FAKE_SERIAL to 34)
            device.onShell { cmd ->
                when {
                    cmd == "pm clear $PACKAGE" ->
                        if (failPrepare) shellOutput("Failed\n", exitCode = 1)
                        else shellOutput("Success\n")
                    cmd == "date +%s" -> shellOutput("1700000000\n")
                    cmd.startsWith("logcat ") -> shellOutput("E CtorBody: crashed after param 0\n")
                    else -> null
                }
            }
            val classPublished = LinkedHashMap<String, String>()
            val extension = BackupRestoreExtension { device.session }
            val outerCtx =
                createChildContext(
                    parent = context,
                    uniqueId = CLASS_ID,
                    stores = stores,
                    reportSink = classPublished,
                    testClass = OuterPerMethodCtorTestClass::class.java,
                )
            extension.beforeAll(outerCtx)
            val outerCtor = OuterPerMethodCtorTestClass::class.java.declaredConstructors.single()
            val outerParamCtx = mock(ParameterContext::class.java)
            `when`(outerParamCtx.parameter).thenReturn(outerCtor.parameters[0])
            `when`(outerParamCtx.index).thenReturn(0)

            // 1. @Nested @TestInstance(PER_CLASS) inner class inside a PER_METHOD outer class:
            // JUnit instantiates the outer class (resolving outerController) and then runs
            // beforeAll(nestedPerClassCtx), which must evict the unclaimed pending capture
            // silently without pulling logcat or writing a bogus FAILED _ctor_ detail JSON.
            val nestedPerClassId = "$CLASS_ID/[nested-class:NestedPerClass]"
            val nestedPerClassCtx =
                createChildContext(
                    parent = outerCtx,
                    uniqueId = nestedPerClassId,
                    stores = stores,
                    testClass = OuterPerMethodCtorTestClass.NestedPerClass::class.java,
                    lifecycle = TestInstance.Lifecycle.PER_CLASS,
                )
            extension.resolveParameter(outerParamCtx, outerCtx)
            extension.beforeAll(nestedPerClassCtx)

            assertEquals(0, device.commands.count { it.startsWith("logcat ") })
            assertTrue(
                File(resultsDir, "suite-details").listFiles().orEmpty().none {
                    "_ctor_" in it.name
                }
            )
            val nestedBound = ActiveCaptureContext.currentThreadBound()
            assertNotNull(nestedBound)
            assertTrue(nestedBound.isShared)

            val nestedTestPublished = LinkedHashMap<String, String>()
            val nestedTestId = "$nestedPerClassId/[method:nestedTest()]"
            val nestedTestCtx =
                createChildContext(
                    parent = nestedPerClassCtx,
                    uniqueId = nestedTestId,
                    stores = stores,
                    reportSink = nestedTestPublished,
                    testClass = OuterPerMethodCtorTestClass.NestedPerClass::class.java,
                    lifecycle = TestInstance.Lifecycle.PER_CLASS,
                )
            extension.beforeEach(nestedTestCtx)
            extension.afterEach(nestedTestCtx)
            extension.afterAll(nestedPerClassCtx)

            // 2. Failed unclaimed attempt (pending.failure set) evicted by the next test's
            // constructor parameter still writes its unique _ctor_ detail JSON & logcat, while a
            // successful unclaimed attempt closes silently without leaking into the next test.
            failPrepare = true
            assertFailsWith<IOException> { extension.resolveParameter(outerParamCtx, outerCtx) }
            failPrepare = false

            extension.resolveParameter(outerParamCtx, outerCtx)
            val test2Published = LinkedHashMap<String, String>()
            val test2Ctx =
                createChildContext(
                    parent = outerCtx,
                    uniqueId = TEST_ID,
                    stores = stores,
                    reportSink = test2Published,
                    testClass = OuterPerMethodCtorTestClass::class.java,
                )
            extension.resolveParameter(outerParamCtx, outerCtx)
            extension.beforeEach(test2Ctx)
            extension.afterEach(test2Ctx)
            extension.afterAll(outerCtx)

            val classDetailId = extension.detailIdOf(CLASS_ID)
            assertFalse(File(resultsDir, "suite-details/$classDetailId.json").exists())
            assertNull(classPublished[BackupReportKeys.DETAIL_PATH])
            val ctorJson =
                File(resultsDir, "suite-details").listFiles().orEmpty().single {
                    it.name.startsWith("${classDetailId}_ctor_") && it.extension == "json"
                }
            val ctorDetailId = ctorJson.nameWithoutExtension
            assertTrue(
                File(resultsDir, "suite-details/$ctorDetailId/logcat-$FAKE_SERIAL.txt").isFile
            )

            // Attempt 2 has only its own single Prepare step, and all collectors are unregistered.
            val test2DetailId = extension.detailIdOf(TEST_ID)
            val test2Record =
                TestDetailRecord.fromJson(
                    File(resultsDir, "suite-details/$test2DetailId.json").readText()
                )
            assertEquals(listOf("Prepare"), test2Record.steps.map { it.name })
            assertEquals(0, HostLogManager.activeCollectorCountForTesting())
        }

    @Test
    fun reportWriteFailureNeverFailsPassingTestOrDropsDeviceLogsNotice() =
        withResultsDir { resultsDir ->
            stubDeviceLogs("E Backup: failure logcat\n")
            val extension = BackupRestoreExtension { device.session }
            val classCtx = childContext(context, CLASS_ID)
            extension.beforeAll(classCtx)

            // 1. Passing test: make target JSON path a directory so Files.write throws IOException.
            val passId = "$CLASS_ID/[method:dummyMethod()]"
            val passDetailId = extension.detailIdOf(passId)
            Files.createDirectories(resultsDir.toPath().resolve("suite-details/$passDetailId.json"))
            val passCtx = childContext(classCtx, passId)
            extension.beforeEach(passCtx)
            extension.track(passCtx, controller())
            extension.afterEach(passCtx)

            // 2. Failing test: make target JSON path a directory; DeviceLogsNotice must still
            // stick.
            val failDetailId = extension.detailIdOf(TEST_ID)
            Files.createDirectories(resultsDir.toPath().resolve("suite-details/$failDetailId.json"))
            val failure = AssertionError("expected data to match")
            val failCtx = childContext(classCtx, TEST_ID, failure)
            extension.beforeEach(failCtx)
            extension.track(failCtx, controller())
            extension.afterEach(failCtx)
            extension.afterAll(classCtx)

            val savedLog = File(resultsDir, "suite-details/$failDetailId/logcat-$FAKE_SERIAL.txt")
            assertTrue(savedLog.isFile)
            assertEquals("Device logs saved to $savedLog", failure.suppressed.single().toString())
        }

    private class FakePrimaryMediaType(val value: String) {
        companion object {
            @JvmStatic fun parse(value: String): FakePrimaryMediaType = FakePrimaryMediaType(value)
        }
    }

    private class FakeJunit512MediaType(val value: String) {
        companion object {
            @JvmStatic
            fun parse(value: String): FakeJunit512MediaType = FakeJunit512MediaType(value)
        }
    }

    private class FakeJunit512Context(private val dir: Path) {
        var lastMediaType: FakeJunit512MediaType? = null
        var lastPrimaryMediaType: FakePrimaryMediaType? = null

        fun publishFile(
            name: String,
            mediaType: FakePrimaryMediaType,
            action: ThrowingConsumer<Path>,
        ) {
            lastPrimaryMediaType = mediaType
            action.accept(dir.resolve(name))
        }

        fun publishFile(
            name: String,
            mediaType: FakeJunit512MediaType,
            action: ThrowingConsumer<Path>,
        ) {
            lastMediaType = mediaType
            action.accept(dir.resolve(name))
        }
    }

    @BackupRestoreConfig(applicationId = "com.example.outer")
    @Isolation(IsolationPolicy.AUTOMATIC)
    private class AutomaticallyIsolatedConfiguredClass

    @BackupRestoreConfig(applicationId = PACKAGE)
    private class OuterPerMethodCtorTestClass(val outerController: BackupRestoreController) {
        inner class Inner(val innerController: BackupRestoreController)

        @TestInstance(TestInstance.Lifecycle.PER_CLASS) inner class NestedPerClass
    }

    /**
     * Returns a child context of [parent] with [uniqueId] and a store of its own, for a test or a
     * test class that failed with [failure], unless it is null.
     */
    private fun childContext(
        parent: ExtensionContext,
        uniqueId: String,
        failure: Throwable? = null,
        testClass: Class<*> = ConfiguredTestClass::class.java,
    ): ExtensionContext =
        createChildContext(
            parent = parent,
            uniqueId = uniqueId,
            stores = stores,
            failureProvider = { failure },
            testClass = testClass,
        )

    /** Runs [block] with a results directory that the system property of a test suite names. */
    private fun withResultsDir(block: (File) -> Unit) {
        val resultsDir = tempFolder.newFolder("results")
        withDeviceProperties(RESULTS_DIR to resultsDir.path) { block(resultsDir) }
    }

    /** Makes [device] print [logs] for `logcat`, and a time for `date`. */
    private fun stubDeviceLogs(logs: String) {
        device.onShell { command ->
            when {
                command == "date +%s" -> shellOutput("1700000000\n")
                command.startsWith("logcat ") -> shellOutput(logs)
                else -> null
            }
        }
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

    /** Returns the method [name] of this class, whose [count] parameters are all controllers. */
    private fun controllerMethod(name: String, count: Int): Method =
        BackupRestoreExtensionTest::class
            .java
            .getDeclaredMethod(name, *Array(count) { BackupRestoreController::class.java })

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

    private companion object {
        const val PACKAGE = "com.example.app"

        /** Serial numbers of online devices, in sorted order. */
        const val SERIAL_1 = "emulator-5554"
        const val SERIAL_2 = "emulator-5556"

        /** Prefix of the system properties that select devices. */
        const val DEVICE_PROPERTY = "androidx.test.backup.device"

        /** System property with the serial number of every device, or prefix of a binding. */
        const val SERIAL_PROPERTY = "$DEVICE_PROPERTY.serial"

        /** The package of the test APK, which AGP names after the app. */
        const val TEST_PACKAGE = "$PACKAGE.test"

        /** Test suite input with the application ID of the tested app. */
        const val TESTED_APPLICATION_ID = "com.android.junit.engine.tested.application.id"

        /** Unique ID of a test class, and of a test method in it. */
        const val CLASS_ID = "[engine:junit-jupiter]/[class:com.example.BackupTest]"
        const val TEST_ID =
            "$CLASS_ID/[method:restoresData(androidx.test.backup.host.BackupRestoreController)]"

        /** The APKs of a test suite: the app, then the test APK. */
        val APKS = listOf(File("app-debug.apk"), File("app-debug-backupTest.apk"))

        /**
         * Returns the [ExtensionContext] of [testClass] itself, which has no test method, like the
         * one in which JUnit resolves the parameters of its constructor and `@BeforeAll` methods.
         */
        fun classContext(testClass: Class<*>): ExtensionContext {
            val context = mock(ExtensionContext::class.java)
            `when`(context.testClass).thenReturn(Optional.of(testClass))
            `when`(context.requiredTestClass).thenReturn(testClass)
            `when`(context.testMethod).thenReturn(Optional.empty())
            `when`(context.requiredTestMethod)
                .thenThrow(
                    PreconditionViolationException(
                        "Illegal state: required test method is not present in the current " +
                            "ExtensionContext"
                    )
                )
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

/** Test suite input with the directory for the results of the tests. */
internal const val RESULTS_DIR = "com.android.junit.engine.results.dir"

/** Stores values in memory and closes [CloseableResource] entries in reverse insertion order. */
internal class FakeStore : ExtensionContext.Store {
    private val values = LinkedHashMap<Any, Any?>()

    /** Stored values in insertion order. */
    val storedValues: Collection<Any?>
        get() = values.values

    /** Closes stored [CloseableResource]s in reverse insertion order. */
    fun closeAll() {
        values.values.reversed().filterIsInstance<CloseableResource>().forEach { it.close() }
    }

    override fun get(key: Any): Any? = values[key]

    override fun <V> get(key: Any, requiredType: Class<V>): V? = requiredType.cast(values[key])

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

    override fun <V> remove(key: Any, requiredType: Class<V>): V? =
        requiredType.cast(values.remove(key))
}

/** Returns a mocked root [ExtensionContext] backed by [store]. */
internal fun contextWithRootStore(store: ExtensionContext.Store): ExtensionContext {
    val context = mock(ExtensionContext::class.java)
    `when`(context.root).thenReturn(context)
    `when`(context.uniqueId).thenReturn("[engine:junit-jupiter]")
    `when`(context.displayName).thenReturn("JUnit Jupiter")
    `when`(context.getStore(any())).thenReturn(store)
    `when`(context.testClass).thenReturn(Optional.empty())
    `when`(context.testMethod).thenReturn(Optional.empty())
    `when`(context.testInstanceLifecycle).thenReturn(Optional.empty())
    `when`(context.executionException).thenReturn(Optional.empty())
    return context
}

/**
 * Returns a mocked child [ExtensionContext] of [parent] with [testClass] and
 * [ExtensionContext.testMethod] stubbed from [uniqueId].
 */
internal fun createChildContext(
    parent: ExtensionContext,
    uniqueId: String,
    stores: MutableList<FakeStore>,
    failureProvider: () -> Throwable? = { null },
    reportSink: MutableMap<String, String>? = null,
    testClass: Class<*>? = null,
    lifecycle: TestInstance.Lifecycle = TestInstance.Lifecycle.PER_METHOD,
): ExtensionContext {
    val root = parent.root
    val store = FakeStore().also { stores += it }
    val ctx = mock(ExtensionContext::class.java)
    `when`(ctx.uniqueId).thenReturn(uniqueId)
    `when`(ctx.parent).thenReturn(Optional.of(parent))
    `when`(ctx.root).thenReturn(root)
    `when`(ctx.getStore(any())).thenReturn(store)
    `when`(ctx.testClass).thenReturn(Optional.ofNullable(testClass))
    if (testClass != null) {
        `when`(ctx.requiredTestClass).thenReturn(testClass)
    }
    val methodName = METHOD_SEGMENT_REGEX.find(uniqueId)?.groupValues?.get(1)
    if (methodName != null) {
        val method =
            testClass?.declaredMethods?.firstOrNull { it.name == methodName }
                ?: SharedStubTestMethods::class.java.declaredMethods.firstOrNull {
                    it.name == methodName
                }
                ?: error(
                    "Declare stub method '$methodName' in SharedStubTestMethods or ${testClass?.name}"
                )
        `when`(ctx.testMethod).thenReturn(Optional.of(method))
        `when`(ctx.requiredTestMethod).thenReturn(method)
    } else {
        `when`(ctx.testMethod).thenReturn(Optional.empty())
    }
    `when`(ctx.displayName).thenReturn(methodName ?: testClass?.simpleName ?: uniqueId)
    `when`(ctx.testInstanceLifecycle).thenReturn(Optional.of(lifecycle))
    `when`(ctx.executionException).thenAnswer { Optional.ofNullable(failureProvider()) }
    if (reportSink != null) {
        doAnswer { inv ->
                reportSink[inv.getArgument(0)] = inv.getArgument(1)
                null
            }
            .`when`(ctx)
            .publishReportEntry(anyString(), anyString())
    }
    return ctx
}

/**
 * Runs [block] with only [properties] set among backup and suite system properties.
 *
 * Restores any previously set system properties once [block] completes.
 */
internal fun withDeviceProperties(vararg properties: Pair<String, String>, block: () -> Unit) {
    val prefixes =
        listOf("androidx.test.backup.", "com.android.junit.engine.", "com.android.agp.test.")
    val previous =
        System.getProperties()
            .stringPropertyNames()
            .filter { key -> prefixes.any { key.startsWith(it) } }
            .associateWith { System.getProperty(it) }
    try {
        previous.keys.forEach { System.clearProperty(it) }
        properties.forEach { (name, value) -> System.setProperty(name, value) }
        block()
    } finally {
        System.getProperties()
            .stringPropertyNames()
            .filter { key -> prefixes.any { key.startsWith(it) } }
            .forEach { System.clearProperty(it) }
        properties.forEach { (name, _) -> System.clearProperty(name) }
        previous.forEach { (name, value) -> System.setProperty(name, value) }
    }
}

private val METHOD_SEGMENT_REGEX = Regex("""\[(?:method|test-template):([^(]+)\(""")

@Suppress("unused")
private object SharedStubTestMethods {
    fun restoresData() {}

    fun dummyMethod() {}

    fun testSessionTokenRestored() {}

    fun testDraftsRestored() {}

    fun testOne() {}

    fun testTwo() {}

    fun setupFails() {}

    fun bmgrInitFails() {}

    fun backupRejected() {}

    fun actionFailure() {}

    fun emptyStorages() {}

    fun nestedTest() {}

    fun passingAfterSharedBackup() {}

    fun postRestoreCleanupFails() {}

    fun midFlowUninstallFails() {}
}
