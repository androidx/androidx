package com.sdkwithcallbacks

public object ResponseConverter {
    public fun fromParcelable(parcelable: ParcelableResponse): Response {
        val annotatedValue = Response(
                response = parcelable.response)
        return annotatedValue
    }

    public fun toParcelable(annotatedValue: Response): ParcelableResponse {
        val parcelable = ParcelableResponse()
        parcelable.response = annotatedValue.response
        return parcelable
    }
}
