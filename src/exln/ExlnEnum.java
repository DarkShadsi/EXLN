package exln;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

class ExlnEnum {
    final String name;
    final List<ExlnEnumValue> members = new ArrayList<>();
    final Map<String, ExlnEnumValue> memberMap = new HashMap<>();

    ExlnEnum(String name) {
        this.name = name;
    }

    boolean addMember(ExlnEnumValue value) {
        if (memberMap.containsKey(value.name)) {
            return false;
        }
        members.add(value);
        memberMap.put(value.name, value);
        return true;
    }

    @Override
    public String toString() {
        return "<enum " + name + ">";
    }
}