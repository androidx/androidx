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

import android.os.IBinder
import android.util.Log
import androidx.annotation.RestrictTo
import androidx.wear.watchface.IndentingPrintWriter
import androidx.wear.watchface.editor.data.EditorStateWireFormat

/** Implementation of [IEditorService], intended for use by EditorSession only. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class EditorService : IEditorService.Stub() {
    private val lock = Any()
    private var nextId: Int = 0
    private val observers = HashMap<Int, IEditorObserver>()
    private val deathObservers = HashMap<Int, IBinder.DeathRecipient>()
    private val closeEditorCallbacks = HashSet<CloseCallback>()

    public companion object {
        /** [EditorService] singleton. */
        public val globalEditorService: EditorService by lazy { EditorService() }

        internal const val TAG = "EditorService"
    }

    public override fun getApiVersion(): Int = API_VERSION

    override fun registerObserver(observer: IEditorObserver): Int {
        synchronized(lock) {
            val id = nextId++
            observers[id] = observer
            val deathObserver =
                IBinder.DeathRecipient {
                    Log.w(TAG, "observer died, closing editor")
                    // If SysUI dies we should close the editor too, otherwise the watchface could
                    // get
                    // left in an inconsistent state where it has local edits that were not
                    // persisted by
                    // the system.
                    closeEditor()
                    unregisterObserver(id)
                }
            observer.asBinder().linkToDeath(deathObserver, 0)
            deathObservers[id] = deathObserver
            return id
        }
    }

    override fun unregisterObserver(observerId: Int) {
        synchronized(lock) {
            deathObservers[observerId]?.let {
                try {
                    observers[observerId]?.asBinder()?.unlinkToDeath(it, 0)
                } catch (e: NoSuchElementException) {
                    // This really shouldn't happen.
                    Log.w(TAG, "unregisterObserver encountered", e)
                }
            }
            observers.remove(observerId)
            deathObservers.remove(observerId)
        }
    }

    public abstract class CloseCallback {
        /** Called when [closeEditor] is called. */
        public abstract fun onClose()
    }

    override fun closeEditor() {
        val callbackCopy = synchronized(lock) { HashSet<CloseCallback>(closeEditorCallbacks) }
        // We iterate on a copy of closeEditorCallbacks to avoid calls to removeCloseCallback
        // mutating a set we're iterating.
        for (observer in callbackCopy) {
            observer.onClose()
        }
    }

    /**
     * Adds [closeCallback] to the set of observers to be called if the client calls [closeEditor].
     */
    public fun addCloseCallback(closeCallback: CloseCallback) {
        synchronized(lock) { closeEditorCallbacks.add(closeCallback) }
    }

    /**
     * Removes [closeCallback] from set of observers to be called if the client calls [closeEditor].
     */
    public fun removeCloseCallback(closeCallback: CloseCallback) {
        synchronized(lock) { closeEditorCallbacks.remove(closeCallback) }
    }

    /** Removes all [closeEditorCallbacks]. */
    public fun clearCloseCallbacks() {
        synchronized(lock) { closeEditorCallbacks.clear() }
    }

    /**
     * Calls [IEditorObserver.onEditorStateChange] with [editorState] for each [IEditorObserver].
     */
    public fun broadcastEditorState(editorState: EditorStateWireFormat) {
        synchronized(lock) {
            for ((_, observer) in observers) {
                if (observer.asBinder().isBinderAlive) {
                    observer.onEditorStateChange(editorState)
                }
            }
        }
    }

    internal fun dump(writer: IndentingPrintWriter) {
        writer.println("EditorService:")
        writer.increaseIndent()
        synchronized(lock) {
            for ((id, observer) in observers) {
                writer.println("id = $id, alive = ${observer.asBinder().isBinderAlive}")
                if (observer.asBinder().isBinderAlive) {
                    writer.println("$apiVersion = {observer.apiVersion}")
                }
            }
        }
        writer.decreaseIndent()
    }
}
