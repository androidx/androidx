package com.sdkwithvalues

import com.sdkwithvalues.SdkActivityLauncherConverter.getLocalOrProxyLauncher
import com.sdkwithvalues.SdkActivityLauncherConverter.toBinder

public object SdkRequestConverter {
    public fun fromParcelable(parcelable: ParcelableSdkRequest): SdkRequest {
        val annotatedValue = SdkRequest(
                id = parcelable.id,
                innerValue =
                        com.sdkwithvalues.InnerSdkValueConverter.fromParcelable(parcelable.innerValue),
                maybeInnerValue = parcelable.maybeInnerValue?.let { notNullValue ->
                        com.sdkwithvalues.InnerSdkValueConverter.fromParcelable(notNullValue) },
                moreValues = parcelable.moreValues.map {
                        com.sdkwithvalues.InnerSdkValueConverter.fromParcelable(it) }.toList(),
                activityLauncher = getLocalOrProxyLauncher(parcelable.activityLauncher))
        return annotatedValue
    }

    public fun toParcelable(annotatedValue: SdkRequest): ParcelableSdkRequest {
        val parcelable = ParcelableSdkRequest()
        parcelable.id = annotatedValue.id
        parcelable.innerValue =
                com.sdkwithvalues.InnerSdkValueConverter.toParcelable(annotatedValue.innerValue)
        parcelable.maybeInnerValue = annotatedValue.maybeInnerValue?.let { notNullValue ->
                com.sdkwithvalues.InnerSdkValueConverter.toParcelable(notNullValue) }
        parcelable.moreValues = annotatedValue.moreValues.map {
                com.sdkwithvalues.InnerSdkValueConverter.toParcelable(it) }.toTypedArray()
        parcelable.activityLauncher = toBinder(annotatedValue.activityLauncher)
        return parcelable
    }
}
