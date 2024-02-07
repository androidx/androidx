package com.mysdk

import android.content.Context

public class RequestFlagConverter(
    public val context: Context,
) {
    public fun fromParcelable(parcelable: ParcelableRequestFlag): RequestFlag =
            RequestFlag.entries[parcelable.variant_ordinal]

    public fun toParcelable(annotatedValue: RequestFlag): ParcelableRequestFlag {
        val parcelable = ParcelableRequestFlag()
        parcelable.variant_ordinal = annotatedValue.ordinal
        return parcelable
    }
}
