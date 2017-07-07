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

package android.arch.persistence.room;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.arch.core.executor.JunitTaskExecutorRule;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.Flowable;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.subscribers.TestSubscriber;

@RunWith(JUnit4.class)
public class RxRoomTest {
    @Rule
    public JunitTaskExecutorRule mExecutor = new JunitTaskExecutorRule(1, false);
    private RoomDatabase mDatabase;
    private InvalidationTracker mInvalidationTracker;
    private List<InvalidationTracker.Observer> mAddedObservers = new ArrayList<>();

    @Before
    public void init() {
        mDatabase = mock(RoomDatabase.class);
        mInvalidationTracker = mock(InvalidationTracker.class);
        when(mDatabase.getInvalidationTracker()).thenReturn(mInvalidationTracker);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                mAddedObservers.add((InvalidationTracker.Observer) invocation.getArguments()[0]);
                return null;
            }
        }).when(mInvalidationTracker).addObserver(any(InvalidationTracker.Observer.class));
    }

    @Test
    public void basicAddRemove() {
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
    public void basicNotify() throws InterruptedException {
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
    public void internalCallable() throws InterruptedException {
        final AtomicReference<String> value = new AtomicReference<>(null);
        String[] tables = {"a", "b"};
        Set<String> tableSet = new HashSet<>(Arrays.asList(tables));
        final Flowable<String> flowable = RxRoom.createFlowable(mDatabase, tables,
                new Callable<String>() {
                    @Override
                    public String call() throws Exception {
                        return value.get();
                    }
                });
        final CountingConsumer consumer = new CountingConsumer();
        flowable.subscribe(consumer);
        InvalidationTracker.Observer observer = mAddedObservers.get(0);
        drain();
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
    }

    private void drain() throws InterruptedException {
        mExecutor.drainTasks(2);
    }

    @Test
    public void exception() throws InterruptedException {
        final Flowable<String> flowable = RxRoom.createFlowable(mDatabase, new String[]{"a"},
                new Callable<String>() {
                    @Override
                    public String call() throws Exception {
                        throw new Exception("i want exception");
                    }
                });
        TestSubscriber<String> subscriber = new TestSubscriber<>();
        flowable.subscribe(subscriber);
        drain();
        assertThat(subscriber.errorCount(), CoreMatchers.is(1));
        assertThat(subscriber.errors().get(0).getMessage(), CoreMatchers.is("i want exception"));
    }

    private static class CountingConsumer implements Consumer<Object> {
        int mCount = 0;

        @Override
        public void accept(@NonNull Object o) throws Exception {
            mCount++;
        }
    }
}
