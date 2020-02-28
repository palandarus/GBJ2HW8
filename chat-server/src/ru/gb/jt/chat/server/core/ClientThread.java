package ru.gb.jt.chat.server.core;

import ru.gb.jt.chat.common.Library;
import ru.gb.jt.network.SocketThread;
import ru.gb.jt.network.SocketThreadListener;

import java.net.Socket;

public class ClientThread extends SocketThread {
    private String nickname;
    private boolean isAuthorized;
    private boolean isReconnecting;
    private boolean isRegistering;
    private boolean isRenaming;

    private long createTime;


    public boolean isReconnecting() {
        return isReconnecting;
    }

    void reconnect() {
        isReconnecting = true;
        isRegistering = false;
        isRenaming=false;
        close();
    }

    public ClientThread(SocketThreadListener listener, String name, Socket socket) {
        super(listener, name, socket);

        this.createTime = System.currentTimeMillis();
    }

    public long getCreateTime() {
        return createTime;


    }

    public String getNickname() {
        return nickname;
    }

    public boolean isAuthorized() {
        return isAuthorized;
    }

    public boolean isRegistering() {
        return isRegistering;
    }
    public boolean isRenaming() {
        return isRenaming;
    }



    void authAccept(String nickname) {
        isAuthorized = true;
        isRegistering = false;
        isRenaming=false;
        this.nickname = nickname;
        sendMessage(Library.getAuthAccept(nickname));
    }

    void authFail() {
        sendMessage(Library.getAuthDenied());

//        close();

    }

    void regFail(String msg) {
        isRegistering = true;
        sendMessage(Library.getRegDenied()+msg);

//        close();

    }
    void chNickFail(String msg) {
        isRenaming = true;
        sendMessage(Library.getRenDenied()+msg);

//        close();

    }

    void msgFormatError(String msg) {
        sendMessage(Library.getMsgFormatError(msg));
        close();
    }
}
