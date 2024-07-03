package de.cubeside.globalserver.permissions.impl;

public class PermissionTest {

    public static void main(String[] args) throws CircularDependenciesException {
        PermissionSystem system = new PermissionSystem();
        system.editGroups(editor -> {
            editor.setGroupPriority("group.group1", 10);
            editor.setGroupPriority("group.group2", 20);
            editor.setGroupPriority("group.group3", 30);
            editor.addPermissionToGroup("group.group1", "perm2", true);
            editor.addPermissionToGroup("group.group1", "group.group2", true);
            editor.addPermissionToGroup("group.group1", "group.group3", true);
            editor.addPermissionToGroup("group.group2", "perm1", false);
            editor.addPermissionToGroup("group.group3", "perm1", true);
            // editor.addPermissionToGroup("group.group3", "group.group1", true);
            // editor.addPermissionToGroup("group.group2", "group.group2", true);
        });
        System.out.println("Has perm? " + system.getGroup("group.group1").hasPermission("perm1"));
        PermissionUser u = system.createOrEditUser("Brokkonaut", editor -> {
            // editor.addPermissionToUser("foo", true);
            editor.addPermissionToUser("group.group1", true);
            // editor.removePermissionFromUser("bar");
        });

        System.out.println("Has Brokko perm? " + u.hasPermission("perm1"));

        system.editGroups(editor -> {
            editor.removePermissionFromGroup("group.group1", "group.group3");
        });
        System.out.println("Has Brokko perm? " + u.hasPermission("perm1"));

        system.editGroups(editor -> {
            editor.removePermissionFromGroup("group.group1", "group.group2");
            editor.addPermissionToGroup("group.group1", "group.group3", true);
        });
        System.out.println("Has Brokko perm? " + u.hasPermission("perm1"));
    }
}
