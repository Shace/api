package Utils;

import java.util.ArrayList;
import java.util.List;

import models.Event;
import models.Media;
import play.libs.F.Function0;
import play.libs.F.Promise;
import controllers.Buckets;

public class BucketsUpdater {

    public class Task {
        public Event event;
        public Media media;
        
        public Task(Event event, Media media) {
            this.event = event;
            this.media = media;
        }
    }
    
    private static final BucketsUpdater instance = new BucketsUpdater(); 
    private List<Task>      tasks;
    private List<String>    events;
    
    Object tasksLock = new Object();
    Object eventsLock = new Object();

    private void releaseEvent(String id) {
        synchronized (eventsLock) {
            events.remove(id);
        }
        execTask();
    }
    
    private void addTask(Task task) {
        synchronized (tasksLock) {
            tasks.add(task);
        }
        execTask();
    }
    
    private void execTask() {
        synchronized (eventsLock) {
            synchronized (tasksLock) {
                for (final Task task : tasks) {
                    if (!events.contains(task.event.id)) {
                        events.add(task.event.id);
                        tasks.remove(task);
                        // Run task
                        Promise.promise(
                                new Function0<Integer>() {
                                    @Override
                                    public Integer apply() throws Throwable {
                                        try {
                                            Buckets.addNewMediaToEvent(task.event, task.media);
                                        } finally {
                                            releaseEvent(task.event.id);
                                        }
                                        return 0;
                                    }
                                });
                        break;
                    }
                }
            }
        }
    }
    
    public static BucketsUpdater get() { 
        return instance; 
    }
    
    private BucketsUpdater() {
        tasks = new ArrayList<>();
        events = new ArrayList<>();
    }
    
    
    public void updateBucket(Event event, Media media) {
        this.addTask(new Task(event, media));
    }
}
