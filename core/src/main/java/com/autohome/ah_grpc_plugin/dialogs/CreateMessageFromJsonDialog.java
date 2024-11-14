package com.autohome.ah_grpc_plugin.dialogs;

import com.autohome.ah_grpc_plugin.pbe.Languages;
import com.autohome.ah_grpc_plugin.services.JsonToMessage;
import com.intellij.json.JsonLanguage;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.LanguageTextField;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import javax.swing.*;
import java.awt.*;

/**
 * JSON 转 Message  弹窗
 */
public class CreateMessageFromJsonDialog extends DialogWrapper {

    int offset = 0;

    Document document;

    Project project;
    LanguageTextField jsonTextField;
    LanguageTextField protoTextField;

    public CreateMessageFromJsonDialog(Project project,Document document,int offset) {
        super(project);
        setTitle("Json To Message");
        init();
        setSize(1200,800);
        this.project = project;
        this.offset = offset;
        this.document = document;
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {

        jsonTextField = new LanguageTextField(JsonLanguage.INSTANCE, project, "", false);
        jsonTextField.addSettingsProvider(editorBody ->{
            editorBody.setVerticalScrollbarVisible(true);
            editorBody.setHorizontalScrollbarVisible(true);
            editorBody.setBorder(null);
            editorBody.setBackgroundColor(null);
            editorBody.getSettings().setLineNumbersShown(true);
        });
        jsonTextField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(@NotNull DocumentEvent event) {
                DocumentListener.super.documentChanged(event);
                String json = event.getDocument().getText();
                String message = new JsonToMessage("MessageFromJson", json).toString();
                protoTextField.setText(message);
            }
        });

        JBPanel l = new JBPanel(new BorderLayout());
        l.add(new JBLabel("Json"),BorderLayout.NORTH);
        l.add(jsonTextField,BorderLayout.CENTER);

        protoTextField = new LanguageTextField(Languages.PB_LANGUAGE, project, "", false);
        protoTextField.addSettingsProvider(editorBody ->{
            editorBody.setBorder(null);
            editorBody.setBackgroundColor(null);
            editorBody.getSettings().setLineNumbersShown(true);
            editorBody.setVerticalScrollbarVisible(true);
            editorBody.setHorizontalScrollbarVisible(true);
        });
        JBPanel r = new JBPanel(new BorderLayout());
        r.add(new JBLabel("Message"),BorderLayout.NORTH);
        r.add(protoTextField,BorderLayout.CENTER);


        JBPanel body = new JBPanel(new GridLayout(1,2));
        body.add(l);
        body.add(r);
        return body;
    }

    @Override
    protected void doOKAction() {
        String text = protoTextField.getText();
        if(StringUtils.isNotBlank(text)){
            WriteCommandAction.runWriteCommandAction(project,()->{
                document.insertString(offset,text);
            });
        }

        super.doOKAction();
    }

}
