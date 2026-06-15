package com.wife.app;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;

public class MessageReceiver implements Runnable {
    private static final String TAG = "MessageReceiver";

    private final Context context;
    private final Socket socket;
    private final boolean isControl;

    public MessageReceiver(Context context, Socket socket, boolean isControl) {
        this.context = context;
        this.socket = socket;
        this.isControl = isControl;
    }

    @Override
    public void run() {
        String clientIp = socket.getInetAddress() != null ? socket.getInetAddress().getHostAddress() : "Unknown IP";
        WifeLogger.log(TAG, "MessageReceiver thread started. Socket Category: " + (isControl ? "Control" : "Text") + " | Client IP: " + clientIp);

        try (BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    WifeLogger.log(TAG, "Received an empty string payload line. Skipping processing.");
                    continue;
                }
                WifeLogger.log(TAG, "Received raw payload line: " + line);

                try {
                    JsonObject jsonObject = JsonParser.parseString(line).getAsJsonObject();
                    String type = jsonObject.has("type") ? jsonObject.get("type").getAsString() : "";
                    WifeLogger.log(TAG, "Parsed JSON packet successfully. Resolved type key: " + type);

                    if (isControl) {
                        handleControlMessage(type, jsonObject);
                    } else {
                        handleTextMessage(type, jsonObject);
                    }
                } catch (Exception parseException) {
                    WifeLogger.log(TAG, "Error parsing incoming JSON packet: " + parseException.getMessage(), parseException);
                }
            }
            WifeLogger.log(TAG, "End of input stream reached for socket from: " + clientIp);
        } catch (Exception e) {
            WifeLogger.log(TAG, "Socket stream read error or exception thrown: " + e.getMessage(), e);
        } finally {
            try {
                if (socket != null && !socket.isClosed()) {
                    WifeLogger.log(TAG, "Closing active connection socket client cleanly.");
                    socket.close();
                }
            } catch (Exception ex) {
                WifeLogger.log(TAG, "Error closing connection socket client: " + ex.getMessage(), ex);
            }
            WifeLogger.log(TAG, "MessageReceiver thread execution finalized for client: " + clientIp);
        }
    }

    private void handleControlMessage(String valType, JsonObject json) {
        String peerIp = socket.getInetAddress() != null ? socket.getInetAddress().getHostAddress() : "Unknown IP";
        WifeLogger.log(TAG, "handleControlMessage() invoked. Action code: " + valType + " | Origin IP: " + peerIp);

        // Dynamically update and register client IP address on any control message received
        ConnectionManager.getInstance(context).updatePeerIpFromAccept(peerIp);
        
        // Check for Call Signals
        if (valType.startsWith("CALL_") || valType.startsWith("VIDEO_CALL_")) {
            WifeLogger.log(TAG, "Forwarding calling signal to CallSignalingManager: " + valType);
            CallSignalingManager.getInstance(context).handleReceivedSignal(valType, json, peerIp);
            return;
        }

        // Check for Heartbeat
        if ("heartbeat".equals(valType)) {
            WifeLogger.log(TAG, "Control event matched: 'heartbeat'. Relaying payload to HeartbeatManager.");
            HeartbeatManager.getInstance(context).onHeartbeatReceived(peerIp);
            return;
        }

        if ("handshake".equals(valType)) {
            Log.d(TAG, "Handshake received, client ip updated.");
            WifeLogger.log(TAG, "Control event matched: 'handshake'. Handshake payload processed successfully.");
            // ConnectionManager.getInstance(context).updatePeerIpFromAccept(peerIp); // Redundant now as it is executed on method entry
        }
    }

    private void handleTextMessage(String valType, JsonObject json) {
        WifeLogger.log(TAG, "handleTextMessage() invoked. Data category matches: " + valType);
        if ("message".equals(valType)) {
            try {
                String id = json.get("id").getAsString();
                String sender = json.get("sender").getAsString();
                long time = json.get("time").getAsLong();
                String text = json.get("text").getAsString();

                WifeLogger.log(TAG, "Deserialized text payload. Msg ID: " + id + " | Sender Device: " + sender + " | Text Length: " + text.length());

                // Store in Database
                MessageEntity entity = new MessageEntity(sender, Utils.getDeviceId(context), text, time);
                RoomDatabaseManager.getInstance(context).messageDao().insert(entity);
                WifeLogger.log(TAG, "Incoming text message written successfully to local SQLite Database.");

                // Notify Active Chat Screen via Local Singleton / Broadcast
                ChatManager.getInstance(context).notifyMessageReceived(entity);
                WifeLogger.log(TAG, "Dispatched notification of text message reception to active ChatManager observers.");
            } catch (Exception e) {
                WifeLogger.log(TAG, "Failed to parse or process incoming text message packet: " + e.getMessage(), e);
            }
        } else {
            WifeLogger.log(TAG, "Unmatched text server packet type ignored: " + valType);
        }
    }
}