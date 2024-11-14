package com.autohome.ah_grpc_plugin.models;

public class ProtoGoPackageInfo {

    public ProtoGoPackageInfo(String goPackage, String name, String publicPackage){
        setGoPackage(goPackage);
        setPublicPackage(publicPackage);
        setName(name);
    }

    String publicPackage;
    String goPackage;
    String name;

    public String getPublicPackage() {
        return publicPackage;
    }

    public void setPublicPackage(String publicPackage) {
        this.publicPackage = publicPackage;
    }

    public String getGoPackage() {
        return goPackage;
    }

    public void setGoPackage(String goPackage) {
        this.goPackage = goPackage;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
