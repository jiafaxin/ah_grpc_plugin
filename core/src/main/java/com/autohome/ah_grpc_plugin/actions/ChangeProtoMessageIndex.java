package com.autohome.ah_grpc_plugin.actions;

import com.autohome.ah_grpc_plugin.dialogs.InputName;
import com.autohome.ah_grpc_plugin.models.ProtoPath;
import com.autohome.ah_grpc_plugin.services.GitlabApiService;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.protobuf.lang.PbFileType;
import com.intellij.protobuf.lang.psi.*;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.util.PsiTreeUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Collectors;

/**
 * 重排message的索引
 */
public class ChangeProtoMessageIndex extends AnAction {

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


        WriteCommandAction.runWriteCommandAction(project,()->{
            PsiFile pbFile = PsiDocumentManager.getInstance(e.getProject()).getPsiFile(document);
            PsiElement psiElement = pbFile.findElementAt(offset);

            if(psiElement instanceof PbMessageDefinition){

            }else{
                while (true){
                    psiElement = psiElement.getParent();

                    if(psiElement == null)
                        break;

                    if(psiElement instanceof PbMessageDefinition)
                        break;
                }
            }

            PbMessageDefinition message = (PbMessageDefinition) psiElement;

            int i = 1;
            for(PsiElement field : message.getBody().getChildren()){
                PbSimpleField simpleField = null;
                PbMapField mapField = null;
                if(field instanceof PbSimpleField){
                    simpleField = (PbSimpleField)field;
                }else if(field instanceof PbMapField){
                    mapField = (PbMapField)field;
                }
                PsiElement newField = createField(project, simpleField,mapField,i++);
                field.replace(newField);
            }

        });
    }

    public PsiElement createField(Project project,PbSimpleField field,PbMapField mapField,int newNumber){
        //关键字
        PbFieldLabel label = null;
        //jsonname
        PbOptionList optionList = null;
        String typeName = "";
        String name = "";
        if(null != field){
            label = field.getDeclaredLabel();
            optionList = field.getOptionList();
            typeName = field.getTypeName().getText();
            name = field.getName();
        }else{
            label = mapField.getDeclaredLabel();
            optionList = mapField.getOptionList();
            //map<string,string>
            typeName = "map<" + mapField.getKeyType().getText()+","+mapField.getValueType().getText()+">";
            name = mapField.getName();
        }

        //返回
        String content = "";
        //都不为空
        if(null != label && null != optionList){
            content = String.format("message Abc{\n %s %s %s = %s %s;\n}",label.getText(), typeName,name,newNumber+"",optionList.getText());
        }else if(null != label && null == optionList){
            content = String.format("message Abc{\n %s %s %s = %s;\n}",label.getText(), typeName,name,newNumber+"");
        }else if(null == label && null != optionList){
            content = String.format("message Abc{\n %s %s = %s %s;\n}" , typeName,name,newNumber+"",optionList.getText());
        }else{
            content = String.format("message Abc{\n %s %s = %s;\n}" , typeName,name,newNumber+"");
        }
        PbFile file = (PbFile)PsiFileFactory.getInstance(project).createFileFromText("a.proto", PbFileType.INSTANCE,content);
        //基本类型
        if(null != field){
            return PsiTreeUtil.findChildrenOfType(file,PbSimpleField.class).stream().collect(Collectors.toList()).get(0);
        }
        //map类型
        return PsiTreeUtil.findChildrenOfType(file,PbMapField.class).stream().collect(Collectors.toList()).get(0);
    }



}
