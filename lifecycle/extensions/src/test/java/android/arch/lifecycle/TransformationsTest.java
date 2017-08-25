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

package android.arch.lifecycle;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.arch.core.executor.AppToolkitTaskExecutor;
import android.arch.core.util.Function;
import android.arch.lifecycle.util.InstantTaskExecutor;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@SuppressWarnings("unchecked")
@RunWith(JUnit4.class)
public class TransformationsTest {

    private LifecycleOwner mOwner;

    @Before
    public void swapExecutorDelegate() {
        AppToolkitTaskExecutor.getInstance().setDelegate(new InstantTaskExecutor());
    }

    @Before
    public void setup() {
        mOwner = mock(LifecycleOwner.class);
        LifecycleRegistry registry = new LifecycleRegistry(mOwner);
        when(mOwner.getLifecycle()).thenReturn(registry);
        registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
        registry.handleLifecycleEvent(Lifecycle.Event.ON_START);
    }

    @Test
    public void testMap() {
        LiveData<String> source = new MutableLiveData<>();
        LiveData<Integer> mapped = Transformations.map(source, new Function<String, Integer>() {
            @Override
            public Integer apply(String input) {
                return input.length();
            }
        });
        Observer<Integer> observer = mock(Observer.class);
        mapped.observe(mOwner, observer);
        source.setValue("four");
        verify(observer).onChanged(4);
    }

    @Test
    public void testSwitchMap() {
        LiveData<Integer> trigger = new MutableLiveData<>();
        final LiveData<String> first = new MutableLiveData<>();
        final LiveData<String> second = new MutableLiveData<>();
        LiveData<String> result = Transformations.switchMap(trigger,
                new Function<Integer, LiveData<String>>() {
                    @Override
                    public LiveData<String> apply(Integer input) {
                        if (input == 1) {
                            return first;
                        } else {
                            return second;
                        }
                    }
                });

        Observer<String> observer = mock(Observer.class);
        result.observe(mOwner, observer);
        verify(observer, never()).onChanged(anyString());
        first.setValue("first");
        trigger.setValue(1);
        verify(observer).onChanged("first");
        second.setValue("second");
        reset(observer);
        verify(observer, never()).onChanged(anyString());
        trigger.setValue(2);
        verify(observer).onChanged("second");
        reset(observer);
        first.setValue("failure");
        verify(observer, never()).onChanged(anyString());
    }

    @Test
    public void testSwitchMap2() {
        LiveData<Integer> trigger = new MutableLiveData<>();
        final LiveData<String> first = new MutableLiveData<>();
        final LiveData<String> second = new MutableLiveData<>();
        LiveData<String> result = Transformations.switchMap(trigger,
                new Function<Integer, LiveData<String>>() {
                    @Override
                    public LiveData<String> apply(Integer input) {
                        if (input == 1) {
                            return first;
                        } else {
                            return second;
                        }
                    }
                });

        Observer<String> observer = mock(Observer.class);
        result.observe(mOwner, observer);

        verify(observer, never()).onChanged(anyString());
        trigger.setValue(1);
        verify(observer, never()).onChanged(anyString());
        first.setValue("fi");
        verify(observer).onChanged("fi");
        first.setValue("rst");
        verify(observer).onChanged("rst");

        second.setValue("second");
        reset(observer);
        verify(observer, never()).onChanged(anyString());
        trigger.setValue(2);
        verify(observer).onChanged("second");
        reset(observer);
        first.setValue("failure");
        verify(observer, never()).onChanged(anyString());
    }

    @Test
    public void testNoRedispatchSwitchMap() {
        LiveData<Integer> trigger = new MutableLiveData<>();
        final LiveData<String> first = new MutableLiveData<>();
        LiveData<String> result = Transformations.switchMap(trigger,
                new Function<Integer, LiveData<String>>() {
                    @Override
                    public LiveData<String> apply(Integer input) {
                        return first;
                    }
                });

        Observer<String> observer = mock(Observer.class);
        result.observe(mOwner, observer);
        verify(observer, never()).onChanged(anyString());
        first.setValue("first");
        trigger.setValue(1);
        verify(observer).onChanged("first");
        reset(observer);
        trigger.setValue(2);
        verify(observer, never()).onChanged(anyString());
    }

    @Test
    public void testSwitchMapToNull() {
        LiveData<Integer> trigger = new MutableLiveData<>();
        final LiveData<String> first = new MutableLiveData<>();
        LiveData<String> result = Transformations.switchMap(trigger,
                new Function<Integer, LiveData<String>>() {
                    @Override
                    public LiveData<String> apply(Integer input) {
                        if (input == 1) {
                            return first;
                        } else {
                            return null;
                        }
                    }
                });

        Observer<String> observer = mock(Observer.class);
        result.observe(mOwner, observer);
        verify(observer, never()).onChanged(anyString());
        first.setValue("first");
        trigger.setValue(1);
        verify(observer).onChanged("first");
        reset(observer);

        trigger.setValue(2);
        verify(observer, never()).onChanged(anyString());
        assertThat(first.hasObservers(), is(false));
    }
}
