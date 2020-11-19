package com.fantow.concurrent;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class EventLoop implements Runnable {

    public BlockingQueue taskQueue = new LinkedBlockingQueue();

    public Selector selector;

    // 需要获取到该EventLoop对自己EventLoopGroup的引用
    public EventLoopGroup eventLoopGroup;

    public EventLoop(EventLoopGroup eventLoopGroup) {
        this.eventLoopGroup = eventLoopGroup;
        try {
            this.selector = Selector.open();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while(true){
            try {
                int nums = selector.select();
                if(nums > 0){
                    Set<SelectionKey> keys = selector.selectedKeys();
                    Iterator<SelectionKey> iterator = keys.iterator();
                    while(iterator.hasNext()){
                        SelectionKey key = iterator.next();
                        iterator.remove();

                        if(key.isAcceptable()){
                            acceptHandler(key);
                        }else if(key.isReadable()){
                            readHandler(key);
                        }

//                        else if(){
//
//
//                        }

                    }
                }

                // 处理task中的内容
                if(!taskQueue.isEmpty()){
                    try {
                        Channel channel = (Channel)taskQueue.take();

                        // 如果channel是ServerSocketChannel，是要注册Accept事件
                        if(channel instanceof ServerSocketChannel){
                            ServerSocketChannel serverSocketChannel = (ServerSocketChannel) channel;
                            serverSocketChannel.register(selector,SelectionKey.OP_ACCEPT);

                        }else if(channel instanceof SocketChannel){
                            // 如果channel是SocketChannel，是要注册读事件
                            SocketChannel socketChannel = (SocketChannel) channel;

                            // 直接分配的堆外内存
                            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
                            socketChannel.register(selector,SelectionKey.OP_READ,byteBuffer);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }


        }
    }


    public void acceptHandler(SelectionKey key){
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
        try {
            System.out.println("进行Accept处理...");
            SocketChannel channel = serverSocketChannel.accept();

            channel.configureBlocking(false);
            eventLoopGroup.transferChannel(channel);
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    public void readHandler(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        ByteBuffer buffer = (ByteBuffer) key.attachment();

        buffer.clear();
        System.out.println("进行Read处理...");
        while(true){
            int nums = socketChannel.read(buffer);
            if(nums > 0){
                buffer.flip();
                while(buffer.hasRemaining()){
                    socketChannel.write(buffer);
                }
                buffer.clear();
            }else if(nums == 0){
                // 跳出循环
                break;
            }else{
                // 断开连接
                key.cancel();
                break;
            }
        }
    }


}
