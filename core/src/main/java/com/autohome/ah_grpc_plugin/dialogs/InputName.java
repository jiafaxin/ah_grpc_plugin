package com.autohome.ah_grpc_plugin.dialogs;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.openapi.ui.popup.*;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * 输入名称
 */
public class InputName {

    public static InputName getInstance(Project project){
        return new InputName(project);
    }

    Project project;
    String pattern;
    String node;
    String title;

    public InputName(Project project){
        this.project = project;
    }

    public InputName setPattern(String pattern,String node){
        this.pattern = pattern;
        this.node = node;
        return this;
    }

    public InputName setTitle(String title){
        this.title = title;
        return this;
    }

    JBPopup popup;

    JPanel box = new JPanel(new VerticalFlowLayout(0, 0));

    MatteBorder errorBorder = new MatteBorder(2,2,2,2,new Color(114,82,82));

    JBTextField fileNameTextFiled = new JBTextField("");

    public void showAndGet(Consumer<String> callback){

        box.add(fileNameTextFiled);

        KeyAdapter adapter = getSelectKeyAdapter();
        fileNameTextFiled.addKeyListener(adapter);

        if(StringUtils.isNotBlank(node)) {
            JBLabel label = new JBLabel(this.node);
            label.setBorder(new EmptyBorder(5, 5, 5, 5));
            label.setForeground(Color.GRAY);
            box.add(label);
        }

        ComponentPopupBuilder builder = JBPopupFactory.getInstance().createComponentPopupBuilder(box, fileNameTextFiled)
                .setFocusable(true)
                .setMovable(true)
                .setProject(project)
                .setRequestFocus(true).setMinSize(new Dimension(360, 0));

        if(StringUtils.isNotBlank(title)){
            builder.setTitle(title);
        }

         popup = builder.createPopup();

        popup.addListener(new JBPopupListener() {
            @Override
            public void onClosed(@NotNull LightweightWindowEvent event) {
                JBPopupListener.super.onClosed(event);
                if (!isMatch()) {
                    return;
                }
                JBPopupListener.super.onClosed(event);
                String name = fileNameTextFiled.getText();
                callback.accept(name);
            }
        });
        popup.showCenteredInCurrentWindow(project);
    }


    public boolean isMatch(){
        String name = fileNameTextFiled.getText();
        if(StringUtils.isBlank(name)) {
            fileNameTextFiled.setBorder(errorBorder);
            return false;
        }
        boolean isMatch = Pattern.matches(pattern, name);
        if(!isMatch){
            fileNameTextFiled.setBorder(errorBorder);
            return false;
        }
        fileNameTextFiled.setBorder(new EmptyBorder(2,2,2,2));
        return true;
    }

    KeyAdapter getSelectKeyAdapter() {
        return new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                super.keyPressed(e);
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_ENTER:
                        if (isMatch()) {
                            popup.closeOk(null);
                        }
                        return;
                }
            }
        };
    }

}
