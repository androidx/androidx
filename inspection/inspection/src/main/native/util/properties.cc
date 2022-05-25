/*
 * Copyright (C) 2022 The Android Open Source Project
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

#include <sys/system_properties.h>

#include "properties.h"

namespace androidx_inspection {

// Returns the value of the system property of the given |key|, or the
// |default_value| if the property is unavailable.
//
// __system_property_read() has been deprecated since API level 26 (O) because
// it works only on property whose name is shorter than 32 char (PROP_NAME_MAX)
// and value is shorter than 92 char (PROP_VALUE_MAX).
// __system_property_read_callback() is recommended since then but it's not
// availale for API < 26. The length limits on the name and value are not a
// concern for us. Therefore, we still use __system_property_read() for
// simplicity.
string GetProperty(const string& key, const string& default_value) {
    string property_value;
    const prop_info* pi = __system_property_find(key.c_str());
    if (pi == nullptr) return default_value;

    char name[PROP_NAME_MAX];
    char value[PROP_VALUE_MAX];

    if (__system_property_read(pi, name, value) == 0) return default_value;
    return value;
}

}  // namespace androidx_inspection

