package com.autohome.ah_grpc_plugin.actions;

import com.autohome.ah_grpc_plugin.dialogs.CreateMessageFromJsonDialog;
import com.autohome.ah_grpc_plugin.enums.NodeType;
import com.autohome.ah_grpc_plugin.services.GrpcConfigService;
import com.autohome.ah_grpc_plugin.utils.PathUtils;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.protobuf.lang.PbFileType;
import com.intellij.psi.impl.file.PsiDirectoryImpl;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * 把Json转为message
 */
public class CreateMessageFromJson extends AnAction {

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

        // 获取编辑器
        Editor editor = e.getData(PlatformDataKeys.EDITOR);
        // 代表光标所在位置
        CaretModel caretModel = editor.getCaretModel();
        //代表当前文件
        Document document = editor.getDocument();
        //光标选中位置
        int offset = caretModel.getOffset();
        //选中位置所在行
        int line = document.getLineNumber(offset);
        //选中行所在位置，供insertString使用
        int lineStartOffset = document.getLineStartOffset(line);


        new CreateMessageFromJsonDialog(e.getProject(),document,lineStartOffset).showAndGet();
    }
}
