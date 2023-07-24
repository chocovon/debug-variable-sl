package util.debugger;

import com.intellij.debugger.DebuggerInvocationUtil;
import com.intellij.debugger.DebuggerManagerEx;
import com.intellij.debugger.engine.JavaValue;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.evaluation.EvaluationContextImpl;
import com.intellij.debugger.engine.events.DebuggerCommandImpl;
import com.intellij.debugger.impl.DebuggerContextImpl;
import com.intellij.debugger.impl.DebuggerSession;
import com.intellij.debugger.jdi.LocalVariableProxyImpl;
import com.intellij.debugger.jdi.StackFrameProxyImpl;
import com.intellij.debugger.jdi.VirtualMachineProxyImpl;
import com.intellij.debugger.ui.impl.watch.*;
import com.intellij.openapi.project.Project;
import com.intellij.ui.AppUIUtil;
import com.intellij.xdebugger.XDebuggerManager;
import com.intellij.xdebugger.evaluation.XDebuggerEvaluator;
import com.intellij.xdebugger.frame.XValue;
import com.intellij.xdebugger.impl.XDebuggerUtilImpl;
import com.intellij.xdebugger.impl.ui.tree.XDebuggerTree;
import com.intellij.xdebugger.impl.ui.tree.XDebuggerTreeState;
import com.intellij.xdebugger.impl.ui.tree.nodes.XValueNodeImpl;
import com.sun.jdi.*;
import data.VariableInfo;
import util.debugger.idea.OldLocalVariablesUtil;
import util.exception.LoadValueInnerException;
import util.exception.StackFrameThreadException;
import util.reflect.ReflectUtil;
import util.thread.AsyncTask;

import static util.thread.DebuggerThreadUtils.invokeOnDebuggerThread;

public class NodeComponents {
    public boolean isPrimitive() {
        return this.valueDescriptor.getType() instanceof PrimitiveType;
    }

    public String getPrimitive() {
        return this.value == null ? null : this.value.toString();
    }

    private enum DesType {
        LOCAL,
        FIELD,
        ARRAY_ELE,
        ARG,
        EVAL,
        UNKNOWN
    }

    private DesType desType = DesType.UNKNOWN;

    public XValueNodeImpl node;

    ValueDescriptorImpl valueDescriptor;
    LocalVariableProxyImpl localVariableProxy;
    FieldDescriptorImpl fieldDescriptor;
    ArrayElementDescriptorImpl arrayElementDescriptor;
    ArgumentValueDescriptorImpl argumentValueDescriptor;
    EvaluationDescriptor evaluationDescriptor;

    Project project;
    DebuggerContextImpl debuggerContext;
    EvaluationContextImpl evalContext;
    public ThreadReference thread;
    Value value;
    VirtualMachine vm;
    XDebuggerEvaluator evaluator;
    StackFrameProxyImpl frameProxy;

    public VariableInfo variableInfo;

    public NodeComponents(XValueNodeImpl node) throws StackFrameThreadException, EvaluateException, ClassNotLoadedException {
        this.node = node;

        XValue xValue = node.getValueContainer();
        this.valueDescriptor = ((JavaValue) xValue).getDescriptor();

        if (this.valueDescriptor instanceof LocalVariableDescriptorImpl) {
            this.localVariableProxy = ((LocalVariableDescriptorImpl) this.valueDescriptor).getLocalVariable();
            this.desType = DesType.LOCAL;
        } else if (this.valueDescriptor instanceof FieldDescriptorImpl) {
            this.fieldDescriptor = (FieldDescriptorImpl) this.valueDescriptor;
            this.desType = DesType.FIELD;
        } else if (this.valueDescriptor instanceof ArrayElementDescriptorImpl) {
            this.arrayElementDescriptor = (ArrayElementDescriptorImpl) this.valueDescriptor;
            this.desType = DesType.ARRAY_ELE;
        } else if (this.valueDescriptor instanceof ArgumentValueDescriptorImpl) {
            this.argumentValueDescriptor = (ArgumentValueDescriptorImpl) this.valueDescriptor;
            this.desType = DesType.ARG;
        } else if (this.valueDescriptor instanceof EvaluationDescriptor) {
            this.evaluationDescriptor = (EvaluationDescriptor) this.valueDescriptor;
            this.desType = DesType.EVAL;
        }

        this.evalContext = valueDescriptor.getStoredEvaluationContext();
        this.project = this.evalContext.getProject();
        this.debuggerContext = DebuggerManagerEx.getInstanceEx(this.project).getContext();

        this.frameProxy = this.evalContext.getFrameProxy();
        this.thread = invokeOnDebuggerThread(() -> (ThreadReference) this.frameProxy
                .threadProxy().getObjectReference(), this.evalContext);

        this.evaluator = XDebuggerManager.getInstance(this.project).getCurrentSession()
                .getDebugProcess().getEvaluator();

        this.vm = this.thread.virtualMachine();
        this.value = this.valueDescriptor.getValue();

        this.variableInfo = invokeOnDebuggerThread(() -> {
            VariableInfo vi = new VariableInfo();
            vi.setName(varName());
            vi.setType(leftTypeName());
            vi.setSource(frameProxy.location().sourceName());
            vi.setProject(project.getName());
            vi.setId(String.valueOf(System.currentTimeMillis()));
            return vi;
        }, this.evalContext);
    }

    public void setValue(Value value) throws StackFrameThreadException {
        AsyncTask<Object> t = new AsyncTask<Object>() {
            @Override
            protected void asyncCodeRun() {
                evalContext.getDebugProcess().getManagerThread().invoke(new DebuggerCommandImpl() {
                    @Override
                    protected void action() {
                        try {
                            setValueInner(value);
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
        update(debuggerContext);
    }

    private String varName() {
        // avoid returning index as name
        if (this.desType == DesType.ARRAY_ELE) {
            Object parent = node.getParent();
            if (parent instanceof XValueNodeImpl) {
                XValueNodeImpl parentNode = (XValueNodeImpl) parent;
                return parentNode.getName();
            }
        }

        return this.valueDescriptor.getName();
    }

    private String leftTypeName() throws EvaluateException, ClassNotLoadedException {
        switch (this.desType) {
            case ARRAY_ELE:
                return ((ArrayType) this.arrayElementDescriptor.getArray().referenceType()).componentTypeName();
            case EVAL:
                return this.evaluationDescriptor.getModifier().getExpectedType().name();
            default:
                return this.valueDescriptor.getDeclaredType();
        }
    }

    private void setValueInner(Value value) throws EvaluateException, ClassNotLoadedException, InvalidTypeException, LoadValueInnerException {
        switch (desType) {
            case LOCAL:
                this.frameProxy.setValue(this.localVariableProxy, value);
                break;
            case FIELD:
                Field field = this.fieldDescriptor.getField();
                if (!field.isStatic()) {
                    ObjectReference object = this.fieldDescriptor.getObject();
                    if (object != null) {
                        object.setValue(field, value);
                        break;
                    }
                    throw new LoadValueInnerException("Cannot get field object : " + this.fieldDescriptor.getName());
                } else {
                    ReferenceType refType = field.declaringType();
                    if (refType instanceof ClassType) {
                        ClassType classType = (ClassType) refType;
                        classType.setValue(field, value);
                        break;
                    }
                    throw new LoadValueInnerException("Cannot get field declaring type : " + field.name());
                }
            case ARRAY_ELE:
                ArrayReference array = this.arrayElementDescriptor.getArray();
                if (VirtualMachineProxyImpl.isCollected(array)) {
                    throw new LoadValueInnerException("Array has been collected");
                }
                array.setValue(this.arrayElementDescriptor.getIndex(), value);
                break;
            case ARG:
                OldLocalVariablesUtil.setValue(this.debuggerContext.getFrameProxy().getStackFrame(),
                        (Integer) ReflectUtil.getFieldValue(this.argumentValueDescriptor.getVariable(), "mySlot"), value);
                break;
            case EVAL:
                if (this.evaluationDescriptor.canSetValue()) {
                    this.evaluationDescriptor.getModifier().setValue(value);
                } else {
                    throw new LoadValueInnerException("Cannot set value on current evaluation descriptor");
                }
                break;
            default:
                throw new LoadValueInnerException("Unknown value descriptor type");
        }
    }

    private void update(final DebuggerContextImpl context) {
        DebuggerInvocationUtil.swingInvokeLater(context.getProject(), () -> {
            final DebuggerSession session = context.getDebuggerSession();
            if (session != null) {
                session.refresh(false);
            }
        });

        XDebuggerTree tree = this.node.getTree();
        XDebuggerTreeState treeState = XDebuggerTreeState.saveState(tree);

        if (tree.isDetached()) {
            AppUIUtil.invokeOnEdt(() -> tree.rebuildAndRestore(treeState));
        }
        XDebuggerUtilImpl.rebuildAllSessionsViews(this.project);

        //node.setState(context);
    }
}
