#  Copyright (C) 2020 The Android Open Source Project
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

# We keep all fields for every generated proto file as the runtime uses
# reflection over them that ProGuard cannot detect. Without this keep
# rule, fields may be removed that would cause runtime failures.
-keepclassmembers class * extends com.google.android.icing.protobuf.GeneratedMessageLite {
  <fields>;
}
-keep class com.google.android.icing.BreakIteratorBatcher { *; }

# This prevents the obfuscation or removal of fields referenced in native.
-keep class com.google.android.icing.IcingSearchEngineImpl
-keepclassmembers public class com.google.android.icing.IcingSearchEngineImpl {
  private long nativePointer;
  native <methods>;
}
