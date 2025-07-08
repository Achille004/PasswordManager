package password.manager.lib;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javafx.scene.paint.Color;
import me.gosimple.nbvcxz.Nbvcxz;

public class Utils {
    private static final Nbvcxz NBVCXZ = new Nbvcxz();

    // Ideal gap is from 20 to 50, represented with linear progress bar with gaps of 1
    public static double passwordStrength(@Nullable String password) {
        return password != null ? NBVCXZ.estimate(password).getEntropy() : 0d;
    }

    public static String passwordStrengthGradient(@NotNull Double progress) throws IllegalArgumentException {
        if (progress < 0 || progress > 1) {
            throw new IllegalArgumentException("Progress must be between 0 and 1");
        }
        StringBuilder gradientStr = new StringBuilder("linear-gradient(to right, #f00 0%, ");
        boolean isHalfProgress = progress >= 0.5;

        // .replace("0x", "#") -> change 0x to # for color
        // .replace("ffx", "") -> remove alpha from color, used with x to not remove other ff by accident
        if (isHalfProgress) {
            gradientStr.append("#ff0 50%, ");

            double halfProgress = progress - 0.5;
            gradientStr.append((Color.YELLOW.interpolate(Color.LIME, halfProgress * 2) + "x").replace("0x", "#").replace("ffx", ""));
            gradientStr.append(" 100%");
        } else {
            gradientStr.append((Color.RED.interpolate(Color.YELLOW, progress * 2) + "x").replace("0x", "#").replace("ffx", ""));
            gradientStr.append(" 100%");
        }
        gradientStr.append(")");

        return gradientStr.toString();
    }
}
