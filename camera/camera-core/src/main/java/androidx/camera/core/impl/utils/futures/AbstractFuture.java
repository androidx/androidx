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

package androidx.camera.core.impl.utils.futures;

import static androidx.core.util.Preconditions.checkNotNull;

import static java.util.concurrent.atomic.AtomicReferenceFieldUpdater.newUpdater;

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.impl.utils.futures.internal.InternalFutureFailureAccess;
import androidx.camera.core.impl.utils.futures.internal.InternalFutures;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Locale;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.concurrent.locks.LockSupport;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Cloned from concurrent-futures package in Guava to AndroidX namespace since we would need
 * ListenableFuture related implementation but not want to include whole Guava library.
 *
 * TODO: b/130187641 Update ImageCapture and remove AbstractFuture.
 *
 * <p>This class implements all methods in {@code ListenableFuture}. Subclasses should provide a way
 * to set the result of the computation through the protected methods {@link #set(Object)}, {@link
 * #setFuture(ListenableFuture)} and {@link #setException(Throwable)}. Subclasses may also override
 * {@link #afterDone()}, which will be invoked automatically when the future completes. Subclasses
 * should rarely override other methods.
 *
 * @author Sven Mawson
 * @author Luke Sandberg
 * @hide
 * @since 1.0
 * @param <V>
 * @deprecated Temporarily to use this class to keep code in the same behavior, will remove after
 * ImageCapture update.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Deprecated
@SuppressWarnings("ShortCircuitBoolean") // we use non-short circuiting comparisons intentionally
public abstract class AbstractFuture<V> extends InternalFutureFailureAccess
        implements ListenableFuture<V> {
    // NOTE: Whenever both tests are cheap and functional, it's faster to use &, | instead of &&, ||

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static final boolean GENERATE_CANCELLATION_CAUSES =
            Boolean.parseBoolean(
                    System.getProperty("guava.concurrent.generate_cancellation_cause", "false"));

    /**
     * Tag interface marking trusted subclasses. This enables some optimizations. The implementation
     * of this interface must also be an AbstractFuture and must not override or expose for
     * overriding any of the public methods of ListenableFuture.
     */
    interface Trusted<V> extends ListenableFuture<V> {
    }

    /**
     * A less abstract subclass of AbstractFuture. This can be used to optimize setFuture by
     * ensuring
     * that {@link #get} calls exactly the implementation of {@link AbstractFuture#get}.
     */
    abstract static class TrustedFuture<V> extends AbstractFuture<V> implements Trusted<V> {
        @Override
        public final V get() throws InterruptedException, ExecutionException {
            return super.get();
        }

        @Override
        public final V get(long timeout, TimeUnit unit)
                throws InterruptedException, ExecutionException, TimeoutException {
            return super.get(timeout, unit);
        }

        @Override
        public final boolean isDone() {
            return super.isDone();
        }

        @Override
        public final boolean isCancelled() {
            return super.isCancelled();
        }

        @Override
        public final void addListener(Runnable listener, Executor executor) {
            super.addListener(listener, executor);
        }

        @Override
        public final boolean cancel(boolean mayInterruptIfRunning) {
            return super.cancel(mayInterruptIfRunning);
        }
    }

    // Logger to LOG exceptions caught when running listeners.
    private static final Logger LOG = Logger.getLogger(AbstractFuture.class.getName());

    // A heuristic for timed gets. If the remaining timeout is less than this, spin instead of
    // blocking. This value is what AbstractQueuedSynchronizer uses.
    private static final long SPIN_THRESHOLD_NANOS = 1000L;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static final AtomicHelper ATOMIC_HELPER;

    static {
        AtomicHelper helper;
        Throwable thrownAtomicReferenceFieldUpdaterFailure = null;

        try {
            helper =
                    new SafeAtomicHelper(
                            newUpdater(Waiter.class, Thread.class, "mThread"),
                            newUpdater(Waiter.class, Waiter.class, "mNext"),
                            newUpdater(AbstractFuture.class, Waiter.class, "mWaiters"),
                            newUpdater(AbstractFuture.class, Listener.class, "mListeners"),
                            newUpdater(AbstractFuture.class, Object.class, "mValue"));
        } catch (Throwable atomicReferenceFieldUpdaterFailure) {
            // Some Android 5.0.x Samsung devices have bugs in JDK reflection APIs that cause
            // getDeclaredField to throw a NoSuchFieldException when the field is definitely there.
            // For these users fallback to a suboptimal implementation, based on synchronized.
            // This will be a definite performance hit to those users.
            // MOE:begin_strip
            // See bugs b/25673935 and b/25650716
            // MOE:end_strip
            thrownAtomicReferenceFieldUpdaterFailure = atomicReferenceFieldUpdaterFailure;
            helper = new SynchronizedHelper();
        }
        ATOMIC_HELPER = helper;

        // Prevent rare disastrous classloading in first call to LockSupport.park.
        // See: https://bugs.openjdk.java.net/browse/JDK-8074773
        @SuppressWarnings("unused")
        Class<?> ensureLoaded = LockSupport.class;

        // Log after all static init is finished; if an installed logger uses any Futures
        // methods, it shouldn't break in cases where reflection is missing/broken.
        if (thrownAtomicReferenceFieldUpdaterFailure != null) {
            LOG.log(
                    Level.SEVERE, "SafeAtomicHelper is broken!",
                    thrownAtomicReferenceFieldUpdaterFailure);
        }
    }

    /** Waiter links form a Treiber stack, in the {@link #mWaiters} field. */
    private static final class Waiter {
        static final Waiter TOMBSTONE = new Waiter(false /* ignored param */);

        volatile @Nullable Thread mThread;
        volatile @Nullable Waiter mNext;

        /**
         * Constructor for the TOMBSTONE, avoids use of ATOMIC_HELPER in case this class is loaded
         * before the ATOMIC_HELPER. Apparently this is possible on some android platforms.
         */
        Waiter(boolean unused) {
        }

        Waiter() {
            // avoid volatile write, write is made visible by subsequent CAS on mWaiters field
            ATOMIC_HELPER.putThread(this, Thread.currentThread());
        }

        // non-volatile write to the mNext field. Should be made visible by subsequent CAS on
        // mWaiters field.
        void setNext(Waiter next) {
            ATOMIC_HELPER.putNext(this, next);
        }

        void unpark() {
            // This is racy with removeWaiter. The consequence of the race is that we may
            // spuriously call unpark even though the thread has already removed itself from the
            // list. But even if we did use a CAS, that race would still exist (it would just be
            // ever so slightly smaller).
            Thread w = mThread;
            if (w != null) {
                mThread = null;
                LockSupport.unpark(w);
            }
        }
    }

    /**
     * Marks the given node as 'deleted' (null waiter) and then scans the list to unlink all deleted
     * nodes. This is an O(n) operation in the common case (and O(n^2) in the worst), but we are
     * saved by two things.
     *
     * <ul>
     * <li>This is only called when a waiting thread times out or is interrupted. Both of which
     * should be rare.
     * <li>The waiters list should be very short.
     * </ul>
     */
    private void removeWaiter(Waiter node) {
        node.mThread = null; // mark as 'deleted'
        restart:
        while (true) {
            Waiter pred = null;
            Waiter curr = mWaiters;
            if (curr == Waiter.TOMBSTONE) {
                return; // give up if someone is calling complete
            }
            Waiter succ;
            while (curr != null) {
                succ = curr.mNext;
                if (curr.mThread != null) { // we aren't unlinking this node, update pred.
                    pred = curr;
                } else if (pred != null) { // We are unlinking this node and it has a predecessor.
                    pred.mNext = succ;
                    if (pred.mThread
                            == null) { // We raced with another node that unlinked pred. Restart.
                        continue restart;
                    }
                } else if (!ATOMIC_HELPER.casWaiters(this, curr, succ)) { // We are unlinking head
                    continue restart; // We raced with an add or complete
                }
                curr = succ;
            }
            break;
        }
    }

    /** Listeners also form a stack through the {@link #mListeners} field. */
    private static final class Listener {
        static final Listener TOMBSTONE = new Listener(null, null);
        final Runnable mTask;
        final Executor mExecutor;

        // writes to mNext are made visible by subsequent CAS's on the mListeners field
        @Nullable
        Listener mNext;

        Listener(Runnable task, Executor executor) {
            this.mTask = task;
            this.mExecutor = executor;
        }
    }

    /** A special value to represent {@code null}. */
    private static final Object NULL = new Object();

    /** A special value to represent failure, when {@link #setException} is called successfully. */
    private static final class Failure {
        static final Failure FALLBACK_INSTANCE = new Failure(
                new Throwable("Failure occurred while trying to finish a future.") {
                    @Override
                    public synchronized Throwable fillInStackTrace() {
                        return this; // no stack trace
                    }
                });
        final Throwable mException;

        Failure(Throwable exception) {
            this.mException = checkNotNull(exception);
        }
    }

    /** A special value to represent cancellation and the 'wasInterrupted' bit. */
    private static final class Cancellation {
        // constants to use when GENERATE_CANCELLATION_CAUSES = false
        static final Cancellation CAUSELESS_INTERRUPTED;
        static final Cancellation CAUSELESS_CANCELLED;

        static {
            if (GENERATE_CANCELLATION_CAUSES) {
                CAUSELESS_CANCELLED = null;
                CAUSELESS_INTERRUPTED = null;
            } else {
                CAUSELESS_CANCELLED = new Cancellation(false, null);
                CAUSELESS_INTERRUPTED = new Cancellation(true, null);
            }
        }

        final boolean mWasInterrupted;
        final @Nullable Throwable mCause;

        Cancellation(boolean wasInterrupted, @Nullable Throwable cause) {
            mWasInterrupted = wasInterrupted;
            mCause = cause;
        }
    }

    /** A special value that encodes the 'setFuture' state. */
    private static final class SetFuture<V> implements Runnable {
        final AbstractFuture<V> mOwner;
        final ListenableFuture<? extends V> mFuture;

        SetFuture(AbstractFuture<V> owner, ListenableFuture<? extends V> future) {
            mOwner = owner;
            mFuture = future;
        }

        @Override
        public void run() {
            if (mOwner.mValue != this) {
                // nothing to do, we must have been cancelled, don't bother inspecting the future.
                return;
            }
            Object valueToSet = getFutureValue(mFuture);
            if (ATOMIC_HELPER.casValue(mOwner, this, valueToSet)) {
                complete(mOwner);
            }
        }
    }

    // TODO(lukes): investigate using the @Contended annotation on these fields when jdk8 is
    // available.
    /**
     * This field encodes the current state of the future.
     *
     * <p>The valid values are:
     *
     * <ul>
     * <li>{@code null} initial state, nothing has happened.
     * <li>{@link Cancellation} terminal state, {@code cancel} was called.
     * <li>{@link Failure} terminal state, {@code setException} was called.
     * <li>{@link SetFuture} intermediate state, {@code setFuture} was called.
     * <li>{@link #NULL} terminal state, {@code set(null)} was called.
     * <li>Any other non-null value, terminal state, {@code set} was called with a non-null
     * argument.
     * </ul>
     */
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    volatile @Nullable Object mValue;

    /** All listeners. */
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    volatile @Nullable Listener mListeners;

    /** All waiting threads. */
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    volatile @Nullable Waiter mWaiters;

    /** Constructor for use by subclasses. */
    protected AbstractFuture() {
    }

    // Gets and Timed Gets
    //
    // * Be responsive to interruption
    // * Don't create Waiter nodes if you aren't going to park, this helps reduce contention on the
    //   mWaiters field.
    // * Future completion is defined by when #mValue becomes non-null/non SetFuture
    // * Future completion can be observed if the mWaiters field contains a TOMBSTONE

    // Timed Get
    // There are a few design constraints to consider
    // * We want to be responsive to small timeouts, unpark() has non trivial latency overheads (I
    //   have observed 12 micros on 64 bit linux systems to wake up a parked thread). So if the
    //   timeout is small we shouldn't park(). This needs to be traded off with the cpu overhead of
    //   spinning, so we use SPIN_THRESHOLD_NANOS which is what AbstractQueuedSynchronizer uses for
    //   similar purposes.
    // * We want to behave reasonably for timeouts of 0
    // * We are more responsive to completion than timeouts. This is because parkNanos depends on
    //   system scheduling and as such we could either miss our deadline, or unpark() could be
    //   delayed so that it looks like we timed out even though we didn't. For comparison FutureTask
    //   respects completion preferably and AQS is non-deterministic (depends on where in the
    //   queue the waiter is). If we wanted to be strict about it, we could store the unpark()
    //   time in the Waiter node and we could use that to make a decision about whether or not we
    //   timed out prior to being unparked.

    /**
     * {@inheritDoc}
     *
     * <p>The default {@link AbstractFuture} implementation throws {@code InterruptedException}
     * if the current thread is interrupted during the call, even if the value is already available.
     *
     * @throws CancellationException {@inheritDoc}
     */
    @Override
    public V get(long timeout, TimeUnit unit)
            throws InterruptedException, TimeoutException, ExecutionException {
        // NOTE: if timeout < 0, remainingNanos will be < 0 and we will fall into the while(true)
        // loop at the bottom and throw a timeoutexception.
        final long timeoutNanos = unit.toNanos(
                timeout); // we rely on the implicit null check on unit.
        long remainingNanos = timeoutNanos;
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        Object localValue = mValue;
        if (localValue != null & !(localValue instanceof SetFuture)) {
            return getDoneValue(localValue);
        }
        // we delay calling nanoTime until we know we will need to either park or spin
        final long endNanos = remainingNanos > 0 ? System.nanoTime() + remainingNanos : 0;
        long_wait_loop:
        if (remainingNanos >= SPIN_THRESHOLD_NANOS) {
            Waiter oldHead = mWaiters;
            if (oldHead != Waiter.TOMBSTONE) {
                Waiter node = new Waiter();
                do {
                    node.setNext(oldHead);
                    if (ATOMIC_HELPER.casWaiters(this, oldHead, node)) {
                        while (true) {
                            LockSupport.parkNanos(this, remainingNanos);
                            // Check interruption first, if we woke up due to interruption we
                            // need to honor that.
                            if (Thread.interrupted()) {
                                removeWaiter(node);
                                throw new InterruptedException();
                            }

                            // Otherwise re-read and check doneness. If we loop then it must have
                            // been a spurious wakeup
                            localValue = mValue;
                            if (localValue != null & !(localValue instanceof SetFuture)) {
                                return getDoneValue(localValue);
                            }

                            // timed out?
                            remainingNanos = endNanos - System.nanoTime();
                            if (remainingNanos < SPIN_THRESHOLD_NANOS) {
                                // Remove the waiter, one way or another we are done parking this
                                // thread.
                                removeWaiter(node);
                                break long_wait_loop; // jump down to the busy wait loop
                            }
                        }
                    }
                    oldHead = mWaiters; // re-read and loop.
                } while (oldHead != Waiter.TOMBSTONE);
            }
            // re-read value, if we get here then we must have observed a TOMBSTONE while trying
            // to add a
            // waiter.
            return getDoneValue(mValue);
        }
        // If we get here then we have remainingNanos < SPIN_THRESHOLD_NANOS and there is no node
        // on the
        // waiters list
        while (remainingNanos > 0) {
            localValue = mValue;
            if (localValue != null & !(localValue instanceof SetFuture)) {
                return getDoneValue(localValue);
            }
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            remainingNanos = endNanos - System.nanoTime();
        }

        String futureToString = toString();
        final String unitString = unit.toString().toLowerCase(Locale.ROOT);
        String message = "Waited " + timeout + " " + unit.toString().toLowerCase(Locale.ROOT);
        // Only report scheduling delay if larger than our spin threshold - otherwise it's just
        // noise
        if (remainingNanos + SPIN_THRESHOLD_NANOS < 0) {
            // We over-waited for our timeout.
            message += " (plus ";
            long overWaitNanos = -remainingNanos;
            long overWaitUnits = unit.convert(overWaitNanos, TimeUnit.NANOSECONDS);
            long overWaitLeftoverNanos = overWaitNanos - unit.toNanos(overWaitUnits);
            boolean shouldShowExtraNanos =
                    overWaitUnits == 0 || overWaitLeftoverNanos > SPIN_THRESHOLD_NANOS;
            if (overWaitUnits > 0) {
                message += overWaitUnits + " " + unitString;
                if (shouldShowExtraNanos) {
                    message += ",";
                }
                message += " ";
            }
            if (shouldShowExtraNanos) {
                message += overWaitLeftoverNanos + " nanoseconds ";
            }

            message += "delay)";
        }
        // It's confusing to see a completed future in a timeout message; if isDone() returns false,
        // then we know it must have given a pending toString value earlier. If not, then the future
        // completed after the timeout expired, and the message might be success.
        if (isDone()) {
            throw new TimeoutException(message + " but future completed as timeout expired");
        }
        throw new TimeoutException(message + " for " + futureToString);
    }

    /**
     * {@inheritDoc}
     *
     * <p>The default {@link AbstractFuture} implementation throws {@code InterruptedException}
     * if the current thread is interrupted during the call, even if the value is already available.
     *
     * @throws CancellationException {@inheritDoc}
     */
    @Override
    public V get() throws InterruptedException, ExecutionException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        Object localValue = mValue;
        if (localValue != null & !(localValue instanceof SetFuture)) {
            return getDoneValue(localValue);
        }
        Waiter oldHead = mWaiters;
        if (oldHead != Waiter.TOMBSTONE) {
            Waiter node = new Waiter();
            do {
                node.setNext(oldHead);
                if (ATOMIC_HELPER.casWaiters(this, oldHead, node)) {
                    // we are on the stack, now wait for completion.
                    while (true) {
                        LockSupport.park(this);
                        // Check interruption first, if we woke up due to interruption we need to
                        // honor that.
                        if (Thread.interrupted()) {
                            removeWaiter(node);
                            throw new InterruptedException();
                        }
                        // Otherwise re-read and check doneness. If we loop then it must have
                        // been a spurious
                        // wakeup
                        localValue = mValue;
                        if (localValue != null & !(localValue instanceof SetFuture)) {
                            return getDoneValue(localValue);
                        }
                    }
                }
                oldHead = mWaiters; // re-read and loop.
            } while (oldHead != Waiter.TOMBSTONE);
        }
        // re-read value, if we get here then we must have observed a TOMBSTONE while trying to
        // add a waiter.
        return getDoneValue(mValue);
    }

    /** Unboxes {@code obj}. Assumes that obj is not {@code null} or a {@link SetFuture}. */
    private V getDoneValue(Object obj) throws ExecutionException {
        // While this seems like it might be too branch-y, simple benchmarking proves it to be
        // unmeasurable (comparing done AbstractFutures with immediateFuture)
        if (obj instanceof Cancellation) {
            throw cancellationExceptionWithCause("Task was cancelled.",
                    ((Cancellation) obj).mCause);
        } else if (obj instanceof Failure) {
            throw new ExecutionException(((Failure) obj).mException);
        } else if (obj == NULL) {
            return null;
        } else {
            @SuppressWarnings("unchecked") // this is the only other option
                    V asV = (V) obj;
            return asV;
        }
    }

    @Override
    public boolean isDone() {
        final Object localValue = mValue;
        return localValue != null & !(localValue instanceof SetFuture);
    }

    @Override
    public boolean isCancelled() {
        final Object localValue = mValue;
        return localValue instanceof Cancellation;
    }

    /**
     * {@inheritDoc}
     *
     * <p>If a cancellation attempt succeeds on a {@code Future} that had previously been
     * {@linkplain
     * #setFuture set asynchronously}, then the cancellation will also be propagated to the delegate
     * {@code Future} that was supplied in the {@code setFuture} call.
     *
     * <p>Rather than override this method to perform additional cancellation work or cleanup,
     * subclasses should override {@link #afterDone}, consulting {@link #isCancelled} and {@link
     * #wasInterrupted} as necessary. This ensures that the work is done even if the future is
     * cancelled without a call to {@code cancel}, such as by calling {@code
     * setFuture(cancelledFuture)}.
     */
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        Object localValue = mValue;
        boolean rValue = false;
        if (localValue == null | localValue instanceof SetFuture) {
            // Try to delay allocating the exception. At this point we may still lose the CAS,
            // but it is certainly less likely.
            Object valueToSet =
                    GENERATE_CANCELLATION_CAUSES
                            ? new Cancellation(
                            mayInterruptIfRunning,
                            new CancellationException("Future.cancel() was called."))
                            : (mayInterruptIfRunning
                                    ? Cancellation.CAUSELESS_INTERRUPTED
                                    : Cancellation.CAUSELESS_CANCELLED);
            AbstractFuture<?> abstractFuture = this;
            while (true) {
                if (ATOMIC_HELPER.casValue(abstractFuture, localValue, valueToSet)) {
                    rValue = true;
                    // We call interuptTask before calling complete(), which is consistent with
                    // FutureTask
                    if (mayInterruptIfRunning) {
                        abstractFuture.interruptTask();
                    }
                    complete(abstractFuture);
                    if (localValue instanceof SetFuture) {
                        // propagate cancellation to the future set in setfuture, this is racy,
                        // and we don't care if we are successful or not.
                        ListenableFuture<?> futureToPropagateTo = ((SetFuture) localValue).mFuture;
                        if (futureToPropagateTo instanceof Trusted) {
                            // If the future is a TrustedFuture then we specifically avoid
                            // calling cancel() this has 2 benefits
                            // 1. for long chains of futures strung together with setFuture we
                            // consume less stack
                            // 2. we avoid allocating Cancellation objects at every level of the
                            // cancellation chain
                            // We can only do this for TrustedFuture, because TrustedFuture
                            // .cancel is final and does nothing but delegate to this method.
                            AbstractFuture<?> trusted = (AbstractFuture<?>) futureToPropagateTo;
                            localValue = trusted.mValue;
                            if (localValue == null | localValue instanceof SetFuture) {
                                abstractFuture = trusted;
                                continue; // loop back up and try to complete the new future
                            }
                        } else {
                            // not a TrustedFuture, call cancel directly.
                            futureToPropagateTo.cancel(mayInterruptIfRunning);
                        }
                    }
                    break;
                }
                // obj changed, reread
                localValue = abstractFuture.mValue;
                if (!(localValue instanceof SetFuture)) {
                    // obj cannot be null at this point, because value can only change from null
                    // to non-null.
                    // So if value changed (and it did since we lost the CAS), then it cannot be
                    // null and since it isn't a SetFuture, then the future must be done and we
                    // should exit the loop
                    break;
                }
            }
        }
        return rValue;
    }

    /**
     * Subclasses can override this method to implement interruption of the future's computation.
     * The method is invoked automatically by a successful call to {@link #cancel(boolean) cancel
     * (true)}.
     *
     * <p>The default implementation does nothing.
     *
     * <p>This method is likely to be deprecated. Prefer to override {@link #afterDone}, checking
     * {@link #wasInterrupted} to decide whether to interrupt your task.
     *
     * @since 10.0
     */
    protected void interruptTask() {
    }

    /**
     * Returns true if this future was cancelled with {@code mayInterruptIfRunning} set to {@code
     * true}.
     *
     * @since 14.0
     */
    protected final boolean wasInterrupted() {
        final Object localValue = mValue;
        return (localValue instanceof Cancellation) && ((Cancellation) localValue).mWasInterrupted;
    }

    /**
     * {@inheritDoc}
     *
     * @since 10.0
     */
    @Override
    public void addListener(Runnable listener, Executor executor) {
        checkNotNull(listener, "Runnable was null.");
        checkNotNull(executor, "Executor was null.");
        // Checking isDone and listeners != TOMBSTONE may seem redundant, but our contract for
        // addListener says that listeners execute 'immediate' if the future isDone(). However, our
        // protocol for completing a future is to assign the value field (which sets isDone to
        // true) and
        // then to release waiters, followed by executing afterDone(), followed by releasing
        // listeners.
        // That means that it is possible to observe that the future isDone and that your listeners
        // don't execute 'immediately'.  By checking isDone here we avoid that.
        // A corollary to all that is that we don't need to check isDone inside the loop because
        // if we
        // get into the loop we know that we weren't done when we entered and therefore we aren't
        // under
        // an obligation to execute 'immediately'.
        if (!isDone()) {
            Listener oldHead = mListeners;
            if (oldHead != Listener.TOMBSTONE) {
                Listener newNode = new Listener(listener, executor);
                do {
                    newNode.mNext = oldHead;
                    if (ATOMIC_HELPER.casListeners(this, oldHead, newNode)) {
                        return;
                    }
                    oldHead = mListeners; // re-read
                } while (oldHead != Listener.TOMBSTONE);
            }
        }
        // If we get here then the Listener TOMBSTONE was set, which means the future is done, call
        // the listener.
        executeListener(listener, executor);
    }

    /**
     * Sets the result of this {@code Future} unless this {@code Future} has already been
     * cancelled or
     * set (including {@linkplain #setFuture set asynchronously}). When a call to this method
     * returns,
     * the {@code Future} is guaranteed to be {@linkplain #isDone done} <b>only if</b> the call was
     * accepted (in which case it returns {@code true}). If it returns {@code false}, the {@code
     * Future} may have previously been set asynchronously, in which case its result may not be
     * known
     * yet. That result, though not yet known, cannot be overridden by a call to a {@code set*}
     * method, only by a call to {@link #cancel}.
     *
     * @param value the value to be used as the result
     * @return true if the attempt was accepted, completing the {@code Future}
     */
    protected boolean set(@Nullable V value) {
        Object valueToSet = value == null ? NULL : value;
        if (ATOMIC_HELPER.casValue(this, null, valueToSet)) {
            complete(this);
            return true;
        }
        return false;
    }

    /**
     * Sets the failed result of this {@code Future} unless this {@code Future} has already been
     * cancelled or set (including {@linkplain #setFuture set asynchronously}). When a call to this
     * method returns, the {@code Future} is guaranteed to be {@linkplain #isDone done} <b>only
     * if</b>
     * the call was accepted (in which case it returns {@code true}). If it returns {@code false}
     * , the
     * {@code Future} may have previously been set asynchronously, in which case its result may
     * not be
     * known yet. That result, though not yet known, cannot be overridden by a call to a {@code
     * set*}
     * method, only by a call to {@link #cancel}.
     *
     * @param throwable the mException to be used as the failed result
     * @return true if the attempt was accepted, completing the {@code Future}
     */
    protected boolean setException(Throwable throwable) {
        Object valueToSet = new Failure(checkNotNull(throwable));
        if (ATOMIC_HELPER.casValue(this, null, valueToSet)) {
            complete(this);
            return true;
        }
        return false;
    }

    /**
     * Sets the result of this {@code Future} to match the supplied input {@code Future} once the
     * supplied {@code Future} is done, unless this {@code Future} has already been cancelled or set
     * (including "set asynchronously," defined below).
     *
     * <p>If the supplied future is {@linkplain #isDone done} when this method is called and the
     * call
     * is accepted, then this future is guaranteed to have been completed with the supplied
     * future by
     * the time this method returns. If the supplied future is not done and the call is accepted,
     * then
     * the future will be <i>set asynchronously</i>. Note that such a result, though not yet known,
     * cannot be overridden by a call to a {@code set*} method, only by a call to {@link #cancel}.
     *
     * <p>If the call {@code setFuture(delegate)} is accepted and this {@code Future} is later
     * cancelled, cancellation will be propagated to {@code delegate}. Additionally, any call to
     * {@code setFuture} after any cancellation will propagate cancellation to the supplied {@code
     * Future}.
     *
     * <p>Note that, even if the supplied future is cancelled and it causes this future to complete,
     * it will never trigger interruption behavior. In particular, it will not cause this future to
     * invoke the {@link #interruptTask} method, and the {@link #wasInterrupted} method will not
     * return {@code true}.
     *
     * @param future the future to delegate to
     * @return true if the attempt was accepted, indicating that the {@code Future} was not
     * previously
     * cancelled or set.
     * @since 19.0
     */
    protected boolean setFuture(ListenableFuture<? extends V> future) {
        checkNotNull(future);
        Object localValue = mValue;
        if (localValue == null) {
            if (future.isDone()) {
                Object value = getFutureValue(future);
                if (ATOMIC_HELPER.casValue(this, null, value)) {
                    complete(this);
                    return true;
                }
                return false;
            }
            SetFuture valueToSet = new SetFuture<V>(this, future);
            if (ATOMIC_HELPER.casValue(this, null, valueToSet)) {
                // the listener is responsible for calling completeWithFuture, directExecutor is
                // appropriate
                // since all we are doing is unpacking a completed future which should be fast.
                try {
                    future.addListener(valueToSet, CameraXExecutors.directExecutor());
                } catch (Throwable t) {
                    // addListener has thrown an exception! SetFuture.run can't throw any
                    // exceptions so this
                    // must have been caused by addListener itself. The most likely explanation is a
                    // misconfigured mock. Try to switch to Failure.
                    Failure failure;
                    try {
                        failure = new Failure(t);
                    } catch (Throwable oomMostLikely) {
                        failure = Failure.FALLBACK_INSTANCE;
                    }
                    // Note: The only way this CAS could fail is if cancel() has raced with us.
                    // That is ok.
                    boolean unused = ATOMIC_HELPER.casValue(this, valueToSet, failure);
                }
                return true;
            }
            localValue = mValue; // we lost the cas, fall through and maybe cancel
        }
        // The future has already been set to something. If it is cancellation we should cancel the
        // incoming future.
        if (localValue instanceof Cancellation) {
            // we don't care if it fails, this is best-effort.
            future.cancel(((Cancellation) localValue).mWasInterrupted);
        }
        return false;
    }

    /**
     * Returns a value that satisfies the contract of the {@link #mValue} field based on the state
     * of given future.
     *
     * <p>This is approximately the inverse of {@link #getDoneValue(Object)}
     */
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static Object getFutureValue(ListenableFuture<?> future) {
        if (future instanceof Trusted) {
            // Break encapsulation for TrustedFuture instances since we know that subclasses cannot
            // override .get() (since it is final) and therefore this is equivalent to calling
            // .get()
            // and unpacking the exceptions like we do below (just much faster because it is a
            // single
            // field read instead of a read, several branches and possibly creating exceptions).
            Object v = ((AbstractFuture<?>) future).mValue;
            if (v instanceof Cancellation) {
                // If the other future was interrupted, clear the interrupted bit while
                // preserving the cause
                // this will make it consistent with how non-trustedfutures work which cannot
                // propagate the
                // wasInterrupted bit
                Cancellation c = (Cancellation) v;
                if (c.mWasInterrupted) {
                    v =
                            c.mCause != null
                                    ? new Cancellation(/* mWasInterrupted= */ false, c.mCause)
                                    : Cancellation.CAUSELESS_CANCELLED;
                }
            }
            return v;
        }
        if (future instanceof InternalFutureFailureAccess) {
            Throwable throwable =
                    InternalFutures.tryInternalFastPathGetFailure(
                            (InternalFutureFailureAccess) future);
            if (throwable != null) {
                return new Failure(throwable);
            }
        }
        boolean wasCancelled = future.isCancelled();
        // Don't allocate a CancellationException if it's not necessary
        if (!GENERATE_CANCELLATION_CAUSES & wasCancelled) {
            return Cancellation.CAUSELESS_CANCELLED;
        }
        // Otherwise calculate the value by calling .get()
        try {
            Object v = getUninterruptibly(future);
            if (wasCancelled) {
                return new Cancellation(
                        false,
                        new IllegalArgumentException(
                                "get() did not throw CancellationException, despite reporting "
                                        + "isCancelled() == true: "
                                        + future));
            }
            return v == null ? NULL : v;
        } catch (ExecutionException exception) {
            if (wasCancelled) {
                return new Cancellation(
                        false,
                        new IllegalArgumentException(
                                "get() did not throw CancellationException, despite reporting "
                                        + "isCancelled() == true: "
                                        + future,
                                exception));
            }
            return new Failure(exception.getCause());
        } catch (CancellationException cancellation) {
            if (!wasCancelled) {
                return new Failure(
                        new IllegalArgumentException(
                                "get() threw CancellationException, despite reporting isCancelled"
                                        + "() == false: "
                                        + future,
                                cancellation));
            }
            return new Cancellation(false, cancellation);
        } catch (Throwable t) {
            return new Failure(t);
        }
    }

    private static <V> V getUninterruptibly(Future<V> future) throws ExecutionException {
        boolean interrupted = false;
        try {
            while (true) {
                try {
                    return future.get();
                } catch (InterruptedException e) {
                    interrupted = true;
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /** Unblocks all threads and runs all listeners. */
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static void complete(AbstractFuture<?> future) {
        Listener next = null;
        outer:
        while (true) {
            future.releaseWaiters();
            // We call this before the listeners in order to avoid needing to manage a separate
            // stack data
            // structure for them.  Also, some implementations rely on this running prior to
            // listeners
            // so that the cleanup work is visible to listeners.
            // afterDone() should be generally fast and only used for cleanup work... but in
            // theory can
            // also be recursive and create StackOverflowErrors
            future.afterDone();
            // push the current set of listeners onto next
            next = future.clearListeners(next);
            future = null;
            while (next != null) {
                Listener curr = next;
                next = next.mNext;
                Runnable task = curr.mTask;
                if (task instanceof SetFuture) {
                    SetFuture<?> setFuture = (SetFuture<?>) task;
                    // We unwind setFuture specifically to avoid StackOverflowErrors in the case
                    // of long
                    // chains of SetFutures
                    // Handling this special case is important because there is no way to pass an
                    // executor to
                    // setFuture, so a user couldn't break the chain by doing this themselves.
                    // It is also
                    // potentially common if someone writes a recursive Futures.transformAsync
                    // transformer.
                    future = setFuture.mOwner;
                    if (future.mValue == setFuture) {
                        Object valueToSet = getFutureValue(setFuture.mFuture);
                        if (ATOMIC_HELPER.casValue(future, setFuture, valueToSet)) {
                            continue outer;
                        }
                    }
                    // other wise the future we were trying to set is already done.
                } else {
                    executeListener(task, curr.mExecutor);
                }
            }
            break;
        }
    }

    /**
     * Callback method that is called exactly once after the future is completed.
     *
     * <p>If {@link #interruptTask} is also run during completion, {@link #afterDone} runs after it.
     *
     * <p>The default implementation of this method in {@code AbstractFuture} does nothing. This is
     * intended for very lightweight cleanup work, for example, timing statistics or clearing
     * fields.
     * If your task does anything heavier consider, just using a listener with an executor.
     *
     * @since 20.0
     */
    protected void afterDone() {
    }

    // TODO(b/114236866): Inherit doc from InternalFutureFailureAccess. Also, -link to its URL.

    /**
     * Usually returns {@code null} but, if this {@code Future} has failed, may <i>optionally</i>
     * return the cause of the failure. "Failure" means specifically "completed with an
     * exception"; it does not include "was cancelled." To be explicit: If this method returns a
     * non-null value,
     * then:
     *
     * <ul>
     * <li>{@code isDone()} must return {@code true}
     * <li>{@code isCancelled()} must return {@code false}
     * <li>{@code get()} must not block, and it must throw an {@code ExecutionException} with the
     * return value of this method as its cause
     * </ul>
     *
     * <p>This method is {@code protected} so that classes like {@code
     * com.google.common.util.concurrent.SettableFuture} do not expose it to their users as an
     * instance method. In the unlikely event that you need to call this method, call {@link
     * InternalFutures#tryInternalFastPathGetFailure(InternalFutureFailureAccess)}.
     *
     * @since 27.0
     */
    @Override
    @Nullable
    protected final Throwable tryInternalFastPathGetFailure() {
        if (this instanceof Trusted) {
            Object obj = mValue;
            if (obj instanceof Failure) {
                return ((Failure) obj).mException;
            }
        }
        return null;
    }

    /**
     * If this future has been cancelled (and possibly interrupted), cancels (and possibly
     * interrupts)
     * the given future (if available).
     */
    final void maybePropagateCancellationTo(@Nullable Future<?> related) {
        if (related != null & isCancelled()) {
            related.cancel(wasInterrupted());
        }
    }

    /** Releases all threads in the {@link #mWaiters} list, and clears the list. */
    private void releaseWaiters() {
        Waiter head;
        do {
            head = mWaiters;
        } while (!ATOMIC_HELPER.casWaiters(this, head, Waiter.TOMBSTONE));
        for (Waiter currentWaiter = head; currentWaiter != null;
                currentWaiter = currentWaiter.mNext) {
            currentWaiter.unpark();
        }
    }

    /**
     * Clears the {@link #mListeners} list and prepends its contents to {@code onto}, least recently
     * added first.
     */
    private Listener clearListeners(Listener onto) {
        // We need to
        // 1. atomically swap the listeners with TOMBSTONE, this is because addListener uses that to
        //    to synchronize with us
        // 2. reverse the linked list, because despite our rather clear contract, people depend
        // on us
        //    executing listeners in the order they were added
        // 3. push all the items onto 'onto' and return the new head of the stack
        Listener head;
        do {
            head = mListeners;
        } while (!ATOMIC_HELPER.casListeners(this, head, Listener.TOMBSTONE));
        Listener reversedList = onto;
        while (head != null) {
            Listener tmp = head;
            head = head.mNext;
            tmp.mNext = reversedList;
            reversedList = tmp;
        }
        return reversedList;
    }

    // TODO(clm): move parts into a default method on ListenableFuture?
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder().append(super.toString()).append("[status=");
        if (isCancelled()) {
            builder.append("CANCELLED");
        } else if (isDone()) {
            addDoneString(builder);
        } else {
            String pendingDescription;
            try {
                pendingDescription = pendingToString();
            } catch (RuntimeException e) {
                // Don't call getMessage or toString() on the exception, in case the exception
                // thrown by the
                // subclass is implemented with bugs similar to the subclass.
                pendingDescription = "Exception thrown from implementation: " + e.getClass();
            }
            // The future may complete during or before the call to getPendingToString, so we use
            // null
            // as a signal that we should try checking if the future is done again.
            if (pendingDescription != null && !pendingDescription.isEmpty()) {
                builder.append("PENDING, info=[").append(pendingDescription).append("]");
            } else if (isDone()) {
                addDoneString(builder);
            } else {
                builder.append("PENDING");
            }
        }
        return builder.append("]").toString();
    }

    /**
     * Provide a human-readable explanation of why this future has not yet completed.
     *
     * @return null if an explanation cannot be provided because the future is done.
     * @since 23.0
     */
    protected @Nullable String pendingToString() {
        Object localValue = mValue;
        if (localValue instanceof SetFuture) {
            return "setFuture=[" + userObjectToString(((SetFuture) localValue).mFuture) + "]";
        } else if (this instanceof ScheduledFuture) {
            return "remaining delay=["
                    + ((ScheduledFuture) this).getDelay(TimeUnit.MILLISECONDS)
                    + " ms]";
        }
        return null;
    }

    private void addDoneString(StringBuilder builder) {
        try {
            V value = getUninterruptibly(this);
            builder.append("SUCCESS, result=[").append(userObjectToString(value)).append("]");
        } catch (ExecutionException e) {
            builder.append("FAILURE, mCause=[").append(e.getCause()).append("]");
        } catch (CancellationException e) {
            builder.append("CANCELLED"); // shouldn't be reachable
        } catch (RuntimeException e) {
            builder.append("UNKNOWN, mCause=[").append(e.getClass()).append(" thrown from get()]");
        }
    }

    /** Helper for printing user supplied objects into our toString method. */
    private String userObjectToString(Object o) {
        // This is some basic recursion detection for when people create cycles via set/setFuture
        // This is however only partial protection though since it only detects self loops.  We
        // could detect arbitrary cycles using a thread local or possibly by catching
        // StackOverflowExceptions but this should be a good enough solution (it is also what jdk
        // collections do in these cases)
        if (o == this) {
            return "this future";
        }
        return String.valueOf(o);
    }

    /**
     * Submits the given runnable to the given {@link Executor} catching and logging all {@linkplain
     * RuntimeException runtime exceptions} thrown by the executor.
     */
    private static void executeListener(Runnable runnable, Executor executor) {
        try {
            executor.execute(runnable);
        } catch (RuntimeException e) {
            // Log it and keep going -- bad runnable and/or executor. Don't punish the other
            // runnables if we're given a bad one. We only catch RuntimeException because we want
            // Errors to propagate up.
            LOG.log(
                    Level.SEVERE,
                    "RuntimeException while executing runnable " + runnable + " with executor "
                            + executor,
                    e);
        }
    }

    private abstract static class AtomicHelper {
        /** Non volatile write of the thread to the {@link Waiter#mThread} field. */
        abstract void putThread(Waiter waiter, Thread newValue);

        /** Non volatile write of the waiter to the {@link Waiter#mNext} field. */
        abstract void putNext(Waiter waiter, Waiter newValue);

        /** Performs a CAS operation on the {@link #mWaiters} field. */
        abstract boolean casWaiters(AbstractFuture<?> future, Waiter expect, Waiter update);

        /** Performs a CAS operation on the {@link #mListeners} field. */
        abstract boolean casListeners(AbstractFuture<?> future, Listener expect, Listener update);

        /** Performs a CAS operation on the {@link #mValue} field. */
        abstract boolean casValue(AbstractFuture<?> future, Object expect, Object update);
    }

    /** {@link AtomicHelper} based on {@link AtomicReferenceFieldUpdater}. */
    private static final class SafeAtomicHelper extends AtomicHelper {
        final AtomicReferenceFieldUpdater<Waiter, Thread> mWaiterThreadUpdater;
        final AtomicReferenceFieldUpdater<Waiter, Waiter> mWaiterNextUpdater;
        final AtomicReferenceFieldUpdater<AbstractFuture, Waiter> mWaitersUpdater;
        final AtomicReferenceFieldUpdater<AbstractFuture, Listener> mListenersUpdater;
        final AtomicReferenceFieldUpdater<AbstractFuture, Object> mValueUpdater;

        SafeAtomicHelper(
                AtomicReferenceFieldUpdater<Waiter, Thread> waiterThreadUpdater,
                AtomicReferenceFieldUpdater<Waiter, Waiter> waiterNextUpdater,
                AtomicReferenceFieldUpdater<AbstractFuture, Waiter> waitersUpdater,
                AtomicReferenceFieldUpdater<AbstractFuture, Listener> listenersUpdater,
                AtomicReferenceFieldUpdater<AbstractFuture, Object> valueUpdater) {
            mWaiterThreadUpdater = waiterThreadUpdater;
            mWaiterNextUpdater = waiterNextUpdater;
            mWaitersUpdater = waitersUpdater;
            mListenersUpdater = listenersUpdater;
            mValueUpdater = valueUpdater;
        }

        @Override
        void putThread(Waiter waiter, Thread newValue) {
            mWaiterThreadUpdater.lazySet(waiter, newValue);
        }

        @Override
        void putNext(Waiter waiter, Waiter newValue) {
            mWaiterNextUpdater.lazySet(waiter, newValue);
        }

        @Override
        boolean casWaiters(AbstractFuture<?> future, Waiter expect, Waiter update) {
            return mWaitersUpdater.compareAndSet(future, expect, update);
        }

        @Override
        boolean casListeners(AbstractFuture<?> future, Listener expect, Listener update) {
            return mListenersUpdater.compareAndSet(future, expect, update);
        }

        @Override
        boolean casValue(AbstractFuture<?> future, Object expect, Object update) {
            return mValueUpdater.compareAndSet(future, expect, update);
        }
    }

    /**
     * {@link AtomicHelper} based on {@code synchronized} and volatile writes.
     *
     * <p>This is an implementation of last resort for when certain basic VM features are broken
     * (like
     * AtomicReferenceFieldUpdater).
     */
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static final class SynchronizedHelper extends AtomicHelper {
        @Override
        void putThread(Waiter waiter, Thread newValue) {
            waiter.mThread = newValue;
        }

        @Override
        void putNext(Waiter waiter, Waiter newValue) {
            waiter.mNext = newValue;
        }

        @Override
        boolean casWaiters(AbstractFuture<?> future, Waiter expect, Waiter update) {
            synchronized (future) {
                if (future.mWaiters == expect) {
                    future.mWaiters = update;
                    return true;
                }
                return false;
            }
        }

        @Override
        boolean casListeners(AbstractFuture<?> future, Listener expect, Listener update) {
            synchronized (future) {
                if (future.mListeners == expect) {
                    future.mListeners = update;
                    return true;
                }
                return false;
            }
        }

        @Override
        boolean casValue(AbstractFuture<?> future, Object expect, Object update) {
            synchronized (future) {
                if (future.mValue == expect) {
                    future.mValue = update;
                    return true;
                }
                return false;
            }
        }
    }

    private static CancellationException cancellationExceptionWithCause(
            @Nullable String message, @Nullable Throwable cause) {
        CancellationException exception = new CancellationException(message);
        exception.initCause(cause);
        return exception;
    }
}
