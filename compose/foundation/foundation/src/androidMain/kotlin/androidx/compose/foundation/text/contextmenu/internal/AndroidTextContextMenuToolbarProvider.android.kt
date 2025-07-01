/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.foundation.text.contextmenu.internal

import android.app.RemoteAction
import android.content.Context
import android.graphics.Rect as AndroidRect
import android.os.Build
import android.os.Looper
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.textclassifier.TextClassification
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.internal.checkPreconditionNotNull
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuData
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuItem
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuSeparator
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuSession
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuTextClassificationItem
import androidx.compose.foundation.text.contextmenu.internal.TextToolbarHelperApi28.addMenuItem
import androidx.compose.foundation.text.contextmenu.provider.LocalTextContextMenuToolbarProvider
import androidx.compose.foundation.text.contextmenu.provider.TextContextMenuDataProvider
import androidx.compose.foundation.text.contextmenu.provider.TextContextMenuProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateObserver
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastRoundToInt
import kotlinx.coroutines.channels.Channel

// TODO(grantapher) Consider making public.
@Composable
internal fun ProvidePlatformTextContextMenuToolbar(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    ProvidePlatformTextContextMenuToolbar(modifier, null, content)
}

/**
 * Sets the [LocalTextContextMenuToolbarProvider] to a provider that uses the platform text toolbar.
 *
 * @param callbackInjector A function that allows installing an interception layer to the
 *   [TextActionModeCallback] used. Useful for testing.
 * @param content The content that will have the provider installed.
 */
@VisibleForTesting
@Composable
internal fun ProvidePlatformTextContextMenuToolbar(
    modifier: Modifier = Modifier,
    callbackInjector: ((TextActionModeCallback) -> TextActionModeCallback)?,
    content: @Composable () -> Unit,
) {
    var layoutCoordinates by remember {
        // onGloballyPositioned may fire with the same LayoutCoordinates containing different
        // positioning data, so always trigger read observation when this is set.
        mutableStateOf<LayoutCoordinates?>(null, policy = neverEqualPolicy())
    }

    val provider =
        platformTextContextMenuToolbarProvider(
            coordinatesProvider = { checkPreconditionNotNull(layoutCoordinates) },
            callbackInjector = callbackInjector,
        )

    CompositionLocalProvider(LocalTextContextMenuToolbarProvider provides provider) {
        Box(
            propagateMinConstraints = true,
            modifier = modifier.onGloballyPositioned { layoutCoordinates = it },
        ) {
            content()
        }
    }
}

@Composable
internal fun platformTextContextMenuToolbarProvider(
    coordinatesProvider: () -> LayoutCoordinates,
    callbackInjector: ((TextActionModeCallback) -> TextActionModeCallback)? = null,
): TextContextMenuProvider {
    val view = LocalView.current
    val provider =
        remember(view) {
            AndroidTextContextMenuToolbarProvider(view, callbackInjector, coordinatesProvider)
        }

    DisposableEffect(provider) {
        provider.start()
        onDispose { provider.dispose() }
    }

    return provider
}

@VisibleForTesting
internal class AndroidTextContextMenuToolbarProvider(
    private val view: View,
    private val callbackInjector: ((TextActionModeCallback) -> TextActionModeCallback)?,
    private val coordinatesProvider: () -> LayoutCoordinates,
) : TextContextMenuProvider {
    private val mutatorMutex = MutatorMutex()
    private val snapshotStateObserver =
        SnapshotStateObserver(
            onChangedExecutor = { command ->
                // This is the same executor logic used by AndroidComposeView's
                // OwnerSnapshotObserver, which drives most of the state observation in compose UI.
                if (view.handler?.looper === Looper.myLooper()) {
                    command()
                } else {
                    view.handler?.post(command)
                }
            }
        )

    private val onDataChange: (Any) -> Unit = { actionMode?.invalidate() }
    private val onPositionChange: (Any) -> Unit = {
        actionMode?.let(TextToolbarHelper::invalidateContentRect)
    }

    private var actionMode: ActionMode? = null

    private var startActionModeRunnable: Runnable? = null

    override suspend fun showTextContextMenu(dataProvider: TextContextMenuDataProvider) {
        mutatorMutex.mutate {
            val session = TextContextMenuSessionImpl()
            val callback = createActionModeCallback(session, dataProvider)

            if (Looper.myLooper() !== view.handler?.looper) {
                val startActionModeRunnable =
                    this.startActionModeRunnable
                        ?: Runnable {
                                val actionMode =
                                    TextToolbarHelper.startActionMode(view, callback).also {
                                        this.actionMode == it
                                    }
                                // Failed to start action mode, close session by us.
                                if (actionMode == null) {
                                    session.close()
                                }
                            }
                            .also { this.startActionModeRunnable = it }
                view.post(startActionModeRunnable)
            } else {
                actionMode = TextToolbarHelper.startActionMode(view, callback) ?: return@mutate
            }

            try {
                session.awaitClose()
            } finally {
                snapshotStateObserver.clear()
                actionMode?.finish()
                startActionModeRunnable?.let { view.removeCallbacks(it) }
                actionMode = null
            }
        }
    }

    fun start() {
        snapshotStateObserver.start()
    }

    fun dispose() {
        snapshotStateObserver.stop()
        snapshotStateObserver.clear()
        actionMode?.finish()
        actionMode = null
    }

    private fun createActionModeCallback(
        session: TextContextMenuSessionImpl,
        dataProvider: TextContextMenuDataProvider,
    ): TextActionModeCallback {
        val textCallback =
            TextActionModeCallbackImpl(
                session = session,
                dataBuilder = { observeAndGetData(dataProvider) },
                positioner = { observeAndGetBounds(dataProvider) },
                view = view,
            )
        return callbackInjector?.invoke(textCallback) ?: textCallback
    }

    private fun observeAndGetData(dataProvider: TextContextMenuDataProvider): TextContextMenuData =
        observeReadsAndGet("dataBuilder", onDataChange) { dataProvider.data() }

    private fun observeAndGetBounds(dataProvider: TextContextMenuDataProvider): Rect =
        observeReadsAndGet("positioner", onPositionChange) { calculateBoundsInRoot(dataProvider) }

    private fun calculateBoundsInRoot(dataProvider: TextContextMenuDataProvider): Rect {
        val destinationCoordinates = coordinatesProvider()
        val localBoundingBox = dataProvider.contentBounds(destinationCoordinates)
        return localBoundingBox.translate(destinationCoordinates.positionInRoot())
    }

    /**
     * Same functionality as [SnapshotStateObserver.observeReads] except this function returns the
     * value returned in [block].
     */
    private fun <T : Any, S : Any> observeReadsAndGet(
        scope: S,
        onValueChanged: (S) -> Unit,
        block: () -> T,
    ): T {
        lateinit var result: T
        snapshotStateObserver.observeReads(scope, onValueChanged) { result = block() }
        return result
    }

    private class TextActionModeCallbackImpl(
        private val session: TextContextMenuSession,
        private val dataBuilder: () -> TextContextMenuData,
        private var positioner: () -> Rect,
        private val view: View,
    ) : TextActionModeCallback {
        private var previousData: TextContextMenuData? = null

        override fun onGetContentRect(mode: ActionMode, view: View?): Rect = positioner()

        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            updateMenuItems(menu)
            return menu.size() > 0
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean =
            updateMenuItems(menu)

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean = false

        override fun onDestroyActionMode(mode: ActionMode) {
            session.close()
        }

        /** @return whether the menu has changed */
        private fun updateMenuItems(menu: Menu): Boolean {
            val data = dataBuilder()
            if (data == previousData) return false

            menu.clear()

            var currentGroupId = 1
            var currentOrderId = 1
            data.components.fastForEach { component ->
                when (component) {
                    is TextContextMenuItem -> {
                        val orderId = currentOrderId++
                        val menuItem =
                            menu.add(
                                /* groupId = */ currentGroupId,
                                // itemId must be unique so that onClick listeners
                                // can be called on the item itself.
                                /* itemId = */ orderId,
                                /* order = */ orderId,
                                /* title = */ component.label,
                            )
                        menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                        menuItem.setOnMenuItemClickListener {
                            with(component) { session.onClick() }
                            true
                        }
                    }
                    is TextContextMenuTextClassificationItem -> {
                        if (Build.VERSION.SDK_INT >= 28) {
                            val orderId = currentOrderId++
                            addMenuItem(
                                menu,
                                orderId,
                                view.context,
                                component.textClassification,
                                component.index,
                            )
                        }
                    }
                    is TextContextMenuSeparator -> currentGroupId++
                }
            }

            return true
        }
    }

    private class TextContextMenuSessionImpl : TextContextMenuSession {
        private val channel = Channel<Unit>()

        override fun close() {
            channel.trySend(Unit)
        }

        suspend fun awaitClose() {
            channel.receive()
        }
    }
}

private object TextToolbarHelper {
    fun startActionMode(view: View, textActionModeCallback: TextActionModeCallback): ActionMode? =
        if (Build.VERSION.SDK_INT >= 23) {
            TextToolbarHelperApi23.startActionMode(
                view = view,
                actionModeCallback = FloatingTextActionModeCallback(textActionModeCallback),
                type = ActionMode.TYPE_FLOATING,
            )
        } else {
            view.startActionMode(PrimaryTextActionModeCallback(textActionModeCallback))
        }

    fun invalidateContentRect(actionMode: ActionMode) {
        if (Build.VERSION.SDK_INT >= 23) TextToolbarHelperApi23.invalidateContentRect(actionMode)
    }
}

@RequiresApi(23)
private object TextToolbarHelperApi23 {
    @RequiresApi(23)
    fun startActionMode(
        view: View,
        actionModeCallback: ActionMode.Callback,
        type: Int,
    ): ActionMode? = view.startActionMode(actionModeCallback, type)

    @RequiresApi(23)
    fun invalidateContentRect(actionMode: ActionMode) {
        actionMode.invalidateContentRect()
    }
}

/**
 * Interface mirroring [ActionMode.Callback2] for API compatibility as well as testing. The only
 * function that doesn't exactly mirror it is [onGetContentRect] which is modified to return a
 * compose [Rect] instead.
 */
@VisibleForTesting
internal interface TextActionModeCallback {
    /** @return where to position the action mode around relative to the root. */
    fun onGetContentRect(mode: ActionMode, view: View?): Rect

    /** @return whether the menu should be created. */
    fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean

    /** @return whether the menu has changed and should be refreshed. */
    fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean

    /** @return whether the click has been handled. */
    fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean

    /** Called when the action mode is about to be destroyed. */
    fun onDestroyActionMode(mode: ActionMode)
}

@RequiresApi(23)
private class FloatingTextActionModeCallback(
    private val textActionModeCallback: TextActionModeCallback
) : ActionMode.Callback2(), ActionMode.Callback {
    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean =
        textActionModeCallback.onCreateActionMode(mode, menu)

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean =
        textActionModeCallback.onPrepareActionMode(mode, menu)

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean =
        textActionModeCallback.onActionItemClicked(mode, item)

    override fun onDestroyActionMode(mode: ActionMode) =
        textActionModeCallback.onDestroyActionMode(mode)

    override fun onGetContentRect(mode: ActionMode, view: View?, outRect: AndroidRect) {
        val contentRect = textActionModeCallback.onGetContentRect(mode, view)
        outRect.set(
            contentRect.left.fastRoundToInt(),
            contentRect.top.fastRoundToInt(),
            contentRect.right.fastRoundToInt(),
            contentRect.bottom.fastRoundToInt(),
        )
    }
}

@RequiresApi(28)
private object TextToolbarHelperApi28 {
    fun addMenuItem(
        menu: Menu,
        orderId: Int,
        context: Context,
        textClassification: TextClassification,
        index: Int,
    ) {
        if (index < 0) {
            addLegacyMenuItem(menu, orderId, context, textClassification)
        } else {
            val isPrimary = (index == 0)
            addMenuItem(menu, orderId, context, isPrimary, textClassification.actions[index])
        }
    }

    fun addMenuItem(
        menu: Menu,
        orderId: Int,
        context: Context,
        isPrimary: Boolean,
        remoteAction: RemoteAction,
    ) {
        val item =
            menu.add(
                android.R.id.textAssist,
                if (isPrimary) android.R.id.textAssist else Menu.NONE,
                orderId,
                remoteAction.title,
            )

        item.setShowAsAction(
            if (isPrimary) MenuItem.SHOW_AS_ACTION_ALWAYS else MenuItem.SHOW_AS_ACTION_NEVER
        )

        if (isPrimary || remoteAction.shouldShowIcon()) {
            item.icon = remoteAction.icon.loadDrawable(context)
        }

        item.setOnMenuItemClickListener {
            TextClassificationHelperApi28.sendPendingIntent(remoteAction.actionIntent)
            true
        }
    }

    @Suppress("DEPRECATION")
    fun addLegacyMenuItem(
        menu: Menu,
        orderId: Int,
        context: Context,
        textClassification: TextClassification,
    ) {
        val item =
            menu.add(
                android.R.id.textAssist,
                android.R.id.textAssist,
                orderId,
                textClassification.label,
            )

        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        item.icon = textClassification.icon

        item.setOnMenuItemClickListener {
            TextClassificationHelperApi28.sendLegacyIntent(context, textClassification)
            true
        }
    }
}

private class PrimaryTextActionModeCallback(
    private val textActionModeCallback: TextActionModeCallback
) : ActionMode.Callback {
    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean =
        textActionModeCallback.onCreateActionMode(mode, menu)

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean =
        textActionModeCallback.onPrepareActionMode(mode, menu)

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean =
        textActionModeCallback.onActionItemClicked(mode, item)

    override fun onDestroyActionMode(mode: ActionMode) {
        textActionModeCallback.onDestroyActionMode(mode)
    }
}
