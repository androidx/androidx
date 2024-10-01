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

package androidx.remotecallback;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to tag a method as callable using {@link CallbackReceiver#createRemoteCallback}.
 * <p>
 * This is only valid on methods on concrete classes that implement {@link CallbackReceiver}.
 * The method must have return type RemoteCallback, and should return {@link RemoteCallback#LOCAL}.
 * <p>
 * At compile time Methods tagged with {@link RemoteCallable} have hooks generated for
 * them. The vast majority of the calls are done through generated code directly,
 * so everything except for class names can be optimized/obfuscated. Given that
 * remote callbacks are only accessible on platform components such as receivers
 * and providers, they are already generally not able to be obfuscated.
 *
 * @see CallbackReceiver#createRemoteCallback
 *
 * @deprecated Slice framework has been deprecated, it will not receive any updates moving
 * forward. If you are looking for a framework that handles communication across apps,
 * consider using {@link android.app.appsearch.AppSearchManager}.
 */
@Deprecated
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface RemoteCallable {
}
