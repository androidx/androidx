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

package androidx.room.integration.testapp.test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;

import java.util.concurrent.TimeoutException;

public abstract class TestObserver<T> implements Observer<T> {
    private T mLastData;
    private boolean mHasValue = false;

    void reset() {
        mHasValue = false;
        mLastData = null;
    }

    @Override
    public void onChanged(@Nullable T o) {
        mLastData = o;
        mHasValue = true;
    }

    public boolean hasValue() throws TimeoutException, InterruptedException {
        drain();
        return mHasValue;
    }

    public T get() throws InterruptedException, TimeoutException {
        drain();
        assertThat(hasValue(), is(true));
        return mLastData;
    }

    protected abstract void drain() throws TimeoutException, InterruptedException;
}
