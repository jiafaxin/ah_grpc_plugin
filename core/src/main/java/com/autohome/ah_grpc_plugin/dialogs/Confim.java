package com.autohome.ah_grpc_plugin.dialogs;

import com.autohome.ah_grpc_plugin.Config;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.function.Consumer;

public class Confim extends DialogWrapper {

    JBLabel contentLabel = new JBLabel();
    public Confim(Project project, String title, String content) {
        super(project);
        setTitle(title);
        init();
        contentLabel.setText(content);
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JBPanel m = new JBPanel(new VerticalFlowLayout());
        m.add(contentLabel);
        return m;
    }
}
