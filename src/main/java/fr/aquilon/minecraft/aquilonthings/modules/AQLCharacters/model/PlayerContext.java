package fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters.model;

import fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters.CharacterDatabase;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Billi on 18/02/2019.
 *
 * @author Billi
 */
public class PlayerContext {
    public final Player player;
    public final CharacterPlayer playerData;
    public final AbstractCharacter character;
    public final CharacterSkin charSkin;
    public final CommonSkin commonSkin;
    public final List<CharacterSkill> skills;

    private String name;
    private byte[] nameData;
    private byte[] raceData;
    private byte[] skinData;
    private byte[] skillsData;

    public PlayerContext(Player player, CharacterPlayer playerData, AbstractCharacter character,
                         CharacterSkin charSkin, CommonSkin commonSkin, List<CharacterSkill> skills) {
        if (player==null) throw new IllegalArgumentException("Player cannot be null");
        this.player = player;
        this.playerData = playerData;
        this.character = character;
        this.charSkin = charSkin;
        this.commonSkin = commonSkin;
        this.skills = skills;
    }

    // Helper getters

    public String getUUID() {
        return player.getUniqueId().toString().replaceAll("-","");
    }

    public String getName() {
        if (name==null) {
            name = player.getName();
            if (playerData!=null && playerData.getName()!=null)
                name = playerData.getName();
            else if (character!=null)
                name = character.getName();
        }
        return name;
    }

    public byte[] getNameData() {
        if (nameData==null) {
            nameData = (getUUID()+":"+getName()).getBytes();
        }
        return nameData;
    }

    public String getRace() {
        if (character==null)
            return null;
        return character.getRace();
    }

    public byte[] getRaceData(String defaultRace) {
        if (raceData==null) {
            String race = getRace();
            if (race==null) {
                if (defaultRace==null) return null;
                return (getUUID()+":"+defaultRace).getBytes();
            }
            raceData = (getUUID()+":"+race).getBytes();
        }
        return raceData;
    }

    public AbstractSkin getCurrentSkin() {
        if (character instanceof TempCharacter) {
            TempCharSkin tempSkin = ((TempCharacter) character).getSkinObject();
            if (tempSkin!=null) return tempSkin;
        }
        return charSkin!=null ? charSkin : commonSkin;
    }

    public String getSkinFile() {
        AbstractSkin skin = getCurrentSkin();
        return skin != null ? skin.getFileName() : null;
    }

    public byte[] getSkinData(String defaultSkin) {
        if (skinData==null) {
            String skin = getSkinFile();
            if (skin==null) {
                if (defaultSkin==null) return null;
                return (getUUID()+":"+defaultSkin).getBytes();
            }
            skinData = (getUUID()+":"+skin).getBytes();
        }
        return skinData;
    }

    public String getCustomName() {
        if (playerData == null) return null;
        return playerData.getName();
    }

    public static String getSkillsString(List<CharacterSkill> skills) {
        if (skills==null) return "";
        return skills.stream().map(CharacterSkill::toPacketData).collect(Collectors.joining(";"));
    }

    public byte[] getSkillsData() {
        if (skillsData==null) {
            String data = getUUID()+";"+getSkillsString(skills);
            skillsData = data.getBytes();
        }
        return skillsData;
    }

    // Static factory

    public static PlayerContext getFromPlayer(Player p, CharacterDatabase m) {
        if (p==null) return null;
        String uuid = p.getUniqueId().toString().replaceAll("-","");
        CharacterPlayer playerData = m.findPlayer(uuid);
        if (playerData==null)
            return new PlayerContext(p, null, null, null, null, null);
        AbstractCharacter character;
        CharacterSkin skin = null;
        CommonSkin commonSkin = null;
        List<CharacterSkill> skills = null;
        if (playerData.getCommonSkin() != 0) {
            commonSkin = m.findCommonSkin(playerData.getCommonSkin());
        }
        if (playerData.getSelectedCharacter()==0) {
            character = m.findTempCharacter(playerData.getUUID());
        } else {
            character = m.findCharacterByID(playerData.getSelectedCharacter());
            if (character!=null) {
                int charID = ((Character) character).getID();
                skills = m.findCharacterSkills(charID);
                if (playerData.getSkinName()!=null) {
                    skin = m.findCharacterSkin(charID, playerData.getSkinName());
                }
            }
        }
        return new PlayerContext(p, playerData, character, skin, commonSkin, skills);
    }
}
