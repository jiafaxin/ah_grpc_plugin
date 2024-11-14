package com.autohome.ah_grpc_plugin.services;

import com.autohome.ah_grpc_plugin.Config;
import com.autohome.ah_grpc_plugin.models.ProtoPath;
import com.autohome.ah_grpc_plugin.models.gitlab.ProtoTreeNode;
import com.autohome.ah_grpc_plugin.utils.PathUtils;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.impl.VirtualDirectoryImpl;

import java.io.File;
import java.util.ArrayList;

@Service(Service.Level.PROJECT)
public final class LocalProto {

    Project project;

    public static LocalProto getInstance(Project project) {
        LocalProto result = project.getService(LocalProto.class);
        if (result.project == null) {
            result.project = project;
        }
        return result;
    }

    public ProtoTreeNode get() {
        return get("/autohome/rpc");
    }

    public ProtoTreeNode get(String path) {
        File fp = new File(Config.getInstance(project).protoBasePath() + path);
        VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(fp);
        if (vf == null)
            return null;
        return scan(vf);
    }

    ProtoTreeNode scan(VirtualFile vf) {
        if (vf == null)
            return null;

        String path = PathUtils.getProtoPath(project,vf.getPath());
        if(path == null)
            return null;

        String name = path.substring(path.lastIndexOf("/")+1);

        ProtoTreeNode node = new ProtoTreeNode() {{
            setPath(path);
            setName(name);
        }};

        if (vf.isDirectory()) {
            node.setType("tree");
            node.setChilds(new ArrayList<>());
        } else {
            node.setType("blob");
        }

        if (vf.getChildren().length == 0)
            return node;

        for (VirtualFile child : vf.getChildren()) {
            ProtoTreeNode childNode = scan(child);
            if (childNode == null) continue;
            node.getChilds().add(childNode);
        }
        return node;
    }

}
