// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: apache/rocketmq/v1/service.proto

package apache.rocketmq.v1;

public interface ResolveOrphanedTransactionRequestOrBuilder extends
    // @@protoc_insertion_point(interface_extends:apache.rocketmq.v1.ResolveOrphanedTransactionRequest)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <code>.apache.rocketmq.v1.Message orphaned_transactional_message = 1;</code>
   * @return Whether the orphanedTransactionalMessage field is set.
   */
  boolean hasOrphanedTransactionalMessage();
  /**
   * <code>.apache.rocketmq.v1.Message orphaned_transactional_message = 1;</code>
   * @return The orphanedTransactionalMessage.
   */
  apache.rocketmq.v1.Message getOrphanedTransactionalMessage();
  /**
   * <code>.apache.rocketmq.v1.Message orphaned_transactional_message = 1;</code>
   */
  apache.rocketmq.v1.MessageOrBuilder getOrphanedTransactionalMessageOrBuilder();

  /**
   * <code>string transaction_id = 2;</code>
   * @return The transactionId.
   */
  java.lang.String getTransactionId();
  /**
   * <code>string transaction_id = 2;</code>
   * @return The bytes for transactionId.
   */
  com.google.protobuf.ByteString
      getTransactionIdBytes();
}
