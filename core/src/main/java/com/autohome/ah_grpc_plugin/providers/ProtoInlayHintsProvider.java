package com.autohome.ah_grpc_plugin.providers;

import com.autohome.ah_grpc_plugin.Config;
import com.autohome.ah_grpc_plugin.models.ProtoPath;
import com.autohome.ah_grpc_plugin.services.FileBlameService;
import com.autohome.ah_grpc_plugin.utils.PathUtils;
import com.intellij.codeInsight.hints.*;
import com.intellij.icons.AllIcons;
import com.intellij.lang.Language;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.protobuf.lang.psi.PbFile;
import com.intellij.protobuf.lang.psi.PbServiceDefinition;
import com.intellij.protobuf.lang.psi.PbServiceMethod;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.ui.layout.LCFlags;
import com.intellij.ui.layout.LayoutKt;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class ProtoInlayHintsProvider implements InlayHintsProvider {

    private SettingsKey<NoSettings> key = new SettingsKey<>("AUTOHOME.hints");

    @Override
    public boolean isVisibleInSettings() {
        return true;
    }

    @NotNull
    @Override
    public SettingsKey getKey() {
        return key;
    }

    @Nls(capitalization = Nls.Capitalization.Sentence)
    @NotNull
    @Override
    public String getName() {
        return "AUTOHOME";
    }

    @Nullable
    @Override
    public String getPreviewText() {
        return "Preview";
    }

    @NotNull
    @Override
    public ImmediateConfigurable createConfigurable(@NotNull Object o) {
        return new ImmediateConfigurable() {
            @NotNull
            @Override
            public JComponent createComponent(@NotNull ChangeListener changeListener) {
                return LayoutKt.panel(new LCFlags[0], "LSP", builder -> {
                    return null;
                });
            }
        };
    }

    @NotNull
    @Override
    public Object createSettings() {
        return new NoSettings();
    }

    @Nullable
    @Override
    public InlayHintsCollector getCollectorFor(@NotNull PsiFile psiFile, @NotNull Editor editor, @NotNull Object o, @NotNull InlayHintsSink inlayHintsSink) {

        return new FactoryInlayHintsCollector(editor) {
            @Override
            public boolean collect(@NotNull PsiElement psiElement, @NotNull Editor editor, @NotNull InlayHintsSink inlayHintsSink) {
                if (!(psiElement instanceof PbFile))
                    return false;

                Project project = psiFile.getProject();
                String path = PathUtils.getProtoPath(project,psiFile.getVirtualFile().getPath());

                Map<String,String> authors = FileBlameService.getLastModifiedUser(project,path);

                Collection<PbServiceDefinition> services = PsiTreeUtil.findChildrenOfType(psiElement, PbServiceDefinition.class);
                services.forEach(service -> {
                    if(authors.containsKey(service.getName())) {
                        String author = authors.get(service.getName());
                        inlayHintsSink.addBlockElement(service.getTextOffset(), true, true, 0, getFactory().text("Last modified by : " + author));
                    }

                    Collection<PbServiceMethod> methods = PsiTreeUtil.findChildrenOfType(service, PbServiceMethod.class);
                    methods.forEach(method -> {
                        if(method.getName()==null){
                            return;
                        }
                        String name = service.getName().concat(".").concat(method.getName());
                        if(authors.containsKey(name)) {
                            String author = authors.get(name);
                            inlayHintsSink.addBlockElement(method.getTextOffset(), true, true, 0, getFactory().text("    Last modified by : " + author));
                            //inlayHintsSink.addInlineElement(method.getTextOffset(), true, getFactory().text("    Last modified by : " + author));
                        }
                    });
                });
                return true;
            }
        };
    }

    @Override
    public boolean isLanguageSupported(@NotNull Language language) {
        return true;
    }
}
