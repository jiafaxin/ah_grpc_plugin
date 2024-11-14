package com.autohome.ah_grpc_plugin.services;

import com.autohome.ah_grpc_plugin.models.gitlab.BlameItem;
import com.autohome.ah_grpc_plugin.pbe.Languages;
import com.intellij.mock.MockPsiElement;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.protobuf.lang.psi.PbFile;
import com.intellij.protobuf.lang.psi.PbServiceDefinition;
import com.intellij.protobuf.lang.psi.PbServiceMethod;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.util.PsiTreeUtil;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class FileBlameService {

    public static Map<String, String> getLastModifiedUser(Project project, String path) {
        try {
            List<BlameItem> items = GitlabApiService.getFileBlames(project, path).join();
            String content = GitlabApiService.getContent(project, path);
            return ApplicationManager.getApplication().runReadAction((Computable<Map<String,String>>)()->{
                Map<String, String> result = new HashMap<>();
                if (items == null)
                    return result;
                Map<Integer, String> lineAuthors = new HashMap<>();
                String[] lines = content.split("\n");
                for (int i = 0; i < lines.length; i++) {
                    String lineContent = lines[i];
                    BlameItem item = items.stream().filter(x -> x.getLines().contains(lineContent)).findFirst().orElse(null);
                    if (item == null)
                        continue;
                    lineAuthors.put(i, item.getCommit().getAuthor_name());
                }

                PbFile file = (PbFile) PsiFileFactory.getInstance(project).createFileFromText(Languages.PB_LANGUAGE, content);
                Document document = PsiDocumentManager.getInstance(project).getDocument(file);

                Collection<PbServiceDefinition> services = PsiTreeUtil.findChildrenOfType(file, PbServiceDefinition.class);
                if (services == null || services.size() == 0)
                    return result;
                for (PbServiceDefinition service : services) {
                    int lineNumber = document.getLineNumber(service.getTextOffset());
                    if (!lineAuthors.containsKey(lineNumber)) {
                        continue;
                    }
                    result.put(service.getName(), lineAuthors.get(lineNumber));
                    Collection<PbServiceMethod> methods = PsiTreeUtil.findChildrenOfType(service, PbServiceMethod.class);
                    if (methods == null || methods.size() == 0)
                        continue;

                    for (PbServiceMethod method : methods) {
                        int methodLineNumber = document.getLineNumber(method.getTextOffset());
                        if (!lineAuthors.containsKey(methodLineNumber)) {
                            continue;
                        }
                        result.put(service.getName().concat(".").concat(method.getName()), lineAuthors.get(methodLineNumber));
                    }
                }
                return result;
            });
        }catch (Exception e){
            return new HashMap<>();
        }

    }

}
