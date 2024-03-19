package com.mysdk

import android.content.Context

public class RequestFlagConverter(
    public val context: Context,
) {
    private val enumValues: List<RequestFlag> = RequestFlag.values().toList()

    public fun fromParcelable(parcelable: ParcelableRequestFlag): RequestFlag =
            enumValues[parcelable.variant_ordinal]

    public fun toParcelable(annotatedValue: RequestFlag): ParcelableRequestFlag {
        val parcelable = ParcelableRequestFlag()
        parcelable.variant_ordinal = annotatedValue.ordinal
        return parcelable
    }
}
