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

package androidx.concurrent.listenablefuture;

import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

/**
 * Temporary version of guava's listenable. Going forward this module will depend
 * guava.listenable-future artifact, so we won't fork this interface
 *
 * @author Sven Mawson
 * @author Nishant Thakkar
 * @since 1.0
 */
@Deprecated
public interface ListenableFuture<V> extends Future<V> {
    /**
     * Registers a listener to be {@linkplain Executor#execute(Runnable) run} on the given executor.
     * The listener will run when the {@code Future}'s computation is {@linkplain Future#isDone()
     * complete} or, if the computation is already complete, immediately.
     *
     * <p>There is no guaranteed ordering of execution of listeners, but any listener added through
     * this method is guaranteed to be called once the computation is complete.
     *
     * <p>Exceptions thrown by a listener will be propagated up to the executor. Any exception thrown
     * during {@code Executor.execute} (e.g., a {@code RejectedExecutionException} or an exception
     * thrown by {@linkplain MoreExecutors#directExecutor direct execution}) will be caught and
     * logged.
     *
     * <p>Note: For fast, lightweight listeners that would be safe to execute in any thread, consider
     * {@link MoreExecutors#directExecutor}. Otherwise, avoid it. Heavyweight {@code directExecutor}
     * listeners can cause problems, and these problems can be difficult to reproduce because they
     * depend on timing. For example:
     *
     * <ul>
     *   <li>The listener may be executed by the caller of {@code addListener}. That caller may be a
     *       UI thread or other latency-sensitive thread. This can harm UI responsiveness.
     *   <li>The listener may be executed by the thread that completes this {@code Future}. That
     *       thread may be an internal system thread such as an RPC network thread. Blocking that
     *       thread may stall progress of the whole system. It may even cause a deadlock.
     *   <li>The listener may delay other listeners, even listeners that are not themselves {@code
     *       directExecutor} listeners.
     * </ul>
     *
     * <p>This is the most general listener interface. For common operations performed using
     * listeners, see {@link Futures}. For a simplified but general listener interface, see {@link
     * Futures#addCallback addCallback()}.
     *
     * <p>Memory consistency effects: Actions in a thread prior to adding a listener <a
     * href="https://docs.oracle.com/javase/specs/jls/se7/html/jls-17.html#jls-17.4.5">
     * <i>happen-before</i></a> its execution begins, perhaps in another thread.
     *
     * @param listener the listener to run when the computation is complete
     * @param executor the executor to run the listener in
     * @throws RejectedExecutionException if we tried to execute the listener immediately but the
     *     executor rejected it.
     */
    void addListener(Runnable listener, Executor executor);
}