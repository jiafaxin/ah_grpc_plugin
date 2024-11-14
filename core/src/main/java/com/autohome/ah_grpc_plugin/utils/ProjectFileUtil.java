package com.autohome.ah_grpc_plugin.utils;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ProjectFileUtil {

    public static List<Project> getFileProjects(VirtualFile virtualFile){
        return Arrays.stream(ProjectManager.getInstance().getOpenProjects()).filter(x->{
            return ProjectRootManager.getInstance(x).getFileIndex().isInContent(virtualFile);
        }).collect(Collectors.toList());
    }


}
