package com.autohome.ah_grpc_plugin.delegates;

import com.intellij.codeInsight.editorActions.TypedHandlerDelegate;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

public class MyTypedHandlerDelegate extends TypedHandlerDelegate {
    @Override
    public Result charTyped(char c, @NotNull Project project, @NotNull final Editor editor, @NotNull final PsiFile file) {
        return Result.CONTINUE;
    }
}
