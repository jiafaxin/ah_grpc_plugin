package com.autohome.ah_grpc_plugin.models;

import java.util.ArrayList;
import java.util.List;

public class ProjectAuthDto {
    List<String> owners;
    List<String> users;
    List<String> teams;

    String project;

    public List<String> getOwners() {
        if(owners==null)
            return new ArrayList<>();
        return owners;
    }

    public void setOwners(List<String> owners) {
        this.owners = owners;
    }

    public List<String> getUsers() {
        if(users==null)
            return new ArrayList<>();
        return users;
    }

    public void setUsers(List<String> users) {
        this.users = users;
    }

    public List<String> getTeams() {
        if(teams==null)
            return new ArrayList<>();
        return teams;
    }

    public void setTeams(List<String> teams) {
        this.teams = teams;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getProject() {
        return project;
    }
}
