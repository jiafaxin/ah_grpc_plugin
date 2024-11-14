package com.autohome.ah_grpc_plugin.utils;

import com.autohome.ah_grpc_plugin.models.gitlab.SearchResult;
import com.autohome.ah_grpc_plugin.pbe.Languages;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.EffectType;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.LanguageTextField;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBDimension;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.util.ArrayList;

public class SearchListRenderer extends JBPanel implements ListCellRenderer<SearchResult> {

    JBLabel fileName;
    EditorTextField content;

    JBPanel fileNamePanel;
    JBPanel contentPanel;

    JBPanel box;

    String keyword;

    TextAttributes color = new TextAttributes(Color.BLACK,Color.ORANGE,null, EffectType.ROUNDED_BOX,Font.PLAIN);

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public SearchListRenderer(Project project) {
        fileName = new JBLabel();
        Font oldFont = fileName.getFont();
        Font newFont = new Font(oldFont.getFontName(), Font.BOLD, oldFont.getSize());
        fileName.setFont(newFont);

        fileNamePanel = new JBPanel(new VerticalFlowLayout(0,0));
        fileNamePanel.setBorder(null);
        fileNamePanel.add(fileName);

        content = new LanguageTextField(Languages.PB_LANGUAGE, project, "", false);
        content.setLayout(new VerticalFlowLayout(0,0));
        content.addSettingsProvider(editor -> {
            editor.setBorder(new EmptyBorder(0,0,5,0));
        });
        content.setBackground(null);
        content.setOpaque(false);
        content.setBorder(null);

        contentPanel = new JBPanel(new VerticalFlowLayout(0,0));
        contentPanel.setBorder(new MatteBorder(1,0,0,0,BorderColors.base));
        contentPanel.add(content);

        box = new JBPanel(new VerticalFlowLayout());
        box.add(fileNamePanel);
        box.add(contentPanel);
        box.setOpaque(false);

        setLayout(new VerticalFlowLayout(0, 0));
        add(box);
        add(getSplit(false,5,BorderColors.base40));
        setBorder(null);
    }

    java.util.List<Integer> getKeywordPosition(String text,String keyword){
        int j = 0;
        int k = keyword.length()-1;
        java.util.List<Integer> result = new ArrayList<>();
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) != keyword.charAt(j)) {
                j = 0;
                continue;
            }
            if (j < k) {
                j++;
                continue;
            }
            result.add(i - k);
            j = 0;
        }
        return result;
    }

    JComponent getSplit(boolean vertical,int width,Color color) {
        JBPanel split = new JBPanel();
        split.setBorder(null);
        split.setBackground(color);
        split.setSize(vertical ? width : 0, vertical ? 0 : width);
        split.setPreferredSize(new JBDimension(vertical ? width : 0, vertical ? 0 : width));
        return split;
    }


    @Override
    public Component getListCellRendererComponent(JList<? extends SearchResult> list, SearchResult value, int index, boolean isSelected, boolean cellHasFocus) {
        fileName.setText(value.getPath());

        //需要去掉最后的换行
        content.setText(StringUtils.stripEnd(value.getData(), "\n"));
        Editor editor = content.getEditor();
        if(editor!=null){
            String text = content.getText();
            java.util.List<Integer> ps = getKeywordPosition(text, keyword);
            for (Integer i : ps) {
                editor.getMarkupModel().addRangeHighlighter(i, i + keyword.length(), HighlighterLayer.LAST, color, HighlighterTargetArea.EXACT_RANGE);
            }
        }

        if (isSelected) {
            box.setBorder(new MatteBorder(0, 5, 0, 0, list.getSelectionBackground()));
        } else {
            box.setBorder(new MatteBorder(0, 5, 0, 0, list.getBackground()));
        }
        return this;
    }
}
