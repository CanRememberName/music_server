package com.music.server.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
public class JsonUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        // Configure ObjectMapper
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        // Do not fail on empty beans
        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    }

    private JsonUtil() {
        // Private constructor to prevent instantiation
    }

    /**
     * Convert object to JSON string
     *
     * @param object The object to convert
     * @return JSON string or null if conversion fails
     */
    public static String toJson(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("Failed to convert object to JSON string", e);
            return null;
        }
    }

    /**
     * Convert JSON string to Object
     *
     * @param json  JSON string
     * @param clazz Target class
     * @param <T>   Target type
     * @return Object of type T or null if conversion fails
     */
    public static <T> T toObject(String json, Class<T> clazz) {
        if (!StringUtils.hasText(json) || clazz == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, clazz);
        } catch (IOException e) {
            log.error("Failed to convert JSON string to object", e);
            return null;
        }
    }

    /**
     * Convert JSON string to List
     *
     * @param json  JSON string
     * @param clazz Class of the list elements
     * @param <T>   Type of the list elements
     * @return List of T or empty list if conversion fails
     */
    public static <T> List<T> toList(String json, Class<T> clazz) {
        if (!StringUtils.hasText(json) || clazz == null) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (IOException e) {
            log.error("Failed to convert JSON string to list", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Convert JSON string to generic object using TypeReference
     * 
     * @param json JSON string
     * @param typeReference Type reference
     * @param <T> Target type
     * @return Object of type T or null
     */
    public static <T> T toObject(String json, TypeReference<T> typeReference) {
        if (!StringUtils.hasText(json) || typeReference == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, typeReference);
        } catch (IOException e) {
            log.error("Failed to convert JSON string to object via TypeReference", e);
            return null;
        }
    }
    
    /**
     * Get the shared ObjectMapper instance
     * @return ObjectMapper
     */
    public static ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}
