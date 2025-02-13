/*
 * Copyright (c) 2023 mrmrmystery
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice (including the next paragraph) shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.somewhatcity.mixer.core.commands;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import de.tr7zw.changeme.nbtapi.NBTItem;
import de.tr7zw.changeme.nbtapi.NBTTileEntity;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.arguments.LocationType;
import net.kyori.adventure.platform.bukkit.BukkitComponentSerializer;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.somewhatcity.mixer.core.commands.dsp.DspCommand;
import net.somewhatcity.mixer.core.MixerPlugin;
import net.somewhatcity.mixer.core.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;

public class MixerCommand extends CommandAPICommand {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private static final AudioPlayerManager APM = new DefaultAudioPlayerManager();

    static {
        AudioSourceManagers.registerRemoteSources(APM);
        AudioSourceManagers.registerLocalSource(APM);
        APM.setFrameBufferDuration(100);
    }

    public MixerCommand() {
        super("mixer");
        withSubcommand(new CommandAPICommand("burn")
                .withPermission("mixer.command.burn")
                .withArguments(new GreedyStringArgument("url"))
                .executesPlayer((player, args) -> {
                    ItemStack item = player.getInventory().getItemInMainHand();
                    if (!Utils.isDisc(item)) {
                        player.sendMessage("§c음반을 들고 있어야 구울 수 있습니다!");
                        return;
                    }

                    String url = (String) args.get(0);

                    if(url.startsWith("file:")) {
                        String filename = url.substring(5);
                        File file = new File(filename);
                        if(file.exists() && file.isFile()) {
                            url = file.getAbsolutePath();
                        }
                    }
                    String finalUrl = url;
                    APM.loadItem(url, new AudioLoadResultHandler() {

                        @Override
                        public void trackLoaded(AudioTrack audioTrack) {
                            AudioTrackInfo info = audioTrack.getInfo();
                            Bukkit.getScheduler().runTask(MixerPlugin.getPlugin(), () -> {
                                ItemMeta meta = item.getItemMeta();
                                meta.setDisplayName(BukkitComponentSerializer.legacy().serialize(MM.deserialize("<reset>%s".formatted(info.title))));
//                                meta.displayName(MM.deserialize("<reset>%s".formatted(info.title)));

                                meta.setLore(Arrays.asList(
                                        BukkitComponentSerializer.legacy().serialize(MM.deserialize("<reset>%s".formatted(info.author)))
                                ));
                                item.setItemMeta(meta);
                                NBTItem nbtItem = new NBTItem(item);
                                nbtItem.setString("mixer_data", finalUrl);
                                nbtItem.applyNBT(item);
                            });
                        }

                        @Override
                        public void playlistLoaded(AudioPlaylist audioPlaylist) {
                            AudioTrackInfo info = audioPlaylist.getSelectedTrack().getInfo();
                            Bukkit.getScheduler().runTask(MixerPlugin.getPlugin(), () -> {
                                ItemMeta meta = item.getItemMeta();
                                meta.setDisplayName(BukkitComponentSerializer.legacy().serialize(MM.deserialize("<reset>%s".formatted(info.title))));
//                                meta.displayName(MM.deserialize("<reset>%s".formatted(info.title)));
                                meta.setLore(Arrays.asList(
                                        BukkitComponentSerializer.legacy().serialize(MM.deserialize("<reset>%s".formatted(info.author)))
                                ));
//                                meta.lore(Arrays.asList(
//                                        MM.deserialize("<reset>%s".formatted(info.author))
//                                ));
                                item.setItemMeta(meta);
                                NBTItem nbtItem = new NBTItem(item);
                                nbtItem.setString("mixer_data", finalUrl);
                                nbtItem.applyNBT(item);
                            });
                        }

                        @Override
                        public void noMatches() {
                            MixerPlugin.getPlugin().adventure().player(player).sendMessage(MM.deserialize("<red>파일을 찾을 수 없습니다."));
                        }

                        @Override
                        public void loadFailed(FriendlyException e) {
                            MixerPlugin.getPlugin().adventure().player(player).sendMessage(MM.deserialize("<red>%s".formatted(e.getMessage())));
                        }
                    });
                })
        );
        withSubcommand(new CommandAPICommand("play")
                        .withArguments(new GreedyStringArgument("url"))
                        .executesPlayer((player, args) -> {
                            String url = (String) args.get(0);

                            if(url.startsWith("file:")) {
                                String filename = url.substring(5);
                                File file = new File(filename);
                                if(file.exists() && file.isFile()) {
                                    url = file.getAbsolutePath();
                                }
                            }
                            String finalUrl = url;
                            APM.loadItem(url, new AudioLoadResultHandler() {

                                @Override
                                public void trackLoaded(AudioTrack audioTrack) {
                                    AudioTrackInfo info = audioTrack.getInfo();
                                    Bukkit.getScheduler().runTask(MixerPlugin.getPlugin(), () -> {
                                        MixerPlugin.getPlugin().adventure().player(player).sendMessage(MM.deserialize(String.format("<green>%s 파일이 준비되었습니다. 주크 박스에 우클릭하면 재생됩니다.", info.title)));
                                        MixerPlugin.currentLoaded.put(player.getUniqueId(), finalUrl);
                                    });
                                }

                                @Override
                                public void playlistLoaded(AudioPlaylist audioPlaylist) {
                                    AudioTrackInfo info = audioPlaylist.getSelectedTrack().getInfo();
                                    Bukkit.getScheduler().runTask(MixerPlugin.getPlugin(), () -> {
                                        MixerPlugin.getPlugin().adventure().player(player).sendMessage(MM.deserialize(String.format("<green>%s 파일이 준비되었습니다. 주크 박스에 우클릭하면 재생됩니다.", info.title)));
                                        MixerPlugin.currentLoaded.put(player.getUniqueId(), finalUrl);
                                    });
                                }

                                @Override
                                public void noMatches() {
                                    MixerPlugin.getPlugin().adventure().player(player).sendMessage(MM.deserialize("<red>파일을 찾을 수 없습니다."));
                                }

                                @Override
                                public void loadFailed(FriendlyException e) {
                                    MixerPlugin.getPlugin().adventure().player(player).sendMessage(MM.deserialize("<red>%s".formatted(e.getMessage())));
                                }
                            });
                        })
        );
        withSubcommand(new CommandAPICommand("link")
                .withPermission("mixer.command.link")
                .withArguments(new LocationArgument("jukebox", LocationType.BLOCK_POSITION))
                .executesPlayer((player, args) -> {
                    Location jukeboxLoc = (Location) args.get(0);
                    Block block = jukeboxLoc.getBlock();

                    if(!block.getType().equals(Material.JUKEBOX)) {
                        MixerPlugin.getPlugin().adventure().player(player).sendMessage(MM.deserialize("<red>No jukebox at location"));
                        return;
                    }

                    JsonArray linked;

                    NBTTileEntity jukebox = new NBTTileEntity(block.getState());
                    String data = jukebox.getPersistentDataContainer().getString("mixer_links");
                    if(data == null || data.isEmpty()) {
                        linked = new JsonArray();
                    } else {
                        linked = (JsonArray) JsonParser.parseString(data);
                    }

                    Location loc = Utils.toCenterLocation(player.getLocation());

                    JsonObject locData = new JsonObject();
                    locData.addProperty("x", loc.getX());
                    locData.addProperty("y", loc.getY());
                    locData.addProperty("z", loc.getZ());
                    locData.addProperty("world", loc.getWorld().getName());

                    linked.add(locData);
                    jukebox.getPersistentDataContainer().setString("mixer_links", linked.toString());

                    MixerPlugin.getPlugin().adventure().player(player).sendMessage(MM.deserialize("<green>Location linked to jukebox"));
                })
        );
        withSubcommand(new CommandAPICommand("redstone")
                .withPermission("mixer.command.redstone")
                .withArguments(new LocationArgument("jukebox", LocationType.BLOCK_POSITION))
                .withArguments(new IntegerArgument("magnitude", 0, 2048))
                .withArguments(new IntegerArgument("trigger", 0))
                .withArguments(new IntegerArgument("delay", 0))
                .executesPlayer(((player, args) -> {
                    Location jukeboxLoc = (Location) args.get(0);
                    Block block = jukeboxLoc.getBlock();

                    if(!block.getType().equals(Material.JUKEBOX)) {
                        MixerPlugin.getPlugin().adventure().player(player).sendMessage(MM.deserialize("<red>No jukebox at location"));
                        return;
                    }

                    JsonArray redstones;
                    NBTTileEntity jukebox = new NBTTileEntity(block.getState());
                    String data = jukebox.getPersistentDataContainer().getString("mixer_redstones");
                    if(data == null || data.isEmpty()) {
                        redstones = new JsonArray();
                    } else {
                        redstones = (JsonArray) JsonParser.parseString(data);
                    }

                    if(player.getTargetBlockExact(10) == null) {
                        MixerPlugin.getPlugin().adventure().player(player).sendMessage(MM.deserialize("<red>Not looking at a block"));
                        return;
                    }

                    Location loc = player.getTargetBlockExact(10).getLocation();

                    JsonObject locData = new JsonObject();
                    locData.addProperty("x", loc.getX());
                    locData.addProperty("y", loc.getY());
                    locData.addProperty("z", loc.getZ());
                    locData.addProperty("world", loc.getWorld().getName());
                    locData.addProperty("mag", (int) args.get(1));
                    locData.addProperty("trigger", (int) args.get(2));
                    locData.addProperty("delay", (int) args.get(3));

                    redstones.add(locData);
                    jukebox.getPersistentDataContainer().setString("mixer_redstones", redstones.toString());

                    MixerPlugin.getPlugin().adventure().player(player).sendMessage(MM.deserialize("<green>Redstone-Location linked to jukebox"));
                }))
        );

        withSubcommand(new DspCommand());
        register();
    }
}
