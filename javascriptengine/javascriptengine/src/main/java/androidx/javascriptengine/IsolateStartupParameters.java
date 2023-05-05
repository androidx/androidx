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
    private long mMaxHeapSizeBytes;
    private int mMaxEvalutationReturnSizeBytes = DEFAULT_MAX_EVALUATION_RETURN_SIZE_BYTES;
    public static final long DEFAULT_ISOLATE_HEAP_SIZE = 0;
    /**
     * Default maximum size in bytes for evaluation returns/errors.
     */
    public static final int DEFAULT_MAX_EVALUATION_RETURN_SIZE_BYTES = 20 * 1024 * 1024;

    public IsolateStartupParameters() {
    }

    /**
     * Sets the max heap size used by the {@link JavaScriptIsolate}.
     *
     * A heap size of {@link IsolateStartupParameters#DEFAULT_ISOLATE_HEAP_SIZE} indicates no
     * limit.
     * <p>
     * If a value higher than the device specific maximum heap size limit is supplied, this limit
     * will be used as the maximum heap size.
     * <p>
     * The applied limit may not be exact. For example, the limit may internally be rounded up to
     * some multiple of bytes, be increased to some minimum value, or reduced to some maximum
     * supported value.
     * <p>
     * Exceeding this limit will usually result in a {@link MemoryLimitExceededException},
     * but beware that not all JavaScript sandbox service implementations (particularly older ones)
     * handle memory exhaustion equally gracefully, and may crash the entire sandbox (see
     * {@link SandboxDeadException}).
     * @param size heap size in bytes
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
     *
     * The default value is
     * {@link IsolateStartupParameters#DEFAULT_MAX_EVALUATION_RETURN_SIZE_BYTES}.
     *
     * If an evaluation exceeds this limit, {@link EvaluationResultSizeLimitExceededException}
     * is thrown. Errors will be truncated to adhere to this limit.
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
        mMaxEvalutationReturnSizeBytes = size;
    }

    /**
     * Gets the max heap size used by the {@link JavaScriptIsolate}.
     *
     * If not set using {@link IsolateStartupParameters#setMaxHeapSizeBytes(long)}, the default
     * value is {@link IsolateStartupParameters#DEFAULT_ISOLATE_HEAP_SIZE} which indicates no
     * heap size limit.
     *
     * @return heap size in bytes
     */
    public @IntRange(from = 0) long getMaxHeapSizeBytes() {
        return mMaxHeapSizeBytes;
    }

    /**
     * Gets the max size for evaluation return values and errors in the {@link JavaScriptIsolate}.
     *
     * If not set using {@link IsolateStartupParameters#setMaxEvaluationReturnSizeBytes(int)}, the
     * default value is {@link IsolateStartupParameters#DEFAULT_MAX_EVALUATION_RETURN_SIZE_BYTES}.
     *
     * @return max size in bytes
     */
    public @IntRange(from = 0) int getMaxEvaluationReturnSizeBytes() {
        return mMaxEvalutationReturnSizeBytes;
    }
}
