package com.autohome.ah_grpc_plugin.pbe;

import com.intellij.lang.Language;

public class Languages {
    public static final Language PB_LANGUAGE;


    static {
        PB_LANGUAGE = Language.findLanguageByID("protobuf");
    }
}