package com.autohome.ah_grpc_plugin.editors;

import com.autohome.ah_grpc_plugin.dialogs.Confim;
import com.autohome.ah_grpc_plugin.models.ProjectAuthDto;
import com.autohome.ah_grpc_plugin.services.AuthService;
import com.autohome.ah_grpc_plugin.services.GitlabApiService;
import com.autohome.ah_grpc_plugin.services.NotifyService;
import com.autohome.ah_grpc_plugin.utils.JsonUtils;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorLocation;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.components.*;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.management.Notification;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 权限编辑
 */
public class AuthEditor implements FileEditor {

    VirtualFile file;
    Project project;

    ProjectAuthDto auth;

    String path;


    public AuthEditor(Project project, VirtualFile file) {
        this.file = file;
        this.project = project;
        path = AuthService.getInstance(project).getAuthPath(file.getName().replace(".config",""));
        auth = getAuth();
    }

    @Override
    public @NotNull JComponent getComponent() {
        JBLabel fTitle = new JBLabel("个人");
        Font font = fTitle.getFont();
        Font newFont = new Font(font.getFontName(),Font.BOLD,font.getSize());
        fTitle.setFont(newFont);
        JBLabel fTitle2 = new JBLabel("填写oa账号，每行一个");

        JBTextArea fList = new JBTextArea();
        fList.setFont(font);
        fList.setBorder(new EmptyBorder(10,10,10,10));
        fList.setEnabled(false);
        if(auth!=null && auth.getUsers()!=null){
            fList.setText(String.join("\n",auth.getUsers()));
        }

        JPanel fp = new JPanel(new VerticalFlowLayout(true,true));
        fp.add(fTitle);
        fp.add(fTitle2);
        fp.add(fList);

        JBLabel sTitle = new JBLabel("团队");
        JBLabel sTitle2 = new JBLabel("填写团队的详细信息（例：汽车之家-研发与数据中心-技术平台部-后端架构团队）");
        sTitle.setFont(newFont);
        JBTextArea sList = new JBTextArea();
        sList.setFont(font);
        sList.setBorder(new EmptyBorder(10,10,10,10));
        sList.setEnabled(false);
        if(auth!=null && auth.getTeams()!=null){
            sList.setText(String.join("\n",auth.getTeams()));
        }

        JPanel sp = new JPanel(new VerticalFlowLayout(true,true));
        sp.add(sTitle);
        sp.add(sTitle2);
        sp.add(sList);

        JBSplitter splitter = new JBSplitter(false,0.3f);
        splitter.setFirstComponent(fp);
        splitter.setSecondComponent(sp);

        JBPanel ownerPanel = new JBPanel(new FlowLayout(FlowLayout.LEFT));
        JBTextField owner = new JBTextField("",20);
        owner.setEnabled(false);
         if(auth!=null && auth.getOwners()!=null && auth.getOwners().size()>0) {
            owner.setText(auth.getOwners().get(0));
            ownerPanel.add(new JBLabel("应用拥有者："));
            ownerPanel.add(owner);
        }

        JButton saveBtn = new JButton("保存");
        saveBtn.setDefaultCapable(true);
        saveBtn.setEnabled(false);

        JBPanel btns = new JBPanel(new FlowLayout(FlowLayout.RIGHT));
        btns.add(saveBtn);
        saveBtn.addActionListener(e->{
            List<String> users = Arrays.stream(fList.getText().split("\n")).collect(Collectors.toList());
            List<String> teams = Arrays.stream(sList.getText().split("\n")).collect(Collectors.toList());
            ProjectAuthDto dto = new ProjectAuthDto();
            dto.setTeams(teams);
            dto.setUsers(users);
            dto.setOwners(Arrays.asList(owner.getText()));
            GitlabApiService.updateFile(project,path,JsonUtils.toString(dto),"修改应用权限");
            NotifyService.info(project,"保存成功");
        });


        JBPanel bottom = new JBPanel(new BorderLayout());
        bottom.add(ownerPanel,BorderLayout.CENTER);
        bottom.add(btns,BorderLayout.EAST);


        JBPanel main = new JBPanel(new BorderLayout());
        main.add(splitter,BorderLayout.CENTER);
        main.add(bottom,BorderLayout.SOUTH);
        main.setBorder(new EmptyBorder(10,10,0,10));
        ApplicationManager.getApplication().runWriteAction(()->{
            if(GitlabApiService.hasLogin()){
                String currentUser = GitlabApiService.getCurrentUser();
                if(auth!=null) {
                    if (auth.getOwners().contains(currentUser)) {
                        fList.setEnabled(true);
                        sList.setEnabled(true);
                        saveBtn.setEnabled(true);
                        owner.setEnabled(true);
                    }
                }
            }
        });
        return main;
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return null;
    }

    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) @NotNull String getName() {
        return "查看与编辑权限";
    }

    @Override
    public void setState(@NotNull FileEditorState state) {

    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void addPropertyChangeListener(@NotNull PropertyChangeListener listener) {

    }

    @Override
    public void removePropertyChangeListener(@NotNull PropertyChangeListener listener) {

    }

    @Override
    public @Nullable FileEditorLocation getCurrentLocation() {
        return null;
    }

    @Override
    public void dispose() {

    }

    @Override
    public <T> @Nullable T getUserData(@NotNull Key<T> key) {
        return null;
    }

    @Override
    public <T> void putUserData(@NotNull Key<T> key, @Nullable T value) {

    }

    @Override
    public VirtualFile getFile() {
        return file;
    }


    public ProjectAuthDto getAuth(){
        try {
            String content = GitlabApiService.getContent(project, path);
            if(StringUtils.isBlank(content)) {
                if(new Confim(project,"设置权限","此应用还没有拥有者，是否将当前用户设为拥有者").showAndGet()){
                    return AuthService.getInstance(project).initOwner(path);
                }
                return null;
            }
            return JsonUtils.toObject(content,ProjectAuthDto.class);
        }catch (Exception e){
            System.out.println("报错了");
        }
        return null;
    }

}
