package com.autohome.ah_grpc_plugin.services;

import com.autohome.ah_grpc_plugin.Config;
import com.autohome.ah_grpc_plugin.models.ProtoPath;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.file.PsiDirectoryFactory;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PsiService {

    public static PsiDirectory createAndGetDirectory(Project project, String path) {
        File fp = new File(project.getBasePath());

        //根目录
        VirtualFile vf = LocalFileSystem.getInstance().findFileByIoFile(fp);

        List<String> ps = Arrays.stream(path.split("/")).collect(Collectors.toList());

        for (String p : ps) {
            if(StringUtils.isBlank(p))
                continue;
            try {
                VirtualFile vf2 = vf.findChild(p);
                if (vf2 == null) {
                    vf = vf.createChildDirectory(null, p);
                } else {
                    vf = vf2;
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        PsiDirectory result = PsiDirectoryFactory.getInstance(project).createDirectory(vf);
        return result;
    }

    public static void deleteFile(Project project, String path,Runnable callback){
        ApplicationManager.getApplication().runWriteAction(()->{
            File file = new File(path);
            if(!file.exists())
                return;
            VirtualFile vf = LocalFileSystem.getInstance().findFileByIoFile(file);
            if(vf == null)
                return;
            if(vf.isDirectory()){
                PsiDirectory directory = PsiDirectoryFactory.getInstance(project).createDirectory(vf);
                directory.delete();
            }else {
                PsiFile psiFile = PsiManager.getInstance(project).findFile(vf);
                psiFile.delete();
            }

            callback.run();
        });

        ProtoPath protoPath = ProtoPath.newInstance(path);

        GitLocalService.getInstance(project).removeItem(protoPath.realPath());
    }

}
