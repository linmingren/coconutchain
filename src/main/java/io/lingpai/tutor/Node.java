package io.lingpai.tutor;

import io.lingpai.tutor.p2p.*;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.InternetProtocolFamily;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;

import java.net.*;
import java.util.List;


//p2p节点实现
@Slf4j
public class Node implements INode {
    private Sender sender;
    private Receiver receiver;
    private String multicastHost;
    private int port;

    public Node(String multicastHost, int port) throws SocketException, UnknownHostException, InterruptedException {
        sender = new Sender(new InetSocketAddress(multicastHost,
                        port));
        this.multicastHost = multicastHost;
        this.port = port;
    }

    @Override
    public void addMessageListener(MessageListener listener) throws SocketException, UnknownHostException {
        //在OSX上通过 echo -n "1:{\"height\":1}" | netcat -u 127.0.0.1(或者) 1111 来发送消息
        receiver = new Receiver(multicastHost, port, listener);
    }

    @Override
    public void sendMessage(IMessage message) throws InterruptedException {
        sender.sendMessage(message);
    }

    @Override
    public void start() throws InterruptedException, SocketException, UnknownHostException {
        if (receiver == null) {
            log.error("Node is not running since no message listener is configured");
            return;
        }
        receiver.bind();
        log.info("Node is running");
    }

    @Override
    public void stop() {
        receiver.stop();
        sender.stop();
    }

    /**
     * 当其它节点挖出新的区块时，会从广播消息中收到新的区块
     * 同样， 如果当前节点挖出新区块，需要自己广播出去
     * 主要的流程有:
     * 1. 发出查询所有区块的消息
     * 2. 收到区块列表后，和自己内部的区块链比较，如果外部区块高度大，则使用外部的链， 如果区块高度一样，则比较最后一个块的哈希值的大小，哈希值大的链保留
     * 3. 当本节点挖出新区块后，广播出去
     * 4. 当本节点有新的交易时，广播出去
     * 5. 当收到其它节点的区块时，先比较高度，高度大的保留。如果高度一样，则区块哈希值大的保留。
     * 6. 当收到其它节点的交易时，保存到内部的未完成交易池中
     **/


    private static class BlockMessageEncoder extends MessageToMessageEncoder<IMessage> {
        private final InetSocketAddress remoteAddress;

        public BlockMessageEncoder(InetSocketAddress remoteAddress) {
            this.remoteAddress = remoteAddress;
        }

        @Override
        protected void encode(ChannelHandlerContext channelHandlerContext,
                              IMessage message, List<Object> out) throws Exception {

            ByteBuf buf = channelHandlerContext.alloc()
                    .buffer(1 + message.getBody().length);
            //消息的第一个字节是消息类型
            buf.writeByte(message.getType());
            //类型后面的就是实际数据
            buf.writeBytes(message.getBody());
            out.add(new DatagramPacket(buf, remoteAddress));
        }
    }


    public static class BlockMessageDecoder extends MessageToMessageDecoder<DatagramPacket> {

        @Override
        protected void decode(ChannelHandlerContext ctx,
                              DatagramPacket datagramPacket, List<Object> out)
                throws Exception {
            ByteBuf data = datagramPacket.content();

            Byte type = data.readByte();
            IMessage message = MessageFactory.from(type, data.slice(1,
                    data.readableBytes()).toString(CharsetUtil.UTF_8));
            out.add(message);
        }
    }

    private static class BlockMessageHandler
            extends SimpleChannelInboundHandler<IMessage> {

        private MessageListener listener;
        public BlockMessageHandler(MessageListener listener) {
            super();
            this.listener = listener;

        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx,
                                    Throwable cause) throws Exception {
            log.error("", cause);
            ctx.close();
        }

        @Override
        public void channelRead0(ChannelHandlerContext ctx,
                                 IMessage event) throws Exception {
            log.info("received new message, type: {}", event.getType());

            switch (event.getType()) {
                case IMessage.GET_BLOCKS:
                    listener.syncBlocks(((GetBlocksResponse)event).getBlockList());
                    case IMessage.NEW_BLOCK:
            }
        }
    }

    private static class Sender {
        private final EventLoopGroup group;
        private final Bootstrap bootstrap;
        Channel ch;

        public Sender(InetSocketAddress address) throws InterruptedException {
            group = new NioEventLoopGroup();
            bootstrap = new Bootstrap();
            bootstrap.group(group).channel(NioDatagramChannel.class)
                    .option(ChannelOption.SO_BROADCAST, true)
                    .handler(new BlockMessageEncoder(address));
            ch = bootstrap.bind(0).sync().channel();

        }


        //发送消息，消息通过decoder格式成byte
        public void sendMessage(IMessage message) throws InterruptedException {
            ch.writeAndFlush(message);
        }

        public void stop() {
            group.shutdownGracefully();
        }

    }

    public static class Receiver {
        private final EventLoopGroup group;
        private final Bootstrap bootstrap;
        private String multicastHost;
        private MessageListener listener;

        public Receiver(String multicastHost, int port, MessageListener listener) throws UnknownHostException, SocketException {
            InetSocketAddress address = new InetSocketAddress(port);
            group = new NioEventLoopGroup();
            bootstrap = new Bootstrap();
            this.multicastHost = multicastHost;

            bootstrap.group(group)
                    .channelFactory(new ChannelFactory<Channel>() {
                        @Override
                        public Channel newChannel() {
                            //使用ipv4协议，否则后面加入多播组会出错
                            return new NioDatagramChannel(InternetProtocolFamily.IPv4);
                        }
                    })
                    .option(ChannelOption.SO_BROADCAST, true) //不设置这个参数，也能收到多播消息，设置这个参数的意思是会把收到的消息继续广播
                    .option(ChannelOption.SO_REUSEADDR, true) //设置后，就可以在一台机器开启多个节点，而不需要换端口
                    .handler(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel channel) {
                            ChannelPipeline pipeline = channel.pipeline();
                            //收到数据包后，先解码成区块消息
                            pipeline.addLast(new BlockMessageDecoder());
                            //然后对区块消息作处理
                            pipeline.addLast(new BlockMessageHandler(listener));
                        }
                    })
                    .localAddress(address);
        }

        public Channel bind() throws UnknownHostException, SocketException, InterruptedException {
            //真实的网卡, 如果设置成loopback网卡，则不能收到多播地址上的消息
            //如果设置成真实网卡，既可以收到127.0.0.1上的消息，也可以收到多播地址上的消息
            NetworkInterface nif = NetworkInterface.getByInetAddress(InetAddress.getByName("192.168.1.102"));
            //多播地址, 端口的值在这里没有用处
            InetSocketAddress remoteSocketAddr = new InetSocketAddress(InetAddress.getByName(multicastHost), 0);


            DatagramChannel channel = (DatagramChannel) bootstrap.bind().syncUninterruptibly().channel();
            //在构造bootstrap时，需要把协议设置成IPV4， 否则这里会提示  IPv6 socket cannot join IPv4 multicast group
            channel.joinGroup(remoteSocketAddr, nif).sync();

            return channel;
        }

        public void stop() {
            group.shutdownGracefully();
        }
    }

    //测试代码
    public static void main(String[] args) throws Exception {

        Node node = new Node("239.254.42.96", 1111);

        node.start();
    }
}
