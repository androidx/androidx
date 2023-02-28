package com.sdkwithvalues

public object SdkRequestConverter {
    public fun fromParcelable(parcelable: ParcelableSdkRequest): SdkRequest {
        val annotatedValue = SdkRequest(
                id = parcelable.id,
                innerValue =
                        com.sdkwithvalues.InnerSdkValueConverter.fromParcelable(parcelable.innerValue),
                maybeInnerValue = parcelable.maybeInnerValue?.let { notNullValue ->
                        com.sdkwithvalues.InnerSdkValueConverter.fromParcelable(notNullValue) },
                moreValues = parcelable.moreValues.map {
                        com.sdkwithvalues.InnerSdkValueConverter.fromParcelable(it) }.toList())
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
        return parcelable
    }
}
