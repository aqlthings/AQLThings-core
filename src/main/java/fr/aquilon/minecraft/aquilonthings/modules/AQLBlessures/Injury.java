package fr.aquilon.minecraft.aquilonthings.modules.AQLBlessures;

import fr.aquilon.minecraft.utils.JSONExportable;
import org.bukkit.ChatColor;
import org.json.JSONObject;

import java.util.Random;

/**
 * Created by Billi on 03/05/2017.
 *
 * @author Billi
 */
public class Injury implements JSONExportable {
    private static Random alea = new Random();

    private boolean severe;
    private boolean aggravated;
    private BodyPart bodyPart;
    private int level;
    private String message;

    public Injury(boolean severe, boolean aggravated, BodyPart bodyPart, int level, String msg) {
        this.severe = severe;
        this.aggravated = aggravated;
        this.bodyPart = bodyPart;
        this.level = level;
        this.message = msg;
    }

    public boolean isSevere() {
        return severe;
    }

    public boolean isAggravated() {
        return aggravated;
    }

    public BodyPart getBodyPart() {
        return bodyPart;
    }

    public int getLevel() {
        return level;
    }

    public String getMessage() {
        return message;
    }

    public PlayerState.PlayerStateEnum getState() {
        if (severe) return PlayerState.PlayerStateEnum.SEVERE_INJURY;
        else return PlayerState.PlayerStateEnum.MINOR_INJURY;
    }

    @Override
    public String toString() {
        return ChatColor.YELLOW+"Blessure "+(isSevere()?(isAggravated()?"légère aggravée":"grave"):"légère") +
                " ("+getBodyPart().name+", niveau "+ getLevel()+"):\n" +
                "    "+ChatColor.GOLD+ChatColor.ITALIC+getMessage();
    }

    @Override
    public JSONObject toJSON() {
        JSONObject res = new JSONObject();
        res.put("severe", isSevere());
        res.put("aggravated", isAggravated());
        res.put("level", getLevel());
        res.put("bodyPart", getBodyPart().name());
        res.put("message", getMessage());
        return res;
    }

    // Static
    public static Injury rollInjury(InjuryConfig conf, boolean severeBase) {
        boolean severe = severeBase;
        boolean aggravated = false;
        int levelRoll = alea.nextInt(10)+1;
        // Blessure légére + critique (10/10) -> blessure grave
        if (!severe && levelRoll==10) {
            severe = aggravated = true;
            levelRoll = alea.nextInt(10)+1;
        }
        Injury.BodyPart bodyPart = Injury.BodyPart.fromScore(alea.nextInt(10)+1);
        int level = levelRoll==10 ? 0 : (levelRoll+1)/2;
        return new Injury(severe, aggravated, bodyPart, levelRoll, conf.getModule().getInjuryMessage(severe, bodyPart.id, level));
    }

    public enum BodyPart {
        HEAD("tete",1,2,"Tête"),
        TORSO("torse",3,6,"Torse"),
        RIGHT_ARM("bras",7,7,"Bras droit"),
        LEFT_ARM("bras",8,8,"Bras gauche"),
        RIGHT_LEG("jambe",9,9,"Jambe droite"),
        LEFT_LEG("jambe",10,10,"Jambe gauche");

        int minScore, maxScore;
        String id, name;

        BodyPart(String id, int minScore, int maxScore, String name) {
            this.id = id;
            this.minScore=minScore;
            this.maxScore=maxScore;
            this.name = name;
        }

        public static BodyPart fromScore(int score) {
            for (BodyPart p : values()) {
                if (score>=p.minScore && score<=p.maxScore) return p;
            }
            throw new IllegalArgumentException("No body part for this score");
        }
    }
}
