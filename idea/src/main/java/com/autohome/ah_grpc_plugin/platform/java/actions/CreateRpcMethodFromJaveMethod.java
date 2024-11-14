package com.autohome.ah_grpc_plugin.platform.java.actions;

import com.autohome.ah_grpc_plugin.platform.java.Services.JavaClassToMessage;
import com.autohome.ah_grpc_plugin.platform.java.Services.JavaMethodToProtobuf;
import com.autohome.ah_grpc_plugin.platform.java.dialogs.CreateMessageFromJavaClassDialog;
import com.autohome.ah_grpc_plugin.platform.java.dialogs.CreateProtoMethodFromJavaMethodDialog;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.file.impl.FileManager;
import com.intellij.psi.search.PsiSearchHelper;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

public class CreateRpcMethodFromJaveMethod extends AnAction {

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setVisible(false);
        PsiMethod method = getMethod(e);
        if (method == null)
            return;
        e.getPresentation().setVisible(true);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        PsiMethod method = getMethod(e);
        new CreateProtoMethodFromJavaMethodDialog(e.getProject(), new JavaMethodToProtobuf(method).toString()).showAndGet();
    }

    PsiMethod getMethod(AnActionEvent e){
        VirtualFile file = e.getData(PlatformDataKeys.VIRTUAL_FILE);
        Editor editor = e.getData(PlatformDataKeys.EDITOR);
        PsiElement element = PsiManager.getInstance(e.getProject()).findFile(file).findElementAt(editor.getCaretModel().getOffset());
        e.getPresentation().setVisible(false);
        if(element==null)
            return null;
        while (true) {
            if (element instanceof PsiMethod) {
                return (PsiMethod) element;
            }
            if (element.getParent() == null)
                return null;
            element = element.getParent();
        }
    }
}
