package com.autohome.ah_grpc_plugin.services;

import com.autohome.ah_grpc_plugin.models.ApiResult;
import com.autohome.ah_grpc_plugin.utils.HttpClientUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.Date;

public class UpdateService {

    public static boolean needUpdate(Project project) {
        IdeaPluginDescriptor plugin = PluginManagerCore.getPlugin(PluginId.getId("com.autohome.ah_grpc_plugin"));
        if (plugin == null)
            return false;
        String version = PluginManagerCore.getPlugin(PluginId.getId("com.autohome.ah_grpc_plugin")).getVersion();
        try {

            ApiResult<String> lastVersion = HttpClientUtil.get("https://plugins.autohome.com.cn/last_version?v=" + (new SimpleDateFormat("yyyyMMddHHmmss").format(new Date())), new TypeReference<String>() {
            }, null, 1000, "UTF-8").join();
            if (lastVersion.getCode() > 300 || StringUtils.isBlank(lastVersion.getResult())) {
                return false;
            }
            if (version.compareTo(lastVersion.getResult()) >= 0)
                return false;
        }catch (Exception e){

            return false;
        }

        return true;
    }

    public static void show(Project project){
        if(!needUpdate(project)){
            return;
        }
        notify(project);
    }

    public static void notify(Project project) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup("com.autohome.ah_grpc_plugin")
                .createNotification("汽车之家gRPC插件需要更新", NotificationType.ERROR)
                .addAction(new NotificationAction("立即升级") {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                        ShowSettingsUtil.getInstance().showSettingsDialog(project, "Plugins");
                    }
                })
                .notify(project);
    }

}
