package com.sdkwithvalues

public object SdkRequestConverter {
    public fun fromParcelable(parcelable: ParcelableSdkRequest): SdkRequest {
        val annotatedValue = SdkRequest(
                id = parcelable.id,
                innerValue =
                        com.sdkwithvalues.InnerSdkValueConverter.fromParcelable(parcelable.innerValue))
        return annotatedValue
    }

    public fun toParcelable(annotatedValue: SdkRequest): ParcelableSdkRequest {
        val parcelable = ParcelableSdkRequest()
        parcelable.id = annotatedValue.id
        parcelable.innerValue =
                com.sdkwithvalues.InnerSdkValueConverter.toParcelable(annotatedValue.innerValue)
        return parcelable
    }
}
