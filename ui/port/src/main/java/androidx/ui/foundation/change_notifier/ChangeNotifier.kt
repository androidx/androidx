/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.foundation.change_notifier

import androidx.annotation.CallSuper
import androidx.ui.VoidCallback
import androidx.ui.foundation.ObserverList
import androidx.ui.foundation.assertions.FlutterError
import androidx.ui.runtimeType
import java.lang.Exception

/**
 * A class that can be extended or mixed in that provides a change notification
 * API using [VoidCallback] for notifications.
 *
 * [ChangeNotifier] is optimized for small numbers (one or two) of listeners.
 * It is O(N) for adding and removing listeners and O(N²) for dispatching
 * notifications (where N is the number of listeners).
 *
 * See also:
 *
 *  * [ValueNotifier], which is a [ChangeNotifier] that wraps a single value.
 */
open class ChangeNotifier : Listenable {
    var listeners: ObserverList<VoidCallback>? = ObserverList()

    // Chagned the method return value from boolean to void, since assertion is not enabled on
    // Android.
    private fun debugAssertNotDisposed() {
        assert(listeners != null) {
            "A $runtimeType was used asfter being disposed.\n" +
                    "Once you have called disposed() on $runtimeType, it can no longer be used."
        }
    }

    /**
     * Whether any listeners are currently registered.
     *
     * Clients should not depend on this value for their behavior, because having one listener's
     * logic change when another listener happens to start or stop listening will lead to extremely
     * hard-to-track bugs. Subclasses might use this information to determine whether to do any
     * work when there are no listeners, however; for example, resuming a [Stream] when a listener
     * is added and pausing it when a listener is removed.
     *
     * Typically this is used by overriding [addListener], checking if [hasListeners] is false
     * before calling `super.addListener()`, and if so, starting whatever work is needed to
     * determine when to call [notifyListeners]; and similarly, by overriding [removeListener],
     * checking if [hasListeners] is false after calling `super.removeListener()`, and if so,
     * stopping that same work.
     */
    fun hasListeners(): Boolean {
        debugAssertNotDisposed()
        return listeners?.let { return it.isNotEmpty() } ?: false
    }

    override fun addListener(listener: VoidCallback) {
        debugAssertNotDisposed()
        listeners?.let { it.add(listener) }
    }

    override fun removeListener(listener: VoidCallback) {
        debugAssertNotDisposed()
        listeners?.let { it.remove(listener) }
    }

    /**
     * Discards any resources used by the object. After this is called, the object is not in a
     * usable state and should be discarded (calls to [addListener] and [removeListener] will throw
     * after the object is disposed).
     *
     * This method should only be called by the object's owner.
     */
    @CallSuper
    open fun dispose() {
        debugAssertNotDisposed()
        listeners = null
    }

    /**
     * Call all the registered listeners.
     *
     * Call this method whenever the object changes, to notify any clients the object may have.
     * Listeners that are added during this iteration will not be visited. Listeners that are
     * removed during this iteration will not be visited after they are removed.
     *
     * Exceptions thrown by listeners will be caught and reported using [FlutterError.reportError].
     *
     * This method must not be called after [dispose] has been called.
     *
     * Surprising behavior can result when reentrantly removing a listener (i.e. in response to a
     * notification) that has been registered multiple times.
     * See the discussion at [removeListener].
     */
    protected fun notifyListeners() {
        debugAssertNotDisposed()
        val ref = listeners
        ref?.let {
            it.toList().forEach {
                try {
                    if (ref.contains(it)) {
                        it()
                    }
                } catch (e: Exception) {
                    TODO("Report errors.")
//                  FlutterError.reportError(new FlutterErrorDetails(
//                      exception: exception,
//                      stack: stack,
//                      library: 'foundation library',
//                      context: 'while dispatching notifications for $runtimeType',
//                      informationCollector: (StringBuffer information) {
//                          information.writeln('The $runtimeType sending notification was:');
//                          information.write('  $this');
//                      }
//                  ));
                }
            }
        }
    }
}
