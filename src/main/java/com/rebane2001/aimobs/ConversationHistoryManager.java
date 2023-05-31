package com.rebane2001.aimobs;

import com.google.common.collect.EvictingQueue;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.UUID;
import java.util.*;
import java.io.*;

public class ConversationHistoryManager {

    private static final int CONVERSATION_HISTORY_SIZE = 5;
    private static final int TOKEN_LIMIT = 4096; // set according to your requirements
    private static Map<UUID, EntityConversation> conversationMap = new HashMap<>();
    
    public static void startConversation(Entity entity, PlayerEntity player) {
        EntityConversation conversation;
        if (!conversationMap.containsKey(entity.getUuid())) {
            conversation = new EntityConversation(entity, player);
            conversationMap.put(entity.getUuid(), conversation);
        } else {
            conversation = conversationMap.get(entity.getUuid());
            conversation.updatePrompt(entity, player);
        }
    }

    public static String getEntityDisplayName(UUID entityUUID) {
        EntityConversation conversation = conversationMap.get(entityUUID);
        if (conversation != null) {
            return conversation.getEntityDisplayName();
        }
        return "Entity";
    }

    public static void saveConversationHistory() {
        try {
            FileOutputStream fos = new FileOutputStream("conversationHistory.ser");
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(conversationMap);
            oos.close();
            fos.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public static void loadConversationHistory() {
        try {
            FileInputStream fis = new FileInputStream("conversationHistory.ser");
            ObjectInputStream ois = new ObjectInputStream(fis);
            Map<UUID, EntityConversation> loadedMap = (Map<UUID, EntityConversation>) ois.readObject();
            conversationMap = loadedMap;
            ois.close();
            fis.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (ClassNotFoundException c) {
            System.out.println("Class not found");
            c.printStackTrace();
        }
    }

    public static String truncateToTokenLimit(String text, int tokenLimit) {
        // Here is a simple implementation that truncates the text at sentence boundaries
        String[] sentences = text.split("\\. ");
        StringBuilder truncatedText = new StringBuilder();
    
        int tokenCount = 0;
        for (int i = sentences.length - 1; i >= 0; i--) {
            String sentence = sentences[i];
            int sentenceTokenCount = sentence.split(" ").length;  // this is a simplified token count. You might need to update it depending on your exact tokenization process
    
            if (tokenCount + sentenceTokenCount <= tokenLimit) {
                truncatedText.insert(0, sentence + ". ");
                tokenCount += sentenceTokenCount;
            } else {
                break;
            }
        }
    
        return truncatedText.toString();
    }

    public static void addPlayerMessageToHistory(String message, PlayerEntity player, UUID entityUUID) {
        EntityConversation conversation = conversationMap.get(entityUUID);
        if (conversation != null) {
            String truncatedMessage = truncateToTokenLimit(message, TOKEN_LIMIT);
            conversation.addPlayerMessage(truncatedMessage, player);
        }
    }

    public static void addAIMessageToHistory(String message, PlayerEntity player, UUID entityUUID) {
        EntityConversation conversation = conversationMap.get(entityUUID);
        if (conversation != null) {
            String truncatedMessage = truncateToTokenLimit(message, TOKEN_LIMIT);
            conversation.addAIMessage(truncatedMessage, player);
        }
    }

    public static String getAIResponse(UUID entityUUID) {
        EntityConversation conversation = conversationMap.get(entityUUID);
        if (conversation != null) {
            // Define maxTokens and isSummary
            Integer maxTokens = TOKEN_LIMIT; // Maximum tokens can vary based on your requirements.
            boolean isSummary = false; // Set it according to your needs.
            try {
                // Use RequestHandler to get the actual response from the OpenAI API.
                String response = RequestHandler.getAIResponse(conversation.prompt, maxTokens, isSummary);
                // Get player from conversation
                PlayerEntity player = conversation.getPlayer();
                addAIMessageToHistory(response, player, entityUUID);
                return response;
            } catch (IOException e) {
                e.printStackTrace();
                return "[AIMobs] Error getting response from OpenAI API";
            }
        }
        return "[AIMobs] Error getting conversation history";
    }    
    
    private static class EntityConversation implements Serializable {
        private final EvictingQueue<String> conversationHistory;
        private String baseName;
        private String displayName;
        private String prompt;
        private final PlayerEntity player;

        public EntityConversation(Entity entity, PlayerEntity player) {
            this.conversationHistory = EvictingQueue.create(CONVERSATION_HISTORY_SIZE);
            this.baseName = entity.getName().getString();
            this.displayName = this.baseName;
            this.player = player;
            updatePrompt(entity, player);
        }

        public PlayerEntity getPlayer() {
            return this.player;
        }

        public void updatePrompt(Entity entity, PlayerEntity player) {
            // Update the prompt based on the entity and player
            this.prompt = "Hello " + player.getName().getString() + ". I am " + entity.getName().getString();
        }
        
        public void addPlayerMessage(String message, PlayerEntity player) {
            // Add the player's message to the conversation history
            conversationHistory.add(player.getName().getString() + ": " + message);
        }
        
        public String getEntityDisplayName() {
            return this.displayName;
        }

        public void addAIMessage(String message, PlayerEntity player) {
            // Add the AI's message to the conversation history
            conversationHistory.add(this.displayName + ": " + message);
        }

    }
}
