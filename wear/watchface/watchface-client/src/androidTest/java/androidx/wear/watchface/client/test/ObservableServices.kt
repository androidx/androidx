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

package androidx.wear.watchface.client.test

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

open class ObservableService(private val latch: CountDownLatch) : Service() {
    private val binder: IBinder = LocalBinder()

    inner class LocalBinder : Binder() {
        val service: ObservableService
            get() = this@ObservableService
    }

    override fun onBind(intent: Intent?) = binder

    override fun onCreate() {
        super.onCreate()
        latch.countDown()
        this.stopSelf()
    }
}

class ObservableServiceA : ObservableService(latch) {
    companion object {
        private var latch = CountDownLatch(1)

        /**
         * Awaits for up to [maxDurationMillis] milliseconds the service to be bound.
         *
         * @return True if the service is bound before the time runs out, or false otherwise.
         */
        fun awaitForServiceToBeBound(maxDurationMillis: Long): Boolean =
            latch.await(maxDurationMillis, TimeUnit.MILLISECONDS)

        fun reset() {
            latch = CountDownLatch(1)
        }

        fun createPendingIntent(context: Context) =
            PendingIntent.getService(
                context,
                101,
                Intent(context, ObservableServiceA::class.java),
                PendingIntent.FLAG_IMMUTABLE
            )
    }
}

class ObservableServiceB : ObservableService(latch) {
    companion object {
        private var latch = CountDownLatch(1)

        /**
         * Awaits for up to [maxDurationMillis] milliseconds the service to be bound.
         *
         * @return True if the service is bound before the time runs out, or false otherwise.
         */
        fun awaitForServiceToBeBound(maxDurationMillis: Long): Boolean =
            latch.await(maxDurationMillis, TimeUnit.MILLISECONDS)

        fun reset() {
            latch = CountDownLatch(1)
        }

        fun createPendingIntent(context: Context) =
            PendingIntent.getService(
                context,
                101,
                Intent(context, ObservableServiceB::class.java),
                PendingIntent.FLAG_IMMUTABLE
            )
    }
}

class ObservableServiceC : ObservableService(latch) {
    companion object {
        private var latch = CountDownLatch(1)

        /**
         * Awaits for up to [maxDurationMillis] milliseconds the service to be bound.
         *
         * @return True if the service is bound before the time runs out, or false otherwise.
         */
        fun awaitForServiceToBeBound(maxDurationMillis: Long): Boolean =
            latch.await(maxDurationMillis, TimeUnit.MILLISECONDS)

        fun reset() {
            latch = CountDownLatch(1)
        }

        fun createPendingIntent(context: Context) =
            PendingIntent.getService(
                context,
                101,
                Intent(context, ObservableServiceC::class.java),
                PendingIntent.FLAG_IMMUTABLE
            )
    }
}
