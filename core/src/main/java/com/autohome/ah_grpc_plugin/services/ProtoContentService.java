package com.autohome.ah_grpc_plugin.services;

import com.autohome.ah_grpc_plugin.Config;
import com.autohome.ah_grpc_plugin.models.ProtoPath;
import com.autohome.ah_grpc_plugin.utils.ProtoNameUtils;
import com.autohome.ah_grpc_plugin.utils.ResourceFileUtils;
import com.intellij.openapi.project.Project;

import java.nio.charset.Charset;

public class ProtoContentService {

    /**
     *
     * @param path  形如：autohome/rpc/car/demo/v1
     * @param name  形如：a.proto
     * @return
     */
    public static String getTemplate(Project project, String path, String name, boolean emptyProto) {
        name = ProtoNameUtils.getNameOnly(name);
        String tempPath = emptyProto?"autohome/templates/plugin_create_empty.prototemp":"autohome/templates/plugin_create.prototemp";
        String temp = GitlabApiService.getContent(project, tempPath);

        ProtoPath protoPath = ProtoPath.newInstance(path);
        String author = GitlabApiService.getCurrentUser();

        temp = temp.replace("{path}", path)
                .replace("{package}", basePackage(path))
                .replace("{fileName}", ProtoNameUtils.toPascal(name))
                .replace("{name}", ProtoNameUtils.inPackage(name))
                .replace("{goPackage}", goPackage(path))
                .replace("{baseDomain}", protoPath.getDomian())
                .replace("{author}", author);
        return temp.replace("\r\n", "\n");
    }

    public static String goPackage(String path){
        String goname = "";
        String[] pathSplit = path.split("/");

        if (pathSplit.length == 6) {
            goname = pathSplit[5];
        } else {
            goname = pathSplit[3];
        }
        return path.concat(";").concat(goname);
    }

    public static String basePackage(String path){
        return path.replace("/", ".");
    }

}
