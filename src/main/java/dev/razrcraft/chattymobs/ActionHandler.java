package dev.razrcraft.chattymobs;

import dev.razrcraft.chattymobs.mixin.ChatHudAccessor;
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
import net.minecraft.world.biome.Biome;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Arrays;

import java.io.IOException;

public class ActionHandler {
    public static String prompts = "";
    public static String entityName = "";
    public static String entityBaseName = ""; // added new variable for base name
    public static String entityDisplayName = ""; // added new variable for display name
    public static int entityId = 0;
    public static UUID initiator = null;
    public static long lastRequest = 0;
    public static HashMap<UUID, List<String>> conversationHistory = new HashMap<>();

    private static ChatHudLine.Visible waitMessage;
    private static List<ChatHudLine.Visible> getChatHudMessages() {
        return ((ChatHudAccessor)MinecraftClient.getInstance().inGameHud.getChatHud()).getVisibleMessages();
    }
    private static void showWaitMessage(String name) {
        if (waitMessage != null) getChatHudMessages().remove(waitMessage);
        waitMessage = new ChatHudLine.Visible(MinecraftClient.getInstance().inGameHud.getTicks(), OrderedText.concat(OrderedText.styledForwardsVisitedString("<" + name + "> ", Style.EMPTY),OrderedText.styledForwardsVisitedString("...", Style.EMPTY.withColor(Formatting.GRAY))), null, true);
        getChatHudMessages().add(0, waitMessage);
    }
/*    private static void hideWaitMessage() {
        if (waitMessage != null) getChatHudMessages().remove(waitMessage);
        waitMessage = null;
    }*/

    private static String getBiome(Entity entity) {
        Optional<RegistryKey<Biome>> biomeKey = entity.getEntityWorld().getBiomeAccess().getBiome(entity.getBlockPos()).getKey();
        if (biomeKey.isEmpty()) return String.valueOf(Text.translatable("chattymobs.place"));
        return I18n.translate(Util.createTranslationKey("biome", biomeKey.get().getValue()));
    }

    public static void startConversation(Entity entity, PlayerEntity player) {
        entityId = entity.getId();
        initiator = player.getUuid();
        // Initialize conversation history for the entity if it doesn't exist
        if (!conversationHistory.containsKey(entity.getUuid())) {
            conversationHistory.put(entity.getUuid(), new ArrayList<>());
        }
        // Load conversation history into prompts
        prompts = String.join("\n", conversationHistory.get(entity.getUuid()));
        prompts = createPrompt(entity, player);
        ItemStack heldItem = player.getMainHandStack();
        if (heldItem.getCount() > 0)
            //prompts = "Estás sosteniendo un objeto cuyo nombre en inglés es " + heldItem.getName().getString() + " en tu mano. " + prompts;
            prompts = String.valueOf(Text.translatable("chattymobs.held_item", Text.translatable(heldItem.getItem().getTranslationKey())).append(" " + prompts));
        showWaitMessage(entityName);
        getResponse(player);
    }
    

    public static void getResponse(PlayerEntity player) {
        if (lastRequest + 1500L > System.currentTimeMillis()) return;
        if (ChattyMobsConfig.config.apiKey.isEmpty()) {
            player.sendMessage(Text.of("[ChattyMobs] " + Text.translatable("chattymobs.set_api_key") + " /chattymobs setkey"));
            return;
        }
        lastRequest = System.currentTimeMillis();
        Thread t = new Thread(() -> {
            try {
                String response = RequestHandler.getAIResponse(prompts);
                player.sendMessage(Text.of("<" + entityDisplayName + "> " + response));
                // Append player's message and GPT-3 model's response to conversation history
                conversationHistory.get(initiator).add("You say: \"" + prompts + "\"");
                conversationHistory.get(initiator).add("The " + entityDisplayName + " says: \"" + response + "\"");
                // Call summarizeConversation method
                summarizeConversation();
            } catch (Exception e) {
                // existing code...
            }
        });
        t.start();
    }

    public static void replyToEntity(String message, PlayerEntity player) {
        if (entityId == 0) return;
        prompts += (player.getUuid() == initiator) ? "Tú dices: \"" : ("Tu amigo " + player.getName().getString() + " dice: \"");
        prompts += message.replace("\"", "'") + "\"\n La entidad de nombre en inglés " + entityName + " dice: \"";
        getResponse(player);
        // Append player's message and GPT-3 model's response to conversation history
        conversationHistory.get(initiator).add((player.getUuid() == initiator) ? "You say: \"" : ("Your friend " + player.getName().getString() + " says: \"") + message.replace("\"", "'") + "\"");
        conversationHistory.get(initiator).add("The " + entityDisplayName + " says: \"" + prompts + "\"");
        // Call summarizeConversation method
        summarizeConversation();
    }

    public static void summarizeConversation() {
        // Get the last 8 messages from the conversation history
        int historySize = conversationHistory.get(initiator).size();
        List<String> recentHistory;
        if (historySize > 8) {
            recentHistory = conversationHistory.get(initiator).subList(historySize - 8, historySize);
        } else {
            recentHistory = conversationHistory.get(initiator);
        }
        // Send recentHistory to the GPT-3 model to generate a summary
        String summary = "";
        try {
            summary = RequestHandler.getAIResponse(String.join("\n", recentHistory));
        } catch (IOException e) {
            //e.printStackTrace();
            ChattyMobsMod.LOGGER.error(Arrays.toString(e.getStackTrace()));
        }
        // Replace the entire content of the conversation history with the summary
        conversationHistory.put(initiator, new ArrayList<>(Arrays.asList(summary.split("\n"))));
    }
    
    private static boolean isEntityHurt(LivingEntity entity) {
        return entity.getHealth() * 1.2 < entity.getAttributeValue(EntityAttributes.GENERIC_MAX_HEALTH);
    }

    private static String createPromptVillager(VillagerEntity villager, PlayerEntity player) {
        boolean isHurt = isEntityHurt(villager);
        entityBaseName = String.valueOf(Text.translatable("entity.minecraft.villager"));
        entityDisplayName = entityBaseName; // initially set display name to be the base name
        //String villageName = villager.getVillagerData().getType().toString().toLowerCase(Locale.ROOT) + " village";
        String villageName = String.valueOf(Text.translatable("chattymobs.village_name", Text.translatable("biome.minecraft." + villager.getVillagerData().getType().toString().toLowerCase(Locale.ROOT))));
        int rep = villager.getReputation(player);
        if (rep < -5) villageName = villageName + Text.translatable("chattymobs.bad_reputation");
        if (rep > 5) villageName = villageName + Text.translatable("chattymobs.good_reputation");
        if (villager.isBaby()) {
            entityBaseName = String.valueOf(Text.translatable("chattymobs.villager_kid"));
            entityDisplayName = entityBaseName; // set display name to be the new base name
            //return String.format("Ves a un niño en un pueblo de tipo %s. El niño grita: \"", villageName);
            return String.valueOf(Text.translatable("chattymobs.kid_say_village_name", villageName));
        }
        String profession = StringUtils.capitalize(villager.getVillagerData().getProfession().toString().toLowerCase(Locale.ROOT)); //.replace("none", "freelancer"));
        // overwrite base name with the profession
        if (profession.equals("none")) {
            entityBaseName = String.valueOf(Text.translatable("chattymobs.freelancer"));
        } else {
            entityBaseName = String.valueOf(Text.translatable("entity.minecraft.villager." + profession));
        }
        entityDisplayName = entityBaseName; // set display name to be the new base name
        //if (villager.getVillagerData().getLevel() >= 3) entityDisplayName = "habilidoso " + entityDisplayName; // modify display name
        entityDisplayName = String.valueOf(Text.translatable("merchant.level." + villager.getVillagerData().getLevel()));
        if (isHurt) entityDisplayName = Text.translatable("chattymobs.hurt") + entityDisplayName; // modify display name
        Text customName = villager.getCustomName();
        if (customName != null)
            entityDisplayName = entityDisplayName + Text.translatable("chattymobs.called") + customName.getString(); // modify display name
        return String.valueOf(Text.translatable("chattymobs.meet_villager", entityDisplayName, villageName));
    }

    public static String createPromptLiving(LivingEntity entity) {
        boolean isHurt = isEntityHurt(entity);
        // set base name for the entity: cow, pig, sheep, etc.
        entityBaseName = entity.getType().getTranslationKey().replace("entity.minecraft.", "").replace("_", " ");
        entityDisplayName = entityBaseName; // initially set display name to be the base name
        Text customName = entity.getCustomName();
        if (customName != null)
            entityDisplayName = StringUtils.capitalize(entityBaseName) + " llamado " + customName.getString();  // modify display name
        if (isHurt) entityDisplayName = Text.translatable("chattymobs.hurt2") + entityDisplayName; // modify display name
        return String.valueOf(Text.translatable("chattymobs.meet_living_entity", entityDisplayName, getBiome(entity), entityBaseName));  // use base name here to keep the entity type in the conversation
    }
    
    public static String createPrompt(Entity entity, PlayerEntity player) {
        if (entity instanceof VillagerEntity villager) return createPromptVillager(villager, player);
        if (entity instanceof LivingEntity entityLiving) return createPromptLiving(entityLiving);
        entityDisplayName = entity.getName().getString();
        return String.valueOf(Text.translatable("chattymobs.meet_entity", entityDisplayName, entityDisplayName));
    }

    public static void handlePunch(Entity entity, Entity player) {
        if (entity.getId() != entityId) return;
        prompts += ((player.getUuid() == initiator) ? "Golpeas" : (player.getName().getString() + " golpea")) + " a la entidad de nombre en inglés " + entityName + ".\n";
        //prompts += "Suddenly, " + player.getName().getString() + " punches the " + entityDisplayName + ". The " + entityBaseName + " screams out in pain: \"";
        //prompts += "De pronto, " + player.getName().getString() + " golpea a la entidad de nombre en inglés " + entityDisplayName + ", y grita de dolor: \"";
        getResponse((PlayerEntity) player);
    }
}
