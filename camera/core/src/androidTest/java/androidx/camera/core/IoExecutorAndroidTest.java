/*
 * Copyright (C) 2019 The Android Open Source Project
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

package androidx.camera.core;

import android.support.annotation.GuardedBy;
import androidx.test.runner.AndroidJUnit4;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class IoExecutorAndroidTest {

  private Executor ioExecutor;

  private enum RunnableState {
    CLEAR,
    RUNNABLE1_WAITING,
    RUNNABLE1_FINISHED,
    RUNNABLE2_FINISHED
  }

  private Lock lock = new ReentrantLock();
  private Condition condition = lock.newCondition();

  @GuardedBy("lock")
  private RunnableState state = RunnableState.CLEAR;

  private final Runnable runnable1 =
      () -> {
        lock.lock();
        try {
          state = RunnableState.RUNNABLE1_WAITING;
          condition.signalAll();
          while (state != RunnableState.CLEAR) {
            condition.await();
          }

          state = RunnableState.RUNNABLE1_FINISHED;
          condition.signalAll();
        } catch (InterruptedException e) {
          throw new RuntimeException("Thread interrupted unexpectedly", e);
        } finally {
          lock.unlock();
        }
      };

  private final Runnable runnable2 =
      () -> {
        lock.lock();
        try {
          while (state != RunnableState.RUNNABLE1_WAITING) {
            condition.await();
          }

          state = RunnableState.RUNNABLE2_FINISHED;
          condition.signalAll();
        } catch (InterruptedException e) {
          throw new RuntimeException("Thread interrupted unexpectedly", e);
        } finally {
          lock.unlock();
        }
      };

  private final Runnable simpleRunnable1 =
      () -> {
        lock.lock();
        try {
          state = RunnableState.RUNNABLE1_FINISHED;
          condition.signalAll();
        } finally {
          lock.unlock();
        }
      };

  @Before
  public void setup() {
    lock.lock();
    try {
      state = RunnableState.CLEAR;
    } finally {
      lock.unlock();
    }
    ioExecutor = IoExecutor.getInstance();
  }

  @Test(timeout = 2000)
  public void canRunRunnable() throws InterruptedException {
    ioExecutor.execute(simpleRunnable1);
    lock.lock();
    try {
      while (state != RunnableState.RUNNABLE1_FINISHED) {
        condition.await();
      }
    } finally {
      lock.unlock();
    }

    // No need to check anything here. Completing this method should signal success.
  }

  @Test(timeout = 2000)
  public void canRunMultipleRunnableInParallel() throws InterruptedException {
    ioExecutor.execute(runnable1);
    ioExecutor.execute(runnable2);

    lock.lock();
    try {
      // runnable2 cannot finish until runnable1 has started
      while (state != RunnableState.RUNNABLE2_FINISHED) {
        condition.await();
      }

      // Allow runnable1 to finish
      state = RunnableState.CLEAR;
      condition.signalAll();

      while (state != RunnableState.RUNNABLE1_FINISHED) {
        condition.await();
      }
    } finally {
      lock.unlock();
    }

    // No need to check anything here. Completing this method should signal success.
  }
}
