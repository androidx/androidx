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

package androidx.datastore.core

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import androidx.testing.TestMessageProto.FooProto
import com.google.common.collect.ImmutableList
import java.io.Serializable
import java.util.concurrent.CountDownLatch

private val THROWABLE_BUNDLE_KEY: String = "throwable"

internal class Latch {
    private var signaled: Boolean = false

    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    fun signal() {
        synchronized(this) {
            signaled = true
            (this as java.lang.Object).notify()
        }
    }

    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    fun awaitSignal() {
        synchronized(this) {
            while (!signaled) {
                try {
                    (this as java.lang.Object).wait()
                } catch (_: InterruptedException) {
                    // Ignore.
                }
            }
            signaled = false
        }
    }
}

abstract class DirectTestService() : Service() {
    private val SERVICE_CLASS = this::class.java

    @Suppress("DEPRECATION")
    private val messenger: Messenger = Messenger(Handler(HandlerCallback()))
    private val resume: Latch = Latch()
    private val done: Latch = Latch()
    private lateinit var testData: Bundle
    private lateinit var thread: Thread
    private lateinit var testFailure: Throwable

    // It should be setup in `beforeTest`
    internal lateinit var store: MultiProcessDataStore<FooProto>

    override fun onBind(intent: Intent): IBinder {
        return messenger.getBinder()
    }

    override fun onCreate() {
        // No-op
    }

    override fun onDestroy() {
        // No-op
    }

    protected abstract fun beforeTest(testData: Bundle)

    protected abstract fun runTest()

    protected fun waitForSignal() {
        done.signal()
        resume.awaitSignal()
    }

    private fun handle(msg: Message) {
        if (!this::thread.isInitialized) {
            testData = msg.getData()
            thread = Thread(Runner())
            thread.start()
        } else {
            resume.signal()
        }
        done.awaitSignal()
        try {
            val response: Message = Message.obtain()
            if (this::testFailure.isInitialized) {
                val data = Bundle()
                data.putSerializable(THROWABLE_BUNDLE_KEY, testFailure as Serializable)
                response.setData(data)
            }
            msg.replyTo.send(response)
        } catch (ex: RemoteException) {
            throw RuntimeException("Test service failed to ack message", ex)
        }
    }

    private inner class HandlerCallback : Handler.Callback {
        override fun handleMessage(msg: Message): Boolean {
            handle(msg)
            return true
        }
    }

    private inner class Runner : Runnable {
        override fun run() {
            try {
                beforeTest(testData)
                waitForSignal()
                runTest()
            } catch (t: Throwable) {
                testFailure = t
            } finally {
                done.signal()
            }
        }
    }
}

class BlockingServiceConnection(
    private val context: Context,
    private val serviceIntent: Intent
) :
    ServiceConnection {
    private lateinit var isConnected: CountDownLatch
    private var service: Messenger? = null
    private var remoteException: Throwable? = null

    override fun onServiceConnected(className: ComponentName, serviceBinder: IBinder) {
        service = Messenger(serviceBinder)
        isConnected.countDown()
    }

    override fun onServiceDisconnected(className: ComponentName) {
        service = null
    }

    fun isServiceConnected(): Boolean {
        return service != null
    }

    @Suppress("DEPRECATION")
    fun connect(connectedLatch: CountDownLatch) {
        isConnected = connectedLatch
        val serviceExists: Boolean =
            context.bindService(serviceIntent, this, Context.BIND_AUTO_CREATE)

        if (!serviceExists) {
            val targetPackage: String = serviceIntent.getComponent()!!.getPackageName()
            val targetService: String = serviceIntent.getComponent()!!.getClassName()

            try {
                context.getPackageManager().getPackageInfo(targetPackage, 0)
            } catch (e: PackageManager.NameNotFoundException) {
                throw IllegalStateException("Package not installed [$targetPackage]", e)
            }
            throw IllegalStateException("Package installed but service not found [$targetService]")
        }
    }

    fun signal(msgData: Bundle?, isDelivered: CountDownLatch) {
        val blockingHandler: BlockingHandler = BlockingHandler(Looper.getMainLooper(), isDelivered)
        val msg: Message = Message.obtain()
        msg.replyTo = Messenger(blockingHandler)
        msgData?.let { msg.setData(it) }
        try {
            service?.send(msg)
        } catch (e: RemoteException) {
            throw RuntimeException("Remote service failed", e)
        }
    }

    fun propagateRemoteExceptionIfPresent() {
        if (remoteException != null) {
            throw RuntimeException(remoteException!!)
        }
    }

    private inner class BlockingHandler constructor(
        private val looper: Looper,
        private val isDelivered: CountDownLatch
    ) :
        Handler(looper) {

        @Suppress("DEPRECATION")
        override fun handleMessage(msg: Message) {
            remoteException = msg.getData().getSerializable(THROWABLE_BUNDLE_KEY) as Throwable?
            isDelivered.countDown()
        }
    }
}

internal fun setUpService(
    context: Context,
    service: Class<out Service>,
    testData: Bundle
): BlockingServiceConnection {
    val serviceIntent = Intent(context, service)
    return setUpServicesInternal(context, ImmutableList.of(serviceIntent), testData)[0]
}

internal fun setUpServices(
    context: Context,
    services: List<Class<out Service>>,
    testData: Bundle
): List<BlockingServiceConnection> {
    val serviceIntents: MutableList<Intent> = ArrayList()
    for (service in services) {
        serviceIntents.add(Intent(context, service))
    }
    return setUpServicesInternal(context, serviceIntents, testData)
}

private fun setUpServicesInternal(
    context: Context,
    serviceIntents: List<Intent>,
    testData: Bundle
): List<BlockingServiceConnection> {
    val connections: MutableList<BlockingServiceConnection> = ArrayList()
    for (serviceIntent in serviceIntents) {
        val connection = BlockingServiceConnection(context, serviceIntent)
        connections.add(connection)
    }
    val connectLatch = CountDownLatch(connections.size)
    for (connection in connections) {
        connection.connect(connectLatch)
    }
    connectLatch.await()

    // Send initial test data
    val signalLatch = CountDownLatch(connections.size)
    for (connection in connections) {
        connection.signal(testData, signalLatch)
    }
    signalLatch.await()
    for (connection in connections) {
        connection.propagateRemoteExceptionIfPresent()
    }
    return connections
}

internal fun signalService(connection: BlockingServiceConnection) {
    return signalServices(ImmutableList.of(connection))
}

internal fun signalServices(connections: List<BlockingServiceConnection>) {
    val latch = CountDownLatch(connections.size)
    for (connection in connections) {
        connection.signal( /* msgData= */null, latch)
    }
    latch.await()
    for (connection in connections) {
        connection.propagateRemoteExceptionIfPresent()
    }
}