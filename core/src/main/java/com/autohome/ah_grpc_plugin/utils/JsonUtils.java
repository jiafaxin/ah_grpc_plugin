package com.autohome.ah_grpc_plugin.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by zbw on 2017/1/17.
 */
public class JsonUtils {

    public static JsonWapper builder() {
        return new JsonWapper();
    }

    public static String toString(Object obj, String dateFormat) {
        return builder().setDateFormat(dateFormat).toString(obj);
    }

    public static <T> T toObject(String jsonString, Class<T> clz, String dateFormat) {
        return builder().setDateFormat(dateFormat).toObject(jsonString, clz);
    }

    public static <T> T toObject(String jsonString, TypeReference<T> tr, String dateFormat) {
        return builder().setDateFormat(dateFormat).toObject(jsonString, tr);
    }

    public static String toString(Object obj) {
        return toString(obj, null);
    }

    public static <T> T toObject(String jsonString, Class<T> clz) {
        return toObject(jsonString, clz, null);
    }

    /**
     * JSON串转换为Java泛型对象，可以是各种类型，此方法最为强大。用法看测试用例。
     * @param <T>
     * @param jsonString JSON字符串
     * @param tr TypeReference,例如: new TypeReference< List<FamousUser> >(){}
     * @return List对象列表
     */
    public static <T> T toObject(String jsonString, TypeReference<T> tr) {
        return toObject(jsonString, tr, null);
    }

    public static <T> T toObject(InputStream stream, TypeReference<T> tr) {
        return builder().toObject(stream,tr);
    }

    public static <T> List<T> toObjectList(String jsonString, Class<T> clz) {
        return toObjectList(jsonString, clz, null);
    }

    public static <T> List<T> toObjectList(String jsonString, Class<T> clz, String dateFormat) {
        return builder().setDateFormat(dateFormat).toObjectList(jsonString, clz);
    }

    private static JavaType getCollectionType(Class<?> collectionClass, Class<?>... elementClasses) {
        return builder().getObjectMapper().getTypeFactory().constructParametricType(collectionClass, elementClasses);
    }

    public static Object toValue(Object fromValue,Class classz){
        return builder().getObjectMapper().convertValue(fromValue, classz);
    }

    public static class JsonWapper {
        ObjectMapper objectMapper = null;

        public ObjectMapper getObjectMapper() {
            //这里在极高并发下可能会有问题，但可能性极小，不加LOCK了
            if (objectMapper == null) {
                objectMapper = new ObjectMapper();
                objectMapper
                        .setSerializationInclusion(JsonInclude.Include.ALWAYS)
                        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                        .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES,true);
            }
            return objectMapper;
        }

        public String toString(Object obj) {
            try {
                return getObjectMapper().writeValueAsString(obj);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * JSON串转换为Java泛型对象，可以是各种类型，此方法最为强大。用法看测试用例。
         *
         * @param <T>
         * @param jsonString JSON字符串
         * @param tr         TypeReference,例如: new TypeReference< List<FamousUser> >(){}
         * @return List对象列表
         */
        public <T> T toObject(String jsonString, TypeReference<T> tr) {
            if (jsonString == null || "".equals(jsonString)) {
                return null;
            } else {
                try {
                    return (T) getObjectMapper().readValue(jsonString, tr);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }


        public <T> T toObject(InputStream stream, TypeReference<T> tr) {
            if (stream == null) {
                return null;
            } else {
                try {
                    return (T) getObjectMapper().readValue(stream, tr);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }

        public <T> List<T> toObjectList(String jsonString, Class<T> clz) {
            JavaType javaType = getCollectionType(ArrayList.class, clz);
            try {
                return getObjectMapper().readValue(jsonString, javaType);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public <T> T toObject(String jsonString, Class<T> clz) {
            try {
                return getObjectMapper().readValue(jsonString, clz);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public JsonWapper setPropertyNamingStrategy(PropertyNamingStrategy propertyNamingStrategy) {
            if (propertyNamingStrategy != null)
                getObjectMapper().setPropertyNamingStrategy(propertyNamingStrategy);
            return this;
        }

        public JsonWapper setDateFormat(String dateFormat) {
            if (dateFormat!=null && !dateFormat.equals(""))
                getObjectMapper().setDateFormat(new SimpleDateFormat(dateFormat));
            return this;
        }
    }
}

