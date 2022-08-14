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

package androidx.window.area

import android.app.Activity
import androidx.window.core.ExperimentalWindowApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.util.concurrent.Executor

/**
 * Empty Implementation for devices that do not
 * support the [WindowAreaController] functionality
 */
@ExperimentalWindowApi
internal class EmptyWindowAreaControllerImpl : WindowAreaController {
    override fun rearDisplayStatus(): Flow<WindowAreaStatus> {
        return flowOf(WindowAreaStatus.UNSUPPORTED)
    }

    override fun rearDisplayMode(
        activity: Activity,
        executor: Executor,
        windowAreaSessionCallback: WindowAreaSessionCallback
    ) {
        throw WindowAreaController.REAR_DISPLAY_ERROR
    }
}