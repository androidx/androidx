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

import androidx.annotation.IntRange;
import androidx.annotation.RequiresFeature;

/**
 * Class used to set startup parameters for {@link JavaScriptIsolate}.
 */
public final class IsolateStartupParameters {
    private long mMaxHeapSizeBytes = AUTOMATIC_MAX_HEAP_SIZE;
    private int mMaxEvaluationReturnSizeBytes = DEFAULT_MAX_EVALUATION_RETURN_SIZE_BYTES;
    /**
     * Special value for automatically selecting a heap size limit (which may be
     * device-specific) when the isolate is created. This is the default setting for max heap size.
     */
    public static final long AUTOMATIC_MAX_HEAP_SIZE = 0;
    /**
     * Default maximum size in bytes for evaluation returns/errors.
     */
    public static final int DEFAULT_MAX_EVALUATION_RETURN_SIZE_BYTES = 20 * 1024 * 1024;

    public IsolateStartupParameters() {
    }

    /**
     * Sets the max heap size used by the {@link JavaScriptIsolate}.
     * <p>
     * A setting of {@link IsolateStartupParameters#AUTOMATIC_MAX_HEAP_SIZE} indicates to
     * automatically chose a limit (which may be device-specific) when the isolate is created. This
     * is the default.
     * <p>
     * If a value higher than the device-specific maximum heap size limit is supplied, the device's
     * maximum limit will be used as the heap size limit.
     * <p>
     * The applied limit may not be exact. For example, the limit may internally be rounded up to
     * some multiple of bytes, be increased to some minimum value, or reduced to some maximum
     * supported value.
     * <p>
     * Exceeding this limit will usually result in all unfinished and future evaluations failing
     * with {@link MemoryLimitExceededException} and the isolate terminating with a status of
     * {@link TerminationInfo#STATUS_MEMORY_LIMIT_EXCEEDED}. Note that exceeding the memory limit
     * will take down the entire sandbox - not just the responsible isolate - and all other
     * isolates will receive generic {@link SandboxDeadException} and
     * {@link TerminationInfo#STATUS_SANDBOX_DEAD} errors.
     * <p>
     * Not all JavaScript sandbox service implementations (particularly older ones) handle memory
     * exhaustion equally, and may crash the sandbox without attributing the failure to memory
     * exhaustion in a particular isolate.
     *
     * @param size {@link IsolateStartupParameters#AUTOMATIC_MAX_HEAP_SIZE} or a heap size limit in
     *             bytes
     */
    @RequiresFeature(name = JavaScriptSandbox.JS_FEATURE_ISOLATE_MAX_HEAP_SIZE,
            enforcement = "androidx.javascriptengine.JavaScriptSandbox#isFeatureSupported")
    public void setMaxHeapSizeBytes(@IntRange(from = 0) long size) {
        if (size < 0) {
            throw new IllegalArgumentException("maxHeapSizeBytes should be >= 0");
        }
        mMaxHeapSizeBytes = size;
    }

    /**
     * Sets the max size for evaluation return values and errors in the {@link JavaScriptIsolate}.
     * <p>
     * The default value is
     * {@link IsolateStartupParameters#DEFAULT_MAX_EVALUATION_RETURN_SIZE_BYTES}.
     * <p>
     * If an evaluation exceeds this limit, {@link EvaluationResultSizeLimitExceededException}
     * is produced. Error messages will be truncated to adhere to this limit.
     *
     * @param size max size in bytes
     */
    @RequiresFeature(name = JavaScriptSandbox.JS_FEATURE_EVALUATE_WITHOUT_TRANSACTION_LIMIT,
            enforcement = "androidx.javascriptengine.JavaScriptSandbox#isFeatureSupported")
    public void setMaxEvaluationReturnSizeBytes(
            @IntRange(from = 0) int size) {
        if (size < 0) {
            throw new IllegalArgumentException("maxEvaluationReturnSizeBytes must be >= 0");
        }
        mMaxEvaluationReturnSizeBytes = size;
    }

    /**
     * Gets the max heap size used by the {@link JavaScriptIsolate}.
     * <p>
     * The default value is {@link IsolateStartupParameters#AUTOMATIC_MAX_HEAP_SIZE} which
     * indicates a limit (which may be device-specific) will be chosen automatically when the
     * isolate is created.
     *
     * @return {@link IsolateStartupParameters#AUTOMATIC_MAX_HEAP_SIZE} or a heap size limit in
     * bytes
     */
    public @IntRange(from = 0) long getMaxHeapSizeBytes() {
        return mMaxHeapSizeBytes;
    }

    /**
     * Gets the max size for evaluation return values and errors in the {@link JavaScriptIsolate}.
     * <p>
     * If not set using {@link IsolateStartupParameters#setMaxEvaluationReturnSizeBytes(int)}, the
     * default value is {@link IsolateStartupParameters#DEFAULT_MAX_EVALUATION_RETURN_SIZE_BYTES}.
     *
     * @return max size in bytes
     */
    public @IntRange(from = 0) int getMaxEvaluationReturnSizeBytes() {
        return mMaxEvaluationReturnSizeBytes;
    }
}
