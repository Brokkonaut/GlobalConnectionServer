package de.cubeside.globalserver.event;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class EventBus {
    public final static Logger LOGGER = LogManager.getLogger("EventBus");
    private final ConcurrentHashMap<Class<?>, ClassEventHandlerList<?>> eventHandlerLists;

    public EventBus() {
        eventHandlerLists = new ConcurrentHashMap<>(16, 0.75f, 1);
    }

    public <E extends Event> boolean dispatchEvent(E event) {
        @SuppressWarnings("unchecked")
        ClassEventHandlerList<E> list = (ClassEventHandlerList<E>) getEventHandlerList(event.getClass());
        AbstractEventHandler<? super E>[] handler = list.getEventHandlers();
        int l = handler.length;
        for (int i = 0; i < l; i++) {
            try {
                handler[i].handle(event);
            } catch (Throwable t) {
                LOGGER.error("Exception while handling event for " + event.getClass().getTypeName(), t);
            }
        }
        return l > 0;
    }

    @SuppressWarnings("unchecked")
    public <T extends Event> ClassEventHandlerList<T> getEventHandlerList(Class<T> eventClass) {
        ClassEventHandlerList<?> list = eventHandlerLists.get(eventClass);
        if (list != null) {
            return (ClassEventHandlerList<T>) list;
        }
        return addKnownClass(eventClass);
    }

    private synchronized <T extends Event> ClassEventHandlerList<T> addKnownClass(Class<T> eventClass) {
        ClassEventHandlerList<T> list = new ClassEventHandlerList<>(eventClass);
        eventHandlerLists.put(eventClass, list);

        Class<? super T> superclass = eventClass.getSuperclass();
        if (superclass != null && Event.class.isAssignableFrom(superclass)) {
            @SuppressWarnings("unchecked")
            ClassEventHandlerList<? super T> superlist = (ClassEventHandlerList<? super T>) getEventHandlerList((Class<? extends Event>) superclass);
            superlist.addSubclass(list);
            // the superclasses handlers will accept those new subclass events
            for (AbstractEventHandler<? super T> e : superlist.getEventHandlers()) {
                list.registerHandler(e);
            }
        }
        return list;
    }

    public <T extends Event> void registerHandler(AbstractEventHandler<T> handler) {
        getEventHandlerList(handler.getEventClass()).registerHandler(handler);
    }

    @SuppressWarnings("unchecked")
    public <T extends Event> void registerHandlers(Listener listener) {
        for (Method m : listener.getClass().getDeclaredMethods()) {
            EventHandler eventHandlerAnnotation = m.getAnnotation(EventHandler.class);
            if (eventHandlerAnnotation != null) {
                Class<?>[] parameters = m.getParameterTypes();
                int priority = eventHandlerAnnotation.priority();
                if (parameters.length != 1 || !Event.class.isAssignableFrom(parameters[0])) {
                    throw new IllegalArgumentException("EventHandler " + m.getName() + " is invalid. EventHandlers must have exatly 1 argument and it must be a subclass of Event.");
                }
                boolean isStatic = Modifier.isStatic(m.getModifiers());
                m.setAccessible(true);
                registerHandler(new ReflectionEventHandler<>((Class<Event>) parameters[0], priority, isStatic ? null : listener, m));
            }
        }
    }

    public <T extends Event> void removeHandler(AbstractEventHandler<T> handler) {
        getEventHandlerList(handler.getEventClass()).removeHandler(handler);
    }

    @SuppressWarnings("unchecked")
    public <T extends Event> void removeHandlers(Listener listener) {
        for (Method m : listener.getClass().getDeclaredMethods()) {
            EventHandler eventHandlerAnnotation = m.getAnnotation(EventHandler.class);
            if (eventHandlerAnnotation != null) {
                Class<?>[] parameters = m.getParameterTypes();
                int priority = eventHandlerAnnotation.priority();
                if (parameters.length != 1 || !Event.class.isAssignableFrom(parameters[0])) {
                    throw new IllegalArgumentException("EventHandler " + m.getName() + " is invalid. EventHandlers must have exatly 1 argument and it must be a subclass of Event.");
                }
                boolean isStatic = Modifier.isStatic(m.getModifiers());
                m.setAccessible(true);
                removeHandler(new ReflectionEventHandler<>((Class<Event>) parameters[0], priority, isStatic ? null : listener, m));
            }
        }
    }
}
