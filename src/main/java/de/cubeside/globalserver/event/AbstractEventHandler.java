package de.cubeside.globalserver.event;

public abstract class AbstractEventHandler<E extends Event> {
    private final int priority;
    private final Class<E> eventClass;

    public AbstractEventHandler(Class<E> eventClass, int priority) {
        if (eventClass == null) {
            throw new NullPointerException("eventClass");
        }
        this.eventClass = eventClass;
        this.priority = priority;
    }

    public final int getPriority() {
        return priority;
    }

    public Class<E> getEventClass() {
        return eventClass;
    }

    public abstract void handle(E event);
}
