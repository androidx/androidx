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
import android.os.ResultReceiver
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
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implements

@RunWith(WearRemoteInteractionsTestRunner::class)
@Config(shadows = [RemoteActivityHelperTest.ActualResultReceiver::class])
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
            val resultReceiver = intent?.let {
                getRemoteIntentResultReceiver(it)
            }
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
    private val testExtraIntent = Intent(Intent.ACTION_VIEW)
        .addCategory(Intent.CATEGORY_BROWSABLE)
        .setData(testUri)
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
    }

    private fun setSystemFeatureWatch(isWatch: Boolean) {
        val shadowPackageManager = shadowOf(context.packageManager)
        shadowPackageManager!!.setSystemFeature(
            RemoteInteractionsUtil.SYSTEM_FEATURE_WATCH, isWatch
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
    fun testStartRemoteActivity_notActionViewIntent() {
        assertThrows(
            ExecutionException::class.java
        ) { mRemoteActivityHelper.startRemoteActivity(Intent(), testNodeId).get() }
    }

    @Test
    fun testStartRemoteActivity_dataNull() {
        assertThrows(
            ExecutionException::class.java
        ) {
            mRemoteActivityHelper.startRemoteActivity(Intent(Intent.ACTION_VIEW), testNodeId).get()
        }
    }

    @Test
    fun testStartRemoteActivity_notCategoryBrowsable() {
        assertThrows(
            ExecutionException::class.java
        ) {
            mRemoteActivityHelper.startRemoteActivity(
                Intent(Intent.ACTION_VIEW).setData(Uri.EMPTY), testNodeId
            ).get()
        }
    }

    @Test
    fun testStartRemoteActivity_watch() {
        setSystemFeatureWatch(true)
        val receiver = TestBroadcastReceiver(RESULT_OK)
        context.registerReceiver(receiver, IntentFilter(ACTION_REMOTE_INTENT))

        try {
            val future = mRemoteActivityHelper.startRemoteActivity(testExtraIntent, testNodeId)
            shadowOf(Looper.getMainLooper()).idle()
            assertTrue(future.isDone)
            future.get()
        } catch (e: Exception) {
            fail("startRemoteActivity.get() shouldn't throw exception in this case.")
        } finally {
            context.unregisterReceiver(receiver)
        }

        val broadcastIntents =
            shadowOf(ApplicationProvider.getApplicationContext() as Application)
                .broadcastIntents
        assertEquals(1, broadcastIntents.size)
        val intent = broadcastIntents[0]
        assertEquals(testExtraIntent, getTargetIntent(intent))
        assertEquals(testNodeId, getTargetNodeId(intent))
        assertEquals(DEFAULT_PACKAGE, intent.`package`)
    }

    @Test
    fun testStartRemoteActivity_watchFailed() {
        setSystemFeatureWatch(true)
        val receiver = TestBroadcastReceiver(RESULT_FAILED)
        context.registerReceiver(receiver, IntentFilter(ACTION_REMOTE_INTENT))

        try {
            val future = mRemoteActivityHelper.startRemoteActivity(testExtraIntent, testNodeId)
            shadowOf(Looper.getMainLooper()).idle()
            assertTrue(future.isDone)
            assertThrows(ExecutionException::class.java) { future.get() }
        } finally {
            context.unregisterReceiver(receiver)
        }
    }

    @Test
    fun testStartRemoteActivity_phoneWithPackageName() {
        setSystemFeatureWatch(false)
        nodeClientReturnFakePackageName(testNodeId, testPackageName)
        val receiver = TestBroadcastReceiver(RESULT_OK)
        context.registerReceiver(receiver, IntentFilter(ACTION_REMOTE_INTENT))

        try {
            val future = mRemoteActivityHelper.startRemoteActivity(testExtraIntent, testNodeId)
            shadowOf(Looper.getMainLooper()).idle()
            assertTrue(future.isDone)
            future.get()
        } catch (e: Exception) {
            fail("startRemoteActivity.get() shouldn't throw exception in this case.")
        } finally {
            context.unregisterReceiver(receiver)
        }

        val broadcastIntents =
            shadowOf(ApplicationProvider.getApplicationContext() as Application)
                .broadcastIntents
        assertEquals(1, broadcastIntents.size)
        assertRemoteIntentEqual(testExtraIntent, testNodeId, testPackageName, broadcastIntents[0])
    }

    @Test
    fun testStartRemoteActivity_phoneWithoutPackageName() {
        setSystemFeatureWatch(false)
        nodeClientReturnFakePackageName(testNodeId, null)
        val receiver = TestBroadcastReceiver(RESULT_OK)
        context.registerReceiver(receiver, IntentFilter(ACTION_REMOTE_INTENT))

        try {
            val future = mRemoteActivityHelper.startRemoteActivity(testExtraIntent, testNodeId)
            shadowOf(Looper.getMainLooper()).idle()
            assertTrue(future.isDone)
            future.get()
        } catch (e: Exception) {
            fail("startRemoteActivity.get() shouldn't throw exception in this case.")
        } finally {
            context.unregisterReceiver(receiver)
        }

        val broadcastIntents =
            shadowOf(ApplicationProvider.getApplicationContext() as Application)
                .broadcastIntents
        assertEquals(1, broadcastIntents.size)
        val intent = broadcastIntents[0]
        assertEquals(testExtraIntent, getTargetIntent(intent))
        assertEquals(testNodeId, getTargetNodeId(intent))
        assertEquals(DEFAULT_PACKAGE, intent.`package`)
    }

    @Test
    fun testStartRemoteActivity_phoneWithoutNodeId_allOk() {
        setSystemFeatureWatch(false)
        nodeClientReturnFakeConnectedNodes()
        nodeClientReturnFakePackageName(testNodeId, testPackageName)
        nodeClientReturnFakePackageName(testNodeId2, testPackageName2)
        val receiver = TestBroadcastReceiver(RESULT_OK)
        context.registerReceiver(receiver, IntentFilter(ACTION_REMOTE_INTENT))

        try {
            val future = mRemoteActivityHelper.startRemoteActivity(
                testExtraIntent, targetNodeId = null
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
            shadowOf(ApplicationProvider.getApplicationContext() as Application)
                .broadcastIntents
        assertEquals(2, broadcastIntents.size)

        assertRemoteIntentEqual(testExtraIntent, testNodeId, testPackageName, broadcastIntents[0])
        assertRemoteIntentEqual(testExtraIntent, testNodeId2, testPackageName2, broadcastIntents[1])
    }

    @Test
    fun testStartRemoteActivity_phoneWithoutNodeId_oneOkOneFail() {
        setSystemFeatureWatch(false)
        nodeClientReturnFakeConnectedNodes()
        nodeClientReturnFakePackageName(testNodeId, testPackageName)
        nodeClientReturnFakePackageName(testNodeId2, testPackageName2)
        val receiver = TestBroadcastReceiver(TestBroadcastReceiver.DIFFERENT_RESULT)
        context.registerReceiver(receiver, IntentFilter(ACTION_REMOTE_INTENT))

        assertThrows(ExecutionException::class.java) {
            val future = mRemoteActivityHelper.startRemoteActivity(
                testExtraIntent, targetNodeId = null
            )
            shadowOf(Looper.getMainLooper()).idle()
            assertTrue(future.isDone)
            future.get()
        }
        context.unregisterReceiver(receiver)

        shadowOf(Looper.getMainLooper()).idle()
        val broadcastIntents =
            shadowOf(ApplicationProvider.getApplicationContext() as Application)
                .broadcastIntents
        assertEquals(2, broadcastIntents.size)
        assertRemoteIntentEqual(testExtraIntent, testNodeId, testPackageName, broadcastIntents[0])
        assertRemoteIntentEqual(testExtraIntent, testNodeId2, testPackageName2, broadcastIntents[1])
    }

    @Test
    fun testStartRemoteActivity_phoneWithoutNodeId_allFail() {
        setSystemFeatureWatch(false)
        nodeClientReturnFakeConnectedNodes()
        nodeClientReturnFakePackageName(testNodeId, testPackageName)
        nodeClientReturnFakePackageName(testNodeId2, testPackageName2)
        val receiver = TestBroadcastReceiver(RESULT_FAILED)
        context.registerReceiver(receiver, IntentFilter(ACTION_REMOTE_INTENT))

        assertThrows(ExecutionException::class.java) {
            val future = mRemoteActivityHelper.startRemoteActivity(
                testExtraIntent, targetNodeId = null
            )
            shadowOf(Looper.getMainLooper()).idle()
            assertTrue(future.isDone)
            future.get()
        }
        context.unregisterReceiver(receiver)

        shadowOf(Looper.getMainLooper()).idle()
        val broadcastIntents =
            shadowOf(ApplicationProvider.getApplicationContext() as Application)
                .broadcastIntents
        assertEquals(2, broadcastIntents.size)
        assertRemoteIntentEqual(testExtraIntent, testNodeId, testPackageName, broadcastIntents[0])
        assertRemoteIntentEqual(testExtraIntent, testNodeId2, testPackageName2, broadcastIntents[1])
    }

    private fun assertRemoteIntentEqual(
        expectedExtraIntent: Intent,
        expectedNodeId: String,
        expectedPackageName: String,
        actualIntent: Intent
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
    fun testStartRemoteActivity_getCompanionPackageErrorPropagates() {
        setSystemFeatureWatch(false)
        val receiver = TestBroadcastReceiver(RESULT_OK)
        context.registerReceiver(receiver, IntentFilter(ACTION_REMOTE_INTENT))

        Mockito.`when`(mockNodeClient.getCompanionPackageForNode(any()))
            .thenReturn(Tasks.forException(IllegalStateException("Error")))

        val future = mRemoteActivityHelper.startRemoteActivity(testExtraIntent, testNodeId)
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(future.isDone)

        val actualException = assertThrows(ExecutionException::class.java) {
            future.get()
        }

        assertTrue(actualException.cause is IllegalStateException)
    }

    @Test
    fun testStartRemoteActivity_getConnectedNodesErrorPropagates() {
        setSystemFeatureWatch(false)
        val receiver = TestBroadcastReceiver(RESULT_OK)
        context.registerReceiver(receiver, IntentFilter(ACTION_REMOTE_INTENT))

        Mockito.`when`(mockNodeClient.connectedNodes)
            .thenReturn(Tasks.forException(IllegalStateException("Error")))

        val future = mRemoteActivityHelper.startRemoteActivity(testExtraIntent)
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(future.isDone)

        val actualException = assertThrows(ExecutionException::class.java) {
            future.get()
        }

        assertTrue(actualException.cause is IllegalStateException)
    }

    @Test
    fun testStartRemoteActivity_noNodeId_getCompanionPackageErrorPropagates() {
        setSystemFeatureWatch(false)
        nodeClientReturnFakeConnectedNodes()
        val receiver = TestBroadcastReceiver(RESULT_OK)
        context.registerReceiver(receiver, IntentFilter(ACTION_REMOTE_INTENT))

        Mockito.`when`(mockNodeClient.getCompanionPackageForNode(any()))
            .thenReturn(Tasks.forException(IllegalStateException("Error")))

        val future = mRemoteActivityHelper.startRemoteActivity(testExtraIntent)
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(future.isDone)

        val actualException = assertThrows(ExecutionException::class.java) {
            future.get()
        }

        assertTrue(actualException.cause is IllegalStateException)
    }

    @Test
    fun testStartRemoteActivity_nodeNotFound() {
        setSystemFeatureWatch(false)
        Mockito.`when`(mockNodeClient.getCompanionPackageForNode(any()))
            .thenReturn(Tasks.forResult(""))

        val future = mRemoteActivityHelper.startRemoteActivity(testExtraIntent, testNodeId)
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(future.isDone)

        val actualException = assertThrows(ExecutionException::class.java) {
            future.get()
        }

        assertTrue(actualException.cause is NotFoundException)
    }

    @Test
    fun testStartRemoveActivity_noNodes() {
        setSystemFeatureWatch(false)
        Mockito.`when`(mockNodeClient.connectedNodes)
            .thenReturn(Tasks.forResult(listOf()))

        val future = mRemoteActivityHelper.startRemoteActivity(testExtraIntent)
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(future.isDone)

        val actualException = assertThrows(ExecutionException::class.java) {
            future.get()
        }

        assertTrue(actualException.cause is NotFoundException)
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

        for (remoteStatus in listOf(
            RemoteActivityHelper.STATUS_AVAILABLE,
            RemoteActivityHelper.STATUS_UNAVAILABLE,
            RemoteActivityHelper.STATUS_TEMPORARILY_UNAVAILABLE)) {
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
}

private class SyncExecutor : Executor {
    override fun execute(command: Runnable?) {
        command?.run()
    }
}
