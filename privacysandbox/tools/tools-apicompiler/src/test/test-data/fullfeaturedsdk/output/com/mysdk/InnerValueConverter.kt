package com.mysdk

public object InnerValueConverter {
    public fun fromParcelable(parcelable: ParcelableInnerValue): InnerValue {
        val annotatedValue = InnerValue(
                numbers = parcelable.numbers.toList())
        return annotatedValue
    }

    public fun toParcelable(annotatedValue: InnerValue): ParcelableInnerValue {
        val parcelable = ParcelableInnerValue()
        parcelable.numbers = annotatedValue.numbers.toIntArray()
        return parcelable
    }
}
