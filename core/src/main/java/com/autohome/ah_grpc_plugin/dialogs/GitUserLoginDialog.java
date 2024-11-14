package com.autohome.ah_grpc_plugin.dialogs;

import com.autohome.ah_grpc_plugin.Config;
import com.autohome.ah_grpc_plugin.enums.ResultCode;
import com.autohome.ah_grpc_plugin.models.MethodResult;
import com.autohome.ah_grpc_plugin.services.GitlabApiService;
import com.autohome.ah_grpc_plugin.services.NotifyService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTextField;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * 用户登录弹窗
 */
public class GitUserLoginDialog extends DialogWrapper {

    JBTextField userName = new JBTextField();
    JBPasswordField password = new JBPasswordField();
    JBLabel msg = new JBLabel();

    Project project;

    public GitUserLoginDialog(Project project ) {
        super( false); // use current window as parent
        this.project = project;
        setTitle("登录到契约大仓");
        init();
        setSize(300, 0);
    }


    @Override
    protected @Nullable JComponent createCenterPanel() {
        JBPanel body = new JBPanel(new VerticalFlowLayout());

        JBSplitter userNameSplitter = new JBSplitter(false, 0.2f);
        userNameSplitter.setResizeEnabled(false);
        userNameSplitter.setFirstComponent(new JBLabel("账号"));
        userNameSplitter.setSecondComponent(userName);


        JBSplitter userNameMsgSplitter = new JBSplitter(false, 0.2f);
        userNameMsgSplitter.setResizeEnabled(false);
        userNameMsgSplitter.setFirstComponent(new JBLabel(" "));
        JBLabel note = new JBLabel("汽车之家邮箱前缀");
        note.setForeground(new Color(128, 128, 128));
        userNameMsgSplitter.setSecondComponent(new JBLabel("汽车之家邮箱前缀"));

        JBSplitter passwordSplitter = new JBSplitter(false, 0.2f);
        passwordSplitter.setResizeEnabled(false);
        passwordSplitter.setFirstComponent(new JBLabel("密码"));
        passwordSplitter.setSecondComponent(password);

        JBPanel panel = new JBPanel(new FlowLayout());
        msg.setForeground(Color.RED);
        panel.add(msg);

        body.add(panel);
        body.add(userNameSplitter);
        body.add(userNameMsgSplitter);
        body.add(passwordSplitter);
        return body;
    }

    @Override
    protected @NotNull Action getOKAction() {
        Action action = new DialogWrapperAction("登录") {
            @Override
            protected void doAction(ActionEvent e) {
                doOKAction();
            }
        };
        action.putValue(DEFAULT_ACTION, Boolean.TRUE);
        return action;
    }


    @Override
    protected void doOKAction() {
        String name = userName.getText();
        String pwd = password.getText();
        if (StringUtils.isBlank(name)) {
            msg.setText("用户名必填");
            return;
        }
        if (StringUtils.isBlank(pwd)) {
            msg.setText("密码必填");
            return;
        }

        try {
            MethodResult methodResult = GitlabApiService.login(project, name, pwd);
            if (!methodResult.isSuccess()) {
                if (methodResult.getCode().equals(ResultCode.USERNAME_OR_PASSWORD_ERROR)) {
                    msg.setText("账号或密码错误");
                    return;
                }
                msg.setText("登录失败，请把以下代码提供给开发人员："+methodResult.getMessage());
                return;
            }
        } catch (Exception e) {
            NotifyService.error(project,"登录失败：" + e.getMessage());
        }

        super.doOKAction();
    }
}
