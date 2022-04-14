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

package androidx.lifecycle;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import static kotlinx.coroutines.test.TestCoroutineDispatchersKt.UnconfinedTestDispatcher;

import androidx.annotation.Nullable;
import androidx.arch.core.executor.ArchTaskExecutor;
import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.testing.TestLifecycleOwner;
import androidx.lifecycle.util.InstantTaskExecutor;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@SuppressWarnings("unchecked")
@RunWith(JUnit4.class)
public class MediatorLiveDataTest {

    @Rule
    public InstantTaskExecutorRule mInstantTaskExecutorRule = new InstantTaskExecutorRule();

    private TestLifecycleOwner mOwner;
    private MediatorLiveData<String> mMediator;
    private LiveData<String> mSource;
    private boolean mSourceActive;

    @Before
    public void setup() {
        mOwner = new TestLifecycleOwner(Lifecycle.State.STARTED,
                UnconfinedTestDispatcher(null, null));
        mMediator = new MediatorLiveData<>();
        mSource = new LiveData<String>() {
            @Override
            protected void onActive() {
                mSourceActive = true;
            }

            @Override
            protected void onInactive() {
                mSourceActive = false;
            }
        };
        mSourceActive = false;
        mMediator.observe(mOwner, mock(Observer.class));
    }

    @Before
    public void swapExecutorDelegate() {
        ArchTaskExecutor.getInstance().setDelegate(new InstantTaskExecutor());
    }

    @Test
    public void testSingleDelivery() {
        Observer observer = mock(Observer.class);
        mMediator.addSource(mSource, observer);
        mSource.setValue("flatfoot");
        verify(observer).onChanged("flatfoot");
        mOwner.handleLifecycleEvent(Lifecycle.Event.ON_STOP);
        reset(observer);
        verify(observer, never()).onChanged(any());
    }

    @Test
    public void testChangeWhileInactive() {
        Observer observer = mock(Observer.class);
        mMediator.addSource(mSource, observer);
        mMediator.observe(mOwner, mock(Observer.class));
        mSource.setValue("one");
        verify(observer).onChanged("one");
        mOwner.handleLifecycleEvent(Lifecycle.Event.ON_STOP);
        reset(observer);
        mSource.setValue("flatfoot");
        mOwner.handleLifecycleEvent(Lifecycle.Event.ON_START);
        verify(observer).onChanged("flatfoot");
    }


    @Test
    public void testAddSourceToActive() {
        mSource.setValue("flatfoot");
        Observer observer = mock(Observer.class);
        mMediator.addSource(mSource, observer);
        verify(observer).onChanged("flatfoot");
    }

    @Test
    public void testAddSourceToInActive() {
        mSource.setValue("flatfoot");
        mOwner.handleLifecycleEvent(Lifecycle.Event.ON_STOP);
        Observer observer = mock(Observer.class);
        mMediator.addSource(mSource, observer);
        verify(observer, never()).onChanged(any());
        mOwner.handleLifecycleEvent(Lifecycle.Event.ON_START);
        verify(observer).onChanged("flatfoot");
    }

    @Test
    public void testRemoveSource() {
        mSource.setValue("flatfoot");
        Observer observer = mock(Observer.class);
        mMediator.addSource(mSource, observer);
        verify(observer).onChanged("flatfoot");
        mMediator.removeSource(mSource);
        reset(observer);
        mSource.setValue("failure");
        verify(observer, never()).onChanged(any());
    }

    @Test
    public void testSourceInactive() {
        Observer observer = mock(Observer.class);
        mMediator.addSource(mSource, observer);
        assertThat(mSourceActive, is(true));
        mOwner.handleLifecycleEvent(Lifecycle.Event.ON_STOP);
        assertThat(mSourceActive, is(false));
        mOwner.handleLifecycleEvent(Lifecycle.Event.ON_START);
        assertThat(mSourceActive, is(true));
    }

    @Test
    public void testNoLeakObserver() {
        // Imitates a destruction of a ViewModel: a listener of LiveData is destroyed,
        // a reference to MediatorLiveData is cleaned up. In this case we shouldn't leak
        // MediatorLiveData as an observer of mSource.
        assertThat(mSource.hasObservers(), is(false));
        Observer observer = mock(Observer.class);
        mMediator.addSource(mSource, observer);
        assertThat(mSource.hasObservers(), is(true));
        mOwner.handleLifecycleEvent(Lifecycle.Event.ON_STOP);
        mOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY);
        mMediator = null;
        assertThat(mSource.hasObservers(), is(false));
    }

    @Test
    public void testMultipleSources() {
        Observer observer1 = mock(Observer.class);
        mMediator.addSource(mSource, observer1);
        MutableLiveData<Integer> source2 = new MutableLiveData<>();
        Observer observer2 = mock(Observer.class);
        mMediator.addSource(source2, observer2);
        mSource.setValue("flatfoot");
        verify(observer1).onChanged("flatfoot");
        verify(observer2, never()).onChanged(any());
        reset(observer1, observer2);
        source2.setValue(1703);
        verify(observer1, never()).onChanged(any());
        verify(observer2).onChanged(1703);
        reset(observer1, observer2);
        mOwner.handleLifecycleEvent(Lifecycle.Event.ON_STOP);
        mSource.setValue("failure");
        source2.setValue(0);
        verify(observer1, never()).onChanged(any());
        verify(observer2, never()).onChanged(any());
    }

    @Test
    public void removeSourceDuringOnActive() {
        // to trigger ConcurrentModificationException,
        // we have to call remove from a collection during "for" loop.
        // ConcurrentModificationException is thrown from next() method of an iterator
        // so this modification shouldn't be at the last iteration,
        // because if it is a last iteration, then next() wouldn't be called.
        // And the last: an order of an iteration over sources is not defined,
        // so I have to call it remove operation  from all observers.
        mOwner.handleLifecycleEvent(Lifecycle.Event.ON_STOP);
        Observer<String> removingObserver = new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                mMediator.removeSource(mSource);
            }
        };
        mMediator.addSource(mSource, removingObserver);
        MutableLiveData<String> source2 = new MutableLiveData<>();
        source2.setValue("nana");
        mMediator.addSource(source2, removingObserver);
        mSource.setValue("petjack");
        mOwner.handleLifecycleEvent(Lifecycle.Event.ON_START);
    }

    @Test(expected = IllegalArgumentException.class)
    public void reAddSameSourceWithDifferentObserver() {
        mMediator.addSource(mSource, mock(Observer.class));
        mMediator.addSource(mSource, mock(Observer.class));
    }

    @Test
    public void addSameSourceWithSameObserver() {
        Observer observer = mock(Observer.class);
        mMediator.addSource(mSource, observer);
        mMediator.addSource(mSource, observer);
        // no exception was thrown
    }

    @Test
    public void addSourceDuringOnActive() {
        mOwner.handleLifecycleEvent(Lifecycle.Event.ON_STOP);
        mSource.setValue("a");
        mMediator.addSource(mSource, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                MutableLiveData<String> source = new MutableLiveData<>();
                source.setValue("b");
                mMediator.addSource(source, new Observer<String>() {
                    @Override
                    public void onChanged(@Nullable String s) {
                        mMediator.setValue("c");
                    }
                });
            }
        });
        mOwner.handleLifecycleEvent(Lifecycle.Event.ON_START);
        assertThat(mMediator.getValue(), is("c"));
    }

}
