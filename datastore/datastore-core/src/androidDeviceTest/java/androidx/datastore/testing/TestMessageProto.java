/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// CHECKSTYLE:OFF Generated code
// NO CHECKED-IN PROTOBUF GENCODE
// source: test.proto
// Protobuf Java Version: 4.33.5

package androidx.datastore.testing;

@com.google.protobuf.Generated
public final class TestMessageProto {
  private TestMessageProto() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistryLite registry) {
  }
  public interface FooProtoOrBuilder extends
      // @@protoc_insertion_point(interface_extends:androidx.datastore.testing.FooProto)
      com.google.protobuf.MessageLiteOrBuilder {

    /**
     * <code>optional string text = 1;</code>
     * @return Whether the text field is set.
     */
    boolean hasText();
    /**
     * <code>optional string text = 1;</code>
     * @return The text.
     */
    java.lang.String getText();
    /**
     * <code>optional string text = 1;</code>
     * @return The bytes for text.
     */
    com.google.protobuf.ByteString
        getTextBytes();

    /**
     * <code>optional bool boolean = 2;</code>
     * @return Whether the boolean field is set.
     */
    boolean hasBoolean();
    /**
     * <code>optional bool boolean = 2;</code>
     * @return The boolean.
     */
    boolean getBoolean();

    /**
     * <code>optional int32 integer = 3;</code>
     * @return Whether the integer field is set.
     */
    boolean hasInteger();
    /**
     * <code>optional int32 integer = 3;</code>
     * @return The integer.
     */
    int getInteger();

    /**
     * <code>optional bytes bytes = 4;</code>
     * @return Whether the bytes field is set.
     */
    boolean hasBytes();
    /**
     * <code>optional bytes bytes = 4;</code>
     * @return The bytes.
     */
    com.google.protobuf.ByteString getBytes();
  }
  /**
   * Protobuf type {@code androidx.datastore.testing.FooProto}
   */
  public  static final class FooProto extends
      com.google.protobuf.GeneratedMessageLite<
          FooProto, FooProto.Builder> implements
      // @@protoc_insertion_point(message_implements:androidx.datastore.testing.FooProto)
      FooProtoOrBuilder {
    private FooProto() {
      text_ = "";
      bytes_ = com.google.protobuf.ByteString.EMPTY;
    }
    private int bitField0_;
    public static final int TEXT_FIELD_NUMBER = 1;
    private java.lang.String text_;
    /**
     * <code>optional string text = 1;</code>
     * @return Whether the text field is set.
     */
    @java.lang.Override
    public boolean hasText() {
      return ((bitField0_ & 0x00000001) != 0);
    }
    /**
     * <code>optional string text = 1;</code>
     * @return The text.
     */
    @java.lang.Override
    public java.lang.String getText() {
      return text_;
    }
    /**
     * <code>optional string text = 1;</code>
     * @return The bytes for text.
     */
    @java.lang.Override
    public com.google.protobuf.ByteString
        getTextBytes() {
      return com.google.protobuf.ByteString.copyFromUtf8(text_);
    }
    /**
     * <code>optional string text = 1;</code>
     * @param value The text to set.
     */
    @java.lang.SuppressWarnings("ReturnValueIgnored")
    private void setText(
        java.lang.String value) {
      value.getClass();  // minimal bytecode null check
      bitField0_ |= 0x00000001;
      text_ = value;
    }
    /**
     * <code>optional string text = 1;</code>
     */
    private void clearText() {
      bitField0_ = (bitField0_ & ~0x00000001);
      text_ = getDefaultInstance().getText();
    }
    /**
     * <code>optional string text = 1;</code>
     * @param value The bytes for text to set.
     */
    private void setTextBytes(
        com.google.protobuf.ByteString value) {
      text_ = value.toStringUtf8();
      bitField0_ |= 0x00000001;
    }

    public static final int BOOLEAN_FIELD_NUMBER = 2;
    private boolean boolean_;
    /**
     * <code>optional bool boolean = 2;</code>
     * @return Whether the boolean field is set.
     */
    @java.lang.Override
    public boolean hasBoolean() {
      return ((bitField0_ & 0x00000002) != 0);
    }
    /**
     * <code>optional bool boolean = 2;</code>
     * @return The boolean.
     */
    @java.lang.Override
    public boolean getBoolean() {
      return boolean_;
    }
    /**
     * <code>optional bool boolean = 2;</code>
     * @param value The boolean to set.
     */
    private void setBoolean(boolean value) {
      bitField0_ |= 0x00000002;
      boolean_ = value;
    }
    /**
     * <code>optional bool boolean = 2;</code>
     */
    private void clearBoolean() {
      bitField0_ = (bitField0_ & ~0x00000002);
      boolean_ = false;
    }

    public static final int INTEGER_FIELD_NUMBER = 3;
    private int integer_;
    /**
     * <code>optional int32 integer = 3;</code>
     * @return Whether the integer field is set.
     */
    @java.lang.Override
    public boolean hasInteger() {
      return ((bitField0_ & 0x00000004) != 0);
    }
    /**
     * <code>optional int32 integer = 3;</code>
     * @return The integer.
     */
    @java.lang.Override
    public int getInteger() {
      return integer_;
    }
    /**
     * <code>optional int32 integer = 3;</code>
     * @param value The integer to set.
     */
    private void setInteger(int value) {
      bitField0_ |= 0x00000004;
      integer_ = value;
    }
    /**
     * <code>optional int32 integer = 3;</code>
     */
    private void clearInteger() {
      bitField0_ = (bitField0_ & ~0x00000004);
      integer_ = 0;
    }

    public static final int BYTES_FIELD_NUMBER = 4;
    private com.google.protobuf.ByteString bytes_;
    /**
     * <code>optional bytes bytes = 4;</code>
     * @return Whether the bytes field is set.
     */
    @java.lang.Override
    public boolean hasBytes() {
      return ((bitField0_ & 0x00000008) != 0);
    }
    /**
     * <code>optional bytes bytes = 4;</code>
     * @return The bytes.
     */
    @java.lang.Override
    public com.google.protobuf.ByteString getBytes() {
      return bytes_;
    }
    /**
     * <code>optional bytes bytes = 4;</code>
     * @param value The bytes to set.
     */
    private void setBytes(com.google.protobuf.ByteString value) {
      java.lang.Class<?> valueClass = value.getClass();
  bitField0_ |= 0x00000008;
      bytes_ = value;
    }
    /**
     * <code>optional bytes bytes = 4;</code>
     */
    private void clearBytes() {
      bitField0_ = (bitField0_ & ~0x00000008);
      bytes_ = getDefaultInstance().getBytes();
    }

    public static FooProto parseFrom(
        java.nio.ByteBuffer data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return com.google.protobuf.GeneratedMessageLite.parseFrom(
          DEFAULT_INSTANCE, data);
    }
    public static FooProto parseFrom(
        java.nio.ByteBuffer data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return com.google.protobuf.GeneratedMessageLite.parseFrom(
          DEFAULT_INSTANCE, data, extensionRegistry);
    }
    public static FooProto parseFrom(
        com.google.protobuf.ByteString data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return com.google.protobuf.GeneratedMessageLite.parseFrom(
          DEFAULT_INSTANCE, data);
    }
    public static FooProto parseFrom(
        com.google.protobuf.ByteString data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return com.google.protobuf.GeneratedMessageLite.parseFrom(
          DEFAULT_INSTANCE, data, extensionRegistry);
    }
    public static FooProto parseFrom(byte[] data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return com.google.protobuf.GeneratedMessageLite.parseFrom(
          DEFAULT_INSTANCE, data);
    }
    public static FooProto parseFrom(
        byte[] data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return com.google.protobuf.GeneratedMessageLite.parseFrom(
          DEFAULT_INSTANCE, data, extensionRegistry);
    }
    public static FooProto parseFrom(java.io.InputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageLite.parseFrom(
          DEFAULT_INSTANCE, input);
    }
    public static FooProto parseFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageLite.parseFrom(
          DEFAULT_INSTANCE, input, extensionRegistry);
    }

    public static FooProto parseDelimitedFrom(java.io.InputStream input)
        throws java.io.IOException {
      return parseDelimitedFrom(DEFAULT_INSTANCE, input);
    }

    public static FooProto parseDelimitedFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return parseDelimitedFrom(DEFAULT_INSTANCE, input, extensionRegistry);
    }
    public static FooProto parseFrom(
        com.google.protobuf.CodedInputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageLite.parseFrom(
          DEFAULT_INSTANCE, input);
    }
    public static FooProto parseFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageLite.parseFrom(
          DEFAULT_INSTANCE, input, extensionRegistry);
    }

    public static Builder newBuilder() {
      return (Builder) DEFAULT_INSTANCE.createBuilder();
    }
    public static Builder newBuilder(FooProto prototype) {
      return DEFAULT_INSTANCE.createBuilder(prototype);
    }

    /**
     * Protobuf type {@code androidx.datastore.testing.FooProto}
     */
    public static final class Builder extends
        com.google.protobuf.GeneratedMessageLite.Builder<
          FooProto, Builder> implements
        // @@protoc_insertion_point(builder_implements:androidx.datastore.testing.FooProto)
        FooProtoOrBuilder {
      // Construct using FooProto.newBuilder()
      private Builder() {
        super(DEFAULT_INSTANCE);
      }


      /**
       * <code>optional string text = 1;</code>
       * @return Whether the text field is set.
       */
      @java.lang.Override
      public boolean hasText() {
        return instance.hasText();
      }
      /**
       * <code>optional string text = 1;</code>
       * @return The text.
       */
      @java.lang.Override
      public java.lang.String getText() {
        return instance.getText();
      }
      /**
       * <code>optional string text = 1;</code>
       * @return The bytes for text.
       */
      @java.lang.Override
      public com.google.protobuf.ByteString
          getTextBytes() {
        return instance.getTextBytes();
      }
      /**
       * <code>optional string text = 1;</code>
       * @param value The text to set.
       * @return This builder for chaining.
       */
      public Builder setText(
          java.lang.String value) {
        copyOnWrite();
        instance.setText(value);
        return this;
      }
      /**
       * <code>optional string text = 1;</code>
       * @return This builder for chaining.
       */
      public Builder clearText() {
        copyOnWrite();
        instance.clearText();
        return this;
      }
      /**
       * <code>optional string text = 1;</code>
       * @param value The bytes for text to set.
       * @return This builder for chaining.
       */
      public Builder setTextBytes(
          com.google.protobuf.ByteString value) {
        copyOnWrite();
        instance.setTextBytes(value);
        return this;
      }

      /**
       * <code>optional bool boolean = 2;</code>
       * @return Whether the boolean field is set.
       */
      @java.lang.Override
      public boolean hasBoolean() {
        return instance.hasBoolean();
      }
      /**
       * <code>optional bool boolean = 2;</code>
       * @return The boolean.
       */
      @java.lang.Override
      public boolean getBoolean() {
        return instance.getBoolean();
      }
      /**
       * <code>optional bool boolean = 2;</code>
       * @param value The boolean to set.
       * @return This builder for chaining.
       */
      public Builder setBoolean(boolean value) {
        copyOnWrite();
        instance.setBoolean(value);
        return this;
      }
      /**
       * <code>optional bool boolean = 2;</code>
       * @return This builder for chaining.
       */
      public Builder clearBoolean() {
        copyOnWrite();
        instance.clearBoolean();
        return this;
      }

      /**
       * <code>optional int32 integer = 3;</code>
       * @return Whether the integer field is set.
       */
      @java.lang.Override
      public boolean hasInteger() {
        return instance.hasInteger();
      }
      /**
       * <code>optional int32 integer = 3;</code>
       * @return The integer.
       */
      @java.lang.Override
      public int getInteger() {
        return instance.getInteger();
      }
      /**
       * <code>optional int32 integer = 3;</code>
       * @param value The integer to set.
       * @return This builder for chaining.
       */
      public Builder setInteger(int value) {
        copyOnWrite();
        instance.setInteger(value);
        return this;
      }
      /**
       * <code>optional int32 integer = 3;</code>
       * @return This builder for chaining.
       */
      public Builder clearInteger() {
        copyOnWrite();
        instance.clearInteger();
        return this;
      }

      /**
       * <code>optional bytes bytes = 4;</code>
       * @return Whether the bytes field is set.
       */
      @java.lang.Override
      public boolean hasBytes() {
        return instance.hasBytes();
      }
      /**
       * <code>optional bytes bytes = 4;</code>
       * @return The bytes.
       */
      @java.lang.Override
      public com.google.protobuf.ByteString getBytes() {
        return instance.getBytes();
      }
      /**
       * <code>optional bytes bytes = 4;</code>
       * @param value The bytes to set.
       * @return This builder for chaining.
       */
      public Builder setBytes(com.google.protobuf.ByteString value) {
        copyOnWrite();
        instance.setBytes(value);
        return this;
      }
      /**
       * <code>optional bytes bytes = 4;</code>
       * @return This builder for chaining.
       */
      public Builder clearBytes() {
        copyOnWrite();
        instance.clearBytes();
        return this;
      }

      // @@protoc_insertion_point(builder_scope:androidx.datastore.testing.FooProto)
    }
    @java.lang.Override
    @java.lang.SuppressWarnings({"ThrowNull"})
    protected final java.lang.Object dynamicMethod(
        com.google.protobuf.GeneratedMessageLite.MethodToInvoke method,
        java.lang.Object arg0, java.lang.Object arg1) {
      switch (method) {
        case NEW_MUTABLE_INSTANCE: {
          return new FooProto();
        }
        case NEW_BUILDER: {
          return new Builder();
        }
        case BUILD_MESSAGE_INFO: {
            java.lang.Object[] objects = new java.lang.Object[] {
              "bitField0_",
              "text_",
              "boolean_",
              "integer_",
              "bytes_",
            };
            java.lang.String info =
                "\u0001\u0004\u0000\u0001\u0001\u0004\u0004\u0000\u0000\u0000" +
                "\u0001\u1008\u0000\u0002" +
                "\u1007\u0001\u0003\u1004\u0002\u0004\u100a\u0003";
            return newMessageInfo(DEFAULT_INSTANCE, info, objects);
        }
        case GET_DEFAULT_INSTANCE: {
          return DEFAULT_INSTANCE;
        }
        case GET_PARSER: {
          com.google.protobuf.Parser<FooProto> parser = PARSER;
          if (parser == null) {
            synchronized (FooProto.class) {
              parser = PARSER;
              if (parser == null) {
                parser =
                    new DefaultInstanceBasedParser<FooProto>(
                        DEFAULT_INSTANCE);
                PARSER = parser;
              }
            }
          }
          return parser;
        }
        case GET_MEMOIZED_IS_INITIALIZED: {
          return (byte) 1;
        }
        // SET_MEMOIZED_IS_INITIALIZED is never called for this message.
        // So it can do anything. Combine with default case for smaller codegen.
        case SET_MEMOIZED_IS_INITIALIZED:
      }
      // Should never happen. Generates tight code to throw an exception.
      throw null;
    }


    // @@protoc_insertion_point(class_scope:androidx.datastore.testing.FooProto)
    private static final FooProto DEFAULT_INSTANCE;
    static {
      FooProto defaultInstance = new FooProto();
      // New instances are implicitly immutable so no need to make
      // immutable.
      DEFAULT_INSTANCE = defaultInstance;
      com.google.protobuf.GeneratedMessageLite.registerDefaultInstance(
        FooProto.class, defaultInstance);
    }

    public static FooProto getDefaultInstance() {
      return DEFAULT_INSTANCE;
    }

    private static volatile com.google.protobuf.Parser<FooProto> PARSER;

    public static com.google.protobuf.Parser<FooProto> parser() {
      return DEFAULT_INSTANCE.getParserForType();
    }
  }


  static {
  }

  // @@protoc_insertion_point(outer_class_scope)
}
