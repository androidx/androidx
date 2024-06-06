/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.pdf.viewer.loader;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.pdf.util.ThreadUtils;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Executor for running AbstractPdfTasks. Tasks should be scheduled by calling
 * {@link #schedule}, then they will be executed once all scheduled tasks of
 * higher priority have been executed.
 * <p>
 * Tasks should be scheduled from the UI thread. Tasks are always started
 * using the UI thread, since this is a requirement of {@code AsyncTask}.
 * <p>
 * pdfClient is not thread-safe, so a single thread is used to execute all tasks.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class PdfTaskExecutor extends Thread {
    private static final String TAG = PdfTaskExecutor.class.getSimpleName();

    private final Queue<AbstractPdfTask<?>> mScheduledTasks = new LinkedList<AbstractPdfTask<?>>();

    private boolean mIsFinished;

    PdfTaskExecutor() {
        super();
        setName("PdfTaskExecutor");
    }

    @Override
    public void run() {
        while (!mIsFinished) {
            AbstractPdfTask<?> taskToRun = getNextTask();
            if (taskToRun != null) {
                executeTask(taskToRun);
            } else {
                waitForTask();
            }
        }
    }

    private void waitForTask() {
        synchronized (this) {
            try {
                // Could wait indefinitely for a notify(), but this is safer.
                this.wait(10000);  // Wait 10 seconds.
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /** Set the value of isFinished to true. */
    public void finish() {
        mIsFinished = true;
    }

    /** Schedule the given task. */
    public void schedule(@NonNull AbstractPdfTask<?> task) {
        synchronized (this) {
            mScheduledTasks.add(task);
            this.notifyAll();
        }
    }

    @Nullable
    private AbstractPdfTask<?> getNextTask() {
        synchronized (this) {
            // Linear search - could use a priority heap for more efficiency, but this
            // way allows for changing priority of tasks that are already scheduled.
            AbstractPdfTask<?> taskToRun = null;
            for (Iterator<AbstractPdfTask<?>> it = mScheduledTasks.iterator(); it.hasNext(); ) {
                AbstractPdfTask<?> task = it.next();
                if (task.isCancelled()) {
                    it.remove();
                } else if (taskToRun == null || task.mPriority.compareTo(taskToRun.mPriority) < 0) {
                    taskToRun = task;
                }
            }
            if (taskToRun != null) {
                mScheduledTasks.remove(taskToRun);
            }
            return taskToRun;
        }

    }

    private <T> void executeTask(final AbstractPdfTask<T> task) {
        final T result = task.findPdfAndDoInBackground();
        ThreadUtils.runOnUiThread(() -> task.onPostExecute(result));
    }
}
