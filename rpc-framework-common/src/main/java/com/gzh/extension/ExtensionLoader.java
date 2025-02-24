package com.gzh.extension;


import com.gzh.utils.StringUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * refer to dubbo spi: https://dubbo.apache.org/zh-cn/docs/source_code_guide/dubbo-spi.html
 */
@Slf4j
public final class ExtensionLoader<T> {

    // 定义扩展目录的路径
    private static final String SERVICE_DIRECTORY = "META-INF/extensions/";

    // 存储所有已加载的 ExtensionLoader 实例
    private static final Map<Class<?>, ExtensionLoader<?>> EXTENSION_LOADERS = new ConcurrentHashMap<>();

    // 存储所有已创建的扩展实例
    private static final Map<Class<?>, Object> EXTENSION_INSTANCES = new ConcurrentHashMap<>();

    // 当前扩展加载器要加载的接口类型
    private final Class<?> type;

    // 存储当前接口类型下，扩展名与扩展类的映射关系
    private final Map<String, Holder<Object>> cachedInstances = new ConcurrentHashMap<>();

    // 存储扩展类的缓存
    private final Holder<Map<String, Class<?>>> cachedClasses = new Holder<>();

    // 构造函数，初始化接口类型
    private ExtensionLoader(Class<?> type) {
        this.type = type;
    }

    /**
     * 获取指定类型的 ExtensionLoader 实例
     * @param type 扩展的接口类型
     * @param <S> 扩展接口的类型
     * @return 返回对应接口类型的 ExtensionLoader 实例
     */
    public static <S> ExtensionLoader<S> getExtensionLoader(Class<S> type) {
        // 确保类型不为 null
        if (type == null) {
            throw new IllegalArgumentException("Extension type should not be null.");
        }
        // 确保扩展类型是接口类型
        if (!type.isInterface()) {
            throw new IllegalArgumentException("Extension type must be an interface.");
        }
        // 确保接口类型上有 @SPI 注解
        if (type.getAnnotation(SPI.class) == null) {
            throw new IllegalArgumentException("Extension type must be annotated by @SPI");
        }
        // 从缓存中获取 ExtensionLoader 实例
        ExtensionLoader<S> extensionLoader = (ExtensionLoader<S>) EXTENSION_LOADERS.get(type);
        if (extensionLoader == null) {
            // 如果缓存中没有则创建一个新的 ExtensionLoader 实例
            EXTENSION_LOADERS.putIfAbsent(type, new ExtensionLoader<S>(type));
            extensionLoader = (ExtensionLoader<S>) EXTENSION_LOADERS.get(type);
        }
        return extensionLoader;
    }

    /**
     * 获取指定名称的扩展实例
     * @param name 扩展的名称
     * @return 扩展实例
     */
    public T getExtension(String name) {
        // 确保扩展名称不为空
        if (StringUtil.isBlank(name)) {
            throw new IllegalArgumentException("Extension name should not be null or empty.");
        }
        // 从缓存中获取实例
        Holder<Object> holder = cachedInstances.get(name);
        if (holder == null) {
            // 如果缓存中没有该实例，则创建一个新的 Holder
            cachedInstances.putIfAbsent(name, new Holder<>());
            holder = cachedInstances.get(name);
        }
        // 获取扩展实例
        Object instance = holder.get();
        if (instance == null) {
            synchronized (holder) {
                instance = holder.get();
                if (instance == null) {
                    // 创建扩展实例
                    instance = createExtension(name);
                    holder.set(instance);
                }
            }
        }
        return (T) instance;
    }

    /**
     * 创建扩展实例
     * @param name 扩展名
     * @return 扩展实例
     */
    private T createExtension(String name) {
        // 获取扩展类
        Class<?> clazz = getExtensionClasses().get(name);
        if (clazz == null) {
            throw new RuntimeException("No such extension of name " + name);
        }
        // 尝试从缓存中获取扩展实例
        T instance = (T) EXTENSION_INSTANCES.get(clazz);
        if (instance == null) {
            try {
                // 如果缓存中没有实例，则创建实例
                EXTENSION_INSTANCES.putIfAbsent(clazz, clazz.newInstance());
                instance = (T) EXTENSION_INSTANCES.get(clazz);
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }
        return instance;
    }

    /**
     * 获取扩展类
     * @return 扩展类的缓存映射
     */
    private Map<String, Class<?>> getExtensionClasses() {
        // 从缓存中获取扩展类
        Map<String, Class<?>> classes = cachedClasses.get();
        // 双重检查锁定
        if (classes == null) {
            synchronized (cachedClasses) {
                classes = cachedClasses.get();
                if (classes == null) {
                    classes = new HashMap<>();
                    // 加载扩展类
                    loadDirectory(classes);
                    cachedClasses.set(classes);
                }
            }
        }
        return classes;
    }

    /**
     * 从指定目录加载扩展类
     * @param extensionClasses 扩展类的缓存映射
     */
    private void loadDirectory(Map<String, Class<?>> extensionClasses) {
        // 拼接出扩展文件的路径
        String fileName = ExtensionLoader.SERVICE_DIRECTORY + type.getName();
        try {
            // 获取所有与文件名匹配的资源
            Enumeration<URL> urls;
            ClassLoader classLoader = ExtensionLoader.class.getClassLoader();

            urls = classLoader.getResources(fileName);
            log.info("Loading extensions from path: {}", fileName);

            if (urls == null || !urls.hasMoreElements()) {
                System.out.println("=====> No resources found for path "+fileName);
                log.warn("No resources found for path: {}", fileName);
            } else {
                System.out.println("Resources found for path: "+fileName);
                log.info("Resources found for path: {}", fileName);
            }

            if (urls != null) {
                // 如果有资源，遍历资源
                while (urls.hasMoreElements()) {
                    URL resourceUrl = urls.nextElement();
                    log.info("Found resource: {}", resourceUrl);
                    // 加载资源内容
                    loadResource(extensionClasses, classLoader, resourceUrl);
                }
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    /**
     * 加载资源内容，并将扩展类存入缓存
     * @param extensionClasses 扩展类缓存
     * @param classLoader 类加载器
     * @param resourceUrl 资源 URL
     */
    private void loadResource(Map<String, Class<?>> extensionClasses, ClassLoader classLoader, URL resourceUrl) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resourceUrl.openStream(), UTF_8))) {
            String line;
            // 逐行读取资源文件
            while ((line = reader.readLine()) != null) {
                // 处理注释
                final int ci = line.indexOf('#');
                if (ci >= 0) {
                    line = line.substring(0, ci); // 去除注释
                }
                line = line.trim();
                if (line.length() > 0) {
                    try {
                        final int ei = line.indexOf('=');
                        String name = line.substring(0, ei).trim();
                        String clazzName = line.substring(ei + 1).trim();
                        // 确保 name 和 clazzName 都不为空
                        if (name.length() > 0 && clazzName.length() > 0) {
                            // 加载扩展类并存入缓存
                            Class<?> clazz = classLoader.loadClass(clazzName);
                            extensionClasses.put(name, clazz);
                        }
                    } catch (ClassNotFoundException e) {
                        log.error(e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}
