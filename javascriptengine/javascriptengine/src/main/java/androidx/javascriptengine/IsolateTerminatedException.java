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

/**
 * Exception thrown when evaluation is terminated due to the {@link JavaScriptIsolate} being closed
 * or crashing.
 * <p>
 * Calling {@link JavaScriptIsolate#close()} will cause this exception to be thrown for all
 * previously requested but pending evaluations.
 * <p>
 * If the individual isolate has crashed, for example, due to exceeding a memory limit, this
 * exception will also be thrown for all pending and future evaluations (until
 * {@link JavaScriptIsolate#close()} is called).
 * <p>
 * Note that if the sandbox as a whole has crashed or been closed, {@link SandboxDeadException} will
 * be thrown instead.
 * <p>
 * Note that this exception will not be thrown if the isolate has been explicitly closed before a
 * call to {@link JavaScriptIsolate#evaluateJavaScriptAsync(String)}, which will instead immediately
 * throw an IllegalStateException (and not asynchronously via a future). This applies even if the
 * isolate was closed following a crash.
 */
public final class IsolateTerminatedException extends JavaScriptException {
    public IsolateTerminatedException() {
        super();
    }
}
