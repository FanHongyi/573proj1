import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class RegistrationServer {
    
    static class PeerRecord{
        String hostname;
        int cookie;
        boolean flag;
        int TTL = 7200;
        int portNum; // only valid for active peers
        int timesNum; // peer active count
        String mostRecentTime;
    }
    
    private static LinkedList<PeerRecord> peerList = new LinkedList<>();
    
    private void peerRegister(String s){
        String[] params = s.split(" ");
        String h = params[1];
        int port = Integer.parseInt(params[2]);

        boolean registered = false;
        for(PeerRecord p: peerList){
            if(p.hostname.equals(h)){
                System.out.println(h+" already registered, cookie: "+ p.cookie);
                registered = true;
                p.flag = true;
                p.TTL = 7200;
                p.timesNum++;
                p.mostRecentTime = java.time.LocalDateTime.now().toString();
            }
        }
        if(!registered){
            PeerRecord newPeer = new PeerRecord();
            newPeer.hostname = h;
            newPeer.cookie = peerList.size();
            newPeer.flag = true;
            newPeer.TTL = 7200;
            newPeer.portNum = port;
            newPeer.timesNum = 1;
            newPeer.mostRecentTime = java.time.LocalDateTime.now().toString();
            peerList.add(newPeer);
        }
    }

    private void peerLeave(String s){
        String[] params = s.split(" ");
        String h = params[1];

        for(PeerRecord p: peerList){
            if(p.hostname.equals(h)){
                p.flag = false;
                p.portNum = -1;
            }
        }
    }

    private String peerQuery(String s){
        String[] params = s.split(" ");
        String h = params[1];
        StringBuilder activePeers = new StringBuilder();
        int peerNum = 0;
        for(PeerRecord p: peerList){
            if(p.hostname.equals(h)){
                p.TTL = 7200;
            }
            if(p.flag && !p.hostname.equals(h)){
                activePeers.append(p.hostname).append(" ").append(p.portNum).append(" ");
                peerNum++;
            }
        }
        return peerNum+" "+activePeers.toString();
    }

    private void peerKeepAlive(String s){
        String[] params = s.split(" ");
        String h = params[1];

        for(PeerRecord p: peerList){
            if(p.hostname.equals(h)){
                p.TTL = 7200;
            }
        }
    }

    // constructor with port
    private RegistrationServer(int port)
    {
        // starts server and waits for a connection
        try
        {
            ServerSocket server = new ServerSocket(port);
            System.out.println("Server started");

            System.out.println("Waiting for a client ...");

            while (true)
            {
                //initialize socket and input stream
                Socket socket = server.accept();
                System.out.println("Client accepted");

                // takes input from the client socket
                DataInputStream in = new DataInputStream(
                        new BufferedInputStream(socket.getInputStream()));
                DataOutputStream outToPeer=new DataOutputStream(socket.getOutputStream());
                String line = "";
                try
                {
                    line = in.readUTF();
                    //register
                    //sample message: Register peer0 65423
                    if(line.startsWith("Register")){
                        peerRegister(line);
                    }
                    //leave
                    //sample message: Leave peer0
                    if(line.startsWith("Leave")){
                        peerLeave(line);
                    }
                    //PQuery
                    //sample message: PQuery peer0
                    if(line.startsWith("PQuery")){
                        String activePeers = peerQuery(line);
                        //sample output: ActivePeers 2 peer0 65423 peer1 65424
                        outToPeer.writeUTF("ActivePeers "+activePeers);
                    }
                    //KeepAlive
                    //sample message: KeepAlive peer0
                    if(line.startsWith("KeepAlive")){
                        peerKeepAlive(line);
                    }
                    outToPeer.writeUTF("RS received: "+line);
                }
                catch(IOException i)
                {
                    System.out.println(i);
                }finally {
                    // close connection
                    System.out.println("close connection");
                    socket.close();
                    in.close();
                }
            }
        }
        catch(IOException i)
        {
            System.out.println(i);
        }
    }

    public static void main(String[] args)
    {
        // decrement TTL and flag peer inactive
        // peer deactivated in not contacted within 7200 seconds
        Runnable helloRunnable = () -> {
            for(PeerRecord p: peerList){
                if(p.TTL!=0)
                    p.TTL--;
                if(p.TTL==0){
                    p.flag = false;
                    System.out.println(p.hostname +" inactivated.");
                }
            }
        };
        
        // scheduled to execute TTL decrement every second 
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(helloRunnable, 0, 1, TimeUnit.SECONDS);

        // port registration
        RegistrationServer server = new RegistrationServer(65423);
    }
}
