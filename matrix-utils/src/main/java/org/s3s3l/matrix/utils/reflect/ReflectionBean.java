package org.s3s3l.matrix.utils.reflect;

import java.util.Map;
import java.util.Properties;

/**
 * ClassName:ReflectionBean <br>
 * 
 * @author kehw_zwei
 * @version 1.0.0
 * @since JDK 1.8
 */
public interface ReflectionBean {

    boolean hasField(String fieldName);

    /**
     * 利用反射获取指定对象的指定属性
     * 
     * @param fieldName
     *            目标属性
     * @return 目标属性的值
     */
    Object getFieldValue(String fieldName);

    /**
     * 利用反射设置指定对象的指定属性为指定的值
     * 
     * @param fieldName
     *            目标属性
     * @param fieldValue
     *            目标值
     * @throws IllegalAccessException
     *             {@link IllegalAccessException}
     */
    void setFieldValue(String fieldName, Object fieldValue);

    void fill(Properties prop);

    void fill(Map<String, Object> map);
}
