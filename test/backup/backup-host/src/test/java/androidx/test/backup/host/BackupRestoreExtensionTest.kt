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
import java.lang.reflect.Method
import java.nio.file.Path
import java.util.Optional
import java.util.Properties
import java.util.function.Function
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.platform.commons.PreconditionViolationException
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

    /**
     * An [ExtensionContext.Store] that closes its [CloseableResource]s on [closeAll], as JUnit does
     * when the context that owns the store ends.
     */
    private class FakeStore : ExtensionContext.Store {
        private val values = LinkedHashMap<Any, Any?>()

        /** The values in this store, in order of insertion. */
        val storedValues: Collection<Any?>
            get() = values.values

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

        /** Serial numbers of online devices, in sorted order. */
        const val SERIAL_1 = "emulator-5554"
        const val SERIAL_2 = "emulator-5556"

        /** Prefix of the system properties that select devices. */
        const val DEVICE_PROPERTY = "androidx.test.backup.device"

        /** System property with the serial number of every device, or prefix of a binding. */
        const val SERIAL_PROPERTY = "$DEVICE_PROPERTY.serial"

        /**
         * Runs [block] with only the device selection system [properties], and then restores the
         * device selection properties that were set before, such as from the command line.
         */
        fun withDeviceProperties(vararg properties: Pair<String, String>, block: () -> Unit) {
            val previous =
                System.getProperties()
                    .stringPropertyNames()
                    .filter { it.startsWith(DEVICE_PROPERTY) }
                    .associateWith { System.getProperty(it) }
            try {
                previous.keys.forEach { System.clearProperty(it) }
                properties.forEach { (name, value) -> System.setProperty(name, value) }
                block()
            } finally {
                properties.forEach { (name, _) -> System.clearProperty(name) }
                previous.forEach { (name, value) -> System.setProperty(name, value) }
            }
        }

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
