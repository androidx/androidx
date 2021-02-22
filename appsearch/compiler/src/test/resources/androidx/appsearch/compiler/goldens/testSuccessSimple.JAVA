package com.example.appsearch;

import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.DataClassFactory;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.exceptions.AppSearchException;
import java.lang.Override;
import java.lang.String;

public class $$__AppSearch__Gift implements DataClassFactory<Gift> {
  private static final String SCHEMA_TYPE = "Gift";

  @Override
  public String getSchemaType() {
    return SCHEMA_TYPE;
  }

  @Override
  public AppSearchSchema getSchema() throws AppSearchException {
    return new AppSearchSchema.Builder(SCHEMA_TYPE)
          .addProperty(new AppSearchSchema.PropertyConfig.Builder("price")
            .setDataType(AppSearchSchema.PropertyConfig.DATA_TYPE_INT64)
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setTokenizerType(AppSearchSchema.PropertyConfig.TOKENIZER_TYPE_NONE)
            .setIndexingType(AppSearchSchema.PropertyConfig.INDEXING_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.PropertyConfig.Builder("cat")
            .setDataType(AppSearchSchema.PropertyConfig.DATA_TYPE_BOOLEAN)
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setTokenizerType(AppSearchSchema.PropertyConfig.TOKENIZER_TYPE_NONE)
            .setIndexingType(AppSearchSchema.PropertyConfig.INDEXING_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.PropertyConfig.Builder("dog")
            .setDataType(AppSearchSchema.PropertyConfig.DATA_TYPE_BOOLEAN)
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setTokenizerType(AppSearchSchema.PropertyConfig.TOKENIZER_TYPE_NONE)
            .setIndexingType(AppSearchSchema.PropertyConfig.INDEXING_TYPE_NONE)
            .build())
          .build();
  }

  @Override
  public GenericDocument toGenericDocument(Gift dataClass) throws AppSearchException {
    GenericDocument.Builder<?> builder =
        new GenericDocument.Builder<>(dataClass.uri, SCHEMA_TYPE);
    builder.setPropertyLong("price", dataClass.price);
    builder.setPropertyBoolean("cat", dataClass.cat);
    builder.setPropertyBoolean("dog", dataClass.getDog());
    return builder.build();
  }

  @Override
  public Gift fromGenericDocument(GenericDocument genericDoc) throws AppSearchException {
    String uriConv = genericDoc.getUri();
    int priceConv = (int) genericDoc.getPropertyLong("price");
    boolean catConv = genericDoc.getPropertyBoolean("cat");
    boolean dogConv = genericDoc.getPropertyBoolean("dog");
    Gift dataClass = new Gift(dogConv, uriConv);
    dataClass.price = priceConv;
    dataClass.cat = catConv;
    return dataClass;
  }
}
