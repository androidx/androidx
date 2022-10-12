package com.sdkwithvalues

public object SdkResponseConverter {
    public fun fromParcelable(parcelable: ParcelableSdkResponse): SdkResponse {
        val annotatedValue = SdkResponse(
                originalRequest =
                        com.sdkwithvalues.SdkRequestConverter.fromParcelable(parcelable.originalRequest),
                success = parcelable.success)
        return annotatedValue
    }

    public fun toParcelable(annotatedValue: SdkResponse): ParcelableSdkResponse {
        val parcelable = ParcelableSdkResponse()
        parcelable.originalRequest =
                com.sdkwithvalues.SdkRequestConverter.toParcelable(annotatedValue.originalRequest)
        parcelable.success = annotatedValue.success
        return parcelable
    }
}
