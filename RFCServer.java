import java.io.*;
import java.net.*;
import java.util.concurrent.*;

/*
    The RFC server at a peer listens on a port specific to the peer; in other words,
    this port is not known in advance to any of the peers. The RFC server at each peer
    must be able to handle multiple simultaneous connections for downloads (of the RFC
    index or an RFC document) by remote peers. To this end, it has a main thread that
    listens to the peer-specific port. When a connection from a remote peer is received,
    the main thread spawns a new thread that handles the downloading for this remote peer;
    the main thread then returns to listening for other connection requests. Once the
    downloading is complete, this new thread terminates.
 */
public class RFCServer {
    private static String localRFC(String s){
        StringBuilder sb = new StringBuilder();
        int RFCNum = 0;
        for(Peer.RFCIndexRecord r: Peer.rfcIndexRecordList){
            RFCNum++;
            sb.append(r.rfcNumber).append(" ");
        }
        return RFCNum+" "+sb.toString();
    }
    private RFCServer(int port){
        final ExecutorService clientProcessingPool = Executors.newFixedThreadPool(10);
        Runnable serverTask = () -> {
            try {
                ServerSocket server = new ServerSocket(port);
                System.out.println("Server started");
                System.out.println("Waiting for a peer ...");
                while (true) {
                    Socket socket = server.accept();
                    clientProcessingPool.submit(new ClientTask(socket));
                }
            } catch (IOException e) {
                System.err.println("Unable to process client request");
                e.printStackTrace();
            }
        };
        Thread serverThread = new Thread(serverTask);
        serverThread.start();
    }

    private static class ClientTask implements Runnable {
        private final Socket clientSocket;

        private ClientTask(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            System.out.println("Got a client !");

            //check if multi-thread works
            Thread t = Thread.currentThread();
            System.out.println("WorkerThread details : Name - "+ t.getName());
            System.out.println("Thread Id of Worker Thread - " + t.getId());

            try
            {
                // takes input from the client socket
                try (DataInputStream in = new DataInputStream(
                        new BufferedInputStream(clientSocket.getInputStream()))) {
                    DataOutputStream outToPeer = new DataOutputStream(clientSocket.getOutputStream());
                    String line = "";
                    line = in.readUTF();
                    // if RFC index is requested
                    // sample msg received may be: RFCQuery
                    if (line.startsWith("RFCQuery")) {
                        // handle RFCQuery
                        System.out.println("RFCQuery");
                        outToPeer.writeUTF("RFCs:" +localRFC(line));
                    }
                    // if a certain RFC document is requested
                    // sample msg received may be: GetRFC 8649(GetRFC docNum)
                    if (line.startsWith("GetRFC")) {
                        // handle RFCQuery
                        System.out.println("GetRFC");
                    }
                    outToPeer.writeUTF("RFCServer received: " + line);
                } catch (IOException i) {
                    System.out.println(i);
                } finally {
                    // close connection
                    System.out.println("close connection");
                    clientSocket.close();
                }
            }
            catch(IOException i)
            {
                System.out.println(i);
            }

//            try {
//                clientSocket.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
        }
    }

    public static void main(String[] args){
        //for testing
        Peer.RFCIndexRecord x= new Peer.RFCIndexRecord();
        x.rfcNumber=8649;
        x.rfcTitle="8649";
        x.rfcHost="1";

        Peer.RFCIndexRecord y= new Peer.RFCIndexRecord();
        y.rfcNumber=8650;
        y.rfcTitle="8650";
        y.rfcHost="2";

        Peer.rfcIndexRecordList.add(x);
        Peer.rfcIndexRecordList.add(y);

        RFCServer server = new RFCServer(65424);
//        new RFCServer().startServer();
    }
}
