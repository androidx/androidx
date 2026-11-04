# Copyright (C) 2026 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# 1. Keep every class that has an `external` method and all its `external` methods.
-keepclasseswithmembers,includedescriptorclasses class androidx.webgpu.** {
    native <methods>;
}

# 2. Keep all constructors, handle field, property getters (val/var), and run()
#    on all classes except Builder and Companion classes.
-if class androidx.webgpu.** {
    native <methods>;
}
-keep class !androidx.webgpu.*$Builder**, !androidx.webgpu.*$Companion, androidx.webgpu.* {
    <init>(...);
    long handle;
    public *** get*();
    public *** is*();
    public void run();
}

-keepclassmembers class androidx.webgpu.GPUOrigin2D, androidx.webgpu.GPUExtent2D, androidx.webgpu.GPUColorSpaceDawn {
    <fields>;
}

# 3. Keep all methods on callback interfaces.
-if class androidx.webgpu.** {
    native <methods>;
}
-keep interface androidx.webgpu.*Callback* {
    <methods>;
}
