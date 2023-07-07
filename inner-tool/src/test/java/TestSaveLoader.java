import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class TestSaveLoader {
    @Test
    public void get_methods() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Class<?> clz = SaveLoader.class;
        Method method = clz.getMethod("saveLoadMethods");
        Object ms = method.invoke(null);
        Method[] methods = (Method[]) ms;
        List<Method> list = new ArrayList<>();
        list.add(methods[0]);
        list.add(null);
        list.add(methods[1]);
        assert methods.length == 2;
    }
}
