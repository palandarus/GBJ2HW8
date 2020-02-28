package ru.gb.jt.chat.client;

import com.sun.jdi.JDIPermission;
import ru.gb.jt.chat.common.Library;
import ru.gb.jt.network.SocketThread;
import ru.gb.jt.network.SocketThreadListener;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Vector;

public class ClientGUI extends JFrame implements ActionListener, ListSelectionListener, Thread.UncaughtExceptionHandler, SocketThreadListener {

    private static final int WIDTH = 600;
    private static final int HEIGHT = 300;
    private String nickname="";

    private final JTextArea log = new JTextArea();
    private final JPanel panelTop = new JPanel(new GridLayout(2, 3));
    //    private final JTextField tfIPAddress = new JTextField("95.84.209.91");
    private final JTextField tfIPAddress = new JTextField("127.0.0.1");

    private final JTextField tfPort = new JTextField("8189");
    private final JCheckBox cbAlwaysOnTop = new JCheckBox("Always on top");
    private final JTextField tfLogin = new JTextField("ivan");
    private final JPasswordField tfPassword = new JPasswordField("123");
    private final JButton btnLogin = new JButton("Login");
    private final JButton btnRegister = new JButton("Register");

    private final JPanel panelBottom = new JPanel(new BorderLayout());
    private final JButton btnDisconnect = new JButton("<html><b>Disconnect</b></html>");
    private final JTextField tfMessage = new JTextField();
    private final JButton btnSend = new JButton("Send");

    private final JList<String> userList = new JList<>();
    private boolean shownIoErrors = false;
    private SocketThread socketThread;
    private final DateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss: ");
    private final String WINDOW_TITLE = "Chat";
    private final String REGISTRATION_FORM_TITLE = "Форма регистрации нового пользователя";
    private RegistrationDialog registrationFrom=new RegistrationDialog(this, REGISTRATION_FORM_TITLE, true, tfLogin.getText(), tfPassword.getText());

    private ClientGUI() {
        Thread.setDefaultUncaughtExceptionHandler(this);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setTitle(WINDOW_TITLE);
        setSize(WIDTH, HEIGHT);
        log.setEditable(false);
        log.setLineWrap(true);
        JScrollPane scrollLog = new JScrollPane(log);
        JScrollPane scrollUser = new JScrollPane(userList);
        scrollUser.setPreferredSize(new Dimension(100, 0));
        btnLogin.setPreferredSize(new Dimension(100, 0));
        btnRegister.setPreferredSize(new Dimension(100, 0));
        cbAlwaysOnTop.addActionListener(this);
        btnSend.addActionListener(this);
        tfMessage.addActionListener(this);
        btnLogin.addActionListener(this);
        btnRegister.addActionListener(this);
        btnDisconnect.addActionListener(this);
        userList.addListSelectionListener(this);
        panelBottom.setVisible(false);

        panelTop.add(tfIPAddress);
        panelTop.add(tfPort);
        panelTop.add(tfLogin);
        panelTop.add(tfPassword);
        panelTop.add(cbAlwaysOnTop);
        panelTop.add(btnLogin);
        panelTop.add(btnRegister);
        panelBottom.add(btnDisconnect, BorderLayout.WEST);
        panelBottom.add(tfMessage, BorderLayout.CENTER);
        panelBottom.add(btnSend, BorderLayout.EAST);

        add(scrollLog, BorderLayout.CENTER);
        add(scrollUser, BorderLayout.EAST);
        add(panelTop, BorderLayout.NORTH);
        add(panelBottom, BorderLayout.SOUTH);

        setVisible(true);
    }

    private void connect() {
        try {
            Socket socket = new Socket(tfIPAddress.getText(), Integer.parseInt(tfPort.getText()));
            socketThread = new SocketThread(this, "Client", socket);
        } catch (IOException e) {
            showException(Thread.currentThread(), e);
        }
    }

    public void register() {

        registrationFrom.tfLogin.setText(tfLogin.getText());
        registrationFrom.tfNickname.setText(tfLogin.getText());
        registrationFrom.tfPassword.setText(tfPassword.getText());
        registrationFrom.setVisible(true);

    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() { // Event Dispatching Thread
                new ClientGUI();
            }
        });
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if (src == cbAlwaysOnTop) {
            setAlwaysOnTop(cbAlwaysOnTop.isSelected());
        } else if (src == btnSend || src == tfMessage) {
            sendMessage();
        } else if (src == btnLogin) {
            connect();
        } else if (src == btnDisconnect) {
            socketThread.close();
        } else if (src == btnRegister) {
            register();
        } else {
            throw new RuntimeException("Unknown source: " + src);
        }
    }

    private void sendMessage() {
        String msg = tfMessage.getText();
        String username = tfLogin.getText();
        if ("".equals(msg)) return;
        tfMessage.setText(null);
        tfMessage.requestFocusInWindow();
        socketThread.sendMessage(Library.getTypeBcastClient(msg));
        //wrtMsgToLogFile(msg, username);
    }

    private void wrtMsgToLogFile(String msg, String username) {
        try (FileWriter out = new FileWriter("log.txt", true)) {
            out.write(username + ": " + msg + "\n");
            out.flush();
        } catch (IOException e) {
            if (!shownIoErrors) {
                shownIoErrors = true;
                showException(Thread.currentThread(), e);
            }
        }
    }

    private void putLog(String msg) {
        if ("".equals(msg)) return;
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                log.append(msg + "\n");
                log.setCaretPosition(log.getDocument().getLength());
            }
        });
    }

    private void showException(Thread t, Throwable e) {
        String msg;
        StackTraceElement[] ste = e.getStackTrace();
        if (ste.length == 0)
            msg = "Empty Stacktrace";
        else {
            msg = "Exception in " + t.getName() + " " +
                    e.getClass().getCanonicalName() + ": " +
                    e.getMessage() + "\n\t at " + ste[0];
        }
        JOptionPane.showMessageDialog(null, msg, "Exception", JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        e.printStackTrace();
        showException(t, e);
        System.exit(1);
    }

    /**
     * Socket thread listener methods
     */


    @Override
    public void onSocketStart(SocketThread thread, Socket socket) {
        putLog("Start");
    }

    @Override
    public void onSocketStop(SocketThread thread) {
        panelBottom.setVisible(false);
        panelTop.setVisible(true);
        setTitle(WINDOW_TITLE);
        userList.setListData(new String[0]);
        nickname="";
    }

    @Override
    public void onSocketReady(SocketThread thread, Socket socket) {
        panelBottom.setVisible(true);
        panelTop.setVisible(false);
        String login = tfLogin.getText();
        String password = new String(tfPassword.getPassword());
        if ("".equals(nickname)) {
            thread.sendMessage(Library.getAuthRequest(login, password));
        } else {
            thread.sendMessage(Library.getRegRequest(login, password, nickname));
        }

    }

    @Override
    public void onReceiveString(SocketThread thread, Socket socket, String msg) {
        handleMessage(msg);
    }

    @Override
    public void onSocketException(SocketThread thread, Exception exception) {
        // showException(thread, exception);
    }

    private void handleMessage(String msg) {
        String[] arr = msg.split(Library.DELIMITER);
        String msgType = arr[0];
        switch (msgType) {
            case Library.AUTH_ACCEPT:
                setTitle(WINDOW_TITLE + " entered with nickname: " + arr[1]);
                nickname = arr[1];
                registrationFrom.setVisible(false);
                break;
            case Library.AUTH_DENIED:

                putLog(msg.replace(Library.DELIMITER, " "));

                break;
            case Library.REG_DENIED:
                new ErrorDialog(this,msg.replace(Library.DELIMITER, " "));
//                throw new RuntimeException("Current user already exist: " + msg);

            case Library.MSG_FORMAT_ERROR:
                putLog(msg);
                socketThread.close();
                break;
            case Library.TYPE_BROADCAST:
                putLog(DATE_FORMAT.format(Long.parseLong(arr[1])) +
                        arr[2] + ": " + arr[3]);
                break;
            case Library.USER_LIST:
                String users = msg.substring(Library.USER_LIST.length() +
                        Library.DELIMITER.length());
                Vector<String> userVector=new Vector<>();
                userVector.addAll(Arrays.asList(users.split(Library.DELIMITER)));
                Collections.sort(userVector);
                userVector.remove(userVector.indexOf(nickname));
                userVector.insertElementAt(nickname,0);
                userList.setListData(userVector);


                break;
            default:
                throw new RuntimeException("Unknown message type: " + msg);
        }
    }

    @Override
    public void valueChanged(ListSelectionEvent listSelectionEvent) {
        Object element = userList.getSelectedValue();
        if (((String) element).equals(nickname)) {
            System.out.println("hi");
        }

    }

    /**
     * Register Dialog Window
     */
    public class RegistrationDialog extends JDialog implements ActionListener {
        private static final int WIDTH = 600;
        private static final int HEIGHT = 115;
        private final JLabel lLogin = new JLabel("Input Login:");
        private final JTextField tfLogin;
        private final JLabel lNickname = new JLabel("Input Nickname:");
        private final JTextField tfNickname;
        private final JLabel lPassword = new JLabel("Input Password:");
        private final JPasswordField tfPassword;
        private final JPanel panelTop = new JPanel(new GridLayout(2, 3));
        private final JPanel panelBottom = new JPanel(new GridLayout(1, 1));
        private final JButton btnRegister = new JButton("Register");
        private final ClientGUI parent;

        public RegistrationDialog(Frame owner, String title, boolean modal, String login, String password) {
            super(owner, title, modal);
            parent = (ClientGUI) owner;

            setLocationRelativeTo(null);
            setSize(WIDTH, HEIGHT);
            setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            Thread.setDefaultUncaughtExceptionHandler((Thread.UncaughtExceptionHandler) owner);
            tfLogin = new JTextField(login);
            tfNickname = new JTextField(login);
            tfPassword = new JPasswordField(password);

            panelTop.add(lLogin);
            panelTop.add(lPassword);
            panelTop.add(lNickname);
            panelTop.add(tfLogin);
            panelTop.add(tfPassword);
            panelTop.add(tfNickname);
            panelBottom.add(btnRegister);
            add(panelTop, BorderLayout.NORTH);
            add(panelBottom, BorderLayout.SOUTH);
            btnRegister.addActionListener(this);
        }

        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            Object src = actionEvent.getSource();
            if (src == btnRegister) {
                parent.tfLogin.setText(tfLogin.getText());
                parent.tfPassword.setText(tfPassword.getText());
                parent.nickname = tfNickname.getText();
                connect();
            }
        }
    }

    /**
     * Error Dialog Window
     */
    public class ErrorDialog extends JDialog {
        private static final int WIDTH = 600;
        private static final int HEIGHT = 100;
        private final JLabel errorMsg;
        private final JPanel panelTop = new JPanel(new GridLayout(1, 1));
        private final JPanel panelBottom = new JPanel(new GridLayout(1, 1));
        private final JButton btnOk=new JButton("Ok");

        public ErrorDialog(JFrame owner, String message) {
            super(owner, "Произошла ошибка", true);

            setLocationRelativeTo(null);
            setSize(WIDTH, HEIGHT);
            setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            Thread.setDefaultUncaughtExceptionHandler((Thread.UncaughtExceptionHandler) owner);
            errorMsg = new JLabel(message);
            panelTop.add(errorMsg);
            panelBottom.add(btnOk);
            add(panelTop, BorderLayout.NORTH);
            setVisible(true);
        }


    }
}
