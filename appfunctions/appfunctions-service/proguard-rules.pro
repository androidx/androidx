#  Copyright (C) 2025 The Android Open Source Project
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.

# Keeps Aggregated inventory/invoker only as they are created from reflection.
-keep,allowoptimization class * implements androidx.appfunctions.service.internal.AggregatedAppFunctionInvoker {
    public *;
}
-keep,allowoptimization class * implements androidx.appfunctions.service.internal.AggregatedAppFunctionInventory {
    public *;
}

# Keeps PlatformAppFunctionService because the caller is from IPC
-keep,allowoptimization class androidx.appfunctions.service.PlatformAppFunctionService {
    public *;
}

# Keeps ExtensionAppFunctionService because the caller is from IPC
-keep,allowoptimization class androidx.appfunctions.service.ExtensionAppFunctionService {
    public *;
}

-keep class androidx.appfunctions.service.internal.AggregatedAppFunctionInvoker { *; }
-keep class androidx.appfunctions.service.internal.AggregatedAppFunctionInventory { *; }
