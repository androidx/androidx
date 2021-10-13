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

package androidx.metrics.performance

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Message
import android.view.Choreographer
import android.view.View
import android.view.ViewTreeObserver
import androidx.annotation.RequiresApi
import java.lang.ref.WeakReference

/**
 * Subclass of JankStatsBaseImpl records frame timing data for API 16 and later,
 * using Choreographer (which was introduced in API 16).
 */
@RequiresApi(16)
internal open class JankStatsApi16Impl(
    jankStats: JankStats,
    view: View
) :
    JankStatsBaseImpl(jankStats) {

    // TODO: decorView may change in Window, think about how to handle that
    // e.g., should we cache Window instead?
    internal val decorViewRef = WeakReference(view)

    lateinit var viewTreeObserver: ViewTreeObserver

    // Must cache this at init time, from view, since some subclasses will not receive callbacks
    // on the UI thread, so they will not have access to the appropriate Choreographer for
    // frame timing values
    val choreographer = Choreographer.getInstance()

    private val onPreDrawListener: ViewTreeObserver.OnPreDrawListener =
        object : ViewTreeObserver.OnPreDrawListener {

            @SuppressLint("ClassVerificationFailure")
            override fun onPreDraw(): Boolean {
                val decorView = decorViewRef.get()
                val frameStart = getFrameStartTime()
                decorView?.let {
                    decorView.handler.sendMessageAtFrontOfQueue(
                        Message.obtain(decorView.handler) {
                            val now = System.nanoTime()
                            val expectedDuration = getExpectedFrameDuration(decorView) *
                                JankStats.jankHeuristicMultiplier
                            jankStats.logFrameData(
                                frameStart,
                                now - frameStart,
                                expectedDuration.toLong()
                            )
                        }.apply {
                            setMessageAsynchronicity(this)
                        }
                    )
                }
                return true
            }
        }

    // Noop prior to API 22
    internal open fun setMessageAsynchronicity(message: Message) {}

    override fun setupFrameTimer(enable: Boolean) {
        val decorView = decorViewRef.get()
        decorView?.let {
            if (enable) {
                viewTreeObserver = it.viewTreeObserver
                viewTreeObserver.addOnPreDrawListener(onPreDrawListener)
            } else {
                // TODO: make sure we actually init it first
                viewTreeObserver.removeOnPreDrawListener(onPreDrawListener)
            }
        }
    }

    internal open fun getFrameStartTime(): Long {
        return choreographerLastFrameTimeField.get(choreographer) as Long
    }

    @Suppress("deprecation") /* defaultDisplay */
    fun getExpectedFrameDuration(view: View?): Long {
        if (frameDuration < 0) {
            var refreshRate = 60f
            val window = if (view?.context is Activity) (view.context as Activity).window else null
            if (window != null) {
                val display = window.windowManager.defaultDisplay
                refreshRate = display.refreshRate
            }
            if (refreshRate < 30f || refreshRate > 200f) {
                // Account for faulty return values (including 0)
                refreshRate = 60f
            }
            frameDuration = (1000 / refreshRate * NANOS_PER_MS).toLong()
        }
        return frameDuration
    }

    companion object {
        val choreographerLastFrameTimeField =
            Choreographer::class.java.getDeclaredField("mLastFrameTimeNanos")

        init {
            choreographerLastFrameTimeField.isAccessible = true
        }
    }
}