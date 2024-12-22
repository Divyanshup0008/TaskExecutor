import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public class Main {

    public enum TaskType {
        READ,
        WRITE,
    }

    public interface TaskExecutor {
        <T> Future<T> submitTask(Task<T> task);
    }

    public record Task<T>(
            UUID taskUUID,
            TaskGroup taskGroup,
            TaskType taskType,
            Callable<T> taskAction
    ) {
        public Task {
            if (taskUUID == null || taskGroup == null || taskType == null || taskAction == null) {
                throw new IllegalArgumentException("All parameters must not be null");
            }
        }
    }

    public record TaskGroup(UUID groupUUID) {
        public TaskGroup {
            if (groupUUID == null) {
                throw new IllegalArgumentException("All parameters must not be null");
            }
        }
    }
}
