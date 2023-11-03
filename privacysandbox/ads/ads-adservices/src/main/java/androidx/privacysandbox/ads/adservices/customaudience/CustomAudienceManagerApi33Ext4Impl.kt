/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.privacysandbox.ads.adservices.customaudience

import android.annotation.SuppressLint
import android.content.Context
import android.os.ext.SdkExtensions
import androidx.annotation.RequiresExtension
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY)
@SuppressLint("NewApi", "ClassVerificationFailure")
@RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 4)
class CustomAudienceManagerApi33Ext4Impl(context: Context) : CustomAudienceManagerImplCommon(
    context.getSystemService(android.adservices.customaudience.CustomAudienceManager::class.java))
