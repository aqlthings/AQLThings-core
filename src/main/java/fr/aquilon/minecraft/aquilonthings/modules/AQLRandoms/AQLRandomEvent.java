package fr.aquilon.minecraft.aquilonthings.modules.AQLRandoms;

import fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters.model.CharacterSkill;
import fr.aquilon.minecraft.aquilonthings.modules.AQLMisc;
import fr.aquilon.minecraft.aquilonthings.utils.AquilonEvent;
import fr.aquilon.minecraft.aquilonthings.utils.Utils;
import fr.aquilon.minecraft.utils.JSONPlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.json.JSONObject;

import java.util.Random;

/**
 * Created by Billi on 01/11/2017.
 */
public class AQLRandomEvent extends Event implements AquilonEvent<AQLRandoms> {
    private static HandlerList handlers = new HandlerList();

    private Player sender;
    private int limit;
    private boolean secret;
    private int customBonus;
    private CharacterSkill skill;
    private int value;

    public AQLRandomEvent(Player sender, int limit, boolean secret, int customBonus, CharacterSkill skill) {
        this.sender = sender;
        this.limit = limit;
        this.secret = secret;
        this.customBonus = customBonus;
        this.skill = skill;
        Random alea = new Random();
        this.value = alea.nextInt(limit)+1;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject res = new JSONObject();
        res.put("secret",isSecret());
        res.put("name",getFinalValue());
        res.put("roll",getValue());
        res.put("limit",getLimit());
        if (getCustomBonus()!=0) res.put("customBonus", getCustomBonus());
        if (skill!=null) {
            res.put("skill",skill.toJSON());
            res.put("skillBonus",getSkillBonus());
        }
        res.put("totalBonus",getTotalBonus());
        res.put("sender", JSONPlayer.toJSON(sender, false));
        return res;
    }

    @Override
    public void call(AQLRandoms m) {
        String rollColor = (value==limit ? ChatColor.GREEN : (value==1 ? ChatColor.RED : "")).toString();
        String text = rollColor + value + " sur " + limit + ChatColor.YELLOW;
        String bonusText = getBonusText();
        if (bonusText!=null) {
            text += " ("+bonusText+"), résultat : "+ChatColor.WHITE+getFinalValue();
        }
        sender.sendMessage((secret?ChatColor.GRAY+"[Secret] ":"") + ChatColor.YELLOW + "Vous tirez " + text);
        for (Entity e : sender.getNearbyEntities(50, 50, 50)) {
            if (!(e instanceof Player)) continue;
            // Si le random est secret et que la cible est pas staff on skip.
            if (secret && !e.hasPermission(AQLRandoms.PERM_RANDOM_SEE_SECRET)) continue;
            e.sendMessage((secret?ChatColor.GRAY+"[Secret] ":"") + Utils.decoratePlayerName(sender) +
                    ChatColor.YELLOW + " a tiré un " + text);
        }
        AQLMisc.LOGGER.mInfo("[Random]"+(secret?"[Secret]":"")+" " +
                sender.getName() + " tire " + ChatColor.stripColor(text));
        Bukkit.getServer().getPluginManager().callEvent(this);
    }

    public int getLimit() {
        return limit;
    }

    public int getCustomBonus() {
        return customBonus;
    }

    public CharacterSkill getSkill() {
        return skill;
    }

    public int getSkillBonus() {
        if (skill == null) return 0;
        return skill.getBonus();
    }

    public String getSkillBonusText() {
        if (skill == null) return "";
        String pre = skill.getCategory()+" - "+skill.getName()+": ";
        int skillBonus = getSkillBonus();
        if (skillBonus>=0) return pre+"+"+skillBonus;
        return pre+skillBonus;
    }

    public int getTotalBonus() {
        return getCustomBonus()+getSkillBonus();
    }

    public String getBonusText() {
        int totalBonus = getTotalBonus();
        if (totalBonus==0 && getSkill()==null) return null;
        String res = getSkillBonusText();
        int customBonus = getCustomBonus();
        if (customBonus!=0) {
            if (res.length()>0) res += ", ";
            if (totalBonus>=0) res += "+"+customBonus;
            else res += customBonus;
        }
        return res;
    }

    public int getValue() {
        return value;
    }

    public int getFinalValue() {
        return value+getTotalBonus();
    }

    public Player getSender() {
        return sender;
    }

    public boolean isSecret() {
        return secret;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
