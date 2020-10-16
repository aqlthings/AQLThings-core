package fr.aquilon.minecraft.aquilonthings.modules.AQLBabel;

import java.util.Objects;

/**
 * Represents a single language
 * @author BilliAlpha <billi.pamege.300@gmail.com>
 */
public class Language {
    private final String key;
    private String name;
    private String alphabet;
    private String desc;

    public Language(String key, String name, String alphabet, String desc) {
        this.key = Objects.requireNonNull(key);
        if (Objects.requireNonNull(name).isEmpty()) throw new IllegalArgumentException("Empty name");
        if (Objects.requireNonNull(alphabet).isEmpty()) throw new IllegalArgumentException("Empty alphabet");
        this.name = name;
        this.alphabet = alphabet;
        this.desc = desc;
    }

    public Language(String key, String name, String alphabet) {
        this(key, name, alphabet, null);
    }

    public String getKey() {
        return key;
    }

    public String getName() {
        return name;
    }

    public String getAlphabet() {
        return alphabet;
    }

    public String getDescription() {
        return desc;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAlphabet(String alphabet) {
        this.alphabet = alphabet;
    }

    public void setDescription(String desc) {
        this.desc = desc;
    }

    public char[] getCharacters() {
        return alphabet.toCharArray();
    }

    public String translate(String input) {
        int len = input.length();
        StringBuilder output = new StringBuilder();
        char[] chars = getCharacters();
        int word = 0;
        for (int i = 0; i < len; i++) {
            if (word > 0 && Math.random() > 0.9-(word/(float)len)) {
                output.append(" ");
                word = 0;
                continue;
            }
            int n = (int) (Math.random()*chars.length);
            output.append(chars[n]);
            word++;
        }
        return output.toString();
    }
}
