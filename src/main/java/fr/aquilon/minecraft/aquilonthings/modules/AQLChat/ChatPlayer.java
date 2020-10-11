package fr.aquilon.minecraft.aquilonthings.modules.AQLChat;

import java.util.ArrayList;
import java.util.List;

public class ChatPlayer {

    private String channel;
    private List<String> hiddenChannels;
    private List<String> channelBans;

    public ChatPlayer(String channel){
        this.channel = channel;
        this.hiddenChannels = new ArrayList<>();
        this.channelBans = new ArrayList<>();
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public List<String> getHiddenChannels() {
        return hiddenChannels;
    }

    public boolean isChannelHidden(String chan) {
        return this.hiddenChannels.contains(chan.toLowerCase());
    }

    public void hideChannel(String chan) {
        this.hiddenChannels.add(chan.toLowerCase());
    }

    public void showChannel(String chan) {
        this.hiddenChannels.remove(chan.toLowerCase());
    }

    public void clearHiddenChannels() {
        this.hiddenChannels.clear();
    }

    public List<String> getChannelBans() {
        return channelBans;
    }

    public boolean isBannedFromChannel(String chan) {
        return this.channelBans.contains(chan.toLowerCase());
    }

    public void banFromChannel(String chan) {
        this.channelBans.add(chan.toLowerCase());
    }

    public void unbanFromChannel(String chan) {
        this.channelBans.remove(chan.toLowerCase());
    }

    public void clearChannelBans() {
        this.channelBans.clear();
    }

    public boolean isInChannel(String chan) {
        return !isChannelHidden(chan.toLowerCase()) && !isBannedFromChannel(chan.toLowerCase());
    }

    public boolean isInChannel(ChatChannel chan) {
        return !isChannelHidden(chan.getName().toLowerCase()) && !isBannedFromChannel(chan.getName().toLowerCase());
    }
}
