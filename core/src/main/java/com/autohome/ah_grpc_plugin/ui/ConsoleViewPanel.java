package com.autohome.ah_grpc_plugin.ui;

import com.intellij.execution.filters.TextConsoleBuilder;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBPanel;

import java.awt.*;

public class ConsoleViewPanel extends JBPanel {

    private ConsoleView view;

    public ConsoleView getView() {
        return view;
    }

    public void setView(ConsoleView view) {
        this.view = view;
    }

    public ConsoleViewPanel(Project project){
        super();
        try {
            TextConsoleBuilder consoleBuilder = TextConsoleBuilderFactory.getInstance().createBuilder(project);
            ConsoleView console = consoleBuilder.getConsole();
            setLayout(new BorderLayout());
            add(console.getComponent(), BorderLayout.CENTER);
            setView(console);
        }catch (Exception e){

        }
    }
}
