package com.autohome.ah_grpc_plugin.providers.services;

import com.autohome.ah_grpc_plugin.Config;
import com.autohome.ah_grpc_plugin.platform.PlatformService;
import com.autohome.ah_grpc_plugin.services.BufExeService;
import com.autohome.ah_grpc_plugin.utils.ProtoPsiUtils;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.protobuf.lang.PbFileType;
import com.intellij.protobuf.lang.psi.impl.PbServiceDefinitionImpl;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * impl 跳转proto
 */
@Service(Service.Level.PROJECT)
public final class ImplProtoService {
    Map<String,String> ImplProto = new HashMap<>();  //实现的name 与 proto文件地址的映射
    Project project;
    PlatformService pi;

    public static ImplProtoService getInstance(Project project){
        ImplProtoService service =  project.getService(ImplProtoService.class);
        if(service.getProject()!=null)
            return service;
        service.setProject(project);
        service.setPi(Config.getInstance(project).platformConfig().getPlatform());
        return service;
    }

    public void put(String key,String value){
        ImplProto.put(key,value);
    }

    public boolean containsKey(String key){
        return ImplProto.containsKey(key);
    }

    public boolean contains(String key){
        if(containsKey(key)){
            return true;
        }
        init();
        return containsKey(key);
    }

    public String get(String key){
        return ImplProto.get(key);
    }

    /**
     * 项目启动的时候扫描所有proto文件,并写入map
     */
    public void init(){
        //查找所有Proto文件
        Collection<VirtualFile> protoFiles = FileTypeIndex.getFiles(PbFileType.INSTANCE, GlobalSearchScope.projectScope(project));
        for (VirtualFile virtualFile : protoFiles) {
            if (virtualFile == null) continue;
            String path = virtualFile.getPath();

            if (path.indexOf("/proto/autohome/rpc") > 0) {
                initProto(virtualFile);
            }
        }
    }


    public void initProto(VirtualFile virtualFile){
        PsiFile simpleFile = PsiManager.getInstance(project).findFile(virtualFile);
        if (simpleFile == null) return;
        initProto(simpleFile);
    }

    public void initProto(PsiFile simpleFile){
        List<PbServiceDefinitionImpl> services = PsiTreeUtil.getChildrenOfTypeAsList(simpleFile, PbServiceDefinitionImpl.class);
        for (PbServiceDefinitionImpl service : services) {
            String path = pi.getTripleServiceName(simpleFile,service.getName());
            String toPath = simpleFile.getVirtualFile().getPath().concat("@@").concat(service.getName());
            put(path,toPath);
        }
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
