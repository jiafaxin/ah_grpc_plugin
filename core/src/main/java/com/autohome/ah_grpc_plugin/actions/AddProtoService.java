package com.autohome.ah_grpc_plugin.actions;

import com.autohome.ah_grpc_plugin.dialogs.CreateMessageFromJsonDialog;
import com.autohome.ah_grpc_plugin.dialogs.InputName;
import com.autohome.ah_grpc_plugin.models.ProtoPath;
import com.autohome.ah_grpc_plugin.services.GitlabApiService;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.protobuf.lang.PbFileType;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * 添加一个Service
 */
public class AddProtoService extends AnAction {

    @Override
    public void update(@NotNull AnActionEvent e) {
        VirtualFile file = e.getData(PlatformDataKeys.VIRTUAL_FILE);
        if(file.getFileType() instanceof PbFileType){
            e.getPresentation().setVisible(true);
        }else{
            e.getPresentation().setVisible(false);
        }
    }

    @Override
    public void actionPerformed(AnActionEvent e) {

        Project project = e.getProject();

        // 获取编辑器
        Editor editor = e.getData(PlatformDataKeys.EDITOR);
        VirtualFile file = e.getData(PlatformDataKeys.VIRTUAL_FILE);
        // 代表光标所在位置
        CaretModel caretModel = editor.getCaretModel();
        //代表当前文件
        Document document = editor.getDocument();
        //光标选中位置
        int offset = caretModel.getOffset();

//        PsiFile pbFile = PsiDocumentManager.getInstance(e.getProject()).getPsiFile(document);
//        PsiElement psiElement = pbFile.findElementAt(offset);


        String temp = "" +
                "/**\n" +
                "  * 【请输入服务说明】\n" +
                "  * 维护人: ${author}\n" +
                "  * 生产环境 - 主机: ${path}.grpc.corpautohome.com\n" +
                "  * 预发环境 - 主机: ${path}.thallo.corpautohome.com\n" +
                "  * 测试环境 - 主机: ${path}.terra.corpautohome.com\n" +
                " */\n" +
                "service ${ServiceName} {\n\n" +
                "}\n\n";

        InputName.getInstance(project)
                .setPattern("[A-Z][a-zA-Z0-9]{0,}", "以大写字符开头，只能包含字母和数字")
                .setTitle("请输入服务名")
                .showAndGet(name -> {
            ProtoPath path = ProtoPath.newInstance(file.getPath());
            String author = GitlabApiService.getCurrentUser();
            String body = temp
                    .replace("${path}", path.getDomian())
                    .replace("${ServiceName}", name)
                    .replace("${author}", StringUtils.isBlank(author) ? "【请输入维护人】" : author);

                    WriteCommandAction.runWriteCommandAction(project,()->{
                        document.insertString(offset, body);
                    });
        });
    }


}
