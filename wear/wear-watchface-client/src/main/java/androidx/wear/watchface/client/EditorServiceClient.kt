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

package androidx.wear.watchface.client

import androidx.annotation.RestrictTo
import androidx.wear.watchface.editor.IEditorObserver
import androidx.wear.watchface.editor.IEditorService
import androidx.wear.watchface.editor.data.EditorStateWireFormat
import java.util.concurrent.Executor

/**
 * Client for the watchface editor service, which observes
 * [androidx.wear.watchface.editor.EditorSession]. This client can be reused to observe multiple
 * editor sessions.
 */
public interface EditorServiceClient {
    /**
     * Starts listening for [EditorState] which is sent when
     * [androidx.wear.watchface.editor.EditorSession] closes. The
     * [EditorListener.onEditorStateChanged] callback is run on the specified [listenerExecutor].
     */
    public fun addListener(editorListener: EditorListener, listenerExecutor: Executor)

    /** Unregisters an [EditorListener] previously registered via [addListener].  */
    public fun removeListener(editorListener: EditorListener)

    /** Instructs any open editor to close. */
    public fun closeEditor()
}

/** Observes state changes in [androidx.wear.watchface.editor.EditorSession]. */
public interface EditorListener {
    /** Called in response to [androidx.wear.watchface.editor.EditorSession.close] .*/
    public fun onEditorStateChanged(editorState: EditorState)
}

/** @hide */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class EditorServiceClientImpl(
    private val iEditorService: IEditorService
) : EditorServiceClient {
    private val lock = Any()
    private val editorMap = HashMap<EditorListener, Int>()

    override fun addListener(
        editorListener: EditorListener,
        listenerExecutor: Executor
    ) {
        val observer = object : IEditorObserver.Stub() {
            override fun getApiVersion() = IEditorObserver.API_VERSION

            override fun onEditorStateChange(editorStateWireFormat: EditorStateWireFormat) {
                listenerExecutor.execute {
                    editorListener.onEditorStateChanged(
                        editorStateWireFormat.asApiEditorState()
                    )
                }
            }
        }

        synchronized(lock) {
            editorMap[editorListener] = iEditorService.registerObserver(observer)
        }
    }

    override fun removeListener(editorListener: EditorListener) {
        synchronized(lock) {
            editorMap[editorListener]?.let {
                iEditorService.unregisterObserver(it)
                editorMap.remove(editorListener)
            }
        }
    }

    override fun closeEditor() {
        iEditorService.closeEditor()
    }
}
