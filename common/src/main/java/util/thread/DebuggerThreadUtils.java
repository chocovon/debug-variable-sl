package util.thread;

import com.intellij.debugger.engine.evaluation.EvaluationContext;
import com.intellij.debugger.engine.managerThread.DebuggerCommand;
import util.exception.StackFrameThreadException;

public class DebuggerThreadUtils {
    public interface SupplierEx<T> {
        T get() throws Exception;
    }

    public static <T> T invokeOnDebuggerThread(SupplierEx<T> invokeOnDebuggerThreadCode,
                                               EvaluationContext evalContext) throws StackFrameThreadException {
        AsyncTask<T> t = new AsyncTask<T>() {
            @Override
            protected void asyncCodeRun() {
                evalContext.getDebugProcess().getManagerThread().invokeCommand(new DebuggerCommand() {
                    @Override
                    public void action() {
                        try {
                            finishRet(invokeOnDebuggerThreadCode.get());
                        } catch (Exception e) {
                            finishError(e);
                        }
                    }

                    @Override
                    public void commandCancelled() {}
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
