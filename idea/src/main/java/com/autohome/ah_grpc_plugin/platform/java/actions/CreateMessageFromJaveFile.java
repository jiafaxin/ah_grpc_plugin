package com.autohome.ah_grpc_plugin.platform.java.actions;

import com.autohome.ah_grpc_plugin.platform.java.Services.JavaClassToMessage;
import com.autohome.ah_grpc_plugin.platform.java.dialogs.CreateMessageFromJavaClassDialog;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;

public class CreateMessageFromJaveFile extends AnAction {

    @Override
    public void update(@NotNull AnActionEvent e) {
        VirtualFile file = e.getData(PlatformDataKeys.VIRTUAL_FILE);
        if(file.getFileType() instanceof JavaFileType){
            e.getPresentation().setVisible(true);
        }else{
            e.getPresentation().setVisible(false);
        }
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        VirtualFile file = e.getData(PlatformDataKeys.VIRTUAL_FILE);
        PsiFile psiFile = PsiManager.getInstance(e.getProject()).findFile(file);

        PsiJavaFile javaFile = (PsiJavaFile) psiFile;
        PsiClass aClass = javaFile.getClasses()[0];


        new CreateMessageFromJavaClassDialog(e.getProject(), new JavaClassToMessage(aClass).toString()).showAndGet();

        System.out.println("123");
    }



}
