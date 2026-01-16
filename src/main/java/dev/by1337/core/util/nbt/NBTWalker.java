package dev.by1337.core.util.nbt;

public interface NBTWalker {
    byte TAG_BYTE = 1;
    byte TAG_SHORT = 2;
    byte TAG_INT = 3;
    byte TAG_LONG = 4;
    byte TAG_FLOAT = 5;
    byte TAG_DOUBLE = 6;
    byte TAG_BYTE_ARRAY = 7;
    byte TAG_STRING = 8;
    byte TAG_LIST = 9;
    byte TAG_COMPOUND = 10;
    byte TAG_INT_ARRAY = 11;
    byte TAG_LONG_ARRAY = 12;

    boolean isInList();
    boolean isInCompound();

    boolean hasNext();

    String currentKey();

    /**
     * Contract for LIST:
     * - Before first next(): currentType() returns declared element type
     * - After next(): currentType() returns actual element type
     */
    int currentType();

    boolean enterCompound();

    boolean enterList();

    void exit();

    byte asByte();

    short asShort();

    int asInt();

    long asLong();

    float asFloat();

    double asDouble();

    byte[] asByteArray();

    int[] asIntArray();

    long[] asLongArray();

    String asString();

    boolean next();
}
