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

package androidx.wear.watchface.editor

import android.content.ComponentName
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.annotation.UiThread
import androidx.concurrent.futures.ResolvableFuture
import androidx.wear.complications.data.ComplicationData
import androidx.wear.watchface.RenderParameters
import androidx.wear.watchface.client.ComplicationState
import androidx.wear.watchface.client.HeadlessWatchFaceClient
import androidx.wear.watchface.client.WatchFaceId
import androidx.wear.watchface.style.UserStyle
import androidx.wear.watchface.style.UserStyleSchema
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/**
 * [ListenableFuture]-based compatibility wrapper around [EditorSession]'s suspending methods.
 */
public class ListenableEditorSession(
    private val wrappedEditorSession: EditorSession
) : EditorSession() {
    public companion object {
        /**
         * Constructs a [ListenableFuture] for a [ListenableEditorSession] for an on watch face
         * editor. This registers an activity result handler and so it must be called during an
         * Activity or Fragment initialization path.
         *
         * If watch face editor takes more than 4s to create a watch face, returned future will be
         * resolved with [TimeoutCancellationException] exception.
         */
        @SuppressWarnings("ExecutorRegistration")
        @JvmStatic
        @UiThread
        public fun listenableCreateOnWatchEditingSession(
            /** The [ComponentActivity] associated with the EditorSession. */
            activity: ComponentActivity,

            /** [Intent] sent by SysUI to launch the editing session. */
            editIntent: Intent
        ): ListenableFuture<ListenableEditorSession?> {
            val result = ResolvableFuture.create<ListenableEditorSession?>()
            val coroutineScope =
                CoroutineScope(object : CoroutineDispatcher() {
                    override fun dispatch(context: CoroutineContext, block: Runnable) {
                        block.run()
                    }
                })
            coroutineScope.launch {
                try {
                    result.set(
                        ListenableEditorSession(createOnWatchEditingSession(activity, editIntent))
                    )
                } catch (e: Exception) {
                    result.setException(e)
                }
            }
            return result
        }

        /** Constructs an [ListenableEditorSession] for a remote watch face editor. */
        @JvmStatic
        @RequiresApi(27)
        @UiThread
        public fun createHeadlessEditingSession(
            /** The [ComponentActivity] associated with the EditorSession. */
            activity: ComponentActivity,

            /** [Intent] sent by SysUI to launch the editing session. */
            editIntent: Intent,

            headlessWatchFaceClient: HeadlessWatchFaceClient
        ): ListenableEditorSession? = EditorSession.createHeadlessEditingSession(
            activity,
            editIntent,
            headlessWatchFaceClient
        ).let {
            ListenableEditorSession(it)
        }
    }

    private fun getCoroutineScope(): CoroutineScope =
        (wrappedEditorSession as BaseEditorSession).coroutineScope

    override val watchFaceComponentName: ComponentName = wrappedEditorSession.watchFaceComponentName

    @get:RequiresApi(Build.VERSION_CODES.R)
    @RequiresApi(Build.VERSION_CODES.R)
    override val watchFaceId: WatchFaceId = wrappedEditorSession.watchFaceId

    override var userStyle: UserStyle
        get() = wrappedEditorSession.userStyle
        set(value) {
            wrappedEditorSession.userStyle = value
        }

    override val previewReferenceTimeMillis: Long = wrappedEditorSession.previewReferenceTimeMillis

    override val userStyleSchema: UserStyleSchema
        get() = wrappedEditorSession.userStyleSchema

    override val complicationsState: Map<Int, ComplicationState>
        get() = wrappedEditorSession.complicationsState

    /** [ListenableFuture] wrapper around [EditorSession.getComplicationsPreviewData]. */
    public fun getListenableComplicationPreviewData():
        ListenableFuture<Map<Int, ComplicationData>> {
            val future = ResolvableFuture.create<Map<Int, ComplicationData>>()
            getCoroutineScope().launch {
                try {
                    future.set(wrappedEditorSession.getComplicationsPreviewData())
                } catch (e: Exception) {
                    future.setException(e)
                }
            }
            return future
        }

    override suspend fun getComplicationsPreviewData(): Map<Int, ComplicationData> =
        wrappedEditorSession.getComplicationsPreviewData()

    @get:SuppressWarnings("AutoBoxing")
    override val backgroundComplicationId: Int?
        get() = wrappedEditorSession.backgroundComplicationId

    @SuppressWarnings("AutoBoxing")
    override fun getComplicationIdAt(x: Int, y: Int): Int? =
        wrappedEditorSession.getComplicationIdAt(x, y)

    override fun renderWatchFaceToBitmap(
        renderParameters: RenderParameters,
        calendarTimeMillis: Long,
        idToComplicationData: Map<Int, ComplicationData>?
    ): Bitmap = wrappedEditorSession.renderWatchFaceToBitmap(
        renderParameters,
        calendarTimeMillis,
        idToComplicationData
    )

    /** [ListenableFuture] wrapper around [EditorSession.openComplicationProviderChooser]. */
    public fun listenableOpenComplicationProviderChooser(
        complicationId: Int
    ): ListenableFuture<ChosenComplicationProvider?> {
        val future = ResolvableFuture.create<ChosenComplicationProvider?>()
        getCoroutineScope().launch {
            try {
                future.set(
                    wrappedEditorSession.openComplicationProviderChooser(complicationId)
                )
            } catch (e: Exception) {
                future.setException(e)
            }
        }
        return future
    }

    override suspend fun openComplicationProviderChooser(complicationId: Int):
        ChosenComplicationProvider? =
            wrappedEditorSession.openComplicationProviderChooser(complicationId)

    override fun close() {
        wrappedEditorSession.close()
    }
}
