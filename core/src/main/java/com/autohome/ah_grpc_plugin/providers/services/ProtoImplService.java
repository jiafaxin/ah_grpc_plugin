package com.autohome.ah_grpc_plugin.providers.services;

import com.autohome.ah_grpc_plugin.Config;
import com.autohome.ah_grpc_plugin.dialogs.Confim;
import com.autohome.ah_grpc_plugin.enums.ResultCode;
import com.autohome.ah_grpc_plugin.models.MethodResult;
import com.autohome.ah_grpc_plugin.platform.PlatformService;
import com.autohome.ah_grpc_plugin.services.ProtoImplementService;
import com.autohome.ah_grpc_plugin.services.PsiService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.protobuf.lang.PbFileType;
import com.intellij.protobuf.lang.psi.impl.PbServiceBodyImpl;
import com.intellij.protobuf.lang.psi.impl.PbServiceMethodImpl;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.indexing.FileBasedIndex;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

//proto 跳转 impl
@Service(Service.Level.PROJECT)
public final class ProtoImplService {

    Map<String,String> ProtoImpl = new HashMap<>();  //存储proto package 与 实现文件地址的映射
    Project project;
    PlatformService pi;

    public static ProtoImplService getInstance(Project project){
        ProtoImplService protoImplService =  project.getService(ProtoImplService.class);
        if(protoImplService.getProject()!=null)
            return protoImplService;
        protoImplService.setProject(project);
        protoImplService.setPi(Config.getInstance(project).platformConfig().getPlatform());
        protoImplService.init();
        return protoImplService;
    }

    public void put(String key,String value){
        ProtoImpl.put(key,value);
    }

    public boolean containsKey(String key){
        return ProtoImpl.containsKey(key);
    }

    public boolean contains(String key){
        if(ProtoImpl.containsKey(key)){
            return true;
        }
        init();
        return ProtoImpl.containsKey(key);
    }

    public String get(String key){
        return ProtoImpl.get(key);
    }

    /**
     * 项目启动的时候扫描所有实现类,并写入map
     */
    public void init(){
        //查找所有文件（java、go等）
        Collection<VirtualFile> virtualFiles = pi.findAllFiles(project);

        if(virtualFiles.size()==0) {
            return;
        }

        if(virtualFiles == null)
            return;
        for (VirtualFile virtualFile : virtualFiles) {
            if (virtualFile == null) continue;
            initImplClass(virtualFile);
        }
    }

    public void initImplClass(VirtualFile virtualFile) {
        PsiFile file = PsiManager.getInstance(project).findFile(virtualFile);
        if (file == null)
            return;

        initImplClass(file);
    }

    public void initImplClass(PsiFile simpleFile){
        List<String> extendTypes = pi.getAllExtendTypes(simpleFile);
        if (extendTypes == null || extendTypes.size() == 0)
            return;

        for (String extendType : extendTypes) {
            put(extendType, simpleFile.getVirtualFile().getPath());
        }
    }

    public void deleteImplClass(PsiFile psiFile){
        List<String> extendTypes = pi.getAllExtendTypes(psiFile);
        if (extendTypes == null || extendTypes.size() == 0)
            return;

        for (String extendType : extendTypes) {
            ProtoImpl.remove(extendType);
        }
    }



    public MethodResult createImpl(Project project, PbServiceBodyImpl serviceBody,String serviceName, Consumer<String> callback) {

        String savePath;
        final PsiFile[] file = {null};
        PsiDirectory directory = null;
        while (true) {
            savePath = ProtoImplementService.selectPath(project);
            if (StringUtils.isBlank(savePath)) {
                return MethodResult.fail(ResultCode.ERROR);
            }
            file[0] = pi.createFile(project, serviceName, serviceBody, savePath);
            directory = PsiService.createAndGetDirectory(project, savePath.substring(project.getBasePath().length() + 1));
            if(directory.findFile(file[0].getName())==null)
                break;
            if (!new Confim(project, "创建失败", "<html>当前文件夹已存在文件：" + file[0].getName() + "。<br> 点击[Ok]选择其他文件夹;<br> 选择点击[Cancel]取消创建</html>").showAndGet()) {
                return MethodResult.fail(ResultCode.ERROR);
            }
        }

        PsiDirectory finalDirectory = directory;
        String finalSavePath = savePath;
        ApplicationManager.getApplication().runWriteAction(() -> {

            finalDirectory.add(file[0]);
            String fileName = file[0].getName();
            file[0] = finalDirectory.findFile(fileName);
            VirtualFile virtualFile = file[0].getVirtualFile();
            //重新刷新索引，防止强制关闭idea，丢失索引
            FileBasedIndex.getInstance().requestReindex(virtualFile);
            initImplClass(virtualFile);
            callback.accept(finalSavePath + "/" + fileName);
        });
        return MethodResult.success();
    }

    public MethodResult insertMethod(Project project, PbServiceMethodImpl method, String protoServicePath, Runnable callback){
        if(! containsKey(protoServicePath)){
            return MethodResult.fail(ResultCode.IMPL_NOT_EXISTS);
        }

        //PSI 不可信，需要重建
        File file = new File(get(protoServicePath));
        VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByIoFile(file);
        PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
        return pi.insertMethod(project,psiFile,method,callback);
    }


    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public PlatformService getPi() {
        return pi;
    }

    public void setPi(PlatformService pi) {
        this.pi = pi;
    }
}
