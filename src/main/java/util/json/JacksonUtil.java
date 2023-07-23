package util.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JacksonUtil {
    // in Windows, '\r' causes error in com.intellij.openapi.util.text.StringUtil.assertValidSeparators
    public static String prettyFormatWithoutSlashR(String content) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        Object object = objectMapper.readValue(content, Object.class);
        // https://github.com/FasterXML/jackson-databind/issues/585#issuecomment-643163524
        return objectMapper.writer(new DefaultPrettyPrinter()
                .withObjectIndenter(new DefaultIndenter().withLinefeed("\n"))).writeValueAsString(object);
    }
}
