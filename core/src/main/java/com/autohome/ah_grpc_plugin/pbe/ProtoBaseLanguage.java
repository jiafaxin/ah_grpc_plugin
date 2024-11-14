package com.autohome.ah_grpc_plugin.pbe;

import com.intellij.lang.Language;

public class ProtoBaseLanguage extends Language {
    public static final ProtoBaseLanguage INSTANCE = new ProtoBaseLanguage();

    private ProtoBaseLanguage() {
        super("protobase");
    }
}