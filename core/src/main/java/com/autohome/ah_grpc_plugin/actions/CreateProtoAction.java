package com.autohome.ah_grpc_plugin.actions;

import com.autohome.ah_grpc_plugin.Config;
import com.autohome.ah_grpc_plugin.enums.NodeType;
import com.autohome.ah_grpc_plugin.services.CreateProtoService;
import com.autohome.ah_grpc_plugin.services.FileService;
import com.autohome.ah_grpc_plugin.services.GrpcConfigService;
import com.autohome.ah_grpc_plugin.utils.AutohomeIcons;
import com.autohome.ah_grpc_plugin.utils.PathUtils;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.psi.impl.file.PsiDirectoryImpl;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * 创建一个proto文件
 */
public class CreateProtoAction extends AnAction {

    public CreateProtoAction(){
        super(AutohomeIcons.ProtoFile);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        if(!GrpcConfigService.getInstance(e.getProject()).getSupportGrpc()){
            return;
        }

        e.getPresentation().setVisible(false);

        Navigatable navigatable = e.getData(CommonDataKeys.NAVIGATABLE);
        if(!(navigatable instanceof PsiDirectoryImpl)){
            return;
        }
        PsiDirectoryImpl directory = (PsiDirectoryImpl) navigatable;
        VirtualFile file = directory.getVirtualFile();
        if(file==null)
            return;

        String path = file.getPath();
        if(StringUtils.isBlank(path))
            return;

        String protoPath = PathUtils.getProtoPath(e.getProject(), path);
        if(StringUtils.isBlank(protoPath))
            return;

         NodeType nodeType = PathUtils.getNodeType(protoPath);
         if(nodeType.equals(NodeType.version)){
             e.getPresentation().setVisible(true);
         }
    }

    @Override
    public void actionPerformed(AnActionEvent e) {

        Navigatable navigatable = e.getData(CommonDataKeys.NAVIGATABLE);
        if(!(navigatable instanceof PsiDirectoryImpl)){
            return;
        }
        PsiDirectoryImpl directory = (PsiDirectoryImpl) navigatable;
        VirtualFile file = directory.getVirtualFile();

        String path = file.getPath();
        path = PathUtils.getProtoPath(e.getProject(),path);
        if(path == null)
            return;

        String finalPath = path;
        new CreateProtoService().create(e.getProject(),path,(fileName)->{
            FileService.open(e.getProject(),finalPath.concat("/").concat(fileName));
        });
    }
}
