package util.debugger;

import com.intellij.debugger.engine.JavaValue;
import com.intellij.debugger.engine.evaluation.EvaluationContextImpl;
import com.intellij.debugger.jdi.ThreadReferenceProxyImpl;
import com.intellij.debugger.ui.impl.watch.ValueDescriptorImpl;
import com.intellij.xdebugger.frame.XValue;
import com.intellij.xdebugger.impl.ui.tree.nodes.XValueNodeImpl;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Value;
import common.Settings;
import util.thread.DebuggerThreadUtils;

import java.util.Objects;

public class JsonCodeProvider {

    public static String genJsonString(XValueNodeImpl node, Settings settings) throws Exception {
        XValue xValue = node.getValueContainer();
        ValueDescriptorImpl valueDescriptor = ((JavaValue) xValue).getDescriptor();
        EvaluationContextImpl evalContext = valueDescriptor.getStoredEvaluationContext();
        ThreadReferenceProxyImpl threadProxy = Objects.requireNonNull(evalContext.getFrameProxy()).threadProxy();

        String jsonResult = DebuggerThreadUtils.invokeOnDebuggerThread(() -> {
            ThreadReference thread = (ThreadReference) threadProxy.getObjectReference();
            Value value = valueDescriptor.getValue();
            String toJson = new JsonCodeGenerator(thread, settings).toJson(value);
            return toJson;
        }, evalContext);

        return jsonResult;
    }
}