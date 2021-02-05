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

import androidx.annotation.RestrictTo
import androidx.wear.watchface.editor.data.EditorStateWireFormat

/**
 * Implementation of [IEditorService], intended for use by EditorSession only.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class EditorService : IEditorService.Stub() {
    private val lock = Any()
    private var nextId: Int = 0
    private val observers = HashMap<Int, IEditorObserver>()

    public companion object {
        /** [EditorService] singleton. */
        public val globalEditorService: EditorService by lazy { EditorService() }
    }

    public override fun getApiVersion(): Int = API_VERSION

    override fun registerObserver(observer: IEditorObserver): Int {
        synchronized(lock) {
            val id = nextId++
            observers[id] = observer
            return id
        }
    }

    override fun unregisterObserver(observerId: Int) {
        synchronized(lock) {
            observers.remove(observerId)
        }
    }

    /**
     * Calls [IEditorObserver.onEditorStateChange] with [editorState] for each [IEditorObserver].
     */
    public fun broadcastEditorState(editorState: EditorStateWireFormat) {
        synchronized(lock) {
            for ((_, observer) in observers) {
                observer.onEditorStateChange(editorState)
            }
        }
    }
}