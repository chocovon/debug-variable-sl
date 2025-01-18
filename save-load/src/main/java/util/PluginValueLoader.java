package util;

import com.intellij.debugger.DebuggerInvocationUtil;
import com.intellij.debugger.engine.events.DebuggerCommandImpl;
import com.intellij.debugger.impl.DebuggerContextImpl;
import com.intellij.debugger.impl.DebuggerSession;
import com.intellij.ui.AppUIUtil;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;
import com.intellij.xdebugger.impl.ui.tree.XDebuggerTree;
import com.intellij.xdebugger.impl.ui.tree.XDebuggerTreeState;
import com.sun.jdi.*;
import data.model.SavedValue;
import util.debugger.NodeComponents;
import util.debugger.ValueUtil;
import util.debugger.VmMethodService;
import util.exception.StackFrameThreadException;
import util.file.FileUtil;
import util.thread.AsyncTask;

import java.io.IOException;
import java.util.Arrays;

import static config.Config.DEFAULT_PATH_ABSOLUTE;
import static config.Config.ERR_SUFFIX;
import static util.debugger.InnerToolAdapter.sendBytesToVm;

public class PluginValueLoader {
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

        setValue(comp, loaded);
    }

    private static void setValue(NodeComponents node, Value value) throws StackFrameThreadException {
        AsyncTask<Object> t = new AsyncTask<Object>() {
            @Override
            protected void asyncCodeRun() {
                node.evalContext.getDebugProcess().getManagerThread().invoke(new DebuggerCommandImpl() {
                    @Override
                    protected void action() {
                        try {
                            node.setNodeValue(value);
                            finishRet(null);
                        } catch (Exception e) {
                            finishError(e);
                        }
                    }
                });
            }
        };
        if (!t.run()) {
            throw new StackFrameThreadException(t.getError());
        }
        refreshDebuggerTreeNode(node);
    }

    private static void refreshDebuggerTreeNode(NodeComponents node) {
        DebuggerContextImpl context = node.debuggerContext;
        DebuggerInvocationUtil.swingInvokeLater(context.getProject(), () -> {
            final DebuggerSession session = context.getDebuggerSession();
            if (session != null) {
                session.refresh(false);
            }
        });

        XDebuggerTree tree = node.node.getTree();
        XDebuggerTreeState treeState = XDebuggerTreeState.saveState(tree);

        if (tree.isDetached()) {
            AppUIUtil.invokeOnEdt(() -> tree.rebuildAndRestore(treeState));
        }

        Arrays.stream(XDebuggerManager.getInstance(node.project).getDebugSessions()).filter(XDebugSession::isSuspended).forEach(XDebugSession::rebuildViews);
    }
}
