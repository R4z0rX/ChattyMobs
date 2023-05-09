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
            prompts = "Estás sosteniendo un objeto cuyo nombre en inglés es " + heldItem.getName().getString() + " en tu mano. " + prompts;
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
        prompts += (player.getUuid() == initiator) ? "Tú dices: \"" : ("Tu amigo " + player.getName().getString() + " dice: \"");
        prompts += message.replace("\"", "'") + "\"\n La entidad de nombre en inglés " + entityName + " dice: \"";
        getResponse(player);
    }
    
    private static boolean isEntityHurt(LivingEntity entity) {
        return entity.getHealth() * 1.2 < entity.getAttributeValue(EntityAttributes.GENERIC_MAX_HEALTH);
    }

    private static String createPromptVillager(VillagerEntity villager, PlayerEntity player) {
        boolean isHurt = isEntityHurt(villager);
        entityName = "Villager";
        String villageName = villager.getVillagerData().getType().toString().toLowerCase(Locale.ROOT) + " village";
        int rep = villager.getReputation(player);
        if (rep < -5) villageName = villageName + " que te ve como horrible";
        if (rep > 5) villageName = villageName + " que te ve como confiable";
        if (villager.isBaby()) {
            entityName = "Villager Kid";
            return String.format("Ves a un niño en un pueblo de tipo %s. El niño grita: \"", villageName);
        }
        String profession = villager.getVillagerData().getProfession().toString().toLowerCase(Locale.ROOT).replace("none", "freelancer");
        entityName = profession;
        if (villager.getVillagerData().getLevel() >= 3) profession = "habilidoso " + profession;
        if (isHurt) profession = "herido " + profession;
        return String.format("Te encuentras con un aldeano de profesión (en inglés) %s en un pueblo de tipo %s. El aldeano te dice: \"", profession, villageName);
    }    

    public static String createPromptLiving(LivingEntity entity) {
        boolean isHurt = isEntityHurt(entity);
        String baseName = entity.getName().getString();
        String name = baseName;
        Text customName = entity.getCustomName();
        if (customName != null)
            name = baseName + " llamado " + customName.getString();
        entityName = baseName;
        if (isHurt) name = "herida " + name;
        return String.format("Te encuentras con una entidad cuyo nombre en inglés es %s en un bioma de tipo %s. La entidad %s te dice: \"", name, getBiome(entity), baseName);
    }    

    public static String createPrompt(Entity entity, PlayerEntity player) {
        if (entity instanceof VillagerEntity villager) return createPromptVillager(villager, player);
        if (entity instanceof LivingEntity entityLiving) return createPromptLiving(entityLiving);
        entityName = entity.getName().getString();
        return "Ves a una entidad cuyo nombre en inglés es " + entityName + ". La entidad " + entityName + " dice: \"";
    }    

    public static void handlePunch(Entity entity, Entity player) {
        if (entity.getId() != entityId) return;
        prompts += ((player.getUuid() == initiator) ? "Golpeas" : (player.getName().getString() + " golpea")) + " a la entidad de nombre en inglés " + entityName + ".\n";
    }    
}