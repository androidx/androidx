/*
 * Copyright 2021 The Android Open Source Project
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
package androidx.wear.watchface

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.ComplicationType.LONG_TEXT
import androidx.wear.watchface.complications.data.ComplicationType.MONOCHROMATIC_IMAGE
import androidx.wear.watchface.complications.data.ComplicationType.SHORT_TEXT
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

const val TIME_OUT_MILLIS = 500L

@RunWith(AndroidJUnit4::class)
@MediumTest
public class ComplicationHelperActivityTest {
    private val mainThreadHandler = Handler(Looper.getMainLooper())

    private val scenarios =
        mapOf(
            ComplicationHelperActivity.PERMISSION_REQUEST_CODE_PROVIDER_CHOOSER to createIntent(),
            ComplicationHelperActivity.PERMISSION_REQUEST_CODE_REQUEST_ONLY to
                createPermissionOnlyIntent()
        )

    @Test
    public fun createProviderChooserHelperIntent_action() {
        assertThat(createIntent().action)
            .isEqualTo(ComplicationHelperActivity.ACTION_START_PROVIDER_CHOOSER)
    }

    @Test
    public fun createProviderChooserHelperIntent_component() {
        assertThat(createIntent().component)
            .isEqualTo(ComponentName(context, ComplicationHelperActivity::class.java))
    }

    @Test
    public fun createProviderChooserHelperIntent_watchFaceComponentName() {
        ComponentName("package-name", "watch-face-service-name").let {
            assertThat(createIntent(watchFaceComponentName = it).watchFaceComponentName)
                .isEqualTo(it)
        }
        ComponentName(context, "service-name").let {
            assertThat(createIntent(watchFaceComponentName = it).watchFaceComponentName)
                .isEqualTo(it)
        }
    }

    @Test
    public fun createProviderChooserHelperIntent_complicationSlotId() {
        assertThat(createIntent(complicationSlotId = -1).complicationSlotId).isEqualTo(-1)
        assertThat(createIntent(complicationSlotId = 1234).complicationSlotId).isEqualTo(1234)
        assertThat(createIntent(complicationSlotId = 30000).complicationSlotId).isEqualTo(30000)
    }

    @Test
    public fun createProviderChooserHelperIntent_supportedTypes() {
        arrayOf<ComplicationType>().let {
            assertThat(createIntent(supportedTypes = it).supportedTypes).isEqualTo(it)
        }
        arrayOf(LONG_TEXT).let {
            assertThat(createIntent(supportedTypes = it).supportedTypes).isEqualTo(it)
        }
        arrayOf(SHORT_TEXT, LONG_TEXT, MONOCHROMATIC_IMAGE).let {
            assertThat(createIntent(supportedTypes = it).supportedTypes).isEqualTo(it)
        }
    }

    @Test
    public fun instanceId() {
        assertThat(
                createIntent(instanceId = "ID-1")
                    .getStringExtra(ComplicationDataSourceChooserIntent.EXTRA_WATCHFACE_INSTANCE_ID)
            )
            .isEqualTo("ID-1")
    }

    fun runOnMainThread(task: () -> Unit) {
        val latch = CountDownLatch(1)
        mainThreadHandler.post {
            task()
            latch.countDown()
        }
        latch.await(TIME_OUT_MILLIS, TimeUnit.MILLISECONDS)
    }

    @Test
    public fun complicationRationaleDialogFragment_shown_if_needed() {
        runOnMainThread {
            scenarios.forEach { (_, intent) ->
                val helper = ComplicationHelperActivity()
                helper.intent = intent
                helper.mDelegate =
                    mock() { on { shouldShowRequestPermissionRationale() } doReturn true }

                helper.start(true)
                verify(helper.mDelegate).launchComplicationRationaleActivity()
            }
        }
    }

    @Test
    public fun complicationRationaleDialogFragment_not_shown_if_not_needed() {
        runOnMainThread {
            scenarios.forEach { (_, intent) ->
                val helper = ComplicationHelperActivity()
                helper.intent = intent
                helper.mDelegate =
                    mock() { on { shouldShowRequestPermissionRationale() } doReturn false }

                helper.start(true)
                verify(helper.mDelegate, never()).launchComplicationRationaleActivity()
            }
        }
    }

    @Test
    public fun complicationRationaleDialogFragment_not_shown_if_not_requested() {
        runOnMainThread {
            scenarios.forEach { (_, intent) ->
                val helper = ComplicationHelperActivity()
                helper.intent = intent
                helper.mDelegate =
                    mock() { on { shouldShowRequestPermissionRationale() } doReturn true }

                helper.start(false)
                verify(helper.mDelegate, never()).launchComplicationRationaleActivity()
            }
        }
    }

    @Test
    public fun permissions_requested_if_needed() {
        runOnMainThread {
            scenarios.forEach { (requestId, intent) ->
                val helper = ComplicationHelperActivity()
                helper.intent = intent
                helper.mDelegate =
                    mock() {
                        on { shouldShowRequestPermissionRationale() } doReturn false
                        on { checkPermission() } doReturn false
                    }

                helper.start(true)
                verify(helper.mDelegate).requestPermissions(requestId)
            }
        }
    }

    @Test
    public fun permissions_not_requested_if_not_needed() {
        runOnMainThread {
            scenarios.forEach { (requestId, intent) ->
                val helper = ComplicationHelperActivity()
                helper.intent = intent
                helper.mDelegate =
                    mock() {
                        on { shouldShowRequestPermissionRationale() } doReturn false
                        on { checkPermission() } doReturn true
                    }

                helper.start(true)
                verify(helper.mDelegate, never()).requestPermissions(requestId)
            }
        }
    }

    @Test
    public fun onRequestPermissionsResult_permission_granted() {
        runOnMainThread {
            val helper = ComplicationHelperActivity()
            helper.intent = createIntent()
            helper.mDelegate = mock()

            helper.onRequestPermissionsResult(
                ComplicationHelperActivity.PERMISSION_REQUEST_CODE_PROVIDER_CHOOSER,
                emptyArray(),
                intArrayOf(PackageManager.PERMISSION_GRANTED)
            )

            verify(helper.mDelegate).startComplicationDataSourceChooser()
            verify(helper.mDelegate).requestUpdateAll()
        }
    }

    @Test
    public fun onRequestPermissionsResult_permission_only_permission_granted() {
        runOnMainThread {
            val helper = ComplicationHelperActivity()
            helper.intent = createIntent()
            helper.mDelegate = mock()

            helper.onRequestPermissionsResult(
                ComplicationHelperActivity.PERMISSION_REQUEST_CODE_REQUEST_ONLY,
                emptyArray(),
                intArrayOf(PackageManager.PERMISSION_GRANTED)
            )

            verify(helper.mDelegate, never()).startComplicationDataSourceChooser()
            verify(helper.mDelegate).requestUpdateAll()
        }
    }

    @Test
    public fun complicationDeniedActivity_launched_if_permission_denied() {
        runOnMainThread {
            scenarios.forEach { (requestId, intent) ->
                val helper = ComplicationHelperActivity()
                helper.intent = intent
                helper.mDelegate = mock()

                helper.onRequestPermissionsResult(
                    requestId,
                    emptyArray(),
                    intArrayOf(PackageManager.PERMISSION_DENIED)
                )

                verify(helper.mDelegate).launchComplicationDeniedActivity()
            }
        }
    }

    @Test
    public fun complicationDeniedActivity_not_launched_if_permission_denied_with_dont_show() {
        val deniedScenarios =
            mapOf(
                ComplicationHelperActivity
                    .PERMISSION_REQUEST_CODE_PROVIDER_CHOOSER_NO_DENIED_INTENT to createIntent(),
                ComplicationHelperActivity.PERMISSION_REQUEST_CODE_REQUEST_ONLY_NO_DENIED_INTENT to
                    createPermissionOnlyIntent()
            )
        runOnMainThread {
            deniedScenarios.forEach { (requestId, intent) ->
                val helper = ComplicationHelperActivity()
                helper.intent = intent
                helper.mDelegate = mock()

                helper.onRequestPermissionsResult(
                    requestId,
                    emptyArray(),
                    intArrayOf(PackageManager.PERMISSION_DENIED)
                )

                verify(helper.mDelegate, never()).launchComplicationDeniedActivity()
            }
        }
    }

    /** Creates an intent with default xml for unspecified parameters. */
    private fun createIntent(
        watchFaceComponentName: ComponentName = defaultWatchFaceComponentName,
        complicationSlotId: Int = defaultComplicationSlotId,
        instanceId: String? = null,
        vararg supportedTypes: ComplicationType = defaultSupportedTypes,
        complicationDeniedIntent: Intent? = Intent(),
        complicationRationalIntent: Intent? = Intent()
    ) =
        ComplicationHelperActivity.createComplicationDataSourceChooserHelperIntent(
            context,
            watchFaceComponentName,
            complicationSlotId,
            supportedTypes.asList(),
            instanceId,
            complicationDeniedIntent,
            complicationRationalIntent
        )

    private fun createPermissionOnlyIntent(
        watchFaceComponentName: ComponentName = defaultWatchFaceComponentName,
        complicationDeniedIntent: Intent? = Intent(),
        complicationRationalIntent: Intent? = Intent()
    ) =
        ComplicationHelperActivity.createPermissionRequestHelperIntent(
            context,
            watchFaceComponentName,
            complicationDeniedIntent,
            complicationRationalIntent
        )

    private companion object {
        /** The context to be used in the various tests. */
        private val context = ApplicationProvider.getApplicationContext<Context>()

        /** The default watch face component name used in the test. */
        private val defaultWatchFaceComponentName = ComponentName("test.package", "test.class")

        /** The default complication slot ID used in the test. */
        private const val defaultComplicationSlotId = 1234

        /** The default supported types used in the test. */
        private val defaultSupportedTypes = arrayOf(SHORT_TEXT, LONG_TEXT)
    }
}

/** The watch face component name encoded in the intent. */
@Suppress("DEPRECATION")
private val Intent.watchFaceComponentName
    get() =
        getParcelableExtra<ComponentName>(
            ComplicationDataSourceChooserIntent.EXTRA_WATCH_FACE_COMPONENT_NAME
        )

/** The complication ID encoded in the intent. */
private val Intent.complicationSlotId
    get() = getIntExtra(ComplicationDataSourceChooserIntent.EXTRA_COMPLICATION_ID, -1)

/** The support types encoded in the intent. */
private val Intent.supportedTypes
    get() =
        ComplicationType.fromWireTypes(
            getIntArrayExtra(ComplicationDataSourceChooserIntent.EXTRA_SUPPORTED_TYPES)!!
        )
