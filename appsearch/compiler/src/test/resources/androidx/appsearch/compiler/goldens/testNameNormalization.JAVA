package com.example.appsearch;

import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.DocumentClassFactory;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.exceptions.AppSearchException;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.util.Collections;
import java.util.List;
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
          .addProperty(new AppSearchSchema.LongPropertyConfig.Builder("mPrice")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setIndexingType(AppSearchSchema.LongPropertyConfig.INDEXING_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.LongPropertyConfig.Builder("price")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setIndexingType(AppSearchSchema.LongPropertyConfig.INDEXING_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.BooleanPropertyConfig.Builder("sold")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .build())
          .addProperty(new AppSearchSchema.LongPropertyConfig.Builder("mx")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
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
        new GenericDocument.Builder<>(document.namespace, document.id, SCHEMA_NAME);
    builder.setPropertyLong("mPrice", document.getMPrice());
    builder.setPropertyLong("price", document.mPrice);
    builder.setPropertyBoolean("sold", document.isSold());
    builder.setPropertyLong("mx", document.mx());
    return builder.build();
  }

  @Override
  public Gift fromGenericDocument(GenericDocument genericDoc) throws AppSearchException {
    String namespaceConv = genericDoc.getNamespace();
    String idConv = genericDoc.getId();
    int getMPriceConv = (int) genericDoc.getPropertyLong("mPrice");
    int mPriceConv = (int) genericDoc.getPropertyLong("price");
    boolean isSoldConv = genericDoc.getPropertyBoolean("sold");
    int mxConv = (int) genericDoc.getPropertyLong("mx");
    Gift document = new Gift();
    document.namespace = namespaceConv;
    document.id = idConv;
    document.setMPrice(getMPriceConv);
    document.mPrice = mPriceConv;
    document.setSold(isSoldConv);
    document.setMx(mxConv);
    return document;
  }
}
