/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.hilt.work;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import dagger.hilt.GeneratesRootInput;

/**
 * Identifies a {@link androidx.work.Worker}'s constructor for injection.
 * <p>
 * Similar to {@link javax.inject.Inject}, a {@code Worker} containing a constructor annotated
 * with {@code WorkerInject} will have its dependencies defined in the constructor parameters
 * injected by Dagger's Hilt. The {@code Worker} will be available for creation by the
 * {@link androidx.hilt.work.HiltWorkerFactory} that should be set in {@code WorkManager}'s
 * configuration via
 * {@link androidx.work.Configuration.Builder#setWorkerFactory(androidx.work.WorkerFactory)}.
 * <p>
 * Example:
 * <pre>
 * public class UploadWorker extends Worker {
 *     &#64;WorkerInject
 *     public UploadWorker(&#64;Assisted Context context, &#64;Assisted WorkerParameters params,
 *             HttpClient httpClient) {
 *         // ...
 *     }
 * }
 * </pre>
 * <pre>
 * &#64;GenerateComponents
 * &#64;AndroidEntryPoint
 * public class MyApplication extends Application implements Configuration.Provider {
 *     &#64;Inject HiltWorkerFactory workerFactory;
 *
 *     &#64;Override
 *     public Configuration getWorkManagerConfiguration() {
 *         return Configuration.Builder()
 *                 .setWorkerFactory(workerFactory)
 *                 .build();
 *     }
 * }
 * </pre>
 * <p>
 * Only one constructor in the {@code Worker} must be annotated with {@code WorkerInject}. The
 * constructor must define parameters for a {@link androidx.hilt.Assisted}-annotated {@code Context}
 * and a {@link androidx.hilt.Assisted}-annotated {@code WorkerParameters} along with any other
 * dependencies. Both the {@code Context} and {@code WorkerParameters} must not be a type param
 * of {@link javax.inject.Provider} nor {@link dagger.Lazy} and must not be qualified.
 * <p>
 * Only dependencies available in the {@link dagger.hilt.android.components.ApplicationComponent}
 * can be injected into the {@code Worker}.
 */
@Target(ElementType.CONSTRUCTOR)
@Retention(RetentionPolicy.CLASS)
@GeneratesRootInput
public @interface WorkerInject {
}
