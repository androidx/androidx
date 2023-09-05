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

package androidx.javascriptengine;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.core.util.Consumer;

import java.util.concurrent.Executor;

/**
 * Exception thrown when evaluation is terminated due to the {@link JavaScriptIsolate} being closed
 * or due to some crash.
 * <p>
 * Calling {@link JavaScriptIsolate#close()} will cause this exception to be thrown for all
 * previously requested but pending evaluations.
 * <p>
 * If an isolate has crashed (but not been closed), subsequently requested evaluations will fail
 * immediately with an IsolateTerminatedException (or a subclass) consistent with that
 * used for evaluations submitted before the crash.
 * <p>
 * Note that this exception will not be thrown if the isolate has been explicitly closed before a
 * call to {@link JavaScriptIsolate#evaluateJavaScriptAsync(String)}, which will instead immediately
 * throw an IllegalStateException (and not asynchronously via a future). This applies even if the
 * isolate was closed following a crash.
 * <p>
 * Do not attempt to parse the information in this exception's message as it may change between
 * JavaScriptEngine versions.
 * <p>
 * Note that it is possible for an isolate to crash outside of submitted evaluations, in which
 * case an IsolateTerminatedException may not be observed. Consider instead using
 * {@link JavaScriptIsolate#addOnTerminatedCallback(Executor, Consumer)} if you need to reliably
 * or immediately detect isolate crashes rather than evaluation failures.
 */
public class IsolateTerminatedException extends JavaScriptException {
    public IsolateTerminatedException() {
        super();
    }
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public IsolateTerminatedException(@NonNull String message) {
        super(message);
    }
}
