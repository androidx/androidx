/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.work.multiprocess

import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.work.impl.WorkManagerImpl
import androidx.work.impl.utils.SerialExecutor
import androidx.work.impl.utils.futures.SettableFuture
import androidx.work.impl.utils.taskexecutor.TaskExecutor
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.`when`
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.atLeastOnce
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import java.util.concurrent.Executor

@RunWith(AndroidJUnit4::class)
public class RemoteWorkManagerClientTest {

    private lateinit var mContext: Context
    private lateinit var mWorkManager: WorkManagerImpl
    private lateinit var mExecutor: Executor
    private lateinit var mClient: RemoteWorkManagerClient

    @Before
    public fun setUp() {
        if (Build.VERSION.SDK_INT <= 27) {
            // Exclude <= API 27, from tests because it causes a SIGSEGV.
            return
        }

        mContext = mock(Context::class.java)
        mWorkManager = mock(WorkManagerImpl::class.java)
        `when`(mContext.applicationContext).thenReturn(mContext)
        mExecutor = Executor {
            it.run()
        }

        val taskExecutor = mock(TaskExecutor::class.java)
        `when`(taskExecutor.backgroundExecutor).thenReturn(SerialExecutor(mExecutor))
        `when`(mWorkManager.workTaskExecutor).thenReturn(taskExecutor)
        mClient = spy(RemoteWorkManagerClient(mContext, mWorkManager))
    }

    @Test
    @MediumTest
    public fun failGracefullyWhenBindFails() {
        if (Build.VERSION.SDK_INT <= 27) {
            // Exclude <= API 27, from tests because it causes a SIGSEGV.
            return
        }

        `when`(
            mContext.bindService(
                any(Intent::class.java), any(ServiceConnection::class.java),
                anyInt()
            )
        ).thenReturn(false)
        val intent = mock(Intent::class.java)
        var exception: Throwable? = null
        try {
            mClient.getSession(intent).get()
        } catch (throwable: Throwable) {
            exception = throwable
        }
        assertNotNull(exception)
        val message = exception?.cause?.message ?: ""
        assertTrue(message.contains("Unable to bind to service"))
    }

    @Test
    @MediumTest
    @Suppress("UNCHECKED_CAST")
    public fun cleanUpWhenDispatcherFails() {
        if (Build.VERSION.SDK_INT <= 27) {
            // Exclude <= API 27, from tests because it causes a SIGSEGV.
            return
        }

        val binder = mock(IBinder::class.java)
        val remoteDispatcher =
            mock(RemoteDispatcher::class.java) as RemoteDispatcher<IWorkManagerImpl>
        val remoteStub = mock(IWorkManagerImpl::class.java)
        val callback = spy(RemoteWorkManagerClient.SessionRemoteCallback(mClient))
        val message = "Something bad happened"
        `when`(remoteDispatcher.execute(remoteStub, callback)).thenThrow(RuntimeException(message))
        `when`(remoteStub.asBinder()).thenReturn(binder)
        val session = SettableFuture.create<IWorkManagerImpl>()
        session.set(remoteStub)
        var exception: Throwable? = null
        try {
            mClient.execute(session, remoteDispatcher, callback).get()
        } catch (throwable: Throwable) {
            exception = throwable
        }
        assertNotNull(exception)
        verify(callback).onFailure(message)
        verify(mClient, never()).cleanUp()
        verify(callback, atLeastOnce()).onRequestCompleted()
    }

    @Test
    @MediumTest
    @Suppress("UNCHECKED_CAST")
    public fun cleanUpWhenSessionIsInvalid() {
        if (Build.VERSION.SDK_INT <= 27) {
            // Exclude <= API 27, from tests because it causes a SIGSEGV.
            return
        }

        val remoteDispatcher =
            mock(RemoteDispatcher::class.java) as RemoteDispatcher<IWorkManagerImpl>
        val callback = spy(RemoteWorkManagerClient.SessionRemoteCallback(mClient))
        val session = SettableFuture.create<IWorkManagerImpl>()
        session.setException(RuntimeException("Something bad happened"))
        var exception: Throwable? = null
        try {
            mClient.execute(session, remoteDispatcher, callback).get()
        } catch (throwable: Throwable) {
            exception = throwable
        }
        assertNotNull(exception)
        verify(callback).onFailure(anyString())
        verify(mClient).cleanUp()
        verify(callback, atLeastOnce()).onRequestCompleted()
    }

    @Test
    @MediumTest
    public fun cleanUpOnSuccessfulDispatch() {
        if (Build.VERSION.SDK_INT <= 27) {
            // Exclude <= API 27, from tests because it causes a SIGSEGV.
            return
        }

        val binder = mock(IBinder::class.java)
        val remoteDispatcher = RemoteDispatcher<IWorkManagerImpl> { _, callback ->
            callback.onSuccess(ByteArray(0))
        }
        val remoteStub = mock(IWorkManagerImpl::class.java)
        val callback = spy(RemoteWorkManagerClient.SessionRemoteCallback(mClient))
        `when`(remoteStub.asBinder()).thenReturn(binder)
        val session = SettableFuture.create<IWorkManagerImpl>()
        session.set(remoteStub)
        var exception: Throwable? = null
        try {
            mClient.execute(session, remoteDispatcher, callback).get()
        } catch (throwable: Throwable) {
            exception = throwable
        }
        assertNull(exception)
        verify(callback).onSuccess(any())
        verify(mClient, never()).cleanUp()
        verify(callback, atLeastOnce()).onRequestCompleted()
    }
}
