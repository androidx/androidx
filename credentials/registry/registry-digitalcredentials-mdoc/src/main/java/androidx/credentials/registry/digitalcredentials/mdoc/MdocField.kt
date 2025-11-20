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

import androidx.credentials.registry.provider.digitalcredentials.DigitalCredentialField
import androidx.credentials.registry.provider.digitalcredentials.FieldDisplayProperties

/**
 * A property of a [MdocEntry].
 *
 * @property namespace the name space of this field, used for matching purpose; for example, the
 *   namespace of an ISO mDL age-over-twenty-one property is "org.iso.18013.5.1"
 * @property identifier the identifier of this field within the `namespace`, used for matching
 *   purpose; for example, the field name of an ISO mDL age-over-twenty-one property is
 *   "age_over_21"
 * @property fieldValue the field value, used for matching purpose; for example, the field value of
 *   an ISO mDL age-over-twenty-one property may be `true`; a null value means that the exact value
 *   of this field cannot be used for matching (e.g. a user photo), or in other words, attempt to do
 *   value matching on this field will automatically fail
 * @property fieldDisplayPropertySet a set of field display metadata, each serving a different UI
 *   style variant
 * @constructor
 */
public class MdocField(
    public val namespace: String,
    public val identifier: String,
    public val fieldValue: Any?,
    fieldDisplayPropertySet: Set<FieldDisplayProperties>,
) : DigitalCredentialField(fieldDisplayPropertySet = fieldDisplayPropertySet) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MdocField) return false
        return this.fieldDisplayPropertySet == other.fieldDisplayPropertySet &&
            this.namespace == other.namespace &&
            this.identifier == other.identifier &&
            this.fieldValue == other.fieldValue
    }

    override fun hashCode(): Int {
        var result = fieldDisplayPropertySet.hashCode()
        result = 31 * result + namespace.hashCode()
        result = 31 * result + identifier.hashCode()
        result = 31 * result + (fieldValue?.hashCode() ?: 0)
        return result
    }
}
