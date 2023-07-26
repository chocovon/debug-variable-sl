package util.serialization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class JsonTest {
    public static class Tester {
        String name;
        String type;
        Tester obj;

        public Tester(String name, String type) {
            this.name = name;
            this.type = type;
        }

        public Tester(String name, String type, Tester obj) {
            this.name = name;
            this.type = type;
            this.obj = obj;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }

    @Test
    public void testOutput() throws JsonProcessingException {
        Map<Tester, Tester> map = new HashMap<>();
        Tester t1 = new Tester("t1", "tt");
        Tester t2 = new Tester("t2", "pp");
        map.put(t1, t2);
        map.put(t2, t1);
        Tester t3 = new Tester("t3", "hh", t1);
        map.put(t3, t3);
        t2.obj = t2;

        Map map2 = new HashMap();
        map2.put(map, map);

        ObjectMapper objectMapper = new ObjectMapper();
        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(map));
        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(map2));
    }
}
