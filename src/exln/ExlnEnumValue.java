package exln;

class ExlnEnumValue {
    final ExlnEnum enumType;
    final String name;
    final int ordinal;

    ExlnEnumValue(ExlnEnum enumType, String name, int ordinal) {
        this.enumType = enumType;
        this.name = name;
        this.ordinal = ordinal;
    }

    @Override
    public String toString() {
        return enumType.name + "." + name;
    }
}