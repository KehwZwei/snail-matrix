package org.s3s3l.matrix.utils.stuctural.jackson;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.URL;

import org.s3s3l.matrix.utils.stuctural.StructuralHelper;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * <p>
 * </p>
 * ClassName: JacksonHelper <br>
 * Date: Dec 8, 2016 5:43:07 PM <br>
 * 
 * @author kehw_zwei
 * @version 1.0.0
 * @since JDK 1.8
 */
public interface JacksonHelper extends StructuralHelper {

    JacksonHelper include(Include include);

    /**
     * 
     * configure object mapper
     * 
     * @param feature
     *            feature
     * @param state
     *            true or false
     * @return this instance
     * @since JDK 1.8
     */
    JacksonHelper configure(DeserializationFeature feature, boolean state);

    /**
     * 
     * configure object mapper
     * 
     * @param feature
     *            feature
     * @param state
     *            true or false
     * @return this instance
     * @since JDK 1.8
     */
    JacksonHelper configure(SerializationFeature feature, boolean state);

    /**
     * 
     * configure object mapper
     * 
     * @param feature
     *            feature
     * @param state
     *            true or false
     * @return this instance
     * @since JDK 1.8
     */
    JacksonHelper configure(MapperFeature feature, boolean state);

    JacksonHelper setPropertyNamingStrategy(PropertyNamingStrategy pns);

    JavaType getJavaType(Type type);

    /**
     * 
     * ??????????????????
     * 
     * @author kehw_zwei
     * @return ??????TreeNode??????
     * @since JDK 1.8
     */
    ObjectNode createObjectNode();

    /**
     * 
     * ??????????????????
     * 
     * @author kehw_zwei
     * @return ??????ArrayNode??????
     * @since JDK 1.8
     */
    ArrayNode createArrayNode();

    /**
     * 
     * ???json??????????????????JsonNode??????
     * 
     * @author kehw_zwei
     * @param str
     *            ????????????
     * @return ??????json??????????????????TreeNode??????
     * @since JDK 1.8
     */
    JsonNode toTreeNode(String str);

    /**
     * 
     * ???json?????????????????????JsonNode??????
     * 
     * @author kehw_zwei
     * @param stream
     *            ???????????????
     * @return ??????json??????????????????TreeNode??????
     * @since JDK 1.8
     */
    JsonNode toTreeNode(InputStream stream);

    /**
     * 
     * ???json?????????????????????JsonNode??????
     * 
     * @author kehw_zwei
     * @param bytes
     *            ???????????????
     * @return ??????json??????????????????TreeNode??????
     * @since JDK 1.8
     */
    JsonNode toTreeNode(byte[] bytes);

    /**
     * 
     * ??????????????????????????????
     * 
     * @author kehw_zwei
     * @param from
     *            ?????????
     * @param toCls
     *            ????????????
     * @param <T>
     *            ????????????
     * @return ??????????????????????????????
     * @since JDK 1.8
     */
    <T> T convert(Object from, Class<T> toCls);

    /**
     * 
     * ??????????????????????????????
     * 
     * @author kehw_zwei
     * @param from
     *            ?????????
     * @param type
     *            ????????????
     * @param <T>
     *            ????????????
     * @return ??????????????????????????????
     * @since JDK 1.8
     */
    <T> T convert(Object from, TypeReference<T> type);

    /**
     * 
     * ??????????????????????????????json?????????
     * 
     * @author kehw_zwei
     * @return ????????????????????????json???JacksonUtils??????
     * @since JDK 1.8
     */
    JacksonHelper prettyPrinter();

    /**
     * 
     * update target fields
     * 
     * @param origin
     *            origin object
     * @param overrideSource
     *            resource for update
     * @return updated object; the fields in overrideSource will override origin
     *         object`s field
     * @since JDK 1.8
     */
    <T> T update(T origin, String overrideSource);

    /**
     * 
     * update target fields
     * 
     * @param origin
     *            origin object
     * @param overrideSource
     *            resource for update
     * @return updated object; the fields in overrideSource will override origin
     *         object`s field
     * @since JDK 1.8
     */
    <T> T update(T origin, File overrideSource);

    /**
     * 
     * update target fields
     * 
     * @param origin
     *            origin object
     * @param overrideSource
     *            resource for update
     * @return updated object; the fields in overrideSource will override origin
     *         object`s field
     * @since JDK 1.8
     */
    <T> T update(T origin, URL overrideSource);

    /**
     * 
     * update target fields
     * 
     * @param origin
     *            origin object
     * @param overrideSource
     *            resource for update
     * @return updated object; the fields in overrideSource will override origin
     *         object`s field
     * @since JDK 1.8
     */
    <T> T update(T origin, InputStream overrideSource);

    /**
     * 
     * update target fields
     * 
     * @param origin
     *            origin object
     * @param overrideSource
     *            resource for update
     * @return updated object; the fields in overrideSource will override origin
     *         object`s field
     * @since JDK 1.8
     */
    <T> T update(T origin, JsonNode overrideSource);

    /**
     * 
     * update target fields
     * 
     * @param origin
     *            origin object
     * @param overrideSource
     *            resource for update
     * @param type
     *            object type
     * @return combined object; the non-empty fields in overrideSource will
     *         override origin object`s field
     * @since JDK 1.8
     */
    <T> T combine(T origin, T overrideSource, Class<T> type);

    /**
     * 
     * Convert {@link TreeNode} to Object.
     * 
     * @param <T>
     *            Customer type.
     * @param node
     *            TreeNode.
     * @param type
     *            Type class.
     * @return Instance of customer type.
     * @since JDK 1.8
     */
    <T> T treeToValue(TreeNode node, Class<T> type);

    /**
     * 
     * Convert {@link JsonNode} to Object.
     * 
     * @param <T>
     *            Customer type.
     * @param node
     *            JsonNode.
     * @param type
     *            Type class.
     * @return Instance of customer type.
     * @since JDK 1.8
     */
    <T> T nodeToValue(JsonNode node, TypeReference<T> type);

    /**
     * 
     * Convert Object to {@link TreeNode}.
     * 
     * @param origin
     *            Origin object.
     * @return {@link TreeNode} for origin object.
     * @since JDK 1.8
     */
    JsonNode valueToTree(Object origin);

}
