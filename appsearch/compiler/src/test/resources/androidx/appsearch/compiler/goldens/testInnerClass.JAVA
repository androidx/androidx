package com.example.appsearch;

import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.DocumentClassFactory;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.exceptions.AppSearchException;
import java.lang.Override;
import java.lang.String;

public class $$__AppSearch__Gift$$__InnerGift implements DocumentClassFactory<Gift.InnerGift> {
  private static final String SCHEMA_NAME = "InnerGift";

  @Override
  public String getSchemaName() {
    return SCHEMA_NAME;
  }

  @Override
  public AppSearchSchema getSchema() throws AppSearchException {
    return new AppSearchSchema.Builder(SCHEMA_NAME)
          .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("arrString")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_NONE)
            .setIndexingType(AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_NONE)
            .build())
          .build();
  }

  @Override
  public GenericDocument toGenericDocument(Gift.InnerGift dataClass) throws AppSearchException {
    GenericDocument.Builder<?> builder =
        new GenericDocument.Builder<>(dataClass.uri, SCHEMA_NAME);
    String[] arrStringCopy = dataClass.arrString;
    if (arrStringCopy != null) {
      builder.setPropertyString("arrString", arrStringCopy);
    }
    return builder.build();
  }

  @Override
  public Gift.InnerGift fromGenericDocument(GenericDocument genericDoc) throws AppSearchException {
    String uriConv = genericDoc.getUri();
    String[] arrStringConv = genericDoc.getPropertyStringArray("arrString");
    Gift.InnerGift dataClass = new Gift.InnerGift();
    dataClass.uri = uriConv;
    dataClass.arrString = arrStringConv;
    return dataClass;
  }
}
