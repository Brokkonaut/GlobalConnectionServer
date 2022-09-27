package de.cubeside.globalserver.event;

import java.lang.reflect.Method;

class ReflectionEventHandler<E extends Event> extends AbstractEventHandler<E> {
    private final Method method;
    private final Object instance;

    public ReflectionEventHandler(Class<E> eventClass, int priority, Object instance, Method method) {
        super(eventClass, priority);
        this.instance = instance;
        this.method = method;
    }

    @Override
    public void handle(E event) {
        try {
            method.invoke(instance, event);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int hashCode() {
        return method.hashCode() ^ (instance == null ? 0 : instance.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof ReflectionEventHandler)) {
            return false;
        }
        ReflectionEventHandler<?> other = (ReflectionEventHandler<?>) obj;
        return other.instance == instance && other.method.equals(method);
    }
}
