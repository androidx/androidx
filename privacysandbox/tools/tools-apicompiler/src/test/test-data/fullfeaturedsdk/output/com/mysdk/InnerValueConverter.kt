package com.mysdk

public object InnerValueConverter {
    public fun fromParcelable(parcelable: ParcelableInnerValue): InnerValue {
        val annotatedValue = InnerValue(
                numbers = parcelable.numbers.toList(),
                maybeNumber = parcelable.maybeNumber.firstOrNull())
        return annotatedValue
    }

    public fun toParcelable(annotatedValue: InnerValue): ParcelableInnerValue {
        val parcelable = ParcelableInnerValue()
        parcelable.numbers = annotatedValue.numbers.toIntArray()
        parcelable.maybeNumber = if (annotatedValue.maybeNumber == null) intArrayOf() else
                intArrayOf(annotatedValue.maybeNumber)
        return parcelable
    }
}
