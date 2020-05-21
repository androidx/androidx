package com.example.appsearch;

import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.GenericDocument;
import java.lang.Boolean;
import java.lang.Byte;
import java.lang.Double;
import java.lang.Float;
import java.lang.Integer;
import java.lang.Long;
import java.lang.String;
import java.util.Set;

class $$__AppSearch__Gift {
  public static final String SCHEMA_TYPE = "Gift";

  public static final AppSearchSchema SCHEMA = new AppSearchSchema.Builder(SCHEMA_TYPE)
    .addProperty(new AppSearchSchema.PropertyConfig.Builder("collectLong")
      .setDataType(AppSearchSchema.PropertyConfig.DATA_TYPE_INT64)
      .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
      .setTokenizerType(AppSearchSchema.PropertyConfig.TOKENIZER_TYPE_PLAIN)
      .setIndexingType(AppSearchSchema.PropertyConfig.INDEXING_TYPE_NONE)
      .build())
    .addProperty(new AppSearchSchema.PropertyConfig.Builder("collectInteger")
      .setDataType(AppSearchSchema.PropertyConfig.DATA_TYPE_INT64)
      .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
      .setTokenizerType(AppSearchSchema.PropertyConfig.TOKENIZER_TYPE_PLAIN)
      .setIndexingType(AppSearchSchema.PropertyConfig.INDEXING_TYPE_NONE)
      .build())
    .addProperty(new AppSearchSchema.PropertyConfig.Builder("collectDouble")
      .setDataType(AppSearchSchema.PropertyConfig.DATA_TYPE_DOUBLE)
      .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
      .setTokenizerType(AppSearchSchema.PropertyConfig.TOKENIZER_TYPE_PLAIN)
      .setIndexingType(AppSearchSchema.PropertyConfig.INDEXING_TYPE_NONE)
      .build())
    .addProperty(new AppSearchSchema.PropertyConfig.Builder("collectFloat")
      .setDataType(AppSearchSchema.PropertyConfig.DATA_TYPE_DOUBLE)
      .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
      .setTokenizerType(AppSearchSchema.PropertyConfig.TOKENIZER_TYPE_PLAIN)
      .setIndexingType(AppSearchSchema.PropertyConfig.INDEXING_TYPE_NONE)
      .build())
    .addProperty(new AppSearchSchema.PropertyConfig.Builder("collectBoolean")
      .setDataType(AppSearchSchema.PropertyConfig.DATA_TYPE_BOOLEAN)
      .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
      .setTokenizerType(AppSearchSchema.PropertyConfig.TOKENIZER_TYPE_PLAIN)
      .setIndexingType(AppSearchSchema.PropertyConfig.INDEXING_TYPE_NONE)
      .build())
    .addProperty(new AppSearchSchema.PropertyConfig.Builder("collectByteArr")
      .setDataType(AppSearchSchema.PropertyConfig.DATA_TYPE_BYTES)
      .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
      .setTokenizerType(AppSearchSchema.PropertyConfig.TOKENIZER_TYPE_PLAIN)
      .setIndexingType(AppSearchSchema.PropertyConfig.INDEXING_TYPE_NONE)
      .build())
    .addProperty(new AppSearchSchema.PropertyConfig.Builder("collectString")
      .setDataType(AppSearchSchema.PropertyConfig.DATA_TYPE_STRING)
      .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
      .setTokenizerType(AppSearchSchema.PropertyConfig.TOKENIZER_TYPE_PLAIN)
      .setIndexingType(AppSearchSchema.PropertyConfig.INDEXING_TYPE_NONE)
      .build())
    .addProperty(new AppSearchSchema.PropertyConfig.Builder("arrBoxLong")
      .setDataType(AppSearchSchema.PropertyConfig.DATA_TYPE_INT64)
      .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
      .setTokenizerType(AppSearchSchema.PropertyConfig.TOKENIZER_TYPE_PLAIN)
      .setIndexingType(AppSearchSchema.PropertyConfig.INDEXING_TYPE_NONE)
      .build())
    .addProperty(new AppSearchSchema.PropertyConfig.Builder("arrUnboxLong")
      .setDataType(AppSearchSchema.PropertyConfig.DATA_TYPE_INT64)
      .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
      .setTokenizerType(AppSearchSchema.PropertyConfig.TOKENIZER_TYPE_PLAIN)
      .setIndexingType(AppSearchSchema.PropertyConfig.INDEXING_TYPE_NONE)
      .build())
    .addProperty(new AppSearchSchema.PropertyConfig.Builder("arrBoxInteger")
      .setDataType(AppSearchSchema.PropertyConfig.DATA_TYPE_INT64)
      .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
      .setTokenizerType(AppSearchSchema.PropertyConfig.TOKENIZER_TYPE_PLAIN)
      .setIndexingType(AppSearchSchema.PropertyConfig.INDEXING_TYPE_NONE)
      .build())
    .addProperty(new AppSearchSchema.PropertyConfig.Builder("arrUnboxInt")
      .setDataType(AppSearchSchema.PropertyConfig.DATA_TYPE_INT64)
      .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
      .setTokenizerType(AppSearchSchema.PropertyConfig.TOKENIZER_TYPE_PLAIN)
      .setIndexingType(AppSearchSchema.PropertyConfig.INDEXING_TYPE_NONE)
      .build())
    .addProperty(new AppSearchSchema.PropertyConfig.Builder("arrBoxDouble")
      .setDataType(AppSearchSchema.PropertyConfig.DATA_TYPE_DOUBLE)
      .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
      .setTokenizerType(AppSearchSchema.PropertyConfig.TOKENIZER_TYPE_PLAIN)
      .setIndexingType(AppSearchSchema.PropertyConfig.INDEXING_TYPE_NONE)
      .build())
    .addProperty(new AppSearchSchema.PropertyConfig.Builder("arrUnboxDouble")
      .setDataType(AppSearchSchema.PropertyConfig.DATA_TYPE_DOUBLE)
      .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
      .setTokenizerType(AppSearchSchema.PropertyConfig.TOKENIZER_TYPE_PLAIN)
      .setIndexingType(AppSearchSchema.PropertyConfig.INDEXING_TYPE_NONE)
      .build())
    .addProperty(new AppSearchSchema.PropertyConfig.Builder("arrBoxFloat")
      .setDataType(AppSearchSchema.PropertyConfig.DATA_TYPE_DOUBLE)
      .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
      .setTokenizerType(AppSearchSchema.PropertyConfig.TOKENIZER_TYPE_PLAIN)
      .setIndexingType(AppSearchSchema.PropertyConfig.INDEXING_TYPE_NONE)
      .build())
    .addProperty(new AppSearchSchema.PropertyConfig.Builder("arrUnboxFloat")
      .setDataType(AppSearchSchema.PropertyConfig.DATA_TYPE_DOUBLE)
      .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
      .setTokenizerType(AppSearchSchema.PropertyConfig.TOKENIZER_TYPE_PLAIN)
      .setIndexingType(AppSearchSchema.PropertyConfig.INDEXING_TYPE_NONE)
      .build())
    .addProperty(new AppSearchSchema.PropertyConfig.Builder("arrBoxBoolean")
      .setDataType(AppSearchSchema.PropertyConfig.DATA_TYPE_BOOLEAN)
      .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
      .setTokenizerType(AppSearchSchema.PropertyConfig.TOKENIZER_TYPE_PLAIN)
      .setIndexingType(AppSearchSchema.PropertyConfig.INDEXING_TYPE_NONE)
      .build())
    .addProperty(new AppSearchSchema.PropertyConfig.Builder("arrUnboxBoolean")
      .setDataType(AppSearchSchema.PropertyConfig.DATA_TYPE_BOOLEAN)
      .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
      .setTokenizerType(AppSearchSchema.PropertyConfig.TOKENIZER_TYPE_PLAIN)
      .setIndexingType(AppSearchSchema.PropertyConfig.INDEXING_TYPE_NONE)
      .build())
    .addProperty(new AppSearchSchema.PropertyConfig.Builder("arrUnboxByteArr")
      .setDataType(AppSearchSchema.PropertyConfig.DATA_TYPE_BYTES)
      .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
      .setTokenizerType(AppSearchSchema.PropertyConfig.TOKENIZER_TYPE_PLAIN)
      .setIndexingType(AppSearchSchema.PropertyConfig.INDEXING_TYPE_NONE)
      .build())
    .addProperty(new AppSearchSchema.PropertyConfig.Builder("boxByteArr")
      .setDataType(AppSearchSchema.PropertyConfig.DATA_TYPE_BYTES)
      .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
      .setTokenizerType(AppSearchSchema.PropertyConfig.TOKENIZER_TYPE_PLAIN)
      .setIndexingType(AppSearchSchema.PropertyConfig.INDEXING_TYPE_NONE)
      .build())
    .addProperty(new AppSearchSchema.PropertyConfig.Builder("arrString")
      .setDataType(AppSearchSchema.PropertyConfig.DATA_TYPE_STRING)
      .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
      .setTokenizerType(AppSearchSchema.PropertyConfig.TOKENIZER_TYPE_PLAIN)
      .setIndexingType(AppSearchSchema.PropertyConfig.INDEXING_TYPE_NONE)
      .build())
    .addProperty(new AppSearchSchema.PropertyConfig.Builder("string")
      .setDataType(AppSearchSchema.PropertyConfig.DATA_TYPE_STRING)
      .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
      .setTokenizerType(AppSearchSchema.PropertyConfig.TOKENIZER_TYPE_PLAIN)
      .setIndexingType(AppSearchSchema.PropertyConfig.INDEXING_TYPE_NONE)
      .build())
    .addProperty(new AppSearchSchema.PropertyConfig.Builder("boxLong")
      .setDataType(AppSearchSchema.PropertyConfig.DATA_TYPE_INT64)
      .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
      .setTokenizerType(AppSearchSchema.PropertyConfig.TOKENIZER_TYPE_PLAIN)
      .setIndexingType(AppSearchSchema.PropertyConfig.INDEXING_TYPE_NONE)
      .build())
    .addProperty(new AppSearchSchema.PropertyConfig.Builder("unboxLong")
      .setDataType(AppSearchSchema.PropertyConfig.DATA_TYPE_INT64)
      .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
      .setTokenizerType(AppSearchSchema.PropertyConfig.TOKENIZER_TYPE_PLAIN)
      .setIndexingType(AppSearchSchema.PropertyConfig.INDEXING_TYPE_NONE)
      .build())
    .addProperty(new AppSearchSchema.PropertyConfig.Builder("boxInteger")
      .setDataType(AppSearchSchema.PropertyConfig.DATA_TYPE_INT64)
      .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
      .setTokenizerType(AppSearchSchema.PropertyConfig.TOKENIZER_TYPE_PLAIN)
      .setIndexingType(AppSearchSchema.PropertyConfig.INDEXING_TYPE_NONE)
      .build())
    .addProperty(new AppSearchSchema.PropertyConfig.Builder("unboxInt")
      .setDataType(AppSearchSchema.PropertyConfig.DATA_TYPE_INT64)
      .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
      .setTokenizerType(AppSearchSchema.PropertyConfig.TOKENIZER_TYPE_PLAIN)
      .setIndexingType(AppSearchSchema.PropertyConfig.INDEXING_TYPE_NONE)
      .build())
    .addProperty(new AppSearchSchema.PropertyConfig.Builder("boxDouble")
      .setDataType(AppSearchSchema.PropertyConfig.DATA_TYPE_DOUBLE)
      .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
      .setTokenizerType(AppSearchSchema.PropertyConfig.TOKENIZER_TYPE_PLAIN)
      .setIndexingType(AppSearchSchema.PropertyConfig.INDEXING_TYPE_NONE)
      .build())
    .addProperty(new AppSearchSchema.PropertyConfig.Builder("unboxDouble")
      .setDataType(AppSearchSchema.PropertyConfig.DATA_TYPE_DOUBLE)
      .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
      .setTokenizerType(AppSearchSchema.PropertyConfig.TOKENIZER_TYPE_PLAIN)
      .setIndexingType(AppSearchSchema.PropertyConfig.INDEXING_TYPE_NONE)
      .build())
    .addProperty(new AppSearchSchema.PropertyConfig.Builder("boxFloat")
      .setDataType(AppSearchSchema.PropertyConfig.DATA_TYPE_DOUBLE)
      .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
      .setTokenizerType(AppSearchSchema.PropertyConfig.TOKENIZER_TYPE_PLAIN)
      .setIndexingType(AppSearchSchema.PropertyConfig.INDEXING_TYPE_NONE)
      .build())
    .addProperty(new AppSearchSchema.PropertyConfig.Builder("unboxFloat")
      .setDataType(AppSearchSchema.PropertyConfig.DATA_TYPE_DOUBLE)
      .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
      .setTokenizerType(AppSearchSchema.PropertyConfig.TOKENIZER_TYPE_PLAIN)
      .setIndexingType(AppSearchSchema.PropertyConfig.INDEXING_TYPE_NONE)
      .build())
    .addProperty(new AppSearchSchema.PropertyConfig.Builder("boxBoolean")
      .setDataType(AppSearchSchema.PropertyConfig.DATA_TYPE_BOOLEAN)
      .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
      .setTokenizerType(AppSearchSchema.PropertyConfig.TOKENIZER_TYPE_PLAIN)
      .setIndexingType(AppSearchSchema.PropertyConfig.INDEXING_TYPE_NONE)
      .build())
    .addProperty(new AppSearchSchema.PropertyConfig.Builder("unboxBoolean")
      .setDataType(AppSearchSchema.PropertyConfig.DATA_TYPE_BOOLEAN)
      .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
      .setTokenizerType(AppSearchSchema.PropertyConfig.TOKENIZER_TYPE_PLAIN)
      .setIndexingType(AppSearchSchema.PropertyConfig.INDEXING_TYPE_NONE)
      .build())
    .addProperty(new AppSearchSchema.PropertyConfig.Builder("unboxByteArr")
      .setDataType(AppSearchSchema.PropertyConfig.DATA_TYPE_BYTES)
      .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
      .setTokenizerType(AppSearchSchema.PropertyConfig.TOKENIZER_TYPE_PLAIN)
      .setIndexingType(AppSearchSchema.PropertyConfig.INDEXING_TYPE_NONE)
      .build())
    .build();

  public GenericDocument toGenericDocument(Gift dataClass) {
    GenericDocument.Builder<?> builder =
        new GenericDocument.Builder<>(dataClass.uri, SCHEMA_TYPE);
    Set<Long> collectLongCopy = dataClass.collectLong;
    if (collectLongCopy != null) {
      long[] collectLongConv = new long[collectLongCopy.size()];
      int i = 0;
      for (Long item : collectLongCopy) {
        collectLongConv[i++] = item;
      }
      builder.setProperty("collectLong", collectLongConv);
    }
    Set<Integer> collectIntegerCopy = dataClass.collectInteger;
    if (collectIntegerCopy != null) {
      long[] collectIntegerConv = new long[collectIntegerCopy.size()];
      int i = 0;
      for (Integer item : collectIntegerCopy) {
        collectIntegerConv[i++] = item;
      }
      builder.setProperty("collectInteger", collectIntegerConv);
    }
    Set<Double> collectDoubleCopy = dataClass.collectDouble;
    if (collectDoubleCopy != null) {
      double[] collectDoubleConv = new double[collectDoubleCopy.size()];
      int i = 0;
      for (Double item : collectDoubleCopy) {
        collectDoubleConv[i++] = item;
      }
      builder.setProperty("collectDouble", collectDoubleConv);
    }
    Set<Float> collectFloatCopy = dataClass.collectFloat;
    if (collectFloatCopy != null) {
      double[] collectFloatConv = new double[collectFloatCopy.size()];
      int i = 0;
      for (Float item : collectFloatCopy) {
        collectFloatConv[i++] = item;
      }
      builder.setProperty("collectFloat", collectFloatConv);
    }
    Set<Boolean> collectBooleanCopy = dataClass.collectBoolean;
    if (collectBooleanCopy != null) {
      boolean[] collectBooleanConv = new boolean[collectBooleanCopy.size()];
      int i = 0;
      for (Boolean item : collectBooleanCopy) {
        collectBooleanConv[i++] = item;
      }
      builder.setProperty("collectBoolean", collectBooleanConv);
    }
    Set<byte[]> collectByteArrCopy = dataClass.collectByteArr;
    if (collectByteArrCopy != null) {
      byte[][] collectByteArrConv = new byte[collectByteArrCopy.size()][];
      int i = 0;
      for (byte[] item : collectByteArrCopy) {
        collectByteArrConv[i++] = item;
      }
      builder.setProperty("collectByteArr", collectByteArrConv);
    }
    Set<String> collectStringCopy = dataClass.collectString;
    if (collectStringCopy != null) {
      String[] collectStringConv = collectStringCopy.toArray(new String[0]);
      builder.setProperty("collectString", collectStringConv);
    }
    Long[] arrBoxLongCopy = dataClass.arrBoxLong;
    if (arrBoxLongCopy != null) {
      long[] arrBoxLongConv = new long[arrBoxLongCopy.length];
      for (int i = 0 ; i < arrBoxLongCopy.length ; i++) {
        arrBoxLongConv[i] = arrBoxLongCopy[i];
      }
      builder.setProperty("arrBoxLong", arrBoxLongConv);
    }
    long[] arrUnboxLongCopy = dataClass.arrUnboxLong;
    if (arrUnboxLongCopy != null) {
      builder.setProperty("arrUnboxLong", arrUnboxLongCopy);
    }
    Integer[] arrBoxIntegerCopy = dataClass.arrBoxInteger;
    if (arrBoxIntegerCopy != null) {
      long[] arrBoxIntegerConv = new long[arrBoxIntegerCopy.length];
      for (int i = 0 ; i < arrBoxIntegerCopy.length ; i++) {
        arrBoxIntegerConv[i] = arrBoxIntegerCopy[i];
      }
      builder.setProperty("arrBoxInteger", arrBoxIntegerConv);
    }
    int[] arrUnboxIntCopy = dataClass.arrUnboxInt;
    if (arrUnboxIntCopy != null) {
      long[] arrUnboxIntConv = new long[arrUnboxIntCopy.length];
      for (int i = 0 ; i < arrUnboxIntCopy.length ; i++) {
        arrUnboxIntConv[i] = arrUnboxIntCopy[i];
      }
      builder.setProperty("arrUnboxInt", arrUnboxIntConv);
    }
    Double[] arrBoxDoubleCopy = dataClass.arrBoxDouble;
    if (arrBoxDoubleCopy != null) {
      double[] arrBoxDoubleConv = new double[arrBoxDoubleCopy.length];
      for (int i = 0 ; i < arrBoxDoubleCopy.length ; i++) {
        arrBoxDoubleConv[i] = arrBoxDoubleCopy[i];
      }
      builder.setProperty("arrBoxDouble", arrBoxDoubleConv);
    }
    double[] arrUnboxDoubleCopy = dataClass.arrUnboxDouble;
    if (arrUnboxDoubleCopy != null) {
      builder.setProperty("arrUnboxDouble", arrUnboxDoubleCopy);
    }
    Float[] arrBoxFloatCopy = dataClass.arrBoxFloat;
    if (arrBoxFloatCopy != null) {
      double[] arrBoxFloatConv = new double[arrBoxFloatCopy.length];
      for (int i = 0 ; i < arrBoxFloatCopy.length ; i++) {
        arrBoxFloatConv[i] = arrBoxFloatCopy[i];
      }
      builder.setProperty("arrBoxFloat", arrBoxFloatConv);
    }
    float[] arrUnboxFloatCopy = dataClass.arrUnboxFloat;
    if (arrUnboxFloatCopy != null) {
      double[] arrUnboxFloatConv = new double[arrUnboxFloatCopy.length];
      for (int i = 0 ; i < arrUnboxFloatCopy.length ; i++) {
        arrUnboxFloatConv[i] = arrUnboxFloatCopy[i];
      }
      builder.setProperty("arrUnboxFloat", arrUnboxFloatConv);
    }
    Boolean[] arrBoxBooleanCopy = dataClass.arrBoxBoolean;
    if (arrBoxBooleanCopy != null) {
      boolean[] arrBoxBooleanConv = new boolean[arrBoxBooleanCopy.length];
      for (int i = 0 ; i < arrBoxBooleanCopy.length ; i++) {
        arrBoxBooleanConv[i] = arrBoxBooleanCopy[i];
      }
      builder.setProperty("arrBoxBoolean", arrBoxBooleanConv);
    }
    boolean[] arrUnboxBooleanCopy = dataClass.arrUnboxBoolean;
    if (arrUnboxBooleanCopy != null) {
      builder.setProperty("arrUnboxBoolean", arrUnboxBooleanCopy);
    }
    byte[][] arrUnboxByteArrCopy = dataClass.arrUnboxByteArr;
    if (arrUnboxByteArrCopy != null) {
      builder.setProperty("arrUnboxByteArr", arrUnboxByteArrCopy);
    }
    Byte[] boxByteArrCopy = dataClass.boxByteArr;
    if (boxByteArrCopy != null) {
      byte[] boxByteArrConv = new byte[boxByteArrCopy.length];
      for (int i = 0 ; i < boxByteArrCopy.length ; i++) {
        boxByteArrConv[i] = boxByteArrCopy[i];
      }
      builder.setProperty("boxByteArr", boxByteArrConv);
    }
    String[] arrStringCopy = dataClass.arrString;
    if (arrStringCopy != null) {
      builder.setProperty("arrString", arrStringCopy);
    }
    String stringCopy = dataClass.string;
    if (stringCopy != null) {
      builder.setProperty("string", stringCopy);
    }
    Long boxLongCopy = dataClass.boxLong;
    if (boxLongCopy != null) {
      builder.setProperty("boxLong", boxLongCopy);
    }
    builder.setProperty("unboxLong", dataClass.unboxLong);
    Integer boxIntegerCopy = dataClass.boxInteger;
    if (boxIntegerCopy != null) {
      builder.setProperty("boxInteger", boxIntegerCopy);
    }
    builder.setProperty("unboxInt", dataClass.unboxInt);
    Double boxDoubleCopy = dataClass.boxDouble;
    if (boxDoubleCopy != null) {
      builder.setProperty("boxDouble", boxDoubleCopy);
    }
    builder.setProperty("unboxDouble", dataClass.unboxDouble);
    Float boxFloatCopy = dataClass.boxFloat;
    if (boxFloatCopy != null) {
      builder.setProperty("boxFloat", boxFloatCopy);
    }
    builder.setProperty("unboxFloat", dataClass.unboxFloat);
    Boolean boxBooleanCopy = dataClass.boxBoolean;
    if (boxBooleanCopy != null) {
      builder.setProperty("boxBoolean", boxBooleanCopy);
    }
    builder.setProperty("unboxBoolean", dataClass.unboxBoolean);
    byte[] unboxByteArrCopy = dataClass.unboxByteArr;
    if (unboxByteArrCopy != null) {
      builder.setProperty("unboxByteArr", unboxByteArrCopy);
    }
    return builder.build();
  }
}
