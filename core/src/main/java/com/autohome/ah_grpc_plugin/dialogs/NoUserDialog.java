package com.autohome.ah_grpc_plugin.dialogs;

import com.autohome.ah_grpc_plugin.enums.GitLocalInfoType;
import com.autohome.ah_grpc_plugin.models.FileDetail;
import com.autohome.ah_grpc_plugin.models.GitAction;
import com.autohome.ah_grpc_plugin.services.CommandService;
import com.autohome.ah_grpc_plugin.services.FileService;
import com.autohome.ah_grpc_plugin.services.GitlabApiService;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * 未找到用户弹窗
 */
public class NoUserDialog extends DialogWrapper {

    Project project;

    public NoUserDialog(Project project) {
        super(project); // use current window as parent
        setTitle("登录失败");
        this.project = project;
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JBPanel body = new JBPanel(new VerticalFlowLayout());
        JBLabel label = new JBLabel("<html>您可能输入了错误的账号或密码或者您是第一次登录<br />" +
                "如果您输入了错误的账号或密码，请点击Cancel后，重新登录<br />" +
                "如果您是您是第一次登录，登录前，您需要先通过web登录一下大仓；<br/>请点击下面的【打开web界面，并登录】按钮并使用OA账户登录后，点击【我已完成web登录，继续登录】按钮继续操作</html>");
        body.add(label);
        JButton webLogin = new JButton("打开web界面，并登录");
        webLogin.addActionListener(e -> {
            BrowserUtil.open("https://git-ops.corpautohome.com");
        });
        JBPanel panel = new JBPanel(new FlowLayout(FlowLayout.LEFT));
        panel.add(webLogin);
        body.add(panel);
        return body;
    }

    @Override
    protected Action @NotNull [] createActions() {
        Action ok = new DialogWrapperAction("我已完成web登录，继续登录") {
            @Override
            protected void doAction(ActionEvent e) {
                doOKAction();
            }
        };

        final List<Action> actions = new ArrayList<>();
        ok.putValue(DEFAULT_ACTION, Boolean.TRUE);
        actions.add(ok);
        actions.add(getCancelAction());
        return actions.toArray(new Action[0]);
    }
}
