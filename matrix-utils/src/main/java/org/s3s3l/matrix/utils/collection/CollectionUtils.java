package org.s3s3l.matrix.utils.collection;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import org.s3s3l.matrix.utils.bean.TreeNode;
import org.s3s3l.matrix.utils.verify.Verify;

/**
 * <p>
 * </p>
 * ClassName:CollectionUtils <br>
 * 
 * @author kehw_zwei
 * @version 1.0.0
 * @since JDK 1.8
 */
public abstract class CollectionUtils {

    public static boolean isEmpty(final Collection<?> coll) {
        return coll == null || coll.isEmpty();
    }

    public static <T extends TreeNode<T>> List<T> flat(TreeNode<T> node) {
        List<T> children = node.getChildren();

        if (children == null || children.isEmpty()) {
            return Collections.emptyList();
        }
        List<T> result = new LinkedList<>(children);

        for (T child : node.getChildren()) {
            result.addAll(flat(child));
        }

        return result;
    }

    @SafeVarargs
    public static <T> T getFirst(T... collection) {
        if (collection == null) {
            return null;
        }
        Optional<T> opt;
        try {
            opt = Arrays.stream(collection)
                    .findFirst();
        } catch (NullPointerException e) {
            return null;
        }
        if (opt.isPresent()) {
            return opt.get();
        }

        return null;
    }

    public static <T> T getFirst(Collection<T> collection) {
        if (collection == null) {
            return null;
        }
        Optional<T> opt;
        try {
            opt = collection.stream()
                    .findFirst();
        } catch (NullPointerException e) {
            return null;
        }
        if (opt.isPresent()) {
            return opt.get();
        }

        return null;
    }

    public static <T> Optional<T> getFirstOptional(Collection<T> collection) {
        if (collection == null) {
            return Optional.empty();
        }
        return collection.stream()
                .findFirst();
    }

    public static <T> T getFirst(Collection<T> collection, Predicate<? super T> predicate) {
        if (collection == null) {
            return null;
        }
        Optional<T> opt;
        try {
            opt = collection.stream()
                    .filter(predicate)
                    .findFirst();
        } catch (NullPointerException e) {
            return null;
        }
        if (opt.isPresent()) {
            return opt.get();
        }

        return null;

    }

    public static <T> Optional<T> getFirstOptional(Collection<T> collection, Predicate<? super T> predicate) {
        Verify.notNull(collection);
        Verify.notNull(predicate);
        return collection.stream()
                .filter(predicate)
                .findFirst();

    }
}
