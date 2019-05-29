/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.concurrent;

import androidx.annotation.NonNull;

import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

/**
 * A {@link Future} that accepts completion listeners. Each listener has an associated executor, and
 * it is invoked using this executor once the future's computation is {@linkplain Future#isDone()
 * complete}. If the computation has already completed when the listener is added, the listener will
 * execute immediately.
 *
 * <p>See the Guava User Guide article on <a
 * href="https://github.com/google/guava/wiki/ListenableFutureExplained">{@code
 * ListenableFuture}</a>.
 *
 * <p>This class is GWT-compatible.
 *
 * <h3>Purpose</h3>
 *
 * <p>The main purpose of {@code ListenableFuture} is to help you chain together a graph of
 * asynchronous operations. You can chain them together manually with calls to methods like
 * {@code Futures.transform}, but you will often find it easier to use a framework. Frameworks
 * automate the process, often adding features like monitoring, debugging, and cancellation.
 * Examples of frameworks include:
 *
 * <ul>
 *   <!-- MOE:begin_intracomment_strip -->
 *   <li><a href="http://go/javaproducers">Apps Framework Producers</a> (for server apps)
 *   <!-- MOE:end_intracomment_strip -->
 *   <li><a href="http://google.github.io/dagger/producers.html">Dagger Producers</a>
 *       <!-- MOE:begin_intracomment_strip -->
 *       (for Android apps)
 *   <li><a href="http://go/async-java">...and more</a>
 *       <!-- MOE:end_intracomment_strip -->
 * </ul>
 *
 * <p>The main purpose of {@link #addListener addListener} is to support this chaining. You will
 * rarely use it directly, in part because it does not provide direct access to the {@code Future}
 * result. (If you want such access, you may prefer {@code Futures.addCallback}.)
 * Still, direct {@code addListener} calls are occasionally useful:
 *
 * <pre>{@code
 * final String name = ...;
 * inFlight.add(name);
 * ListenableFuture<Result> future = service.query(name);
 * future.addListener(new Runnable() {
 *   public void run() {
 *     processedCount.incrementAndGet();
 *     inFlight.remove(name);
 *     lastProcessed.set(name);
 *     logger.info("Done with {0}", name);
 *   }
 * }, executor);
 * }</pre>
 *
 * <h3>How to get an instance</h3>
 *
 * <p>We encourage you to return {@code ListenableFuture} from your methods so that your users can
 * take advantage of the utilities built atop the class. The way that you will
 * create {@code ListenableFuture} instances depends on how you currently create {@code Future}
 * instances:
 *
 * <ul>
 *   <li>If you receive them from an {@code java.util.concurrent.ExecutorService}, convert that
 *       service to a {@code ListeningExecutorService}, usually by calling
 *       {@code MoreExecutors.listeningDecorator}.
 *   <li>If you manually call {@code FutureTask.set} or a similar method,
 *       create a {@code SettableFuture} instead. (If your needs are more complex, you may prefer
 *       {@code AbstractFuture}.)
 * </ul>
 *
 * <p><b>Test doubles</b>: If you need a {@code ListenableFuture} for your test, try a
 * {@code SettableFuture} or one of the methods in the {@code Futures.immediate*}
 * family. <b>Avoid</b> creating a mock or stub {@code Future}. Mock and stub implementations are
 * fragile because they assume that only certain methods will be called and because they often
 * implement subtleties of the API improperly.
 *
 * <p><b>Custom implementation</b>: Avoid implementing {@code ListenableFuture} from scratch. If you
 * can't get by with the standard implementations, prefer to derive a new {@code Future} instance
 * with the methods in {@code Futures} or, if necessary, to extend {@code AbstractFuture}.
 *
 * <p>Occasionally, an API will return a plain {@code Future} and it will be impossible to change
 * the return type. For this case, we provide a more expensive workaround in {@code
 * JdkFutureAdapters}. However, when possible, it is more efficient and reliable to create a {@code
 * ListenableFuture} directly.
 *
 * @author Sven Mawson
 * @author Nishant Thakkar
 * @since 1.0
 */
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
     * thrown by direct execution) will be caught and
     * logged.
     *
     * <p>Note: For fast, lightweight listeners that would be safe to execute in any thread, consider
     * {@code MoreExecutors.directExecutor}. Otherwise, avoid it. Heavyweight {@code directExecutor}
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
     * listeners, see {@code Futures}. For a simplified but general listener interface, see {@code
     * addCallback()}.
     *
     * <p>Memory consistency effects: Actions in a thread prior to adding a listener <a
     * href="https://docs.oracle.com/javase/specs/jls/se7/html/jls-17.html#jls-17.4.5">
     * <i>happen-before</i></a> its execution begins, perhaps in another thread.
     *
     * <p>Guava implementations of {@code ListenableFuture} promptly release references to listeners
     * after executing them.
     *
     * @param listener the listener to run when the computation is complete
     * @param executor the executor to run the listener in
     * @throws java.util.concurrent.RejectedExecutionException if we tried to execute the listener
     *     immediately but the executor rejected it.
     */
    void addListener(@NonNull Runnable listener, @NonNull Executor executor);
}
