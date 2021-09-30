import java.io.*;

// Steps for pubkey generation:
// 1. Run "ssh-keygen" - Don't give passphrase
// 2. ssh-copy-id -i "your key" user@host // to one machine is enough

public class Ssh {
   public static void main(String[] args) {
      String username = "mkodali";      //Login UserID
      String projPath = "/cise/homes/mkodali/cnproject/Group_10/"; // path of the project where PeerProcess binary
                                                              // is
      String pubKey = "rsakey"; // location of the generated key
      try {
         Runtime.getRuntime().exec("ssh -i " +pubKey+ " " + username +
         "@lin114-10.cise.ufl.edu cd " + projPath
         + " ; java PeerProcess 1001 ");
         Runtime.getRuntime().exec("ssh -i " + pubKey+" " + username +
         "@lin114-06.cise.ufl.edu cd " + projPath
         + " ; java PeerProcess 1002 ");
         Runtime.getRuntime().exec("ssh -i" + pubKey+" " + username +
         "@lin114-07.cise.ufl.edu cd " + projPath
         + " ; java PeerProcess 1003 ");
         Runtime.getRuntime().exec("ssh -i" + pubKey+" " + username +
         "@lin114-08.cise.ufl.edu cd " + projPath
         + " ; java PeerProcess 1004 ");
         Runtime.getRuntime().exec("ssh -i " + pubKey+" " + username +
         "@lin114-09.cise.ufl.edu cd " + projPath
         + " ; java PeerProcess 1005 ");
         Runtime.getRuntime().exec("ssh -i " + pubKey+" " + username +
         "@lin114-11.cise.ufl.edu cd " + projPath
         + " ; java PeerProcess 1006 ");
      } catch (Exception e) {
      }
   }
}
