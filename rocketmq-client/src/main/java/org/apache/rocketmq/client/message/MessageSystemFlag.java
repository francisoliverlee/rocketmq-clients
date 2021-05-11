package org.apache.rocketmq.client.message;

public class MessageSystemFlag {
    public static final int EMPTY_FLAG = 0;
    public static final int BODY_COMPRESSED_FLAG = 0x1;
    public static final int MULTI_TAGS_FLAG = 0x1 << 1;
    public static final int TRANSACTION_NOT_TYPE = 0;
    public static final int TRANSACTION_PREPARED_TYPE = 0x1 << 2;
    public static final int TRANSACTION_COMMIT_TYPE = 0x2 << 2;
    public static final int TRANSACTION_ROLLBACK_TYPE = 0x3 << 2;

    private MessageSystemFlag() {
    }

    public static int setTransactionPreparedFlag(final int flag) {
        return flag | TRANSACTION_PREPARED_TYPE;
    }

    public static int getTransactionValue(final int flag) {
        return flag & TRANSACTION_ROLLBACK_TYPE;
    }

    public static int resetTransactionValue(final int flag, final int type) {
        return (flag & (~TRANSACTION_ROLLBACK_TYPE)) | type;
    }

    public static boolean isBodyCompressed(final int flag) {
        return BODY_COMPRESSED_FLAG == (flag & BODY_COMPRESSED_FLAG);
    }

    public static int clearBodyCompressedFlag(final int flag) {
        return flag & (~BODY_COMPRESSED_FLAG);
    }
}
