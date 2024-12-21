package data;

public class VariableInfo {
    private String name;
    private String type;
    private String source;
    private String project;
    private String id;
    private String val;

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSource() {
        return source;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getProject() {
        return project;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setVal(String val) {
        this.val = val;
    }

    public String getVal() {
        return val;
    }

    public String genMeta() {
        return this.id + "|" + this.project + "|" + this.source + "|" + this.type + "|" + this.name + "|" + this.val;
    }
}
