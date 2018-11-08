package io.lingpai.tutor.p2p;

import java.net.SocketException;
import java.net.UnknownHostException;

//一个节点抽象出来就是接收和发送消息2个功能
public interface INode {
    void addMessageListener(MessageListener listener) throws SocketException, UnknownHostException;
    void sendMessage(IMessage message) throws InterruptedException;
    void start() throws InterruptedException, SocketException, UnknownHostException;
    void stop();
}
