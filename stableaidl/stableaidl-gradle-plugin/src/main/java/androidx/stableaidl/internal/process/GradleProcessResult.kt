/*
 * Copyright 2023 The Android Open Source Project
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
package androidx.stableaidl.internal.process

import com.android.ide.common.process.ProcessException
import com.android.ide.common.process.ProcessInfo
import com.android.ide.common.process.ProcessResult
import com.google.common.base.Joiner
import org.gradle.process.ExecResult
import org.gradle.process.internal.ExecException

/**
 * Cloned from `com.android.build.gradle.internal.process.GradleProcessResult`.
 */
internal class GradleProcessResult(
    private val result: ExecResult,
    private val processInfo: ProcessInfo
) : ProcessResult {
    @Throws(ProcessException::class)
    override fun assertNormalExitValue(): ProcessResult {
        try {
            result.assertNormalExitValue()
        } catch (e: ExecException) {
            throw buildProcessException(e)
        }
        return this
    }

    override fun getExitValue(): Int {
        return result.exitValue
    }

    @Throws(ProcessException::class)
    override fun rethrowFailure(): ProcessResult {
        try {
            result.rethrowFailure()
        } catch (e: ExecException) {
            throw buildProcessException(e)
        }
        return this
    }

    private fun buildProcessException(e: ExecException): ProcessException {
        return ProcessException(
            String.format(
                "Error while executing %s with arguments {%s}",
                processInfo.description,
                Joiner.on(' ').join(processInfo.args)
            ),
            e
        )
    }
}
