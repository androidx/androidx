/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.benchmark.macro

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.benchmark.Arguments
import androidx.benchmark.DeviceInfo
import androidx.benchmark.InstrumentationResults
import androidx.benchmark.Outputs
import androidx.benchmark.Profiler
import androidx.benchmark.Shell
import androidx.benchmark.macro.MacrobenchmarkScope.Companion.Api24ContextHelper.createDeviceProtectedStorageContextCompat
import androidx.benchmark.macro.perfetto.forceTrace
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiAutomatorTestScope
import androidx.tracing.trace
import java.io.File

/**
 * Provides access to common operations in app automation, such as killing the app, or navigating
 * home.
 */
public class MacrobenchmarkScope(
    /** ApplicationId / Package name of the app being tested. */
    val packageName: String,
    /**
     * Controls whether launches will automatically set [Intent.FLAG_ACTIVITY_CLEAR_TASK].
     *
     * Default to true, so Activity launches go through full creation lifecycle stages, instead of
     * just resume.
     */
    private val launchWithClearTask: Boolean,
) : UiAutomatorTestScope() {

    internal val context = instrumentation.context

    /** The per-iteration file label used as a prefix when storing Macrobenchmark results. */
    internal lateinit var fileLabel: String

    /**
     * Controls if the process will be launched with method tracing turned on.
     *
     * Default to false, because we only want to turn on method tracing when explicitly enabled via
     * `Arguments.methodTracingOptions`.
     */
    private var isMethodTracingActive: Boolean = false

    /** This is `true` iff method tracing is currently active for this benchmarking session. */
    private var isMethodTracingSessionActive: Boolean = false

    /** Additional flags that can be used to determine the type of process kill. */
    internal data class KillMode(
        /**
         * When `true`, we are not strict about the process staying dead after it was initially
         * killed. This is useful in the context of generating a Baseline profile for System apps
         * which end up restarting right-away after a process kill happens.
         */
        val isKillSoftly: Boolean = false,
        /**
         * When used, the app will be forced to flush its ART profiles to disk before being killed.
         * This allows them to be later collected e.g. by a `BaselineProfile` capture, or immediate
         * compilation by [CompilationMode.Partial] with warmupIterations.
         */
        val flushArtProfiles: Boolean = false,
        /**
         * After killing the process, clear any potential runtime image.
         *
         * Starting in API 34 (and below with mainline), `verify` complied apps will attempt to
         * store initialized classes to disk directly. To consistently capture worst case `verify`
         * performance, this means macrobenchmark must recompile the target app with `verify`.
         *
         * @See DeviceInfo.supportsRuntimeImages
         */
        val clearArtRuntimeImage: Boolean = false,
    ) {
        companion object {
            /** The default kill mode. Kills the process, strictly. */
            internal val None = KillMode()
        }
    }

    internal inline fun <T> withKillMode(
        current: KillMode,
        override: KillMode,
        block: MacrobenchmarkScope.() -> T,
    ): T {
        check(killMode == current) { "Expected KFM = $current, was $killMode" }
        killMode = override
        try {
            return block(this)
        } finally {
            check(killMode == override) { "Expected KFM at end to be = $override, was $killMode" }
            killMode = current
        }
    }

    internal var killMode: KillMode = KillMode.None
        private set(value) {
            hasFlushedArtProfiles = false
            field = value
        }

    /**
     * When `true`, the app has successfully flushed art profiles at least once.
     *
     * This will only be set by [killProcessAndFlushArtProfiles] when called directly, or
     * [killProcess] when [KillMode.flushArtProfiles] is used.
     */
    internal var hasFlushedArtProfiles: Boolean = false
        private set

    /**
     * When `true`, the app has attempted to flush the runtime image during [killProcess].
     *
     * This will only be set by [killProcess] when [KillMode.clearArtRuntimeImage] is used.
     */
    internal var hasClearedRuntimeImage: Boolean = false
        private set

    /**
     * Current Macrobenchmark measurement iteration, or null if measurement is not yet enabled.
     *
     * Non-measurement iterations can occur due to warmup a [CompilationMode], or prior to the first
     * iteration for [StartupMode.WARM] or [StartupMode.HOT], to create the Process or Activity
     * ahead of time.
     */
    @get:Suppress("AutoBoxing") // low frequency, non-perf-relevant part of test
    var iteration: Int? = null
        internal set

    /**
     * The list of method traces accumulated during a benchmarking session. The [Pair] has the label
     * and the absolute path to the trace. These should be reported at the end of a Macro
     * benchmarking session, if method tracing was on.
     */
    private val methodTraces: MutableList<Pair<String, String>> = mutableListOf()

    /**
     * Start an activity, by default the launcher activity of the package, and wait until its launch
     * completes.
     *
     * This call supports primitive extras on the intent, but will ignore any
     * [android.os.Parcelable] extras, as the start is performed by converting the Intent to a URI,
     * and starting via the `am start` shell command. Note that from api 33 the launch intent needs
     * to have category `android.intent.category.LAUNCHER`.
     *
     * @param block Allows customization of the intent used to launch the activity.
     * @throws IllegalStateException if unable to acquire intent for package.
     */
    @JvmOverloads
    public fun startActivityAndWait(block: (Intent) -> Unit = {}) {
        val intent =
            context.packageManager.getLaunchIntentForPackage(packageName)
                ?: context.packageManager.getLeanbackLaunchIntentForPackage(packageName)
                ?: throw IllegalStateException("Unable to acquire intent for package $packageName")

        block(intent)
        startActivityAndWait(intent)
    }

    /**
     * Start an activity with the provided intent, and wait until its launch completes.
     *
     * This call supports primitive extras on the intent, but will ignore any
     * [android.os.Parcelable] extras, as the start is performed by converting the Intent to a URI,
     * and starting via the `am start` shell command. Note that from api 33 the launch intent needs
     * to have category `android.intent.category.LAUNCHER`.
     *
     * @param intent Specifies which app/Activity should be launched.
     */
    public fun startActivityAndWait(intent: Intent): Unit =
        forceTrace("startActivityAndWait") {
            // Must launch with new task, as we're not launching from an existing task
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (launchWithClearTask) {
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }

            // Note: intent.toUri(0) produces a String that can't be parsed by `am start-activity`.
            // intent.toUri(Intent.URI_ANDROID_APP_SCHEME) also works though.
            startActivityImpl(intent.toUri(Intent.URI_INTENT_SCHEME))
        }

    private fun startActivityImpl(uri: String) {
        if (
            isMethodTracingActive &&
                !isMethodTracingSessionActive &&
                !Shell.isPackageAlive(packageName)
        ) {
            isMethodTracingSessionActive = true
            // Use the canonical trace path for the given package name.
            val tracePath = methodTraceRecordPath(packageName)
            val profileArgs = "--start-profiler \"$tracePath\" --streaming"
            amStartAndWait(uri, profileArgs)
        } else {
            amStartAndWait(uri)
        }
    }

    override fun startActivityIntent(intent: Intent) {
        startActivityAndWait(intent)
    }

    @SuppressLint("BanThreadSleep") // Cannot always detect activity launches.
    private fun amStartAndWait(uri: String, profilingArgs: String? = null) {
        val ignoredUniqueNames =
            if (!launchWithClearTask) {
                emptyList()
            } else {
                // ignore existing names, as we expect a new window
                getFrameStats().map { it.uniqueName }
            }

        val preLaunchTimestampNs = System.nanoTime()

        val additionalArgs = profilingArgs ?: ""
        val cmd = "am start $additionalArgs -W \"$uri\""
        Log.d(TAG, "Starting activity with command: $cmd")

        // executeShellScript used to access stderr, and avoid need to escape special chars like `;`
        val result = Shell.executeScriptCaptureStdoutStderr(cmd)

        if (result.stderr.contains("java.lang.SecurityException")) {
            throw SecurityException(result.stderr)
        }
        if (result.stderr.isNotEmpty()) {
            throw IllegalStateException(result.stderr)
        }

        val outputLines = result.stdout.split("\n").map { it.trim() }

        // Check for errors
        outputLines.forEach {
            if (it.startsWith("Error:")) {
                throw IllegalStateException(it)
            }
        }

        Log.d(TAG, "Result: ${result.stdout}")

        if (outputLines.any { it.startsWith("Warning: Activity not started") }) {
            // Intent was sent to running activity, which may not produce a new frame.
            // Since we can't be sure, simply sleep and hope launch has completed.
            Log.d(TAG, "Unable to safely detect Activity launch, waiting 2s")
            Thread.sleep(2000)
            return
        }

        if (!Shell.isPackageAlive(packageName)) {
            throw IllegalStateException(
                "Target package $packageName is not running," +
                    " check logcat to verify the activity launched correctly"
            )
        }

        // `am start -W` doesn't reliably wait for process to complete and renderthread to produce
        // a new frame (b/226179160), so we use `dumpsys gfxinfo <package> framestats` to determine
        // when the next frame is produced.
        var lastFrameStats: List<FrameStatsResult> = emptyList()
        repeat(100) {
            lastFrameStats = getFrameStats()
            if (
                lastFrameStats.any {
                    it.uniqueName !in ignoredUniqueNames &&
                        it.lastFrameNs != null &&
                        it.lastFrameNs > preLaunchTimestampNs
                }
            ) {
                return // success, launch observed!
            }

            trace("wait for $packageName to draw") {
                // Note - sleep must not be long enough to miss activity initial draw in 120 frame
                // internal ring buffer of `dumpsys gfxinfo <pkg> framestats`.
                Thread.sleep(100)
            }
        }
        throw IllegalStateException(
            "Unable to confirm activity launch completion $lastFrameStats" +
                " Please report a bug with the output of" +
                " `adb shell dumpsys gfxinfo $packageName framestats`"
        )
    }

    /**
     * Uses `dumpsys gfxinfo <pkg> framestats` to detect the initial timestamp of the most recently
     * completed (fully rendered) activity launch frame.
     */
    internal fun getFrameStats(): List<FrameStatsResult> {
        // iterate through each subprocess, since UI may not be in primary process
        return Shell.getRunningProcessesForPackage(packageName).flatMap { processName ->
            val frameStatsOutput =
                trace("dumpsys gfxinfo framestats") {
                    // we use framestats here because it gives us not just frame counts, but actual
                    // timestamps for new activity starts. Frame counts would mostly work, but would
                    // have false positives if some window of the app is still animating/rendering.
                    Shell.executeScriptCaptureStdout("dumpsys gfxinfo $processName framestats")
                }
            FrameStatsResult.parse(frameStatsOutput)
        }
    }

    /**
     * Perform a home button click.
     *
     * Useful for resetting the test to a base condition in cases where the app isn't killed in each
     * iteration.
     */
    @JvmOverloads
    @SuppressLint("BanThreadSleep") // Defaults to no delays at all.
    public fun pressHome(delayDurationMs: Long = 0) {
        device.pressHome()

        // This delay is unnecessary, since UiAutomator's pressHome already waits for device idle.
        // This sleep remains just for API stability.
        Thread.sleep(delayDurationMs)
    }

    /**
     * Force-stop the process being measured.
     *
     * @param useKillAll should be set to `true` for System apps or pre-installed apps.
     */
    @Deprecated(
        "Use the parameter-less killProcess() API instead",
        replaceWith = ReplaceWith("killProcess()"),
    )
    @Suppress("UNUSED_PARAMETER")
    fun killProcess(useKillAll: Boolean = false) {
        killProcess()
    }

    /** Force-stop the process being measured. */
    fun killProcess() {
        // Method traces are only flushed is a method tracing session is active.
        flushMethodTraces()

        if (killMode.flushArtProfiles && Build.VERSION.SDK_INT >= 24) {
            // Flushing ART profiles will also kill the process at the end.
            killProcessAndFlushArtProfiles()
        } else {
            killProcessImpl()
            if (killMode.clearArtRuntimeImage && Build.VERSION.SDK_INT >= 24) {
                if (DeviceInfo.verifyClearsRuntimeImage) {
                    // clear the runtime image
                    CompilationMode.cmdPackageCompile(packageName, "verify")
                } else if (Shell.isSessionRooted()) {
                    CompilationMode.cmdPackageCompileReset(packageName)
                } else {
                    // TODO - follow up!
                    // b/368404173
                    InstrumentationResults.scheduleIdeWarningOnNextReport(
                        "Unable to clear Runtime Image, subsequent launches/iterations may" +
                            " exhibit faster startup than production due to accelerated class" +
                            " loading."
                    )
                }
            }
        }
    }

    /**
     * Deletes the shader cache for an application.
     *
     * Used by `measureRepeated(startupMode = StartupMode.COLD)` to remove compiled shaders for each
     * measurement, to ensure their cost is captured each time.
     *
     * Requires `profileinstaller` 1.3.0-alpha02 to be used by the target, or a rooted device.
     *
     * @throws IllegalStateException if the device is not rooted, and the target app cannot be
     *   signalled to drop its shader cache.
     */
    fun dropShaderCache() {
        if (Arguments.dropShadersEnable) {
            Log.d(TAG, "Dropping shader cache for $packageName")
            val dropError = ProfileInstallBroadcast.dropShaderCache(packageName)
            if (dropError != null && !DeviceInfo.isEmulator) {
                if (!dropShaderCacheRoot()) {
                    if (Arguments.dropShadersThrowOnFailure) {
                        throw IllegalStateException(dropError)
                    } else {
                        Log.d(TAG, dropError)
                    }
                }
            }
        } else {
            Log.d(TAG, "Skipping drop shader cache for $packageName")
        }
    }

    /**
     * Returns true if rooted, and delete operation succeeded without error.
     *
     * Note that if no files are present in the shader dir, true will still be returned.
     */
    internal fun dropShaderCacheRoot(): Boolean {
        if (Shell.isSessionRooted()) {
            // fall back to root approach
            val path = getShaderCachePath(packageName)

            // Use -f to allow missing files, since app may not have generated shaders.
            Shell.executeScriptSilent("find $path -type f | xargs rm -f")
            return true
        }
        return false
    }

    /**
     * Drop caches via setprop added in API 31
     *
     * Feature for dropping caches without root added in 31: https://r.android.com/1584525 Passing 3
     * will cause caches to be dropped, and prop will go back to 0 when it's done
     */
    @RequiresApi(31)
    @SuppressLint("BanThreadSleep") // Need to poll to drop kernel page caches
    private fun dropKernelPageCacheSetProp() {
        val result = Shell.executeScriptCaptureStdoutStderr("setprop perf.drop_caches 3")
        check(result.stdout.isEmpty() && result.stderr.isEmpty()) {
            "Failed to trigger drop cache via setprop: $result"
        }
        // Polling duration is very conservative, on Pixel 4L finishes in ~150ms
        repeat(50) {
            Thread.sleep(50)
            when (val getPropResult = Shell.getprop("perf.drop_caches")) {
                "0" -> return // completed!
                "3" -> {} // not completed, continue
                else ->
                    throw IllegalStateException(
                        "Unable to drop caches: Failed to read drop cache via getprop: $getPropResult"
                    )
            }
        }
        throw IllegalStateException(
            "Unable to drop caches: Did not observe perf.drop_caches reset automatically"
        )
    }

    @RequiresApi(24)
    internal fun killProcessAndFlushArtProfiles(allowFlushWithBroadcast: Boolean = true) {
        Log.d(TAG, "Flushing ART profiles for $packageName")
        // For speed profile compilation, ART team recommended to wait for 5 secs when app
        // is in the foreground, dump the profile in each process waiting an additional second each
        // before speed-profile compilation.
        @Suppress("BanThreadSleep") Thread.sleep(5000)
        val saveResult =
            if (allowFlushWithBroadcast) {
                ProfileInstallBroadcast.saveProfilesForAllProcesses(packageName)
            } else {
                // test codepath only, force failed save result (as if broadcast receiver not
                // present)
                val processCount = Shell.getRunningPidsAndProcessesForPackage(packageName).size
                ProfileInstallBroadcast.SaveProfileResult(
                    processCount = processCount,
                    error = if (processCount == 0) null else "skipped",
                )
            }
        if (saveResult.processCount > 0) {
            if (saveResult.error != null) {
                if (Shell.isSessionRooted()) {
                    Log.d(
                        TAG,
                        "Unable to saveProfile with profileinstaller ($saveResult), trying kill",
                    )
                    val response =
                        Shell.executeScriptCaptureStdoutStderr("killall -s SIGUSR1 $packageName")
                    check(response.isBlank()) {
                        "Failed to dump profile for $packageName ($response),\n" +
                            " and failed to save profile with broadcast: ${saveResult.error}"
                    }
                    @SuppressLint("BanThreadSleep") Thread.sleep(Arguments.saveProfileWaitMillis)
                } else {
                    // unable to flush profiles, throw
                    throw RuntimeException(saveResult.error)
                }
            }
            // we only mark flush successful and kill the target if running processes found
            Log.d(TAG, "Flushed profiles in ${saveResult.processCount} processes")
            hasFlushedArtProfiles = true
            killProcessImpl()
        }
    }

    /** Force-stop the process being measured. */
    private fun killProcessImpl() {
        val onFailure: (String) -> Unit = { errorMessage ->
            if (killMode.isKillSoftly) {
                Log.w(TAG, "Unable to kill process ($packageName): $errorMessage")
            } else {
                throw IllegalStateException(errorMessage)
            }
        }
        Shell.killProcessesAndWait(packageName, onFailure = onFailure) {
            Log.d(TAG, "Force-stopping process $packageName")
            Shell.executeScriptSilent("am force-stop $packageName")

            // System Apps need an additional Thread.sleep() to ensure that the process is killed.
            @Suppress("BanThreadSleep") Thread.sleep(Arguments.killProcessDelayMillis)
        }
    }

    /**
     * Drop Kernel's in-memory cache of disk pages.
     *
     * Enables measuring disk-based startup cost, without simply accessing cache of disk data held
     * in memory, such as during [cold startup](androidx.benchmark.macro.StartupMode.COLD).
     *
     * @Throws IllegalStateException if dropping the cache fails on a API 31+ or rooted device,
     *   where it is expected to work.
     */
    public fun dropKernelPageCache() {
        if (Build.VERSION.SDK_INT >= 31) {
            dropKernelPageCacheSetProp()
        } else {
            val result =
                Shell.executeScriptCaptureStdoutStderr(
                    "echo 3 > /proc/sys/vm/drop_caches && echo Success || echo Failure"
                )
            // Older user builds don't allow drop caches, should investigate workaround
            if (result.stdout.trim() != "Success") {
                if (DeviceInfo.isRooted && !Shell.isSessionRooted()) {
                    throw IllegalStateException("Failed to drop caches - run `adb root`")
                }
                Log.w(TAG, "Failed to drop kernel page cache, result: '$result'")
            }
        }
    }

    /**
     * Cancels the job responsible for running background `dexopt`.
     *
     * Background `dexopt` is a CPU intensive operation that can interfere with benchmarks. By
     * cancelling this job, we ensure that this operation will not interfere with the benchmark, and
     * we get stable numbers.
     */
    @RequiresApi(33)
    internal fun cancelBackgroundDexopt() {
        val result =
            if (Build.VERSION.SDK_INT >= 34) {
                Shell.executeScriptCaptureStdout("pm bg-dexopt-job --cancel")
            } else {
                // This command is deprecated starting Android U, and is just an alias for the
                // command above. More info in the link below.
                // https://cs.android.com/android/platform/superproject/main/+/main:art/libartservice/service/java/com/android/server/art/ArtShellCommand.java;l=123;drc=93f35d39de15c555b0ddea16121b0ee3f0aa9f91
                Shell.executeScriptCaptureStdout("pm cancel-bg-dexopt-job")
            }
        // We expect one of the following messages in stdout.
        val expected = listOf("Success", "Background dexopt job cancelled")
        if (expected.none { it == result.trim() }) {
            throw IllegalStateException("Failed to cancel background dexopt job, result: '$result'")
        }
    }

    /** Starts method tracing for the given [packageName]. */
    internal fun startMethodTracing() {
        require(!isMethodTracingActive && !isMethodTracingSessionActive) {
            "Method tracing should not already be active."
        }
        isMethodTracingActive = true
        // If the process is running, start a profiling session by connecting to the process.
        // Otherwise, given isMethodTracingActive = true, startActivityAndWait(...) will ensure
        // that a subsequent process launch happens with tracing turned on.
        if (Shell.isPackageAlive(packageName)) {
            isMethodTracingSessionActive = true
            // Use the canonical trace path for the given package name.
            val tracePath = methodTraceRecordPath(packageName)
            // Clock Type is only available starting Android V
            // https://cs.android.com/android/platform/superproject/main/+/main:frameworks/base/core/java/android/app/ProfilerInfo.java;l=115;drc=c58be09d9273485c54d6a16defc42d9f26182b73
            val clockType =
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    "--clock-type wall"
                } else {
                    ""
                }
            val arguments = "--streaming $clockType $packageName \"$tracePath\""
            Shell.executeScriptSilent("am profile start $arguments")
        }
    }

    /**
     * Stops a method tracing session for the provided [packageName]. This returns a list of traces
     * accumulated in an active Method tracing session.
     */
    internal fun stopMethodTracing(): List<Profiler.ResultFile> {
        require(isMethodTracingActive) {
            "startMethodTracing() must be called prior to a call to stopMethodTracing()."
        }
        // Only flushes method traces when a trace session is active.
        flushMethodTraces()
        isMethodTracingActive = false
        val results = methodTraces.map { Profiler.ResultFile.ofMethodTrace(it.first, it.second) }
        methodTraces.clear()
        return results
    }

    /**
     * Stops the current method tracing session and copies the output to the
     * `additionalTestOutputDir` if a session was active.
     */
    private fun flushMethodTraces() {
        if (isMethodTracingSessionActive) {
            isMethodTracingSessionActive = false
            val tracePath = methodTraceRecordPath(packageName)

            // We have to poll here as `am profile stop` is async, but it's hard to calibrate these
            // numbers, as different devices take drastically different amounts of time.
            // E.g. pixel 8 takes 100ms for it's full flush, while mokey takes 1700ms to start,
            // then a few hundred ms to complete.
            //
            // Ideally, we'd use the native approach that Studio profilers use (on P+):
            // https://cs.android.com/android-studio/platform/tools/base/+/mirror-goog-studio-main:transport/native/utils/activity_manager.cc;l=111;drc=a4c97db784418341c9f1be60b98ba22301b5ced8
            Shell.waitForFileFlush(
                tracePath,
                maxInitialFlushWaitIterations = 50, // up to 2.5 sec of waiting on flush to start
                maxStableFlushWaitIterations = 50, // up to 2.5 sec of waiting on flush to complete
                stableIterations = 8, // 400ms of stability after flush starts
                pollDurationMs = 50L,
            ) {
                Shell.executeScriptSilent("am profile stop $packageName")
            }
            // unique label so source is clear, dateToFileName so each run of test is unique on host
            val outputFileName = "$fileLabel-methodTracing-${Outputs.dateToFileName()}.trace"
            val stagingFile =
                File.createTempFile("methodTrace", null, Outputs.dirUsableByAppAndShell)
            // Staging location before we write it again using Outputs.writeFile(...)
            // NOTE: staging copy may be unnecessary if we just use a single `cp`
            Shell.cp(from = tracePath, to = stagingFile.absolutePath)

            // Report file
            val outputPath =
                Outputs.writeFile(outputFileName) {
                    Log.d(TAG, "Writing method traces to ${it.absolutePath}")
                    stagingFile.copyTo(it, overwrite = true)

                    // Cleanup
                    stagingFile.delete()
                    Shell.rm(tracePath)
                }
            val traceLabel = "MethodTrace iteration ${iteration ?: 0}"
            // Keep track of the label and the corresponding output paths.
            methodTraces += traceLabel to outputPath
        }
    }

    internal companion object {
        fun getShaderCachePath(packageName: String): String {
            val context = InstrumentationRegistry.getInstrumentation().context

            // Shader paths sourced from ActivityThread.java
            val shaderDirectory =
                if (Build.VERSION.SDK_INT >= 34) {
                    // U switched to cache dir, so it's not deleted on each app update
                    context.createDeviceProtectedStorageContextCompat().cacheDir
                } else if (Build.VERSION.SDK_INT >= 24) {
                    // shaders started using device protected storage context once it was added in N
                    context.createDeviceProtectedStorageContextCompat().codeCacheDir
                } else {
                    // getCodeCacheDir was added in L, but not used by platform for shaders until M
                    // as M is minApi of this library, that's all we support here
                    context.codeCacheDir
                }
            return shaderDirectory.absolutePath.replace(context.packageName, packageName)
        }

        /** Path for method trace during record, before fully flushed/stopped, move to outputs */
        fun methodTraceRecordPath(packageName: String): String {
            return "/data/local/tmp/$packageName-method.trace"
        }

        @RequiresApi(Build.VERSION_CODES.N)
        internal object Api24ContextHelper {
            fun Context.createDeviceProtectedStorageContextCompat(): Context =
                createDeviceProtectedStorageContext()
        }
    }
}
