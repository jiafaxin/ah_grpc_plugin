package com.autohome.ah_grpc_plugin.utils;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessHandlerFactory;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.util.ExecUtil;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class CommandUtils {

    public static String run(List<String> cmds,String workDirectory) {
        try {
            GeneralCommandLine generalCommandLine = createGeneralCommandLine(cmds,workDirectory);
            generalCommandLine.createProcess();
            String result = ExecUtil.execAndGetOutput(generalCommandLine).getStdout();
            return result;
        } catch (ExecutionException e) {
            return "error";
        }
    }

    public static String runBackground(List<String> cmds,String workDirectory,ConsoleView consoleView) {
        try {
            GeneralCommandLine generalCommandLine = createGeneralCommandLine(cmds,workDirectory);
            OSProcessHandler osProcessHandler = ProcessHandlerFactory.getInstance().createColoredProcessHandler(generalCommandLine);
            consoleView.attachToProcess(osProcessHandler);
            osProcessHandler.startNotify();
            osProcessHandler.getProcess().waitFor();
            return "";
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    static GeneralCommandLine createGeneralCommandLine(List<String> cmds,String workDirectory){
        List<String> cmd = new ArrayList<>();
        if(OsUtil.isWindows()) {
            cmd.add(OsUtil.baseExePath());
        }
        for (String c : cmds) {
            cmd.add(c);
        }
        GeneralCommandLine generalCommandLine = new GeneralCommandLine(cmd);
        generalCommandLine.withWorkDirectory(workDirectory);
        generalCommandLine.setCharset(Charset.forName("UTF-8"));
        return generalCommandLine;
    }

}
