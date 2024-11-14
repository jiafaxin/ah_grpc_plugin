package com.autohome.ah_grpc_plugin.services;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.CredentialAttributesKt;
import com.intellij.credentialStore.Credentials;
import com.intellij.ide.passwordSafe.PasswordSafe;

public class DbService {

    final static String systemName = "autohome_gRPC_plugin";


    public String get(String key){
        CredentialAttributes credentialAttributes = createCredentialAttributes(key);
        String password = PasswordSafe.getInstance().getPassword(credentialAttributes);
        return password;
    }

    public void set(String key,String value) {
        CredentialAttributes credentialAttributes = createCredentialAttributes(key); // see previous sample
        Credentials credentials = new Credentials(null, value);
        PasswordSafe.getInstance().set(credentialAttributes, credentials);
    }

    private CredentialAttributes createCredentialAttributes(String key) {
        return new CredentialAttributes( CredentialAttributesKt.generateServiceName(systemName, key)
        );
    }


}
