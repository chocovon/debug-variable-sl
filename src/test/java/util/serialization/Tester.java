package util.serialization;

public class Tester {
    String name;
    String type;
    Tester obj;

    public Tester(String name, String type) {
        this.name = name;
        this.type = type;
    }

    public Tester(String name, String type, Tester obj) {
        this.name = name;
        this.type = type;
        this.obj = obj;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
