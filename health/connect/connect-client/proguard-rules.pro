#  Copyright (C) 2022 The Android Open Source Project
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

# libproto uses reflection to deserialize a Proto, which Proguard can't accurately detect.
# Keep all the class members of any generated messages to ensure we can deserialize properly inside
# these classes.
-keepclassmembers class * extends androidx.health.platform.client.proto.GeneratedMessageLite {
  <fields>;
}

# When constructing ErrorStatus, we use reflection to check the field is
# declared, so names need to be kept in client apps.
-keepclassmembers class androidx.health.platform.client.error.ErrorCode { *; }

# Permissions are put into Intents and retrieved via getParcelable, which uses
# reflection and require name to be kept in client apps.
-keepnames class androidx.health.platform.client.permission.Permission

# ExerciseRoute is put into Intents and retrieved via getParcelable, which uses
# reflection and require name to be kept in client apps.
-keepnames class androidx.health.platform.client.exerciseroute.ExerciseRoute
