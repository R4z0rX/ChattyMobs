package dev.razrcraft.chattymobs;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.util.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChattyMobsMod implements ClientModInitializer {
	public static final String MOD_ID = "chattymobs";
	public static final String MOD_NAME = "Chatty Mobs";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitializeClient() {
		ChattyMobsConfig.loadConfig();
		ClientCommandRegistrationCallback.EVENT.register(ChattyMobsCommand::setupChattyMobsCommand);
		AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if (!ChattyMobsConfig.config.enabled) return ActionResult.PASS;
			if (!player.isSneaking()) {
				if (entity.getId() == ActionHandler.entityId)
					ActionHandler.handlePunch(entity, player);
				return ActionResult.PASS;
			}
			ActionHandler.startConversation(entity, player);
			return ActionResult.FAIL;
		});
	}
}
