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

package androidx.compose.ui.autofill

import android.graphics.Rect
import android.os.Build
import android.view.View
import android.view.autofill.AutofillManager
import android.view.autofill.AutofillValue
import androidx.annotation.RequiresApi

/** Wrapper for the final AutofillManager class. This can be mocked in testing. */
@RequiresApi(Build.VERSION_CODES.O)
internal interface PlatformAutofillManager {
    fun notifyViewEntered(view: View, semanticsId: Int, bounds: Rect)

    fun notifyViewExited(view: View, semanticsId: Int)

    fun notifyValueChanged(view: View, semanticsId: Int, autofillValue: AutofillValue)

    fun notifyViewVisibilityChanged(view: View, semanticsId: Int, isVisible: Boolean)

    fun commit()

    fun cancel()

    fun requestAutofill(view: View, semanticsId: Int, bounds: Rect)
}

@RequiresApi(Build.VERSION_CODES.O)
internal class PlatformAutofillManagerImpl(val platformAndroidManager: AutofillManager) :
    PlatformAutofillManager {

    override fun notifyViewEntered(view: View, semanticsId: Int, bounds: Rect) {
        platformAndroidManager.notifyViewEntered(view, semanticsId, bounds)
    }

    override fun notifyViewExited(view: View, semanticsId: Int) {
        platformAndroidManager.notifyViewExited(view, semanticsId)
    }

    override fun notifyValueChanged(view: View, semanticsId: Int, autofillValue: AutofillValue) {
        platformAndroidManager.notifyValueChanged(view, semanticsId, autofillValue)
    }

    override fun notifyViewVisibilityChanged(view: View, semanticsId: Int, isVisible: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            AutofillApi27Helper.notifyViewVisibilityChanged(
                view,
                platformAndroidManager,
                semanticsId,
                isVisible,
            )
        }
    }

    override fun commit() {
        platformAndroidManager.commit()
    }

    override fun cancel() {
        platformAndroidManager.cancel()
    }

    override fun requestAutofill(view: View, semanticsId: Int, bounds: Rect) {
        platformAndroidManager.requestAutofill(view, semanticsId, bounds)
    }
}
