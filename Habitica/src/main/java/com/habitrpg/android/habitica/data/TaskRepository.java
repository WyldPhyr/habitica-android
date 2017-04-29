package com.habitrpg.android.habitica.data;

import com.habitrpg.android.habitica.models.responses.TaskDirectionData;
import com.habitrpg.android.habitica.models.responses.TaskScoringResult;
import com.habitrpg.android.habitica.models.tasks.ChecklistItem;
import com.habitrpg.android.habitica.models.tasks.RemindersItem;
import com.habitrpg.android.habitica.models.tasks.Task;
import com.habitrpg.android.habitica.models.tasks.TaskList;
import com.habitrpg.android.habitica.models.tasks.TaskTag;
import com.habitrpg.android.habitica.models.tasks.TasksOrder;
import com.habitrpg.android.habitica.models.user.User;

import java.util.List;

import io.realm.Realm;
import io.realm.RealmResults;
import rx.Observable;

public interface TaskRepository extends BaseRepository  {
    Observable<RealmResults<Task>> getTasks(String taskType, String userID);
    Observable<RealmResults<Task>> getTasks(String userId);
    void saveTasks(String userId, TasksOrder order, TaskList tasks);

    Observable<TaskList> refreshTasks(TasksOrder tasksOrder);

    Observable<TaskScoringResult> taskChecked(User user, Task task, boolean up);
    Observable<TaskScoringResult> taskChecked(User user, String taskId, boolean up);
    Observable<Task> scoreChecklistItem(String taskId, String itemId);

    Observable<Task> getTask(String taskId);
    Observable<Task> getTaskCopy(String taskId);
    Observable<Task> createTask(Task task);
    Observable<Task> updateTask(Task task);
    Observable<Void> deleteTask(String taskId);
    void saveTask(Task task);

    Observable<List<Task>> createTasks(List<Task> newTasks);

    void markTaskCompleted(String taskId, boolean isCompleted);

    void saveReminder(RemindersItem remindersItem);

    void executeTransaction(Realm.Transaction transaction);
}
