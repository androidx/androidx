/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.compose.testing

import android.view.Display
import androidx.activity.ComponentActivity
import androidx.annotation.NonNull
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.xr.compose.platform.SceneManager
import androidx.xr.compose.platform.setSubspaceContent
import androidx.xr.compose.subspace.SubspaceComposable
import androidx.xr.scenecore.JxrPlatformAdapter
import androidx.xr.scenecore.Session
import androidx.xr.scenecore.impl.JxrPlatformAdapterAxr
import androidx.xr.scenecore.impl.perception.PerceptionLibrary
import androidx.xr.scenecore.testing.FakeImpressApi
import androidx.xr.scenecore.testing.FakeScheduledExecutorService
import androidx.xr.scenecore.testing.FakeXrExtensions
import com.google.androidxr.splitengine.SplitEngineSubspaceManager
import com.google.ar.imp.view.splitengine.ImpSplitEngineRenderer
import org.mockito.Mockito.mock
import org.robolectric.shadows.ShadowDisplay

/** Custom test class that should be used for testing [SubspaceComposable] content. */
@Suppress("ForbiddenSuperClass")
public class SubspaceTestingActivity : ComponentActivity() {
    public val extensions: FakeXrExtensions = FakeXrExtensions()
    public val session: Session by lazy { createFakeSession(this) }

    /** Throws an exception by default under test; return Robolectric Display impl instead. */
    @NonNull override fun getDisplay(): Display = ShadowDisplay.getDefaultDisplay()

    override fun onStart() {
        SceneManager.start()
        super.onStart()
    }

    override fun onDestroy() {
        SceneManager.stop()
        super.onDestroy()
    }
}

/** Analog to [AndroidComposeTestRule.setContent] for testing [SubspaceComposable] content. */
public fun AndroidComposeTestRule<*, SubspaceTestingActivity>.setSubspaceContent(
    content: @Composable @SubspaceComposable () -> Unit
) {
    setContent {} // Necessary to avoid crashes, as ComposeTestRule expects a call to setContent {}
    activity.setSubspaceContent(session = activity.session, content = content)
}

/** Analog to [AndroidComposeTestRule.setContent] for testing [SubspaceComposable] content. */
public fun AndroidComposeTestRule<*, SubspaceTestingActivity>.setSubspaceContent(
    uiContent: @Composable () -> Unit,
    content: @Composable @SubspaceComposable () -> Unit,
) {
    setContent(uiContent)
    activity.setSubspaceContent(session = activity.session, content = content)
}

/** Subspace version of onNode in Compose. */
public fun AndroidComposeTestRule<*, SubspaceTestingActivity>.onSubspaceNode(
    matcher: SubspaceSemanticsMatcher
): SubspaceSemanticsNodeInteraction =
    SubspaceSemanticsNodeInteraction(SubspaceTestContext(this), matcher)

/** Subspace version of onAllNodes in Compose. */
public fun AndroidComposeTestRule<*, SubspaceTestingActivity>.onAllSubspaceNodes(
    matcher: SubspaceSemanticsMatcher
): SubspaceSemanticsNodeInteractionCollection =
    SubspaceSemanticsNodeInteractionCollection(SubspaceTestContext(this), matcher)

/** Subspace version of onNodeWithTag in Compose. */
public fun AndroidComposeTestRule<*, SubspaceTestingActivity>.onSubspaceNodeWithTag(
    testTag: String
): SubspaceSemanticsNodeInteraction = onSubspaceNode(hasTestTag(testTag))

/** Subspace version of onAllNodesWithTag in Compose. */
public fun AndroidComposeTestRule<*, SubspaceTestingActivity>.onAllSubspaceNodesWithTag(
    testTag: String
): SubspaceSemanticsNodeInteractionCollection = onAllSubspaceNodes(hasTestTag(testTag))

/**
 * Create a fake [Session] for testing.
 *
 * TODO(b/370856223) Update documentation to include params
 */
public fun createFakeSession(
    activity: SubspaceTestingActivity,
    runtime: JxrPlatformAdapter = createFakeRuntime(activity),
): Session = Session.create(activity, runtime)

/**
 * Create a fake [JxrPlatformAdapter] for testing.
 *
 * TODO(b/370856223) Update documentation to include params
 */
public fun createFakeRuntime(activity: SubspaceTestingActivity): JxrPlatformAdapter =
    JxrPlatformAdapterAxr.create(
        /* activity = */ activity,
        /* executor = */ FakeScheduledExecutorService(),
        /* extensions = */ activity.extensions,
        /* impressApi = */ FakeImpressApi(),
        /* perceptionLibrary = */ PerceptionLibrary(),
        /* splitEngineSubspaceManager = */ mock(SplitEngineSubspaceManager::class.java),
        /* splitEngineRenderer = */ mock(ImpSplitEngineRenderer::class.java),
    )
