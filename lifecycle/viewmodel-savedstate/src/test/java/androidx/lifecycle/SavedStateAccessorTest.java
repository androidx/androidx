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

package androidx.lifecycle;


import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@SuppressWarnings("unchecked")
@RunWith(JUnit4.class)
public class SavedStateAccessorTest {

    @Rule
    public InstantTaskExecutorRule mInstantTaskExecutorRule = new InstantTaskExecutorRule();

    @Test
    public void testSetGet() {
        SavedStateAccessor accessor = new SavedStateAccessor();
        accessor.set("foo", "trololo");
        assertThat(accessor.<String>get("foo"), is("trololo"));
        MutableLiveData<String> fooLd = accessor.getLiveData("foo");
        assertThat(fooLd.getValue(), is("trololo"));
        fooLd.setValue("another");
        assertThat(accessor.<String>get("foo"), is("another"));
    }

    @Test
    public void testSetObserve() {
        SavedStateAccessor accessor = new SavedStateAccessor();
        LiveData<Integer> liveData = accessor.getLiveData("a");
        Observer<Integer> observer = mock(Observer.class);
        liveData.observeForever(observer);
        accessor.set("a", 261);
        verify(observer).onChanged(261);
    }

    @Test
    public void testContains() {
        SavedStateAccessor accessor = new SavedStateAccessor();
        MutableLiveData<Integer> foo = accessor.getLiveData("foo");
        assertThat(accessor.contains("foo"), is(false));
        foo.setValue(712);
        assertThat(accessor.contains("foo"), is(true));

        accessor.get("foo2");
        assertThat(accessor.contains("foo2"), is(false));
        accessor.set("foo2", "spb");
        assertThat(accessor.contains("foo2"), is(true));
    }

    @Test
    public void testRemove() {
        SavedStateAccessor accessor = new SavedStateAccessor();
        accessor.set("s", "pb");
        assertThat(accessor.contains("s"), is(true));
        assertThat(accessor.<String>remove("s"), is("pb"));
        assertThat(accessor.contains("s"), is(false));

        assertThat(accessor.remove("don't exist"), nullValue());
    }

    @Test
    public void testRemoveWithLD() {
        SavedStateAccessor accessor = new SavedStateAccessor();
        accessor.set("spb", 1703);
        LiveData<Integer> ld = accessor.getLiveData("spb");
        assertThat(accessor.contains("spb"), is(true));
        Observer<Integer> observer = mock(Observer.class);
        ld.observeForever(observer);
        reset(observer);
        assertThat(accessor.<Integer>remove("spb"), is(1703));
        assertThat(accessor.contains("spb"), is(false));
        assertThat(accessor.remove("spb"), nullValue());
        verify(observer, never()).onChanged(anyInt());
        reset(observer);
        accessor.set("spb", 1914);
        verify(observer, never()).onChanged(anyInt());
        ld.setValue(1924);
        assertThat(accessor.<Integer>get("spb"), is(1914));
    }

    @Test
    public void testKeySet() {
        SavedStateAccessor accessor = new SavedStateAccessor();
        accessor.set("s", "pb");
        accessor.getLiveData("ld").setValue("a");
        assertThat(accessor.keys().size(), is(2));
        assertThat(accessor.keys(), hasItems("s", "ld"));
    }
}
