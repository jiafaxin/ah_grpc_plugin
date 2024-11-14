package com.autohome.ah_grpc_plugin.services;

import com.autohome.ah_grpc_plugin.dialogs.Confim;
import com.autohome.ah_grpc_plugin.models.*;
import com.autohome.ah_grpc_plugin.utils.HttpClientUtil;
import com.autohome.ah_grpc_plugin.utils.JsonUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;

import java.net.http.HttpHeaders;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

@Service(Service.Level.PROJECT)
public final class AuthService {

    Project project;

    public static AuthService getInstance(Project project){
        AuthService result = project.getService(AuthService.class);
        if(result.project == null){
            result.project = project;
        }
        return result;
    }

    public PermissionDto hasPermission(String path,String user) {
        ProtoPath protoPath = ProtoPath.newInstance(path);
        PermissionDto result = new PermissionDto();
        result.setHasPermission(false);

        if(!GitlabApiService.hasLogin()){
            return result;
        }
        if (protoPath == null) {
            return result;
        }
        String projectPath = protoPath.getProjectPath();
        ProjectAuthDto auth = getAuth(projectPath);
        if (auth == null) {
            return result;
        }
        result.setOwners(auth.getOwners());
        if( auth.getOwners().contains(user) || auth.getUsers().contains(user)){
            result.setHasPermission(true);
            return result;
        }
        String dept = getDeptfullname(user).join();
        if(auth.getTeams().stream().anyMatch(x-> StringUtils.isNotBlank(x) && dept.startsWith(x))){
            result.setHasPermission(true);
            return result;
        }
        return result;
    }

    public ProjectAuthDto getAuth(String path){
        try {
            String content = GitlabApiService.getContent(project, getAuthPath(path));
            if(StringUtils.isBlank(content)) {
                return null;
            }
            return JsonUtils.toObject(content,ProjectAuthDto.class);
        }catch (Exception e){
            System.out.println("报错了");
        }
        return null;
    }

    public ProjectAuthDto initOwner(String path){
        if(!path.endsWith(".txt")){
            path = getAuthPath(path);
        }
        ProjectAuthDto result = new ProjectAuthDto();
        result.setOwners(new ArrayList<>());
        result.getOwners().add(GitlabApiService.getCurrentUser());
        GitlabApiService.createFile(project,path,JsonUtils.toString(result),"初始化管理员").join();
        return result;
    }

    public CompletableFuture<String> getDeptfullname(String userName){
        io.netty.handler.codec.http.HttpHeaders httpHeaders  =  new DefaultHttpHeaders();
        httpHeaders.add("appid","grpc");
        httpHeaders.add("secret","5e3fc329-c1f2-4451-a54d-69e6e248d630");
        String public_key = "news-lightapp-api";
        String private_key = "fzUgGF7wuX";
        String authorization = new String(Base64.encodeBase64((public_key + ":" + private_key).getBytes(StandardCharsets.UTF_8)));
        httpHeaders.add("Authorization","Basic "+authorization);
        CompletableFuture<ApiResult<UserInfoResult>> result = HttpClientUtil.postJson("http://autohome-hrcenter-outapiim.openapi.corpautohome.com/outapiim/out/employeeall", "{\"account\":\"" + userName + "\"}", new TypeReference<UserInfoResult>() {},httpHeaders,3000);

        return result.thenApply(x->{
            if(x.getCode()>=300){
                return "";
            }
            if(x.getResult().getStatus()!=1 || x.getResult().getData()==null || x.getResult().getData().size() == 0)
                return "";
            return x.getResult().getData().get(0).getDeptfullname().replace(".","-");
        });

    }


    public String getAuthPath(String path){
        return path.concat("/auth.txt");
    }

}
