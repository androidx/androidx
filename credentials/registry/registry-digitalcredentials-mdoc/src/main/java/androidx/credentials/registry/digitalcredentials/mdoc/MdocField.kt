/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.credentials.registry.digitalcredentials.mdoc

import androidx.annotation.RestrictTo
import androidx.credentials.registry.provider.digitalcredentials.DigitalCredentialField
import androidx.credentials.registry.provider.digitalcredentials.FieldDisplayData

/**
 * A property of a [MdocEntry].
 *
 * @constructor
 * @property fieldName the field name, used for matching purpose; for example, the field name of an
 *   ISO mDL age-over-twenty-one property is "org.iso.18013.5.1.age_over_21"
 * @property fieldValue the field value, used for matching purpose; for example, the field value of
 *   an ISO mDL age-over-twenty-one property may be `true`
 * @property fieldDisplayData a set of field display metadata, each serving a different UI style
 *   variant
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class MdocField(
    public val fieldName: String,
    public val fieldValue: Any?,
    fieldDisplayData: Set<FieldDisplayData>,
) :
    DigitalCredentialField(
        fieldDisplayData = fieldDisplayData,
    )
