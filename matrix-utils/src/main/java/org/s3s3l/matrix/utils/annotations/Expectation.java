package org.s3s3l.matrix.utils.annotations;

/**
 * <p>
 * </p>
 * ClassName:Expectation <br>
 * 
 * @author kehw_zwei
 * @version 1.0.0
 * @since JDK 1.8
 */
public enum Expectation {
    NULL,
    NOT_NULL,
    HAS_LENGTH,
    EMPTY,
    NOT_EMPTY,
    LENGTH_LIMIT,
    FIXED_LENGTH,
    TYPE,
    IS_INTERFACE,
    EXAMINED,
    LARGER_THAN,
    LESS_THAN,
    MATCH;
}
