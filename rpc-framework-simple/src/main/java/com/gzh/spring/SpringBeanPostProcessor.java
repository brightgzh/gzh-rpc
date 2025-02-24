package com.gzh.spring;


import com.gzh.annotation.RpcReference;
import com.gzh.annotation.RpcService;
import com.gzh.config.RpcServiceConfig;
import com.gzh.enums.RpcRequestTransportEnum;
import com.gzh.extension.ExtensionLoader;
import com.gzh.factory.SingletonFactory;
import com.gzh.provider.ServiceProvider;
import com.gzh.provider.ZkServiceProviderImpl;
import com.gzh.proxy.RpcClientProxy;
import com.gzh.remoting.transport.RpcRequestTransport;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

/**
 * call this method before creating the bean to see if the class is annotated
 *
 * @author shuang.kou
 * @createTime 2020年07月14日 16:42:00
 */
@Slf4j
@Component
public class SpringBeanPostProcessor implements BeanPostProcessor {

    private final ServiceProvider serviceProvider;
    private final RpcRequestTransport rpcClient;

    public SpringBeanPostProcessor() {
        this.serviceProvider = SingletonFactory.getInstance(ZkServiceProviderImpl.class);
        this.rpcClient = ExtensionLoader.getExtensionLoader(RpcRequestTransport.class).getExtension(RpcRequestTransportEnum.NETTY.getName());
    }

    @SneakyThrows
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (bean.getClass().isAnnotationPresent(RpcService.class)) {
            log.info("[{}] is annotated with  [{}]", bean.getClass().getName(), RpcService.class.getCanonicalName());
            // get RpcService annotation
            RpcService rpcService = bean.getClass().getAnnotation(RpcService.class);
            // build RpcServiceProperties
            RpcServiceConfig rpcServiceConfig = RpcServiceConfig.builder()
                    .group(rpcService.group())
                    .version(rpcService.version())
                    .service(bean).build();
            serviceProvider.publishService(rpcServiceConfig);
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        // 获取当前bean的类类型
        Class<?> targetClass = bean.getClass();

        // 获取当前bean的所有声明的字段（即类中定义的所有成员变量）
        Field[] declaredFields = targetClass.getDeclaredFields();

        // 遍历所有字段
        for (Field declaredField : declaredFields) {
            // 检查当前字段是否有 @RpcReference 注解
            RpcReference rpcReference = declaredField.getAnnotation(RpcReference.class);

            // 如果字段上有 @RpcReference 注解，则处理该字段
            if (rpcReference != null) {
                // 创建一个RpcServiceConfig配置对象，通过注解中提供的group和version来配置
                RpcServiceConfig rpcServiceConfig = RpcServiceConfig.builder()
                        .group(rpcReference.group())  // 使用注解中提供的group
                        .version(rpcReference.version())  // 使用注解中提供的version
                        .build();

                // 创建RpcClientProxy代理对象，传入rpcClient和配置对象
                RpcClientProxy rpcClientProxy = new RpcClientProxy(rpcClient, rpcServiceConfig);

                // 使用RpcClientProxy来生成代理对象，代理对象的类型为当前字段的类型（即声明的字段类型）
                Object clientProxy = rpcClientProxy.getProxy(declaredField.getType());

                // 设置字段为可访问（如果字段是私有的）
                declaredField.setAccessible(true);

                try {
                    // 将创建的代理对象注入到当前bean的字段中
                    declaredField.set(bean, clientProxy);
                } catch (IllegalAccessException e) {
                    // 捕获并处理非法访问异常，通常发生在字段不可访问时
                    e.printStackTrace();
                }
            }
        }

        // 返回bean本身，这样Spring可以继续使用它
        return bean;
    }

}
