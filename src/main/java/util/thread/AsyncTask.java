package util.thread;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public abstract class AsyncTask<T> {
    private T ret;
    private final CountDownLatch countDownLatch = new CountDownLatch(1);
    private Throwable innerThrow;

    public T getRet() {
        return ret;
    }

    public Throwable getError() {
        return innerThrow;
    }

    public boolean run() {
        try {
            asyncCodeRun();
        } catch (Exception e) {
            this.innerThrow = e;
            this.countDownLatch.countDown();
        }
        try {
            if (!this.countDownLatch.await(7, TimeUnit.SECONDS)) {
                this.innerThrow = new TimeoutException();
            }
        } catch (InterruptedException e) {
            this.innerThrow = e;
        }
        return this.innerThrow == null;
    }

    protected abstract void asyncCodeRun();

    protected void finishRet(T ret) {
        this.ret = ret;
        this.countDownLatch.countDown();
    }

    protected void finishError(Throwable innerThrow) {
        this.innerThrow = innerThrow;
        this.countDownLatch.countDown();
    }
}
