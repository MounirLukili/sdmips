package support;

import ast.Node;
import java.util.LinkedList;
import java.util.List;

public class Errors {

    private List<String> errors;

    public Errors() {
        errors = new LinkedList<>();
    }

    public List<String> get() {
        return errors;
    }

    public void add(Node n, String e) {
        if (n == null) {
            errors.add(e);
        } else {
            errors.add(n.getPosition() + " : " + e);
        }
    }

    public void add(String e) {
        errors.add(e);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public void merge(Errors e) {
        for (String s : e.get()) {
            errors.add(s);
        }
    }
}
