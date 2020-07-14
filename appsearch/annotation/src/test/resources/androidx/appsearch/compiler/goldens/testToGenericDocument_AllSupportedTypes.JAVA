package com.example.appsearch;

import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.DataClassFactory;
import androidx.appsearch.app.GenericDocument;
import java.lang.Boolean;
import java.lang.Byte;
import java.lang.Double;
import java.lang.Float;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Override;
import java.lang.String;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class $$__AppSearch__Gift implements DataClassFactory<Gift> {
  private static final String SCHEMA_TYPE = "Gift";

  private static final AppSearchSchema SCHEMA = new AppSearchSchema.Builder(SCHEMA_TYPE)
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
      .setDataType(AppSearchSchema.PropertyConfig.DATA_TYPE_DOCUMENT)
      .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
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

  @Override
  public String getSchemaType() {
    return SCHEMA_TYPE;
  }

  @Override
  public AppSearchSchema getSchema() {
    return SCHEMA;
  }

  @Override
  public GenericDocument toGenericDocument(Gift dataClass) {
    GenericDocument.Builder<?> builder =
        new GenericDocument.Builder<>(dataClass.uri, SCHEMA_TYPE);
    Collection<Long> collectLongCopy = dataClass.collectLong;
    if (collectLongCopy != null) {
      long[] collectLongConv = new long[collectLongCopy.size()];
      int i = 0;
      for (Long item : collectLongCopy) {
        collectLongConv[i++] = item;
      }
      builder.setProperty("collectLong", collectLongConv);
    }
    Collection<Integer> collectIntegerCopy = dataClass.collectInteger;
    if (collectIntegerCopy != null) {
      long[] collectIntegerConv = new long[collectIntegerCopy.size()];
      int i = 0;
      for (Integer item : collectIntegerCopy) {
        collectIntegerConv[i++] = item;
      }
      builder.setProperty("collectInteger", collectIntegerConv);
    }
    Collection<Double> collectDoubleCopy = dataClass.collectDouble;
    if (collectDoubleCopy != null) {
      double[] collectDoubleConv = new double[collectDoubleCopy.size()];
      int i = 0;
      for (Double item : collectDoubleCopy) {
        collectDoubleConv[i++] = item;
      }
      builder.setProperty("collectDouble", collectDoubleConv);
    }
    Collection<Float> collectFloatCopy = dataClass.collectFloat;
    if (collectFloatCopy != null) {
      double[] collectFloatConv = new double[collectFloatCopy.size()];
      int i = 0;
      for (Float item : collectFloatCopy) {
        collectFloatConv[i++] = item;
      }
      builder.setProperty("collectFloat", collectFloatConv);
    }
    Collection<Boolean> collectBooleanCopy = dataClass.collectBoolean;
    if (collectBooleanCopy != null) {
      boolean[] collectBooleanConv = new boolean[collectBooleanCopy.size()];
      int i = 0;
      for (Boolean item : collectBooleanCopy) {
        collectBooleanConv[i++] = item;
      }
      builder.setProperty("collectBoolean", collectBooleanConv);
    }
    Collection<byte[]> collectByteArrCopy = dataClass.collectByteArr;
    if (collectByteArrCopy != null) {
      byte[][] collectByteArrConv = new byte[collectByteArrCopy.size()][];
      int i = 0;
      for (byte[] item : collectByteArrCopy) {
        collectByteArrConv[i++] = item;
      }
      builder.setProperty("collectByteArr", collectByteArrConv);
    }
    Collection<String> collectStringCopy = dataClass.collectString;
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

  @Override
  public Gift fromGenericDocument(GenericDocument genericDoc) {
    String uriConv = genericDoc.getUri();
    long[] collectLongCopy = genericDoc.getPropertyLongArray("collectLong");
    List<Long> collectLongConv = null;
    if (collectLongCopy != null) {
      collectLongConv = new ArrayList<>(collectLongCopy.length);
      for (int i = 0; i < collectLongCopy.length; i++) {
        collectLongConv.set(i, collectLongCopy[i]);
      }
    }
    long[] collectIntegerCopy = genericDoc.getPropertyLongArray("collectInteger");
    List<Integer> collectIntegerConv = null;
    if (collectIntegerCopy != null) {
      collectIntegerConv = new ArrayList<>(collectIntegerCopy.length);
      for (int i = 0; i < collectIntegerCopy.length; i++) {
        collectIntegerConv.set(i, collectIntegerCopy[i]);
      }
    }
    double[] collectDoubleCopy = genericDoc.getPropertyDoubleArray("collectDouble");
    List<Double> collectDoubleConv = null;
    if (collectDoubleCopy != null) {
      collectDoubleConv = new ArrayList<>(collectDoubleCopy.length);
      for (int i = 0; i < collectDoubleCopy.length; i++) {
        collectDoubleConv.set(i, collectDoubleCopy[i]);
      }
    }
    double[] collectFloatCopy = genericDoc.getPropertyDoubleArray("collectFloat");
    List<Float> collectFloatConv = null;
    if (collectFloatCopy != null) {
      collectFloatConv = new ArrayList<>(collectFloatCopy.length);
      for (int i = 0; i < collectFloatCopy.length; i++) {
        collectFloatConv.set(i, collectFloatCopy[i]);
      }
    }
    boolean[] collectBooleanCopy = genericDoc.getPropertyBooleanArray("collectBoolean");
    List<Boolean> collectBooleanConv = null;
    if (collectBooleanCopy != null) {
      collectBooleanConv = new ArrayList<>(collectBooleanCopy.length);
      for (int i = 0; i < collectBooleanCopy.length; i++) {
        collectBooleanConv.set(i, collectBooleanCopy[i]);
      }
    }
    byte[][] collectByteArrCopy = genericDoc.getPropertyBytesArray("collectByteArr");
    List<byte[]> collectByteArrConv = null;
    if (collectByteArrCopy != null) {
      collectByteArrConv = new ArrayList<>(collectByteArrCopy.length);
      for (int i = 0; i < collectByteArrCopy.length; i++) {
        collectByteArrConv.set(i, collectByteArrCopy[i]);
      }
    }
    String[] collectStringCopy = genericDoc.getPropertyStringArray("collectString");
    List<String> collectStringConv = null;
    if (collectStringCopy != null) {
      collectStringConv = Arrays.asList(collectStringCopy);
    }
    long[] arrBoxLongCopy = genericDoc.getPropertyLongArray("arrBoxLong");
    Long[] arrBoxLongConv = null;
    if (arrBoxLongCopy != null) {
      arrBoxLongConv = new Long[arrBoxLongCopy.length];
      for (int i = 0; i < arrBoxLongCopy.length; i++) {
        arrBoxLongConv[i] = arrBoxLongCopy[i];
      }
    }
    long[] arrUnboxLongConv = genericDoc.getPropertyLongArray("arrUnboxLong");
    long[] arrBoxIntegerCopy = genericDoc.getPropertyLongArray("arrBoxInteger");
    Integer[] arrBoxIntegerConv = null;
    if (arrBoxIntegerCopy != null) {
      arrBoxIntegerConv = new Integer[arrBoxIntegerCopy.length];
      for (int i = 0; i < arrBoxIntegerCopy.length; i++) {
        arrBoxIntegerConv[i] = arrBoxIntegerCopy[i];
      }
    }
    long[] arrUnboxIntCopy = genericDoc.getPropertyLongArray("arrUnboxInt");
    int[] arrUnboxIntConv = null;
    if (arrUnboxIntCopy != null) {
      arrUnboxIntConv = new int[arrUnboxIntCopy.length];
      for (int i = 0; i < arrUnboxIntCopy.length; i++) {
        arrUnboxIntConv[i] = arrUnboxIntCopy[i];
      }
    }
    double[] arrBoxDoubleCopy = genericDoc.getPropertyDoubleArray("arrBoxDouble");
    Double[] arrBoxDoubleConv = null;
    if (arrBoxDoubleCopy != null) {
      arrBoxDoubleConv = new Double[arrBoxDoubleCopy.length];
      for (int i = 0; i < arrBoxDoubleCopy.length; i++) {
        arrBoxDoubleConv[i] = arrBoxDoubleCopy[i];
      }
    }
    double[] arrUnboxDoubleConv = genericDoc.getPropertyDoubleArray("arrUnboxDouble");
    double[] arrBoxFloatCopy = genericDoc.getPropertyDoubleArray("arrBoxFloat");
    Float[] arrBoxFloatConv = null;
    if (arrBoxFloatCopy != null) {
      arrBoxFloatConv = new Float[arrBoxFloatCopy.length];
      for (int i = 0; i < arrBoxFloatCopy.length; i++) {
        arrBoxFloatConv[i] = arrBoxFloatCopy[i];
      }
    }
    double[] arrUnboxFloatCopy = genericDoc.getPropertyDoubleArray("arrUnboxFloat");
    float[] arrUnboxFloatConv = null;
    if (arrUnboxFloatCopy != null) {
      arrUnboxFloatConv = new float[arrUnboxFloatCopy.length];
      for (int i = 0; i < arrUnboxFloatCopy.length; i++) {
        arrUnboxFloatConv[i] = arrUnboxFloatCopy[i];
      }
    }
    boolean[] arrBoxBooleanCopy = genericDoc.getPropertyBooleanArray("arrBoxBoolean");
    Boolean[] arrBoxBooleanConv = null;
    if (arrBoxBooleanCopy != null) {
      arrBoxBooleanConv = new Boolean[arrBoxBooleanCopy.length];
      for (int i = 0; i < arrBoxBooleanCopy.length; i++) {
        arrBoxBooleanConv[i] = arrBoxBooleanCopy[i];
      }
    }
    boolean[] arrUnboxBooleanConv = genericDoc.getPropertyBooleanArray("arrUnboxBoolean");
    byte[][] arrUnboxByteArrConv = genericDoc.getPropertyBytesArray("arrUnboxByteArr");
    byte[][] boxByteArrCopy = genericDoc.getPropertyBytesArray("boxByteArr");
    Byte[] boxByteArrConv = null;
    if (boxByteArrCopy != null) {
      boxByteArrConv = new Byte[boxByteArrCopy.length];
      for (int i = 0; i < boxByteArrCopy.length; i++) {
        boxByteArrConv[i] = boxByteArrCopy[i];
      }
    }
    String[] arrStringConv = genericDoc.getPropertyStringArray("arrString");
    String[] stringCopy = genericDoc.getPropertyStringArray("string");
    String stringConv = null;
    if (stringCopy != null && stringCopy.length != 0) {
      stringConv = stringCopy[0];
    }
    long[] boxLongCopy = genericDoc.getPropertyLongArray("boxLong");
    Long boxLongConv = null;
    if (boxLongCopy != null && boxLongCopy.length != 0) {
      boxLongConv = boxLongCopy[0];
    }
    long unboxLongConv = genericDoc.getPropertyLong("unboxLong");
    long[] boxIntegerCopy = genericDoc.getPropertyLongArray("boxInteger");
    Integer boxIntegerConv = null;
    if (boxIntegerCopy != null && boxIntegerCopy.length != 0) {
      boxIntegerConv = boxIntegerCopy[0];
    }
    int unboxIntConv = genericDoc.getPropertyLong("unboxInt");
    double[] boxDoubleCopy = genericDoc.getPropertyDoubleArray("boxDouble");
    Double boxDoubleConv = null;
    if (boxDoubleCopy != null && boxDoubleCopy.length != 0) {
      boxDoubleConv = boxDoubleCopy[0];
    }
    double unboxDoubleConv = genericDoc.getPropertyDouble("unboxDouble");
    double[] boxFloatCopy = genericDoc.getPropertyDoubleArray("boxFloat");
    Float boxFloatConv = null;
    if (boxFloatCopy != null && boxFloatCopy.length != 0) {
      boxFloatConv = boxFloatCopy[0];
    }
    float unboxFloatConv = genericDoc.getPropertyDouble("unboxFloat");
    boolean[] boxBooleanCopy = genericDoc.getPropertyBooleanArray("boxBoolean");
    Boolean boxBooleanConv = null;
    if (boxBooleanCopy != null && boxBooleanCopy.length != 0) {
      boxBooleanConv = boxBooleanCopy[0];
    }
    boolean unboxBooleanConv = genericDoc.getPropertyBoolean("unboxBoolean");
    byte[][] unboxByteArrCopy = genericDoc.getPropertyBytesArray("unboxByteArr");
    byte[] unboxByteArrConv = null;
    if (unboxByteArrCopy != null && unboxByteArrCopy.length != 0) {
      unboxByteArrConv = unboxByteArrCopy[0];
    }
    Gift dataClass = new Gift();
    dataClass.uri = uriConv;
    dataClass.collectLong = collectLongConv;
    dataClass.collectInteger = collectIntegerConv;
    dataClass.collectDouble = collectDoubleConv;
    dataClass.collectFloat = collectFloatConv;
    dataClass.collectBoolean = collectBooleanConv;
    dataClass.collectByteArr = collectByteArrConv;
    dataClass.collectString = collectStringConv;
    dataClass.arrBoxLong = arrBoxLongConv;
    dataClass.arrUnboxLong = arrUnboxLongConv;
    dataClass.arrBoxInteger = arrBoxIntegerConv;
    dataClass.arrUnboxInt = arrUnboxIntConv;
    dataClass.arrBoxDouble = arrBoxDoubleConv;
    dataClass.arrUnboxDouble = arrUnboxDoubleConv;
    dataClass.arrBoxFloat = arrBoxFloatConv;
    dataClass.arrUnboxFloat = arrUnboxFloatConv;
    dataClass.arrBoxBoolean = arrBoxBooleanConv;
    dataClass.arrUnboxBoolean = arrUnboxBooleanConv;
    dataClass.arrUnboxByteArr = arrUnboxByteArrConv;
    dataClass.boxByteArr = boxByteArrConv;
    dataClass.arrString = arrStringConv;
    dataClass.string = stringConv;
    dataClass.boxLong = boxLongConv;
    dataClass.unboxLong = unboxLongConv;
    dataClass.boxInteger = boxIntegerConv;
    dataClass.unboxInt = unboxIntConv;
    dataClass.boxDouble = boxDoubleConv;
    dataClass.unboxDouble = unboxDoubleConv;
    dataClass.boxFloat = boxFloatConv;
    dataClass.unboxFloat = unboxFloatConv;
    dataClass.boxBoolean = boxBooleanConv;
    dataClass.unboxBoolean = unboxBooleanConv;
    dataClass.unboxByteArr = unboxByteArrConv;
    return dataClass;
  }
}
