/*
* Dateiname: TestDatasourceJoiner.java
* Projekt  : WollMux
* Funktion : Simple Implementierung eines DatasourceJoiners zum testen.
* 
* Copyright: Landeshauptstadt München
*
* Änderungshistorie:
* Datum      | Wer | Änderungsgrund
* -------------------------------------------------------------------
* 19.10.2005 | BNK | Erstellung
* 20.10.2005 | BNK | Fertig
* 20.10.2005 | BNK | Fallback Rolle -> OrgaKurz
* 24.10.2005 | BNK | Erweitert um die Features, die PAL Verwalten braucht
* 31.10.2005 | BNK | TestDJ ist jetzt nur noch normaler DJ mit Default-
*                    initialisierung und ohne speichern
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.db;

import java.io.File;
import java.net.URL;
import java.util.Iterator;
import java.util.Set;

import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.TimeoutException;


/**
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class TestDatasourceJoiner extends DatasourceJoiner
{
  
  public void saveCacheAndLOS()
  {
    //TestDJ soll nichts überschreiben
  }
  
  public TestDatasourceJoiner()
  { //TESTED
    try
    {
      File curDir = new File(System.getProperty("user.dir"));
      URL context = curDir.toURL();
      File losCache = new File(curDir, "testdata/cache.conf");
      String confFile = "testdata/testdjjoin.conf";
      URL confURL = new URL(context,confFile);
      ConfigThingy joinConf = new ConfigThingy("",confURL);
      init(joinConf, "Personal", losCache, context);
    }
    catch (Exception e)
    {
      e.printStackTrace();
      System.exit(1);
    }
  }
  
    
  public static void printResults(String query, Set schema, QueryResults results)
  {
    System.out.println("Results for query \""+query+"\":");
    Iterator resIter = results.iterator();
    while (resIter.hasNext())
    {
      Dataset result = (Dataset)resIter.next();
      
      Iterator spiter = schema.iterator();
      while (spiter.hasNext())
      {
        String spalte = (String)spiter.next();
        String wert = "Spalte "+spalte+" nicht gefunden!";
        try{ 
          wert = result.get(spalte);
          if (wert == null) 
            wert = "unbelegt";
          else
            wert = "\""+wert+"\"";
        }catch(ColumnNotFoundException x){};
        System.out.print(spalte+"="+wert+(spiter.hasNext()?", ":""));
      }
      System.out.println();
    }
    System.out.println();
  }
  
  
  public static void main(String[] args) throws TimeoutException
  {
    TestDatasourceJoiner dj = new TestDatasourceJoiner();
    printResults("Nachname = Benkmux", dj.getMainDatasourceSchema(), dj.find("Nachname","Benkmux"));
    printResults("Nachname = Benkm*", dj.getMainDatasourceSchema(), dj.find("Nachname","Benkm*"));
    printResults("Nachname = *ux", dj.getMainDatasourceSchema(), dj.find("Nachname","*ux"));
    printResults("Nachname = *oe*", dj.getMainDatasourceSchema(), dj.find("Nachname","*oe*"));
    printResults("Nachname = Schlonz", dj.getMainDatasourceSchema(), dj.find("Nachname","Schlonz"));
    printResults("Nachname = Lutz", dj.getMainDatasourceSchema(), dj.find("Nachname","Lutz"));
    printResults("Nachname = *uX, Vorname = m*", dj.getMainDatasourceSchema(), dj.find("Nachname","*uX","Vorname","m*"));
    printResults("Local Override Storage", dj.getMainDatasourceSchema(), dj.getLOS());
  }
  
}
