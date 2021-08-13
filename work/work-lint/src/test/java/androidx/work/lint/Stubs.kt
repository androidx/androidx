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

package androidx.work.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest.java
import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
import com.android.tools.lint.checks.infrastructure.TestFile

object Stubs {

    val WORKER_FACTORY: TestFile = kotlin(
        "androidx/work/WorkerFactory.kt",
        """
        package androidx.work

        open class WorkerFactory
    """
    ).indented().within("src")

    val WORK_MANAGER_CONFIGURATION_PROVIDER: TestFile = java(
        "androidx/work/Configuration.java",
        """
                 package androidx.work; 

                 class Configuration {
                    static class Builder {
                        void setJobSchedulerJobIdRange(int minId, int maxId) {

                        }
                        void setWorkerFactory(WorkerFactory factory) {

                        }
                    }
                    interface Provider {
                        Configuration getWorkManagerConfiguration();
                    }
                 }
            """
    )
        .indented().within("src")

    val ANDROID_APPLICATION: TestFile = kotlin(
        "android/app/Application.kt",
        """
                package android.app
                open class Application {
                  fun onCreate() {

                  }
                }
            """
    )
        .indented().within("src")

    val LISTENABLE_WORKER: TestFile = kotlin(
        "androidx/work/ListenableWorker.kt",
        """
            package androidx.work

            open class ListenableWorker
        """
    ).indented().within("src")

    val RX_WORKER: TestFile = kotlin(
        "androidx/work/RxWorker.kt",
        """
            package androidx.work

            open class RxWorker: ListenableWorker() {
                fun setProgress() {

                }

                fun setCompletableProgress() {

                }
            }
        """
    ).indented().within("src")

    val WORK_REQUEST: TestFile = kotlin(
        "androidx/work/WorkRequest.kt",
        """
            package androidx.work

            open class WorkRequest
        """
    ).indented().within("src")

    val ONE_TIME_WORK_REQUEST: TestFile = kotlin(
        "androidx/work/OneTimeWorkRequest.kt",
        """
            package androidx.work

            class OneTimeWorkRequest: WorkRequest()
        """
    ).indented().within("src")

    val PERIODIC_WORK_REQUEST: TestFile = java(
        "androidx/work/PeriodicWorkRequest.java",
        """
            package androidx.work;

            import androidx.work.ListenableWorker;
            import java.time.Duration;
            import java.util.concurrent.TimeUnit;

            class PeriodicWorkRequest extends WorkRequest {
                static class Builder {
                    public Builder(ListenableWorker worker, long interval, TimeUnit unit) {
                        
                    }
                    public Builder(ListenableWorker worker, Duration duration) {
                        
                    }
                    public Builder(
                        ListenableWorker worker,
                        long interval, TimeUnit intervalUnit, 
                        long flex,
                        TimeUnit flexUnits) {
                        
                    }
                    public Builder(
                        ListenableWorker worker,
                        Duration intervalDuration,
                        Duration flexDuration) {

                    }
                }
            }
        """
    ).indented().within("src")

    val CONSTRAINTS: TestFile = java(
        "androidx/work/Constraints.java",
        """
        package androidx.work;

        class Constraints {
            public static class Builder {
                public Builder setRequiresDeviceIdle(boolean requiresDeviceIdle) {
                    return this;
                }
                public Builder setRequiresCharging(boolean requiresDeviceIdle) {
                    return this;
                }
            }
        }
    """
    ).indented().within("src")

    val NOTIFICATION: TestFile = kotlin(
        "android/app/Notification.kt",
        """
            package android.app

            class Notification {
            }
        """
    ).indented().within("src")

    val JOB_SERVICE: TestFile = kotlin(
        "android/app/job/JobService.kt",
        """
            package android.app.job

            open class JobService {

            }
        """
    ).indented().within("src")

    val FOREGROUND_INFO: TestFile = kotlin(
        "androidx/work/ForegroundInfo.kt",
        """
            package androidx.work

            import android.app.Notification

            class ForegroundInfo(id: Int, notification: Notification, serviceType: Int) {
                constructor(id: Int, notification: Notification) {
                   this(id, notification, 0)
                }
            }
        """
    ).indented().within("src")

    val WORK_MANAGER: TestFile = kotlin(
        "androidx/work/WorkManager.kt",
        """
                 package androidx.work

                 interface WorkManager {
                    fun enqueue(request: WorkRequest)
                    fun enqueue(requests: List<WorkRequest>)
                    fun enqueueUniqueWork(name: String, request: PeriodicWorkRequest)
                 }
            """
    )
        .indented().within("src")
}
