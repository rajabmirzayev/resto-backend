package az.codlab.common.util;

public final class PhoneUtils {

    private PhoneUtils() {
    }

    public static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String digits = value.replaceAll("\\D", "");
        return digits.isEmpty() ? null : digits;
    }

}
