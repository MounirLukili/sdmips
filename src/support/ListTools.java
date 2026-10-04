package support;

import java.util.ArrayList;
import java.util.List;

/** Petits utilitaires sur les listes. */
public final class ListTools {

    private ListTools() {}

    public static <T> List<T> mklist(T t) {
        List<T> list = new ArrayList<T>();
        list.add(t);
        return list;
    }
}
