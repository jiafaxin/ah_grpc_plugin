package com.autohome.ah_grpc_plugin.ui.menus;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.function.Consumer;

public class RefreshMenu extends JMenuItem {

    public RefreshMenu(Runnable callback){
        super("刷新子节点");
        super.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                callback.run();
            }
        });
    }
}
