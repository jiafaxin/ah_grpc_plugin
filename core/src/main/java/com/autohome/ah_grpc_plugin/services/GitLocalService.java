package com.autohome.ah_grpc_plugin.services;

import com.autohome.ah_grpc_plugin.Config;
import com.autohome.ah_grpc_plugin.dialogs.GitDialog;
import com.autohome.ah_grpc_plugin.enums.GitLocalInfoType;
import com.autohome.ah_grpc_plugin.factory.BufToolWindow;
import com.autohome.ah_grpc_plugin.models.FileDetail;
import com.autohome.ah_grpc_plugin.models.GitAction;
import com.autohome.ah_grpc_plugin.models.MethodResult;
import com.autohome.ah_grpc_plugin.models.ProtoPath;
import com.autohome.ah_grpc_plugin.utils.JsonUtils;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.json.JsonFileType;
import com.intellij.json.JsonLanguage;
import com.intellij.json.psi.JsonFile;
import com.intellij.json.psi.JsonValue;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.protobuf.lang.PbFileType;
import com.intellij.psi.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;


@Service(Service.Level.PROJECT)
public final class GitLocalService {
    Project project;

    public static GitLocalService getInstance(Project project) {
        GitLocalService instance = project.getService(GitLocalService.class);
        if(instance.project==null){
            instance.project = project;
//            instance.scanAll();
        }
        return instance;
    }

    public void create(String path) {
        GitAction info = new GitAction();
        info.setAction(GitLocalInfoType.create);
        info.setFile_path(path);
        write(info);
    }

    //遵循gitlab，不能创建文件夹，必须创建文件
    public void createDirectory(String path) {
    }

    public void update(String path) {
        GitAction info = new GitAction();
        info.setAction(GitLocalInfoType.update);
        info.setFile_path(path);
        write(info);
    }

    public void delete(String path){
        delete(path,null);
    }

    public void delete(String path,GitLocalInfoType infoType){
        JsonFile gitFile = getFile(false);
        ApplicationManager.getApplication().invokeLater(() -> {
            WriteCommandAction.runWriteCommandAction(project, () -> {
                for (JsonValue allTopLevelValue : gitFile.getAllTopLevelValues()) {
                    GitAction action = JsonUtils.toObject(allTopLevelValue.getText(),GitAction.class);
                    if(action==null){
                        allTopLevelValue.delete();
                    }
                    if(infoType==null) {
                        if (action.getFile_path().indexOf(path) == 0) {
                            allTopLevelValue.delete();
                        }
                    }else{
                        if(action.getFile_path().equals(path) && action.getAction().equals(infoType)){
                            allTopLevelValue.delete();
                        }
                    }
                }
            });
        });
    }

    synchronized public MethodResult push() {
        GrpcConfigService grpcConfigService = GrpcConfigService.getInstance(project);
        if(StringUtils.isBlank(grpcConfigService.getIdlPath())){
            NotifyService.warning(project,"请先绑定项目后再推送!");
            return null;
        }
        String path = grpcConfigService.getIdlPath();

        //推送之前保存所有文件
        FileDocumentManager.getInstance().saveAllDocuments();
        List<GitAction> actions = getVirtualFileAllActions(path);
        ConsoleView consoleView = BufToolWindow.getAndShowGitView(project);

//        consoleView.print("LOG_DEBUG_OUTPUT \n", ConsoleViewContentType.LOG_DEBUG_OUTPUT);
//        consoleView.print("LOG_VERBOSE_OUTPUT \n", ConsoleViewContentType.LOG_VERBOSE_OUTPUT);
//        consoleView.print("LOG_INFO_OUTPUT \n", ConsoleViewContentType.LOG_INFO_OUTPUT);
//        consoleView.print("LOG_WARNING_OUTPUT \n", ConsoleViewContentType.LOG_WARNING_OUTPUT);
//        consoleView.print("NORMAL_OUTPUT \n", ConsoleViewContentType.NORMAL_OUTPUT);
//        consoleView.print("LOG_ERROR_OUTPUT \n", ConsoleViewContentType.LOG_ERROR_OUTPUT);
//        consoleView.print("ERROR_OUTPUT \n", ConsoleViewContentType.ERROR_OUTPUT);
//        consoleView.print("SYSTEM_OUTPUT \n", ConsoleViewContentType.SYSTEM_OUTPUT);
//        consoleView.print("USER_INPUT \n", ConsoleViewContentType.USER_INPUT);

        consoleView.print("[变更检查] 开始检查文件变更 \n", ConsoleViewContentType.NORMAL_OUTPUT);
        List<CompletableFuture> tasks = new ArrayList<>();
        for (GitAction action : actions) {
            tasks.add(CompletableFuture.supplyAsync(() -> {
                if (action.getFileName().equals("README.md")) {

                } else {
                    action.setContent(FileService.getFileContent(project, action.getFile_path()));
                }

                FileDetail fileDetail = GitlabApiService.getFile(project, action.getFile_path());
                if (fileDetail == null || fileDetail.notExists()) {
                    consoleView.print("[变更检查] 创建 "+ action.getFile_path() +" \n", ConsoleViewContentType.SYSTEM_OUTPUT);
                    action.setAction(GitLocalInfoType.create);
                } else {
                    if(action.getContent().equals(fileDetail.getContent())) {
                        consoleView.print("[变更检查] "+ action.getFile_path() +" 无变更 \n", ConsoleViewContentType.NORMAL_OUTPUT);
                        action.setAction(GitLocalInfoType.none);
                    }else{
                        consoleView.print("[变更检查] "+ action.getFile_path() +" 有变更 \n", ConsoleViewContentType.LOG_INFO_OUTPUT);
                        action.setAction(GitLocalInfoType.update);
                    }
                }
                return true;
            }));
        }

        CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0])).join();

        actions.removeIf(x->x.getAction().equals(GitLocalInfoType.none));

        if (actions.size() == 0) {
            NotifyService.info(project,"没有可推送的文件或变更");
            consoleView.print("[变更检查] 没有可提交的变更 \n", ConsoleViewContentType.LOG_INFO_OUTPUT);
            return MethodResult.success();
        }

        GitDialog dialog = new GitDialog(project, actions, path,consoleView, (pass)->{
//            if(pass){
//                resetFile(otherActions);
//            }
        });
        dialog.showAndGet();
        return MethodResult.success();
    }

    public List<GitAction> getAllActions( ) {
        JsonFile gitFile = getFile(false);
        if (gitFile == null)
            return new ArrayList<>();

        return ApplicationManager.getApplication().runReadAction((Computable<List<GitAction>>) () -> {
            List<GitAction> actions = new ArrayList<>();
            for (JsonValue item : gitFile.getAllTopLevelValues()) {
                actions.add(JsonUtils.toObject(item.getText(), GitAction.class));
            }
            return actions;
        });
    }

    public List<GitAction> getVirtualFileAllActions(String idlPath) {
        List<VirtualFile> virtualFiles = getVirtualFile(idlPath);
        if (CollectionUtils.isEmpty(virtualFiles)) {
            return new ArrayList<>();
        }
        List<GitAction> actions = new ArrayList<>();
        for (VirtualFile virtualFile : virtualFiles) {
            int index = virtualFile.getPath().indexOf(idlPath);
            String filePath = "";
            if(index != -1){
                filePath = virtualFile.getPath().substring(index + idlPath.length());
            }
            if(StringUtils.isNotBlank(filePath)){
                GitAction gitAction = new GitAction();
                gitAction.setFile_path(idlPath + filePath);
                actions.add(gitAction);
            }
        }
        return actions;
//        return ApplicationManager.getApplication().runReadAction((Computable<List<GitAction>>) () -> {
//            List<GitAction> actions = new ArrayList<>();
//            for (PsiFile psiFile : gitFile) {
//                actions.add(JsonUtils.toObject(psiFile.getText(), GitAction.class));
//            }
//            return actions;
//        });
    }

    public void removeItem(String path) {
        JsonFile gitFile = getFile(false);
        if (gitFile == null)
            return;

        ApplicationManager.getApplication().invokeLater(() -> {
            WriteCommandAction.runWriteCommandAction(project, () -> {
                for (JsonValue item : gitFile.getAllTopLevelValues()) {
                    GitAction action = JsonUtils.toObject(item.getText(), GitAction.class);
                    if (action.getFile_path().startsWith(path)) {
                        item.delete();
                    }
                }
            });
        });
    }

    synchronized void write( GitAction info) {
        if(info.getFile_path().indexOf("autohome/rpc")!=0)
            return;

        if(info.getAction().equals(GitLocalInfoType.create)){
            if(GitlabApiService.getFile(project, info.getFile_path())!=null){
                return;
            }
        }

        JsonFile gitFile = getFile(true);
        String content = JsonUtils.toString(info);
        ApplicationManager.getApplication().invokeLater(() -> {
            WriteCommandAction.runWriteCommandAction(project, () -> {

                JsonFile file = (JsonFile) PsiFileFactory.getInstance(project).createFileFromText(JsonLanguage.INSTANCE, content);
                for (JsonValue item : gitFile.getAllTopLevelValues()) {
                    GitAction action = JsonUtils.toObject(item.getText(), GitAction.class);
                    if (!action.getFile_path().equals(info.getFile_path())) {
                        continue;
                    }
                    return;
                }
                gitFile.add(file.getTopLevelValue());


            });
        });
    }

    synchronized JsonFile getFile( boolean createIfNotExists) {
        String fileName = "grpc.json";
        String path = project.getBasePath() + "/.idea";

        VirtualFile file = LocalFileSystem.getInstance().findFileByIoFile(new File(path + "/" + fileName));
        if (!createIfNotExists && file == null)
            return null;
        if (file == null) {
            return ApplicationManager.getApplication().runWriteAction((Computable<JsonFile>)()->{
                PsiFile psiFile = PsiFileFactory.getInstance(project).createFileFromText(fileName, JsonFileType.INSTANCE, "");
                PsiDirectory psiDirectory = PsiService.createAndGetDirectory(project,"/.idea");
                psiDirectory.add(psiFile);
                return (JsonFile)  psiDirectory.findFile(fileName);
            });
        } else {
            return ApplicationManager.getApplication().runReadAction((Computable<JsonFile>)()->{
                return (JsonFile) PsiManager.getInstance(project).findFile(file);
            });
        }
    }
    synchronized List<VirtualFile> getVirtualFile(String idlPath) {
        String path = Config.getInstance(project).protoBasePath()+idlPath;
        VirtualFile file = LocalFileSystem.getInstance().findFileByIoFile(new File(path));
        List<VirtualFile> virtualFiles = new ArrayList<>();
        if(null == file){
            return virtualFiles;
        }
        // 递归遍历文件夹中的所有文件
        processDirectory(file, virtualFiles);
        return virtualFiles;
    }

    private static void processDirectory(VirtualFile directory, List<VirtualFile> virtualFiles) {
        for (VirtualFile virtualFile : directory.getChildren()) {
            if (virtualFile.isDirectory()) {
                // 递归处理子文件夹
                processDirectory(virtualFile, virtualFiles);
            } else {
                // 将文件转换为 PsiFile 并添加到 CompositePsiFile 中
                //PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
                if (virtualFile != null) {
                    virtualFiles.add(virtualFile);
                }
            }
        }
    }


    //重置git文件，只保留未提交的
//    void resetFile( List<GitAction> actions) {
//        ApplicationManager.getApplication().invokeLater(() -> {
//            WriteCommandAction.runWriteCommandAction(project, () -> {
//                JsonFile gitFile = getFile(false);
//                if (gitFile == null)
//                    return;
//
//                List<JsonValue> removeValues = new ArrayList<>();
//                for (JsonValue itemJson : gitFile.getAllTopLevelValues()) {
//                    GitAction item = JsonUtils.toObject(itemJson.getText(), GitAction.class);
//                    if (item == null) {
//                        removeValues.add(itemJson);
//                        continue;
//                    }
//                    if (!actions.stream().anyMatch(y -> y.getFile_path().equals(item.getFile_path()))) {
//                        removeValues.add(itemJson);
//                        continue;
//                    }
//                }
//                for (JsonValue removeValue : removeValues) {
//                    removeValue.delete();
//                }
//
//                Document document = PsiDocumentManager.getInstance(project).getDocument(gitFile);
//                FileDocumentManager.getInstance().saveDocument(document);
//            });
//        });
//    }

    public boolean contains(String path) {
        JsonFile psiFile = getFile(false);
        return psiFile.getAllTopLevelValues().stream().anyMatch(x -> {
            GitAction action = JsonUtils.toObject(x.getText(), GitAction.class);
            if (action.getFile_path().equals(path)) {
                return true;
            }
            return false;
        });
    }

//    public void scanAll() {
//        String path = Config.getInstance(project).protoBasePath();
//        if(StringUtils.isBlank(path))
//            return;
//        File base = new File(Config.getInstance(project).protoBasePath());
//        if (!base.exists()) {
//            return;
//        }
//        VirtualFile d = LocalFileSystem.getInstance().findFileByIoFile(base);
//        scan(d);
//    }

//    void scan( VirtualFile directory){
//        ApplicationManager.getApplication().invokeLater(()->{
//            ApplicationManager.getApplication().runWriteAction((Computable<Boolean>)()->{
//                directory.refresh(false, false);
//                return true;
//            });
//        });
//        for (VirtualFile child : directory.getChildren()) {
//            if(child.isDirectory()){
//                scan(child);
//            }else if(child.getFileType() instanceof PbFileType){
//                initFile(child);
//            }
//        }
//    }

//    void initFile(VirtualFile file){
//        ProtoPath path = ProtoPath.newInstance(file.getPath());
//        if(path == null)
//            return;
//        String protoPath = path.realPath();
//        GitlabApiService.getFileAsync(project, protoPath).thenAccept(result->{
//            //远程服务器没有的文件
//            if(result==null) {
//                create(protoPath);
//            }
//        });
//    }
}
