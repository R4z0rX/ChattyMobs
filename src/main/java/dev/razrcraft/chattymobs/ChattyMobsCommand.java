package dev.razrcraft.chattymobs;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import static dev.razrcraft.chattymobs.ChattyMobsMod.MOD_NAME;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;

public class ChattyMobsCommand {

    public static void setupChattyMobsCommand(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(literal("chattymobs")
                .executes(ChattyMobsCommand::status)
                .then(literal("help").executes(ChattyMobsCommand::help))
                .then(literal("setkey")
                        .then(argument("key", StringArgumentType.string())
                                .executes(ChattyMobsCommand::setAPIKey)
                        ))
                .then(literal("setmodel")
                        .then(argument("model", StringArgumentType.string())
                                .executes(ChattyMobsCommand::setModel)
                        ))
                .then(literal("settemp")
                        .then(argument("temperature", FloatArgumentType.floatArg(0,1))
                                .executes(ChattyMobsCommand::setTemp)
                        ))
                .then(literal("enable").executes(context -> setEnabled(context, true)))
                .then(literal("disable").executes(context -> setEnabled(context, false)))
        );
    }
    public static int setEnabled(CommandContext<FabricClientCommandSource> context, boolean enabled) {
        ChattyMobsConfig.config.enabled = enabled;
        ChattyMobsConfig.saveConfig();
        context.getSource().sendFeedback(Text.of(MOD_NAME + " " + (enabled ? "enabled" : "disabled")));
        return 1;
    }

    public static int status(CommandContext<FabricClientCommandSource> context) {
        boolean hasKey = !ChattyMobsConfig.config.apiKey.isEmpty();
        Text yes = Text.literal("Yes").formatted(Formatting.GREEN);
        Text no = Text.literal("No").formatted(Formatting.RED);
        Text helpText = Text.literal("")
                .append(Text.literal(MOD_NAME).formatted(Formatting.UNDERLINE))
                .append("").formatted(Formatting.RESET)
                .append("\nEnabled: ").append(ChattyMobsConfig.config.enabled ? yes : no)
                .append("\nAPI Key: ").append(hasKey ? yes : no)
                .append("\nModel: ").append(ChattyMobsConfig.config.model)
                .append("\nTemp: ").append(String.valueOf(ChattyMobsConfig.config.temperature))
                .append("\n\nUse ").append(Text.literal("/chattymobs help").formatted(Formatting.GRAY)).append(" for help");
        context.getSource().sendFeedback(helpText);
        return 1;
    }

    public static int help(CommandContext<FabricClientCommandSource> context) {
        Text helpText = Text.literal("")
                .append(MOD_NAME + " Commands").formatted(Formatting.UNDERLINE)
                .append("").formatted(Formatting.RESET)
                .append("\n/chattymobs - View configuration status")
                .append("\n/chattymobs help - View commands help")
                .append("\n/chattymobs enable/disable - Enable/disable the mod")
                .append("\n/chattymobs setkey <key> - Set OpenAI API key")
                .append("\n/chattymobs setmodel <model> - Set AI model")
                .append("\n/chattymobs settemp <temperature> - Set model temperature")
                .append("\nYou can talk to mobs by shift-clicking on them!");
        context.getSource().sendFeedback(helpText);
        return 1;
    }
    public static int setAPIKey(CommandContext<FabricClientCommandSource> context) {
        String apiKey = context.getArgument("key", String.class);
        if (!apiKey.isEmpty()) {
            ChattyMobsConfig.config.apiKey = apiKey;
            ChattyMobsConfig.saveConfig();
            context.getSource().sendFeedback(Text.of("API key set"));
            return 1;
        }
        return 0;
    }
    public static int setModel(CommandContext<FabricClientCommandSource> context) {
        String model = context.getArgument("model", String.class);
        if (!model.isEmpty()) {
            ChattyMobsConfig.config.model = model;
            ChattyMobsConfig.saveConfig();
            context.getSource().sendFeedback(Text.of("Model set"));
            return 1;
        }
        return 0;
    }
    public static int setTemp(CommandContext<FabricClientCommandSource> context) {
        ChattyMobsConfig.config.temperature = context.getArgument("temperature", float.class);
        ChattyMobsConfig.saveConfig();
        context.getSource().sendFeedback(Text.of("Temperature set"));
        return 1;
    }
}
