package com.fantow.main;

import com.fantow.concurrent.EventLoopGroup;

// 启动主线程
public class MainThread {
    public static void main(String[] args) {

        EventLoopGroup bossGroup = new EventLoopGroup(1);

        EventLoopGroup workerGroup = new EventLoopGroup(3);

        bossGroup.setWorkerGroup(workerGroup);

        bossGroup.bind(9000);

    }
}
