# Copyright (C) 2018 The Android Open Source Project
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

# Need to keep ShortcutInfoCompatSaverInfo class name and getInstance(...) method, which may be
# loaded by reflection in androidx.core.content.pm.ShortcutManagerCompat
-if public class androidx.core.content.pm.ShortcutManagerCompat
-keep public class androidx.sharetarget.ShortcutInfoCompatSaverImpl extends androidx.core.content.pm.ShortcutInfoCompatSaver {
    public static ShortcutInfoCompatSaverImpl getInstance(...);
}