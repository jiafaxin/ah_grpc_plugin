package com.autohome.ah_grpc_plugin.services;

import com.autohome.ah_grpc_plugin.Config;
import com.autohome.ah_grpc_plugin.factory.BufToolWindow;
import com.autohome.ah_grpc_plugin.utils.CommandUtils;
import com.autohome.ah_grpc_plugin.utils.OsUtil;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessHandlerFactory;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.execution.util.ExecUtil;
import com.intellij.ide.actions.SaveAllAction;
import com.intellij.ide.actions.SynchronizeCurrentFileAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.tukaani.xz.rangecoder.RangeEncoderToBuffer;

import java.io.File;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CommandService {

    public static final String lintPassMsg = "[规范检查] .............. 通过";
    public static final String lintNotPassMsg = "[规范检查] .............. 未通过";
    public static final String breakingPassMsg = "[兼容检查] .............. 通过";
    public static final String breakingNotPassMsg = "[兼容检查] .............. 未通过";

    public static final String allPassMsg = "[契约检查] 通过";
    public static final String allNotPassMsg ="[契约检查] 未通过";

    public static final String errorStart = "[异常信息] ";

    static final String success = "success";

    public static CompletableFuture<Boolean> buf(Project project) {
        ConsoleView consoleView = BufToolWindow.getAndShowBufView(project);
        return buf(project,consoleView);
    }

    public static CompletableFuture<Boolean> buf(Project project,ConsoleView consoleView) {

        FileDocumentManager.getInstance().saveAllDocuments();

        ApplicationManager.getApplication().runWriteAction((Computable<Boolean>)()->{
            FileService.delete(project,"buf.yaml");
            FileService.download(project, "buf.yaml",false);
            return true;
        });

        consoleView.print("[契约检查] "+new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())+"\n",ConsoleViewContentType.LOG_INFO_OUTPUT);
        consoleView.print("\n",ConsoleViewContentType.LOG_INFO_OUTPUT);

        String basePath = Config.getInstance(project).protoBasePath();

        //拼装排除的路径
        String systemPath = basePath.concat("autohome").concat(File.separator).concat("rpc").concat(File.separator).concat("system");
        String apiPath = basePath +"autohome"+File.separator+"api";
        String googlePath = basePath+"google";

        List<String> excludePaths = new ArrayList<>();
        if(new File(systemPath).exists()){
            excludePaths.add(systemPath);
        }
        if(new File(apiPath).exists()){
            excludePaths.add(apiPath);
        }
        if(new File(googlePath).exists()){
            excludePaths.add(googlePath);
        }

        String exclude =excludePaths.size() ==0
                ? ""
                : " --exclude-path "+ String.join(",",excludePaths);

        String breakingexclude = " --exclude-path autohome/api;google";

//        String lint =  BufExeService.localFilePath() + " lint " + basePath;
//        lint += exclude;

        if(!OsUtil.isWindows()){
            List<String> chmods = new ArrayList<>();
            chmods.add("chmod");
            chmods.add("777");
            chmods.add(BufExeService.localFilePath());
            String chmodsResult = run(project, chmods,consoleView);
            if(StringUtils.isNotBlank(chmodsResult)){
                consoleView.print("chmod 执行失败 \n", ConsoleViewContentType.LOG_INFO_OUTPUT);
            }else{
                consoleView.print("chmod 执行成功 \n", ConsoleViewContentType.LOG_INFO_OUTPUT);
            }
        }


        List<String> lintcmds = new ArrayList<>();
        lintcmds.add(BufExeService.localFilePath());
        lintcmds.add("lint");
        lintcmds.add(basePath);
        if(excludePaths.size()>0){
            lintcmds.add("--exclude-path");
            lintcmds.add(String.join(",",excludePaths));
        }

        //String breaking = BufExeService.localFilePath() + " breaking \"" + basePath + "\" --against \"" + Config.gitBasePath + "#branch=master\"";
        List<String> breakingcmds = new ArrayList<>();
        breakingcmds.add(BufExeService.localFilePath());
        breakingcmds.add("breaking");
        breakingcmds.add(""+basePath+"");
        breakingcmds.add("--against");
        breakingcmds.add(""+Config.gitBasePath+"#branch=master");


        return CompletableFuture.supplyAsync(()->{

            consoleView.print("[版本检查] 开始检查buf.exe版本 \n", ConsoleViewContentType.LOG_INFO_OUTPUT);
            if(!BufExeService.downExe(project, consoleView)){
                return false;
            }
//            consoleView.print("[版本检查] 结束检查buf.exe版本 \n \n", ConsoleViewContentType.LOG_INFO_OUTPUT);


            boolean lintPass = true;
            consoleView.print("[规范检查] ", ConsoleViewContentType.LOG_INFO_OUTPUT);

            if(!success.equals(run(project, lintcmds,consoleView))){
                lintPass = false;
            }
            consoleView.print("\n", ConsoleViewContentType.LOG_INFO_OUTPUT);
            if(lintPass){
                consoleView.print(lintPassMsg + "\n", ConsoleViewContentType.LOG_INFO_OUTPUT);
            }else{
                consoleView.print(lintNotPassMsg + "\n", ConsoleViewContentType.LOG_ERROR_OUTPUT);
            }
            consoleView.print( "\n", ConsoleViewContentType.NORMAL_OUTPUT);

            boolean breakingPass = true;

            consoleView.print("[兼容检查] ", ConsoleViewContentType.LOG_INFO_OUTPUT);

            if(!success.equals(run(project, breakingcmds,consoleView))){
                breakingPass = false;
            }

            consoleView.print("\n", ConsoleViewContentType.LOG_INFO_OUTPUT);

            if(breakingPass){
                consoleView.print(breakingPassMsg + "\n", ConsoleViewContentType.LOG_INFO_OUTPUT);
            }else{
                consoleView.print(breakingNotPassMsg + "\n", ConsoleViewContentType.LOG_ERROR_OUTPUT);
            }

            consoleView.print("\n", ConsoleViewContentType.NORMAL_OUTPUT);
            consoleView.print("[检查结果] ------------------------------------\n", ConsoleViewContentType.NORMAL_OUTPUT);
            if(lintPass){
                consoleView.print(lintPassMsg + "\n", ConsoleViewContentType.LOG_INFO_OUTPUT);
            }else{
                consoleView.print(lintNotPassMsg + "\n", ConsoleViewContentType.LOG_ERROR_OUTPUT);
            }
            if(breakingPass){
                consoleView.print(breakingPassMsg + "\n", ConsoleViewContentType.LOG_INFO_OUTPUT);
            }else{
                consoleView.print(breakingNotPassMsg + "\n", ConsoleViewContentType.LOG_ERROR_OUTPUT);
            }

            if(lintPass && breakingPass){
                consoleView.print(allPassMsg + "\n", ConsoleViewContentType.LOG_INFO_OUTPUT);
                return true;
            }else{
                consoleView.print(allNotPassMsg + "\n", ConsoleViewContentType.LOG_ERROR_OUTPUT);
                return false;
            }
        });
    }

    public static String run(Project project, List<String> cmd,ConsoleView consoleView) {
        List<String> commands = new ArrayList<>();
        if(OsUtil.isWindows()){
            commands.add(OsUtil.baseExePath());
        }
        commands.addAll(cmd);
        GeneralCommandLine generalCommandLine = new GeneralCommandLine(commands);
        generalCommandLine.setCharset(Charset.forName("UTF-8"));
        generalCommandLine.setWorkDirectory(project.getBasePath());

        try {
            //generalCommandLine.createProcess();
            OSProcessHandler osProcessHandler = ProcessHandlerFactory.getInstance().createColoredProcessHandler(generalCommandLine);
            consoleView.attachToProcess(osProcessHandler);
            osProcessHandler.startNotify();
            String result = ExecUtil.execAndGetOutput(generalCommandLine).getStdout();
            osProcessHandler.getProcess().waitFor();
            return result;
        } catch (Exception e) {
            return "error";
        }
    }


    public static boolean hasMvn(Project project){
        return StringUtils.isNotBlank(CommandUtils.run(Arrays.asList("mvn","-v"),project.getBasePath()));
    }

    public static void protobufCompile(Project project) {
        FileDocumentManager.getInstance().saveAllDocuments();
        ConsoleView consoleView = BufToolWindow.getAndShowCompileView(project);
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "编译契约，并更新索引") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setText("编译契约，并更新索引");
                indicator.setIndeterminate(true);

                if(!hasMvn(project)){
                    consoleView.print("未检测到maven环境，请先正确安装maven，确保可以执行 mvn 命令",ConsoleViewContentType.LOG_INFO_OUTPUT);
                    return;
                }

                String path = Config.getInstance(project).grpcPath();

                List<String> commands = new ArrayList<>();
                if(OsUtil.isWindows()){
                    commands.add(OsUtil.baseExePath());
                }
                commands.add("mvn");
                commands.add("protobuf:compile");
                GeneralCommandLine generalCommandLine = new GeneralCommandLine(commands);
                generalCommandLine.setCharset(Charset.forName("UTF-8"));
                generalCommandLine.setWorkDirectory(path);

                try {
                    OSProcessHandler osProcessHandler = ProcessHandlerFactory.getInstance().createColoredProcessHandler(generalCommandLine);
                    consoleView.attachToProcess(osProcessHandler);
                    osProcessHandler.startNotify();
                    osProcessHandler.waitFor();
                } catch (ExecutionException e) {
                    throw new RuntimeException(e);
                }

                SynchronizeCurrentFileAction.synchronizeFiles(Arrays.asList( project.getBaseDir()),project,true);
                Config.getInstance(project).platformConfig().getPlatform().refreshRoot(project);
            }
        });

    }
}
