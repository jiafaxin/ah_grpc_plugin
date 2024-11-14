package com.autohome.ah_grpc_plugin.providers;

import com.autohome.ah_grpc_plugin.editors.AuthEditor;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class AuthProvider implements FileEditorProvider, DumbAware {

    static final String EDITOR_TYPE_ID = "AUTH_FILE_PROVIDER";

    @Override
    public boolean accept(@NotNull Project project, @NotNull VirtualFile file) {
        if(!(file instanceof LightVirtualFile))
            return false;
        return file.getName().endsWith(".config");
    }

    @Override
    public @NotNull FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile file) {
        return new AuthEditor(project, file);
    }

    @Override
    public @NotNull @NonNls String getEditorTypeId() {
        return EDITOR_TYPE_ID;
    }

    @Override
    public @NotNull FileEditorPolicy getPolicy() {
        return FileEditorPolicy.HIDE_DEFAULT_EDITOR;
    }
}
