package com.autohome.ah_grpc_plugin.utils;

import com.autohome.ah_grpc_plugin.models.ChangeTreeItem;
import com.autohome.ah_grpc_plugin.models.GitActionTreeItem;
import com.autohome.ah_grpc_plugin.models.TreeItem;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

/**
 * 自定义tree 图标
 */
public class GitTreeCellRenderer extends DefaultTreeCellRenderer {

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                  boolean sel,
                                                  boolean expanded,
                                                  boolean leaf, int row,
                                                  boolean hasFocus){
        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
        setText(value.toString());

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
        Object object = node.getUserObject();
        ChangeTreeItem item = (ChangeTreeItem) object;
        this.setIcon(item.getIcon());
        return this;
    }

}
