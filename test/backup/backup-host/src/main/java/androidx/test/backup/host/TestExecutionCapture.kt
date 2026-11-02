/*
 * Copyright (C) 2026 The Android Open Source Project
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

package androidx.test.backup.host

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CompletionException
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.ExecutionException
import java.util.logging.Logger
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.ThreadContextElement
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource

internal object InsideCompositeStepKey : CoroutineContext.Key<InsideCompositeStep>

/** Marks coroutine execution inside an outer composite step so nested calls are not re-recorded. */
internal object InsideCompositeStep : AbstractCoroutineContextElement(InsideCompositeStepKey)

/**
 * Propagates the active [TestExecutionCapture] across threads and coroutine dispatchers.
 *
 * Routes calls on controllers resolved in [org.junit.jupiter.api.BeforeAll] or a class constructor
 * to the active [org.junit.jupiter.api.Test] method capture.
 */
internal object ActiveCaptureContext {
    private val activeCaptures = CopyOnWriteArraySet<TestExecutionCapture>()
    private val threadStack = ThreadLocal<ArrayDeque<TestExecutionCapture>>()
    private val fallbackSuppressed = ThreadLocal<Boolean>()

    fun register(capture: TestExecutionCapture) {
        activeCaptures.add(capture)
    }

    fun unregister(capture: TestExecutionCapture) {
        activeCaptures.remove(capture)
        unbindCurrentThread(capture)
    }

    fun currentThreadBound(): TestExecutionCapture? {
        val stack = threadStack.get() ?: return null
        while (stack.isNotEmpty() && stack.last().isClosed) {
            stack.removeLast()
        }
        if (stack.isEmpty()) {
            threadStack.remove()
            return null
        }
        return stack.last()
    }

    fun isBoundOnCurrentThread(capture: TestExecutionCapture): Boolean =
        threadStack.get()?.any { !it.isClosed && it === capture } == true

    fun pushCurrentThread(capture: TestExecutionCapture) {
        if (capture.isClosed) return
        fallbackSuppressed.remove()
        val stack = threadStack.get() ?: ArrayDeque<TestExecutionCapture>().also(threadStack::set)
        if (stack.lastOrNull() !== capture) {
            stack.removeAll { it === capture || it.isClosed }
            stack.addLast(capture)
        }
    }

    fun unbindCurrentThread(capture: TestExecutionCapture) {
        val stack = threadStack.get() ?: return
        stack.removeAll { it === capture || it.isClosed }
        if (stack.isEmpty()) {
            threadStack.remove()
        }
    }

    fun clearCurrentThread() {
        threadStack.remove()
        fallbackSuppressed.remove()
    }

    fun current(): TestExecutionCapture? {
        currentThreadBound()?.let {
            return it
        }
        if (fallbackSuppressed.get() == true) {
            return null
        }
        return activeCaptures
            .filter { c -> !c.isClosed && activeCaptures.none { !it.isClosed && it.parent === c } }
            .singleOrNull()
    }

    fun contextElement(capture: TestExecutionCapture? = current()): CoroutineContext =
        if (capture != null) CaptureContextElement(capture) else EmptyCoroutineContext

    private data class SavedThreadState(
        val stack: ArrayDeque<TestExecutionCapture>?,
        val suppressed: Boolean?,
    )

    private class CaptureContextElement(private val capture: TestExecutionCapture) :
        AbstractCoroutineContextElement(Key), ThreadContextElement<SavedThreadState> {
        companion object Key : CoroutineContext.Key<CaptureContextElement>

        override fun updateThreadContext(context: CoroutineContext): SavedThreadState {
            val prevStack = threadStack.get()?.let { ArrayDeque(it) }
            val prevSuppressed = fallbackSuppressed.get()
            fallbackSuppressed.set(true)
            if (capture.isClosed) {
                threadStack.remove()
            } else {
                threadStack.set(ArrayDeque<TestExecutionCapture>().apply { addLast(capture) })
            }
            return SavedThreadState(prevStack, prevSuppressed)
        }

        override fun restoreThreadContext(context: CoroutineContext, oldState: SavedThreadState) {
            val live = oldState.stack?.apply { removeAll { it.isClosed } }
            if (live.isNullOrEmpty()) {
                threadStack.remove()
            } else {
                threadStack.set(live)
            }
            if (oldState.suppressed == true) {
                fallbackSuppressed.set(true)
            } else {
                fallbackSuppressed.remove()
            }
        }
    }
}

/**
 * Captures device participants, steps, archive summaries, and host logs for one test or class.
 *
 * Scoped to either a test class ([isShared] is `true`) or a test method ([isShared] is `false`).
 */
internal class TestExecutionCapture(
    val start: Instant,
    val isShared: Boolean,
    var testClassName: String?,
    var testMethodName: String?,
    var parent: TestExecutionCapture? = null,
) : CloseableResource, AutoCloseable {

    internal enum class StepKind {
        PREPARE,
        SEED_ACTION,
        BACKUP,
        CLEAR_DATA,
        RESTORE,
        VERIFY_ACTION,
        OTHER,
    }

    internal data class StepDraft(
        val isNumbered: Boolean,
        val kind: StepKind,
        val name: String,
        val controllerSerial: String?,
        val controllerRole: String,
        val shared: Boolean,
        val call: String,
        val durationMs: Long,
        val status: String,
        val returnedActionFailureMessage: String? = null,
        val thrownFailure: Throwable? = null,
    )

    internal data class SavedLogcatFile(val path: Path, val truncated: Boolean = false)

    internal data class ArchiveSnapshot(
        val fileName: String,
        val summaryValues: Map<String, String>,
        val rawTempCopy: Path?,
        val rawSizeBytes: Long,
    )

    private val lock = Any()
    private val logger = Logger.getLogger(TestExecutionCapture::class.java.name)

    val hostLogCollector: HostLogCollector = HostLogCollector()

    private val ownControllers = ArrayList<BackupRestoreControllerImpl>()
    internal val ownSteps = ArrayList<StepDraft>()
    private val ownStorages = ArrayList<StorageDomain>()
    private val ownActionTargets = ArrayList<BackupRulesChecker.TargetCoordinate>()
    private val tempFilesToDelete = ArrayList<Path>()

    private var ownTransportMode: BackupTransportMode? = null
    private var ownBackupSerial: String? = null
    private var ownRestoreSerial: String? = null
    private var ownHasSeenBackup: Boolean = false
    private var ownHasSeenRestore: Boolean = false
    private var ownArchiveSnapshot: ArchiveSnapshot? = null
    private var ownFirstCallFrame: StackTraceElement? = null
    private var sealedSharedLogSnapshot: Pair<String, Boolean>? = null

    @Volatile
    var isClosed: Boolean = false
        private set

    @Volatile var finalized: Boolean = false
    @Volatile var onUnfinalizedClose: (() -> Unit)? = null

    @Volatile var publishedFlowTelemetry: Boolean = false
    var preconditionFailed: Boolean = false

    init {
        HostLogManager.register(hostLogCollector)
        ActiveCaptureContext.register(this)
    }

    /** Returns a [CoroutineContext] propagating this capture across coroutine dispatchers. */
    fun coroutineContext(insideCompositeStep: Boolean = false): CoroutineContext =
        if (insideCompositeStep) {
            ActiveCaptureContext.contextElement(this) + InsideCompositeStep
        } else {
            ActiveCaptureContext.contextElement(this)
        }

    /**
     * Freezes the shared host-log prefix so post-[org.junit.jupiter.api.BeforeAll] logs do not leak
     * across tests.
     */
    fun sealSharedPrefix() {
        synchronized(lock) {
            if (sealedSharedLogSnapshot == null) {
                sealedSharedLogSnapshot =
                    hostLogCollector.snapshotText() to hostLogCollector.isOverflowTruncated()
            }
        }
    }

    /**
     * Returns the frozen [org.junit.jupiter.api.BeforeAll] host-log snapshot combined with any
     * ancestor prefixes.
     */
    fun sharedHostLogSnapshot(): Pair<String, Boolean> {
        val (ownText, ownTrunc) =
            synchronized(lock) {
                sealedSharedLogSnapshot
                    ?: (hostLogCollector.snapshotText() to hostLogCollector.isOverflowTruncated())
            }
        val parentCapture = parent ?: return ownText to ownTrunc
        val (parentText, parentTrunc) = parentCapture.sharedHostLogSnapshot()
        val combined =
            when {
                parentText.isEmpty() -> ownText
                ownText.isEmpty() -> parentText
                parentText.endsWith('\n') -> parentText + ownText
                else -> "$parentText\n$ownText"
            }
        return combined to (parentTrunc || ownTrunc)
    }

    val hasSeenBackup: Boolean
        get() = synchronized(lock) { ownHasSeenBackup } || (parent?.hasSeenBackup == true)

    val hasSeenRestore: Boolean
        get() = synchronized(lock) { ownHasSeenRestore } || (parent?.hasSeenRestore == true)

    val effectiveTransportMode: BackupTransportMode?
        get() = synchronized(lock) { ownTransportMode } ?: parent?.effectiveTransportMode

    fun addParticipant(controller: BackupRestoreControllerImpl) {
        synchronized(lock) { mergeController(ownControllers, controller) }
    }

    fun allControllers(): List<BackupRestoreControllerImpl> =
        collectMergedControllers().distinctBy { it.serialNumber }

    fun captureFirstCallSiteIfNeeded() {
        synchronized(lock) {
            if (ownFirstCallFrame != null) return
        }
        val frame = findRelevantStackFrame(Thread.currentThread().stackTrace, testClassName)
        if (frame != null) {
            synchronized(lock) {
                if (ownFirstCallFrame == null) {
                    ownFirstCallFrame = frame
                }
            }
        }
    }

    fun recordFlowEndpoints(
        mode: BackupTransportMode,
        backupSerial: String,
        restoreSerial: String,
    ) {
        synchronized(lock) {
            ownTransportMode = mode
            ownBackupSerial = backupSerial
            ownRestoreSerial = restoreSerial
        }
    }

    fun markBackupStarted(serial: String, mode: BackupTransportMode) {
        synchronized(lock) {
            ownTransportMode = mode
            ownBackupSerial = serial
            ownHasSeenBackup = true
        }
    }

    fun markRestoreStarted(serial: String, detectedMode: BackupTransportMode? = null) {
        synchronized(lock) {
            if (ownTransportMode == null && detectedMode != null) {
                ownTransportMode = detectedMode
            }
            ownRestoreSerial = serial
            ownHasSeenRestore = true
        }
    }

    fun recordStorages(storages: List<StorageDomain>) {
        synchronized(lock) { ownStorages.addAll(storages) }
    }

    fun recordActionArgs(args: Map<String, String>) {
        val coord = BackupRulesChecker.targetCoordinateFromArgs(args) ?: return
        synchronized(lock) {
            if (coord !in ownActionTargets) {
                ownActionTargets += coord
            }
        }
    }

    fun recordArchive(
        fileName: String,
        summaryValues: Map<String, String>,
        rawSourceFile: Path?,
        includeRawCopy: Boolean,
    ) {
        var snapshot: Path? = null
        var rawSize = 0L
        if (includeRawCopy && rawSourceFile != null && Files.isRegularFile(rawSourceFile)) {
            try {
                rawSize = Files.size(rawSourceFile)
                val created = Files.createTempFile("backup_raw_archive_", "_$fileName")
                snapshot = created
                Files.copy(rawSourceFile, created, StandardCopyOption.REPLACE_EXISTING)
            } catch (e: Exception) {
                snapshot?.let {
                    try {
                        Files.deleteIfExists(it)
                    } catch (_: Exception) {}
                }
                snapshot = null
                logger.warning("Failed to snapshot raw backup archive $fileName: ${e.message}")
            }
        }
        val savedCopy = snapshot
        var deleteImmediately = false
        synchronized(lock) {
            if (isClosed) {
                deleteImmediately = true
            } else {
                if (savedCopy != null) {
                    tempFilesToDelete.add(savedCopy)
                }
                ownArchiveSnapshot = ArchiveSnapshot(fileName, summaryValues, savedCopy, rawSize)
            }
        }
        if (deleteImmediately && savedCopy != null) {
            try {
                Files.deleteIfExists(savedCopy)
            } catch (_: Exception) {}
        }
    }

    /** Runs [block] inside a composite step and records its timed pass or fail step. */
    suspend fun <T> runStep(
        isNumbered: Boolean,
        kind: StepKind,
        name: String,
        controller: BackupRestoreController?,
        call: String,
        durationMillisOverride: (() -> Long)? = null,
        actionFailureExtractor: ((T) -> String?)? = null,
        onStepFailed: (() -> Unit)? = null,
        block: suspend () -> T,
    ): T {
        val startNanos = if (durationMillisOverride == null) System.nanoTime() else 0L
        return withContext(coroutineContext(insideCompositeStep = true)) {
            try {
                val result = block()
                val durationMs =
                    durationMillisOverride?.invoke()
                        ?: Duration.ofNanos(System.nanoTime() - startNanos).toMillis()
                addStep(
                    isNumbered = isNumbered,
                    kind = kind,
                    name = name,
                    controller = controller,
                    call = call,
                    durationMs = durationMs,
                    status = TestDetailRecord.STATUS_PASSED,
                    returnedActionFailureMessage = actionFailureExtractor?.invoke(result),
                )
                result
            } catch (t: Throwable) {
                val durationMs =
                    durationMillisOverride?.invoke()
                        ?: Duration.ofNanos(System.nanoTime() - startNanos).toMillis()
                addStep(
                    isNumbered = isNumbered,
                    kind = kind,
                    name = name,
                    controller = controller,
                    call = call,
                    durationMs = durationMs,
                    status = TestDetailRecord.STATUS_FAILED,
                    thrownFailure = t,
                )
                onStepFailed?.invoke()
                throw t
            }
        }
    }

    fun addStep(
        isNumbered: Boolean,
        kind: StepKind = StepKind.OTHER,
        name: String,
        controller: BackupRestoreController?,
        call: String,
        durationMs: Long,
        status: String,
        returnedActionFailureMessage: String? = null,
        thrownFailure: Throwable? = null,
    ) {
        val role = (controller as? BackupRestoreControllerImpl)?.explicitRole.orEmpty()
        synchronized(lock) {
            if (controller is BackupRestoreControllerImpl) {
                mergeController(ownControllers, controller)
            }
            ownSteps +=
                StepDraft(
                    isNumbered = isNumbered,
                    kind = kind,
                    name = name,
                    controllerSerial = controller?.serialNumber,
                    controllerRole = role,
                    shared = isShared,
                    call = call,
                    durationMs = durationMs.coerceAtLeast(0L),
                    status = status,
                    returnedActionFailureMessage = returnedActionFailureMessage,
                    thrownFailure = thrownFailure,
                )
        }
    }

    /** Derives the step phase name for a step-by-step controller call from current progress. */
    fun deriveStepPhase(isClearLike: Boolean): String =
        when {
            !hasSeenBackup -> "Setup"
            !hasSeenRestore -> if (isClearLike) "Clear" else "Mutate"
            else -> "Verify"
        }

    override fun close() {
        val unfinalizedHook =
            synchronized(lock) {
                if (isClosed) return
                onUnfinalizedClose.also { onUnfinalizedClose = null }
            }
        try {
            if (!finalized && unfinalizedHook != null) {
                unfinalizedHook.invoke()
            }
        } finally {
            var shouldCleanup = false
            val files =
                synchronized(lock) {
                    if (isClosed) {
                        emptyList()
                    } else {
                        isClosed = true
                        shouldCleanup = true
                        ArrayList(tempFilesToDelete).also { tempFilesToDelete.clear() }
                    }
                }
            if (shouldCleanup) {
                ActiveCaptureContext.unregister(this)
                HostLogManager.unregister(hostLogCollector)
                for (file in files) {
                    try {
                        Files.deleteIfExists(file)
                    } catch (_: Exception) {}
                }
            }
        }
    }

    private fun collectMergedControllers(): List<BackupRestoreControllerImpl> {
        val inherited = parent?.collectMergedControllers().orEmpty()
        val ownCopy = synchronized(lock) { ownControllers.toList() }
        val combined = ArrayList<BackupRestoreControllerImpl>(inherited.size + ownCopy.size)
        for (controller in inherited + ownCopy) {
            mergeController(combined, controller)
        }
        return combined
    }

    companion object {
        private val INTERNAL_STACK_PREFIXES =
            listOf(
                "java.",
                "javax.",
                "jdk.",
                "sun.",
                "kotlin.",
                "kotlinx.",
                "org.junit.",
                "org.opentest4j.",
                "org.mockito.",
                "com.android.",
                "com.google.common.",
                "androidx.test.backup.host.",
            )

        internal fun unwrapAsyncFailure(failure: Throwable): Throwable {
            var current = failure
            val seen = HashSet<Throwable>()
            while (
                (current is ExecutionException || current is CompletionException) &&
                    current.cause != null &&
                    seen.add(current)
            ) {
                current = current.cause!!
            }
            return current
        }

        private fun mergeController(
            list: MutableList<BackupRestoreControllerImpl>,
            candidate: BackupRestoreControllerImpl,
        ) {
            val candidateRole = candidate.explicitRole
            val idx = list.indexOfFirst { existing ->
                existing.serialNumber == candidate.serialNumber &&
                    (existing.explicitRole == candidateRole ||
                        existing.explicitRole.isEmpty() ||
                        candidateRole.isEmpty())
            }
            if (idx >= 0) {
                val prev = list[idx]
                val winner =
                    if (candidateRole.isEmpty() && prev.explicitRole.isNotEmpty()) {
                        prev
                    } else {
                        candidate
                    }
                if (winner.cachedDeviceFacts == null) {
                    winner.cachedDeviceFacts = prev.cachedDeviceFacts ?: candidate.cachedDeviceFacts
                }
                list[idx] = winner
            } else {
                list += candidate
            }
        }

        private fun findRelevantStackFrame(
            frames: Array<StackTraceElement>,
            testClassName: String?,
        ): StackTraceElement? {
            if (testClassName != null) {
                findFrameInTestClass(frames, testClassName)?.let {
                    return it
                }
            }
            return frames.firstOrNull { frame ->
                INTERNAL_STACK_PREFIXES.none { frame.className.startsWith(it) }
            }
        }

        private fun findFrameInTestClass(
            frames: Array<StackTraceElement>,
            testClassName: String,
        ): StackTraceElement? {
            val outerRoot = testClassName.substringBefore('$')
            return frames.firstOrNull {
                it.className == testClassName || it.className.startsWith("$testClassName$")
            }
                ?: frames.firstOrNull {
                    it.className == outerRoot || it.className.startsWith("$outerRoot$")
                }
        }
    }
}
