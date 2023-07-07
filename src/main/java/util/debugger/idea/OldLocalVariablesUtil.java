package util.debugger.idea;

import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.ReflectionUtil;
import com.sun.jdi.StackFrame;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

//copied from com.intellij.debugger.jdi.LocalVariablesUtil in 2017.3.6
public class OldLocalVariablesUtil {
    private static final Logger LOG = Logger.getInstance("#com.intellij.debugger.jdi.LocalVariablesUtil");

    private static final boolean ourInitializationOk;
    private static Class<?> ourSlotInfoClass;
    private static Constructor<?> slotInfoConstructor;
    private static Method ourEnqueueMethod;
    private static Method ourWaitForReplyMethod;

    private static final boolean ourInitializationOkSet;
    private static Class<?> ourSlotInfoClassSet;
    private static Constructor<?> slotInfoConstructorSet;
    private static Method ourEnqueueMethodSet;
    private static Method ourWaitForReplyMethodSet;

    static {
        // get values init
        boolean success = false;
        try {
            String GetValuesClassName = "com.sun.tools.jdi.JDWP$StackFrame$GetValues";
            ourSlotInfoClass = Class.forName(GetValuesClassName + "$SlotInfo");
            slotInfoConstructor = ourSlotInfoClass.getDeclaredConstructor(int.class, byte.class);
            slotInfoConstructor.setAccessible(true);

            Class<?> ourGetValuesClass = Class.forName(GetValuesClassName);
            ourEnqueueMethod = getDeclaredMethodByName(ourGetValuesClass, "enqueueCommand");
            ourWaitForReplyMethod = getDeclaredMethodByName(ourGetValuesClass, "waitForReply");

            success = true;
        }
        catch (Throwable e) {
            LOG.info(e);
        }
        ourInitializationOk = success;

        // set value init
        success = false;
        try {
            String setValuesClassName = "com.sun.tools.jdi.JDWP$StackFrame$SetValues";
            ourSlotInfoClassSet = Class.forName(setValuesClassName + "$SlotInfo");
            slotInfoConstructorSet = ourSlotInfoClassSet.getDeclaredConstructors()[0];
            slotInfoConstructorSet.setAccessible(true);

            Class<?> ourGetValuesClassSet = Class.forName(setValuesClassName);
            ourEnqueueMethodSet = getDeclaredMethodByName(ourGetValuesClassSet, "enqueueCommand");
            ourWaitForReplyMethodSet = getDeclaredMethodByName(ourGetValuesClassSet, "waitForReply");

            success = true;
        }
        catch (Throwable e) {
            LOG.info(e);
        }
        ourInitializationOkSet = success;
    }

    public static void setValue(StackFrame frame, int slot, Value value) throws EvaluateException {
        try {
            final Long frameId = ReflectionUtil.getField(frame.getClass(), frame, long.class, "id");
            final VirtualMachine vm = frame.virtualMachine();
            final Method stateMethod = vm.getClass().getDeclaredMethod("state");
            stateMethod.setAccessible(true);

            Object slotInfoArray = createSlotInfoArraySet(slot, value);

            Object ps;
            final Object vmState = stateMethod.invoke(vm);
            synchronized (vmState) {
                ps = ourEnqueueMethodSet.invoke(null, vm, frame.thread(), frameId, slotInfoArray);
            }

            ourWaitForReplyMethodSet.invoke(null, vm, ps);
        }
        catch (Exception e) {
            throw new EvaluateException("Unable to set value", e);
        }
    }

    private static Object createSlotInfoArraySet(int slot, Value value)
            throws IllegalAccessException, InvocationTargetException, InstantiationException {
        Object arrayInstance = Array.newInstance(ourSlotInfoClassSet, 1);
        Array.set(arrayInstance, 0, slotInfoConstructorSet.newInstance(slot, value));
        return arrayInstance;
    }

    private static Method getDeclaredMethodByName(Class aClass, String methodName) throws NoSuchMethodException {
        for (Method method : aClass.getDeclaredMethods()) {
            if (methodName.equals(method.getName())) {
                method.setAccessible(true);
                return method;
            }
        }
        throw new NoSuchMethodException(aClass.getName() + "." + methodName);
    }
}
