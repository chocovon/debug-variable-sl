import org.junit.Test;
import util.KryoUtil;

import java.util.Vector;

public class TestKryo {

    // vector deserializing causes NPE by default
    // com.esotericsoftware.kryo.serializers.CollectionSerializer.create
    @Test(expected = Exception.class)
    public void test_vector_bug() {
        Vector<Class> vector = new Vector<>();
        vector.add(KryoUtil.class);
        vector.add(this.getClass());
        byte[] bytes = KryoUtil.toBytes(vector);
        Object object = KryoUtil.loadObject(bytes);
        Vector<Class> vector1 = (Vector<Class>) object;
        assert vector1.get(1) != null;
    }
}
