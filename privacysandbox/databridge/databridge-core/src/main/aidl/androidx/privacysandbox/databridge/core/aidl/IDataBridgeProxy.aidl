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


package androidx.privacysandbox.databridge.core.aidl;

import androidx.privacysandbox.databridge.core.aidl.IGetValuesResultCallback;
import androidx.privacysandbox.databridge.core.aidl.IKeyUpdateInternalCallback;
import androidx.privacysandbox.databridge.core.aidl.ISetValuesResultCallback;
import androidx.privacysandbox.databridge.core.aidl.IRemoveValuesResultCallback;
import androidx.privacysandbox.databridge.core.aidl.ValueInternal;

@JavaPassthrough(annotation="@androidx.annotation.RestrictTo(androidx.annotation.RestrictTo.Scope.LIBRARY)")
oneway interface IDataBridgeProxy {
	void getValues(in List<String> keyNames, in List<String> keyTypes, IGetValuesResultCallback callback);
    void setValues(in List<String> keyNames, in List<ValueInternal> data, ISetValuesResultCallback callback);
    void removeValues(in List<String> keyNames, in List<String> keyTypes, IRemoveValuesResultCallback callback);

    void addKeysForUpdates(String uuid, in List<String> keyNames, in List<String> keyTypes, IKeyUpdateInternalCallback callback);
    void removeKeysFromUpdates(String uuid, in List<String> keyNames, in List<String> keyTypes, boolean unregisterCallback);
}
