//package com.autohome.ah_grpc_plugin.actions;
//
//import com.autohome.ah_grpc_plugin.Config;
//import com.autohome.ah_grpc_plugin.enums.NodeType;
//import com.autohome.ah_grpc_plugin.services.GitLocalService;
//import com.autohome.ah_grpc_plugin.services.GrpcConfigService;
//import com.autohome.ah_grpc_plugin.utils.PathUtils;
//import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode;
//import com.intellij.openapi.actionSystem.AnAction;
//import com.intellij.openapi.actionSystem.AnActionEvent;
//import com.intellij.openapi.actionSystem.CommonDataKeys;
//import com.intellij.openapi.vfs.VirtualFile;
//import com.intellij.pom.Navigatable;
//import com.intellij.psi.impl.file.PsiDirectoryImpl;
//import org.apache.commons.lang3.StringUtils;
//import org.jetbrains.annotations.NotNull;
//
//public class GitPushAction extends AnAction {
//
//    public GitPushAction(){
//    }
//
//
////    @Override
////    public void update(@NotNull AnActionEvent e) {
////
////        e.getPresentation().setVisible(false);
////
////        Navigatable[] navigatables = e.getData(CommonDataKeys.NAVIGATABLE_ARRAY);
////        if(navigatables==null)
////            return;
////        if(navigatables.length==0)
////            return;
////
////        Navigatable navigatable = navigatables[0];
////        if(!(navigatable instanceof PsiDirectoryNode)){
////            return;
////        }
////        PsiDirectoryNode directory = (PsiDirectoryNode) navigatable;
////        VirtualFile file = directory.getVirtualFile();
////
////        String path = file.getPath();
////
////        String protoPath = PathUtils.getProtoPath(path);
////        if(StringUtils.isBlank(protoPath))
////            return;
////
////        e.getPresentation().setVisible(true);
////    }
//
//    @Override
//    public void actionPerformed(AnActionEvent e) {
//
//        if(!GrpcConfigService.getInstance(e.getProject()).getSupportGrpc()){
//            return;
//        }
//
//        Navigatable navigatable = e.getData(CommonDataKeys.NAVIGATABLE);
//        if(!(navigatable instanceof PsiDirectoryImpl)){
//            return;
//        }
//        PsiDirectoryImpl directory = (PsiDirectoryImpl) navigatable;
//        VirtualFile file = directory.getVirtualFile();
//
//        String path = file.getPath();
//
//        String protoPath = PathUtils.getProtoPath(e.getProject(),path);
//
//        GitLocalService.getInstance(e.getProject()).push(protoPath);
//    }
//}
