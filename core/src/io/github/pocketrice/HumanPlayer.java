package io.github.pocketrice;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.math.Vector3;
import io.github.pocketrice.Prysm.Rigidbody;
import lombok.Getter;
import net.mgsx.gltf.loaders.gltf.GLTFLoader;

import java.util.Scanner;
import java.util.UUID;

import static io.github.pocketrice.AnsiCode.ANSI_RED;
import static io.github.pocketrice.AnsiCode.ANSI_RESET;

@Getter
public class HumanPlayer extends Player {
    public HumanPlayer() {
        Model model = new GLTFLoader().load(Gdx.files.internal("models/schucannon.gltf")).scene.model;
        health = 3;
        rb = new Rigidbody(model);
        playerId = UUID.randomUUID();
    }

    public HumanPlayer(float m, Vector3 loc, Vector3 velo, Vector3 acc) {
        Model model = new GLTFLoader().load(Gdx.files.internal("models/schucannon.gltf")).scene.model;
        health = 3;
        rb = new Rigidbody(model, m, loc, velo, acc);
        playerId = UUID.randomUUID();
    }


    public static String prompt(String message, String errorMessage, String[] bounds, boolean lineMode, boolean isCaseSensitive) // <+> APM
    {
        Scanner input = new Scanner(System.in);
        String nextInput;

        while (true)
        {
            System.out.print(message);
            if (!message.isEmpty())
                System.out.println();

            if (lineMode) {
                input.nextLine();
                nextInput = input.nextLine();
            }
            else {
                nextInput = input.next();
            }

            if (!isCaseSensitive)
            {
                nextInput = nextInput.toLowerCase();

                for (int i = 0; i < bounds.length; i++)
                    bounds[i] = bounds[i].toLowerCase();
            }

            if (nextInput.matches(String.join("|", bounds)) || bounds[0].isEmpty()) {
                return nextInput;
            } else {
                System.out.println(ANSI_RED + errorMessage + ANSI_RESET);
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
                System.out.println(ANSI_RED + errorMessage + ANSI_RESET);
            }
        }
    }

    @Override
    public void deductHealth() {
        health--;
    }

    @Override
    public void requestProjVector() {
        // TEMP
        Scanner input = new Scanner(System.in);
        int x = (int) prompt("Input projectile velocity vector X.", "Not a number or too high!", 0, 100, true);
        int y = (int) prompt("Input projectile velocity vector Y.", "Not a number or too high!", 0, 100, true);
        int z = (int) prompt("Input projectile velocity vector Z.", "Not a number or too high!", 0, 100, true);
        projVector = new Vector3(x,y,z);
    }

    @Override
    public String toString() {
        return "Human player " + (playerName.isEmpty() ? playerId : playerName);
    }
}
