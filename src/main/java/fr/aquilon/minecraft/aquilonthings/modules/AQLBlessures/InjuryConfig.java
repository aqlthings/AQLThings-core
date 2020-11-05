package fr.aquilon.minecraft.aquilonthings.modules.AQLBlessures;


import fr.aquilon.minecraft.utils.JSONExportable;
import org.json.JSONObject;

/**
 * Created by Billi on 10/01/2018.
 *
 * @author Billi
 */
public class InjuryConfig implements JSONExportable {
    public static final int DEF_SCORE_MINOR_INJURY = 80;
    public static final int DEF_SCORE_MAJOR_INJURY = 120;
    public static final int DEF_SCORE_DEAD = 200;
    public static final int DEF_SCORE_INCREMENT = 50;
    public static final boolean DEF_FREEZE_ON_DEATH = true;

    private AQLBlessures module;
    private int scoreMinor;
    private int scoreSevere;
    private int scoreDeath;
    private int scoreInc;
    private boolean freezePlayersOnDeath;

    public InjuryConfig(AQLBlessures module, int scoreMinor, int scoreSevere, int scoreDeath, int scoreInc, boolean freezeOnDeath) {
        this.module = module;
        this.scoreMinor = scoreMinor;
        this.scoreSevere = scoreSevere;
        this.scoreDeath = scoreDeath;
        this.scoreInc = scoreInc;
        this.freezePlayersOnDeath = freezeOnDeath;
    }

    public InjuryConfig(InjuryConfig cloneFrom) {
        this(
                cloneFrom.module,
                cloneFrom.scoreMinor,
                cloneFrom.scoreSevere,
                cloneFrom.scoreDeath,
                cloneFrom.scoreInc,
                cloneFrom.freezePlayersOnDeath
        );
    }

    public AQLBlessures getModule() {
        return module;
    }

    public InjuryConfig setScoreMinor(int scoreMinor) {
        this.scoreMinor = scoreMinor;
        return this;
    }

    public int getScoreMinor() {
        return scoreMinor;
    }

    public InjuryConfig setScoreSevere(int scoreSevere) {
        this.scoreSevere = scoreSevere;
        return this;
    }

    public int getScoreSevere() {
        return scoreSevere;
    }

    public InjuryConfig setScoreDeath(int scoreDeath) {
        this.scoreDeath = scoreDeath;
        return this;
    }

    public int getScoreDeath() {
        return scoreDeath;
    }

    public InjuryConfig setScoreIncrementLimit(int scoreInc) {
        this.scoreInc = scoreInc;
        return this;
    }

    public int getScoreIncrementLimit() {
        return scoreInc;
    }

    public int getStateScore(PlayerState.PlayerStateEnum state) {
        switch (state) {
            case NORMAL: return 0;
            case MINOR_INJURY: return getScoreMinor();
            case SEVERE_INJURY: return getScoreSevere();
            case DEAD: return getScoreDeath();
            default:
                throw new UnsupportedOperationException("Unknown player state");
        }
    }

    public InjuryConfig setFreezePlayersOnDeath(boolean freeze) {
        this.freezePlayersOnDeath = freeze;
        return this;
    }

    public boolean freezePlayersOnDeath() {
        return freezePlayersOnDeath;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject res = new JSONObject();
        res.put("limitMinorInjury", scoreMinor);
        res.put("limitSevereInjury", scoreSevere);
        res.put("limitDeath", scoreDeath);
        res.put("randomIncrement", scoreInc);
        res.put("freezePlayersOnDeath", freezePlayersOnDeath);
        return res;
    }
}
