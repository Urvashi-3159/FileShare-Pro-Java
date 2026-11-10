package com.fileshare;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.nio.file.Path;

public class ClientGUI extends JFrame {
    private final JTextField host=new JTextField("127.0.0.1",10), port=new JTextField("5000",4), username=new JTextField(10);
    private final JPasswordField password=new JPasswordField(10);
    private final DefaultListModel<FileClient.RemoteFile> model=new DefaultListModel<>();
    private final JList<FileClient.RemoteFile> list=new JList<>(model);
    private final JLabel status=new JLabel("Register a new account or log in.");
    private final JProgressBar bar=new JProgressBar(0,100);
    private boolean loggedIn=false;

    public ClientGUI(){
        super("FileShare Pro - Secure Account File Sharing");
        setDefaultCloseOperation(EXIT_ON_CLOSE);setSize(760,520);setLocationRelativeTo(null);
        JPanel root=new JPanel(new BorderLayout(10,10));root.setBorder(new EmptyBorder(14,14,14,14));setContentPane(root);
        JLabel title=new JLabel("FILESHARE PRO");title.setFont(new Font("SansSerif",Font.BOLD,24));
        JLabel sub=new JLabel("Java TCP Networking • Upload • Download • Personal file space");
        JPanel head=new JPanel(new GridLayout(2,1));head.add(title);head.add(sub);root.add(head,BorderLayout.NORTH);
        JPanel center=new JPanel(new BorderLayout(8,8));
        JPanel login=new JPanel(new FlowLayout(FlowLayout.LEFT));
        login.add(new JLabel("Server"));login.add(host);login.add(new JLabel("Port"));login.add(port);
        login.add(new JLabel("Username"));login.add(username);login.add(new JLabel("Password"));login.add(password);
        JButton register=new JButton("Register"), signIn=new JButton("Login"), refresh=new JButton("Refresh");
        login.add(register);login.add(signIn);login.add(refresh);center.add(login,BorderLayout.NORTH);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);center.add(new JScrollPane(list),BorderLayout.CENTER);
        root.add(center,BorderLayout.CENTER);
        JPanel bottom=new JPanel(new BorderLayout(8,8));JPanel actions=new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton upload=new JButton("Upload"),download=new JButton("Download"),delete=new JButton("Delete Selected");
        actions.add(upload);actions.add(download);actions.add(delete);bottom.add(actions,BorderLayout.NORTH);
        bar.setStringPainted(true);bar.setString("Ready");bottom.add(bar,BorderLayout.CENTER);bottom.add(status,BorderLayout.SOUTH);root.add(bottom,BorderLayout.SOUTH);
        register.addActionListener(e->authenticate("REGISTER"));signIn.addActionListener(e->authenticate("LOGIN"));
        refresh.addActionListener(e->refreshFiles());upload.addActionListener(e->upload());download.addActionListener(e->download());delete.addActionListener(e->delete());
        setActions(false);
    }
    private FileClient client(){try{return new FileClient(host.getText().trim(),Integer.parseInt(port.getText().trim()));}catch(Exception e){throw new IllegalArgumentException("Enter a valid server IP and port.");}}
    private String user(){return username.getText().trim();} private String pass(){return new String(password.getPassword());}
    private void authenticate(String action){
        work("Connecting...",()->{String msg=client().auth(action,user(),pass());loggedIn=true;SwingUtilities.invokeLater(()->{status.setText(msg);setActions(true);refreshFiles();});});
    }
    private void refreshFiles(){
        if(!loggedIn){status.setText("Please log in first.");return;}
        work("Refreshing files...",()->{var arr=client().list(user(), pass());SwingUtilities.invokeLater(()->{model.clear();for(var f:arr)model.addElement(f);status.setText(arr.length+" file(s) in your account.");});});
    }
    private void upload(){
        if(!loggedIn)return;JFileChooser c=new JFileChooser();if(c.showOpenDialog(this)!=JFileChooser.APPROVE_OPTION)return;Path p=c.getSelectedFile().toPath();
        setBusy(true);new SwingWorker<String,Integer>(){
            protected String doInBackground()throws Exception{return client().transferUpload(user(),pass(),p,(d,t)->publish(t==0?100:(int)(d*100/t)));}
            protected void process(java.util.List<Integer>x){updateBar(x.get(x.size()-1),"Uploading");}
            protected void done(){try{status.setText(get());refreshFiles();}catch(Exception e){error(e);}finally{setBusy(false);}}
        }.execute();
    }
    private void download(){
        var f=list.getSelectedValue();if(!loggedIn||f==null){status.setText("Select a file first.");return;}
        JFileChooser c=new JFileChooser();c.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);c.setDialogTitle("Choose download folder");if(c.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION)return;Path folder=c.getSelectedFile().toPath();
        setBusy(true);new SwingWorker<Path,Integer>(){
            protected Path doInBackground()throws Exception{return client().transferDownload(user(),pass(),f,folder,(d,t)->publish(t==0?100:(int)(d*100/t)));}
            protected void process(java.util.List<Integer>x){updateBar(x.get(x.size()-1),"Downloading");}
            protected void done(){try{status.setText("Downloaded: "+get());}catch(Exception e){error(e);}finally{setBusy(false);}}
        }.execute();
    }
    private void delete(){
        var f=list.getSelectedValue();if(!loggedIn||f==null)return;
        if(JOptionPane.showConfirmDialog(this,"Delete "+f.name()+" from your server storage?","Confirm delete",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
        work("Deleting...",()->{client().delete(user(),pass(),f.name());SwingUtilities.invokeLater(()->{status.setText("File deleted.");refreshFiles();});});
    }
    private void work(String msg,Task task){setBusy(true);status.setText(msg);new SwingWorker<Void,Void>(){Exception ex;protected Void doInBackground(){try{task.run();}catch(Exception e){ex=e;}return null;}protected void done(){if(ex!=null)error(ex);setBusy(false);}}.execute();}
    private void error(Exception e){Throwable t=e;while(t.getCause()!=null)t=t.getCause();String m=t.getMessage()==null?t.toString():t.getMessage();status.setText("Error: "+m);JOptionPane.showMessageDialog(this,m,"FileShare Pro",JOptionPane.ERROR_MESSAGE);}
    private void updateBar(int n,String label){bar.setValue(n);bar.setString(label+": "+n+"%");}
    private void setActions(boolean enabled){list.setEnabled(enabled);}
    private void setBusy(boolean b){for(Component c:((JPanel)((JPanel)getContentPane()).getComponent(1)).getComponents())c.setEnabled(!b);}
    @FunctionalInterface interface Task{void run()throws Exception;}
    public static void main(String[] args){SwingUtilities.invokeLater(()->new ClientGUI().setVisible(true));}
}
