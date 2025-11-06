package cn.acecandy.fasaxi.emma.dao.toolkit.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybatisflex.core.exception.FlexExceptions;
import com.mybatisflex.core.handler.BaseJsonTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.postgresql.util.PGobject;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;

/**
 *
 * @author tangningzhu
 * @since 2025/11/5
 */
@MappedTypes({Object.class})
@MappedJdbcTypes({JdbcType.OTHER})
public class JsonbTypeHandler extends BaseJsonTypeHandler<Object> {

    private static ObjectMapper objectMapper;
    private final Class<?> propertyType;
    private Class<?> genericType;
    private JavaType javaType;

    public JsonbTypeHandler(Class<?> propertyType) {
        this.propertyType = propertyType;
    }

    public JsonbTypeHandler(Class<?> propertyType, Class<?> genericType) {
        this.propertyType = propertyType;
        this.genericType = genericType;
    }

    @Override
    protected Object parseJson(String json) {
        try {
            if (genericType != null && Collection.class.isAssignableFrom(propertyType)) {
                return getObjectMapper().readValue(json, getJavaType());
            } else {
                return getObjectMapper().readValue(json, propertyType);
            }
        } catch (IOException e) {
            throw FlexExceptions.wrap(e, "Can not parseJson by JacksonTypeHandler: " + json);
        }
    }

    @Override
    protected String toJson(Object object) {
        try {
            return getObjectMapper().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw FlexExceptions.wrap(e, "Can not convert object to Json by JacksonTypeHandler: " + object);
        }
    }


    public JavaType getJavaType() {
        if (javaType == null) {
            javaType = getObjectMapper().getTypeFactory().constructCollectionType(
                    (Class<? extends Collection>) propertyType, genericType);
        }
        return javaType;
    }

    public static ObjectMapper getObjectMapper() {
        if (null == objectMapper) {
            objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        }
        return objectMapper;
    }

    public static void setObjectMapper(ObjectMapper objectMapper) {
        JsonbTypeHandler.objectMapper = objectMapper;
    }


    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Object parameter, JdbcType jdbcType) throws SQLException {
        PGobject pgObject = new PGobject();
        pgObject.setType("jsonb");
        pgObject.setValue(this.toJson(parameter));
        ps.setObject(i, pgObject);
    }

}