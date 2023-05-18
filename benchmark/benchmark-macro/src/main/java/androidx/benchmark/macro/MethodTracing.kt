package androidx.benchmark.macro

import android.content.Context
import androidx.annotation.RestrictTo
import androidx.benchmark.Outputs
import androidx.benchmark.Shell
import androidx.benchmark.getFirstMountedMediaDir
import androidx.benchmark.userspaceTrace
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File

private const val OPTION_SAMPLED = "Sampled"
private const val RECEIVER_NAME = "androidx.benchmark.internal.MethodTracingReceiver"

// Extras
private const val ACTION = "ACTION"
private const val UNIQUE_NAME = "UNIQUE_NAME"

// Actions
private const val METHOD_TRACE_START = "METHOD_TRACE_START"
private const val METHOD_TRACE_START_SAMPLED = "METHOD_TRACE_START_SAMPLED"
private const val METHOD_TRACE_END = "ACTION_METHOD_TRACE_END"

@RestrictTo(RestrictTo.Scope.LIBRARY)
object MethodTracing {
    private val context: Context = InstrumentationRegistry.getInstrumentation().context

    /**
     * Starts method tracing on the [packageName].
     * <br/>
     * The only [options] supported are `Full` and `Sampled`.
     */
    fun startTracing(packageName: String, uniqueName: String, options: String) {
        val fileName = fileName(uniqueName)
        // The only options possible are `Full` and `Sampled`
        val extras = if (options == (OPTION_SAMPLED)) {
            "-e $ACTION $METHOD_TRACE_START_SAMPLED -e $UNIQUE_NAME $fileName"
        } else {
            "-e $ACTION $METHOD_TRACE_START -e $UNIQUE_NAME $fileName"
        }
        broadcast(packageName, extras)
    }

    /**
     * Stops method tracing and copies the output trace to the `additionalTestOutputDir`.
     */
    fun stopTracing(packageName: String, uniqueName: String) {
        val fileName = fileName(uniqueName)
        val extras = "-e $ACTION $METHOD_TRACE_END"
        broadcast(packageName, extras)
        // The reason we are doing this is to make trace collection possible.
        // The target APK stores the method traces in the first media mounted directory.
        // This is because, that happens to be the only directory accessible to the app and shell.
        // We then subsequently copy it to the actual `Outputs.dirUsableByAppAndShell` from the
        // perspective of the test APK.
        val mediaDirParent = context.getFirstMountedMediaDir()?.parentFile
        val sourcePath = "$mediaDirParent/$packageName/$fileName"
        // Staging location before we write it again using Outputs.writeFile(...)
        // :(
        val stagingPath = "${Outputs.dirUsableByAppAndShell}/_$fileName"
        Shell.executeScriptSilent("cp '$sourcePath' '$stagingPath'")
        // Report
        Outputs.writeFile(fileName, fileName) {
            val staging = File(stagingPath)
            // No need to clean up, here because the clean up happens automatically on subsequent
            // test runs.
            staging.copyTo(it, overwrite = true)
        }
    }

    private fun fileName(uniqueName: String): String {
        return "${uniqueName}_method.trace"
    }

    private fun broadcast(targetPackageName: String, extras: String) {
        userspaceTrace("methodTracingBroadcast") {
            val action = "androidx.benchmark.experiments.ACTION_METHOD_TRACE"
            val result =
                Shell.amBroadcast("-a $action $extras $targetPackageName/$RECEIVER_NAME") ?: 0
            require(result > 0) {
                """
                    Operation with $extras failed (result code $result).
                    Make sure you add a dependency on:
                    `project(":benchmark:benchmark-internal")`
                """.trimIndent()
            }
        }
    }
}
