package com.myotherpackage

import android.content.Context

public class MyOtherPackageDataClassConverter(
    public val context: Context,
) {
    public fun fromParcelable(parcelable: ParcelableMyOtherPackageDataClass): MyOtherPackageDataClass {
        val annotatedValue = MyOtherPackageDataClass(
                query = parcelable.query)
        return annotatedValue
    }

    public fun toParcelable(annotatedValue: MyOtherPackageDataClass): ParcelableMyOtherPackageDataClass {
        val parcelable = ParcelableMyOtherPackageDataClass()
        parcelable.query = annotatedValue.query
        return parcelable
    }
}
