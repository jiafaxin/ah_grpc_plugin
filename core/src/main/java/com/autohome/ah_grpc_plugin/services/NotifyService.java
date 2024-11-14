package com.autohome.ah_grpc_plugin.services;

import com.autohome.ah_grpc_plugin.Config;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;

public class NotifyService {
    public static void info(Project project, String content){
        notify(project,content,NotificationType.INFORMATION);
    }


    public static void error(Project project,String content){
        notify(project,content,NotificationType.ERROR);
    }

    public static void warning(Project project,String content){
        notify(project,content,NotificationType.WARNING);
    }

    public static void notify(Project project,String content,NotificationType notificationType){
        NotificationGroupManager.getInstance()
                .getNotificationGroup("com.autohome.ah_grpc_plugin")
                .createNotification(content, notificationType)
                .notify(project);
    }
}
