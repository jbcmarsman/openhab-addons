/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.volumio2.internal;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

import org.json.JSONException;
import org.json.JSONObject;
import org.openhab.binding.volumio2.internal.mapping.Volumio2Commands;
import org.openhab.core.library.types.PercentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

/**
 * @author Patrick Sernetz - Initial Contribution
 * @author Chris Wohlbrecht - Adaption for openHAB 3
 */
public class Volumio2Service {

    private static final Logger logger = LoggerFactory.getLogger(Volumio2Service.class);

    private final Socket socket;

    private boolean connected;

    public Volumio2Service(String protocol, String hostname, int port, int timeout)
            throws URISyntaxException, UnknownHostException {

        String uriString = String.format("%s://%s:%d", protocol, hostname, port);

        URI destUri = new URI(uriString);

        IO.Options opts = new IO.Options();
        opts.reconnection = true;
        opts.reconnectionDelay = 1000 * 30;
        opts.reconnectionDelayMax = 1000 * 60;
        opts.timeout = timeout;

        // Connection to mdns endpoint is only available after fetching ip.
        InetAddress ipaddress = InetAddress.getByName(hostname);
        logger.debug("Resolving {} to IP {}", hostname, ipaddress.getHostAddress());

        socket = IO.socket(destUri, opts);

        bindDefaultEvents(hostname);
    }

    private void bindDefaultEvents(String hostname) {

        socket.on(Socket.EVENT_CONNECT_ERROR, arg -> logger.error("Could not connect to Volumio on {}", hostname));

        socket.on(Socket.EVENT_CONNECT, arg -> {
            logger.info("Connected to Volumio2 on {}", hostname);
            setConnected(true);

        }).on(Socket.EVENT_DISCONNECT, arg -> {
            logger.warn("Disconnected from Volumio2 on {}", hostname);
            setConnected(false);
        });
    }

    public void connect() throws InterruptedException {
        socket.connect();
    }

    public void disconnect() {
        socket.disconnect();
    }

    public void close() {
        socket.off();
        socket.close();
    }

    public void on(String eventName, Emitter.Listener listener) {
        socket.on(eventName, listener);
    }

    public void once(String eventName, Emitter.Listener listener) {
        socket.once(eventName, listener);
    }

    public void getState() {
        socket.emit(Volumio2Commands.GET_STATE);
    }

    public void play() {
        socket.emit(Volumio2Commands.PLAY);
    }

    public void pause() {
        socket.emit(Volumio2Commands.PAUSE);
    }

    public void stop() {
        socket.emit(Volumio2Commands.STOP);
    }

    public void play(Integer index) {
        socket.emit(Volumio2Commands.PLAY, index);
    }

    public void next() {
        socket.emit(Volumio2Commands.NEXT);
    }

    public void previous() {
        socket.emit(Volumio2Commands.PREVIOUS);
    }

    public void setVolume(PercentType level) {
        socket.emit(Volumio2Commands.VOLUME, level.intValue());
    }

    public void shutdown() {
        socket.emit(Volumio2Commands.SHUTDOWN);
    }

    public void reboot() {
        socket.emit(Volumio2Commands.REBOOT);
    }

    public void playPlaylist(String playlistName) {
        JSONObject item = new JSONObject();

        try {
            item.put("name", playlistName);

            socket.emit(Volumio2Commands.PLAY_PLAYLIST, item);

        } catch (JSONException e) {
            logger.error("The following error occurred {}", e.getMessage());
        }
    }

    public void clearQueue() {
        socket.emit(Volumio2Commands.CLEAR_QUEUE);
    }

    public void setRandom(boolean val) {
        JSONObject item = new JSONObject();

        try {
            item.put("value", val);

            socket.emit(Volumio2Commands.RANDOM, item);
        } catch (JSONException e) {
            logger.error("The following error occurred {}", e.getMessage());
        }
    }

    public void setRepeat(boolean val) {
        JSONObject item = new JSONObject();

        try {
            item.put("value", val);

            socket.emit(Volumio2Commands.REPEAT, item);
        } catch (JSONException e) {
            logger.error("The following error occurred {}", e.getMessage());
        }
    }

    public void playFavorites(String favoriteName) {
        JSONObject item = new JSONObject();

        try {
            item.put("name", favoriteName);

            socket.emit(Volumio2Commands.PLAY_FAVOURITES, item);
        } catch (JSONException e) {
            logger.error("The following error occurred {}", e.getMessage());
        }
    }

    /**
     * Play a radio station from volumio´s Radio Favourites identifed by
     * its index.
     *
     * @param index
     */
    public void playRadioFavourite(final Integer index) {
        logger.debug("socket.emit({})", Volumio2Commands.PLAY_RADIO_FAVOURITES);

        socket.once("pushPlayRadioFavourites", arg -> play(index));

        socket.emit(Volumio2Commands.PLAY_RADIO_FAVOURITES);
    }

    public void playURI(String uri) {
        JSONObject item = new JSONObject();
        logger.debug("PlayURI: {}", uri);
        try {
            item.put("uri", uri);

            socket.emit(Volumio2Commands.PLAY, uri);
        } catch (JSONException e) {
            logger.error("The following error occurred {}", e.getMessage());
        }
    }

    public void addPlay(String uri, String title, String serviceType) {
        JSONObject item = new JSONObject();

        try {
            item.put("uri", uri);
            item.put("title", title);
            item.put("service", serviceType);

            socket.emit(Volumio2Commands.ADD_PLAY, item);

        } catch (JSONException e) {
            logger.error("The following error occurred {}", e.getMessage());
        }
    }

    public void replacePlay(String uri, String title, String serviceType) {
        JSONObject item = new JSONObject();

        try {
            item.put("uri", uri);
            item.put("title", title);
            item.put("service", serviceType);

            socket.emit(Volumio2Commands.REPLACE_AND_PLAY, item);

        } catch (JSONException e) {
            logger.error("The following error occurred {}", e.getMessage());
        }
    }

    public boolean isConnected() {
        return this.connected;
    }

    public void setConnected(boolean status) {
        this.connected = status;
    }

    public void sendSystemCommand(String string) {
        logger.warn("Jukebox Command: {}", string);
        switch (string) {
            case Volumio2Commands.SHUTDOWN:
                shutdown();
                break;
            case Volumio2Commands.REBOOT:
                reboot();
                break;
            default:
                break;
        }
    }
}
