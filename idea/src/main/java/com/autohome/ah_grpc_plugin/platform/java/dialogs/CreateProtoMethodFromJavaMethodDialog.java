package com.autohome.ah_grpc_plugin.platform.java.dialogs;

import com.autohome.ah_grpc_plugin.pbe.Languages;
import com.autohome.ah_grpc_plugin.services.NotifyService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.LanguageTextField;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;

public class CreateProtoMethodFromJavaMethodDialog extends DialogWrapper {
    Project project;
    LanguageTextField protoTextField;
    String message = "";

    public CreateProtoMethodFromJavaMethodDialog(Project project, String message) {
        super(project);
        this.message = message;
        this.project = project;
        setTitle("java method to rpc method");
        init();
        setSize(1200,800);
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {

        protoTextField = new LanguageTextField(Languages.PB_LANGUAGE, project, "", false);
        protoTextField.addSettingsProvider(editorBody ->{
            editorBody.setBorder(null);
            editorBody.setBackgroundColor(null);
            editorBody.getSettings().setLineNumbersShown(true);
            editorBody.setVerticalScrollbarVisible(true);
            editorBody.setHorizontalScrollbarVisible(true);
        });
        JBPanel r = new JBPanel(new BorderLayout());
        //r.add(new JBLabel("Message"),BorderLayout.NORTH);
        r.add(new JBScrollPane(protoTextField),BorderLayout.CENTER);
        protoTextField.setText(message);
        return r;
    }

    @Override
    protected void doOKAction() {
        String text = protoTextField.getText();
        if(StringUtils.isNotBlank(text)){

        }

        super.doOKAction();
    }

    protected Action @NotNull [] createActions() {
//        Action createNewProtoAction = new DialogWrapperAction("插入到新的Proto文件") {
//            @Override
//            protected void doAction(ActionEvent e) {
//
//            }
//        };
//        Action updateProtoAction = new DialogWrapperAction("插入到现有Proto文件") {
//            @Override
//            protected void doAction(ActionEvent e) {
//
//            }
//        };
        Action copyAction = new DialogWrapperAction("复制到剪贴板") {
            @Override
            protected void doAction(ActionEvent e) {
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(new StringSelection(message),null);
                NotifyService.info(project,"已复制到剪贴板");
                close(OK_EXIT_CODE);
            }
        };

        return new Action[]{ copyAction,getCancelAction()};
    }

}
