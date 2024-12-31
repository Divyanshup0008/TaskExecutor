import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;

public class ExecutorRunner {
    public static void main(String[] args) {

        TaskExecutorImpl executor = new TaskExecutorImpl(5);

        // Number of tasks to generate
        int numTasks = 20;
        // Generate random task groups
        List<Main.TaskGroup> taskGroups = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            taskGroups.add(new Main.TaskGroup(UUID.randomUUID()));
        }

        // Create and submit random tasks
        List<Future<String>> futures = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(numTasks); // Latch to track task completion
        Random random = new Random();
        for (int i = 0; i < numTasks; i++) {
            Main.TaskGroup taskGroup = taskGroups.get(random.nextInt(taskGroups.size()));

            // Create a task with random delay and actions
            Main.Task<String> task = new Main.Task<>(
                    UUID.randomUUID(),
                    taskGroup,
                    Main.TaskType.READ,
                    () -> {
                        int delay = random.nextInt(2000) + 500 ; // Random delay between 500ms and 2500ms
                        Thread.sleep(delay);
                        return String.format("Task in group %s completed after %d ms",
                                taskGroup.groupUUID(), delay);
                    }
            );

            // Submit the task and store its future
            futures.add(executor.submitTask(task));
        }

        // Wait for all tasks to complete and print their results
        for (Future<String> future : futures) {
            try {
                latch.await(); // Wait for all tasks to finish
                System.out.println(future.get());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Shut down the executor
        executor.shutdown();
    }
}