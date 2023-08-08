/*
 * Copyright 2022 The Android Open Source Project
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

package dalvik.annotation.optimization;

import androidx.annotation.RestrictTo;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Applied to native methods to enable an ART runtime built-in optimization:
 * methods that are annotated this way can speed up JNI transitions for methods that contain no
 * objects (in parameters or return values, or as an implicit {@code this}).
 *
 * <p>
 * The native implementation must exclude the {@code JNIEnv} and {@code jclass} parameters from its
 * function signature. As an additional limitation, the method must be explicitly registered with
 * {@code RegisterNatives} instead of relying on the built-in dynamic JNI linking.
 * </p>
 *
 * <p>
 * Performance of JNI transitions:
 * <ul>
 * <li>Regular JNI cost in nanoseconds: 115
 * <li>Fast {@code (!)} JNI cost in nanoseconds: 60
 * <li>{@literal @}{@link FastNative} cost in nanoseconds: 35
 * <li>{@literal @}{@code CriticalNative} cost in nanoseconds: 25
 * </ul>
 * (Measured on angler-userdebug in 07/2016).
 * </p>
 *
 * <p>
 * A similar annotation, {@literal @}{@link FastNative}, exists with similar performance guarantees.
 * However, unlike {@code @CriticalNative} it supports non-statics, object return values, and object
 * parameters. If a method absolutely must have access to a {@code jobject}, then use
 * {@literal @}{@link FastNative} instead of this.
 * </p>
 *
 * <p>
 * This has the side-effect of disabling all garbage collections while executing a critical native
 * method. Use with extreme caution. Any long-running methods must not be marked with
 * {@code @CriticalNative} (including usually-fast but generally unbounded methods)!
 * </p>
 *
 * <p>
 * <b>Deadlock Warning:</b> As a rule of thumb, do not acquire any locks during a critical native
 * call if they aren't also locally released [before returning to managed code].
 * </p>
 *
 * <p>
 * Say some code does:
 *
 * <code>
 * critical_native_call_to_grab_a_lock();
 * does_some_java_work();
 * critical_native_call_to_release_a_lock();
 * </code>
 *
 * <p>
 * This code can lead to deadlocks. Say thread 1 just finishes
 * {@code critical_native_call_to_grab_a_lock()} and is in {@code does_some_java_work()}.
 * GC kicks in and suspends thread 1. Thread 2 now is in
 * {@code critical_native_call_to_grab_a_lock()} but is blocked on grabbing the
 * native lock since it's held by thread 1. Now thread suspension can't finish
 * since thread 2 can't be suspended since it's doing CriticalNative JNI.
 * </p>
 *
 * <p>
 * Normal natives don't have the issue since once it's executing in native code,
 * it is considered suspended from the runtime's point of view.
 * CriticalNative natives however don't do the state transition done by the normal natives.
 * </p>
 *
 * <p>
 * This annotation has no effect when used with non-native methods.
 * The runtime must throw a {@code VerifierError} upon class loading if this is used with a native
 * method that contains object parameters, an object return value, or a non-static.
 * </p>
 *
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@Retention(RetentionPolicy.CLASS)  // Save memory, don't instantiate as an object at runtime.
@Target(ElementType.METHOD)
public @interface CriticalNative {}

