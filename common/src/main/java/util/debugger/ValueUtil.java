package util.debugger;

import com.sun.jdi.*;

import java.util.Arrays;
import java.util.List;

public class ValueUtil {
    public enum PrimitiveType{
        BOOLEAN,
        BYTE,
        CHAR,
        DOUBLE,
        FLOAT,
        INT,
        LONG,
        SHORT
    }

    public static Value invokeMethod(ObjectReference object, String methodName, ThreadReference thread, Value... args) throws InvocationException, InvalidTypeException, ClassNotLoadedException, IncompatibleThreadStateException {
        List<Method> methods = object.referenceType().methodsByName(methodName);
        if (methods.size() == 0) {
            throw new NoSuchMethodError(methodName);
        }
        for (Method m : methods) {
            List<Type> argType = m.argumentTypes();
            if (argType.size() == args.length - 1 || argType.size() == args.length) {
                try {
                    return object.invokeMethod(thread, m, Arrays.asList(args), 0);
                } catch (IllegalArgumentException ignored) {
                    //after all method tried then throw this exception
                    //todo using local method
                }
            }
        }
        throw new IllegalArgumentException();
    }

    public static Value valueOfPrimitive(VirtualMachine vm, String val, String type) {
        switch (type) {
            case "boolean":
                return vm.mirrorOf(Boolean.parseBoolean(val));
            case "byte":
                return vm.mirrorOf(Byte.parseByte(val));
            case "char":
                return vm.mirrorOf(val.charAt(0));
            case "double":
                return vm.mirrorOf(Double.parseDouble(val));
            case "float":
                return vm.mirrorOf(Float.parseFloat(val));
            case "int":
                return vm.mirrorOf(Integer.parseInt(val));
            case "long":
                return vm.mirrorOf(Long.parseLong(val));
            case "short":
                return vm.mirrorOf(Short.parseShort(val));
            default:
                return vm.mirrorOfVoid();
        }
    }

//    public static boolean matchType(Type val, Type sig) {
//        if (sig instanceof PrimitiveType) {
//            return sig.equals(val);
//        } else {
//
//        }
//        Set<Type> allTypes = new HashSet<>();
//        allTypes.addAll()
//    }

//    public static Value invokeMethodByClz(ObjectReference clzObj, String methodName, ThreadReference thread, Value... args) {
//        try {
//            Value methodInVm = getMethodByClz(clzObj, methodName, thread, args);
//            return invokeMethod((ObjectReference) methodInVm, "invoke", thread, args);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return null;
//    }
//
//    public static Value getMethodByClz(ObjectReference clzObj, String methodName, ThreadReference thread, Value... args) throws InvocationException, InvalidTypeException, ClassNotLoadedException, IncompatibleThreadStateException {
//        Method getMethod = clzObj.referenceType()
//                .methodsByName("getMethod").get(0);
//
//        List<Value> methodNameAndArgTypes = new ArrayList<>();
//        methodNameAndArgTypes.add(clzObj.virtualMachine().mirrorOf(methodName));
//        for (Value arg : args) {
//            if (arg instanceof ObjectReference) {
//                ObjectReference or = (ObjectReference) arg;
//                Method getClass = or.referenceType().methodsByName("getClass").get(0);
//                Value argClass = ((ObjectReference) arg).invokeMethod(thread, getClass, new ArrayList<>(), 0);
//                methodNameAndArgTypes.add(argClass);
//            } else {
//                //TODO primitive args
//            }
//        }
//
//        return clzObj.invokeMethod(thread, getMethod, methodNameAndArgTypes, 0);
//    }

//    public static Value invokeStaticMethod(ObjectReference object, String methodName, ThreadReference thread, Value... args) {
//        Method m = object.referenceType()
//                .methodsByName(methodName).get(0);
//        try {
//            List<Value> argsWithNull = new ArrayList<>();
//            argsWithNull.add(null);
//            argsWithNull.addAll(Arrays.asList(args));
//            return object.invokeMethod(thread, m, argsWithNull, 0);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return null;
//    }
}
