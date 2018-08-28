import java.io.* ;
import java.net.* ;
import java.util.* ;
import java.awt.* ;

public class TheMajesticServer {
    public static Vector<Socket> ClientSockets;
    public static Vector<String> LoginNames;
    public static Vector<Chatroom> Chatrooms;
    public static Map<String,Chatroom> ConnectedChatroom;
    public static int max_no_clients;
    public static Vector<Integer> Ports;
    public static DatagramSocket SocUDP;
    TheMajesticServer(int max_no_clients_) {
        try {
            System.out.println("Server running on localhost Port-6666(TCP), 6661(UDP)");
            ServerSocket Soc = new ServerSocket(6666) ;
            DatagramSocket SocUDP = new DatagramSocket(6661);
            ClientSockets = new Vector<Socket>() ; LoginNames = new Vector<String>() ; Chatrooms = new Vector<Chatroom>() ; ConnectedChatroom = new HashMap<String,Chatroom>(); max_no_clients=max_no_clients_; Ports = new Vector<Integer>();
            while(true)
            {
                Socket CSoc = Soc.accept();
                AcceptClient client_ = new AcceptClient(CSoc,SocUDP) ;
            }
        }
        catch(Exception e) {e.printStackTrace(System.out);System.exit(0);}
    }
    public static void main(String args[]) throws Exception {
        if(args.length==0)
        {
            System.out.println("Maximum number of Users for the Server not given."); System.exit(0);
        }
        TheMajesticServer server = new TheMajesticServer(Integer.parseInt(args[0])) ;
    }
}

class AcceptClient extends Thread {
    Socket ClientSocket; DataInputStream din ; DataOutputStream dout ; String LoginName; DatagramSocket SocUDP;
    AcceptClient (Socket CSoc, DatagramSocket SocUDP_) throws Exception {
        ClientSocket = CSoc ; din = new DataInputStream(ClientSocket.getInputStream()) ; dout = new DataOutputStream(ClientSocket.getOutputStream()) ;
        SocUDP=SocUDP_;
        byte[] intial = new byte[1000];
        DatagramPacket recieve_inital = new DatagramPacket(intial, intial.length);
        SocUDP.receive(recieve_inital);
        LoginName = din.readUTF() ;
        if(TheMajesticServer.LoginNames.size()==TheMajesticServer.max_no_clients)
        {
            System.out.println("Cannot login user: Server's maximum limit reached");
            dout.writeUTF("Cannot connect: Reached Server's maximum capacity");
            ClientSocket.close() ; din.close() ; dout.close() ; return;
        }
        System.out.println("User "+LoginName+" logged in");
        int port = recieve_inital.getPort();
        TheMajesticServer.Ports.add(port);
        TheMajesticServer.LoginNames.add(LoginName) ; TheMajesticServer.ClientSockets.add(ClientSocket) ; TheMajesticServer.ConnectedChatroom.put(LoginName,null);
        start() ;
    }
    public void run() {
        while(true)
        {
            try
            {
                String commandfromClient = new String() ;
                commandfromClient = din.readUTF() ;
                StringTokenizer tokenedcommand = new StringTokenizer(commandfromClient);
                String command=tokenedcommand.nextToken();
                if(command.equals("LOGOUT"))
                {
                    Chatroom C=TheMajesticServer.ConnectedChatroom.get(LoginName);
                    if(C!=null)
                    {
                        String outp=TheMajesticServer.ConnectedChatroom.get(LoginName).Leave(LoginName);
                        if(outp.equals("DEL"))
                        {
                            TheMajesticServer.Chatrooms.remove(C);
                        }
                        else dout.writeUTF(outp);
                        ClientSocket.close(); din.close(); dout.close();
                        if(TheMajesticServer.Chatrooms.contains(C)) C.Notify(LoginName+" left the chatroom",LoginName);
                        C=null;
                    }
                    TheMajesticServer.LoginNames.remove(LoginName) ; TheMajesticServer.ClientSockets.remove(ClientSocket) ;
                }
                if(command.equals("create"))
                {
                    if(TheMajesticServer.ConnectedChatroom.get(LoginName)!=null) dout.writeUTF("You are already part of chatroom "+TheMajesticServer.ConnectedChatroom.get(LoginName).name);
                    else
                    {
                        String chatroomName=tokenedcommand.nextToken(); chatroomName=tokenedcommand.nextToken();
                        Chatroom chatR = new Chatroom(chatroomName, LoginName);
                        TheMajesticServer.Chatrooms.add(chatR);
                        dout.writeUTF("Chatroom "+chatroomName+" created\nYou are in chatroom "+chatroomName);
                    }
                }
                else if(command.equals("list"))
                {
                    String nxtcomm=tokenedcommand.nextToken();
                    if(nxtcomm.equals("chatrooms"))
                    {
                        String outp="";
                        if(TheMajesticServer.Chatrooms.size()==0) dout.writeUTF("No Chatrooms exist");
                        else
                        {
                            for(int i=0;i<TheMajesticServer.Chatrooms.size();i++) outp=outp+TheMajesticServer.Chatrooms.elementAt(i).name+"\n";
                            dout.writeUTF(outp);
                        }
                    }
                    else if(nxtcomm.equals("users"))
                    {
                        if(TheMajesticServer.ConnectedChatroom.get(LoginName)==null) dout.writeUTF("You are not part of any chatroom");
                        else
                        {
                            Vector<String> outpl=TheMajesticServer.ConnectedChatroom.get(LoginName).ListUsers();
                            String outp="";
                            for(int i=0;i<outpl.size();i++)outp=outp+outpl.elementAt(i)+"\n";
                            dout.writeUTF(outp);
                        }
                    }
                    else {dout.writeUTF("Unrecognised Command");}
                }
                else if(command.equals("join"))
                {
                    String chatroomName=tokenedcommand.nextToken();
                    if(TheMajesticServer.ConnectedChatroom.get(LoginName)!=null) dout.writeUTF("You are already part of chatroom "+TheMajesticServer.ConnectedChatroom.get(LoginName));
                    else
                    {
                        int i=0;
                        for(i=0;i<TheMajesticServer.Chatrooms.size();i++) if(TheMajesticServer.Chatrooms.elementAt(i).name.equals(chatroomName))
                        {
                            String outp=TheMajesticServer.Chatrooms.elementAt(i).Join(LoginName);
                            dout.writeUTF(outp); TheMajesticServer.Chatrooms.elementAt(i).Notify(LoginName+" joined the chatroom",LoginName); break;
                        }
                        if(i==TheMajesticServer.Chatrooms.size()) dout.writeUTF(chatroomName+" doesn't exist");
                    }
                }
                else if(command.equals("leave"))
                {
                    if(TheMajesticServer.ConnectedChatroom.get(LoginName)==null) dout.writeUTF("You are not part of any chatroom");
                    else
                    {
                        Chatroom c = TheMajesticServer.ConnectedChatroom.get(LoginName);
                        String name_=c.name;
                        String outp = TheMajesticServer.ConnectedChatroom.get(LoginName).Leave(LoginName);
                        c.Notify(LoginName+" left the chatroom",LoginName);
                        if(outp.equals("DEL"))
                        {
                            TheMajesticServer.Chatrooms.remove(c); c=null;
                            dout.writeUTF("You left Chatroom "+name_+'\n'+name_+" deleted");
                        }
                        else dout.writeUTF(outp);
                    }
                }
                else if(command.equals("add"))
                {
                    String user = tokenedcommand.nextToken();
                    if(TheMajesticServer.ConnectedChatroom.get(LoginName)==null) dout.writeUTF("You are not a part of any chatroom");
                    else
                    {
                        String outp = TheMajesticServer.ConnectedChatroom.get(LoginName).Add(user);
                        if(!outp.contains("Connot"))
                            TheMajesticServer.ConnectedChatroom.get(LoginName).Notify(LoginName+" added "+user+" to chatroom "+TheMajesticServer.ConnectedChatroom.get(LoginName).name,LoginName);
                        dout.writeUTF(outp);
                    }
                }
                else if(command.equals("reply"))
                {
                    StringTokenizer st = new StringTokenizer(commandfromClient);
                    String cmd=st.nextToken(),fl,tp;
                    boolean isFile=false;
                    if(st.hasMoreTokens())
                    {
                        fl=st.nextToken();
                        if(st.hasMoreTokens())
                        {
                            tp=st.nextToken();
                            if(tp.equals("tcp"))
                            {
                                isFile=true;
                                //File transfer
                                Chatroom C = TheMajesticServer.ConnectedChatroom.get(LoginName);
                                if(C==null) dout.writeUTF("You are not part of any chatroom");
                                else
                                {
                                    String st_ = din.readUTF(); StringTokenizer stt = new StringTokenizer(st_); stt.nextToken() ; int fileLength = Integer.parseInt(stt.nextToken());
                                    StringTokenizer fileName = new StringTokenizer(fl,"/"); while(fileName.hasMoreTokens())fl=fileName.nextToken();
                                    C.Notify("FILE "+fl+" TCP  LENGTH "+fileLength,LoginName);
                                    byte[] file_contents = new byte[1000];
                                    int bytesRead=0,size=1000;
                                    if(size>fileLength)size=fileLength;
                                    while((bytesRead=din.read(file_contents,0,size))!=-1 && fileLength>0)
                                    {
                                        for(int i=0;i<C.Members.size();i++)
                                        {
                                            if(!C.Members.elementAt(i).equals(LoginName))
                                            {
                                                DataOutputStream senddout = new DataOutputStream(TheMajesticServer.ClientSockets.elementAt(TheMajesticServer.LoginNames.indexOf(C.Members.elementAt(i))).getOutputStream());
                                                senddout.write(file_contents,0,size);
                                            }
                                        }
                                        fileLength-=size; if(size>fileLength) size=fileLength;
                                    }
                                    System.out.println("Sent");
                                }
                            }
                            else if(tp.equals("udp"))
                            {
                                isFile=true;
                                //File transfer
                                Chatroom C = TheMajesticServer.ConnectedChatroom.get(LoginName);
                                if(C==null) dout.writeUTF("You are not part of any chatroom");
                                else
                                {
                                    String st_ = din.readUTF(); StringTokenizer stt = new StringTokenizer(st_); stt.nextToken() ; int fileLength = Integer.parseInt(stt.nextToken());
                                    StringTokenizer fileName = new StringTokenizer(fl,"/"); while(fileName.hasMoreTokens())fl=fileName.nextToken();
                                    C.Notify("FILE "+fl+" UDP LENGTH "+fileLength,LoginName);
                                    int size = 1024;
                                    byte[] file_contents = new byte[size];
                                    if(size>fileLength)size=fileLength;
                                    //System.out.println(fileLength);
                                    DatagramPacket packetUDP;
                                    while(fileLength>0)
                                    {
                                        packetUDP = new DatagramPacket(file_contents,size);
                                        SocUDP.receive(packetUDP);
                                        for(int i=0;i<C.Members.size();i++)
                                        {
                                            if(!C.Members.elementAt(i).equals(LoginName))
                                            {
                                                packetUDP = new DatagramPacket(file_contents,size,InetAddress.getByName("127.0.0.1"),Integer.parseInt((TheMajesticServer.Ports.elementAt(TheMajesticServer.LoginNames.indexOf(C.Members.elementAt(i)))).toString()));
                                                SocUDP.send(packetUDP);
                                            }
                                        }
                                        fileLength-=size; if(size>fileLength) size=fileLength;
                                    }
                                }
                            }
                        }
                    }
                    if(isFile==false)
                    {
                        String msgfromClient=LoginName+":";
                        Chatroom C = TheMajesticServer.ConnectedChatroom.get(LoginName);
                        while(tokenedcommand.hasMoreTokens()) msgfromClient=msgfromClient+" "+tokenedcommand.nextToken();
                        if(C==null) dout.writeUTF("You are not part of any chatroom");
                        else C.Notify(msgfromClient,LoginName);
                    }
                }
                else
                {
                    dout.writeUTF("Unrecognised command");
                }
            }
            catch(Exception e) {
                e.printStackTrace(System.out) ; break;
            }
        }
    }
}

class Chatroom {
    Vector<String> Members = new Vector<String>();
    String name;
    Chatroom (String name,String member) {
        this.name = name;
        this.Members.add(member);
        TheMajesticServer.ConnectedChatroom.put(member,this);
    }
    public String Join (String member) {
        this.Members.add(member);
        TheMajesticServer.ConnectedChatroom.put(member,this);
        return ("Joined Chatroom "+this.name);
    }
    public String Leave (String member) {
        this.Members.remove(member);
        TheMajesticServer.ConnectedChatroom.put(member,null);
        if(this.Members.isEmpty()) return ("DEL");
        else return("You left chatroom "+this.name);
    }
    public Vector<String> ListUsers() {
        return this.Members;
    }
    public String Add(String memberAdd) {
        if(this.Members.contains(memberAdd)) return(memberAdd+" is already a part of "+this.name);
        if(!TheMajesticServer.LoginNames.contains(memberAdd)) return("The username "+memberAdd+" doesn't exist");
        for(int c=0; c<TheMajesticServer.Chatrooms.size();c++)
        {
            Chatroom C = TheMajesticServer.Chatrooms.elementAt(c);
            if(C.Members.contains(memberAdd)) return("Cannot add "+memberAdd+" to chatroom "+this.name+"\n"+memberAdd+" already a part of chatroom "+C.name);
        }
        this.Members.add(memberAdd);
        TheMajesticServer.ConnectedChatroom.put(memberAdd,this);
        return(memberAdd+" added to chatroom "+this.name);
    }
    public void Notify(String msg,String no_notif) {
        for(int i=0;i<this.Members.size();i++)
        {
            if(!this.Members.elementAt(i).equals(no_notif))
            {
                try {
                    Socket sendSoc = TheMajesticServer.ClientSockets.elementAt(TheMajesticServer.LoginNames.indexOf(this.Members.elementAt(i)));
                    DataOutputStream senddout = new DataOutputStream(sendSoc.getOutputStream());
                    senddout.writeUTF(msg);
                }
                catch(Exception e){ int ii=0;  }
            }
        }
    }
}
