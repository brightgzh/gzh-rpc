package com.gzh.serviceimpl;

import com.gzh.Hello;
import com.gzh.HelloService;
import com.gzh.annotation.RpcService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RpcService(group = "test1",version = "version1")
public class HelloServiceImpl implements HelloService {
    static {
        System.out.println("helloServiceImpl 被创建");
    }

    @Override
    public String hello(Hello hello) {
        log.info("helloService 接受到消息：{}",hello.getMessage());
        String result = "hello description is "+ hello.getDescription();
        log.info("helloServiceimpl 返回：{}",result);
        return result;
    }
}
