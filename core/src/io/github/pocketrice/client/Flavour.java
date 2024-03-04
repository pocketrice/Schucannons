package io.github.pocketrice.client;

import java.util.*;
import java.util.stream.Collectors;

import static io.github.pocketrice.client.BotPlayer.weightedRandom;

public class Flavour {
    private static final String[] warCries = { "Preparing for war", "Drafting war plan", "Packaging rations", "Distributing shovels", "Deploying troops", "Polishing cannons", "Marching onwards", "Translating from British English", "Rallying the troops", "Speedrunning quantum physics", "Polishing trombone", "Stabbing an orc", "Vanquishing Santa in chess", "Scribbling a formula sheet", "Using taxpayer money", "Finishing the adaptive followup", "Finding Waldo", "Doin' the do-si-do", "Accelerating a mass", "Snoozing a quick nap", "Firing a test shot", "Wasting your time :-)", "Writing the IA", "Handshaking with server", "Sampling IP address", "Crushing peppercorns", "Crunching numbers", "Removing Herobrine", "Photosynthesising volatile memory", "Transmuting minicio", "Makin' ladders", "Teleporting bread", "Moving pawn to e5", "Making a sandwich", "Finishing Coltrane tetrachord", "Hiding secrets", "Freeform jazz", "Finding theta", "Randomising dice", "Punching a tree", "Discovering impulse", "Opening jar", "Spychecking", "Boarding flight", "Watching sunsets", "Fixing frontend", "Checking if it's Wednesday", "git pulling world", "Munching corn cube", "Bestowing armada", "Catching shiny", "Manning the fort", "Grabbing formulas", "Rushing B", "Hatching an egg", "Gambling responsibly", "java fast-facts.jar", "Importing <hw> tag", "Breaking the flimsy boat", "Tasting flavours", "Updating time-space", "Conquering barbarians", "Minting SPAM cans", "Programming the game", "Engineering game", "Porting to N64", "Revving a T-spin", "data:image/png;base64", "Squashing bugs", "Making bacon", "Watering bonsai", "Riding the Orion", "Preparing storytime", "Throwing exception", "Getting achievements", "Crafting a cannon", "Feelin' happy", "Linking new chip", "0xDEADBEEF", "Waking anti-cheat", "Painting the sky", "Applying finishing touches", "Painting textures", "Fluffing coats", "Digging trenches", "Lobbing explosive coconuts", "Improvising killer sax solo", "Chaining interlerps", "Executing /gamemode 3", "Tightening screws", "Stockpiling cannonballs", "Taking the next right", "Clearing for liftoff", "Printing fonts", "Readying deadly laser", "Flooding sunshine", "Dusting corners", "Loading chunks", "Solving today's Wordle", "Browsing Wikipedia", "Running DOOM", "Throwing boulder", "Throwing blanket", "Throwing bird", "Tuning digitridoo", "Hogging money", "Exploring Fivepolis", "Collecting trinkets", "Booting Navidex", "Learning CSS positioning", "Signing with oversized fountain pen", "Revising timeline", "Taking the nuclear option", "Making trade offer", "Converting torque", "Finishing question 12", "Hacking Super Mario World", "Collecting sun", "Setting clocks", "Going eastward", "Having a party" };
    private static final String[] hobbitNames = { "Baggins", "Bungo", "Bullroarer", "Gandalf", "Radagast", "Dain", "Thorin", "Fili", "Kili", "Balin", "Dwalin", "Oin", "Gloin", "Dori", "Nori", "Ori", "Bifur", "Bofur", "Bombur", "Elrond", "Galion", "Bard", "Beorn", "Tom", "Bert", "Huggins", "Gollum", "Smaug", "Carc", "Roac", "Bolg", "Golfimbul" };
    private static final String[] spiceNames = { "Achiote", "Ajwan", "Amchoor", "Anise", "Aniseed", "Annatto", "Arrowroot", "Asafoetida", "Baharat", "Fragrant", "Balti", "Basil", "Roasted", "Bay", "BBQ", "Biryani", "Cajun", "Caraway", "Cassia", "Cayenne", "Celery", "Chervil", "Chicken", "Chilli", "Chives", "Cloves", "Colombo", "Royal", "Curly", "Madras", "Dhansak", "Dill", "Fajita", "Fennel", "Fenugreek", "Fines", "Tilapia", "Five", "Galangal", "Garam", "Chamomile", "Ginger", "Cardamom", "Herbes", "Jalfrezi", "Jerk", "Juniper", "Kaffir", "Korma", "Lamb", "Lavender", "Lemon", "Lime", "Liquorice", "Mace", "Mango", "Salsa", "Mint", "Mixed", "Mulled", "Mustard", "Nigella", "Nutmeg", "Onion", "Orange", "Oregano", "Paella", "Paprika", "Parsley", "Pepper", "Peppercorns", "Pickling", "Pimento", "Piri", "Pizza", "Poppy", "Marjoram", "Poudre", "Ras-el-Hanout", "Rice", "Rose", "Rosemary", "Saffron", "Sage", "Salt", "Adhesive", "Sesame", "Spearmint", "Steak", "Sumac", "Basil", "Sweet", "Tagine", "Tandoori", "Tarragon", "Curry", "Thyme", "Masala", "Turmeric", "Bean", "Vanilla", "Vegetable", "Zahtar" };
    private static final String[] botNames = { "Notbot", "Anna", "Heavy Weapons Guy", "Gordon", "Woz", "Jobim", "Jianyao", "Wil", "Mundy", "Lando", "Vinny", "Shogo", "Jar", "Isa", "Jeroo", "Ado", "Hal", "Mark", "Bird", "Onuki", "Minton", "Lorry", "Carton", "Gilbert", "The Legend", "Luya", "Hubert", "Schudawg" };
    private static final List<String> usedFlavours = new ArrayList<>();
    public static String random(FlavourType ft) {
        String[] flavourSet = null;

        switch (ft) {
            case WAR_CRIES -> flavourSet = warCries;

            case HOBBIT -> flavourSet = hobbitNames;

            case SPICE -> flavourSet = spiceNames;

            case BOT -> flavourSet = botNames;
        }

        if (new HashSet<>(usedFlavours).containsAll(List.of(flavourSet))) { // Just using list + containsAll is not performant
            usedFlavours.clear();
        }

        String res = weightedRandom(Arrays.stream(flavourSet).filter(s -> !usedFlavours.contains(s)).collect(Collectors.toSet()));
        usedFlavours.add(res);

        return res;
    }

    public static String random(FlavourType... fts) {
        StringBuilder resBuilder = new StringBuilder();

        do {
            resBuilder.delete(0, resBuilder.length());

            for (FlavourType ft : fts) {
                resBuilder.append(random(ft)).append(" ");
            }
        } while (usedFlavours.contains(resBuilder.toString().trim()));

        String res = resBuilder.toString().trim();
        usedFlavours.add(res);
        return res;
    }

    public enum FlavourType {
        WAR_CRIES,
        HOBBIT,
        SPICE,
        BOT
    }
}
