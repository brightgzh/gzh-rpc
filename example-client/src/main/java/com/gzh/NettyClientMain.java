package com.gzh;

import com.gzh.annotation.RpcScan;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.ArrayList;
import java.util.ServiceLoader;

@RpcScan(basePackage = {"com.gzh"})
public class NettyClientMain {

    public static void main(String[] args) throws InterruptedException {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(NettyClientMain.class);
        HelloController helloController = (HelloController) context.getBean("helloController");
        helloController.test();

    }
}
