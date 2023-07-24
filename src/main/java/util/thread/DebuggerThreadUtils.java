package util.thread;

import com.intellij.debugger.engine.evaluation.EvaluationContextImpl;
import com.intellij.debugger.engine.events.DebuggerCommandImpl;
import util.exception.StackFrameThreadException;

public class DebuggerThreadUtils {
    public interface SupplierEx<T> {
        T get() throws Exception;
    }

    public static <T> T invokeOnDebuggerThread(SupplierEx<T> invokeOnDebuggerThreadCode,
                                               EvaluationContextImpl evalContext) throws StackFrameThreadException {
        AsyncTask<T> t = new AsyncTask<T>() {
            @Override
            protected void asyncCodeRun() {
                evalContext.getDebugProcess().getManagerThread().invoke(new DebuggerCommandImpl() {
                    @Override
                    protected void action() {
                        try {
                            finishRet(invokeOnDebuggerThreadCode.get());
                        } catch (Exception e) {
                            finishError(e);
                        }
                    }
                });
            }
        };
        if (t.run()) {
            return t.getRet();
        } else {
            throw new StackFrameThreadException(t.getError());
        }
    }

}
