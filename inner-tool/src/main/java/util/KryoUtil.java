package util;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.objenesis.strategy.StdInstantiatorStrategy;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.LinkedHashSet;
import java.util.Set;

public class KryoUtil {
    // TODO: kryo 5.1.0 depends on objenesis 3.2 which is build in 52.0 (java8), save/load still not usable in java7
    static Kryo kryo = new Kryo();
    static Set<ClassLoader> classLoaders = new LinkedHashSet<>();

    static {
        kryo.setReferences(true);
        kryo.setRegistrationRequired(false);
        kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());

        // default class loader is a URLClassLoader inherited from systemClassLoader's parent, which
        // may have not loaded application's classes.
        classLoaders.add(ClassLoader.getSystemClassLoader());
//        kryo.setDefaultSerializer(CompatibleFieldSerializer.class);
    }

    public static byte[] toBytes(Object obj) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Output output = new Output(bos);
        kryo.writeClassAndObject(output, obj);
        output.close();

        classLoaders.add(obj.getClass().getClassLoader());
        return bos.toByteArray();
    }

    public static void writeToFile(Object object, String path) throws FileNotFoundException {
        Output output = new Output(new FileOutputStream(path));
        kryo.writeClassAndObject(output, object);
        output.close();
    }

    public static Object loadFromFile(String path) throws FileNotFoundException {
        return kryo.readClassAndObject(new Input(new FileInputStream(path)));
    }

    public static Object loadObject(byte[] bytes) {
        KryoException lastException = new KryoException("ClassLoader not found");

        // start with system ClassLoader, then try other ClassLoaders known in previous "save"
        for (ClassLoader classLoader : classLoaders) {
            kryo.setClassLoader(classLoader);
            try {
                return kryo.readClassAndObject(new Input(bytes));
            } catch (KryoException kryoException) {
                if (kryoException.getCause() instanceof ClassNotFoundException) {
                    lastException = kryoException;
                } else {
                    throw kryoException;
                }
            }
        }

        throw lastException;
    }

    public static ClassLoader getClassLoader() {
        return kryo.getClassLoader();
    }
}
