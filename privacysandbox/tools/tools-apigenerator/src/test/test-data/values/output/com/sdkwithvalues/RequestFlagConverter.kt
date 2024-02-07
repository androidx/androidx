package com.sdkwithvalues

public object RequestFlagConverter {
    public fun fromParcelable(parcelable: ParcelableRequestFlag): RequestFlag =
            RequestFlag.entries[parcelable.variant_ordinal]

    public fun toParcelable(annotatedValue: RequestFlag): ParcelableRequestFlag {
        val parcelable = ParcelableRequestFlag()
        parcelable.variant_ordinal = annotatedValue.ordinal
        return parcelable
    }
}
