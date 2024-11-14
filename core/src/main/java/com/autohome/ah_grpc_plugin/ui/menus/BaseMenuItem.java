package com.autohome.ah_grpc_plugin.ui.menus;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class BaseMenuItem extends JMenuItem {

    public BaseMenuItem(String name,Runnable callback){
        super(name);
        super.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                callback.run();
            }
        });
    }
}
