/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.room.rxjava3;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.ArchTaskExecutor;
import androidx.arch.core.executor.testing.CountingTaskExecutorRule;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.subscribers.TestSubscriber;

@RunWith(JUnit4.class)
public class RxRoomTest {
    @Rule
    public CountingTaskExecutorRule mExecutor = new CountingTaskExecutorRule();

    private RoomDatabase mDatabase;
    private InvalidationTracker mInvalidationTracker;
    private List<InvalidationTracker.Observer> mAddedObservers = new ArrayList<>();

    @Before
    public void init() {
        mDatabase = mock(RoomDatabase.class);
        mInvalidationTracker = mock(InvalidationTracker.class);
        when(mDatabase.getInvalidationTracker()).thenReturn(mInvalidationTracker);
        when(mDatabase.getQueryExecutor()).thenReturn(ArchTaskExecutor.getIOThreadExecutor());
        doAnswer(invocation -> {
            mAddedObservers.add((InvalidationTracker.Observer) invocation.getArguments()[0]);
            return null;
        }).when(mInvalidationTracker).addObserver(any(InvalidationTracker.Observer.class));
    }

    @Test
    public void basicAddRemove_Flowable() {
        Flowable<Object> flowable = RxRoom.createFlowable(mDatabase, "a", "b");
        verify(mInvalidationTracker, never()).addObserver(any(InvalidationTracker.Observer.class));
        Disposable disposable = flowable.subscribe();
        verify(mInvalidationTracker).addObserver(any(InvalidationTracker.Observer.class));
        assertThat(mAddedObservers.size(), CoreMatchers.is(1));

        InvalidationTracker.Observer observer = mAddedObservers.get(0);
        disposable.dispose();

        verify(mInvalidationTracker).removeObserver(observer);

        disposable = flowable.subscribe();
        verify(mInvalidationTracker, times(2))
                .addObserver(any(InvalidationTracker.Observer.class));
        assertThat(mAddedObservers.size(), CoreMatchers.is(2));
        assertThat(mAddedObservers.get(1), CoreMatchers.not(CoreMatchers.sameInstance(observer)));
        InvalidationTracker.Observer observer2 = mAddedObservers.get(1);
        disposable.dispose();
        verify(mInvalidationTracker).removeObserver(observer2);
    }

    @Test
    public void basicAddRemove_Observable() {
        Observable<Object> observable = RxRoom.createObservable(mDatabase, "a", "b");
        verify(mInvalidationTracker, never()).addObserver(any(InvalidationTracker.Observer.class));
        Disposable disposable = observable.subscribe();
        verify(mInvalidationTracker).addObserver(any(InvalidationTracker.Observer.class));
        assertThat(mAddedObservers.size(), CoreMatchers.is(1));

        InvalidationTracker.Observer observer = mAddedObservers.get(0);
        disposable.dispose();

        verify(mInvalidationTracker).removeObserver(observer);

        disposable = observable.subscribe();
        verify(mInvalidationTracker, times(2))
                .addObserver(any(InvalidationTracker.Observer.class));
        assertThat(mAddedObservers.size(), CoreMatchers.is(2));
        assertThat(mAddedObservers.get(1), CoreMatchers.not(CoreMatchers.sameInstance(observer)));
        InvalidationTracker.Observer observer2 = mAddedObservers.get(1);
        disposable.dispose();
        verify(mInvalidationTracker).removeObserver(observer2);
    }

    @Test
    public void basicNotify_Flowable() {
        String[] tables = {"a", "b"};
        Set<String> tableSet = new HashSet<>(Arrays.asList(tables));
        Flowable<Object> flowable = RxRoom.createFlowable(mDatabase, tables);
        CountingConsumer consumer = new CountingConsumer();
        Disposable disposable = flowable.subscribe(consumer);
        assertThat(mAddedObservers.size(), CoreMatchers.is(1));
        InvalidationTracker.Observer observer = mAddedObservers.get(0);
        assertThat(consumer.mCount, CoreMatchers.is(1));
        observer.onInvalidated(tableSet);
        assertThat(consumer.mCount, CoreMatchers.is(2));
        observer.onInvalidated(tableSet);
        assertThat(consumer.mCount, CoreMatchers.is(3));
        disposable.dispose();
        observer.onInvalidated(tableSet);
        assertThat(consumer.mCount, CoreMatchers.is(3));
    }

    @Test
    public void basicNotify_Observable() {
        String[] tables = {"a", "b"};
        Set<String> tableSet = new HashSet<>(Arrays.asList(tables));
        Observable<Object> observable = RxRoom.createObservable(mDatabase, tables);
        CountingConsumer consumer = new CountingConsumer();
        Disposable disposable = observable.subscribe(consumer);
        assertThat(mAddedObservers.size(), CoreMatchers.is(1));
        InvalidationTracker.Observer observer = mAddedObservers.get(0);
        assertThat(consumer.mCount, CoreMatchers.is(1));
        observer.onInvalidated(tableSet);
        assertThat(consumer.mCount, CoreMatchers.is(2));
        observer.onInvalidated(tableSet);
        assertThat(consumer.mCount, CoreMatchers.is(3));
        disposable.dispose();
        observer.onInvalidated(tableSet);
        assertThat(consumer.mCount, CoreMatchers.is(3));
    }

    @Test
    public void internalCallable_Flowable() throws Exception {
        final AtomicReference<String> value = new AtomicReference<>(null);
        String[] tables = {"a", "b"};
        Set<String> tableSet = new HashSet<>(Arrays.asList(tables));
        final Flowable<String> flowable = RxRoom.createFlowable(
                mDatabase, false, tables, value::get);
        final CountingConsumer consumer = new CountingConsumer();
        final Disposable disposable = flowable.subscribe(consumer);
        drain();
        InvalidationTracker.Observer observer = mAddedObservers.get(0);
        // no value because it is null
        assertThat(consumer.mCount, CoreMatchers.is(0));
        value.set("bla");
        observer.onInvalidated(tableSet);
        drain();
        // get value
        assertThat(consumer.mCount, CoreMatchers.is(1));
        observer.onInvalidated(tableSet);
        drain();
        // get value
        assertThat(consumer.mCount, CoreMatchers.is(2));
        value.set(null);
        observer.onInvalidated(tableSet);
        drain();
        // no value
        assertThat(consumer.mCount, CoreMatchers.is(2));
        disposable.dispose();
    }

    @Test
    public void internalCallable_Observable() throws Exception {
        final AtomicReference<String> value = new AtomicReference<>(null);
        String[] tables = {"a", "b"};
        Set<String> tableSet = new HashSet<>(Arrays.asList(tables));
        final Observable<String> flowable = RxRoom.createObservable(
                mDatabase, false, tables, value::get);
        final CountingConsumer consumer = new CountingConsumer();
        final Disposable disposable = flowable.subscribe(consumer);
        drain();
        InvalidationTracker.Observer observer = mAddedObservers.get(0);
        // no value because it is null
        assertThat(consumer.mCount, CoreMatchers.is(0));
        value.set("bla");
        observer.onInvalidated(tableSet);
        drain();
        // get value
        assertThat(consumer.mCount, CoreMatchers.is(1));
        observer.onInvalidated(tableSet);
        drain();
        // get value
        assertThat(consumer.mCount, CoreMatchers.is(2));
        value.set(null);
        observer.onInvalidated(tableSet);
        drain();
        // no value
        assertThat(consumer.mCount, CoreMatchers.is(2));
        disposable.dispose();
    }

    @Test
    public void exception_Flowable() throws Exception {
        final Flowable<String> flowable = RxRoom.createFlowable(
                mDatabase,
                false,
                new String[]{"a"},
                () -> {
                    throw new Exception("i want exception");
                });
        TestSubscriber<String> subscriber = new TestSubscriber<>();
        flowable.subscribe(subscriber);
        drain();
        subscriber.assertError(
                throwable -> Objects.equals(throwable.getMessage(), "i want exception"));
    }

    @Test
    public void exception_Observable() throws Exception {
        final Observable<String> flowable = RxRoom.createObservable(
                mDatabase,
                false,
                new String[]{"a"},
                () -> {
                    throw new Exception("i want exception");
                });
        TestObserver<String> observer = new TestObserver<>();
        flowable.subscribe(observer);
        drain();
        observer.assertError(
                throwable -> Objects.equals(throwable.getMessage(), "i want exception"));
    }

    private void drain() throws Exception {
        mExecutor.drainTasks(10, TimeUnit.SECONDS);
    }

    private static class CountingConsumer implements Consumer<Object> {
        int mCount = 0;

        @Override
        public void accept(@NonNull Object o) {
            mCount++;
        }
    }
}
