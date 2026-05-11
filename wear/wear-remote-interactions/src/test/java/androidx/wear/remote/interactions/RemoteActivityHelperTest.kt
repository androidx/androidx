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

package androidx.wear.remote.interactions

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Resources.NotFoundException
import android.net.Uri
import android.os.Build.VERSION_CODES
import android.os.Looper
import android.os.OutcomeReceiver
import android.os.ResultReceiver
import androidx.concurrent.futures.await
import androidx.test.core.app.ApplicationProvider
import androidx.wear.remote.interactions.RemoteActivityHelper.Companion.ACTION_REMOTE_INTENT
import androidx.wear.remote.interactions.RemoteActivityHelper.Companion.DEFAULT_PACKAGE
import androidx.wear.remote.interactions.RemoteActivityHelper.Companion.RESULT_FAILED
import androidx.wear.remote.interactions.RemoteActivityHelper.Companion.RESULT_OK
import androidx.wear.remote.interactions.RemoteActivityHelper.Companion.getRemoteIntentResultReceiver
import androidx.wear.remote.interactions.RemoteActivityHelper.Companion.getTargetIntent
import androidx.wear.remote.interactions.RemoteActivityHelper.Companion.getTargetNodeId
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.NodeClient
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import java.util.function.Consumer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implements

@RunWith(WearRemoteInteractionsTestRunner::class)
@org.robolectric.annotation.Config(
    shadows = [RemoteActivityHelperTest.ActualResultReceiver::class],
    sdk = [org.robolectric.annotation.Config.TARGET_SDK],
)
class RemoteActivityHelperTest {
    @Implements(ResultReceiver::class)
    class ActualResultReceiver {
        // Robolectric stubs out ResultReceiver. The stubbed version just calls onReceiveResult
        // from send(). Problem is, the ResultReceiver in the BroadcastReceiver below has already
        // been parceled (and back), so it doesn't have an implementation of onReceiveResult; it
        // instead wants to call the original version over the embedded IBinder.
        //
        // To fix, this class replaces that shadow with a version that just falls back to the
        // proper Android implementation.
    }

    class TestBroadcastReceiver(private val result: Int) : BroadcastReceiver() {
        companion object {
            // If this is set, result receiver will send [RESULT_OK] and {RESULT_FAILED]
            // alternatively.
            const val DIFFERENT_RESULT = -1
        }

        private var altResult = RESULT_OK

        override fun onReceive(context: Context?, intent: Intent?) {
            val resultReceiver = intent?.let { getRemoteIntentResultReceiver(it) }
            if (result == DIFFERENT_RESULT) {
                altResult = (altResult + 1) % 2
                resultReceiver?.send(result, null)
            } else {
                resultReceiver?.send(result, null)
            }
        }
    }

    private val testPackageName = "package.name"
    private val testPackageName2 = "package.name2"
    private val testNodeId = "Test Node ID"
    private val testNodeId2 = "Test Node ID2"
    private val testUri = Uri.parse("market://details?id=com.google.android.wearable.app")
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val testExtraIntent =
        Intent(Intent.ACTION_VIEW).addCategory(Intent.CATEGORY_BROWSABLE).setData(testUri)
    private lateinit var mRemoteActivityHelper: RemoteActivityHelper

    @Mock private var mockNodeClient: NodeClient = mock()
    @Mock private val mockTestNode: Node = mock()
    @Mock private val mockTestNode2: Node = mock()
    private val remoteInteractionsManager: IRemoteInteractionsManager = mock()

    @Before
    fun setUp() {
        mRemoteActivityHelper = RemoteActivityHelper(context, SyncExecutor())
        mRemoteActivityHelper.nodeClient = mockNodeClient
        mRemoteActivityHelper.remoteInteractionsManager = remoteInteractionsManager
        shadowOf(context as Application)
            .grantPermissions(RemoteActivityHelper.PERMISSION_SEND_CONTINUE_ACTIVITY_ON_PHONE)
    }

    private fun setSystemFeatureWatch(isWatch: Boolean) {
        val shadowPackageManager = shadowOf(context.packageManager)
        shadowPackageManager!!.setSystemFeature(
            RemoteInteractionsUtil.SYSTEM_FEATURE_WATCH,
            isWatch,
        )
    }

    private fun nodeClientReturnFakePackageName(nodeId: String, packageName: String?) {
        Mockito.`when`(mockNodeClient.getCompanionPackageForNode(nodeId))
            .thenReturn(Tasks.forResult(packageName))
    }

    private fun nodeClientReturnFakeConnectedNodes() {
        Mockito.`when`(mockTestNode.id).thenReturn(testNodeId)
        Mockito.`when`(mockTestNode2.id).thenReturn(testNodeId2)
        Mockito.`when`(mockNodeClient.connectedNodes)
            .thenReturn(Tasks.forResult(listOf(mockTestNode, mockTestNode2)))
    }

    @Test
    fun testStartRemoteActivityLegacy_notActionViewIntent() {
        assertThrows(ExecutionException::class.java) {
            mRemoteActivityHelper.startRemoteActivityLegacy(Intent(), testNodeId).get()
        }
    }

    @Test
    fun testStartRemoteActivityLegacy_dataNull() {
        assertThrows(ExecutionException::class.java) {
            mRemoteActivityHelper
                .startRemoteActivityLegacy(Intent(Intent.ACTION_VIEW), testNodeId)
                .get()
        }
    }

    @Test
    fun testStartRemoteActivityLegacy_notCategoryBrowsable() {
        assertThrows(ExecutionException::class.java) {
            mRemoteActivityHelper
                .startRemoteActivityLegacy(
                    Intent(Intent.ACTION_VIEW).setData(Uri.EMPTY),
                    testNodeId,
                )
                .get()
        }
    }

    @Test
    fun testStartRemoteActivityLegacy_watch() {
        setSystemFeatureWatch(true)
        val receiver = TestBroadcastReceiver(RESULT_OK)
        context.registerReceiver(receiver, IntentFilter(ACTION_REMOTE_INTENT))

        try {
            val future =
                mRemoteActivityHelper.startRemoteActivityLegacy(testExtraIntent, testNodeId)
            shadowOf(Looper.getMainLooper()).idle()
            assertTrue(future.isDone)
            future.get()
        } catch (e: Exception) {
            fail("startRemoteActivity.get() shouldn't throw exception in this case.")
        } finally {
            context.unregisterReceiver(receiver)
        }

        val broadcastIntents =
            shadowOf(ApplicationProvider.getApplicationContext() as Application).broadcastIntents
        assertEquals(1, broadcastIntents.size)
        val intent = broadcastIntents[0]
        assertEquals(testExtraIntent, getTargetIntent(intent))
        assertEquals(testNodeId, getTargetNodeId(intent))
        assertEquals(DEFAULT_PACKAGE, intent.`package`)
    }

    @Test
    fun testStartRemoteActivityLegacy_watchFailed() {
        setSystemFeatureWatch(true)
        val receiver = TestBroadcastReceiver(RESULT_FAILED)
        context.registerReceiver(receiver, IntentFilter(ACTION_REMOTE_INTENT))

        try {
            val future =
                mRemoteActivityHelper.startRemoteActivityLegacy(testExtraIntent, testNodeId)
            shadowOf(Looper.getMainLooper()).idle()
            assertTrue(future.isDone)
            assertThrows(ExecutionException::class.java) { future.get() }
        } finally {
            context.unregisterReceiver(receiver)
        }
    }

    @Test
    fun testStartRemoteActivityLegacy_phoneWithPackageName() {
        setSystemFeatureWatch(false)
        nodeClientReturnFakePackageName(testNodeId, testPackageName)
        val receiver = TestBroadcastReceiver(RESULT_OK)
        context.registerReceiver(receiver, IntentFilter(ACTION_REMOTE_INTENT))

        try {
            val future =
                mRemoteActivityHelper.startRemoteActivityLegacy(testExtraIntent, testNodeId)
            shadowOf(Looper.getMainLooper()).idle()
            assertTrue(future.isDone)
            future.get()
        } catch (e: Exception) {
            fail("startRemoteActivity.get() shouldn't throw exception in this case.")
        } finally {
            context.unregisterReceiver(receiver)
        }

        val broadcastIntents =
            shadowOf(ApplicationProvider.getApplicationContext() as Application).broadcastIntents
        assertEquals(1, broadcastIntents.size)
        assertRemoteIntentEqual(testExtraIntent, testNodeId, testPackageName, broadcastIntents[0])
    }

    @Test
    fun testStartRemoteActivityLegacy_phoneWithoutPackageName() {
        setSystemFeatureWatch(false)
        nodeClientReturnFakePackageName(testNodeId, null)
        val receiver = TestBroadcastReceiver(RESULT_OK)
        context.registerReceiver(receiver, IntentFilter(ACTION_REMOTE_INTENT))

        try {
            val future =
                mRemoteActivityHelper.startRemoteActivityLegacy(testExtraIntent, testNodeId)
            shadowOf(Looper.getMainLooper()).idle()
            assertTrue(future.isDone)
            future.get()
        } catch (e: Exception) {
            fail("startRemoteActivity.get() shouldn't throw exception in this case.")
        } finally {
            context.unregisterReceiver(receiver)
        }

        val broadcastIntents =
            shadowOf(ApplicationProvider.getApplicationContext() as Application).broadcastIntents
        assertEquals(1, broadcastIntents.size)
        val intent = broadcastIntents[0]
        assertEquals(testExtraIntent, getTargetIntent(intent))
        assertEquals(testNodeId, getTargetNodeId(intent))
        assertEquals(DEFAULT_PACKAGE, intent.`package`)
    }

    @Test
    fun testStartRemoteActivityLegacy_phoneWithoutNodeId_allOk() {
        setSystemFeatureWatch(false)
        nodeClientReturnFakeConnectedNodes()
        nodeClientReturnFakePackageName(testNodeId, testPackageName)
        nodeClientReturnFakePackageName(testNodeId2, testPackageName2)
        val receiver = TestBroadcastReceiver(RESULT_OK)
        context.registerReceiver(receiver, IntentFilter(ACTION_REMOTE_INTENT))

        try {
            val future =
                mRemoteActivityHelper.startRemoteActivityLegacy(
                    testExtraIntent,
                    targetNodeId = null,
                )
            shadowOf(Looper.getMainLooper()).idle()
            assertTrue(future.isDone)
            future.get()
        } catch (e: Exception) {
            fail("startRemoteActivity.get() shouldn't throw exception in this case.")
        } finally {
            context.unregisterReceiver(receiver)
        }

        shadowOf(Looper.getMainLooper()).idle()
        val broadcastIntents =
            shadowOf(ApplicationProvider.getApplicationContext() as Application).broadcastIntents
        assertEquals(2, broadcastIntents.size)

        assertRemoteIntentEqual(testExtraIntent, testNodeId, testPackageName, broadcastIntents[0])
        assertRemoteIntentEqual(testExtraIntent, testNodeId2, testPackageName2, broadcastIntents[1])
    }

    @Test
    fun testStartRemoteActivityLegacy_phoneWithoutNodeId_oneOkOneFail() {
        setSystemFeatureWatch(false)
        nodeClientReturnFakeConnectedNodes()
        nodeClientReturnFakePackageName(testNodeId, testPackageName)
        nodeClientReturnFakePackageName(testNodeId2, testPackageName2)
        val receiver = TestBroadcastReceiver(TestBroadcastReceiver.DIFFERENT_RESULT)
        context.registerReceiver(receiver, IntentFilter(ACTION_REMOTE_INTENT))

        assertThrows(ExecutionException::class.java) {
            val future =
                mRemoteActivityHelper.startRemoteActivityLegacy(
                    testExtraIntent,
                    targetNodeId = null,
                )
            shadowOf(Looper.getMainLooper()).idle()
            assertTrue(future.isDone)
            future.get()
        }
        context.unregisterReceiver(receiver)

        shadowOf(Looper.getMainLooper()).idle()
        val broadcastIntents =
            shadowOf(ApplicationProvider.getApplicationContext() as Application).broadcastIntents
        assertEquals(2, broadcastIntents.size)
        assertRemoteIntentEqual(testExtraIntent, testNodeId, testPackageName, broadcastIntents[0])
        assertRemoteIntentEqual(testExtraIntent, testNodeId2, testPackageName2, broadcastIntents[1])
    }

    @Test
    fun testStartRemoteActivityLegacy_phoneWithoutNodeId_allFail() {
        setSystemFeatureWatch(false)
        nodeClientReturnFakeConnectedNodes()
        nodeClientReturnFakePackageName(testNodeId, testPackageName)
        nodeClientReturnFakePackageName(testNodeId2, testPackageName2)
        val receiver = TestBroadcastReceiver(RESULT_FAILED)
        context.registerReceiver(receiver, IntentFilter(ACTION_REMOTE_INTENT))

        assertThrows(ExecutionException::class.java) {
            val future =
                mRemoteActivityHelper.startRemoteActivityLegacy(
                    testExtraIntent,
                    targetNodeId = null,
                )
            shadowOf(Looper.getMainLooper()).idle()
            assertTrue(future.isDone)
            future.get()
        }
        context.unregisterReceiver(receiver)

        shadowOf(Looper.getMainLooper()).idle()
        val broadcastIntents =
            shadowOf(ApplicationProvider.getApplicationContext() as Application).broadcastIntents
        assertEquals(2, broadcastIntents.size)
        assertRemoteIntentEqual(testExtraIntent, testNodeId, testPackageName, broadcastIntents[0])
        assertRemoteIntentEqual(testExtraIntent, testNodeId2, testPackageName2, broadcastIntents[1])
    }

    private fun assertRemoteIntentEqual(
        expectedExtraIntent: Intent,
        expectedNodeId: String,
        expectedPackageName: String,
        actualIntent: Intent,
    ) {
        assertEquals(expectedExtraIntent, getTargetIntent(actualIntent))
        assertEquals(expectedNodeId, getTargetNodeId(actualIntent))
        assertEquals(expectedPackageName, actualIntent.`package`)
    }

    @Test
    fun testActionRemoteIntentWithExtras() {
        val intent = mRemoteActivityHelper.createIntent(testExtraIntent, null, testNodeId)

        assertTrue(intent.action == ACTION_REMOTE_INTENT)
        assertEquals(testExtraIntent, getTargetIntent(intent))
        assertEquals(testNodeId, getTargetNodeId(intent))
    }

    @Test
    fun testStartRemoteActivityLegacy_getCompanionPackageErrorPropagates() {
        setSystemFeatureWatch(false)
        val receiver = TestBroadcastReceiver(RESULT_OK)
        context.registerReceiver(receiver, IntentFilter(ACTION_REMOTE_INTENT))

        Mockito.`when`(mockNodeClient.getCompanionPackageForNode(any()))
            .thenReturn(Tasks.forException(IllegalStateException("Error")))

        val future = mRemoteActivityHelper.startRemoteActivityLegacy(testExtraIntent, testNodeId)
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(future.isDone)

        val actualException = assertThrows(ExecutionException::class.java) { future.get() }

        assertTrue(actualException.cause is IllegalStateException)
    }

    @Test
    fun testStartRemoteActivityLegacy_getConnectedNodesErrorPropagates() {
        setSystemFeatureWatch(false)
        val receiver = TestBroadcastReceiver(RESULT_OK)
        context.registerReceiver(receiver, IntentFilter(ACTION_REMOTE_INTENT))

        Mockito.`when`(mockNodeClient.connectedNodes)
            .thenReturn(Tasks.forException(IllegalStateException("Error")))

        val future = mRemoteActivityHelper.startRemoteActivityLegacy(testExtraIntent)
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(future.isDone)

        val actualException = assertThrows(ExecutionException::class.java) { future.get() }

        assertTrue(actualException.cause is IllegalStateException)
    }

    @Test
    fun testStartRemoteActivityLegacy_noNodeId_getCompanionPackageErrorPropagates() {
        setSystemFeatureWatch(false)
        nodeClientReturnFakeConnectedNodes()
        val receiver = TestBroadcastReceiver(RESULT_OK)
        context.registerReceiver(receiver, IntentFilter(ACTION_REMOTE_INTENT))

        Mockito.`when`(mockNodeClient.getCompanionPackageForNode(any()))
            .thenReturn(Tasks.forException(IllegalStateException("Error")))

        val future = mRemoteActivityHelper.startRemoteActivityLegacy(testExtraIntent)
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(future.isDone)

        val actualException = assertThrows(ExecutionException::class.java) { future.get() }

        assertTrue(actualException.cause is IllegalStateException)
    }

    @Test
    fun testStartRemoteActivityLegacy_nodeNotFound() {
        setSystemFeatureWatch(false)
        Mockito.`when`(mockNodeClient.getCompanionPackageForNode(any()))
            .thenReturn(Tasks.forResult(""))

        val future = mRemoteActivityHelper.startRemoteActivityLegacy(testExtraIntent, testNodeId)
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(future.isDone)

        val actualException = assertThrows(ExecutionException::class.java) { future.get() }

        assertTrue(actualException.cause is NotFoundException)
    }

    @Test
    fun testStartRemoteActivityLegacy_noNodes() {
        setSystemFeatureWatch(false)
        Mockito.`when`(mockNodeClient.connectedNodes).thenReturn(Tasks.forResult(listOf()))

        val future = mRemoteActivityHelper.startRemoteActivityLegacy(testExtraIntent)
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(future.isDone)

        val actualException = assertThrows(ExecutionException::class.java) { future.get() }

        assertTrue(actualException.cause is NotFoundException)
    }

    @Test
    fun testStartRemoteActivity_isNotWatch_startRemoteActivityNotExecuted() {
        setSystemFeatureWatch(false)

        mRemoteActivityHelper.startRemoteActivity(testExtraIntent)
        shadowOf(Looper.getMainLooper()).idle()

        verify(remoteInteractionsManager).isWearSdkApiStartRemoteActivitySupported
        verifyNoMoreInteractions(remoteInteractionsManager)
    }

    @Test
    fun testStartRemoteActivity_isWatchAndUseWearSdkImplTrue_startRemoteActivityExecuted() {
        whenever(remoteInteractionsManager.isWearSdkApiStartRemoteActivitySupported)
            .thenReturn(true)

        mRemoteActivityHelper.startRemoteActivity(testExtraIntent)
        shadowOf(Looper.getMainLooper()).idle()

        verify(remoteInteractionsManager).startRemoteActivity(any(), any(), any(), any())
    }

    @Test
    fun testStartRemoteActivity_isWatchAndUseWearSdkImplFalse_startRemoteActivityNotExecuted() {
        whenever(remoteInteractionsManager.isWearSdkApiStartRemoteActivitySupported)
            .thenReturn(false)

        mRemoteActivityHelper.startRemoteActivity(testExtraIntent)
        shadowOf(Looper.getMainLooper()).idle()

        verify(remoteInteractionsManager).isWearSdkApiStartRemoteActivitySupported
        verifyNoMoreInteractions(remoteInteractionsManager)
    }

    @Test
    @Config(minSdk = VERSION_CODES.TIRAMISU)
    fun testStartRemoteActivity_await_startRemoteActivityExecuted() = runTest {
        whenever(remoteInteractionsManager.isWearSdkApiStartRemoteActivitySupported)
            .thenReturn(true)

        val result = mRemoteActivityHelper.startRemoteActivity(testExtraIntent)

        val captor = argumentCaptor<OutcomeReceiver<Void?, Throwable>>()
        verify(remoteInteractionsManager).startRemoteActivity(any(), any(), any(), captor.capture())
        captor.firstValue.onResult(null)
        assertEquals(result.await(), null)
    }

    @Test
    fun remoteActivityHelperStatus_notWatch_unknown() {
        setSystemFeatureWatch(false)
        val remoteActivityHelperStatus = runBlocking {
            mRemoteActivityHelper.availabilityStatus.first()
        }

        assertEquals(remoteActivityHelperStatus, RemoteActivityHelper.STATUS_UNKNOWN)
        verify(remoteInteractionsManager, never())
            .registerRemoteActivityHelperStatusListener(any(), any())
    }

    @Test
    @Config(minSdk = VERSION_CODES.TIRAMISU)
    fun remoteActivityHelperStatus_notSupported_unknown() {
        setSystemFeatureWatch(true)
        whenever(remoteInteractionsManager.isAvailabilityStatusApiSupported).thenReturn(false)
        val remoteActivityHelperStatus = runBlocking {
            mRemoteActivityHelper.availabilityStatus.first()
        }

        assertEquals(remoteActivityHelperStatus, RemoteActivityHelper.STATUS_UNKNOWN)
        verify(remoteInteractionsManager, never())
            .registerRemoteActivityHelperStatusListener(any(), any())
    }

    @Test
    @Config(minSdk = VERSION_CODES.TIRAMISU)
    fun remoteActivityHelperStatus_supported_propagateStatus() {
        setSystemFeatureWatch(true)

        for (remoteStatus in
            listOf(
                RemoteActivityHelper.STATUS_AVAILABLE,
                RemoteActivityHelper.STATUS_UNAVAILABLE,
                RemoteActivityHelper.STATUS_TEMPORARILY_UNAVAILABLE,
            )) {
            whenever(remoteInteractionsManager.isAvailabilityStatusApiSupported).thenReturn(true)
            doAnswer {
                    @Suppress("UNCHECKED_CAST")
                    val consumer: Consumer<Int> = it.arguments[1] as (Consumer<Int>)
                    consumer.accept(remoteStatus)
                }
                .whenever(remoteInteractionsManager)
                .registerRemoteActivityHelperStatusListener(any(), any())

            val remoteActivityHelperStatus = runBlocking {
                mRemoteActivityHelper.availabilityStatus.first()
            }

            assertEquals(remoteActivityHelperStatus, remoteStatus)
            verify(remoteInteractionsManager)
                .registerRemoteActivityHelperStatusListener(any(), any())
            verify(remoteInteractionsManager).unregisterRemoteActivityHelperStatusListener(any())
            reset(remoteInteractionsManager)
        }
    }

    @Test
    @Config(minSdk = 37)
    fun testStartPhoneActivityWithUnlock_delegatesToManager() = runTest {
        whenever(remoteInteractionsManager.isWearSdkApiContinueActivityOnPhoneWithUnlockSupported)
            .thenReturn(true)

        val result =
            mRemoteActivityHelper.startPhoneActivityWithUnlock(
                testPackageName,
                testPackageName,
                "target.action",
                testUri,
                listOf("category1"),
            )

        val captor = argumentCaptor<OutcomeReceiver<Void?, Throwable>>()
        verify(remoteInteractionsManager)
            .continueActivityOnPhoneWithUnlock(
                eq(testPackageName),
                eq("target.action"),
                eq(testUri),
                eq(listOf("category1")),
                eq(testPackageName),
                any(),
                captor.capture(),
            )
        verify(remoteInteractionsManager, never()).startRemoteActivity(any(), any(), any(), any())
        captor.firstValue.onResult(null)
        assertEquals(result.await(), null)
    }

    @Test
    @Config(minSdk = VERSION_CODES.TIRAMISU)
    fun testStartRemoteActivityAttemptUnlock_delegatesToManager() = runTest {
        whenever(remoteInteractionsManager.isWearSdkApiContinueActivityOnPhoneWithUnlockSupported)
            .thenReturn(true)

        val result = mRemoteActivityHelper.startRemoteActivityAttemptUnlock(testUri)

        val captor = argumentCaptor<OutcomeReceiver<Void?, Throwable>>()
        verify(remoteInteractionsManager)
            .continueActivityOnPhoneWithUnlock(
                eq(""),
                eq(Intent.ACTION_VIEW),
                eq(testUri),
                eq(listOf(Intent.CATEGORY_BROWSABLE)),
                eq(context.packageName),
                any(),
                captor.capture(),
            )
        captor.firstValue.onResult(null)
        assertEquals(result.await(), null)
    }

    @Test
    @Config(sdk = [VERSION_CODES.R])
    fun testStartRemoteActivityAttemptUnlock_api30_unsupported_fallback_success() = runTest {
        setSystemFeatureWatch(true)
        val receiver = TestBroadcastReceiver(RESULT_OK)
        context.registerReceiver(receiver, IntentFilter(ACTION_REMOTE_INTENT))

        try {
            val result = mRemoteActivityHelper.startRemoteActivityAttemptUnlock(testUri)

            shadowOf(Looper.getMainLooper()).idle()
            assertTrue(result.isDone)
            result.get()
        } catch (e: Exception) {
            fail("startRemoteActivityAttemptUnlock.get() shouldn't throw exception.")
        } finally {
            context.unregisterReceiver(receiver)
        }

        val broadcastIntents =
            shadowOf(ApplicationProvider.getApplicationContext() as Application).broadcastIntents
        assertEquals(1, broadcastIntents.size)
        val intent = broadcastIntents[0]
        assertEquals(Intent.ACTION_VIEW, getTargetIntent(intent)?.action)
        assertEquals(testUri, getTargetIntent(intent)?.data)
    }

    @Test
    @Config(minSdk = 37)
    fun testStartPhoneActivityWithUnlock_emptyPackage_otherAction_fails() = runTest {
        whenever(remoteInteractionsManager.isWearSdkApiContinueActivityOnPhoneWithUnlockSupported)
            .thenReturn(true)

        val future =
            mRemoteActivityHelper.startPhoneActivityWithUnlock(
                testPackageName,
                "",
                Intent.ACTION_SEND,
                testUri,
                listOf(Intent.CATEGORY_BROWSABLE),
            )

        val exception = assertThrows(ExecutionException::class.java) { future.get() }
        assertTrue(exception.cause is IllegalStateException)
    }

    @Test
    @Config(minSdk = 37)
    fun testStartPhoneActivityWithUnlock_emptyAction_fails() = runTest {
        whenever(remoteInteractionsManager.isWearSdkApiContinueActivityOnPhoneWithUnlockSupported)
            .thenReturn(true)

        val future =
            mRemoteActivityHelper.startPhoneActivityWithUnlock(
                testPackageName,
                testPackageName,
                "",
                testUri,
                emptyList(),
            )

        val exception = assertThrows(ExecutionException::class.java) { future.get() }
        assertTrue(exception.cause is IllegalStateException)
    }

    @Test
    @Config(minSdk = 37)
    fun testStartPhoneActivityWithUnlock_emptyUri_fails() = runTest {
        whenever(remoteInteractionsManager.isWearSdkApiContinueActivityOnPhoneWithUnlockSupported)
            .thenReturn(true)

        val future =
            mRemoteActivityHelper.startPhoneActivityWithUnlock(
                testPackageName,
                testPackageName,
                "target.action",
                Uri.EMPTY,
                listOf("category1"),
            )

        val exception = assertThrows(ExecutionException::class.java) { future.get() }
        assertTrue(exception.cause is IllegalStateException)
    }

    @Test
    @Config(minSdk = 37)
    fun testStartPhoneActivityWithUnlock_emptyCaller_fails() = runTest {
        whenever(remoteInteractionsManager.isWearSdkApiContinueActivityOnPhoneWithUnlockSupported)
            .thenReturn(true)

        val future =
            mRemoteActivityHelper.startPhoneActivityWithUnlock(
                "",
                testPackageName,
                "target.action",
                testUri,
                emptyList(),
            )

        val exception = assertThrows(ExecutionException::class.java) { future.get() }
        assertTrue(exception.cause is IllegalStateException)
    }

    @Test
    @Config(minSdk = 37)
    fun testStartPhoneActivityWithUnlock_emptyCategories_fails() = runTest {
        whenever(remoteInteractionsManager.isWearSdkApiContinueActivityOnPhoneWithUnlockSupported)
            .thenReturn(true)

        val future =
            mRemoteActivityHelper.startPhoneActivityWithUnlock(
                testPackageName,
                testPackageName,
                "target.action",
                testUri,
                emptyList(),
            )

        val exception = assertThrows(ExecutionException::class.java) { future.get() }
        assertTrue(exception.cause is IllegalStateException)
    }

    @Test
    @Config(minSdk = 37)
    fun testStartPhoneActivityWithUnlock_unsupported_throwsUnsupportedOperationException() =
        runTest {
            whenever(
                    remoteInteractionsManager.isWearSdkApiContinueActivityOnPhoneWithUnlockSupported
                )
                .thenReturn(false)

            val future =
                mRemoteActivityHelper.startPhoneActivityWithUnlock(
                    testPackageName,
                    testPackageName,
                    "target.action",
                    testUri,
                    listOf("category1"),
                )

            val exception = assertThrows(ExecutionException::class.java) { future.get() }
            assertTrue(exception.cause is UnsupportedOperationException)
        }

    @Test
    @Config(minSdk = 37)
    fun testStartPhoneActivityWithUnlock_managerThrows_futureFails() = runTest {
        whenever(remoteInteractionsManager.isWearSdkApiContinueActivityOnPhoneWithUnlockSupported)
            .thenReturn(true)
        whenever(
                remoteInteractionsManager.continueActivityOnPhoneWithUnlock(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                )
            )
            .thenThrow(UnsupportedOperationException("Not supported"))

        val future =
            mRemoteActivityHelper.startPhoneActivityWithUnlock(
                testPackageName,
                testPackageName,
                "target.action",
                testUri,
                listOf("category1"),
            )

        val exception = assertThrows(ExecutionException::class.java) { future.get() }
        assertTrue(exception.cause is UnsupportedOperationException)
    }

    @Test
    @Config(minSdk = VERSION_CODES.TIRAMISU)
    fun testStartRemoteActivityAttemptUnlock_noPermission_fallback_success() = runTest {
        val localManager = spy(RemoteInteractionsManagerCompat(context))
        mRemoteActivityHelper.remoteInteractionsManager = localManager
        shadowOf(context as Application)
            .denyPermissions(RemoteActivityHelper.PERMISSION_SEND_CONTINUE_ACTIVITY_ON_PHONE)
        doReturn(true).whenever(localManager).isWearSdkApiStartRemoteActivitySupported
        doReturn(true).whenever(localManager).isWearSdkApiContinueActivityOnPhoneWithUnlockSupported
        doAnswer {
                val receiver = it.getArgument<OutcomeReceiver<Void?, Throwable>>(3)
                receiver.onResult(null)
            }
            .whenever(localManager)
            .startRemoteActivity(any(), any(), any(), any())

        val result = mRemoteActivityHelper.startRemoteActivityAttemptUnlock(testUri)

        verify(localManager).startRemoteActivity(eq(testUri), any(), any(), any())
        assertEquals(result.await(), null)
    }

    @Test
    @Config(minSdk = VERSION_CODES.TIRAMISU)
    fun testStartRemoteActivityAttemptUnlock_unsupported_fallback_success() = runTest {
        val localManager = spy(RemoteInteractionsManagerCompat(context))
        mRemoteActivityHelper.remoteInteractionsManager = localManager
        doReturn(false)
            .whenever(localManager)
            .isWearSdkApiContinueActivityOnPhoneWithUnlockSupported
        doReturn(true).whenever(localManager).isWearSdkApiStartRemoteActivitySupported
        doAnswer {
                val receiver = it.getArgument<OutcomeReceiver<Void?, Throwable>>(3)
                receiver.onResult(null)
            }
            .whenever(localManager)
            .startRemoteActivity(any(), any(), any(), any())

        val result = mRemoteActivityHelper.startRemoteActivityAttemptUnlock(testUri)

        verify(localManager).startRemoteActivity(eq(testUri), any(), any(), any())
        assertEquals(result.await(), null)
    }

    @Test
    @Config(minSdk = VERSION_CODES.TIRAMISU)
    fun testStartRemoteActivityAttemptUnlock_managerFails_fallback_success() = runTest {
        val localManager = spy(RemoteInteractionsManagerCompat(context))
        mRemoteActivityHelper.remoteInteractionsManager = localManager
        doReturn(true).whenever(localManager).isWearSdkApiStartRemoteActivitySupported
        doReturn(true).whenever(localManager).isWearSdkApiContinueActivityOnPhoneWithUnlockSupported
        doAnswer {
                val receiver = it.getArgument<OutcomeReceiver<Void?, Throwable>>(6)
                receiver.onError(IllegalStateException("Failed"))
            }
            .whenever(localManager)
            .continueActivityOnPhoneWithUnlock(any(), any(), any(), any(), any(), any(), any())
        doAnswer {
                val receiver = it.getArgument<OutcomeReceiver<Void?, Throwable>>(3)
                receiver.onResult(null)
            }
            .whenever(localManager)
            .startRemoteActivity(any(), any(), any(), any())

        val result = mRemoteActivityHelper.startRemoteActivityAttemptUnlock(testUri)

        verify(localManager)
            .continueActivityOnPhoneWithUnlock(any(), any(), any(), any(), any(), any(), any())
        verify(localManager).startRemoteActivity(eq(testUri), any(), any(), any())
        assertEquals(result.await(), null)
    }

    @Test
    fun testStartRemoteActivityAttemptUnlock_missingBrowsable_fails() = runTest {
        val future =
            mRemoteActivityHelper.startRemoteActivityAttemptUnlock(
                testUri,
                targetCategories = emptyList(),
            )

        val exception = assertThrows(ExecutionException::class.java) { future.get() }
        assertTrue(exception.cause is IllegalArgumentException)
    }

    @Test
    fun testStartRemoteActivityAttemptUnlock_emptyUri_fails() = runTest {
        val future =
            mRemoteActivityHelper.startRemoteActivityAttemptUnlock(
                Uri.EMPTY,
                targetCategories = listOf(Intent.CATEGORY_BROWSABLE),
            )

        val exception = assertThrows(ExecutionException::class.java) { future.get() }
        assertTrue(exception.cause is IllegalArgumentException)
    }

    @Test
    @Config(minSdk = 37)
    fun testStartPhoneActivityWithUnlock_noPermission_fails() = runTest {
        shadowOf(context as Application)
            .denyPermissions(RemoteActivityHelper.PERMISSION_SEND_CONTINUE_ACTIVITY_ON_PHONE)

        val future =
            mRemoteActivityHelper.startPhoneActivityWithUnlock(
                testPackageName,
                testPackageName,
                "target.action",
                testUri,
                listOf("category1"),
            )

        val exception = assertThrows(ExecutionException::class.java) { future.get() }
        assertTrue(exception.cause is IllegalStateException)
    }
}

private class SyncExecutor : Executor {
    override fun execute(command: Runnable?) {
        command?.run()
    }
}
