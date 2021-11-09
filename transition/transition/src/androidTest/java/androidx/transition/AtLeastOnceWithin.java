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

package androidx.transition;

import org.mockito.exceptions.base.MockitoAssertionError;
import org.mockito.internal.verification.Description;
import org.mockito.internal.verification.api.VerificationData;
import org.mockito.invocation.Invocation;
import org.mockito.invocation.MatchableInvocation;
import org.mockito.verification.VerificationMode;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

class AtLeastOnceWithin implements VerificationMode {

    private final long mTimeoutMs;

    static AtLeastOnceWithin atLeastOnceWithin(long timeoutMs) {
        return new AtLeastOnceWithin(timeoutMs);
    }

    private AtLeastOnceWithin(long timeoutMs) {
        mTimeoutMs = timeoutMs;
    }

    @Override
    public void verify(final VerificationData data) {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicBoolean done = new AtomicBoolean(false);
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (!done.get()) {
                    final List<Invocation> invocations = data.getAllInvocations();
                    final MatchableInvocation target = data.getTarget();
                    for (int i = 0; i < invocations.size(); i++) {
                        if (target.matches(invocations.get(i))) {
                            latch.countDown();
                            return;
                        }
                    }
                }
            }
        }).start();
        try {
            final boolean matched = latch.await(mTimeoutMs, TimeUnit.MILLISECONDS);
            done.set(true);
            if (!matched) {
                throw new MockitoAssertionError("Not invoked: " + data.getTarget());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public VerificationMode description(String description) {
        return new Description(this, "a");
    }

}
