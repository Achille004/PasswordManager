package password.manager.lib;

import org.jetbrains.annotations.NotNull;

import javafx.scene.paint.Color;
import me.gosimple.nbvcxz.Nbvcxz;

public class Utils {
    private static final Nbvcxz NBVCXZ = new Nbvcxz();

    // Ideal gap is from 20 to 50, represented with linear progress bar with gaps of
    // 1
    public static double passwordStrength(@NotNull String password) {
        return NBVCXZ.estimate(password).getEntropy();
    }

    public static String passwordStrengthGradient(@NotNull Double progress) throws IllegalArgumentException {
        if (progress < 0 || progress > 1) {
            throw new IllegalArgumentException("Progress must be between 0 and 1");
        }
        String gradientStr = "linear-gradient(to right, #f00 0%";
        boolean isHalfProgress = progress >= 0.5;

        if (isHalfProgress) {
            double halfProgress = progress - 0.5;

            gradientStr += ", #ff0 " + (1 - halfProgress) * 100 + "%";
            gradientStr += ", " + (Color.YELLOW.interpolate(Color.GREEN, halfProgress * 2) + "x").replace("0x", "#").replace("ffx", "") + " 100%";
        } else {
            gradientStr += ", " + (Color.RED.interpolate(Color.YELLOW, progress * 2) + "x").replace("0x", "#").replace("ffx", "") + " 100%";
        }
        gradientStr += ")";

        return gradientStr;
    }
}
