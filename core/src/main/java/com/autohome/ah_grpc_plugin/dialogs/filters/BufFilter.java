package com.autohome.ah_grpc_plugin.dialogs.filters;

import com.autohome.ah_grpc_plugin.services.CommandService;
import com.intellij.execution.filters.Filter;
import com.intellij.openapi.editor.markup.EffectType;
import com.intellij.openapi.editor.markup.TextAttributes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.Arrays;
import java.util.List;


public class BufFilter  implements Filter {

    @Override
    public @Nullable Result applyFilter(@NotNull String line, int entireLength) {

        List<String> okList = Arrays.asList(CommandService.allPassMsg,CommandService.breakingPassMsg,CommandService.lintPassMsg);
        int highlightStartOffset= entireLength-line.length();
        for (String s : okList) {
            if(line.startsWith(s)){
                TextAttributes color = new TextAttributes(new Color(98,151,85),null,Color.GREEN, EffectType.SEARCH_MATCH,Font.PLAIN);
                return new Result(highlightStartOffset,highlightStartOffset + s.length(),null,color);
            }
        }

        List<String> errorList = Arrays.asList(CommandService.allNotPassMsg,CommandService.breakingNotPassMsg,CommandService.lintNotPassMsg,CommandService.errorStart);
        TextAttributes errorColor = new TextAttributes(new Color(204,102,110),null,Color.GREEN, EffectType.SEARCH_MATCH,Font.PLAIN);
        for (String s : errorList) {
            if(line.startsWith(s)){
                return new Result(highlightStartOffset,highlightStartOffset + s.length(),null,errorColor);
            }
        }


        return null;
    }
}
