package com.autohome.ah_grpc_plugin.services;

import com.autohome.ah_grpc_plugin.factory.BufToolWindow;
import com.autohome.ah_grpc_plugin.factory.MainFactory;
import com.autohome.ah_grpc_plugin.ui.MainToolwindow;
import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.lang.xml.XMLLanguage;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlDocument;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.apache.xmlbeans.XmlLanguage;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service(Service.Level.PROJECT)
public final class GrpcConfigService {
    final static String fileName = "grpc.xml";

    Project project;
    Boolean supportGrpc =false;
    Boolean hasInit = false;
    String idlPath = "";

    String grpcModulePath="";

    public static GrpcConfigService getInstance(Project project) {
        GrpcConfigService service = project.getService(GrpcConfigService.class);
        if(service.getHasInit()){
            return service;
        }
        service.setProject(project);
        service.init();
        service.setHasInit(true);
        return service;
    }

    void init(){
        XmlFile file = getFile();
        if(file==null){
            setSupportGrpc(false);
            return;
        }
        ApplicationManager.getApplication().runReadAction(()->{
            XmlDocument document = file.getDocument();
            XmlTag root = document.getRootTag();
            String sg = root.getSubTagText("support_grpc");
            String idlPath = root.getSubTagText("idl_path");
            String grpcModulePath = root.getSubTagText("grpc_module_path");
            if(StringUtils.isNotBlank(sg)){
                setSupportGrpc(sg.equals("true"));
            }
            if(StringUtils.isNotBlank(idlPath)){
                setIdlPath(idlPath);
            }
            if(StringUtils.isNotBlank(grpcModulePath)){
                setGrpcModulePath(grpcModulePath);
            }
        });
    }

    public void support(String grpcModulePath) {
        this.grpcModulePath = grpcModulePath;
        XmlFile file = getFile();
        if (file == null) {
            createFile();
        }
    }

    /**
     * 取消绑定
     * 把grpc.xml中的idl_path去掉
     */
    public void cancelPath(){
        cancelPathFile();
    }
    /**
     * 绑定
     * 把grpc.xml添加idl_path
     */
    public void supportPath(String path){
        createPathFile(path);
    }

    void createFile() {
        Boolean mms = ModuleManager.getInstance(project).getModules().length > 1;
        String baseText = "" +
                "<project>\n" +
                "   <grpc_module_path>" + grpcModulePath + "</grpc_module_path>\n" +
                "   <support_grpc>true</support_grpc>\n" +
                "</project>";
        ApplicationManager.getApplication().runWriteAction((Computable<XmlFile>) () -> {
            PsiFile psiFile = PsiFileFactory.getInstance(project).createFileFromText(fileName, XmlFileType.INSTANCE, baseText);
            PsiDirectory psiDirectory = PsiService.createAndGetDirectory(project, "/");
            psiDirectory.add(psiFile);
            psiFile = psiDirectory.findFile(fileName);
            return (XmlFile) psiFile;
        });
    }
    void cancelPathFile(){
        Boolean mms = ModuleManager.getInstance(project).getModules().length > 1;
        String baseText = "" +
                "<project>\n" +
                "   <grpc_module_path>" + grpcModulePath + "</grpc_module_path>\n" +
                "   <support_grpc>true</support_grpc>\n" +
                "</project>";
        ApplicationManager.getApplication().runWriteAction(() -> {
            String path = project.getBasePath() + "/";
            VirtualFile file = LocalFileSystem.getInstance().findFileByIoFile(new File(path + "/" + fileName));
            try {
                file.setBinaryContent(baseText.getBytes(StandardCharsets.UTF_8));
                setIdlPath("");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    void createPathFile(String idlPath) {
        Boolean mms = ModuleManager.getInstance(project).getModules().length > 1;
        String baseText = "" +
                "<project>\n" +
                "   <grpc_module_path>" + grpcModulePath + "</grpc_module_path>\n" +
                "   <support_grpc>true</support_grpc>\n" +
                "   <idl_path>" + idlPath + "</idl_path>\n" +
                "</project>";
        ApplicationManager.getApplication().runWriteAction(() -> {
            File iofile = new File(project.getBasePath() + "/" + fileName);
            VirtualFile file = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(iofile);
            if(file==null){
                LocalFileSystem.getInstance().refresh(true);
                file = LocalFileSystem.getInstance().findFileByIoFile(iofile);
            }
            try {
                file.setBinaryContent(baseText.getBytes(StandardCharsets.UTF_8));
                setIdlPath(idlPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }


    synchronized XmlFile getFile() {
        String path = project.getBasePath() + "/";
        VirtualFile file = LocalFileSystem.getInstance().findFileByIoFile(new File(path + "/" + fileName));
        if (file == null || !file.exists())
            return null;


        return ApplicationManager.getApplication().runReadAction((Computable<XmlFile>) () -> {
            byte[] content = null;
            try {
                content = file.contentsToByteArray();
            } catch (IOException e) {
                return null;
            }

            return (XmlFile)PsiFileFactory.getInstance(project).createFileFromText(XMLLanguage.INSTANCE,new String(content, StandardCharsets.UTF_8));
        });
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public Boolean getSupportGrpc() {
        return supportGrpc;
    }

    public void setSupportGrpc(Boolean supportGrpc) {
        this.supportGrpc = supportGrpc;
    }

    public Boolean getHasInit() {
        return hasInit;
    }

    public void setHasInit(Boolean hasInit) {
        this.hasInit = hasInit;
    }

    public String getIdlPath() {
        return idlPath;
    }

    public void setIdlPath(String idlPath) {
        this.idlPath = idlPath;
    }

    public String getGrpcModulePath() {
        return grpcModulePath;
    }

    public void setGrpcModulePath(String grpcModulePath) {
        this.grpcModulePath = grpcModulePath;
    }
}
