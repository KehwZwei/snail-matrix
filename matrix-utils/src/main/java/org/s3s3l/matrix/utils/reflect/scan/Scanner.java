package org.s3s3l.matrix.utils.reflect.scan;

import java.util.Set;

public interface Scanner {

    Set<Class<?>> scan(String... packages);
}
