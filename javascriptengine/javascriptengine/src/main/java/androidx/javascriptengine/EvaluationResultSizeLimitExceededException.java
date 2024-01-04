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

/**
 * Indicates that a JavaScriptIsolate's evaluation failed due to it returning an oversized result.
 * <p>
 * This exception is produced when exceeding the size limit configured for the isolate via
 * {@link IsolateStartupParameters}, or the default limit.
 * <p>
 * The isolate may continue to be used after this exception has been thrown.
 */
public final class EvaluationResultSizeLimitExceededException extends JavaScriptException {
    public EvaluationResultSizeLimitExceededException(@NonNull String error) {
        super(error);
    }
    public EvaluationResultSizeLimitExceededException() {
        super();
    }
}
