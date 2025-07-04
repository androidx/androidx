package com.mysdk

import android.content.Context

public class InnerValueConverter(
    public val context: Context,
) {
    public fun fromParcelable(parcelable: ParcelableInnerValue): InnerValue {
        val annotatedValue = InnerValue(
                numbers = parcelable.numbers.toList(),
                maybeNumber = parcelable.maybeNumber.firstOrNull())
        return annotatedValue
    }

    public fun toParcelable(annotatedValue: InnerValue): ParcelableInnerValue {
        val parcelable = ParcelableInnerValue()
        parcelable.numbers = annotatedValue.numbers.toIntArray()
        parcelable.maybeNumber = if (annotatedValue.maybeNumber == null) intArrayOf() else intArrayOf(annotatedValue.maybeNumber)
        return parcelable
    }
}
