package io.github.pocketrice.client;

import com.badlogic.gdx.math.Vector3;
import io.github.pocketrice.shared.FuzzySearch;
import lombok.Getter;

import java.util.List;
import java.util.Scanner;
import java.util.UUID;

import static io.github.pocketrice.client.AnsiCode.*;
import static io.github.pocketrice.client.BotPlayer.weightedRandom;

@Getter
public class HumanPlayer extends Player {
    Boolean isVectorMode; // boxed = bad, valve pls fix

    public HumanPlayer() {
        this(UUID.randomUUID(), weightedRandom(new String[]{"Notbot", "Anna", "Heavy Weapons Guy", "Scott", "Jianyao", "Wil", "Mundy", "Lando", "Vinny", "Shogo", "Wario", "Lyra", "Ado", "Hal", "Mark", "Bird", "Korb", "Minton", "Lorry", "Heathcliff", "Gilbert", "The Legend", "Plonko", "Plinko", "Hubert", "Pauling"}, new double[0], true));
    }

    public HumanPlayer(UUID uuid, String name) {
        health = 3;
        playerId = uuid;
        playerName = name;
        pos = new Vector3(0,0,0);
    }

    public SpectatorPlayer convertSpec() {
        return new SpectatorPlayer(this);
    }

    @Override
    public void deductHealth() {
        health--;
    }

    @Override
    public void requestProjVector() {
        if (isVectorMode == null)
            isVectorMode = prompt("Would you like to use vector-based or angle-based mode?", "", new String[]{"vector", "angle"}, true, true, true).equals("vector");

        Scanner input = new Scanner(System.in);

        if (isVectorMode) {
            int x = (int) prompt("Input projectile velocity vector X.", "NaN or out of range!", 0, 100, true); // <-- velocity-based vector inputs
            int y = (int) prompt("Input projectile velocity vector Y.", "NaN or out of range!", 0, 100, true);
            int z = (int) prompt("Input projectile velocity vector Z.", "NaN or out of range!", 0, 100, true);
            projVector = new Vector3(x, y, z);
        }
        else { // NOTE: angle-mode will not account for Z (to simulate being pre-turned to conform to 2D)
            float angle = (float) ((float) prompt("Input cannon angle (deg).", "NaN or out of range!", 0, 180, false) * (Math.PI / 180));
            int cannonLevel = (int) prompt("Set cannon level.", "NaN or out of range!", 1, 3, true);
            // a = (vf^2 - vi^2)/2∆x
            // a = (5.827^2 m/s - 0)/2*0.06 m <-- estimated; TODO measure this
            // a = 282.95 m/s^2

            // assume that top -> L1 = 0.06m, then 0.04m for each level.
            // so, L2 = 0.1m, L3 = 0.14m
            // assuming vi = 0; vf^2 = 2a∆x
            // vf = sqrt(2(282.95 m/s^2)*∆x)
            // vf (L2) = 7.523 m/s
            // vf (L3) = 8.901 m/s
            double veloHypo = Math.sqrt(2 * 282.95 * (0.06 + 0.04 * (cannonLevel-1)));
            projVector = new Vector3((float) (Math.cos(angle) * veloHypo), (float) (Math.sin(angle) * veloHypo), 0);
        }
    }

    @Override
    public String toString() {
        return "HUMAN " + getIdentifier();
    }

    @Override
    public String getIdentifier() {
       return playerName.isEmpty() ? playerId.toString() : playerName;
    }
    public static String prompt(String message, String errorMessage, String[] strs, boolean isFuzzy, boolean ignoreCase, boolean lineMode) // <+> APM 1.1; includes fuzzy search!!
    {
        Scanner input = new Scanner(System.in);
        String nextInput;

        while (true)
        {
            System.out.print(message);
            if (!message.isEmpty())
                System.out.println();

            nextInput = (lineMode) ? input.nextLine() : input.next();

            FuzzySearch fs = new FuzzySearch(strs);
            String fuzzyResult = fs.getFuzzy(nextInput)[0];

            if (List.of(strs).contains(nextInput))
                return fuzzyResult;
            if (isFuzzy && fuzzyResult != null) {
                    System.out.println(ANSI_PURPLE + "Could not find " + nextInput + ", preferring " + fuzzyResult + " instead.\n");
                    return fuzzyResult;
            }
            else {
                System.out.println(ANSI_RED + (errorMessage.isEmpty() ? "Error." : "Error: " + errorMessage) + " " + fs.getHumanFuzzy((ignoreCase) ? nextInput.toLowerCase() : nextInput, false) + ANSI_RESET + "\n"); // TODO: fix lowercase inputs
            }

        }
    }
    public static String prompt(String message, String errorMessage, String rgx, boolean lineMode) // <+> APM
    {
        Scanner input = new Scanner(System.in);
        String nextInput;

        while (true)
        {
            System.out.print(message);
            if (!message.isEmpty())
                System.out.println();

            if (lineMode) {
                nextInput = input.nextLine();
            }
            else {
                nextInput = input.next();
            }

            if (nextInput.matches(rgx)) {
                return nextInput;
            } else {
                System.out.println(ANSI_RED + (errorMessage.isEmpty() ? "Error." : "Error: " + errorMessage) + ANSI_RESET);
            }

        }
    }

    public static double prompt(String message, String errorMessage, double min, double max, boolean isIntegerMode)
    {
        Scanner input = new Scanner(System.in);
        String nextInput;
        double parsedInput = 0;
        boolean isValid;

        while (true) {
            System.out.print(message);
            if (!message.isEmpty())
                System.out.println();

            nextInput = input.next();
            try {

                if (!isIntegerMode) {
                    parsedInput = Double.parseDouble(nextInput);
                } else {
                    parsedInput = Integer.parseInt(nextInput);
                }

                input.nextLine();
                isValid = true;
            } catch (Exception e) {
                isValid = false;
            }

            if (parsedInput >= min && parsedInput <= max && isValid) {
                return parsedInput;
            } else {
                System.out.println(ANSI_RED + (errorMessage.isEmpty() ? "Error." : "Error: " + errorMessage) + ANSI_RESET);
            }
        }
    }
}
