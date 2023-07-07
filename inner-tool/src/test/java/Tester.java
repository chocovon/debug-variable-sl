
import com.alibaba.fastjson.JSON;

import java.awt.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Tester {
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


    public static void main(String[] args) throws IOException {
        Map<Tester, Tester> map = new HashMap<>();
        Tester t1 = new Tester("t1", "tt");
        Tester t2 = new Tester("t2", "pp");
        map.put(t1, t2);
        map.put(t2, t1);
        Tester t3 = new Tester("t3", "hh", t1);
        map.put(t3, t3);
        t2.obj = t2;
        System.out.println(JSON.toJSONString(new Dimension(2,3), true));

        System.out.println(JSON.toJSONString(t2, true));

//        String json = toJSON(map);
//        System.out.println(json);
//
//        Object object = KryoUtil.loadObject(KryoUtil.toBytes(t3));

//        String kryoedObjJSON = toJSON(object);
//        System.out.println(kryoedObjJSON);
//        Tester t4 = objectMapper.readValue(kryoedObjJSON, Tester.class);
//
//        System.out.println(t4);

    }
}
