package com.rebane2001.aimobs;

import com.rebane2001.aimobs.mixin.ChatHudAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import net.minecraft.registry.RegistryKey;
// import net.minecraft.world.EntityView;
import net.minecraft.world.biome.Biome;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public class ActionHandler {
    public static String prompts = "";
    public static String entityName = "";
    public static int entityId = 0;
    public static UUID initiator = null;
    public static long lastRequest = 0;

    // The waitMessage is the thing that goes '<Name> ...' before an actual response is received
    private static ChatHudLine.Visible waitMessage;
    private static List<ChatHudLine.Visible> getChatHudMessages() {
        return ((ChatHudAccessor)MinecraftClient.getInstance().inGameHud.getChatHud()).getVisibleMessages();
    }
    private static void showWaitMessage(String name) {
        if (waitMessage != null) getChatHudMessages().remove(waitMessage);
        waitMessage = new ChatHudLine.Visible(MinecraftClient.getInstance().inGameHud.getTicks(), OrderedText.concat(OrderedText.styledForwardsVisitedString("<" + name + "> ", Style.EMPTY),OrderedText.styledForwardsVisitedString("...", Style.EMPTY.withColor(Formatting.GRAY))), null, true);
        getChatHudMessages().add(0, waitMessage);
    }
    private static void hideWaitMessage() {
        if (waitMessage != null) getChatHudMessages().remove(waitMessage);
        waitMessage = null;
    }

    private static String getBiome(Entity entity) {
        Optional<RegistryKey<Biome>> biomeKey = entity.getEntityWorld().getBiomeAccess().getBiome(entity.getBlockPos()).getKey();
        if (biomeKey.isEmpty()) return "place";
        return I18n.translate(Util.createTranslationKey("biome", biomeKey.get().getValue()));
    }

    public static void startConversation(Entity entity, PlayerEntity player) {
        entityId = entity.getId();
        initiator = player.getUuid();
        prompts = createPrompt(entity, player);
        ItemStack heldItem = player.getMainHandStack();
        if (heldItem.getCount() > 0)
            prompts = "Ти тримаєш " + heldItem.getName().getString() + " у руці. " + prompts;
        showWaitMessage(entityName);
        getResponse(player);
    }

    public static void getResponse(PlayerEntity player) {
        // 1.5 second cooldown between requests
        if (lastRequest + 1500L > System.currentTimeMillis()) return;
        if (AIMobsConfig.config.apiKey.length() == 0) {
            player.sendMessage(Text.of("[AIMobs] You have not set an API key! Get one from https://beta.openai.com/account/api-keys and set it with /aimobs setkey"));
            return;
        }
        lastRequest = System.currentTimeMillis();
        Thread t = new Thread(() -> {
            try {
                String response = RequestHandler.getAIResponse(prompts);
                player.sendMessage(Text.of("<" + entityName + "> " + response));
                prompts += response + "\"\n";
            } catch (Exception e) {
                player.sendMessage(Text.of("[AIMobs] Error getting response"));
                e.printStackTrace();
            } finally {
                hideWaitMessage();
            }
        });
        t.start();
    }

    public static void replyToEntity(String message, PlayerEntity player) {
        if (entityId == 0) return;
        prompts += (player.getUuid() == initiator) ? "Ти кажеш: \"" : ("Твій друг " + player.getName().getString() + " каже: \"");
        prompts += message.replace("\"", "'") + "\"\n Сутність з англійською назвою " + entityName + " каже: \"";
        getResponse(player);
    }
    
    private static boolean isEntityHurt(LivingEntity entity) {
        return entity.getHealth() * 1.2 < entity.getAttributeValue(EntityAttributes.GENERIC_MAX_HEALTH);
    }

    private static String createPromptVillager(VillagerEntity villager, PlayerEntity player) {
        boolean isHurt = isEntityHurt(villager);
        entityName = "Villager";
        String villageName = "село, де живуть " + villager.getVillagerData().getType().toString().toLowerCase(Locale.ROOT) + " мешканці";
        int rep = villager.getReputation(player);
        if (rep < -5) villageName = villageName + ", яке вважає тебе жахливим";
        if (rep > 5) villageName = villageName + ", яке вважає тебе надійним";        
        if (villager.isBaby()) {
            entityName = "Villager Kid";
            return String.format("Ти бачиш дитину у %s. Дитина кричить: \"", villageName);
        }
        String profession = villager.getVillagerData().getProfession().toString().toLowerCase(Locale.ROOT).replace("none", "freelancer");
        entityName = profession;
        if (villager.getVillagerData().getLevel() >= 3) profession = "вправний " + profession;
        if (isHurt) profession = "ранений " + profession;
        return String.format("Ти зустрічаєш %s у %s. Мешканець говорить тобі: \"", profession, villageName);
    }    

    public static String createPromptLiving(LivingEntity entity) {
        boolean isHurt = isEntityHurt(entity);
        String baseName = entity.getName().getString();
        String name = baseName;
        Text customName = entity.getCustomName();
        if (customName != null)
            name = baseName + " назвою " + customName.getString();
        entityName = baseName;
        if (isHurt) name = "поранений " + name;
        return String.format("Ти зустрічаєш сутність з англійською назвою %s у біомі типу %s. Сутність %s говорить тобі: \"", name, getBiome(entity), baseName);
    }    

    public static String createPrompt(Entity entity, PlayerEntity player) {
        if (entity instanceof VillagerEntity villager) return createPromptVillager(villager, player);
        if (entity instanceof LivingEntity entityLiving) return createPromptLiving(entityLiving);
        entityName = entity.getName().getString();
        return "Ти бачиш сутність з англійською назвою " + entityName + ". Сутність " + entityName + " говорить: \"";
    }    

    public static void handlePunch(Entity entity, Entity player) {
        if (entity.getId() != entityId) return;
        prompts += ((player.getUuid() == initiator) ? "Ти б'єш" : (player.getName().getString() + " б'є")) + " сутність з англійською назвою " + entityName + ".\n";
    }    
}