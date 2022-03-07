package fr.aquilon.minecraft.utils;

import org.bukkit.ChatColor;

import java.util.HashMap;
import java.util.Stack;

/**
 * Helper to convert a minecraft formatted string to HTML or unix
 * @see <a href="https://github.com/BilliAlpha/Minecraft_BookTool/blob/master/src/mcbook/Editor.java">
 *          github.com/BilliAlpha/Minecraft_BookTool
 *     </a>
 * @author Billi
 */
public class MinecraftParser {
    public static final HashMap<Character, String> COLOR_NAMES = new HashMap<>();
    public static final HashMap<Character, String> FORMATS_HTML = new HashMap<>();
    public static final HashMap<Character, String> COLORS_HTML = new HashMap<>();
    public static final HashMap<Character, String> COLORS_UNIX = new HashMap<>();

    static {
        COLOR_NAMES.put('0', "black");
        COLOR_NAMES.put('1', "dark_blue");
        COLOR_NAMES.put('2', "dark_green");
        COLOR_NAMES.put('3', "dark_aqua");
        COLOR_NAMES.put('4', "dark_red");
        COLOR_NAMES.put('5', "dark_purple");
        COLOR_NAMES.put('6', "gold");
        COLOR_NAMES.put('7', "gray");
        COLOR_NAMES.put('8', "dark_gray");
        COLOR_NAMES.put('9', "blue");
        COLOR_NAMES.put('a', "green");
        COLOR_NAMES.put('b', "aqua");
        COLOR_NAMES.put('c', "red");
        COLOR_NAMES.put('d', "light_purple");
        COLOR_NAMES.put('e', "yellow");
        COLOR_NAMES.put('f', "white");

        FORMATS_HTML.put('l', "b");
        FORMATS_HTML.put('m', "s");
        FORMATS_HTML.put('n', "em");
        FORMATS_HTML.put('o', "i");
        FORMATS_HTML.put('k', "u");

        COLORS_HTML.put('0', "000000");
        COLORS_HTML.put('1', "0000AA");
        COLORS_HTML.put('2', "00AA00");
        COLORS_HTML.put('3', "00AAAA");
        COLORS_HTML.put('4', "AA0000");
        COLORS_HTML.put('5', "AA00AA");
        COLORS_HTML.put('6', "FFAA00");
        COLORS_HTML.put('7', "AAAAAA");
        COLORS_HTML.put('8', "555555");
        COLORS_HTML.put('9', "5555FF");
        COLORS_HTML.put('a', "55FF55");
        COLORS_HTML.put('b', "55FFFF");
        COLORS_HTML.put('c', "FF5555");
        COLORS_HTML.put('d', "FF55FF");
        COLORS_HTML.put('e', "FFFF55");
        COLORS_HTML.put('f', "FFFFFF");

        // '[0;30;22m', '[0;34;22m', '[0;32;22m', '[0;36;22m', '[0;31;22m', '[0;35;22m',
        // '[0;33;22m', '[0;37;22m', '[0;30;1m', '[0;34;1m', '[0;32;1m', '[0;36;1m',
        // '[0;31;1m', '[0;35;1m', '[0;33;1m', '[0;37;1m', '[m', '[3m
        COLORS_UNIX.put('0', "000000");
        COLORS_UNIX.put('1', "0000AA");
        COLORS_UNIX.put('2', "00AA00");
        COLORS_UNIX.put('3', "00AAAA");
        COLORS_UNIX.put('4', "AA0000");
        COLORS_UNIX.put('5', "AA00AA");
        COLORS_UNIX.put('6', "FFAA00");
        COLORS_UNIX.put('7', "AAAAAA");
        COLORS_UNIX.put('8', "555555");
        COLORS_UNIX.put('9', "5555FF");
        COLORS_UNIX.put('a', "55FF55");
        COLORS_UNIX.put('b', "55FFFF");
        COLORS_UNIX.put('c', "FF5555");
        COLORS_UNIX.put('d', "FF55FF");
        COLORS_UNIX.put('e', "FFFF55");
        COLORS_UNIX.put('f', "FFFFFF");
    }

    // From https://github.com/BilliAlpha/Minecraft_BookTool/blob/master/src/mcbook/Editor.java
    public static String parseHTML(String in) {
        return parseHTML(in, false);
    }
    public static String parseHTML(String in, boolean classes) { // test string : §n§2Alpha§r §9Beta§0 Gamma
        in = in.replaceAll("<", "&#60;");
        in = in.replaceAll(">", "&#62;");
        in = in.replaceAll("\n", "<br/>\n");
        StringBuilder res = new StringBuilder("<p>" + in);
        boolean colorInUse = false;
        int index = res.toString().indexOf('§');
        Stack<Character> symboles = new Stack<>();
        while (index!=-1 && index<res.length()-2) { // Pour eviter le bug du § en dernier caractère
            char key = res.charAt(index+1);
            StringBuilder tags = new StringBuilder();
            if (key == 'r') {
                if (colorInUse) {
                    if (classes) tags.append("</span>");
                    else tags.append("</font>");
                }
                while (!symboles.isEmpty()) {
                    tags.append("</").append(FORMATS_HTML.get(symboles.pop())).append(">");
                }
                if (classes) tags.append("<span class=\"color-").append(COLOR_NAMES.get(key)).append("\">");
                else tags.append("<font color='#").append(COLORS_HTML.get(key)).append("'>");
            } else {
                if (COLORS_HTML.containsKey(key)) {
                    if (colorInUse && key == '0') {
                        if (classes) tags.append("</span>");
                        else tags.append("</font>");
                        colorInUse = false;
                    } else {
                        if (colorInUse) {
                            if (classes) tags.append("</span>");
                            else tags.append("</font>");
                        }
                        if (classes) tags.append("<span class=\"color-").append(COLOR_NAMES.get(key)).append("\">");
                        else tags.append("<font color='#").append(COLORS_HTML.get(key)).append("'>");
                        colorInUse = true;
                    }
                } else {
                    tags.append("<").append(FORMATS_HTML.get(key)).append(">");
                    symboles.add(key);
                }
            }
            res = new StringBuilder(res.substring(0, index).concat(tags.toString()).concat(res.substring(index + 2)));
            index = res.toString().indexOf('§');
        }
        if (colorInUse){
            if (classes) res.append("</span>");
            else res.append("</font>");
        }
        while(!symboles.isEmpty()) {
            res.append("</").append(FORMATS_HTML.get(symboles.pop())).append(">");
        }
        return res+"</p>";
    }

    // TODO: 20/04/2017
    public static String parseUnix(String in) { // test string : §n§2Alpha§r §9Beta§0 Gamma
        return in.replaceAll("§.","");
    }

    public static String htmlColor(char mcColor) {
        return COLORS_HTML.get(mcColor);
    }

    public static String unixColor(char mcColor) {
        return COLORS_UNIX.get(mcColor);
    }

    public static String colorName(char mcColor) {
        return COLOR_NAMES.get(mcColor);
    }

    public static ChatColor colorFromName(String name) {
        for (Character c: COLOR_NAMES.keySet()) {
            if (COLOR_NAMES.get(c).equalsIgnoreCase(name)) return ChatColor.getByChar(c);
        }
        return null;
    }
}
