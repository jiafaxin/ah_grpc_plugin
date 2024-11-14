package com.autohome.ah_grpc_plugin.new_project;

import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.ide.wizard.GeneratorNewProjectWizard;
import com.intellij.ide.wizard.GeneratorNewProjectWizardBuilderAdapter;
import com.intellij.ide.wizard.NewProjectWizardStep;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class GrpcGeneratorNewProjectWizard implements GeneratorNewProjectWizard {

    @Nullable
    @Override
    public String getDescription() {
        return "getDescription";
    }

    @Nullable
    @Override
    public String getGroupName() {
        return "abc";
    }

    @NotNull
    @Override
    public Icon getIcon() {
        return null;
    }

    @NotNull
    @Override
    public String getId() {
        return "abc";
    }

    @NotNull
    @Override
    public String getName() {
        return "abc";
    }

    @NotNull
    @Override
    public NewProjectWizardStep createStep(@NotNull WizardContext wizardContext) {
        return new GrpcNewProjectWizardStep();
    }

    public static class Builder extends   GeneratorNewProjectWizardBuilderAdapter{
        public Builder() {
            super(new GrpcGeneratorNewProjectWizard());
        }
    }
}
