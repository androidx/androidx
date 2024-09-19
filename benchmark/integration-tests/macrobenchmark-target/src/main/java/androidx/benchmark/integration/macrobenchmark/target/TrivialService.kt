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

package androidx.benchmark.integration.macrobenchmark.target

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Messenger

class TrivialService : Service() {
    private val TEST_ACTION1 = "androidx.benchmark.integration.macrobenchmark.target.TEST_ACTION1"
    private val TEST_ACTION2 = "androidx.benchmark.integration.macrobenchmark.target.TEST_ACTION2"

    private val handler = Handler(Looper.getMainLooper())
    private val messenger = Messenger(handler)

    override fun onBind(intent: Intent?): IBinder {
        println("onBind, ac = ${intent?.action}")
        when (intent?.action) {
            TEST_ACTION1 ->
                handler.post {
                    // DexClassLoader.getSystemClassLoader().loadClass(SingleColorActivity::class.java.name)
                    InnerClass().function1()
                }
            TEST_ACTION2 ->
                handler.post {
                    // DexClassLoader.getSystemClassLoader().loadClass(TrivialStartupActivity::class.java.name)
                    InnerClass().function2()
                }
            else -> throw IllegalArgumentException()
        }
        return messenger.binder
    }

    private class InnerClass {
        fun function1() {
            println("function1")
        }

        fun function2() {
            println("function2")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        println("onStartCommand")
        if (intent != null) {
            when (intent.action) {
                TEST_ACTION1 -> {
                    println("executing action 1")
                }
                TEST_ACTION2 -> {
                    println("executing action 2")
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        println("destroy!")
    }
}
