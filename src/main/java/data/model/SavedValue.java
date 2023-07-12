
package data.model;

import data.VariableInfo;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class SavedValue {
    static Set<String> PRIMITIVES = new HashSet<>(Arrays.asList(
            "boolean", "byte", "char", "double", "float", "int", "long", "short"));

    String id;
    String project;
    String source;
    String type;
    String name;
    String val;
    String json;

    public SavedValue() {
    }

    public boolean hasSameOrigin(VariableInfo variableInfo) {
        return this.project.equals(variableInfo.getProject())
                && this.source.equals(variableInfo.getSource())
                && this.type.equals(variableInfo.getType())
                && this.name.equals(variableInfo.getName());
    }

    public boolean isPrimitive() {
        return PRIMITIVES.contains(this.type);
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProject() {
        return this.project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getSource() {
        return this.source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getJson() {
        return this.json;
    }

    public void setJson(String json) {
        this.json = json;
    }

    public void setVal(String val) {
        this.val = val;
    }

    public String getVal() {
        return this.val;
    }
}
