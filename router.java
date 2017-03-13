import java.io.*;  
import java.net.*;  
import java.util.*;
import java.nio.*;

class router {

    private static int NBR_ROUTER = 5;

   

    public static void main(String argv[]) throws Exception {

        int id = Integer.parseInt(argv[0]);
        InetAddress nhost = InetAddress.getByName(argv[1]);
        int nport = Integer.parseInt(argv[2]);
        int rport = Integer.parseInt(argv[3]);

	PrintWriter log = new PrintWriter("router" + id + ".log");		
        ArrayList<pkt_LSPDU> db = new ArrayList<pkt_LSPDU>();

        route[] rib = new route[5];
        for(int i=0;i<5;i++){
			rib[i]=new route();
			if(i==id-1){
				rib[i].next_router=id;
				rib[i].cost=0;
			}
		}

        DatagramSocket socket = new DatagramSocket(rport);
        byte[] sendData = new byte[1024];
        byte[] rcvData = new byte[1024];

        //INIT
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(id);
        sendData = buffer.array();
        DatagramPacket send = new DatagramPacket(sendData, 4, nhost, nport);
        socket.send(send);
        
       	log.println("Send INIT");
		
        //get circuit_DB
        DatagramPacket receivepacket = new DatagramPacket(rcvData, rcvData.length);
        socket.receive(receivepacket);
        circuit_DB cdb = circuit_DB.parseUDPdata(rcvData);
        log.println("Receives circuit_DB");
        
        //update link database
        for (int i = 0; i < cdb.nbr_link; i++) {
            pkt_LSPDU pkt = new pkt_LSPDU(id, id, cdb.linkcost[i].link, cdb.linkcost[i].cost, -1);
            db.add(pkt);
        }

        //send HELLO
        for (int i = 0; i < cdb.nbr_link; i++) {
            pkt_HELLO hello = new pkt_HELLO(id, cdb.linkcost[i].link);
            sendData = hello.getUDPdata();
            DatagramPacket sendpacket = new DatagramPacket(sendData, 8, nhost, nport);
            socket.send(sendpacket);
            log.println("Send Hello: reciever " + hello.router_id
                    + ", link_id " + hello.link_id);
        }

        //recieve packets
        while (true) {
            DatagramPacket receivePacket = new DatagramPacket(rcvData, rcvData.length);
            socket.receive(receivePacket);

            if (receivePacket.getLength() == 8) {//recieve a HELLO

                pkt_HELLO rhello = pkt_HELLO.parseUDPdata(rcvData);
                log.println("Receives HELLO: sender " + rhello.router_id
                        + ", link_id " + rhello.link_id);
                //respond HELLO
                for (int i = 0; i < db.size(); i++) {
                    pkt_LSPDU send_LSPDU = new pkt_LSPDU(id, db.get(i).router_id, db.get(i).link_id, db.get(i).cost, rhello.link_id);
                    sendData = send_LSPDU.getUDPdata();
                    DatagramPacket sendPacket = new DatagramPacket(sendData, 20, nhost, nport);
                    socket.send(sendPacket);
                    log.println("Send LS PDU: sender " + send_LSPDU.sender
                            + ", router_id " + send_LSPDU.router_id + ", link_id "
                            + send_LSPDU.link_id + ", cost " + send_LSPDU.cost
                            + ", via " + send_LSPDU.via);
                }

            } else {//recieve a LSPDU
                pkt_LSPDU pkt = pkt_LSPDU.parseUDPdata(rcvData);
                log.println("Receives LS PDU: sender " + pkt.sender
                        + ", router_id " + pkt.router_id + ", link_id "
                        + pkt.link_id + ", cost " + pkt.cost + ", via " + pkt.via);
				
				//check if in the database
                int new_db = 1;//new data
                int link_to = -1;//the router_id in database have same link_id
                for (int i = 0; i < db.size(); i++) {
                    if (db.get(i).router_id == pkt.router_id && db.get(i).link_id == pkt.link_id) {
                        new_db = 0;
                        break;
                    }
                    if (db.get(i).link_id == pkt.link_id && db.get(i).router_id != pkt.router_id) {
                        link_to = db.get(i).router_id;
                    }
                }
                
                if (new_db == 1) {//if not in database 
					//add to database and send to other neighbors
                    db.add(pkt);
                    for (int i = 0; i < cdb.nbr_link; i++) {
                        if (cdb.linkcost[i].link != pkt.via) {
                            pkt_LSPDU send_LSPDU = new pkt_LSPDU(id, pkt.router_id,
                                    pkt.link_id, pkt.cost, cdb.linkcost[i].link);
                            sendData = send_LSPDU.getUDPdata();
                            DatagramPacket sendPacket = new DatagramPacket(sendData, 20, nhost, nport);
                            socket.send(sendPacket);
                            log.println("Send LS PDU: sender " + send_LSPDU.sender
                                    + ", router_id " + send_LSPDU.router_id + ", link_id "
                                    + send_LSPDU.link_id + ", cost " + send_LSPDU.cost
                                    + ", via " + send_LSPDU.via);
                        }
                    }
                    if (link_to > 0) {//new link to that router_id
						//calculate the cost after use new link
                        int newc = rib[link_to - 1].cost + pkt.cost;
                        //if cost lower, update 
                        if (newc < rib[pkt.router_id - 1].cost) {
                            rib[pkt.router_id - 1].cost = newc;
                            rib[pkt.router_id - 1].next_router = pkt.sender;
                        }
                    }
                    print_db(db, rib, id, log);
                }
            }
            log.flush();
        }
           
    }
    
     public static void print_db(ArrayList<pkt_LSPDU> db, route[] rib, int id, PrintWriter log) throws Exception {

        log.println("# Topology database");
        for (int i = 1; i <= 5; i++) {
            ArrayList<pkt_LSPDU> tdb = new ArrayList<pkt_LSPDU>();
            for (int j = 0; j < db.size(); j++) {
                if (db.get(j).router_id == i) {
                    tdb.add(db.get(j));
                }
            }
            log.println("R" + id + " -> R" + i + " nbr link " + tdb.size());
            for (int j = 0; j < tdb.size(); j++) {
                log.println("R" + id + " -> R" + i + " link " + tdb.get(j).link_id + " cost " + tdb.get(j).cost);
            }
        }

        log.println("# RIB");
        for (int i = 0; i < 5; i++) {

            if (i == id - 1) {
                log.println("R" + id + " -> R" + id + " -> Local, 0");

            } else {
                if (rib[i].next_router > 0) {
                    log.println("R" + id + " -> R" + (i + 1) + " -> R" + rib[i].next_router + ", " + rib[i].cost);
                } else {
                    log.println("R" + id + " -> R" + (i + 1) + " no connection");
                }
            }
        }

    }

}

class pkt_HELLO {

    public int router_id; /* id of the router who sends the HELLO PDU */
    public int link_id; /* id of the link through which it is sent */


    public pkt_HELLO(int router, int link) {
        router_id = router;
        link_id = link;
    }

    public byte[] getUDPdata() {
        ByteBuffer buffer = ByteBuffer.allocate(512);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(router_id);
        buffer.putInt(link_id);
        return buffer.array();
    }

    public static pkt_HELLO parseUDPdata(byte[] UDPdata) throws Exception {
        ByteBuffer buffer = ByteBuffer.wrap(UDPdata);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        int router_id = buffer.getInt();
        int link_id = buffer.getInt();
        return new pkt_HELLO(router_id, link_id);
    }
};

class pkt_LSPDU {

    public int sender; /* sender of the LS PDU */
    public int router_id; /* router id */
    public int link_id; /* link id */
    public int cost; /* cost of the link */
    public int via; /* id of the link through which the LS PDU is sent */


    public pkt_LSPDU(int Sender, int router, int link, int Cost, int Via) {
        router_id = router;
        sender = Sender;
        router_id = router;
        link_id = link;
        cost = Cost;
        via = Via;
    }

    public byte[] getUDPdata() {
        ByteBuffer buffer = ByteBuffer.allocate(512);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(sender);
        buffer.putInt(router_id);
        buffer.putInt(link_id);
        buffer.putInt(cost);
        buffer.putInt(via);
        return buffer.array();
    }

    public static pkt_LSPDU parseUDPdata(byte[] UDPdata) throws Exception {
        ByteBuffer buffer = ByteBuffer.wrap(UDPdata);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        int sender = buffer.getInt();
        int router_id = buffer.getInt();
        int link_id = buffer.getInt();
        int cost = buffer.getInt();
        int via = buffer.getInt();
        return new pkt_LSPDU(sender, router_id, link_id, cost, via);
    }
};

class pkt_INIT {

    public int router_id; /* id of the router that sends the INIT PDU */

};

class link_cost {

    public int link; /* link id */

    public int cost; /* associated cost */


    public link_cost(int Link, int Cost) {
        link = Link;
        cost = Cost;
    }

};

class circuit_DB {

    public int nbr_link; /* number of links attached to a router */

    public link_cost[] linkcost = new link_cost[5];
    /* we assume that at most NBR_ROUTER links are attached to each router */

    public static circuit_DB parseUDPdata(byte[] UDPdata) throws Exception {
        circuit_DB cdb = new circuit_DB();
        ByteBuffer buffer = ByteBuffer.wrap(UDPdata);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        cdb.nbr_link = buffer.getInt();
        for (int i = 0; i < cdb.nbr_link; i++) {
            int link = buffer.getInt();
            int cost = buffer.getInt();
            cdb.linkcost[i] = new link_cost(link, cost);
        }
        return cdb;
    }

};

class route {

    public int next_router = -1;//next router should go
    public int cost = Integer.MAX_VALUE/2;//the lowest cost

};
