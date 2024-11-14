package com.autohome.ah_grpc_plugin.ui.menus;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class DeleteMenu extends JMenuItem {

    public DeleteMenu(){
        super("删除");
        super.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

            }
        });
    }
}
