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

# TODO(b/402763650): Generate a mapping instead of using keep
-keepnames @androidx.appfunctions.AppFunctionSerializable class ** {}
-keep,allowoptimization class ** implements androidx.appfunctions.internal.AppFunctionSerializableFactory {
  public *;
}

-keep class androidx.appfunctions.internal.SchemaAppFunctionInventory { *; }
-keep,allowoptimization class * implements androidx.appfunctions.internal.SchemaAppFunctionInventory {
    public *;
}

-keep class androidx.appfunctions.internal.AggregatedAppFunctionInventory { *; }
-keep,allowoptimization class * implements androidx.appfunctions.internal.AggregatedAppFunctionInventory {
    public *;
}

# TODO: b/440484133 - Remove once AppSearch updates their rules
-if class androidx.appsearch.app.DocumentClassFactoryRegistry {}
-keep,allowshrinking @androidx.appsearch.annotation.Document class ** {}
