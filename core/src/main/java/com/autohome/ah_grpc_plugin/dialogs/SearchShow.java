package com.autohome.ah_grpc_plugin.dialogs;

import com.autohome.ah_grpc_plugin.Config;
import com.autohome.ah_grpc_plugin.models.gitlab.SearchResult;
import com.autohome.ah_grpc_plugin.pbe.Languages;
import com.autohome.ah_grpc_plugin.services.FileService;
import com.autohome.ah_grpc_plugin.services.GitlabApiService;
import com.autohome.ah_grpc_plugin.services.NotifyService;
import com.autohome.ah_grpc_plugin.utils.BorderColors;
import com.autohome.ah_grpc_plugin.utils.SearchListRenderer;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.editor.highlighter.HighlighterClient;
import com.intellij.openapi.editor.highlighter.HighlighterIterator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.ExpandedItemListCellRendererWrapper;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.LanguageTextField;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.*;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * 搜索弹窗
 */
public class SearchShow {

    Project project;

    JBPopup popup;
    JBList list;
    LanguageTextField editor;
    JBTextField textField;

    JBLabel selectedPath;

    JBScrollPane editorPane;

    JBSplitter splitter;

    JButton pullBtn;

    JButton openBtn;

    public void search(Project project, ActionEvent e, String keyword) {
        this.project = project;

        JComponent body = createBody();
        int width = 1400;
        int height = 900;

        final WindowManager manager = WindowManager.getInstance();
        final JFrame frame = project != null ? manager.getFrame(project) : manager.findVisibleFrame();
        if (width > frame.getWidth() - 100) {
            width = frame.getWidth() - 100;
        }
        if (height > frame.getHeight() - 100) {
            height = frame.getHeight() - 100;
        }

        popup = JBPopupFactory.getInstance().createComponentPopupBuilder(body, textField)
                .setProject(project)
                .setTitle("搜索契约文件")
                .setFocusable(true)
                .setMovable(true)
                .setRequestFocus(true)
                .setMinSize(new Dimension(width, height))
                .createPopup();

        int x = (frame.getWidth() - width) / 2;
        int y = (frame.getHeight() - height) / 2;
        popup.show(new RelativePoint(frame.getRootPane(), new Point(x, y)));


        if (StringUtils.isNotBlank(keyword)) {
            textField.setText(keyword);
            search();
        }
    }

    JComponent createBody(){
        JBPanel panel = new JBPanel(new BorderLayout(0,0));
        panel.add(getNorth(),BorderLayout.NORTH);
        panel.add(getCenter(),BorderLayout.CENTER);
        return panel;
    }

    JComponent getCenter(){
        splitter = new JBSplitter(false,0.5f);
        splitter.setEnabled(false);
        splitter.setResizeEnabled(false);
        splitter.setFirstComponent(getList());
        splitter.setSecondComponent(getEditor());
        splitter.setShowDividerControls(false);
        splitter.setDividerWidth(1);
        splitter.setShowDividerIcon(false);

        JBPanel panel = new JBPanel(new GridLayout(1,1));
        panel.add(splitter);
        return splitter;
    }

    JComponent getList(){
        list = new JBList();
        list.setBorder(null);
        list.setCellRenderer(new SearchListRenderer(project));
        list.addListSelectionListener(e -> {
            SearchResult item =(SearchResult) list.getSelectedValue();
            if(item==null){
                return;
            }
            String content = GitlabApiService.getContent(project, item.getPath());
            selectedPath.setText(item.getPath());
            editor.getDocument().setReadOnly(false);
            editor.setText(content);
            editor.getDocument().setReadOnly(true);
            if(FileService.exists(project, getPath())){
                openBtn.setVisible(true);
                pullBtn.setVisible(false);
            }else{
                openBtn.setVisible(false);
                pullBtn.setVisible(true);
            }
        });
        JBScrollPane listScrollPane = new JBScrollPane(list);
        listScrollPane.setBorder(new MatteBorder(0,0,0,1, BorderColors.base40));
        return listScrollPane;
    }

    JComponent getEditor(){
        pullBtn = new JButton("拉取", AllIcons.Actions.TraceInto);
        pullBtn.setVisible(false);
        pullBtn.addActionListener((e) -> {
            if(StringUtils.isNotBlank(selectedPath.getText())){
                FileService.download(project, selectedPath.getText());
                popup.cancel();
            }else{
                NotifyService.warning(project,"没有选择任何文件");
            }
        });

        openBtn = new JButton("打开",AllIcons.Actions.MenuOpen);
        openBtn.setVisible(false);
        openBtn.addActionListener((e)->{
            FileService.open(project, getPath());
            popup.cancel();
        });

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT,5,5));
        btnPanel.add(pullBtn);
        btnPanel.add(openBtn);

        selectedPath = new JBLabel();
        Font old = selectedPath.getFont();
        selectedPath.setFont(new Font(old.getFontName(),Font.BOLD,old.getSize()));
        selectedPath.setBorder(new EmptyBorder(0,10,0,0));

        JPanel actions = new JPanel(new BorderLayout());
        actions.add(btnPanel,BorderLayout.EAST);
        actions.add(selectedPath,BorderLayout.CENTER);

        createEditor("");
        editorPane = new JBScrollPane(editor);
        editorPane.setBorder(null);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(actions,BorderLayout.NORTH);
        panel.add(editorPane,BorderLayout.CENTER);

        return panel;
    }

    String getPath(){
        return selectedPath.getText();
    }

    void createEditor(String text){
        editor = new LanguageTextField(Languages.PB_LANGUAGE,project,text,false);
        editor.setBorder(new EmptyBorder(0,0,0,0));
        editor.addSettingsProvider(editorBody -> {
            editorBody.setBorder(null);
            editorBody.setBackgroundColor(null);
            editorBody.getSettings().setLineNumbersShown(true);
            //editorBody.createBoundColorSchemeDelegate()

            editorBody.setHighlighter(new EditorHighlighter() {
                @Override
                public @NotNull HighlighterIterator createIterator(int startOffset) {
                    return null;
                }

                @Override
                public void setEditor(@NotNull HighlighterClient editor) {

                }
            });
        });
        editor.getDocument().setReadOnly(true);
        editor.setBackground(null);
    }


    JComponent getNorth(){
        textField = new JBTextField();
        textField.getEmptyText().setText("输入url(比如包含autohome/rpc/)或关键词进行搜索");
        textField.setBorder(new EmptyBorder(5,0,5,5));
        textField.addActionListener((e)->{
            search();
        });

        JLabel icon = new JBLabel(AllIcons.Actions.Search);
        icon.setBorder(new EmptyBorder(5,10,5,0));

        JBPanel panel = new JBPanel(new BorderLayout());
        panel.setBorder(new MatteBorder(0,0,1,0,new Color(100,100,100)));
        panel.setBackground(textField.getBackground());
        panel.add(textField,BorderLayout.CENTER);
        panel.add(icon,BorderLayout.WEST);

        return panel;
    }

    void search(){
        String keyword = textField.getText();
        ((SearchListRenderer) ((ExpandedItemListCellRendererWrapper)list.getCellRenderer()).getWrappee()).setKeyword(keyword);
        java.util.List<SearchResult> fileList = GitlabApiService.search(project, keyword);
        DefaultListModel<SearchResult> model = new DefaultListModel<>();
        for (SearchResult searchResult : fileList) {
            //不处理非proto的结果
            if(!searchResult.getPath().endsWith(".proto")) continue;
            model.addElement(searchResult);
        }
        list.setModel(model);
        list.setSelectedIndex(0);
        //list.updateUI();
    }




}
