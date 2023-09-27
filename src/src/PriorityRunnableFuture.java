import java.util.Comparator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class PriorityRunnableFuture<T> implements RunnableFuture<T> {
    private final RunnableFuture<T> src;
    private final int priority;

    public PriorityRunnableFuture(RunnableFuture<T> src, int priority) {
        this.src = src;
        this.priority = priority;
    }


    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return src.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return src.isCancelled();
    }

    @Override
    public boolean isDone() {
        return src.isDone();
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        return src.get();
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return src.get(timeout, unit);
    }

    @Override
    public T resultNow() {
        return src.resultNow();
    }

    @Override
    public Throwable exceptionNow() {
        return src.exceptionNow();
    }

    @Override
    public State state() {
        return src.state();
    }

    @Override
    public void run() {
        src.run();
    }

    public int getPriority() {
        return priority;
    }
}

class PriorityRunnableFutureComparator implements Comparator<Runnable> {
    @Override
    public int compare(Runnable o1, Runnable o2) {
        if (o1 == null && o2 == null)
            return 0;
        else if (o1 == null)
            return -1;
        else if (o2 == null)
            return 1;
        else {
            int p1 = ((PriorityRunnableFuture<?>) o1).getPriority();
            int p2 = ((PriorityRunnableFuture<?>) o2).getPriority();
            return Integer.compare(p1, p2);
        }
    }
}

