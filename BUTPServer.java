//Server

package butp;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Random;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

/**
 *
 * @author Haider Rehman, Varaynya Chivukula
 */

public class BUTPServer {

	private static final long serialVersionUID = 1L;
        
        private DatagramPacket RecievedPac;
	private DatagramSocket ServerSoc;
	private InetAddress host;
        //Defining Port Number
        int port = 8888;
        
        int OpCode;
	int RandomCongestion = new Random().nextInt(9)+1;
	int CheckSumLen = 0;
	int WindowSize = 1024;
        int packetNumber = 1;

        
	String ReceivedData = "";
	String FileContent = "";
	String[] SplittedStr;
	String FilenameForServer;
	String Responce;
        
	
	double SequenceNo;
	long Checksum_Identifier;
	long checksum;
        
        
        //CRC32Checksum
        
        public long CRC32Checksum(String checksumData) {
		CheckSumLen = checksumData.length();
		byte tempBuffer[] = checksumData.getBytes();
		Checksum checksumEngine = new CRC32();
		checksumEngine.update(tempBuffer, 0, tempBuffer.length);
		return checksumEngine.getValue();
	}

        //Writing the Data Recieved from client to new file.
        
	public void writefile(String tmpPackdata) throws IOException {
		File serverfile = new File(FilenameForServer);
		try {
			FileWriter filewriter = new FileWriter(serverfile);
                        
			int textsize = tmpPackdata.length();
			
                        filewriter.write(tmpPackdata, 0, textsize);
			
                        
                        filewriter.flush();
			filewriter.close();
			String msg = "\nFile Written at location : "+ serverfile.getCanonicalPath()+"\n";
                        
                        
                        System.out.println (msg);
                        
		} catch (IOException exc) {
                        
                    
                    System.out.println (exc.toString());
                }
	}


	public BUTPServer() {
		
		
		try {
			ServerSoc = new DatagramSocket(port);
                        
			String msg = "Server Running On Port :" +port+ "\n";
			
                        System.out.println (msg);
                        
                        while (true) {
			    
                            //Recieving Data From Client.
                            
                            byte[] receiveData = new byte[WindowSize];
                            RecievedPac = new DatagramPacket(receiveData, receiveData.length);
                            ServerSoc.receive(RecievedPac);
                            host = RecievedPac.getAddress();
                            String DataFromClient = new String(RecievedPac.getData());
                            
                            //Spliting the Message Sent from Client.
                            SplittedStr = new String[4];
                            
                            SplittedStr = DataFromClient.split("@_@");
                            
                            
                            
                            //Chunk Format
                            OpCode = Integer.parseInt(SplittedStr[0]);
                            ReceivedData = SplittedStr[1];
                            Checksum_Identifier = Long.parseLong(SplittedStr[2]);
                            SequenceNo = Double.parseDouble(SplittedStr[3]);
                            
                            
                            if (OpCode == StandardConstants.WRQ) {
                                    
					FilenameForServer = ReceivedData; 
					msg = host+" Sender Sending file: " + ReceivedData+ "\n";
					System.out.println (msg);
                                        
                                        packetNumber = 1;
					
				} else {
					checksum = CRC32Checksum(ReceivedData);
                                        
					if (checksum != Checksum_Identifier) {
                                            
						msg = "ERROR : Droping Packet Number '"+packetNumber+"' due to checksum error. \n";
						Responce = StandardConstants.NAK+"";
					
                                                System.out.println (msg);
                                                
                                        } else if (RandomCongestion == SequenceNo) {
						msg = "ERROR : Droping Packet Number '"+packetNumber+"' due to congestion \n";
						
                                                System.out.println (msg);
                                                
						Responce = StandardConstants.NAK+"_congestion_"+(new Random().nextInt(1024 - 100)+100);
						RandomCongestion += new Random().nextInt(10);
					} else if (packetNumber != SequenceNo) {
						msg = "ERROR : Droping Packet Number '"+packetNumber+"' due to unexpected sequence number \n";
						
                                                System.out.println (msg);
                                                
						Responce = StandardConstants.NAK+"";
					} else {
						Responce = StandardConstants.ACK+"";;
						FileContent += ReceivedData;
                                                
                                                msg = "\n Packet Number: "+SequenceNo+" Recieved Successfully \n";
                                                System.out.println (msg);
                                                
                                                packetNumber++;
                                                
                                                //Write The Content To new File.
                                                writefile(FileContent);
					}

                                        //Sending Responce to Client.
                                        
                                        byte[] buf = Responce.getBytes();
                                        host = RecievedPac.getAddress();
                                        port = RecievedPac.getPort();
                                        RecievedPac = new DatagramPacket(buf, buf.length, host, port);
                                        ServerSoc.send(RecievedPac);
                            }
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String args[]) {
		new BUTPServer();
	}
}

