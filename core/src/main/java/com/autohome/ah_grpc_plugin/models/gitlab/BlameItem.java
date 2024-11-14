package com.autohome.ah_grpc_plugin.models.gitlab;

import java.util.List;

public class BlameItem {
    BlameCommit commit;
    List<String> lines;

    public BlameCommit getCommit() {
        return commit;
    }

    public void setCommit(BlameCommit commit) {
        this.commit = commit;
    }

    public List<String> getLines() {
        return lines;
    }

    public void setLines(List<String> lines) {
        this.lines = lines;
    }
}
