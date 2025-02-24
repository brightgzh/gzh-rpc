package com.gzh.utils;

import java.util.Collection;

public class CollectionUtils {
    public static boolean isEmpty(Collection<?> c) {
        return c == null || c.isEmpty();
    }
}
