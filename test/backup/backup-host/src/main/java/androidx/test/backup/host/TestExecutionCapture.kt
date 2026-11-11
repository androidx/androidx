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

import androidx.annotation.VisibleForTesting
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Duration
import java.time.Instant
import java.util.Locale
import java.util.Properties
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

    @VisibleForTesting
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
    @Volatile var testClassName: String?,
    @Volatile var testMethodName: String?,
    @Volatile var parent: TestExecutionCapture? = null,
    @Volatile var includeRawArchives: Boolean = false,
) : CloseableResource, AutoCloseable {

    internal enum class StepKind(val stage: BackupExecutionStage? = null) {
        PREPARE,
        SEED_ACTION(BackupExecutionStage.SEEDING),
        BACKUP(BackupExecutionStage.BACKUP),
        CLEAR_DATA(BackupExecutionStage.CLEAR_DATA),
        RESTORE(BackupExecutionStage.RESTORE),
        VERIFY_ACTION(BackupExecutionStage.VERIFICATION),
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

    internal data class SavedLogcatFile(
        val path: Path,
        val truncated: Boolean = false,
        val attached: Boolean = true,
    )

    internal data class ArchiveSnapshot(
        val fileName: String,
        val summaryValues: Map<String, String>,
        val rawTempCopy: Path?,
        val rawSizeBytes: Long,
    )

    private val lock = Any()
    private val publishLock = Any()
    private val logger = Logger.getLogger(TestExecutionCapture::class.java.name)

    val hostLogCollector: HostLogCollector = HostLogCollector()

    private val ownControllers = ArrayList<BackupRestoreControllerImpl>()
    private val ownSteps = ArrayList<StepDraft>()
    private val ownStorages = ArrayList<StorageDomain>()
    private val ownActionTargets = ArrayList<BackupRulesChecker.TargetCoordinate>()
    private val tempFilesToDelete = ArrayList<Path>()
    private val publishedReportEntries = LinkedHashMap<String, String>()

    private var ownTransportMode: BackupTransportMode? = null
    private var ownBackupSerial: String? = null
    private var ownRestoreSerial: String? = null
    private var ownHasSeenBackup: Boolean = false
    private var ownHasSeenRestore: Boolean = false
    private var ownArchiveSnapshot: ArchiveSnapshot? = null
    private var ownFirstCallFrame: StackTraceElement? = null
    private var sealedSharedLogSnapshot: Pair<String, Boolean>? = null
    @Volatile private var liveEntryPublisher: ((Map<String, String>) -> Unit)? = null

    @Volatile
    var isClosed: Boolean = false
        private set

    @Volatile var finalized: Boolean = false
    @Volatile var onUnfinalizedClose: (() -> Unit)? = null

    @Volatile var publishedFlowTelemetry: Boolean = false
    @Volatile var preconditionFailed: Boolean = false

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
        return joinLines(parentText, ownText) to (parentTrunc || ownTrunc)
    }

    val hasSeenBackup: Boolean
        get() = synchronized(lock) { ownHasSeenBackup } || (parent?.hasSeenBackup == true)

    val hasSeenRestore: Boolean
        get() = synchronized(lock) { ownHasSeenRestore } || (parent?.hasSeenRestore == true)

    val hasOwnTransportMode: Boolean
        get() = synchronized(lock) { ownTransportMode != null }

    val effectiveTransportMode: BackupTransportMode?
        get() = synchronized(lock) { ownTransportMode } ?: parent?.effectiveTransportMode

    val effectiveIncludeRawArchives: Boolean
        get() = includeRawArchives || (parent?.effectiveIncludeRawArchives == true)

    private val effectiveBackupSerial: String?
        get() = synchronized(lock) { ownBackupSerial } ?: parent?.effectiveBackupSerial

    private val effectiveRestoreSerial: String?
        get() = synchronized(lock) { ownRestoreSerial } ?: parent?.effectiveRestoreSerial

    private val effectiveArchiveSnapshot: ArchiveSnapshot?
        get() = synchronized(lock) { ownArchiveSnapshot } ?: parent?.effectiveArchiveSnapshot

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
        }
    }

    fun markRestoreCompleted() {
        synchronized(lock) { ownHasSeenRestore = true }
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
        if (isNumbered && !isShared && !isClosed) {
            publishLiveSnapshotIfNeeded()
        }
    }

    /**
     * Attaches [publisher] for streaming completed test steps as JUnit report entries while the
     * test runs.
     */
    fun attachLiveEntryPublisher(publisher: (Map<String, String>) -> Unit) {
        synchronized(publishLock) { liveEntryPublisher = publisher }
        if (!isShared && !isClosed && synchronized(lock) { ownSteps.any { it.isNumbered } }) {
            publishLiveSnapshotIfNeeded()
        }
    }

    /**
     * Publishes any `androidx.report.*` entries in [finalEntries] not yet emitted or changed since
     * live step streaming.
     */
    fun publishFinalEntries(
        finalEntries: Map<String, String>,
        publisher: (Map<String, String>) -> Unit,
    ) {
        synchronized(publishLock) {
            val delta = LinkedHashMap<String, String>()
            for ((k, v) in finalEntries) {
                if (publishedReportEntries[k] != v) {
                    delta[k] = v
                }
            }
            if (delta.isNotEmpty()) {
                publisher(delta)
                publishedReportEntries.putAll(delta)
            }
        }
    }

    private fun publishLiveSnapshotIfNeeded() {
        synchronized(publishLock) {
            val publisher = liveEntryPublisher ?: return
            if (isShared || isClosed || finalized) return
            val entries = buildLiveSnapshotEntries()
            val delta = LinkedHashMap<String, String>()
            for ((k, v) in entries) {
                if (publishedReportEntries[k] != v) {
                    delta[k] = v
                }
            }
            if (delta.isEmpty()) return
            try {
                publisher(delta)
                publishedReportEntries.putAll(delta)
            } catch (_: Throwable) {
                // Live progress publishing must never mask test execution.
            }
        }
    }

    private class StepSnapshot(
        val actorsWithLookup: ResolvedActors,
        val rawSteps: List<StepDraft>,
        val ownStartIndex: Int,
        val numberedSteps: MutableList<TestDetailRecord.StepRecord>,
        val nextStepNumber: Int,
    )

    private fun buildStepSnapshot(): StepSnapshot {
        val actorsWithLookup =
            resolveActors(collectMergedControllers(), effectiveBackupSerial, effectiveRestoreSerial)
        val (inheritedSteps, ownStepsCopy) = collectMergedStepsPartitioned()
        val rawSteps = inheritedSteps + ownStepsCopy
        val numberedSteps = ArrayList<TestDetailRecord.StepRecord>(rawSteps.size)
        var counter = 0
        for (draft in rawSteps) {
            val stepNum = if (draft.isNumbered) ++counter else null
            val actorId = actorsWithLookup.actorIdFor(draft.controllerSerial, draft.controllerRole)
            numberedSteps +=
                TestDetailRecord.StepRecord(
                    n = stepNum,
                    name = draft.name,
                    on = actorId,
                    shared = draft.shared,
                    call = draft.call,
                    durationMs = draft.durationMs,
                    status = draft.status,
                )
        }
        return StepSnapshot(
            actorsWithLookup = actorsWithLookup,
            rawSteps = rawSteps,
            ownStartIndex = inheritedSteps.size,
            numberedSteps = numberedSteps,
            nextStepNumber = counter,
        )
    }

    private fun buildLiveSnapshotEntries(): Map<String, String> {
        val snapshot = buildStepSnapshot()
        return TestDetailRecord(
                schemaVersion = TestDetailRecord.SCHEMA_VERSION,
                suiteType = TestDetailRecord.SUITE_TYPE,
                sourceLocation = resolveSourceLocation(failure = null),
                steps = snapshot.numberedSteps,
            )
            .toReportEntries()
    }

    /** Derives the step phase name for a step-by-step controller call from current progress. */
    fun deriveStepPhase(isClearLike: Boolean): String =
        when {
            !hasSeenBackup -> "Setup"
            !hasSeenRestore -> if (isClearLike) "Clear" else "Mutate"
            else -> "Verify"
        }

    internal data class FinalizedReport(
        val record: TestDetailRecord,
        val failureNoticeText: String?,
    )

    /**
     * Builds the finalized [TestDetailRecord] and publishes `host-log.txt` and any requested raw
     * archive via [filePublisher] when provided.
     */
    fun buildFinalizedReport(
        savedLogcatFiles: Map<String, SavedLogcatFile>,
        failure: Throwable?,
        suiteProperties: Properties?,
        filePublisher: ((fileName: String, mediaType: String, writer: (Path) -> Unit) -> Path?)?,
    ): FinalizedReport {
        val snapshot = buildStepSnapshot()
        val actorsWithLookup = snapshot.actorsWithLookup
        val actors = actorsWithLookup.actors
        val numberedSteps = snapshot.numberedSteps
        val stepDraftRefs = ArrayList<StepDraft?>(snapshot.rawSteps)
        val restoreSerial = effectiveRestoreSerial

        val transport = effectiveTransportMode
        val storages = collectMergedStorages()
        val actionTargets = collectMergedActionTargets()

        var callout: TestDetailRecord.Callout? = null
        var failureNoticeText: String? = null

        if (failure != null) {
            val rootFailure = unwrapAsyncFailure(failure)
            val failureMessage =
                rootFailure.message?.takeIf { it.isNotBlank() } ?: rootFailure.javaClass.simpleName
            val isPostRestoreAssertion = hasSeenRestore && rootFailure is AssertionError

            val match =
                findAttributedFailedStep(
                    steps = snapshot.rawSteps,
                    startIndex = snapshot.ownStartIndex,
                    failure = failure,
                    rootFailure = rootFailure,
                    isPostRestoreAssertion = isPostRestoreAssertion,
                )
            val failedIndex =
                when {
                    match != null -> {
                        if (match.markReturnedActionFailed) {
                            numberedSteps[match.index] =
                                numberedSteps[match.index].copy(
                                    status = TestDetailRecord.STATUS_FAILED
                                )
                        }
                        match.index
                    }
                    isPostRestoreAssertion -> {
                        val nextNum = snapshot.nextStepNumber + 1
                        val targetActorId =
                            actorsWithLookup.actorIdFor(restoreSerial, "")
                                ?: actors.lastOrNull()?.id
                        val methodCall =
                            if (!testMethodName.isNullOrEmpty()) "Assertion in $testMethodName()"
                            else "Assertion"
                        numberedSteps +=
                            TestDetailRecord.StepRecord(
                                n = nextNum,
                                name = "Verify",
                                on = targetActorId,
                                shared = false,
                                call = methodCall,
                                durationMs = 0L,
                                status = TestDetailRecord.STATUS_FAILED,
                            )
                        stepDraftRefs += null
                        numberedSteps.lastIndex
                    }
                    else -> -1
                }

            val failedStepRecord = numberedSteps.getOrNull(failedIndex)
            val failedStepDraft = stepDraftRefs.getOrNull(failedIndex)
            val isBackupRuleEligible =
                isPostRestoreAssertion || failedStepDraft?.kind in BACKUP_RULE_ELIGIBLE_KINDS
            val isInfrastructureError =
                (rootFailure as? BackupRestoreException)?.errorCode in INFRASTRUCTURE_ERROR_CODES
            val likelyCause =
                if (isBackupRuleEligible && !isInfrastructureError) {
                    BackupRulesChecker.diagnoseLikelyCause(
                        suiteProperties = suiteProperties,
                        transportMode = transport,
                        storages = storages,
                        actionTargets = actionTargets,
                        failureMessage = failureMessage,
                    )
                } else {
                    null
                }

            val isAssertion =
                rootFailure is AssertionError || failedStepDraft?.kind == StepKind.VERIFY_ACTION
            val kind =
                if (isAssertion) TestDetailRecord.CALLOUT_ASSERTION_FAILURE
                else TestDetailRecord.CALLOUT_STEP_FAILURE
            val prefix = if (isAssertion) "Assertion failed" else "Step failed"
            val title =
                if (failedStepRecord != null) {
                    "$prefix at ${failedStepRecord.name}"
                } else {
                    prefix
                }

            callout =
                TestDetailRecord.Callout(
                    kind = kind,
                    failedStep = failedStepRecord?.n,
                    title = title,
                    message = failureMessage,
                    likelyCause = likelyCause,
                )

            if (failedStepRecord != null || likelyCause != null) {
                failureNoticeText = buildString {
                    if (failedStepRecord != null) {
                        append("Failed at ${failedStepRecord.name}")
                    }
                    if (likelyCause != null) {
                        if (isNotEmpty()) append(". ")
                        append("Likely cause: ")
                        append(likelyCause)
                    }
                }
            }
        }

        val logs = ArrayList<TestDetailRecord.LogRecord>()
        if (filePublisher != null) {
            val (sharedPrefix, sharedTruncated) = parent?.sharedHostLogSnapshot() ?: ("" to false)
            val encodedHostLog = hostLogCollector.toBoundedUtf8(sharedPrefix, sharedTruncated)
            val attachedHostLog =
                try {
                    filePublisher("host-log.txt", MEDIA_TYPE_TEXT_UTF8) { path ->
                        Files.write(path, encodedHostLog.bytes)
                    }
                } catch (e: Exception) {
                    logger.warning("Failed to attach host-log.txt: ${e.message}")
                    null
                }
            if (attachedHostLog != null) {
                logs +=
                    TestDetailRecord.LogRecord(
                        title = "Host log",
                        path = attachedHostLog.fileName.toString(),
                        truncated = encodedHostLog.truncated,
                    )
            }
            for ((serial, savedLogcat) in savedLogcatFiles) {
                if (!savedLogcat.attached) continue
                val actorId = actorsWithLookup.actorIdFor(serial, "")
                val logTitle = if (actorId != null) "Logcat ($actorId)" else "Logcat ($serial)"
                logs +=
                    TestDetailRecord.LogRecord(
                        title = logTitle,
                        path = savedLogcat.path.fileName.toString(),
                        truncated = savedLogcat.truncated,
                    )
            }
        }

        val artifacts = ArrayList<TestDetailRecord.ArtifactEntry>()
        val archive = effectiveArchiveSnapshot
        if (archive != null) {
            artifacts +=
                TestDetailRecord.ArtifactEntry(
                    name = "Backup file summary",
                    values = archive.summaryValues,
                    path = null,
                )
            val tempCopy = archive.rawTempCopy
            if (filePublisher != null && tempCopy != null && Files.isRegularFile(tempCopy)) {
                val attachedArchive =
                    try {
                        filePublisher(archive.fileName, MEDIA_TYPE_OCTET_STREAM) { path ->
                            Files.copy(tempCopy, path, StandardCopyOption.REPLACE_EXISTING)
                        }
                    } catch (e: Exception) {
                        logger.warning("Failed to attach ${archive.fileName}: ${e.message}")
                        null
                    }
                if (attachedArchive != null) {
                    artifacts +=
                        TestDetailRecord.ArtifactEntry(
                            name = archive.fileName,
                            values = mapOf("size" to formatByteSize(archive.rawSizeBytes)),
                            path = attachedArchive.fileName.toString(),
                        )
                }
            }
        }

        val sourceLocation = resolveSourceLocation(failure)
        val summaryAttributes = DeviceFactsReader.buildSummaryAttributes(transport, actors)

        val detailRecord =
            TestDetailRecord(
                schemaVersion = TestDetailRecord.SCHEMA_VERSION,
                suiteType = TestDetailRecord.SUITE_TYPE,
                sourceLocation = sourceLocation,
                summaryAttributes = summaryAttributes,
                callout = callout,
                actors = actors,
                steps = numberedSteps,
                logs = logs,
                artifacts = artifacts,
            )

        return FinalizedReport(detailRecord, failureNoticeText)
    }

    /**
     * Publishes `BackupRestore.*` report entries for step-by-step tests that did not invoke
     * [BackupRestoreController.runBackupRestoreFlow].
     */
    fun publishStepByStepTelemetryIfNeeded(
        failure: Throwable?,
        publish: (key: String, value: String) -> Unit,
    ) {
        if (publishedFlowTelemetry || preconditionFailed) return
        val ownStepsSnapshot = synchronized(lock) { ownSteps.toList() }
        val ownRanBackupOrRestore = ownStepsSnapshot.any {
            it.kind == StepKind.BACKUP || it.kind == StepKind.RESTORE
        }
        if (failure == null) {
            if (!ownRanBackupOrRestore) return
            val transport = effectiveTransportMode
            try {
                if (transport != null) {
                    publish(BackupReportKeys.TRANSPORT_MODE, transport.toString())
                }
                publish(BackupReportKeys.STATUS, BackupReportKeys.REPORT_STATUS_SUCCESS)
            } catch (_: Throwable) {
                // Telemetry publishing must never mask test outcomes.
            }
            return
        }

        val rootFailure = unwrapAsyncFailure(failure)
        val isPostRestoreAssertion = hasSeenRestore && rootFailure is AssertionError
        val hasOwnNumberedSteps = ownStepsSnapshot.any { it.isNumbered }
        if (!hasOwnNumberedSteps && !isPostRestoreAssertion) return
        val transport = effectiveTransportMode
        try {
            if (transport != null) {
                publish(BackupReportKeys.TRANSPORT_MODE, transport.toString())
            }
            publish(BackupReportKeys.STATUS, BackupReportKeys.REPORT_STATUS_FAILURE)
            val match =
                findAttributedFailedStep(
                    steps = ownStepsSnapshot,
                    startIndex = 0,
                    failure = failure,
                    rootFailure = rootFailure,
                    isPostRestoreAssertion = isPostRestoreAssertion,
                )
            val failedStage =
                match?.let { ownStepsSnapshot[it.index].kind.stage }
                    ?: BackupExecutionStage.VERIFICATION.takeIf { isPostRestoreAssertion }
            val errorCode =
                failedStage?.errorCodeFor(rootFailure)
                    ?: (rootFailure as? BackupRestoreException)?.errorCode
                    ?: BackupErrorCode.UNKNOWN_ERROR
            publish(BackupReportKeys.ERROR_CODE, errorCode.name)
            rootFailure.message
                ?.takeIf { it.isNotBlank() }
                ?.let { publish(BackupReportKeys.ERROR_MESSAGE, it) }
        } catch (_: Throwable) {
            // Telemetry publishing must never mask test outcomes.
        }
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

    private fun resolveSourceLocation(failure: Throwable?): TestDetailRecord.SourceLocation? {
        val clsName = testClassName ?: return null
        val frame =
            if (failure != null) {
                findFailureStackFrame(failure, clsName) ?: synchronized(lock) { ownFirstCallFrame }
            } else {
                synchronized(lock) { ownFirstCallFrame }
            }
        val fileName = frame?.fileName?.takeIf { it.isNotBlank() } ?: return null
        return TestDetailRecord.SourceLocation(
            file = fileName,
            line = frame.lineNumber.takeIf { it > 0 },
        )
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

    private fun collectMergedStepsPartitioned(): Pair<List<StepDraft>, List<StepDraft>> {
        val inherited =
            parent
                ?.collectMergedStepsPartitioned()
                ?.let { (anc, pOwn) -> (anc + pOwn).map { it.copy(shared = true) } }
                .orEmpty()
        val ownCopy = synchronized(lock) { ownSteps.map { it.copy() } }
        return inherited to ownCopy
    }

    private fun collectMergedStorages(): List<StorageDomain> =
        parent?.collectMergedStorages().orEmpty() + synchronized(lock) { ownStorages.toList() }

    private fun collectMergedActionTargets(): List<BackupRulesChecker.TargetCoordinate> =
        (parent?.collectMergedActionTargets().orEmpty() +
                synchronized(lock) { ownActionTargets.toList() })
            .distinct()

    private class ResolvedActors(
        val actors: List<TestDetailRecord.ActorRecord>,
        private val bySerialAndRole: Map<Pair<String, String>, String>,
        private val bySerial: Map<String, String>,
    ) {
        fun actorIdFor(serial: String?, explicitRole: String): String? {
            if (serial == null) return null
            if (explicitRole.isNotEmpty()) {
                bySerialAndRole[serial to explicitRole]?.let {
                    return it
                }
            }
            return bySerial[serial]
        }
    }

    private fun resolveActors(
        controllers: List<BackupRestoreControllerImpl>,
        backupSerial: String?,
        restoreSerial: String?,
    ): ResolvedActors {
        if (controllers.isEmpty()) {
            return ResolvedActors(emptyList(), emptyMap(), emptyMap())
        }

        // Order backup device before restore device when a cross-device flow ran on two devices.
        val ordered =
            if (
                controllers.size >= 2 &&
                    (backupSerial != null || restoreSerial != null) &&
                    backupSerial != restoreSerial
            ) {
                controllers.sortedBy { c ->
                    when (c.serialNumber) {
                        backupSerial -> 0
                        restoreSerial -> 1
                        else -> if (backupSerial == null) 0 else if (restoreSerial == null) 1 else 2
                    }
                }
            } else {
                controllers
            }

        val usedIds = HashSet<String>()
        val actors = ArrayList<TestDetailRecord.ActorRecord>(ordered.size)
        val bySerialAndRole = HashMap<Pair<String, String>, String>()
        val bySerial = LinkedHashMap<String, String>()

        for ((index, controller) in ordered.withIndex()) {
            val serial = controller.serialNumber
            val api = controller.apiLevel
            val explicitRole = controller.explicitRole
            val facts =
                controller.cachedDeviceFacts
                    ?: DeviceFacts(model = serial, api = api, image = "AOSP", playServices = null)

            val defaultId =
                when {
                    ordered.size == 1 -> "device"
                    index == 0 -> "source"
                    index == 1 -> "target"
                    else -> "device_${index + 1}"
                }
            val baseId = explicitRole.ifEmpty { defaultId }
            var uniqueId = baseId
            var suffix = 2
            while (!usedIds.add(uniqueId)) {
                uniqueId = "${baseId}_$suffix"
                suffix++
            }
            val roleUpper = explicitRole.ifEmpty { defaultId }.uppercase(Locale.ROOT)
            actors +=
                TestDetailRecord.ActorRecord(
                    id = uniqueId,
                    role = roleUpper,
                    name = facts.model,
                    api = facts.api,
                    serial = serial,
                    image = facts.image,
                    playServices = facts.playServices,
                )
            if (explicitRole.isNotEmpty()) {
                bySerialAndRole[serial to explicitRole] = uniqueId
            }
            bySerial.putIfAbsent(serial, uniqueId)
        }
        return ResolvedActors(actors, bySerialAndRole, bySerial)
    }

    companion object {
        internal const val MEDIA_TYPE_TEXT_UTF8 = "text/plain; charset=utf-8"
        internal const val MEDIA_TYPE_OCTET_STREAM = "application/octet-stream"

        private val BACKUP_RULE_ELIGIBLE_KINDS =
            setOf(StepKind.BACKUP, StepKind.RESTORE, StepKind.VERIFY_ACTION)

        private val INFRASTRUCTURE_ERROR_CODES =
            setOf(
                BackupErrorCode.NO_ONLINE_DEVICE,
                BackupErrorCode.NO_MATCHING_DEVICE,
                BackupErrorCode.UNSUPPORTED_API_LEVEL,
                BackupErrorCode.GMSCORE_OUTDATED_OR_MISSING,
                BackupErrorCode.BMGR_INIT_FAILED,
                BackupErrorCode.RESTORE_POLL_TIMEOUT,
            )

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

        private data class AttributedStepMatch(
            val index: Int,
            val markReturnedActionFailed: Boolean,
        )

        private fun findAttributedFailedStep(
            steps: List<StepDraft>,
            startIndex: Int,
            failure: Throwable,
            rootFailure: Throwable,
            isPostRestoreAssertion: Boolean,
        ): AttributedStepMatch? {
            val thrownIdx =
                (startIndex..steps.lastIndex).lastOrNull { idx ->
                    val draft = steps[idx]
                    draft.status == TestDetailRecord.STATUS_FAILED &&
                        draft.thrownFailure != null &&
                        isInFailureChain(failure, draft.thrownFailure)
                }
            if (thrownIdx != null) {
                return AttributedStepMatch(thrownIdx, markReturnedActionFailed = false)
            }
            val actionIdx =
                (startIndex..steps.lastIndex).lastOrNull { idx ->
                    val draft = steps[idx]
                    draft.status == TestDetailRecord.STATUS_PASSED &&
                        matchesReturnedActionFailure(draft, rootFailure)
                }
            if (actionIdx != null) {
                return AttributedStepMatch(actionIdx, markReturnedActionFailed = true)
            }
            if (isPostRestoreAssertion) return null
            val fallbackIdx =
                (startIndex..steps.lastIndex).lastOrNull { idx ->
                    steps[idx].status == TestDetailRecord.STATUS_FAILED
                }
            return fallbackIdx?.let { AttributedStepMatch(it, markReturnedActionFailed = false) }
        }

        private fun isInFailureChain(failure: Throwable, target: Throwable): Boolean {
            var current: Throwable? = failure
            val seen = HashSet<Throwable>()
            while (current != null && seen.add(current)) {
                if (current === target) return true
                if (target in current.suppressed) return true
                current = current.cause
            }
            return false
        }

        private fun matchesReturnedActionFailure(draft: StepDraft, failure: Throwable): Boolean {
            val actionMsg = draft.returnedActionFailureMessage ?: return false
            return failure is IOException &&
                (failure.message == actionMsg ||
                    failure.message?.startsWith("$actionMsg\n") == true)
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

        private fun findFailureStackFrame(
            failure: Throwable,
            testClassName: String,
        ): StackTraceElement? {
            var current: Throwable? = failure
            val seen = HashSet<Throwable>()
            while (current != null && seen.add(current)) {
                findFrameInTestClass(current.stackTrace, testClassName)?.let {
                    return it
                }
                current = current.cause
            }
            return null
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
