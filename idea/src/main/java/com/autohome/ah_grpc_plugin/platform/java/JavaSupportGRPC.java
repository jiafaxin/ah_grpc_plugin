package com.autohome.ah_grpc_plugin.platform.java;

import com.autohome.ah_grpc_plugin.Config;
import com.autohome.ah_grpc_plugin.models.MethodResult;
import com.autohome.ah_grpc_plugin.platform.SupportGRPC;
import com.autohome.ah_grpc_plugin.services.NotifyService;
import com.autohome.ah_grpc_plugin.services.PsiService;
import com.autohome.ah_grpc_plugin.utils.ResourceFileUtils;
import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectNotificationAware;
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectTracker;
import com.intellij.openapi.externalSystem.autoimport.ProjectRefreshAction;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.module.StdModuleTypes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiJavaFileImpl;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlDocument;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlTagValue;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import org.jetbrains.idea.maven.utils.actions.MavenActionUtil;
//import org.jetbrains.yaml.YAMLElementGenerator;
//import org.jetbrains.yaml.YAMLFileType;
//import org.jetbrains.yaml.psi.YAMLDocument;
//import org.jetbrains.yaml.psi.YAMLFile;
//import org.jetbrains.yaml.psi.YAMLKeyValue;
//import org.jetbrains.yaml.psi.YAMLValue;
//import org.jetbrains.yaml.psi.impl.YAMLBlockMappingImpl;
//import org.jetbrains.yaml.psi.impl.YAMLFileImpl;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import com.intellij.ide.highlighter.JavaFileType;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

public class JavaSupportGRPC implements SupportGRPC {
    static final String dubboConfig = "\n\n\n" +
            "dubbo:\n" +
            "  application:\n" +
            "    name: ${appName}\n" +
            "    qos-port: 22222\n" +
            "    metadataServicePort: 20885\n" +
            "    qosEnable: true\n" +
            "    qosAcceptForeignIp: true\n" +
            "  protocol:\n" +
            "    name: tri\n" +
            "    port: 50051\n" +
            "  registry:\n" +
            "    address: N/A\n" +
            "  consumer:  #此节点只有消费者需要\n" +
            "    meshEnable: true\n" +
            "  tri:\n" +
            "    builtin:\n" +
            "      service:\n" +
            "        init: true\n";

    String groupId;
    String artifactId;
    String version;
    Project project;

    @Override
    public boolean support(Project _project) {
        project = _project;
        //获取所有模块
        Module[] modules = ModuleManager.getInstance(project).getModules();
        //查找根目录的pom文件
        VirtualFile pom = getPom(project, null,"");

        //是否springboot项目；springboot版本是否支持
        if(!springbootSupport(project,pom)){
            return false;
        }

        //获取根目录下pom的groupId、artifactId、version
        init(project, pom);


        ApplicationManager.getApplication().invokeLater(() -> {
            WriteCommandAction.runWriteCommandAction(project, () -> {

                //初始化根目录的pom文件：设置version &
                initRootPom(project,pom);

                if (modules.length > 1) {  //多模块项目

                    //多模块项目
                    initModule(project);

                    for (Module module : modules) {
                        MethodResult result = initApplication(project, module);
                        if (result.isSuccess()) {
                            //给yml添加配置
                            initYml(project, module,result.getMessage());
                            VirtualFile modulePom = getPom(project, module,result.getMessage());
                            initModulePom(project, modulePom);
                        }
                    }
                } else {  //单模块项目
                    //给application添加@EnableDubbo注解
                    initApplication(project, null);
                    //给yml添加配置
                    initYml(project, null,"");
                    //添加引用
                    initPom(project, pom);
                }
            });
        });

        refreshProject();

        return true;
    }

    @Override
    public boolean support(Project _project, AnActionEvent _event, String currPath) {
        project = _project;
        VirtualFile currentFile = LocalFileSystem.getInstance().findFileByIoFile(new File(currPath + "/pom.xml"));
        if(null == currentFile){
            NotifyService.error(project,"当前路径下没有pom.xml,不支持grpc");
            return false;
        }
        Module[] modules = ModuleManager.getInstance(project).getModules();
        if(currPath.equals(project.getBasePath()) && modules.length > 1){
            NotifyService.error(project,"当前路径下不支持grpc");
            return false;
        }
        //查找根目录的pom文件
        VirtualFile pom = getPom(project, null,"");

        //获取根目录下pom的groupId、artifactId、version
        //init(project, pom);
        ApplicationManager.getApplication().invokeLater(() -> {
            WriteCommandAction.runWriteCommandAction(project, () -> {
                String path = "";
                if(currPath.equals(project.getBasePath())){
                    path = "src/main/proto/autohome/rpc";
                    initPom(project, currentFile);
                }else{
                    int index = project.getBasePath().length() + 1;
                    path = currPath.substring(index , currPath.length()) + "/src/main/proto/autohome/rpc";
                    initRootPomNew(project,pom);
                    initModulePomNew(project,currentFile);
                }
                initProtoDir(project,path);
            });
        });
        refreshProject();
        return true;
    }

    @Override
    public boolean supportGrpc(Project _project, AnActionEvent _event, String currPath,boolean isConsumer) {
        project = _project;
        VirtualFile currentFile = LocalFileSystem.getInstance().findFileByIoFile(new File(currPath + "/pom.xml"));
        if(null == currentFile){
            NotifyService.error(project,"当前路径下没有pom.xml,不支持在当前路径下支持grpc");
            return false;
        }
        Module[] modules = ModuleManager.getInstance(project).getModules();
        if(currPath.equals(project.getBasePath()) && modules.length > 1){
            NotifyService.error(project,"根目录下不可以创建Provider或Consumer");
            return false;
        }
        //查找根目录的pom文件
        VirtualFile pom = getPom(project, null,"");

        //获取根目录下pom的groupId、artifactId、version
        init(project, pom);
        ApplicationManager.getApplication().invokeLater(() -> {
            WriteCommandAction.runWriteCommandAction(project, () -> {
                Navigatable[] navigatables = _event.getData(CommonDataKeys.NAVIGATABLE_ARRAY);
                if (navigatables == null || navigatables.length == 0)
                    return;
                PsiDirectoryNode directoryNode = (PsiDirectoryNode) navigatables[0];
                if(modules.length > 1){
                    for (Module module : modules) {
                        if (directoryNode.getName() != null && !directoryNode.getName().endsWith(module.getName())) {
                            continue;
                        }
                        VirtualFile grpcFile = LocalFileSystem.getInstance().findFileByIoFile(new File(project.getBasePath() + "/grpc.xml"));
                        String grpcModelName = getGrpcModelName(project, grpcFile);
                        if(StringUtils.isNotBlank(grpcModelName)){
                            String[] split = grpcModelName.split("/");
                            grpcModelName = split[split.length-1];
                        }
                        //POM 引用 gRPC 模块
                        initModulePom(project, currentFile,grpcModelName);
                        //给application添加@EnableDubbo注解
                        initApplication(project, module);
                        //yml
                        int index = project.getBasePath().length() + 1;
                        String path = currPath.substring(index , currPath.length());
                        initYml(project, module,path);
                    }
                }else{
                    //给application添加@EnableDubbo注解
                    initApplication(project, null);
                    //yml
                    initYml(project, null,"");
                }
                //消费者的话添加grpc-service.xml
                if(isConsumer){
                    String path = "";
                    if(!currPath.equals(project.getBasePath())){
                        int index = project.getBasePath().length() + 1;
                        path = currPath.substring(index , currPath.length()) + "/src/main/resources";
                    }else{
                        path = "/src/main/resources";
                    }
                    PsiDirectory directory = PsiService.createAndGetDirectory(project, path);
                    addGrpcService(directory);
                }
            });
        });
        refreshProject();
        return true;
    }

    boolean hasGrpc(){
        if(ModuleManager.getInstance(project).findModuleByName("grpc") == null)
            return false;
        String path = project.getBasePath().concat("/grpc");
        File file = new File(path);
        if(file == null)
            return false;
        return true;
    }


    void refreshProject(){
        ExternalSystemProjectNotificationAware projectNotificationAware = ExternalSystemProjectNotificationAware.getInstance(project);
        Set<ProjectSystemId> systemIds = projectNotificationAware.getSystemIds();
        if (ExternalSystemUtil.confirmLoadingUntrustedProject(project, systemIds)) {
            ExternalSystemProjectTracker projectTracker = ExternalSystemProjectTracker.getInstance(project);
            projectTracker.scheduleProjectRefresh();
        }
    }

    void init(Project project, VirtualFile pom) {
        XmlFile file = (XmlFile) PsiManager.getInstance(project).findFile(pom);
        XmlDocument document = file.getDocument();
        XmlTag root = document.getRootTag();
        groupId = root.getSubTagText("groupId");
        artifactId = root.getSubTagText("artifactId");
        version = root.getSubTagText("version");
    }

    String getGrpcModelName(Project project, VirtualFile pom){
        XmlFile file = (XmlFile) PsiManager.getInstance(project).findFile(pom);
        XmlDocument document = file.getDocument();
        XmlTag root = document.getRootTag();
        XmlTag parent = getChildByName(root, "grpc_module_path");
        if(null != parent ){
            return parent.getValue().getText();
        }
        return null;
    }

    boolean springbootSupport(Project project, VirtualFile pom) {
        XmlFile file = (XmlFile) PsiManager.getInstance(project).findFile(pom);
        XmlDocument document = file.getDocument();
        XmlTag root = document.getRootTag();
        XmlTag parent = getChildByName(root, "parent");
        if (parent == null
                || (!parent.getSubTagText("groupId").equals("org.springframework.boot"))
                || (!parent.getSubTagText("artifactId").equals("spring-boot-starter-parent"))) {

            NotifyService.error(project,"非Springboot项目，不支持转gRPC");
            return false;
        }
        String version = parent.getSubTagText("version");
        if (version.compareTo("2.5.0") < 0) {
            NotifyService.error(project,"Springboot版本太低，请先升级到 >= 2.5.0 后重试");
            return false;
        }
        return true;
    }

    XmlTag getChildByName(XmlTag parent,String childName){
        for (XmlTag subTag : parent.getSubTags()) {
            if(subTag.getName().equals(childName))
                return subTag;
        }
        return null;
    }


    void initModule(Project project) {
        //ModuleManager.getInstance(project).newModule(project.getBasePath() + "/grpc/grpc.iml", StdModuleTypes.JAVA.getId());
        //module 根目录
        PsiDirectory directory = PsiService.createAndGetDirectory(project,"grpc");
        PsiService.createAndGetDirectory(project,"grpc/src/main/proto/autohome/rpc");
        //添加pom文件
        addPom(directory, groupId);
        //添加配置文件
        //addYml(main);
    }
    void initProtoDir(Project project,String path) {
        PsiService.createAndGetDirectory(project,path);
    }
    void initProtoDir(Project project) {
        PsiService.createAndGetDirectory(project,"src/main/proto/autohome/rpc");
    }

    void addPom(PsiDirectory directory, String groupId) {
        PsiFile pom = directory.findFile("pom.xml");
        if(pom!=null) {
            pom.delete();
        }
        byte[] bytes = ResourceFileUtils.read("templates/pom.xml");
        String temp = new String(bytes, Charset.forName("UTF-8"));
        temp = temp
                .replace("${groupId}", groupId)
                .replace("${artifactId}", artifactId)
                .replace("${version}", version).replace("\r\n", "\n");
        PsiFile file = PsiFileFactory.getInstance(project).createFileFromText("pom.xml", XmlFileType.INSTANCE, temp);
        directory.add(file);
        //把pom添加为maven项目
        MavenProjectsManager manager = MavenProjectsManager.getInstance(project);
        manager.addManagedFiles(Collections.singletonList(directory.findFile("pom.xml").getVirtualFile()));

    }

    void addGrpcService(PsiDirectory directory) {
        PsiFile pom = directory.findFile("grpc-services.xml");
        if(pom != null) {
            pom.delete();
        }
        byte[] bytes = ResourceFileUtils.read("templates/grpc-services.xml");
        String temp = new String(bytes, Charset.forName("UTF-8"));
        temp = temp.replace("\r\n", "\n");
        PsiFile file = PsiFileFactory.getInstance(project).createFileFromText("grpc-services.xml", XmlFileType.INSTANCE, temp);
        directory.add(file);
        //把pom添加为maven项目
        MavenProjectsManager manager = MavenProjectsManager.getInstance(project);
        manager.addManagedFiles(Collections.singletonList(directory.findFile("grpc-services.xml").getVirtualFile()));

    }


    VirtualFile getPom(Project project, Module module,String modulePath) {
        String path = module == null ? project.getBasePath() + "/pom.xml" : getModulePath(modulePath) + "/pom.xml";
        File file = new File(path);
        VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByIoFile(file);
        return virtualFile;
    }

    void initPom(Project project, VirtualFile virtualFile) {
        if (virtualFile == null)
            return;

        XmlFile file = (XmlFile) PsiManager.getInstance(project).findFile(virtualFile);
        XmlDocument document = file.getDocument();
        XmlTag root = document.getRootTag();
        XmlTag dependencies = getOrCreateSubTag(root, "dependencies");
        boolean dubboExists = false;
        boolean grpc = false;
        boolean protobufExists = false;
        for (XmlTag subTag : dependencies.getSubTags()) {
            String artifactId = subTag.getSubTagText("artifactId");
            switch (artifactId) {
                case "dubbo":
                    dubboExists = true;
                    break;
                case "grpc-all":
                    grpc = true;
                    break;
                case "protobuf-java":
                    protobufExists = true;
                    break;
                default:
                    continue;
            }
        }
        if (!dubboExists) {
            String body = "" +
                    "            <groupId>org.apache.dubbo</groupId>\n" +
                    "            <artifactId>dubbo</artifactId>\n" +
                    "            <version>${dubbo.version}</version>\n";
            addChildTag(project, dependencies, "dependency", body);
        }
        if (!protobufExists) {
            String body = "" +
                    "            <groupId>com.google.protobuf</groupId>\n" +
                    "            <artifactId>protobuf-java</artifactId>\n" +
                    "            <version>${protoc.version}</version>\n";
            addChildTag(project, dependencies, "dependency", body);
        }
        if (!grpc) {
            String body = "" +
                    "            <groupId>io.grpc</groupId>\n" +
                    "            <artifactId>grpc-all</artifactId>\n" +
                    "            <version>${grpc.version}</version>\n";
            addChildTag(project, dependencies, "dependency", body);
        }

        initProperties(project, root);

        XmlTag build = getOrCreateSubTag(root, "build");

        XmlTag extensions = getOrCreateSubTag(build,"extensions");
        if(!Arrays.stream(extensions.getSubTags()).anyMatch(x->x.getSubTagText("artifactId").equals("os-maven-plugin"))){
            String extension = "" +
                    "                <groupId>kr.motd.maven</groupId>\n" +
                    "                <artifactId>os-maven-plugin</artifactId>\n" +
                    "                <version>1.6.1</version>";

            addChildTag(project, extensions, "extension", extension);
        }


        XmlTag plugins = getOrCreateSubTag(build, "plugins");

        if (!Arrays.stream(plugins.getSubTags()).anyMatch(x -> x.getSubTagText("artifactId").equals("protobuf-maven-plugin"))) {
            String body = "" +
                    "                <groupId>org.xolstice.maven.plugins</groupId>\n" +
                    "                <artifactId>protobuf-maven-plugin</artifactId>\n" +
                    "                <version>0.6.1</version>\n" +
                    "                <configuration>\n" +
                    "                    <protocArtifact>com.google.protobuf:protoc:${protoc.version}:exe:${os.detected.classifier}</protocArtifact>\n" +
                    "                    <protocPlugins>\n" +
                    "                        <protocPlugin>\n" +
                    "                            <id>dubbo</id>\n" +
                    "                            <groupId>org.apache.dubbo</groupId>\n" +
                    "                            <artifactId>dubbo-compiler</artifactId>\n" +
                    "                            <version>${dubbo.version}</version>\n" +
                    "                            <mainClass>org.apache.dubbo.gen.tri.Dubbo3TripleGenerator</mainClass>\n" +
                    "                        </protocPlugin>\n" +
                    "                    </protocPlugins>\n" +
                    "                </configuration>\n" +
                    "                <executions>\n" +
                    "                    <execution>\n" +
                    "                        <goals>\n" +
                    "                            <goal>compile</goal>\n" +
                    "                        </goals>\n" +
                    "                    </execution>\n" +
                    "                </executions>";
            addChildTag(project, plugins, "plugin", body);
        }
    }


    void initRootPomNew(Project project, VirtualFile virtualFile){
        if (virtualFile == null) {
            return;
        }

        XmlFile file = (XmlFile) PsiManager.getInstance(project).findFile(virtualFile);
        XmlDocument document = file.getDocument();
        XmlTag root = document.getRootTag();
        //版本
        initProperties(project, root);
        XmlTag dependencyManagement = getOrCreateSubTag(root, "dependencyManagement");

        XmlTag dependencies = getOrCreateSubTag(dependencyManagement, "dependencies");
        boolean dubboExists = false;
        boolean grpc = false;
        boolean protobufExists = false;
        for (XmlTag subTag : dependencies.getSubTags()) {
            String artifactId = subTag.getSubTagText("artifactId");
            switch (artifactId) {
                case "dubbo":
                    dubboExists = true;
                    break;
                case "grpc-all":
                    grpc = true;
                    break;
                case "protobuf-java":
                    protobufExists = true;
                    break;
                default:
                    continue;
            }
        }
        if (!dubboExists) {
            String body = "" +
                    "                <groupId>org.apache.dubbo</groupId>\n" +
                    "                <artifactId>dubbo</artifactId>\n" +
                    "                <version>${dubbo.version}</version>\n";
            addChildTag(project, dependencies, "dependency", body);
        }
        if (!protobufExists) {
            String body = "" +
                    "                <groupId>com.google.protobuf</groupId>\n" +
                    "                <artifactId>protobuf-java</artifactId>\n" +
                    "                <version>${protoc.version}</version>\n";
            addChildTag(project, dependencies, "dependency", body);
        }
        if (!grpc) {
            String body = "" +
                    "                <groupId>io.grpc</groupId>\n" +
                    "                <artifactId>grpc-all</artifactId>\n" +
                    "                <version>${grpc.version}</version>\n";

            addChildTag(project, dependencies, "dependency", body);
        }

    }

    void initModulePomNew(Project project, VirtualFile virtualFile) {
        if (virtualFile == null) {
            return;
        }
        XmlFile file = (XmlFile) PsiManager.getInstance(project).findFile(virtualFile);
        XmlDocument document = file.getDocument();
        XmlTag root = document.getRootTag();
        XmlTag dependencies = getOrCreateSubTag(root, "dependencies");
        boolean dubboExists = false;
        boolean grpc = false;
        boolean protobufExists = false;
        for (XmlTag subTag : dependencies.getSubTags()) {
            String artifactId = subTag.getSubTagText("artifactId");
            switch (artifactId) {
                case "dubbo":
                    dubboExists = true;
                    break;
                case "grpc-all":
                    grpc = true;
                    break;
                case "protobuf-java":
                    protobufExists = true;
                    break;
                default:
                    continue;
            }
        }
        if (!dubboExists) {
            String body = "" +
                    "            <groupId>org.apache.dubbo</groupId>\n" +
                    "            <artifactId>dubbo</artifactId>\n" ;
            addChildTag(project, dependencies, "dependency", body);
        }
        if (!protobufExists) {
            String body = "" +
                    "            <groupId>com.google.protobuf</groupId>\n" +
                    "            <artifactId>protobuf-java</artifactId>\n" ;
            addChildTag(project, dependencies, "dependency", body);
        }
        if (!grpc) {
            String body = "" +
                    "            <groupId>io.grpc</groupId>\n" +
                    "            <artifactId>grpc-all</artifactId>\n" ;
            addChildTag(project, dependencies, "dependency", body);
        }

        XmlTag build = getOrCreateSubTag(root, "build");

        XmlTag extensions = getOrCreateSubTag(build,"extensions");
        if(!Arrays.stream(extensions.getSubTags()).anyMatch(x->x.getSubTagText("artifactId").equals("os-maven-plugin"))){
            String extension = "" +
                    "                <groupId>kr.motd.maven</groupId>\n" +
                    "                <artifactId>os-maven-plugin</artifactId>\n" +
                    "                <version>1.6.1</version>";

            addChildTag(project, extensions, "extension", extension);
        }


        XmlTag plugins = getOrCreateSubTag(build, "plugins");

        if (!Arrays.stream(plugins.getSubTags()).anyMatch(x -> x.getSubTagText("artifactId").equals("protobuf-maven-plugin"))) {
            String body = "" +
                    "                <groupId>org.xolstice.maven.plugins</groupId>\n" +
                    "                <artifactId>protobuf-maven-plugin</artifactId>\n" +
                    "                <version>0.6.1</version>\n" +
                    "                <configuration>\n" +
                    "                    <protocArtifact>com.google.protobuf:protoc:${protoc.version}:exe:${os.detected.classifier}</protocArtifact>\n" +
                    "                    <protocPlugins>\n" +
                    "                        <protocPlugin>\n" +
                    "                            <id>dubbo</id>\n" +
                    "                            <groupId>org.apache.dubbo</groupId>\n" +
                    "                            <artifactId>dubbo-compiler</artifactId>\n" +
                    "                            <version>${dubbo.version}</version>\n" +
                    "                            <mainClass>org.apache.dubbo.gen.tri.Dubbo3TripleGenerator</mainClass>\n" +
                    "                        </protocPlugin>\n" +
                    "                    </protocPlugins>\n" +
                    "                </configuration>\n" +
                    "                <executions>\n" +
                    "                    <execution>\n" +
                    "                        <goals>\n" +
                    "                            <goal>compile</goal>\n" +
                    "                        </goals>\n" +
                    "                    </execution>\n" +
                    "                </executions>";
            addChildTag(project, plugins, "plugin", body);
        }
    }


    void initRootPom(Project project, VirtualFile virtualFile) {
        if (virtualFile == null)
            return;

        XmlFile file = (XmlFile) PsiManager.getInstance(project).findFile(virtualFile);
        XmlDocument document = file.getDocument();
        XmlTag root = document.getRootTag();

        initProperties(project, root);

        XmlTag modules = getOrCreateSubTag(root, "modules");
        XmlTag[] subTags = modules.getSubTags();
        boolean hasGrpc = false;
        for (XmlTag subTag : subTags) {
            String text = subTag.getValue().getText();
            if (StringUtils.equals(text, "grpc")) {
                hasGrpc = true;
                break;
            }
        }
        if(!hasGrpc){
            addChildTag(project, modules, "module", "grpc");
        }


        XmlTag dependencyManagement = getOrCreateSubTag(root, "dependencyManagement");

        XmlTag dependencies = getOrCreateSubTag(dependencyManagement, "dependencies");
        boolean dubboExists = false;
        boolean grpc = false;
        boolean protobufExists = false;
        boolean grpcModule = false;
        for (XmlTag subTag : dependencies.getSubTags()) {
            String artifactId = subTag.getSubTagText("artifactId");
            switch (artifactId) {
                case "dubbo":
                    dubboExists = true;
                    break;
                case "grpc-all":
                    grpc = true;
                    break;
                case "protobuf-java":
                    protobufExists = true;
                    break;
                case "grpc":
                    grpcModule = true;
                    break;
                default:
                    continue;
            }
        }
        if (!dubboExists) {
            String body = "" +
                    "                <groupId>org.apache.dubbo</groupId>\n" +
                    "                <artifactId>dubbo</artifactId>\n" +
                    "                <version>${dubbo.version}</version>\n";
            addChildTag(project, dependencies, "dependency", body);
        }
        if (!protobufExists) {
            String body = "" +
                    "                <groupId>com.google.protobuf</groupId>\n" +
                    "                <artifactId>protobuf-java</artifactId>\n" +
                    "                <version>${protoc.version}</version>\n";
            addChildTag(project, dependencies, "dependency", body);
        }
        if (!grpc) {
            String body = "" +
                    "                <groupId>io.grpc</groupId>\n" +
                    "                <artifactId>grpc-all</artifactId>\n" +
                    "                <version>${grpc.version}</version>\n";

            addChildTag(project, dependencies, "dependency", body);
        }
        if (!grpcModule) {
            String body = "" +
                    "            <groupId>" + groupId + "</groupId>\n" +
                    "            <artifactId>grpc</artifactId>\n" +
                    "            <version>" + version + "</version>\n";
            addChildTag(project, dependencies, "dependency", body);
        }
    }

    private void initProperties(Project project, XmlTag root) {
        XmlTag properties = getOrCreateSubTag(root, "properties");

        if(StringUtils.isBlank(properties.getSubTagText("dubbo.version"))){
            addChildTag(project, properties, "dubbo.version", "3.2.5");
        }
        if(StringUtils.isBlank(properties.getSubTagText("protoc.version"))){
            addChildTag(project, properties, "protoc.version", "3.21.7");
        }
        if(StringUtils.isBlank(properties.getSubTagText("grpc.version"))){
            addChildTag(project, properties, "grpc.version", "1.52.0");
        }
    }

    void initModulePom(Project project, VirtualFile virtualFile) {
        if (virtualFile == null)
            return;

        XmlFile file = (XmlFile) PsiManager.getInstance(project).findFile(virtualFile);
        XmlDocument document = file.getDocument();
        XmlTag root = document.getRootTag();
        XmlTag dependencies = getOrCreateSubTag(root, "dependencies");
        boolean grpcExists = Arrays.stream(dependencies.getSubTags()).anyMatch(x -> x.getSubTagText("artifactId").equals("grpc") && x.getSubTagText("groupId").equals(groupId));
        if (!grpcExists) {
            String body = "" +
                    "            <groupId>" + groupId + "</groupId>\n" +
                    "            <artifactId>grpc</artifactId>\n" +
                    "            <version>"+version+"</version>";
            addChildTag(project, dependencies, "dependency", body);
        }
    }

    void initModulePom(Project project, VirtualFile virtualFile,String artifactId) {
        if (virtualFile == null)
            return;

        XmlFile file = (XmlFile) PsiManager.getInstance(project).findFile(virtualFile);
        XmlDocument document = file.getDocument();
        XmlTag root = document.getRootTag();
        XmlTag dependencies = getOrCreateSubTag(root, "dependencies");
        boolean grpcExists = Arrays.stream(dependencies.getSubTags()).anyMatch(x -> x.getSubTagText("artifactId").equals("grpc") && x.getSubTagText("groupId").equals(groupId));
        if (!grpcExists) {
            String body = "" +
                    "            <groupId>" + groupId + "</groupId>\n" +
                    "            <artifactId>" + artifactId + "</artifactId>\n" +
                    "            <version>"+version+"</version>";
            addChildTag(project, dependencies, "dependency", body);
        }
    }

    void addChildTag(Project project, XmlTag parent, String name, String body) {
        parent.addSubTag(parent.createChildTag(name, "", body, false),parent.getSubTags().length==0);
    }


    XmlTag getOrCreateSubTag(XmlTag parent, String name) {
        XmlTag sub = getSubTag(parent,name);
        if(sub!=null)
            return sub;
        XmlTag newTag = parent.createChildTag(name, "", "", false);
        parent.addSubTag(newTag, parent.getSubTags().length == 0);
        return getSubTag(parent,name);
    }

    XmlTag getSubTag(XmlTag parent ,String name){
        for (XmlTag tag : parent.getSubTags()) {
            if (!tag.getName().equals(name)) continue;
            return tag;
        }
        return null;
    }

    VirtualFile findYml(Project project, Module module,String basePath) {
        String ymlName = "application.yml";
        String mPath = basePath;
        String vPath = (module == null ? "" : mPath) + "/src/main/resources";
        PsiDirectory directory = PsiService.createAndGetDirectory(project,vPath);
        VirtualFile file = directory.getVirtualFile().findChild(ymlName);
        if(file!=null){
            return file;
        }
        return createFile(directory.getVirtualFile(),ymlName);
    }



    void initYml(Project project, Module module,String basePath) {
        VirtualFile virtualFile = findYml(project, module,basePath);
        if (virtualFile == null)
            return;


        ApplicationManager.getApplication().runReadAction(() -> {
            try (InputStream inputStream = virtualFile.getInputStream()) {
                StringBuilder contentBuilder = new StringBuilder();
                Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8.name()).useDelimiter("\\A");
                while (scanner.hasNextLine()) {
                    contentBuilder.append(scanner.nextLine()).append("\n");
                }
                Yaml yaml = new Yaml();
                Map<String, Object> data = yaml.load(contentBuilder.toString());

                if(data==null){
                    data = new LinkedHashMap<>();
                }
                if (!data.containsKey("dubbo")) {
                    data.put("dubbo", getDubboConfig());
                }

                DumperOptions options = new DumperOptions();
                options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
                Yaml dumper = new Yaml(options);
                String updatedContent = dumper.dump(data);

                WriteCommandAction.runWriteCommandAction(null, () -> {
                    try {
                        virtualFile.setBinaryContent(updatedContent.getBytes(StandardCharsets.UTF_8));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
//
//        YAMLFileImpl yamlFile = (YAMLFileImpl) PsiManager.getInstance(project).findFile(virtualFile);
//        YAMLDocument document = yamlFile.getDocuments().get(0);
//        YAMLElementGenerator generator = YAMLElementGenerator.getInstance(project);
//        YAMLKeyValue dubbo = null;
//        for (PsiElement child : document.getChildren()) {
//            YAMLBlockMappingImpl map = (YAMLBlockMappingImpl) child;
//            dubbo = map.getKeyValueByKey("dubbo");
//            if (dubbo != null)
//                break;
//        }
//        if (dubbo == null) {
//            YAMLFile dubboFile = generator.createDummyYamlWithText(dubboConfig.replace("${appName}",groupId.replace(".","-").concat("-").concat(artifactId)));
//            Collection<YAMLValue> values = PsiTreeUtil.collectElementsOfType(dubboFile, new Class[]{YAMLValue.class});
//            YAMLValue value = values.stream().collect(Collectors.toList()).get(0);
//            document.addAfter(value,document.getLastChild());
//        }
        return;
    }

    MethodResult initApplication(Project project, Module module) {
        GlobalSearchScope scope = module == null ? GlobalSearchScope.projectScope(project) : GlobalSearchScope.moduleScope(module);
        Collection<VirtualFile> virtualFiles = FileTypeIndex.getFiles(JavaFileType.INSTANCE, scope);
        String basePath = "";
        for (VirtualFile virtualFile : virtualFiles) {
            PsiFile simpleFile = PsiManager.getInstance(project).findFile(virtualFile);
            if (simpleFile == null) continue;

            PsiJavaFileImpl javaFile = (PsiJavaFileImpl) simpleFile;
            PsiClass[] clazzes = javaFile.getClasses();
            if (clazzes.length == 0)
                continue;
            PsiClass clazz = clazzes[0];
            PsiAnnotation[] annotations = clazz.getAnnotations();
            PsiAnnotation springbootAnnotation = Arrays.stream(annotations).filter(x -> x.getQualifiedName().equals("org.springframework.boot.autoconfigure.SpringBootApplication")).findFirst().orElse(null);

            if (springbootAnnotation == null) {
                continue;
            }
            String dubboAnnotationName = "org.apache.dubbo.config.spring.context.annotation.EnableDubbo";
            String path = virtualFile.getPath();
            path = path.substring(project.getBasePath().length()+1);
            path = path.substring(0, path.indexOf("/"));
            if (Arrays.stream(annotations).anyMatch(x -> x.getQualifiedName().equals(dubboAnnotationName))) {
                return MethodResult.success(path);
            }
            clazz.getModifierList().addAnnotation(dubboAnnotationName);

            return MethodResult.success(path);
        }
        return MethodResult.fail();
    }

    String getModulePath(String modulePath){
        return project.getBasePath().concat("/").concat(modulePath);
    }

    LinkedHashMap<String,Object> getDubboConfig() {
        LinkedHashMap<String, Object> application = new LinkedHashMap<>();
        application.put("name", groupId.replace(".","-").concat("-").concat(artifactId));
        application.put("qos-port", 22222);
        application.put("metadataServicePort", 20885);
        application.put("qosEnable", true);
        application.put("qosAcceptForeignIp", true);

        LinkedHashMap<String, Object> protocol = new LinkedHashMap<>();
        protocol.put("name", "tri");
        protocol.put("port", 50051);

        LinkedHashMap<String, Object> registry = new LinkedHashMap<>();
        registry.put("address", "N/A");

        LinkedHashMap<String, Object> consumer = new LinkedHashMap<>();
        consumer.put("meshEnable", true);

        LinkedHashMap<String, Object> service = new LinkedHashMap<>();
        service.put("init", true);

        LinkedHashMap<String, Object> builtin = new LinkedHashMap<>();
        builtin.put("service", service);

        LinkedHashMap<String, Object> tri = new LinkedHashMap<>();
        tri.put("builtin", builtin);

        LinkedHashMap<String, Object> configs = new LinkedHashMap<>();
        configs.put("application", application);
        configs.put("protocol", protocol);
        configs.put("registry", registry);
        configs.put("consumer", consumer);
        configs.put("tri", tri);

        return configs;
    }

    private VirtualFile createFile(VirtualFile directory, String fileName) {
        VirtualFile[] newFile = new VirtualFile[1];

        WriteCommandAction.runWriteCommandAction(null, () -> {
            try {
                newFile[0] = directory.createChildData(this, fileName);

                // 如果需要，可以在这里写入初始内容到文件中
                String initialContent = "# Initial content by plugin\n";
                newFile[0].setBinaryContent(initialContent.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        return newFile[0];
    }


}
