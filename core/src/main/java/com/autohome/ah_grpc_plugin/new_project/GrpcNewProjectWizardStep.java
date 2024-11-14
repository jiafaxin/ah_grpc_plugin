package com.autohome.ah_grpc_plugin.new_project;

import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.ide.wizard.NewProjectWizardStep;
import com.intellij.openapi.observable.properties.PropertyGraph;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.UserDataHolder;
import com.intellij.ui.dsl.builder.Panel;
import org.jetbrains.annotations.NotNull;

public class GrpcNewProjectWizardStep implements NewProjectWizardStep {
    @NotNull
    @Override
    public WizardContext getContext() {
        return null;
    }

    @NotNull
    @Override
    public UserDataHolder getData() {
        return null;
    }

    @NotNull
    @Override
    public Keywords getKeywords() {
        return null;
    }

    @NotNull
    @Override
    public PropertyGraph getPropertyGraph() {
        return null;
    }

    @Override
    public void setupProject(@NotNull Project project) {
        NewProjectWizardStep.super.setupProject(project);
    }

    @Override
    public void setupUI(@NotNull Panel builder) {
        NewProjectWizardStep.super.setupUI(builder);
    }
}
