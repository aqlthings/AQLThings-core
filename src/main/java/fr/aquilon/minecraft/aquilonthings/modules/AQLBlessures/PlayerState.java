package fr.aquilon.minecraft.aquilonthings.modules.AQLBlessures;

import fr.aquilon.minecraft.aquilonthings.AquilonThings;
import fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters.AQLCharacters;
import fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters.CharacterDatabase;
import fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters.model.CharacterPlayer;
import fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters.model.CharacterSkill;
import fr.aquilon.minecraft.aquilonthings.utils.Utils;
import fr.aquilon.minecraft.utils.JSONExportable;
import fr.aquilon.minecraft.utils.JSONPlayer;
import fr.aquilon.minecraft.utils.JSONUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Created by Billi on 03/05/2017.
 *
 * @author Billi
 */
public class PlayerState implements JSONExportable {
    public static final String MESSAGE_MORT = ChatColor.DARK_RED+"Votre personnage est décédé.";

    private static Random alea = new Random();

    private UUID playerUUUID;
    private Player player;
    private int deathCount;
    private int score;
    private int increment;
    private ArrayList<Injury> blessures;
    private PlayerStateEnum currentState;
    private Location deathPoint;


    public PlayerState(InjuryCounter ctx, Player p) {
        this.playerUUUID = p.getUniqueId();
        this.deathCount = 0;
        this.score = 0;
        this.increment = 0;
        AQLCharacters aqlchars =  AquilonThings.instance.getModuleData(AQLCharacters.class);
        if (aqlchars != null) {
            CharacterDatabase charDB = aqlchars.getCharacterDB();
            CharacterPlayer charP = charDB.findPlayer(p.getUniqueId().toString().replaceAll("-",""));
            if (charP != null && charP.getSelectedCharacter()!=0) {
                CharacterSkill skill = charDB.findCharacterSkillFromShorthand(charP.getSelectedCharacter(), "PHY");
                if (skill != null) {
                    int max = ctx.getConfig().getScoreIncrementLimit();
                    this.increment = max - skill.getLevel() * (int)(0.1 * max); // -10% par niveau de condition physique
                }
            }
        }
        this.blessures = new ArrayList<>();
        this.currentState = PlayerStateEnum.NORMAL;
        this.deathPoint = null;
        updatePlayer(ctx, p);
    }

    // Getters
    public Player getPlayer() {
        return player;
    }

    public UUID getPlayerUUID() {
        return playerUUUID;
    }

    public int getDeathCount() {
        return deathCount;
    }

    public Location getDeathPoint() {
        return deathPoint;
    }

    public int getScore() {
        return score;
    }

    public PlayerStateEnum getState() {
        return currentState;
    }

    public boolean isDead() {
        return currentState == PlayerStateEnum.DEAD;
    }

    public String getPlayerName() {
        // TODO: Check when offline
        return player.getName();
    }

    public String getColoredPlayerName() {
        // TODO: Check when offline
        return Utils.decoratePlayerName(player) + (!player.isOnline()?ChatColor.RED+"*":"");
    }

    public List<Injury> getInjuries() {
        return blessures;
    }

    public int getIncrement() {
        return increment;
    }

    public void setIncrement(int increment) {
        this.increment = increment;
    }

    // Logic
    public void updatePlayer(InjuryCounter ctx, Player p) {
        this.player = p;
        p.setPlayerListName(getPlayerlistName(ctx));
        ctx.getPlayerStates().put(p.getUniqueId().toString().replaceAll("-",""), this);
    }

    public String getPlayerlistName(InjuryCounter ctx) {
        if (!ctx.isStarted()) return Utils.decoratePlayerName(player);
        return getState().getColor().toString()+
                String.format("%3d", getScore()) + ChatColor.WHITE + " | " +
                Utils.decoratePlayerName(getPlayer(), null, ctx.isActive() ? ChatColor.ITALIC : null, null);
    }

    /**
     * Used to reset a player to the normal state.
     * Call this when you remove the player from the blessure listener.
     * @param ctx InjuryCounter The counter context
     * @return Whether the reset was successful or not (if true the entry should be removed for the player states)
     */
    public boolean reset(InjuryCounter ctx) {
        if (!player.isOnline()) return false;
        // TODO: Joueurs déconnectés
        player.setPlayerListName(getPlayerlistName(ctx));
        if (ctx.getModule().isFrozen(playerUUUID.toString().replaceAll("-",""))) {
            ctx.getModule().unfreezePlayer(player);
        }
        return true;
    }

    public String stateString(boolean longFormat) {
        // TODO: Joueurs déconnectés
        String message = getColoredPlayerName() + ChatColor.YELLOW + " (" + ChatColor.GRAY + player.getDisplayName() + ChatColor.YELLOW + "): ";
        message+=currentState.getColor().toString() + getScore() + " / " + getDeathCount() + " mort" + (getDeathCount() > 1 ? "s" : "");
        message+=", "+currentState.text+".";
        if (longFormat) {
            for (Injury b: blessures) {
                message += "\n    "+ChatColor.GOLD+ChatColor.ITALIC+b.getMessage();
            }
            if (isDead()) message += "\n    "+MESSAGE_MORT;
        }
        return message;
    }

    public void addDeath(InjuryCounter ctx, Location loc) {
        deathPoint = loc;
        deathCount++;
        int inc = getIncrement()>0 ? getIncrement() : ctx.getConfig().getScoreIncrementLimit();
        addScore(ctx, alea.nextInt(inc)+1, true);
    }

    public void addScore(InjuryCounter ctx, int points, boolean waitRespawn) {
        if (points<1) return;
        score+=points;

        if (score>=ctx.getConfig().getScoreDeath() && !currentState.isWorseOrEqualTo(PlayerStateEnum.DEAD)) {
            currentState = PlayerStateEnum.DEAD;
            warnPlayer(ctx.getModule(), currentState, null, waitRespawn);
            if (ctx.getConfig().freezePlayersOnDeath()) ctx.getModule().freezePlayer(player);
        } else if (
                (score>=ctx.getConfig().getScoreMinor() && !currentState.isWorseOrEqualTo(PlayerStateEnum.MINOR_INJURY))
                        || (score>=ctx.getConfig().getScoreSevere() && !currentState.isWorseOrEqualTo(PlayerStateEnum.SEVERE_INJURY))
                ) {
            Injury injury = Injury.rollInjury(ctx.getConfig(), currentState==PlayerStateEnum.MINOR_INJURY);
            blessures.add(injury);
            currentState = injury.getState();
            warnPlayer(ctx.getModule(), currentState, injury, waitRespawn);
        }
        player.setPlayerListName(getPlayerlistName(ctx));
    }

    public void setScore(InjuryCounter ctx, int newScore, boolean waitRespawn) {
        if (newScore < 0) throw new IllegalArgumentException("Negative scores are not allowed");
        if (newScore > score) addScore(ctx, newScore-score, waitRespawn);
        else if (newScore < score) { // Decrement score -> remove injuries
            score = newScore;
            while (blessures.size() > 0 &&
                    score < ctx.getConfig().getStateScore(blessures.get(blessures.size()-1).getState())) {
                blessures.remove(blessures.size()-1);
            }
            if (blessures.size()>0)
                currentState = blessures.get(blessures.size()-1).getState();
            else
                currentState = PlayerStateEnum.NORMAL;
            player.setPlayerListName(getPlayerlistName(ctx));
        }
    }

    /**
     * Envoie un message au joueur et aux staffeux lors de l'atteinte d'un palier
     * @param m
     * @param state
     */
    private void warnPlayer(AQLBlessures m, PlayerStateEnum state, Injury injury, boolean waitRespawn) {
        if (player == null) return;
        InjuryEvent evt = new InjuryEvent(this);
        evt.call(m);
        player.sendMessage(ChatColor.GRAY + "Vous êtes "+state.getNiceText()+ChatColor.GRAY+" !");
        Utils.warnStaff(m.getClass(), getColoredPlayerName()
                + ChatColor.YELLOW + " (" + ChatColor.GRAY + player.getDisplayName() + ChatColor.YELLOW
                + ") est " + state.getNiceText() + ChatColor.YELLOW + ".", new String[]{getPlayerName()});

        // TODO: Add a sound ? (like wither.spawn)
        Runnable playerWarningAction = () ->
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "title "+player.getName()+" actionbar [" +
                        "{\"text\":\"Vous êtes \",\"color\":\"light_gray\"}," +
                        "{\"text\":\""+state.getText(false).toUpperCase()+"\",\"color\":\""+state.getColor().name().toLowerCase()+"\"}," +
                        "{\"text\":\" !\",\"color\":\"light_gray\"}]");

        if (injury!=null) m.showInjuryRoll(player, injury);

        if (waitRespawn)
            m.delayActionAfterEvent(playerUUUID, PlayerRespawnEvent.class, playerWarningAction, 10);
        else
            playerWarningAction.run();
    }

    // Implement interfaces
    @Override
    public JSONObject toJSON() {
        JSONObject res = new JSONObject();
        res.put("player", JSONPlayer.toJSON(getPlayer(), false));
        res.put("score", getScore());
        JSONObject state = new JSONObject();
        state.put("name",getState().name());
        state.put("text",getState().getText(false));
        state.put("color",getState().getColor().name());
        res.put("state", state);
        res.put("deathCount", getDeathCount());
        res.put("increment", getIncrement()>0 ? getIncrement() : JSONObject.NULL);
        res.put("injuries", JSONUtils.jsonArray(blessures));
        return res;
    }

    // Subclasses
    public enum PlayerStateEnum {
        NORMAL(ChatColor.DARK_GREEN, "entier"),
        MINOR_INJURY(ChatColor.GOLD, "légérement blessé"),
        SEVERE_INJURY(ChatColor.DARK_PURPLE, "gravement blessé"),
        DEAD(ChatColor.DARK_RED, "mort");

        private ChatColor color;
        private String text;

        PlayerStateEnum(ChatColor color, String text) {
            this.color = color;
            this.text = text;
        }

        public ChatColor getColor() {
            return color;
        }

        public String getText() {
            return getText(true);
        }

        public String getText(boolean colored) {
            return (colored?color.toString():"")+text;
        }

        public String getNiceText() {
            return color.toString()+text.toUpperCase();
        }

        public boolean isWorseThan(PlayerStateEnum state) {
            return this.ordinal()>state.ordinal();
        }

        public boolean isWorseOrEqualTo(PlayerStateEnum state) {
            return this.ordinal()>=state.ordinal();
        }
    }
}
