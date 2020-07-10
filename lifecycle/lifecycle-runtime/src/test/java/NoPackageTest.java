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

import static androidx.lifecycle.Lifecycle.Event.ON_CREATE;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class NoPackageTest {

    private LifecycleOwner mLifecycleOwner;
    private Lifecycle mLifecycle;
    private LifecycleRegistry mRegistry;

    @Before
    public void init() {
        mLifecycleOwner = mock(LifecycleOwner.class);
        mLifecycle = mock(Lifecycle.class);
        when(mLifecycleOwner.getLifecycle()).thenReturn(mLifecycle);
        mRegistry = new LifecycleRegistry(mLifecycleOwner);
    }

    @Test
    public void testNoPackage() {
        NoPackageObserver observer = mock(NoPackageObserver.class);
        mRegistry.addObserver(observer);
        mRegistry.handleLifecycleEvent(ON_CREATE);
        verify(observer).onCreate();
    }

}

