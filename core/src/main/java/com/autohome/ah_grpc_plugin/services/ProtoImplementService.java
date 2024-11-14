package com.autohome.ah_grpc_plugin.services;

import com.autohome.ah_grpc_plugin.Config;
import com.autohome.ah_grpc_plugin.enums.ResultCode;
import com.autohome.ah_grpc_plugin.models.MethodResult;
import com.autohome.ah_grpc_plugin.utils.ProtoNameUtils;
import com.autohome.ah_grpc_plugin.utils.ProtoPsiUtils;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.protobuf.lang.PbFileType;
import com.intellij.protobuf.lang.psi.PbServiceMethod;
import com.intellij.protobuf.lang.psi.impl.PbServiceDefinitionImpl;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.PsiNavigateUtil;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProtoImplementService {

    static VirtualFile toSelect;


    public static String selectPath(Project project){
        FileChooserDescriptor chooserDescriptor = new FileChooserDescriptor(false, true, false, false, false, false);
        VirtualFile ts = toSelect == null ? LocalFileSystem.getInstance().findFileByPath((project.getBasePath())) : toSelect;
        VirtualFile virtualFile = FileChooser.chooseFile(chooserDescriptor, null, ts);
        if (virtualFile == null) {
            return "";
        }
        toSelect = virtualFile;
        String newPath = virtualFile.getPath();
        return newPath;
    }


}
