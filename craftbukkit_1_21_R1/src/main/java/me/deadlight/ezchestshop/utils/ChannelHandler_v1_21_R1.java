package me.deadlight.ezchestshop.utils;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import me.deadlight.ezchestshop.EzChestShop;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ChannelHandler_v1_21_R1 extends ChannelInboundHandlerAdapter {

    private final Player player;
    private static Field updateSignArrays;
    private static Method updateSignMethod;

    static {
        try {
            updateSignArrays = ServerboundSignUpdatePacket.class.getDeclaredField("c");
            updateSignArrays.setAccessible(true);
        } catch (NoSuchFieldException ignored) {
            // 1.21 may rename the backing field, we will discover it lazily at runtime
        }
    }

    public ChannelHandler_v1_21_R1(Player player) {
        this.player = player;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        if (msg instanceof ServerboundInteractPacket) {

            if (!Utils.enabledOutlines.contains(player.getUniqueId())) {
                ctx.fireChannelRead(msg);
                return;
            }

            //now we check if player is right clicking on the outline shulkerbox, if so we open the shop for them
            ServerboundInteractPacket packet = (ServerboundInteractPacket) msg;
            Field field;
            try {
                field = packet.getClass().getDeclaredField("a"); //The field a is entity ID
            } catch (NoSuchFieldException e) {
                ctx.fireChannelRead(msg);
                return; //This is for ModelEngine
            }

            field.setAccessible(true);
            int entityID = (int) field.get(packet);
            if (Utils.activeOutlines.containsKey(entityID)) {
                BlockOutline outline = Utils.activeOutlines.get(entityID);
                outline.hideOutline();
                //Then it means somebody is clicking on the outline shulkerbox
                EzChestShop.getPlugin().getServer().getScheduler().runTaskLater(
                        EzChestShop.getPlugin(), () -> {
                            Bukkit.getPluginManager().callEvent(new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK,
                                    player.getInventory().getItemInMainHand(), outline.block,
                                    outline.block.getFace(outline.block), null));
                        }, 1L);
            }
        }

        if (msg instanceof ServerboundSignUpdatePacket) {
            for (Map.Entry<SignMenuFactory, UpdateSignListener> entry : v1_21_R1.getListeners().entrySet()) {
                UpdateSignListener listener = entry.getValue();

                try {
                    String[] lines = extractSignText((ServerboundSignUpdatePacket) msg);
                    if (lines != null) {
                        listener.listen(player, lines);
                    }
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }

                if (listener.isCancelled()) {
                    ctx.fireChannelRead(msg);
                    return;
                }
            }
        }

        ctx.fireChannelRead(msg);
    }

    private String[] extractSignText(ServerboundSignUpdatePacket packet)
            throws IllegalAccessException, InvocationTargetException {
        if (updateSignArrays != null) {
            return (String[]) updateSignArrays.get(packet);
        }

        if (updateSignMethod != null) {
            Object result = updateSignMethod.invoke(packet);
            if (result instanceof String[]) {
                return (String[]) result;
            }
            if (result instanceof List<?>) {
                return listToArray((List<?>) result);
            }
        }

        for (Field field : packet.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            if (field.getType().isArray() && field.getType().getComponentType() == String.class) {
                updateSignArrays = field;
                return (String[]) field.get(packet);
            }
            if (List.class.isAssignableFrom(field.getType())) {
                Object value = field.get(packet);
                if (value instanceof List<?>) {
                    String[] lines = listToArray((List<?>) value);
                    if (lines != null) {
                        return lines;
                    }
                }
            }
        }

        for (Method method : packet.getClass().getDeclaredMethods()) {
            if (method.getParameterCount() == 0) {
                Class<?> returnType = method.getReturnType();
                if (returnType.isArray() && returnType.getComponentType() == String.class) {
                    method.setAccessible(true);
                    updateSignMethod = method;
                    Object result = method.invoke(packet);
                    return result instanceof String[] ? (String[]) result : null;
                }
                if (List.class.isAssignableFrom(returnType)) {
                    method.setAccessible(true);
                    updateSignMethod = method;
                    Object result = method.invoke(packet);
                    if (result instanceof List<?>) {
                        return listToArray((List<?>) result);
                    }
                }
            }
        }
        return null;
    }

    private String[] listToArray(List<?> list) {
        if (list == null) {
            return null;
        }
        String[] lines = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            Object element = list.get(i);
            if (element == null) {
                lines[i] = "";
                continue;
            }
            if (element instanceof String) {
                lines[i] = (String) element;
                continue;
            }
            try {
                Method asString = element.getClass().getMethod("getString");
                lines[i] = Objects.toString(asString.invoke(element), "");
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
                lines[i] = element.toString();
            }
        }
        return lines;
    }
}
