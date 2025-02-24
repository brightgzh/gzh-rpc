package com.gzh.serviceimpl;

import com.gzh.Hello;
import com.gzh.HelloService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HelloServiceImpl2 implements HelloService {
    static {
        System.out.println("helloServiceImpl2 被创建");
    }

    @Override
    public String hello(Hello hello) {
        log.info("helloServiceimpl2 接受到消息：{}",hello.getMessage());
        String result = "Hello desctiption is: "+hello.getDescription();
        log.info("helloServiceImpl2 放回：{}",result);
        return result;
    }
}
