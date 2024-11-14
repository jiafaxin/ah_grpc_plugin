package com.autohome.ah_grpc_plugin.services;

import com.autohome.ah_grpc_plugin.Config;
import com.autohome.ah_grpc_plugin.factory.BufToolWindow;
import com.autohome.ah_grpc_plugin.utils.CommandUtils;
import com.autohome.ah_grpc_plugin.utils.FileDownloadUtils;
import com.autohome.ah_grpc_plugin.utils.LocalPathUtil;
import com.autohome.ah_grpc_plugin.utils.OsUtil;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.protobuf.lang.PbFileType;
import com.intellij.protobuf.lang.psi.impl.PbServiceDefinitionImpl;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service(Service.Level.PROJECT)
public final class GenProtoService {

    Project project;

    boolean hasGoEnv = false;

    boolean hasProtoc = false;

    boolean hasGengo = false;


    boolean hasGoWork = false;

    //go需要的grpc依赖
    private final static String GO_GRPC = "google.golang.org/grpc";

    //pb 大仓地址
    private final static String GIT_GO_GEN_PROTO = "git.corpautohome.com/microservice/go-genproto";

    public static GenProtoService getInstance(Project project) {


        GenProtoService service = project.getService(GenProtoService.class);
        if(service.project==null) {
            service.project = project;
        }
        return service;
    }

    //go 生成stub
    public void compile(List<String> protoPaths){
        if(CollectionUtils.isEmpty(protoPaths)){
            NotifyService.warning(project,"请先创建proto文件");
            return;
        }
        ConsoleView consoleView = BufToolWindow.getAndShowCompileView(project);
        compile(protoPaths,consoleView);
    }

    public CompletableFuture<Boolean> compile(List<String> protoPaths,ConsoleView consoleView) {
        return CompletableFuture.supplyAsync(()->{
            if(!hasGo(consoleView)){
                return false;
            }
            if(!hasProtoc(consoleView)){
                return false;
            }
            if(!hasGenGo(consoleView)){
                return false;
            }
            //下载pb大仓
            String path = project.getBasePath();
            int lastIndexOf = path.lastIndexOf("/");
            String pPath = path.substring(0,lastIndexOf);
            path = pPath.concat("/").concat("go-genproto");
            File file = new File(path);
            if(!file.exists()){
                runGenGit(Arrays.asList("git","clone","git@git.corpautohome.com:microservice/go-genproto.git"),consoleView,pPath);
            }
            //绑定到项目中
            if(!hasGoWork(consoleView)){
                return false;
            }

            List<String> cmds = new ArrayList<>();

            String filePath = LocalPathUtil.localPath().concat(protocFileName());

            cmds.add(filePath);
            String s1= "../go-genproto/";
            String s2 = String.join(" ",protoPaths.stream().map(x->"proto/"+x+"").collect(Collectors.toList()));

            cmds.add("--proto_path=./proto/");
            cmds.add(String.format("--go_out=%s",s1));
            cmds.add("--go_opt=paths=source_relative");
            cmds.add(String.format("--go-grpc_out=%s",s1));
            cmds.add("--go-grpc_opt=paths=source_relative");
            cmds.add(String.format("--grpc-gateway_out=%s",s1));
            cmds.add("--grpc-gateway_opt=paths=source_relative");
            cmds.add(s2);
            consoleView.print("[编译] 开始编译 \n",ConsoleViewContentType.NORMAL_OUTPUT);
            String result = runBackground(consoleView, cmds);
            if(StringUtils.isBlank(result)){
                consoleView.print("[编译] 编译完成 \n",ConsoleViewContentType.LOG_INFO_OUTPUT);
                return true;
            }else{
                consoleView.print("[编译] 编译出错：" + result,ConsoleViewContentType.NORMAL_OUTPUT);
                return false;
            }

        });
    }

    public boolean hasGenGo(ConsoleView consoleView){
        consoleView.print("[环境检查] 检测编译插件... \n", ConsoleViewContentType.NORMAL_OUTPUT);
        if(hasGengo==false){
            //protoc-gen-go grpc工具包
            if(!fileExists(gengoFileName())){
                consoleView.print("[环境检查] 未检测到编译插件:protoc-gen-go , 开始安装...  \n", ConsoleViewContentType.NORMAL_OUTPUT);
                runBackground(consoleView,Arrays.asList("go","install","google.golang.org/protobuf/cmd/protoc-gen-go@latest"));
                if(!fileExists(gengoFileName())){
                    consoleView.print("[环境检查] 插件protoc-gen-go安装失败，请查看以上错误信息  \n", ConsoleViewContentType.NORMAL_OUTPUT);
                    return false;
                }else{
                    consoleView.print("[环境检查] 插件protoc-gen-go安装成功 \n", ConsoleViewContentType.LOG_INFO_OUTPUT);
                }
            }

            //protoc-gen-go-grpc grpc工具包
            if(!fileExists(gengoGrpcFileName())) {
                consoleView.print("[环境检查] 未检测到编译插件： protoc-gen-go-grpc , 开始安装...   \n", ConsoleViewContentType.NORMAL_OUTPUT);
                runBackground(consoleView,Arrays.asList("go","install","google.golang.org/grpc/cmd/protoc-gen-go-grpc@latest"));
                if(!fileExists(gengoGrpcFileName())){
                    consoleView.print("[环境检查] 插件protoc-gen-go-grpc安装失败，请查看以上错误信息  \n", ConsoleViewContentType.NORMAL_OUTPUT);
                    return false;
                }else{
                    consoleView.print("[环境检查] 插件protoc-gen-go-grpc安装成功 \n", ConsoleViewContentType.LOG_INFO_OUTPUT);
                }
            }
            //protoc-gen-grpc-gateway grpc工具包
            if(!fileExists(getGoGrpcGateWayFileName())){
                consoleView.print("[环境检查] 未检测到编译插件： protoc-gen-grpc-gateway , 开始安装...   \n", ConsoleViewContentType.NORMAL_OUTPUT);
                runBackground(consoleView,Arrays.asList("go","install","github.com/grpc-ecosystem/grpc-gateway/v2/protoc-gen-grpc-gateway@latest"));
                if(!fileExists(getGoGrpcGateWayFileName())){
                    consoleView.print("[环境检查] 插件protoc-gen-grpc-gateway安装失败，请查看以上错误信息  \n", ConsoleViewContentType.NORMAL_OUTPUT);
                    return false;
                }else{
                    consoleView.print("[环境检查] 插件protoc-gen-grpc-gateway安装成功 \n", ConsoleViewContentType.LOG_INFO_OUTPUT);
                }
            }
            hasGengo = true;
        }
        consoleView.print("[环境检查] 所有插件已经安装... \n", ConsoleViewContentType.LOG_INFO_OUTPUT);
        return true;
    }

    boolean fileExists(String name){
        String path = goEnv("GOPATH").concat(File.separator).concat("bin").concat(File.separator).concat(name);
        return new File(path).exists();
    }

    String goEnv(String name){
        String result = run(Arrays.asList("go","env",name));
        if(StringUtils.isNotBlank(result)){
            result = result.replace("\n","");
        }
        return result;
    }


    public boolean hasProtoc(ConsoleView consoleView) {
        consoleView.print("[环境检查] protoc 环境检查... \n", ConsoleViewContentType.NORMAL_OUTPUT);
        if (hasProtoc == false) {
            String checksumFilePath = LocalPathUtil.localPath().concat(protocChecksumFileName());
            String filePath = LocalPathUtil.localPath().concat(protocFileName());
            if(FileDownloadUtils.check(checksumFilePath,protocChecksumUrl()) && (new File(filePath).exists())){
                hasProtoc = true;
                consoleView.print("[环境检查] protoc 环境检查 通过 \n", ConsoleViewContentType.LOG_INFO_OUTPUT);
            }else{
                consoleView.print("[环境检查] 未检测到 protoc 环境，开始安装环境 \n", ConsoleViewContentType.NORMAL_OUTPUT);
                try {
                    FileService.download(protocChecksumUrl(), LocalPathUtil.localPath(), protocChecksumFileName());
                    FileService.download(protocUrl(), LocalPathUtil.localPath(), protocFileName());
                    hasProtoc = true;
                } catch (Exception e) {
                    consoleView.print("[环境检查] protoc 安装环境失败：" + e.getMessage() + " \n", ConsoleViewContentType.LOG_ERROR_OUTPUT);
                    return false;
                }
            }
        }else{
            consoleView.print("[环境检查] protoc 环境检查已经通过 \n", ConsoleViewContentType.LOG_INFO_OUTPUT);
        }
        return hasProtoc;
    }


    boolean hasGoEnv(String name){
        return StringUtils.isNotBlank(goEnv(name));
    }

    public boolean hasGo(ConsoleView consoleView) {
        consoleView.print("[环境检查] go 环境检查... \n", ConsoleViewContentType.NORMAL_OUTPUT);
        if (hasGoEnv == false) {
            if (!hasGoEnv("GOPATH")) {
                consoleView.print("[环境检查] 未检测到环境变量：GOPATH \n", ConsoleViewContentType.LOG_ERROR_OUTPUT);
            } else {
                if (StringUtils.isBlank(run(Arrays.asList("go","version")))) {
                    consoleView.print("[环境检查] 命令go version未返回任何数据 \n", ConsoleViewContentType.LOG_ERROR_OUTPUT);
                } else {
                    hasGoEnv = true;
                }
            }
        }

        if (hasGoEnv) {
            consoleView.print("[环境检查] go 环境检查 通过 \n", ConsoleViewContentType.LOG_INFO_OUTPUT);
        } else {
            consoleView.print("[环境检查] go 环境检查 未通过，请正确安装go环境后重试 \n", ConsoleViewContentType.LOG_ERROR_OUTPUT);
        }
        return hasGoEnv;
    }


    public String run(List<String> cmds) {
        String path = Config.getInstance(project).grpcPath();
        return run(path,cmds);
    }

    public String run(String path,List<String> cmds) {
        return CommandUtils.run(cmds,path);
    }

    public String runBackground(ConsoleView consoleView, List<String> cmds) {
        return CommandUtils.runBackground(cmds,Config.getInstance(project).grpcPath(),consoleView);
    }

    String protocFileName(){
        String fileName = "protoc";
        if(OsUtil.isWindows()){
            return fileName.concat(".exe");
        }
        return fileName;
    }

    String protocChecksumFileName(){
        String fileName = "protoc";
        if(OsUtil.isWindows()){
            return fileName.concat(".exe.checksum");
        }
        return fileName.concat(".checksum");
    }

    String gengoFileName(){
        String fileName = "protoc-gen-go";
        if(OsUtil.isWindows()){
            return fileName.concat(".exe");
        }
        return fileName;
    }

    String gengoGrpcFileName(){
        String fileName = "protoc-gen-go-grpc";
        if(OsUtil.isWindows()){
            return fileName.concat(".exe");
        }
        return fileName;
    }

    String gengoTripleFileName(){
        String fileName = "protoc-gen-go-triple";
        if(OsUtil.isWindows()){
            return fileName.concat(".exe");
        }
        return fileName;
    }

    String gengoDubbo3GrpcFileName(){
        String fileName = "protoc-gen-dubbo3grpc";
        if(OsUtil.isWindows()){
            return fileName.concat(".exe");
        }
        return fileName;
    }

    String getGoGrpcGateWayFileName(){
        String fileName = "protoc-gen-grpc-gateway";
        if(OsUtil.isWindows()){
            return fileName.concat(".exe");
        }
        return fileName;
    }


    String protocUrl(){
        return baseUrl() + protocFileName();
    }

    String protocChecksumUrl(){
        return baseUrl() + protocChecksumFileName();
    }

    String baseUrl(){
        String baseUrl = "http://nfiles3four.in.autohome.com.cn/autohome-grpc-protoc/grpc/";
        if (OsUtil.isWindows()){
            return baseUrl + "windows/";
        }else{
            return baseUrl + "os/";
        }
    }

    public boolean push(ConsoleView consoleView,String commitText){
        String path = project.getBasePath();
        int lastS = path.lastIndexOf("/");
        String pPath = path.substring(0,lastS);
        //获取路径
        path = pPath.concat("/").concat("go-genproto");
        //go pb文件提交到gitlab仓库
        runGenGit(Arrays.asList("git","add ."),consoleView,path);
        runGenGit(Arrays.asList("git","commit -m \""+commitText+"\""),consoleView,path);
        runGenGit(Arrays.asList("git","pull"),consoleView,path);
        runGenGit(Arrays.asList("git","push"),consoleView,path);
        return true;
    }

    public String runGenGit(List<String> cmds,ConsoleView consoleView,String path) {
        String result = run(path, cmds);
        consoleView.print(path.concat(" > ") .concat(String.join(" ",cmds) + "\n"), ConsoleViewContentType.LOG_INFO_OUTPUT);
        consoleView.print(result,ConsoleViewContentType.NORMAL_OUTPUT);
        consoleView.print("\n", ConsoleViewContentType.NORMAL_OUTPUT);
        return result;
    }



    public void goProviderCheck(){
        ConsoleView consoleView = BufToolWindow.getAndShowCompileView(project);
        this.goProviderCheck(consoleView);
    }


    public CompletableFuture<Boolean> goProviderCheck(ConsoleView consoleView) {
        return CompletableFuture.supplyAsync(()->{
            //判断是都有go环境
            if(!hasGo(consoleView)){
                return false;
            }
            //protoc 环境检查
            if(!hasProtoc(consoleView)){
                return false;
            }
            hasGenGo(consoleView);
            hasGoDependent(consoleView);
            consoleView.print("[插件依赖检查] 插件和依赖已成功安装... \n", ConsoleViewContentType.LOG_INFO_OUTPUT);
            return true;
        });
    }

    public void goConsumerCheck(){
        ConsoleView consoleView = BufToolWindow.getAndShowCompileView(project);
        this.goConsumerCheck(consoleView);
    }

    public CompletableFuture<Boolean> goConsumerCheck(ConsoleView consoleView){
        return CompletableFuture.supplyAsync(()->{
            //判断是否有go环境
            if(!hasGo(consoleView)){
                return false;
            }
            String line = run(Arrays.asList("go","list","-m","all"));
            hasGoGenProtoDependent(line,consoleView);
            consoleView.print("[依赖检查] 所有依赖已安装... \n", ConsoleViewContentType.LOG_INFO_OUTPUT);
            return true;
        });
    }

    public void hasGoDependent(ConsoleView consoleView){
        //获取当前项目下的所有依赖
        String line = run(Arrays.asList("go","list","-m","all"));
        hasGoGrpcDependent(line,consoleView);
        hasGoGenProtoDependent(line,consoleView);
        consoleView.print("[依赖检查] 所有依赖已安装... \n", ConsoleViewContentType.LOG_INFO_OUTPUT);
    }

    /**
     * go grpc 依赖
     * @param line
     * @param consoleView
     */
    public void hasGoGrpcDependent(String line,ConsoleView consoleView){
        //google.golang.org/grpc
        if(StringUtils.isBlank(line) || !line.contains(GO_GRPC)){
            consoleView.print("[依赖检查] 未检测到依赖:google.golang.org/grpc , 开始获取依赖...  \n", ConsoleViewContentType.NORMAL_OUTPUT);
            runBackground(consoleView,Arrays.asList("go","get","-u","google.golang.org/grpc"));
        }else{
            consoleView.print("[依赖检查] 检测到依赖:google.golang.org/grpc 已安装... \n", ConsoleViewContentType.LOG_INFO_OUTPUT);
        }
    }

    /**
     * go pb大仓依赖
     * @param line
     * @param consoleView
     */
    public void hasGoGenProtoDependent(String line,ConsoleView consoleView){
        //git.corpautohome.com/microservice/go-genproto
        if(StringUtils.isBlank(line) || !line.contains(GIT_GO_GEN_PROTO)){
            consoleView.print("[依赖检查] 未检测到依赖:git.corpautohome.com/microservice/go-genproto , 开始获取依赖...  \n", ConsoleViewContentType.NORMAL_OUTPUT);
            runBackground(consoleView,Arrays.asList("go","get","-u","git.corpautohome.com/microservice/go-genproto"));
        }else{
            consoleView.print("[依赖检查] 检测到依赖:git.corpautohome.com/microservice/go-genproto 已安装... \n", ConsoleViewContentType.LOG_INFO_OUTPUT);
        }
    }


    public boolean hasGoWork(ConsoleView consoleView){
        if(!hasGoWork){
            String genProto = "../go-genproto";
            String rootPath = project.getBasePath();
            File workFile = new File(rootPath + "/go.work");
            //有go work
            if(!workFile.exists()){
                runBackground(consoleView,Arrays.asList("go","work","init"));
            }
            runBackground(consoleView,Arrays.asList("go","work","use","."));
            runBackground(consoleView,Arrays.asList("go","work","use",genProto));
            hasGoWork = true;
        }
        return true;
    }

    /**
     * 获取指定路径下的所有proto文件
     * @param directoryPath
     * @return
     */
    public List<String> getAllProtoFilesPaths(String directoryPath) {
        // 使用 VfsUtil 从项目的基础目录找到相对路径的 VirtualFile
        VirtualFile root = VfsUtil.findRelativeFile(directoryPath, project.getBaseDir());
        if (root == null) {
            return new ArrayList<>();
        }
        List<String> protoFilePaths = new ArrayList<>();
        collectProtoFilesPaths(root, protoFilePaths,project.getBaseDir().getPath());
        return protoFilePaths;
    }

    /**
     * 递归获取
     * @param directory
     * @param protoFilePaths
     * @param basePath
     */
    private void collectProtoFilesPaths(VirtualFile directory, List<String> protoFilePaths,String basePath) {
        for (VirtualFile child : directory.getChildren()) {
            if (!child.isDirectory() && "proto".equals(child.getExtension())) {
                // 从路径中移除 basePath 部分
                String relativePath = child.getPath().substring(basePath.length() + 1 + "proto/".length());
                protoFilePaths.add(relativePath);  // 添加文件路径到结果列表
            } else if (child.isDirectory()) {
                collectProtoFilesPaths(child, protoFilePaths,basePath);  // 递归遍历子目录
            }
        }
    }


}
