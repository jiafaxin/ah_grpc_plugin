package com.autohome.ah_grpc_plugin.icons;

import com.intellij.icons.AllIcons;
import com.intellij.util.ui.EmptyIcon;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class AuthorIcon extends EmptyIcon {
    private String name;
    public AuthorIcon(String name) {
        super(EmptyIcon.create(AllIcons.Vcs.Author));
        this.name = name;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int i, int j) {
        if (StringUtils.isBlank(name)) {
            return;
        }
        g.setColor(Color.cyan);
        g.drawString(name, i,(int)(j + getIconHeight() + 1.5));
    }

    @Override
    public int getIconWidth() {
        return name.length() * 5;
    }

    @Override
    public int getIconHeight() {
        return 8;
    }
}
