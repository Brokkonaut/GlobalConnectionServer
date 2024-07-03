package de.cubeside.globalserver.permissions.impl;

import de.cubeside.globalserver.utils.Preconditions;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class PermissionSystem {
    private final ConcurrentHashMap<String, PermissionGroup> groups = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, PermissionUser> users = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<MapWrapperWithCachedHash<HashMap<String, Boolean>>, CalculatedUserPermissions> calculatedUserPerms = new ConcurrentHashMap<>();

    public PermissionGroup getGroup(String name) {
        return groups.get(name);
    }

    public Collection<PermissionGroup> getGroups() {
        return new ArrayList<>(groups.values());
    }

    public PermissionUser createOrEditUser(String name, Consumer<UserEditor> consumer) {
        PermissionUser user = users.computeIfAbsent(name, PermissionUser::new);
        synchronized (user) {
            consumer.accept(new UserEditor(user));
            user.commit();
            HashMap<String, Boolean> perms = user.getDirectPermissions();
            MapWrapperWithCachedHash<HashMap<String, Boolean>> wrappedPerms = new MapWrapperWithCachedHash<>(perms);
            CalculatedUserPermissions calculated;
            synchronized (this) {
                calculated = calculatedUserPerms.computeIfAbsent(wrappedPerms, p -> new CalculatedUserPermissions(p.getMap(), groups));
                user.setCalculatedPermissions(calculated);
            }
        }
        return user;
    }

    public void unloadUser(String name) {
        PermissionUser user = users.remove(name);
        if (user != null) {
            synchronized (this) {
                user.setCalculatedPermissions(null);
            }
        }
    }

    public void editGroups(Consumer<GroupEditor> consumer) throws CircularDependenciesException {
        synchronized (this) {
            GroupEditor editor = new GroupEditor();
            consumer.accept(editor);
            // check for circular dependencies
            try {
                editor.checkDependencies();
            } catch (CircularDependenciesException e) {
                editor.rollback();
                throw e;
            }
            editor.commit();
        }
    }

    public class UserEditor {
        private PermissionUser user;

        public UserEditor(PermissionUser user) {
            this.user = user;
        }

        public void addPermissionToUser(String permission, boolean value) {
            user.addEditorPermission(permission, value);
        }

        public void removePermissionFromUser(String permission) {
            user.removeEditorPermission(permission);
        }

        public void removeAllPermissions(String permission) {
            user.removeEditorAllPermissions(permission);
        }
    }

    public class GroupEditor {
        private HashMap<String, PermissionGroup> editorGroups;
        private HashSet<String> removedGroups;
        private HashSet<String> newGroups;

        public GroupEditor() {
            editorGroups = new HashMap<>(groups);
            removedGroups = new HashSet<>();
            newGroups = new HashSet<>();
        }

        void rollback() {
            for (PermissionGroup group : editorGroups.values()) {
                group.cancelPermissionUpdates();
            }
        }

        void commit() {
            for (PermissionGroup group : editorGroups.values()) {
                group.commitPermissionUpdates(editorGroups);
            }
            groups.putAll(editorGroups);
            Iterator<PermissionGroup> it = groups.values().iterator();
            while (it.hasNext()) {
                PermissionGroup group = it.next();
                if (!editorGroups.containsKey(group.getName())) {
                    it.remove();
                }
            }
            Iterator<CalculatedUserPermissions> it2 = calculatedUserPerms.values().iterator();
            while (it2.hasNext()) {
                CalculatedUserPermissions perms = it2.next();
                if (perms.getUseCounter() == 0) {
                    it2.remove();
                } else {
                    perms.calculate(groups, removedGroups.isEmpty() && newGroups.isEmpty());
                }
            }
        }

        void checkDependencies() throws CircularDependenciesException {
            ArrayDeque<PermissionGroup> groupStack = new ArrayDeque<>();
            for (PermissionGroup group : editorGroups.values()) {
                group.updateDependencies(editorGroups, removedGroups.isEmpty() && newGroups.isEmpty(), groupStack);
                if (!groupStack.isEmpty()) {
                    throw new IllegalStateException("groupStack should be empty");
                }
            }
        }

        public void removeAllGroups() {
            for (String groupName : editorGroups.keySet()) {
                if (!newGroups.remove(groupName)) {
                    removedGroups.add(groupName);
                }
            }
            editorGroups.clear();
        }

        public void removeGroup(String groupName) {
            Preconditions.notNull(groupName, "groupName may not be null");
            if (editorGroups.remove(groupName) != null) {
                if (!newGroups.remove(groupName)) {
                    removedGroups.add(groupName);
                }
            }
        }

        public void createGroup(String groupName) {
            Preconditions.notNull(groupName, "groupName may not be null");
            createGroupInternal(groupName);
        }

        private PermissionGroup createGroupInternal(String groupName) {
            PermissionGroup group = editorGroups.get(groupName);
            if (group == null) {
                group = new PermissionGroup(PermissionSystem.this, groupName);
                editorGroups.put(groupName, group);
                if (!removedGroups.remove(groupName)) {
                    newGroups.add(groupName);
                }
            }
            return group;
        }

        public void addPermissionToGroup(String groupName, String permission, boolean value) {
            Preconditions.notNull(groupName, "groupName may not be null");
            Preconditions.notNull(permission, "permission may not be null");
            PermissionGroup group = createGroupInternal(groupName);
            group.addEditorPermission(permission, value);
        }

        public void removePermissionFromGroup(String groupName, String permission) {
            Preconditions.notNull(groupName, "groupName may not be null");
            Preconditions.notNull(permission, "permission may not be null");
            PermissionGroup group = editorGroups.get(groupName);
            if (group == null) {
                return;
            }
            group.removeEditorPermission(permission);
        }

        public void setGroupPriority(String groupName, int priority) {
            Preconditions.notNull(groupName, "groupName may not be null");
            PermissionGroup group = createGroupInternal(groupName);
            group.setPriority(priority);
        }
    }
}