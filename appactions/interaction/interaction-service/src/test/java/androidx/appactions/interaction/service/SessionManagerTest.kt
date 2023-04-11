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

package androidx.appactions.interaction.service

import androidx.appactions.interaction.capabilities.core.impl.CapabilitySession
import androidx.appactions.interaction.capabilities.core.impl.ArgumentsWrapper
import androidx.appactions.interaction.capabilities.core.impl.CallbackInternal
import androidx.appactions.interaction.capabilities.core.impl.TouchEventCallback
import androidx.appactions.interaction.proto.AppActionsContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SessionManagerTest {

    private val fakeSessionId = "12345"
    private val session1 =
        object : CapabilitySession {
            override val sessionId: String = fakeSessionId

            override val uiHandle: Any
                get() = this

            override fun execute(argumentsWrapper: ArgumentsWrapper, callback: CallbackInternal) {}

            override fun setTouchEventCallback(callback: TouchEventCallback) {}

            override val state: AppActionsContext.AppDialogState
                get() = AppActionsContext.AppDialogState.getDefaultInstance()

            override val isActive: Boolean = true

            override fun destroy() {}
        }

    private val session2 =
        object : CapabilitySession {
            override val sessionId: String = fakeSessionId

            override val uiHandle: Any
                get() = this

            override fun execute(argumentsWrapper: ArgumentsWrapper, callback: CallbackInternal) {}

            override fun setTouchEventCallback(callback: TouchEventCallback) {}

            override val state: AppActionsContext.AppDialogState
                get() = AppActionsContext.AppDialogState.getDefaultInstance()

            override val isActive: Boolean = true

            override fun destroy() {}
        }

    @Test
    fun sessionManager_putGetRemoveLifecycle() {
        SessionManager.putSession(fakeSessionId, session1)
        assertThat(SessionManager.getSession(fakeSessionId)).isEqualTo(session1)

        SessionManager.removeSession(fakeSessionId)
        assertThat(SessionManager.getSession(fakeSessionId)).isNull()
    }

    @Test
    fun sessionManager_multipleOverwrites_returnsLatestWrite() {
        SessionManager.putSession(fakeSessionId, session1)
        SessionManager.putSession(fakeSessionId, session2)
        assertThat(SessionManager.getSession(fakeSessionId)).isEqualTo(session2)
    }

    @Test
    fun sessionManager_getSession_returnsNullWhenNotFound() {
        val session2 = fakeSessionId + "test"
        SessionManager.putSession(fakeSessionId, session1)

        assertThat(SessionManager.getSession(session2)).isNull()
    }
}
