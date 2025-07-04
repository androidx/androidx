package com.sdkwithvalues

public object SdkResponseConverter {
    public fun fromParcelable(parcelable: ParcelableSdkResponse): SdkResponse {
        val annotatedValue = SdkResponse(
                success = parcelable.success,
                originalRequest = com.sdkwithvalues.SdkRequestConverter.fromParcelable(parcelable.originalRequest))
        return annotatedValue
    }

    public fun toParcelable(annotatedValue: SdkResponse): ParcelableSdkResponse {
        val parcelable = ParcelableSdkResponse()
        parcelable.success = annotatedValue.success
        parcelable.originalRequest = com.sdkwithvalues.SdkRequestConverter.toParcelable(annotatedValue.originalRequest)
        return parcelable
    }
}
