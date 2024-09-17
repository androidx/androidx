/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.benchmark.macro.junit4

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert

/** Convenience wrapper around TrivialService able to invoke multiple bind actions */
class TrivialServiceHandle {
    private val TAG = this::class.java.simpleName

    private val context = InstrumentationRegistry.getInstrumentation().context
    private var serviceConnection: ServiceConnection? = null

    enum class Action(val actionName: String) {
        TEST_ACTION1("$TARGET.TEST_ACTION1"),
        TEST_ACTION2("$TARGET.TEST_ACTION2"),
    }

    /** Connect to a TrivialService with the specified bind action */
    fun connect(actionToInvoke: Action) {
        require(serviceConnection == null)
        val intent =
            Intent().apply {
                component = ComponentName(TARGET, "$TARGET.TrivialService")
                action = actionToInvoke.actionName
            }
        serviceConnection =
            object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName, service: IBinder) {
                    println("connected")
                }

                override fun onServiceDisconnected(name: ComponentName) {
                    println("disconnected")
                }

                override fun onNullBinding(name: ComponentName?) {
                    println("onNullBinding")
                }

                override fun onBindingDied(name: ComponentName?) {
                    println("onBindingDied")
                }
            }

        val isBound = context.bindService(intent, serviceConnection!!, Context.BIND_AUTO_CREATE)
        Assert.assertTrue("Service not bound", isBound)
    }

    /** Disconnect if service is bound */
    fun disconnect() {
        if (serviceConnection != null) {
            Log.d(TAG, "disconnecting")
            context.unbindService(serviceConnection!!)
            serviceConnection = null
        }
    }

    companion object {
        const val TARGET = "androidx.benchmark.integration.macrobenchmark.target"
        const val TARGET_SERVICE_PROCESS = "$TARGET:ServiceProcess"
    }
}
