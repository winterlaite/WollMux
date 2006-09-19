/*
 * Dateiname: PrintFunctionLibrary.java
 * Projekt  : WollMux
 * Funktion : Eine Bibliothek von benannten Druckfunktionen
 * 
 * Copyright: Landeshauptstadt M�nchen
 *
 * �nderungshistorie:
 * Datum      | Wer | �nderungsgrund
 * -------------------------------------------------------------------
 * 19.09.2006 | LUT | Erstellung.
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.func;

import java.util.HashMap;
import java.util.Map;

/**
 * Eine Bibliothek von benannten PrintFunctions
 * 
 * @author christoph.lutz
 */
public class PrintFunctionLibrary
{
  private Map mapIdToFunction;

  private PrintFunctionLibrary baselib;

  /**
   * Erzeugt eine leere Funktionsbibliothek.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public PrintFunctionLibrary()
  {
    this(null);
  }

  /**
   * Erzeugt eine Funktionsbibliothek, die baselib referenziert (nicht
   * kopiert!). baselib wird immer dann befragt, wenn die Funktionsbibliothek
   * selbst keine Funktion des entsprechenden Namens enth�lt. baselib darf null
   * sein.
   * 
   * @param baselib
   */
  public PrintFunctionLibrary(PrintFunctionLibrary baselib)
  {
    mapIdToFunction = new HashMap();
    this.baselib = baselib;
  }

  /**
   * F�gt func dieser Funktionsbibliothek unter dem Namen funcName hinzu. Eine
   * bereits existierende Funktion mit diesem Namen wird dabei ersetzt.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void add(String funcName, PrintFunction func)
  {
    if (func == null || funcName == null)
      throw new NullPointerException(
          "Weder Funktionsname noch Funktion darf null sein");
    mapIdToFunction.put(funcName, func);
  }

  /**
   * Liefert die Function namens funcName zur�ck oder null, falls keine Funktion
   * mit diesem Namen bekannt ist. Wurde die Funktionsbibliothek mit einer
   * Referenz auf eine andere Funktionsbibliothek initialisiert, so wird diese
   * befragt, falls die Funktionsbibliothek selbst keine Funktion des
   * entsprechenden Namens kennt.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public PrintFunction get(String funcName)
  {
    PrintFunction func = (PrintFunction) mapIdToFunction.get(funcName);
    if (func == null && baselib != null) func = baselib.get(funcName);
    return func;
  }

}
