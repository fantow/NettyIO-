package com.fantow.concurrent;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.Channel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicInteger;

public class EventLoopGroup {

    EventLoop[] eventLoops;

    EventLoopGroup workerGroup = this;

    AtomicInteger count = new AtomicInteger(0);


    public EventLoopGroup(int nums) {
        this.eventLoops = new EventLoop[nums];
        for(int i = 0;i < eventLoops.length;i++){
            eventLoops[i] = new EventLoop(this);
            new Thread(eventLoops[i]).start();
        }

    }


    public void setWorkerGroup(EventLoopGroup workerGroup){
        this.workerGroup = workerGroup;
    }



    // 创建ServerSocketChannel并绑定端口号
    public void bind(int port){

        try {
            ServerSocketChannel server = ServerSocketChannel.open();
            server.configureBlocking(false);
            server.bind(new InetSocketAddress(port));

            // 调用workerGroup进行事件绑定
            transferChannel(server);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    public void transferChannel(Channel channel){
//        if(channel instanceof ServerSocketChannel){
//            EventLoop eventLoop = getNextEventLoop();
//            eventLoop.taskQueue.add(channel);
//
//            // 唤醒eventLoop中被selector.select()阻塞的线程
//            eventLoop.selector.wakeup();
//        }else if(channel instanceof SocketChannel){
//
//
//        }
        EventLoop eventLoop = getNextEventLoop();
        eventLoop.taskQueue.add(channel);

        // 唤醒eventLoop中被selector.select()阻塞的线程
        eventLoop.selector.wakeup();
    }

    public EventLoop getNextEventLoop(){
        int length = eventLoops.length;
        int idx = count.incrementAndGet() % length;

        return workerGroup.eventLoops[idx];
    }


}

