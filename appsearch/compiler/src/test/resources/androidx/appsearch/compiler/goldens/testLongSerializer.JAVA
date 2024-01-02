package com.example.appsearch;

import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.DocumentClassFactory;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.exceptions.AppSearchException;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.annotation.processing.Generated;

@Generated("androidx.appsearch.compiler.AppSearchCompiler")
public final class $$__AppSearch__Gift implements DocumentClassFactory<Gift> {
  public static final String SCHEMA_NAME = "Gift";

  @Override
  public String getSchemaName() {
    return SCHEMA_NAME;
  }

  @Override
  public AppSearchSchema getSchema() throws AppSearchException {
    return new AppSearchSchema.Builder(SCHEMA_NAME)
          .addProperty(new AppSearchSchema.LongPropertyConfig.Builder("pricePoint")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setIndexingType(AppSearchSchema.LongPropertyConfig.INDEXING_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.LongPropertyConfig.Builder("pricePointList")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .setIndexingType(AppSearchSchema.LongPropertyConfig.INDEXING_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.LongPropertyConfig.Builder("pricePointArr")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .setIndexingType(AppSearchSchema.LongPropertyConfig.INDEXING_TYPE_NONE)
            .build())
          .build();
  }

  @Override
  public List<Class<?>> getDependencyDocumentClasses() throws AppSearchException {
    return Collections.emptyList();
  }

  @Override
  public GenericDocument toGenericDocument(Gift document) throws AppSearchException {
    GenericDocument.Builder<?> builder =
        new GenericDocument.Builder<>(document.mNamespace, document.mId, SCHEMA_NAME);
    Gift.PricePoint mPricePointCopy = document.mPricePoint;
    if (mPricePointCopy != null) {
      Gift.PricePointAsOrdinalSerializer serializer = new Gift.PricePointAsOrdinalSerializer();
      long mPricePointConv = serializer.serialize(mPricePointCopy);
      builder.setPropertyLong("pricePoint", mPricePointConv);
    }
    List<Gift.PricePoint> mPricePointListCopy = document.mPricePointList;
    if (mPricePointListCopy != null) {
      long[] mPricePointListConv = new long[mPricePointListCopy.size()];
      Gift.PricePointAsOrdinalSerializer serializer = new Gift.PricePointAsOrdinalSerializer();
      int i = 0;
      for (Gift.PricePoint item : mPricePointListCopy) {
        mPricePointListConv[i++] = serializer.serialize(item);
      }
      builder.setPropertyLong("pricePointList", mPricePointListConv);
    }
    Gift.PricePoint[] mPricePointArrCopy = document.mPricePointArr;
    if (mPricePointArrCopy != null) {
      long[] mPricePointArrConv = new long[mPricePointArrCopy.length];
      Gift.PricePointAsOrdinalSerializer serializer = new Gift.PricePointAsOrdinalSerializer();
      for (int i = 0; i < mPricePointArrConv.length; i++) {
        mPricePointArrConv[i] = serializer.serialize(mPricePointArrCopy[i]);
      }
      builder.setPropertyLong("pricePointArr", mPricePointArrConv);
    }
    return builder.build();
  }

  @Override
  public Gift fromGenericDocument(GenericDocument genericDoc,
      Map<String, List<String>> documentClassMap) throws AppSearchException {
    String mIdConv = genericDoc.getId();
    String mNamespaceConv = genericDoc.getNamespace();
    long mPricePointCopy = genericDoc.getPropertyLong("pricePoint");
    Gift.PricePoint mPricePointConv = null;
    mPricePointConv = new Gift.PricePointAsOrdinalSerializer().deserialize(mPricePointCopy);
    long[] mPricePointListCopy = genericDoc.getPropertyLongArray("pricePointList");
    List<Gift.PricePoint> mPricePointListConv = null;
    if (mPricePointListCopy != null) {
      mPricePointListConv = new ArrayList<>(mPricePointListCopy.length);
      Gift.PricePointAsOrdinalSerializer serializer = new Gift.PricePointAsOrdinalSerializer();
      for (int i = 0; i < mPricePointListCopy.length; i++) {
        Gift.PricePoint elem = serializer.deserialize(mPricePointListCopy[i]);
        if (elem == null) {
          mPricePointListConv = null;
          break;
        }
        mPricePointListConv.add(elem);
      }
    }
    long[] mPricePointArrCopy = genericDoc.getPropertyLongArray("pricePointArr");
    Gift.PricePoint[] mPricePointArrConv = null;
    if (mPricePointArrCopy != null) {
      mPricePointArrConv = new Gift.PricePoint[mPricePointArrCopy.length];
      Gift.PricePointAsOrdinalSerializer serializer = new Gift.PricePointAsOrdinalSerializer();
      for (int i = 0; i < mPricePointArrCopy.length; i++) {
        Gift.PricePoint elem = serializer.deserialize(mPricePointArrCopy[i]);
        if (elem == null) {
          mPricePointArrConv = null;
          break;
        }
        mPricePointArrConv[i] = elem;
      }
    }
    Gift document = new Gift();
    document.mId = mIdConv;
    document.mNamespace = mNamespaceConv;
    document.mPricePoint = mPricePointConv;
    document.mPricePointList = mPricePointListConv;
    document.mPricePointArr = mPricePointArrConv;
    return document;
  }
}
