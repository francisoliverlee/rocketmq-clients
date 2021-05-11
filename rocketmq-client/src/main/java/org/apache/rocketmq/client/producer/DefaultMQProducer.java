package org.apache.rocketmq.client.producer;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.RandomUtils;
import org.apache.rocketmq.client.constant.SystemTopic;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.exception.MQServerException;
import org.apache.rocketmq.client.exception.RemotingException;
import org.apache.rocketmq.client.impl.ClientConfig;
import org.apache.rocketmq.client.impl.producer.DefaultMQProducerImpl;
import org.apache.rocketmq.client.message.Message;
import org.apache.rocketmq.client.message.MessageBatch;
import org.apache.rocketmq.client.message.MessageQueue;

@Getter
@Setter
public class DefaultMQProducer extends ClientConfig {

    /**
     * Wrapping internal implementations for virtually all methods presented in this class.
     */
    protected final DefaultMQProducerImpl impl;

    /**
     * Just for testing or demo program
     */
    private String createTopicKey = SystemTopic.DEFAULT_TOPIC;

    /**
     * Number of queues to create per default topic.
     */
    private volatile int defaultTopicQueueNums = 4;

    /**
     * Timeout for sending messages.
     */
    private int sendMsgTimeout = 3000;

    /**
     * Compress message body threshold, namely, message body larger than 4k will be compressed on
     * default.
     */
    private int compressMsgBodyOverHowmuch = 1024 * 4;

    /**
     * Maximum number of retry to perform internally before claiming sending failure in synchronous
     * mode.
     *
     * <p>This may potentially cause message duplication which is up to application developers to
     * resolve.
     */
    private int retryTimesWhenSendFailed = 2;

    /**
     * Maximum number of retry to perform internally before claiming sending failure in asynchronous
     * mode.
     *
     * <p>This may potentially cause message duplication which is up to application developers to
     * resolve.
     */
    private int retryTimesWhenSendAsyncFailed = 2;

    /**
     * Indicate whether to retry another broker on sending failure internally.
     */
    private boolean retryAnotherBrokerWhenNotStoreOK = false;

    /**
     * Maximum allowed message size in bytes.
     */
    private int maxMessageSize = 1024 * 1024 * 4; // 4M

    /**
     * random sign for identifying echo producer
     */
    private int randomSign = RandomUtils.nextInt(0, 2147483647);

    /**
     * Indicate whether add extend unique info for producer
     */
    private boolean addExtendUniqInfo = false;

    /**
     * If topic route not found when sending message, whether use the default topic route.
     */
    private boolean useDefaultTopicIfNotFound = true;

    /**
     * Constructor specifying producer group.
     *
     * @param producerGroup Producer group, see the name-sake field.
     */
    public DefaultMQProducer(final String producerGroup) {
        this(null, producerGroup);
    }

    public DefaultMQProducer(final String namespace, final String producerGroup) {
        this.setNamespace(namespace);
        this.setGroupName(producerGroup);
        this.impl = new DefaultMQProducerImpl(this);
    }

    public String getProducerGroup() {
        return this.getGroupName();
    }

    public void setProducerGroup(String producerGroup) {
        if (impl.hasBeenStarted()) {
            throw new RuntimeException("Please set producerGroup before producer started.");
        }
        setGroupName(producerGroup);
    }

    /**
     * Start this producer instance. <strong> Much internal initializing procedures are carried out to
     * make this instance prepared, thus, it's a must to invoke this method before sending or querying
     * messages. </strong>
     *
     * @throws MQClientException if there is any unexpected error.
     */
    public void start() throws MQClientException {
        this.setGroupName(withNamespace(this.getProducerGroup()));
        this.impl.start();
    }

    /**
     * This method shuts down this producer instance and releases related resources.
     */
    public void shutdown() throws MQClientException {
        this.impl.shutdown();
    }

    /**
     * Fetch message queues of topic <code>topic</code>, to which we may send/publish messages.
     *
     * @param topic Topic to fetch.
     * @return List of message queues readily to send messages to
     * @throws MQClientException if there is any client error.
     */
    public List<MessageQueue> fetchPublishMessageQueues(String topic) throws MQClientException {
        return this.impl.fetchPublishMessageQueues(withNamespace(topic));
    }

    /**
     * Send message in synchronous mode. This method returns only when the sending procedure totally
     * completes. <strong>Warn:</strong> this method has internal retry-mechanism, that is, internal
     * implementation will retry {@link #retryTimesWhenSendFailed} times before claiming failure. As a
     * result, multiple messages may potentially delivered to broker(s). It's up to the application
     * developers to resolve potential duplication issue.
     *
     * @param msg Message to send.
     * @return {@link SendResult} instance to inform senders details of the deliverable, say Message
     * ID of the message, {@link SendStatus} indicating broker storage/replication status, message
     * queue sent to, etc.
     * @throws MQClientException    if there is any client error.
     * @throws RemotingException    if there is any network-tier error.
     * @throws MQBrokerException    if there is any error with broker.
     * @throws InterruptedException if the sending thread is interrupted.
     */
    public SendResult send(Message msg)
            throws MQClientException, RemotingException, MQBrokerException, InterruptedException {
        msg.setTopic(withNamespace(msg.getTopic()));
        return this.impl.send(msg);
    }

    /**
     * Same to {@link #send(Message)} with send timeout specified in addition.
     *
     * @param msg     Message to send.
     * @param timeout send timeout.
     * @return {@link SendResult} instance to inform senders details of the deliverable, say Message
     * ID of the message, {@link SendStatus} indicating broker storage/replication status, message
     * queue sent to, etc.
     * @throws MQClientException    if there is any client error.
     * @throws RemotingException    if there is any network-tier error.
     * @throws MQBrokerException    if there is any error with broker.
     * @throws InterruptedException if the sending thread is interrupted.
     */
    public SendResult send(Message msg, long timeout)
            throws MQClientException, RemotingException, MQBrokerException, InterruptedException {
        msg.setTopic(withNamespace(msg.getTopic()));
        return this.impl.send(msg, timeout);
    }

    /**
     * Send message to broker asynchronously.
     *
     * <p>This method returns immediately. On sending completion, <code>sendCallback</code> will be
     * executed.
     *
     * <p>Similar to {@link #send(Message)}, internal implementation would potentially retry up to
     * {@link #retryTimesWhenSendAsyncFailed} times before claiming sending failure, which may yield
     * message duplication and application developers are the one to resolve this potential issue.
     *
     * @param msg          Message to send.
     * @param sendCallback Callback to execute on sending completed, either successful or
     *                     unsuccessful.
     * @throws MQClientException    if there is any client error.
     * @throws RemotingException    if there is any network-tier error.
     * @throws InterruptedException if the sending thread is interrupted.
     */
    public void send(Message msg, SendCallback sendCallback)
            throws MQClientException, RemotingException, InterruptedException {
        msg.setTopic(withNamespace(msg.getTopic()));
        this.impl.send(msg, sendCallback);
    }

    /**
     * Same to {@link #send(Message, SendCallback)} with send timeout specified in addition.
     *
     * @param msg          message to send.
     * @param sendCallback Callback to execute.
     * @param timeout      send timeout.
     * @throws MQClientException    if there is any client error.
     * @throws RemotingException    if there is any network-tier error.
     * @throws InterruptedException if the sending thread is interrupted.
     */
    public void send(Message msg, SendCallback sendCallback, long timeout)
            throws MQClientException, RemotingException, InterruptedException {
        msg.setTopic(withNamespace(msg.getTopic()));
        this.impl.send(msg, sendCallback, timeout);
    }

    /**
     * Similar to <a href="https://en.wikipedia.org/wiki/User_Datagram_Protocol">UDP</a>, this method
     * won't wait for acknowledgement from broker before return. Obviously, it has maximums throughput
     * yet potentials of message loss.
     *
     * @param msg Message to send.
     * @throws MQClientException    if there is any client error.
     * @throws RemotingException    if there is any network-tier error.
     * @throws InterruptedException if the sending thread is interrupted.
     */
    public void sendOneway(Message msg)
            throws MQClientException, RemotingException, InterruptedException {
        msg.setTopic(withNamespace(msg.getTopic()));
        this.impl.sendOneway(msg);
    }

    /**
     * Same to {@link #send(Message)} with target message queue specified in addition.
     *
     * @param msg Message to send.
     * @param mq  Target message queue.
     * @return {@link SendResult} instance to inform senders details of the deliverable, say Message
     * ID of the message, {@link SendStatus} indicating broker storage/replication status, message
     * queue sent to, etc.
     * @throws MQClientException    if there is any client error.
     * @throws RemotingException    if there is any network-tier error.
     * @throws MQBrokerException    if there is any error with broker.
     * @throws InterruptedException if the sending thread is interrupted.
     */
    public SendResult send(Message msg, MessageQueue mq)
            throws MQClientException, RemotingException, MQBrokerException, InterruptedException,
                   MQServerException {
        msg.setTopic(withNamespace(msg.getTopic()));
        return this.impl.send(msg, queueWrapWithNamespace(mq));
    }

    /**
     * Same to {@link #send(Message)} with target message queue and send timeout specified.
     *
     * @param msg     Message to send.
     * @param mq      Target message queue.
     * @param timeout send timeout.
     * @return {@link SendResult} instance to inform senders details of the deliverable, say Message
     * ID of the message, {@link SendStatus} indicating broker storage/replication status, message
     * queue sent to, etc.
     * @throws MQClientException    if there is any client error.
     * @throws RemotingException    if there is any network-tier error.
     * @throws MQBrokerException    if there is any error with broker.
     * @throws InterruptedException if the sending thread is interrupted.
     */
    public SendResult send(Message msg, MessageQueue mq, long timeout)
            throws MQClientException, RemotingException, MQBrokerException, InterruptedException,
                   MQServerException {
        msg.setTopic(withNamespace(msg.getTopic()));
        return this.impl.send(msg, queueWrapWithNamespace(mq), timeout);
    }

    /**
     * Same to {@link #send(Message, SendCallback)} with target message queue specified.
     *
     * @param msg          Message to send.
     * @param mq           Target message queue.
     * @param sendCallback Callback to execute on sending completed, either successful or
     *                     unsuccessful.
     * @throws MQClientException    if there is any client error.
     * @throws RemotingException    if there is any network-tier error.
     * @throws InterruptedException if the sending thread is interrupted.
     */
    public void send(Message msg, MessageQueue mq, SendCallback sendCallback)
            throws MQClientException, RemotingException, InterruptedException, MQServerException {
        msg.setTopic(withNamespace(msg.getTopic()));
        this.impl.send(msg, queueWrapWithNamespace(mq), sendCallback);
    }

    /**
     * Same to {@link #send(Message, SendCallback)} with target message queue and send timeout
     * specified.
     *
     * @param msg          Message to send.
     * @param mq           Target message queue.
     * @param sendCallback Callback to execute on sending completed, either successful or
     *                     unsuccessful.
     * @param timeout      Send timeout.
     * @throws MQClientException    if there is any client error.
     * @throws RemotingException    if there is any network-tier error.
     * @throws InterruptedException if the sending thread is interrupted.
     */
    public void send(Message msg, MessageQueue mq, SendCallback sendCallback, long timeout)
            throws MQClientException, RemotingException, InterruptedException, MQServerException {
        msg.setTopic(withNamespace(msg.getTopic()));
        this.impl.send(msg, queueWrapWithNamespace(mq), sendCallback, timeout);
    }

    /**
     * Same to {@link #sendOneway(Message)} with target message queue specified.
     *
     * @param msg Message to send.
     * @param mq  Target message queue.
     * @throws MQClientException    if there is any client error.
     * @throws RemotingException    if there is any network-tier error.
     * @throws InterruptedException if the sending thread is interrupted.
     */
    public void sendOneway(Message msg, MessageQueue mq)
            throws MQClientException, RemotingException, InterruptedException, MQServerException {
        msg.setTopic(withNamespace(msg.getTopic()));
        this.impl.sendOneway(msg, queueWrapWithNamespace(mq));
    }

    /**
     * Same to {@link #send(Message)} with message queue selector specified.
     *
     * @param msg      Message to send.
     * @param selector Message queue selector, through which we get target message queue to deliver
     *                 message to.
     * @param arg      Argument to work along with message queue selector.
     * @return {@link SendResult} instance to inform senders details of the deliverable, say Message
     * ID of the message, {@link SendStatus} indicating broker storage/replication status, message
     * queue sent to, etc.
     * @throws MQClientException    if there is any client error.
     * @throws RemotingException    if there is any network-tier error.
     * @throws MQBrokerException    if there is any error with broker.
     * @throws InterruptedException if the sending thread is interrupted.
     */
    public SendResult send(Message msg, MessageQueueSelector selector, Object arg)
            throws MQClientException, RemotingException, MQBrokerException, InterruptedException {
        msg.setTopic(withNamespace(msg.getTopic()));
        return this.impl.send(msg, selector, arg);
    }

    /**
     * Same to {@link #send(Message, MessageQueueSelector, Object)} with send timeout specified.
     *
     * @param msg      Message to send.
     * @param selector Message queue selector, through which we get target message queue to deliver
     *                 message to.
     * @param arg      Argument to work along with message queue selector.
     * @param timeout  Send timeout.
     * @return {@link SendResult} instance to inform senders details of the deliverable, say Message
     * ID of the message, {@link SendStatus} indicating broker storage/replication status, message
     * queue sent to, etc.
     * @throws MQClientException    if there is any client error.
     * @throws RemotingException    if there is any network-tier error.
     * @throws MQBrokerException    if there is any error with broker.
     * @throws InterruptedException if the sending thread is interrupted.
     */
    public SendResult send(Message msg, MessageQueueSelector selector, Object arg, long timeout)
            throws MQClientException, RemotingException, MQBrokerException, InterruptedException {
        msg.setTopic(withNamespace(msg.getTopic()));
        return this.impl.send(msg, selector, arg, timeout);
    }

    /**
     * Same to {@link #send(Message, SendCallback)} with message queue selector specified.
     *
     * @param msg          Message to send.
     * @param selector     Message selector through which to get target message queue.
     * @param arg          Argument used along with message queue selector.
     * @param sendCallback callback to execute on sending completion.
     * @throws MQClientException    if there is any client error.
     * @throws RemotingException    if there is any network-tier error.
     * @throws InterruptedException if the sending thread is interrupted.
     */
    public void send(
            Message msg, MessageQueueSelector selector, Object arg, SendCallback sendCallback)
            throws MQClientException, RemotingException, InterruptedException {
        msg.setTopic(withNamespace(msg.getTopic()));
        this.impl.send(msg, selector, arg, sendCallback);
    }

    /**
     * Same to {@link #send(Message, MessageQueueSelector, Object, SendCallback)} with timeout
     * specified.
     *
     * @param msg          Message to send.
     * @param selector     Message selector through which to get target message queue.
     * @param arg          Argument used along with message queue selector.
     * @param sendCallback callback to execute on sending completion.
     * @param timeout      Send timeout.
     * @throws MQClientException    if there is any client error.
     * @throws RemotingException    if there is any network-tier error.
     * @throws InterruptedException if the sending thread is interrupted.
     */
    public void send(
            Message msg,
            MessageQueueSelector selector,
            Object arg,
            SendCallback sendCallback,
            long timeout)
            throws MQClientException, RemotingException, InterruptedException {
        msg.setTopic(withNamespace(msg.getTopic()));
        this.impl.send(msg, selector, arg, sendCallback, timeout);
    }

    /**
     * Same to {@link #sendOneway(Message)} with message queue selector specified.
     *
     * @param msg      Message to send.
     * @param selector Message queue selector, through which to determine target message queue to
     *                 deliver message
     * @param arg      Argument used along with message queue selector.
     * @throws MQClientException    if there is any client error.
     * @throws RemotingException    if there is any network-tier error.
     * @throws InterruptedException if the sending thread is interrupted.
     */
    public void sendOneway(Message msg, MessageQueueSelector selector, Object arg)
            throws MQClientException, RemotingException, InterruptedException {
        msg.setTopic(withNamespace(msg.getTopic()));
        this.impl.sendOneway(msg, selector, arg);
    }

    /**
     * This method is to send transactional messages.
     *
     * @param msg          Transactional message to send.
     * @param tranExecuter local transaction executor.
     * @param arg          Argument used along with local transaction executor.
     * @return Transaction result.
     * @throws MQClientException if there is any client error.
     */
    public TransactionSendResult sendMessageInTransaction(
            Message msg, LocalTransactionExecutor tranExecuter, final Object arg)
            throws MQClientException {
        throw new RuntimeException(
                "sendMessageInTransaction not implement, please use TransactionMQProducer class");
    }

    public SendResult send(Collection<Message> msgs)
            throws MQClientException, RemotingException, MQBrokerException, InterruptedException {
        return this.impl.send(batch(msgs));
    }

    public SendResult send(Collection<Message> msgs, long timeout)
            throws MQClientException, RemotingException, MQBrokerException, InterruptedException {
        return this.impl.send(batch(msgs), timeout);
    }

    public void send(Collection<Message> msgs, SendCallback sendCallback, long timeout)
            throws MQClientException, RemotingException, MQBrokerException, InterruptedException {
        this.impl.send(batch(msgs), sendCallback, timeout);
    }

    public SendResult send(Collection<Message> msgs, MessageQueue messageQueue)
            throws MQClientException, RemotingException, MQBrokerException, InterruptedException,
                   MQServerException {
        return this.impl.send(batch(msgs), messageQueue);
    }

    public SendResult send(Collection<Message> msgs, MessageQueue messageQueue, long timeout)
            throws MQClientException, RemotingException, MQBrokerException, InterruptedException,
                   MQServerException {
        return this.impl.send(batch(msgs), messageQueue, timeout);
    }

    public void send(
            Collection<Message> msgs, MessageQueue mq, SendCallback sendCallback, long timeout)
            throws MQClientException, RemotingException, MQBrokerException, InterruptedException,
                   MQServerException {
        this.impl.send(batch(msgs), mq, sendCallback, timeout);
    }

    /**
     * Sets an Executor to be used for executing callback methods. If the Executor is not set, will be
     * used.
     *
     * @param callbackExecutor the instance of Executor
     */
    public void setCallbackExecutor(final ExecutorService callbackExecutor) {
        this.impl.setCallbackExecutor(callbackExecutor);
    }

    // Not yet implemented.
    private MessageBatch batch(Collection<Message> msgs) throws MQClientException {
        return null;
    }
}
