/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.build.gradle.internal.fixtures

import java.io.File
import java.io.InputStream
import java.io.OutputStream
import org.gradle.api.Action
import org.gradle.process.BaseExecSpec
import org.gradle.process.CommandLineArgumentProvider
import org.gradle.process.ExecOperations
import org.gradle.process.ExecResult
import org.gradle.process.ExecSpec
import org.gradle.process.JavaExecSpec
import org.gradle.process.ProcessForkOptions

/** This implementation only captures arguments used to launch process, it does not run it. */
class FakeGradleExecOperations : ExecOperations {
    var capturedExecutions = mutableListOf<ExecSpec>()

    override fun exec(p0: Action<in ExecSpec>): ExecResult {
        p0.execute(CapturingExecSpec().also { capturedExecutions.add(it) })
        return object : ExecResult {
            override fun getExitValue() = 1

            override fun assertNormalExitValue() = this

            override fun rethrowFailure() = this
        }
    }

    override fun javaexec(p0: Action<in JavaExecSpec>?): ExecResult {
        TODO("Not yet implemented")
    }
}

class CapturingExecSpec : ExecSpec {
    private var executable: String? = null
    private val commandLine: MutableList<String> = mutableListOf()
    private var environment: MutableMap<String, Any> = mutableMapOf()
    private val args: MutableList<String> = mutableListOf()

    override fun getExecutable() = executable

    override fun setExecutable(p0: Any) {
        executable = p0.toString()
    }

    override fun setExecutable(p0: String) {
        executable = p0
    }

    override fun setStandardOutput(p0: OutputStream): BaseExecSpec {
        // ignore
        return this
    }

    override fun setCommandLine(p0: MutableList<String>) {
        commandLine.clear()
        commandLine.addAll(p0)
    }

    override fun setCommandLine(vararg p0: Any) {
        commandLine.clear()
        commandLine.add(p0.toString())
    }

    override fun setCommandLine(p0: MutableIterable<*>) {
        commandLine.clear()
        p0.filterNotNull().forEach { commandLine.add(it.toString()) }
    }

    override fun environment(p0: MutableMap<String, *>): ProcessForkOptions {
        p0.forEach { (t, any) -> environment[t] = any.toString() }
        return this
    }

    override fun environment(p0: String, p1: Any): ProcessForkOptions {
        environment[p0] = p1.toString()
        return this
    }

    override fun commandLine(vararg p0: Any): ExecSpec {
        commandLine.add(p0.toString())
        return this
    }

    override fun commandLine(p0: MutableIterable<*>): ExecSpec {
        commandLine.addAll(p0.filterNotNull().map { it.toString() })
        return this
    }

    override fun workingDir(p0: Any?): ProcessForkOptions {
        TODO("Not yet implemented")
    }

    override fun setWorkingDir(p0: File?) {
        TODO("Not yet implemented")
    }

    override fun setWorkingDir(p0: Any?) {
        TODO("Not yet implemented")
    }

    override fun getCommandLine(): MutableList<String> {
        return commandLine
    }

    override fun setEnvironment(p0: MutableMap<String, *>) {
        environment.clear()
        environment(p0)
    }

    override fun args(vararg p0: Any): ExecSpec {
        args.add(p0.toString())
        return this
    }

    override fun args(p0: MutableIterable<*>): ExecSpec {
        args.addAll(p0.filterNotNull().map { it.toString() })
        return this
    }

    override fun getArgumentProviders(): MutableList<CommandLineArgumentProvider> {
        TODO("Not yet implemented")
    }

    override fun executable(p0: Any): ProcessForkOptions {
        executable = p0.toString()
        return this
    }

    override fun setIgnoreExitValue(p0: Boolean): BaseExecSpec {
        // ignore
        return this
    }

    override fun getStandardInput(): InputStream {
        TODO("Not yet implemented")
    }

    override fun setStandardInput(p0: InputStream?): BaseExecSpec {
        // ignore
        return this
    }

    override fun getStandardOutput(): OutputStream {
        TODO("Not yet implemented")
    }

    override fun setErrorOutput(p0: OutputStream?): BaseExecSpec {
        // ignore
        return this
    }

    override fun getArgs(): MutableList<String> {
        return args
    }

    override fun copyTo(p0: ProcessForkOptions?): ProcessForkOptions {
        TODO("Not yet implemented")
    }

    override fun isIgnoreExitValue(): Boolean {
        TODO("Not yet implemented")
    }

    override fun getWorkingDir(): File {
        TODO("Not yet implemented")
    }

    override fun getEnvironment(): MutableMap<String, Any> {
        return environment
    }

    override fun setArgs(p0: MutableList<String>): ExecSpec {
        args.clear()
        args.addAll(p0)
        return this
    }

    override fun setArgs(p0: MutableIterable<*>): ExecSpec {
        args.clear()
        args.addAll(p0.filterNotNull().map { it.toString() })
        return this
    }

    override fun getErrorOutput(): OutputStream {
        TODO("Not yet implemented")
    }
}
