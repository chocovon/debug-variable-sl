package util;

import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.xdebugger.impl.ui.tree.nodes.XValueNodeImpl;
import com.sun.jdi.*;
import data.VariableInfo;
import data.tool.ToolMessage;
import util.debugger.NodeComponents;
import util.debugger.ValueUtil;
import util.debugger.VmMethodService;
import util.exception.JsonSerializeException;
import util.exception.SaveValueInnerException;
import util.exception.StackFrameThreadException;
import util.file.FileUtil;

import java.io.IOException;

import static config.Config.*;
import static data.tool.Status.*;

public class PluginValueSaver {
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

    private static ToolMessage saveObject(NodeComponents comp, ObjectReference saveMethod) throws InvocationException, InvalidTypeException, ClassNotLoadedException, IncompatibleThreadStateException {
        ObjectReference msg = (ObjectReference) ValueUtil.invokeMethod(saveMethod, "invoke", comp.thread, null, comp.value);

        return new ToolMessage(
                ((StringReference) msg.getValue(msg.referenceType().fieldByName("status"))).value(),
                ((StringReference) msg.getValue(msg.referenceType().fieldByName("kryo"))).value(),
                ((StringReference) msg.getValue(msg.referenceType().fieldByName("json"))).value()
        );
    }

    private static void writeSavedMeta(VariableInfo vi) throws IOException {
        FileUtil.appendToFile(vi.genMeta(), DEFAULT_PATH_ABSOLUTE + META_NAME);
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
}
