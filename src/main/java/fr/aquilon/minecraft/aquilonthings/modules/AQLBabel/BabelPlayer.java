package fr.aquilon.minecraft.aquilonthings.modules.AQLBabel;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Holds AQLBabel player info
 * @author BilliAlpha <billi.pamege.300@gmail.com>
 */
public class BabelPlayer {
    private final UUID player;
    private String playerName;
    private Language selected;
    private final Map<String, PlayerLanguage> languages;

    public BabelPlayer(UUID player, String playerName) {
        this.player = Objects.requireNonNull(player);
        this.playerName = playerName;
        this.languages = new HashMap<>();
    }

    public BabelPlayer(Player player) {
        this(player.getUniqueId(), player.getName());
    }

    public UUID getPlayerId() {
        return player;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    /**
     * @return The selected language or <code>null</code> for common tongue
     */
    public Language getSelectedLanguage() {
        return selected;
    }

    public boolean reads(Language lang) {
        return reads(lang.getKey());
    }

    public boolean reads(String lang) {
        PlayerLanguage pLang = languages.get(lang);
        if (pLang == null) return false;
        return pLang.level >= 1;
    }

    public boolean speaks(Language lang) {
        return speaks(lang.getKey());
    }

    public boolean speaks(String lang) {
        PlayerLanguage pLang = languages.get(lang);
        if (pLang == null) return false;
        return pLang.level >= 2;
    }

    public void setLanguage(Language lang, int level, String comment) {
        Objects.requireNonNull(lang);
        if (level < 0) throw new IllegalArgumentException("Invalid language understanding level");
        languages.put(lang.getKey(), new PlayerLanguage(lang.getKey(), level, comment));
    }

    public void removeLanguage(Language lang) {
        setLanguage(lang, 0, null);
    }

    public void selectLanguage(Language lang) {
        Objects.requireNonNull(lang);
        if (!speaks(lang)) throw new IllegalArgumentException("Cannot select unknown language: "+lang.getName());
        selected = lang;
    }

    public Set<PlayerLanguage> getLanguages() {
        return new HashSet<>(languages.values());
    }

    public static class PlayerLanguage {
        private final String language;
        private final int level;
        private final String comment;

        public PlayerLanguage(String language, int level, String comment) {
            this.language = Objects.requireNonNull(language);
            this.level = level;
            this.comment = comment;
        }

        public PlayerLanguage(String language, int level) {
            this(language, level, null);
        }

        public String getLanguage() {
            return language;
        }

        public int getLevel() {
            return level;
        }

        public String getComment() {
            return comment;
        }
    }
}
