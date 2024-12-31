import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class TaskExecutorImpl implements Main.TaskExecutor {

    private final ExecutorService executorService;
    private final Map<UUID, Semaphore> taskGroupLocks = new ConcurrentHashMap<>();
    private final BlockingQueue<Runnable> taskQueue = new LinkedBlockingQueue<>();
    private final AtomicInteger runningTasks = new AtomicInteger(0);
    private final int maxConcurrency;

    public TaskExecutorImpl(int maxConcurrency) {
        this.maxConcurrency = maxConcurrency;
        this.executorService = Executors.newCachedThreadPool();
        startTaskProcessing();
    }

    @Override
    public <T> Future<T> submitTask(Main.Task<T> task) {
        Objects.requireNonNull(task, "Task must not be null");

        Callable<T> wrappedTask = () -> {
            Semaphore groupLock = taskGroupLocks.computeIfAbsent(task.taskGroup().groupUUID(), key -> new Semaphore(1));
            System.out.println("Task " + task.taskUUID() + " waiting for group " + task.taskGroup().groupUUID());
            groupLock.acquire();
            try {
                System.out.println("Task " + task.taskUUID() + " running for group " + task.taskGroup().groupUUID());
                return task.taskAction().call();
            } finally {
                System.out.println("Task " + task.taskUUID() + " finished for group " + task.taskGroup().groupUUID());
                groupLock.release();
            }
        };

        TaskWrapper<T> taskWrapper = new TaskWrapper<>(wrappedTask);
        taskQueue.offer(taskWrapper);
        return taskWrapper.getFuture();
    }
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
    }

    private void startTaskProcessing() {
        new Thread(() -> {
            while (true) {
                try {
                    if (runningTasks.get() < maxConcurrency) {
                        Runnable task = taskQueue.poll(100, TimeUnit.MILLISECONDS);
                        if (task != null) {
                            runningTasks.incrementAndGet();
                            executorService.execute(() -> {
                                try {
                                    task.run();
                                } finally {
                                    runningTasks.decrementAndGet();
                                }
                            });
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }).start();
    }

    private static class TaskWrapper<T> implements Runnable {
        private final Callable<T> task;
        private final CompletableFuture<T> future = new CompletableFuture<>();

        public TaskWrapper(Callable<T> task) {
            this.task = task;
        }

        @Override
        public void run() {
            try {
                T result = task.call();
                future.complete(result);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        }

        public Future<T> getFuture() {
            return future;
        }
    }
}