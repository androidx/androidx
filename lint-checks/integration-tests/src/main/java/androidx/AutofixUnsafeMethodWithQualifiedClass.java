/*
 * Copyright 2022 The Android Open Source Project
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

package androidx;

import android.view.SearchEvent;
import android.view.Window;

import androidx.annotation.RequiresApi;

/**
 * Test class containing unsafe method reference that uses a value defined with a qualified class.
 */
@SuppressWarnings("unused")
public class AutofixUnsafeMethodWithQualifiedClass {
    /**
     * This method uses Window.Callback (not Callback), so the fix should also use Window.Callback.
     */
    @RequiresApi(23)
    public boolean unsafeReferenceOnQualifiedClass(Window.Callback callback,
            SearchEvent searchEvent) {
        return callback.onSearchRequested(searchEvent);
    }
}
