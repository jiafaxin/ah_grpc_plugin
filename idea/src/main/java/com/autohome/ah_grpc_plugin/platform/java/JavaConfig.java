package com.autohome.ah_grpc_plugin.platform.java;

import com.autohome.ah_grpc_plugin.platform.PlatformConfig;
import com.autohome.ah_grpc_plugin.platform.PlatformService;
import com.autohome.ah_grpc_plugin.platform.SupportGRPC;
import com.intellij.ide.wizard.GeneratorNewProjectWizard;
import com.intellij.ide.wizard.GeneratorNewProjectWizardBuilderAdapter;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import org.jetbrains.idea.maven.project.actions.UpdateFoldersAction;
import org.jetbrains.idea.maven.wizards.MavenNewProjectWizardStep;

import java.util.List;

public class JavaConfig implements PlatformConfig {

    Project project;

    public JavaConfig(Project project) {
        this.project = project;
    }


    @Override
    public SupportGRPC getSuppprtGRPC() {
        return new JavaSupportGRPC();
    }

    @Override
    public PlatformService getPlatform() {
        return new JavaPlatformService();
    }

    public boolean isMultiModule(Project project) {
        return getModules(project).size() > 0;
    }

    public boolean hasGrpcModule(Project project) {
        List<MavenProject> modules = getModules(project);
        if(modules.size()==0)
            return false;
        for (MavenProject module : modules) {
            if(module.getDisplayName().equals("grpc"))
                return true;
        }
        return false;
    }

    List<MavenProject> getModules(Project project){
        MavenProjectsManager manager = MavenProjectsManager.getInstance(project);
        return manager.getModules(manager.getRootProjects().get(0));
    }



}
