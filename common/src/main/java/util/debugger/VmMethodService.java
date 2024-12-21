package util.debugger;

import com.intellij.debugger.engine.JavaValue;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.ui.impl.watch.ValueDescriptorImpl;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.xdebugger.XDebuggerBundle;
import com.intellij.xdebugger.XExpression;
import com.intellij.xdebugger.evaluation.EvaluationMode;
import com.intellij.xdebugger.evaluation.XDebuggerEvaluator;
import com.intellij.xdebugger.frame.XValue;
import com.intellij.xdebugger.impl.breakpoints.XExpressionImpl;
import com.intellij.xdebugger.impl.ui.tree.nodes.XEvaluationCallbackBase;
import com.sun.jdi.*;
import org.jetbrains.annotations.NotNull;
import ui.common.SimplePopupHint;
import util.file.StreamUtil;
import util.thread.AsyncTask;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static config.Config.*;

public class VmMethodService {
    public static Map<VirtualMachine, ObjectReference[]> cache = new HashMap<>();

    public static ObjectReference getSaveMethod(NodeComponents comp) throws IOException {
        return getMethods(comp)[0];
    }

    public static ObjectReference getLoadMethod(NodeComponents comp) throws IOException {
        return getMethods(comp)[1];
    }

    public static ObjectReference getCodeMethod(NodeComponents comp) throws IOException {
        return getMethods(comp)[2];
    }

    public static ObjectReference getGenCodeMethod(NodeComponents comp) throws IOException {
        return getMethods(comp)[3];
    }

    private static ObjectReference[] getMethods(NodeComponents comp) throws IOException {
        ObjectReference[] methods = cache.get(comp.vm);
        if (methods == null) {
            methods = loadMethodsFromVm(comp);
        }
        return methods;
    }

    @NotNull
    private static ObjectReference[] loadMethodsFromVm(NodeComponents comp) throws IOException {
        ObjectReference[] methods;
        ArrayReference methodArr = getMethodsFromVm(comp);
        methods = new ObjectReference[]{
                (ObjectReference)methodArr.getValue(0),
                (ObjectReference)methodArr.getValue(1),
                (ObjectReference)methodArr.getValue(2),
                (ObjectReference)methodArr.getValue(3)
        };
        cache.put(comp.vm, methods);
        return methods;
    }

    private static boolean checkAndroid(NodeComponents comp) {
        String exprText = "InMemoryDexClassLoader.class.getClassLoader().getParent() == null";
        String imports = "dalvik.system.InMemoryDexClassLoader,";
        XExpression expression = new XExpressionImpl(exprText, JavaLanguage.INSTANCE, imports, EvaluationMode.CODE_FRAGMENT);
        Value checkResult = evaluateExpr(expression, comp.evaluator);
        return checkResult instanceof BooleanValue && ((BooleanValue) checkResult).value();
    }

    private static ArrayReference getMethodsFromVm(NodeComponents comp) throws IOException {
        InputStream resourceAsStream = VmMethodService.class.getResourceAsStream("/lib/" + JAR_NAME);

        String bytes = StreamUtil.readBytesAsISOString(resourceAsStream);

        String exprText =
                "        String str = \"" + escape(bytes) + "\";" +
                "        byte[] bytes = str.getBytes(StandardCharsets.ISO_8859_1);\n" +
                "        File f = File.createTempFile(\"inner-tool\", \".jar\");" +
                "        f.deleteOnExit();" +
                "        Files.write(f.toPath(), bytes);" +
                "        URLClassLoader urlClassLoader = new URLClassLoader(\n" +
                "                new URL[] {f.toURI().toURL()},\n" +
                "                ClassLoader.getSystemClassLoader().getParent()\n" +
                "        );\n" +
                "        Class<?> clz = Class.forName(\"SaveLoader\", true, urlClassLoader);" +
                "        Method method = clz.getMethod(\"saveLoadMethods\");\n" +
                "        Object ms = method.invoke(null);" +
                "        Method[] methods = (Method[]) ms;\n" +
                "        methods";
        String imports =
                "java.io.File," +
                "java.net.URL," +
                "java.net.URLClassLoader," +
                "java.lang.reflect.Method," +
                "java.nio.charset.StandardCharsets," +
                "java.nio.file.Files,";

        XExpression expression = new XExpressionImpl(exprText, JavaLanguage.INSTANCE, imports, EvaluationMode.CODE_FRAGMENT);
        Value ret = evaluateExpr(expression, comp.evaluator);
        if (ret instanceof ArrayReference) {
            return (ArrayReference) ret;
        } else {
            throw new RuntimeException(ret.toString());
        }
    }

    private static Value evaluateExpr(XExpression expression, XDebuggerEvaluator evaluator) {
        AsyncTask<Value> asyncTask = new AsyncTask<Value>(){
            @Override
            public void asyncCodeRun() {
                XDebuggerEvaluator.XEvaluationCallback callback = new XEvaluationCallbackBase() {
                    @Override
                    public void evaluated(@NotNull final XValue result) {
                        ValueDescriptorImpl valueDescriptor = ((JavaValue) result).getDescriptor();
                        finishRet(valueDescriptor.getValue());
                    }

                    @Override
                    public void errorOccurred(@NotNull final String errorMessage) {
                        finishError(new EvaluateException(errorMessage));
                    }
                };

                if (evaluator == null) {
                    callback.errorOccurred(XDebuggerBundle.message("xdebugger.evaluate.stack.frame.has.not.evaluator"));
                } else {
                    evaluator.evaluate(expression, callback, null);
                }
            }
        };
        if (asyncTask.run()) {
            return asyncTask.getRet();
        } else {
            throw new RuntimeException(asyncTask.getError());
        }
    }

    public static String escape(String s){
        return s.replace("\\", "\\\\")
                .replace("\t", "\\t")
                .replace("\b", "\\b")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\f", "\\f")
                .replace("'", "\\'")
                .replace("\"", "\\\"");
    }

}
