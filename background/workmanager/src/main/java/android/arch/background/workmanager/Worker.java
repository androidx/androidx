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

package android.arch.background.workmanager;

import android.arch.background.workmanager.model.Arguments;
import android.arch.background.workmanager.model.WorkSpec;
import android.content.Context;
import android.util.Log;

/**
 * The basic unit of work.
 */
public abstract class Worker {

    private static final String TAG = "Worker";

    private Context mAppContext;
    private Arguments mArguments;

    protected final Context getAppContext() {
        return mAppContext;
    }

    protected final Arguments getArguments() {
        return mArguments;
    }

    /**
     * Override this method to do your actual background processing.
     *
     * @throws Exception An {@link Exception} if the work failed
     */
    public abstract void doWork() throws Exception;

    private void internalInit(Context appContext, Arguments arguments) {
        mAppContext = appContext;
        mArguments = arguments;
    }

    @SuppressWarnings("ClassNewInstance")
    static Worker fromWorkSpec(Context context, WorkSpec workSpec) {
        Context appContext = context.getApplicationContext();
        String workerClassName = workSpec.getWorkerClassName();
        Arguments arguments = workSpec.getArguments();
        try {
            Class<?> clazz = Class.forName(workerClassName);
            if (Worker.class.isAssignableFrom(clazz)) {
                Worker worker = (Worker) clazz.newInstance();
                worker.internalInit(appContext, arguments);
                return worker;
            } else {
                Log.e(TAG, "" + workerClassName + " is not of type Worker");
            }
        } catch (Exception e) {
            Log.e(TAG, "Trouble instantiating " + workerClassName, e);
        }
        return null;
    }
}
