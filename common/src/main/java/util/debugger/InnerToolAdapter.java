package util.debugger;

import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.xdebugger.impl.ui.tree.nodes.XValueNodeImpl;
import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.InvalidTypeException;
import com.sun.jdi.InvocationException;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.StringReference;
import com.sun.jdi.ThreadReference;
import common.GenCodeRequest;
import common.Settings;
import data.VariableInfo;
import data.model.SavedValue;
import data.tool.GenCodeMessage;
import data.tool.ToolMessage;
import util.SerUtil;
import util.exception.JsonSerializeException;
import util.exception.SaveValueInnerException;
import util.exception.StackFrameThreadException;
import util.file.FileUtil;

import java.io.IOException;

import static config.Config.DEFAULT_PATH_ABSOLUTE;
import static config.Config.ERR_SUFFIX;
import static config.Config.JSON_SUFFIX;
import static config.Config.META_NAME;
import static data.tool.Status.JSON_ERROR;
import static data.tool.Status.KRYO_ERROR;
import static data.tool.Status.OK;

public class InnerToolAdapter {
    public static String genJavaCode(XValueNodeImpl node, Settings settings) throws Exception {
        NodeComponents comp = new NodeComponents(node);

        VariableInfo vi = comp.variableInfo;

        String code;

        if (comp.isPrimitive() || comp.value == null) {
            String clazz = vi.getType().replaceAll(".*\\.", "");
            code = clazz + " " + vi.getName() + " = " + comp.getPrimitive() + ";";
        } else {
            ObjectReference genCodeMethod = VmMethodService.getGenCodeMethod(comp);

            GenCodeRequest genCodeRequest = new GenCodeRequest();
            genCodeRequest.setSettings(settings);
            genCodeRequest.setVariableName(vi.getName());
            genCodeRequest.setVariableType(vi.getType());
            GenCodeMessage retMessage = genCodeObject(comp, genCodeMethod, genCodeRequest);

            if (retMessage.getStatus() == OK) {
                code = retMessage.getCode();
            } else {
                code = "// " + retMessage.getErr();
            }
        }

        return code;
    }

    private static GenCodeMessage genCodeObject(NodeComponents comp, ObjectReference genCodeMethod, GenCodeRequest genCodeRequest) throws Exception {
        StringReference genCodeRequestValue = comp.vm.mirrorOf(SerUtil.writeValueAsString(genCodeRequest));

        ObjectReference msg = (ObjectReference) ValueUtil.invokeMethod(genCodeMethod, "invoke",
                comp.thread, null, comp.value, genCodeRequestValue);

        return new GenCodeMessage(
                ((StringReference) msg.getValue(msg.referenceType().fieldByName("status"))).value(),
                ((StringReference) msg.getValue(msg.referenceType().fieldByName("code"))).value(),
                ((StringReference) msg.getValue(msg.referenceType().fieldByName("err"))).value()
        );
    }

    public static String getCode(NodeComponents comp, SavedValue savedValue) throws IOException, InvocationException, InvalidTypeException, ClassNotLoadedException, IncompatibleThreadStateException {
        ObjectReference codeMethod = VmMethodService.getCodeMethod(comp);

        if (savedValue.isPrimitive()) {
            return savedValue.getVal();
        } else {
            ObjectReference codeMessage = sendBytesToVm(comp, savedValue, codeMethod);

            String loadStatus = ((StringReference) codeMessage.getValue(codeMessage.referenceType().fieldByName("status"))).value();
            if (loadStatus.equals("ok")) {
                return ((StringReference) codeMessage.getValue(codeMessage.referenceType().fieldByName("code"))).value();
            } else {
                String errStackTrace = ((StringReference) codeMessage.getValue(codeMessage.referenceType().fieldByName("err"))).value();
                String errMsg = errStackTrace.split("\n", 2)[0];
                FileUtil.appendToFile(errStackTrace, DEFAULT_PATH_ABSOLUTE + savedValue.getId() + ERR_SUFFIX);
                throw new InvalidTypeException("Code generate failed: " + errMsg);
            }
        }
    }

    public static ObjectReference sendBytesToVm(NodeComponents comp, SavedValue savedValue, ObjectReference loadMethod) throws IOException, InvocationException, InvalidTypeException, ClassNotLoadedException, IncompatibleThreadStateException {
        String kryoIsoString = FileUtil.readBytesFileAsISOString(DEFAULT_PATH_ABSOLUTE + savedValue.getId());
        StringReference kryoStringInVm = comp.vm.mirrorOf(kryoIsoString);
        return (ObjectReference) ValueUtil.invokeMethod(loadMethod, "invoke",
                comp.thread, null, kryoStringInVm);
    }
}
