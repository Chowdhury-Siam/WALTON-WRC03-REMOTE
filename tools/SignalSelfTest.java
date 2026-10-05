import dev.siam.quietremote.Wrc03;

/** Run with javac/java, without Android or a test framework. */
public final class SignalSelfTest {
    public static void main(String[] args) {
        if (Wrc03.keys().size() != 25 || Wrc03.FREQUENCY != 38000)
            throw new AssertionError("Wrong profile");
        for (String key : Wrc03.keys()) {
            int[] pattern = Wrc03.pattern(key);
            int total = 0;
            for (int pulse : pattern) {
                if (pulse <= 0) throw new AssertionError(key);
                total += pulse;
            }
            if (total >= 2_000_000 || pattern.length < 68) throw new AssertionError(key);
            int first = pattern[0];
            pattern[0] = 0;
            if (Wrc03.pattern(key)[0] != first) throw new AssertionError("Mutable pattern");
        }
        try {
            Wrc03.pattern("settings");
            throw new AssertionError("Unknown key accepted");
        } catch (IllegalArgumentException expected) {}
        try {
            Wrc03.keys().clear();
            throw new AssertionError("Mutable keys");
        } catch (UnsupportedOperationException expected) {}
        System.out.println("PASS: WRC03 runtime lookup, valid durations and defensive copies");
    }
}
