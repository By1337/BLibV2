package dev.by1337.core.util.nbt;

public abstract class NBTStream implements NBTStreamWriter {

    @Override
    public void accept(NBTWalker walker) {
        if (walker.isInList()) {
            acceptList(walker);
        } else if (walker.isInCompound()) {
            acceptCompound(walker);
        } else {
            acceptUnnamedTag(walker);
        }
    }

    public void acceptCompound(NBTWalker walker) {
        pushObject();
        while (walker.next()) {
            pushKey(walker.currentKey());
            acceptUnnamedTag(walker);
        }
        popObject();
    }

    public void acceptList(NBTWalker walker) {
        pushList(walker.currentType());
        while (walker.next()) {
            acceptUnnamedTag(walker);
        }
        popList();
    }

    public void acceptUnnamedTag(NBTWalker walker) {
        switch (walker.currentType()) {
            case TAG_BYTE -> pushByte(walker.asByte());
            case TAG_SHORT -> pushShort(walker.asShort());
            case TAG_INT -> pushInt(walker.asInt());
            case TAG_LONG -> pushLong(walker.asLong());
            case TAG_FLOAT -> pushFloat(walker.asFloat());
            case TAG_DOUBLE -> pushDouble(walker.asDouble());
            case TAG_BYTE_ARRAY -> pushByteArray(walker.asByteArray());
            case TAG_STRING -> pushString(walker.asString());
            case TAG_INT_ARRAY -> pushIntArray(walker.asIntArray());
            case TAG_LONG_ARRAY -> pushLongArray(walker.asLongArray());
            case TAG_COMPOUND -> {
                if (walker.enterCompound()) {
                    acceptCompound(walker);
                    walker.exit();
                }
            }
            case TAG_LIST -> {
                if (walker.enterList()) {
                    acceptList(walker);
                    walker.exit();
                }
            }
        }
    }
}
