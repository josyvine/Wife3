package com.wife.app;

import android.content.Context;
import java.util.ArrayList;
import java.util.List;

public class ChatManager {
    private static volatile ChatManager instance;
    private final Context context;
    private final List<MessageListener> listeners = new ArrayList<>();

    public interface MessageListener {
        void onMessageReceived(MessageEntity message);
        
        // Callback interface method to handle real-time unsend deletions
        void onMessageUnsent(long targetTimestamp);
    }

    public static ChatManager getInstance(Context context) {
        if (instance == null) {
            synchronized (ChatManager.class) {
                if (instance == null) {
                    instance = new ChatManager(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    private ChatManager(Context context) {
        this.context = context;
    }

    public synchronized void registerMessageListener(MessageListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public synchronized void unregisterMessageListener(MessageListener listener) {
        listeners.remove(listener);
    }

    public synchronized void notifyMessageReceived(MessageEntity message) {
        for (MessageListener listener : listeners) {
            listener.onMessageReceived(message);
        }
    }

    // Synchronized dispatcher to broadcast incoming unsend signals to active observers
    public synchronized void notifyMessageUnsent(long targetTimestamp) {
        for (MessageListener listener : listeners) {
            listener.onMessageUnsent(targetTimestamp);
        }
    }

    // Fixed: Added check to see if any observers are active in the foreground
    public synchronized boolean hasListeners() {
        return !listeners.isEmpty();
    }
}