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

package androidx.wear.watchface.complications.datasource;

import android.os.RemoteException;

import androidx.annotation.Nullable;
import androidx.wear.watchface.complications.data.ComplicationData;

/** Tests that Java interfaces implementing kotlin interfaces with defaults compile. */
public class JavaCompatTest {
    class ComplicationRequestListenerImpl
            implements ComplicationDataSourceService.ComplicationRequestListener {

        @Override
        public void onComplicationData(@Nullable ComplicationData complicationData)
                throws RemoteException {}
    }
}
