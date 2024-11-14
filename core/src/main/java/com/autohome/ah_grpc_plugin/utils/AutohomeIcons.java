package com.autohome.ah_grpc_plugin.utils;

import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.IconManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public final class AutohomeIcons {

    private static @NotNull Icon load(@NotNull String path, int cacheKey, int flags) {
        return IconManager.getInstance().loadRasterizedIcon(path, AutohomeIcons.class.getClassLoader(), cacheKey, flags);
    }
    public static final @NotNull Icon NewProto = load("icons/newProto.png", 0, 1);
    public static final @NotNull Icon EditProto = load("icons/editProto.png", 0, 1);
    public static final @NotNull Icon DeleteProto = load("icons/deleteProto.png", 0, 1);
    public static final @NotNull Icon OnlineProto = load("icons/onlineProto.png", 0, 1);

    public static final @NotNull Icon ProtoFile = load("icons/protoFile.png", 0, 1);
    public static final @NotNull Icon NewFolder =IconLoader.getIcon("icons/folder.svg", AutohomeIcons.class) ;
    public static final @NotNull Icon BindFolder =IconLoader.getIcon("icons/folderSuccess.svg", AutohomeIcons.class) ;
    public static final @NotNull Icon DeleteFolder =IconLoader.getIcon("icons/deleteFolder.svg", AutohomeIcons.class) ;

    public static final @NotNull Icon ArtifactSmall = IconLoader.getIcon("icons/artifactSmall.svg", AutohomeIcons.class) ;

    public static final @NotNull Icon ArtifactSmallDark = IconLoader.getIcon("icons/artifactSmall_dark.svg", AutohomeIcons.class) ;





}
