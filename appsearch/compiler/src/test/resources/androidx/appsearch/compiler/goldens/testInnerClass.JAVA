package com.example.appsearch;

import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.DataClassFactory;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.exceptions.AppSearchException;
import java.lang.Override;
import java.lang.String;

public class $$__AppSearch__Gift$$__InnerGift implements DataClassFactory<Gift.InnerGift> {
  private static final String SCHEMA_TYPE = "InnerGift";

  @Override
  public String getSchemaType() {
    return SCHEMA_TYPE;
  }

  @Override
  public AppSearchSchema getSchema() throws AppSearchException {
    return new AppSearchSchema.Builder(SCHEMA_TYPE)
          .addProperty(new AppSearchSchema.PropertyConfig.Builder("arrString")
            .setDataType(AppSearchSchema.PropertyConfig.DATA_TYPE_STRING)
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .setTokenizerType(AppSearchSchema.PropertyConfig.TOKENIZER_TYPE_NONE)
            .setIndexingType(AppSearchSchema.PropertyConfig.INDEXING_TYPE_NONE)
            .build())
          .build();
  }

  @Override
  public GenericDocument toGenericDocument(Gift.InnerGift dataClass) throws AppSearchException {
    GenericDocument.Builder<?> builder =
        new GenericDocument.Builder<>(dataClass.uri, SCHEMA_TYPE);
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
