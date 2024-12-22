import java.util.UUID;
import java.util.concurrent.Future;

public class ExecutorRunner {
    public static void main(String[] args) {

        TaskExecutorImpl executor = new TaskExecutorImpl(5);

        Main.TaskGroup taskGroup = new Main.TaskGroup(UUID.randomUUID());

               Future<String> future = executor.submitTask(
                new Main.Task<>(
                        UUID.randomUUID(),
                        taskGroup,
                        Main.TaskType.READ,
                        () -> {
                            Thread.sleep(1000);
                            return "Task Completed!";
                        }
                )
        );

        try {
            String result = future.get();
            System.out.println("Task Result: " + result);
        } catch (Exception e) {
            e.printStackTrace();
        }

        executor.shutdown();
    }
}
