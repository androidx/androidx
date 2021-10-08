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

/**
 * WorkManager Testing is a library for testing app code that runs using WorkManager.
 * <p>
 * This testing library provides a way to manually initialize WorkManager for tests by using
 * {@link androidx.work.testing.WorkManagerTestInitHelper}.  Once initialized, you can use
 * {@link androidx.work.testing.WorkManagerTestInitHelper#getTestDriver(android.content.Context)} to
 * drive constraints and timing-related triggers for your background work.
 * <p>
 * For ease of testing, this library defaults to using a synchronous
 * {@link java.util.concurrent.Executor}; you can change this in the
 * {@link androidx.work.Configuration} if you wish.
 */
package androidx.work.testing;

