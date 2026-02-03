package com.liskovsoft.sharedutils.helpers;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

public class Helpers {
    public static boolean equals(Object obj1, Object obj2) {
        if (obj1 == null && obj2 == null) return true;
        if (obj1 == null || obj2 == null) return false;
        return obj1.equals(obj2);
    }

    public static InputStream toStream(String content) {
        if (content == null) {
            return null;
        }
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    public static <T> List<T> removeIf(Collection<T> collection, Predicate<T> predicate) {
        if (collection == null || predicate == null) {
            return null;
        }
        List<T> removed = new ArrayList<>();
        Iterator<T> iterator = collection.iterator();
        while (iterator.hasNext()) {
            T item = iterator.next();
            if (predicate.test(item)) {
                removed.add(item);
                iterator.remove();
            }
        }
        return removed;
    }

    public static <T> T findFirst(Collection<T> collection, Predicate<T> predicate) {
        if (collection == null || predicate == null) {
            return null;
        }
        for (T item : collection) {
            if (predicate.test(item)) {
                return item;
            }
        }
        return null;
    }
}
