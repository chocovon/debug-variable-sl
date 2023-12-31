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
import com.sun.jdi.Value;
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

public class PluginSaveLoader {
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

    public static void save(XValueNodeImpl node) throws ClassNotLoadedException, EvaluateException, IOException, InvocationException, IncompatibleThreadStateException, InvalidTypeException, SaveValueInnerException, JsonSerializeException, StackFrameThreadException {
        NodeComponents comp = new NodeComponents(node);

        ObjectReference saveMethod = VmMethodService.getSaveMethod(comp);
        VariableInfo vi = comp.variableInfo;

        if (comp.isPrimitive()) {
            vi.setVal(comp.getPrimitive());
            writeSavedMeta(vi);
        } else {
            ObjectReference obj = (ObjectReference) comp.value;
            if (obj == null) {
                throw new InvalidTypeException("Cannot save null value");
            }
            vi.setVal(getObjString(obj, comp.thread));

            ToolMessage retMessage = saveObject(comp, saveMethod);
            if (retMessage.getStatus() == OK) {
                FileUtil.saveFile(retMessage.getKryo(), DEFAULT_PATH_ABSOLUTE + vi.getId());
                FileUtil.saveFile(retMessage.getJson(), DEFAULT_PATH_ABSOLUTE + vi.getId() + JSON_SUFFIX);
                writeSavedMeta(vi);
            } else if (retMessage.getStatus() == KRYO_ERROR) {
                FileUtil.appendToFile(retMessage.getErrStackTrace(), DEFAULT_PATH_ABSOLUTE + vi.getId() + ERR_SUFFIX);
                throw new SaveValueInnerException(retMessage.getErrMsg());
            } else if (retMessage.getStatus() == JSON_ERROR) {
                FileUtil.saveFile(retMessage.getKryo(), DEFAULT_PATH_ABSOLUTE + vi.getId());
                FileUtil.appendToFile(retMessage.getErrStackTrace(), DEFAULT_PATH_ABSOLUTE + vi.getId() + ERR_SUFFIX);
                writeSavedMeta(vi);
                throw new JsonSerializeException(retMessage.getErrMsg());
            }
        }
    }

    private static String getObjString(ObjectReference obj, ThreadReference thread) {
        String objStr;
        try {
            objStr = ((StringReference) ValueUtil.invokeMethod(obj,
                    "toString", thread)).value();
        } catch (Throwable e) {
            objStr = obj.toString();
        }
        return objStr;
    }

    private static ToolMessage saveObject(NodeComponents comp, ObjectReference saveMethod) throws InvocationException, InvalidTypeException, ClassNotLoadedException, IncompatibleThreadStateException {
        ObjectReference msg = (ObjectReference) ValueUtil.invokeMethod(saveMethod, "invoke", comp.thread, null, comp.value);

        return new ToolMessage(
                ((StringReference) msg.getValue(msg.referenceType().fieldByName("status"))).value(),
                ((StringReference) msg.getValue(msg.referenceType().fieldByName("kryo"))).value(),
                ((StringReference) msg.getValue(msg.referenceType().fieldByName("json"))).value()
        );
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

    private static void writeSavedMeta(VariableInfo vi) throws IOException {
        FileUtil.appendToFile(vi.genMeta(), DEFAULT_PATH_ABSOLUTE + META_NAME);
    }

    public static void load(NodeComponents comp, SavedValue savedValue) throws InvalidTypeException, ClassNotLoadedException, InvocationException, IncompatibleThreadStateException, StackFrameThreadException, IOException {
        ObjectReference loadMethod = VmMethodService.getLoadMethod(comp);

        Value loaded = null;
        String errMsg = null;
        if (savedValue.isPrimitive()) {
            loaded = ValueUtil.valueOfPrimitive(comp.vm, savedValue.getVal(), savedValue.getType());
        } else {
            ObjectReference loadMessage = sendBytesToVm(comp, savedValue, loadMethod);

            String loadStatus = ((StringReference) loadMessage.getValue(loadMessage.referenceType().fieldByName("status"))).value();
            if (loadStatus.equals("ok")) {
                loaded = loadMessage.getValue(loadMessage.referenceType().fieldByName("object"));
            } else {
                String errStackTrace = ((StringReference) loadMessage.getValue(loadMessage.referenceType().fieldByName("err"))).value();
                errMsg = errStackTrace.split("\n", 2)[0];
                FileUtil.appendToFile(errStackTrace, DEFAULT_PATH_ABSOLUTE + savedValue.getId() + ERR_SUFFIX);
            }
        }
        if (loaded == null) {
            throw new InvalidTypeException("VM load value failed: " + errMsg);
        }

        comp.setValue(loaded);
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

    private static ObjectReference sendBytesToVm(NodeComponents comp, SavedValue savedValue, ObjectReference loadMethod) throws IOException, InvocationException, InvalidTypeException, ClassNotLoadedException, IncompatibleThreadStateException {
        String kryoIsoString = FileUtil.readBytesFileAsISOString(DEFAULT_PATH_ABSOLUTE + savedValue.getId());
        StringReference kryoStringInVm = comp.vm.mirrorOf(kryoIsoString);
        return (ObjectReference) ValueUtil.invokeMethod(loadMethod, "invoke",
                comp.thread, null, kryoStringInVm);
    }
}
