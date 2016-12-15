//Client

package butp;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Random;
import java.util.zip.CRC32;
import java.util.zip.Checksum;
import javax.swing.JFileChooser;
import javax.swing.JFrame;


/**
 *
 * @author Haider Rehman, Varaynya Chivukula
 */


public final class BUTPClient extends JFrame {

	private static final long serialVersionUID = 1L;
        
        private DatagramPacket sendPacket;
	private DatagramSocket clientSOC;
	private InetAddress host;
        //Defining Port Number
	int port = 8888;
	
        
        //Generate Random Seqence Number To Be Consider as Corrupt
	int Random_Corrupt_Seq = (new Random().nextInt(4))+1;
        
        //Getnerate Random Squence Number to be consider Wrong Sequence
	int Random_Wrong_Seq = (new Random().nextInt(7))+1;

        //Defining Window size to be 1024 Bytes
	int WindowSize = 1024;//Bytes
        
        //Defining Packet Size subtracting the 24 Bytes Header
	int PacSize = WindowSize - 24;
	int TempPac = PacSize;
        
       
	byte[] fileContent;
	long checksum;
	//Initializing Sequence Number by 1.
        double SequenceNo = 1;
        
        
        //Starting Pointer Poistion at 0.
	int pointer = 0;
        
	int SizeOfFile;
	double TotalNoOfPac;
        
        String SerRes;
	String FName;
        String strtoSend;
	String DataPac = "";
        
    
        //CRC32Checksum
        
        
        public long CRC32Checksum(String checksumData) {
            
		byte tempBuffer[] = checksumData.getBytes();
		Checksum checksumEngine = new CRC32();
		checksumEngine.update(tempBuffer, 0, tempBuffer.length);
		return checksumEngine.getValue();
	}
        
        
        
        //Getting Selected File Info.
        
	public byte[] GetFileInfo() throws IOException {

            //Create a file chooser
            final JFileChooser fc = new JFileChooser();
            
            int result = fc.showOpenDialog(this);
		if (result == JFileChooser.CANCEL_OPTION) {
                    
                        //Check if the user Canceled the File Chooser
			System.exit(1);
		}
                
            File file = fc.getSelectedFile();
            
            //Getting the Name of the selected file.
            FName = file.getName();
            
            //Creat New Instance of the Input Stream for the selected file
            FileInputStream selectedFile = new FileInputStream(file);
            
            //Get Length Of the File
	    byte fileContent[] = new byte[(int) file.length()];
            
            
            //Read the content of the file
	    selectedFile.read(fileContent);
            
            //Convert to string and get the size of the file
	    SizeOfFile = new String(fileContent).trim().length();
            
            //Now we need to calculate how many packets we will be sending
            //So the total SizeOfFile will be divided by the defined packet size
            //Which will give us the number of packets that will be sending.
	    TotalNoOfPac = Math.ceil((float) SizeOfFile / PacSize);
	    return fileContent;
            
	}

	public BUTPClient(String hostname) {
            
		try {
                        //Creating New Socket
			clientSOC = new DatagramSocket();
			
                        //Get Address of the Host By Name (localhost/127.0.0.1).
                        host = InetAddress.getByName(hostname);
                        
                        //Get File Content Using File Chooser
			fileContent = GetFileInfo();
                        
                        String Displaymsg;
                        
                        //Creating The Initial Display Message.
                        
			Displaymsg = "Connecting PORT:*"+ port + "*";
		        Displaymsg +=" -Filename: "+ FName;
			Displaymsg +=" -FileSize: "+ SizeOfFile; 
			Displaymsg +=" -PacketSize: "+ PacSize + "\n";
                        
                        
                        System.out.println (Displaymsg);
                        
                        String tempString = StandardConstants.WRQ+"@_@"+ FName + "@_@" + CRC32Checksum(DataPac) + "@_@0";
                        
                        
                        //Sending Message..
                        byte sendable[] = tempString.getBytes();
                        sendPacket = new DatagramPacket(sendable, sendable.length, host, port);
                        clientSOC.send(sendPacket);
                        clientSOC.setSoTimeout(2000);
                        
                        
                        Displaymsg =  "\n Sending File : "+FName;
                        Displaymsg += "\n Total Reliable Packets to Be sent is : "+ TotalNoOfPac + " Packets.\n";

                        System.out.println (Displaymsg);
                        

			while (SizeOfFile > 0) {
				PacSize = TempPac;
				try {
					//Thread.sleep(800);
					PacSize = (SizeOfFile >= PacSize) ? PacSize: SizeOfFile;
                                        
					DataPac = new String(fileContent, pointer, PacSize);
					
					checksum = CRC32Checksum(DataPac);
					double seq = SequenceNo;
					
                                        
                                        
                                        // Creating Congestion On Random Sequence Number.
					if (Random_Corrupt_Seq == SequenceNo){    
						DataPac = new String(fileContent, pointer, (PacSize - 19));
						DataPac +="adding-some-garbage";
						Random_Corrupt_Seq += new Random().nextInt(5);
					}
                                        
                                        // Creating Wrong Sequnce on Random Sequence Number.
					if (Random_Wrong_Seq == SequenceNo){	
						seq = seq + 0.1;
						Random_Wrong_Seq += new Random().nextInt(8);
					}
					
                                        strtoSend = StandardConstants.DATA +"@_@"+ DataPac + "@_@"+ checksum + "@_@" + seq;

                                        //Sending Message with Data..
                                        byte sendable_data[] = strtoSend.getBytes();
                                        sendPacket = new DatagramPacket(sendable_data, sendable_data.length, host, port);
                                        clientSOC.send(sendPacket);
                                        clientSOC.setSoTimeout(2000);
                                        
                                        
                                        Displaymsg =  "\n Sequence Number:             " + SequenceNo;
                                        Displaymsg += "\n Current Window Size:         " + WindowSize + " Bytes";
                                        Displaymsg += "\n Current Chunk Length:        " + DataPac.length(); 
                                        Displaymsg += "\n Checksum:                    " + checksum+"\n";

                                        System.out.println (Displaymsg);
                                        
                                        //Recieving Data From Server.
                                        
                                        byte[] receiveData = new byte[WindowSize];
                                        DatagramPacket rPacket = new DatagramPacket(receiveData,receiveData.length);
                                        clientSOC.receive(rPacket);
                                        
                                        SerRes = new String(rPacket.getData()).trim();
                                        
					String[] responce = SerRes.split("_");
					SerRes = responce[0];
                                        
					if (responce.length > 1){
                                            
						WindowSize  = Integer.parseInt(responce[2]);
						TempPac     = WindowSize - 24;
					
                                        } else {
						
                                                WindowSize  = 1024;
						TempPac     = WindowSize - 24;
					}
                                        
                                        
					if (Integer.parseInt(SerRes) == StandardConstants.NAK){
                                            
						Displaymsg = "\nPacket Droped with Sequence Number: " + SequenceNo + " >>-Sending again->";
						System.out.println (Displaymsg);
                                                
					}else {
						pointer     = pointer + PacSize;
						SizeOfFile  = SizeOfFile - PacSize;
						SequenceNo  = SequenceNo + 1;
					}
				} catch (IOException e) {
					Displaymsg = "\n Server Not Responding.\n";
                                        System.out.println (Displaymsg);
                                }
			}

		} catch (IOException | NumberFormatException e) {
		}
	}

	public static void main(String args[]) {
            new BUTPClient("localhost");
	}

}

