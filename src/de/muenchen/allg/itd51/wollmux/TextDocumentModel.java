/*
 * Dateiname: TextDocumentModel.java
 * Projekt  : WollMux
 * Funktion : Repr�sentiert ein aktuell ge�ffnetes TextDokument.
 * 
 * Copyright: Landeshauptstadt M�nchen
 *
 * �nderungshistorie:
 * Datum      | Wer | �nderungsgrund
 * -------------------------------------------------------------------
 * 15.09.2006 | LUT | Erstellung als TextDocumentModel
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;

import com.sun.star.awt.PosSize;
import com.sun.star.frame.FrameSearchFlag;
import com.sun.star.frame.XController;
import com.sun.star.frame.XFrame;
import com.sun.star.frame.XModel;
import com.sun.star.lang.EventObject;
import com.sun.star.text.XTextDocument;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.util.CloseVetoException;
import com.sun.star.util.XCloseListener;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.former.FormularMax4000;

/**
 * Diese Klasse repr�sentiert das Modell eines aktuell ge�ffneten TextDokuments
 * und erm�glicht den Zugriff auf alle interessanten Aspekte des TextDokuments.
 * 
 * @author christoph.lutz
 * 
 */
public class TextDocumentModel
{
  /**
   * Enth�lt die Referenz auf das XTextDocument-interface des eigentlichen
   * TextDocument-Services der zugeh�rigen UNO-Komponente.
   */
  public final XTextDocument doc;

  /**
   * Erm�glicht den Zugriff auf einen Vector aller FormField-Objekte in diesem
   * TextDokument �ber den Namen der zugeordneten ID.
   */
  private HashMap idToFormFields;

  /**
   * Falls es sich bei dem Dokument um ein Formular handelt, wird das zugeh�rige
   * FormModel hier gespeichert und beim dispose() des TextDocumentModels mit
   * geschlossen.
   */
  private FormModel formModel;

  /**
   * In diesem Feld wird der CloseListener gespeichert, nachdem die Methode
   * registerCloseListener() aufgerufen wurde.
   */
  private XCloseListener closeListener;

  /**
   * Enth�lt die Instanz des aktuell ge�ffneten, zu diesem Dokument geh�renden
   * FormularMax4000.
   */
  private FormularMax4000 currentMax4000;

  /**
   * Dieses Feld stellt ein Zwischenspeicher f�r Fragment-Urls dar. Es wird dazu
   * benutzt, im Fall eines openTemplate-Befehls die urls der �bergebenen
   * frag_id-Liste tempor�r zu speichern.
   */
  private String[] fragUrls;

  /**
   * Enth�lt den Namen der Druckfunktion, die der DocumentCommandInterpreter
   * beim Ausf�hren der Dokumentkommandos feststellen kann.
   */
  private String printFunctionName;

  /**
   * TODO: comment
   */
  private XPrintModel printModel = new PrintModel();

  /**
   * Erzeugt ein neues TextDocumentModel zum XTextDocument doc und sollte nie
   * direkt aufgerufen werden, da neue TextDocumentModels �ber das
   * WollMuxSingletonie (WollMuxSingleton.getTextDocumentModel()) erzeugt und
   * verwaltet werden.
   * 
   * @param doc
   */
  public TextDocumentModel(XTextDocument doc)
  {
    this.doc = doc;
    this.idToFormFields = new HashMap();
    this.fragUrls = new String[] {};
    this.currentMax4000 = null;
    this.closeListener = null;
    this.printFunctionName = null;
  }

  /**
   * TextDocumentModels forwarden die hashCode-Methode an das referenzierte
   * XTextDocument weiter - so kann ein TextDocumentModel �ber eine HashMap
   * verwaltet werden.
   */
  public int hashCode()
  {
    if (doc != null) return doc.hashCode();
    return 0;
  }

  /**
   * Zwei TextDocumentModels sind dann gleich, wenn sie das selbe XTextDocument
   * doc referenzieren - so kann ein TextDocumentModel �ber eine HashMap
   * verwaltet werden. ACHTUNG: Die Gleichheit zweier TextDocumentModes
   * beschreibt nur die Gleichheit des verkn�pften XTextDocuments, nicht jedoch
   * die Gleichheit aller Felder des TextDocumentModels! Zwei Instanzen, die
   * equals==true zur�ckliefern m�ssen also nicht unbedingt wirklich identisch
   * sein.
   */
  public boolean equals(Object b)
  {
    if (b != null && b instanceof TextDocumentModel)
    {
      TextDocumentModel other = (TextDocumentModel) b;
      return UnoRuntime.areSame(this.doc, other.doc);
    }
    return false;
  }

  /**
   * Wird derzeit vom DocumentCommandInterpreter aufgerufen und gibt dem
   * TextDocumentModel alle FormFields bekannt, die beim Durchlauf des
   * FormScanners gefunden wurden.
   * 
   * @param idToFormFields
   */
  public void setIDToFormFields(HashMap idToFormFields)
  {
    this.idToFormFields = idToFormFields;
  }

  /**
   * Erm�glicht den Zugriff auf einen Vector aller FormField-Objekte in diesem
   * TextDokument �ber den Namen der zugeordneten ID.
   * 
   * @return Eine HashMap, die unter dem Schl�ssel ID einen Vector mit den
   *         entsprechenden FormFields bereith�lt.
   */
  public HashMap getIDToFormFields()
  {
    return idToFormFields;
  }

  /**
   * Speichert das FormModel formModel im TextDocumentModel und wird derzeit vom
   * DocumentCommandInterpreter aufgerufen, wenn er ein FormModel erzeugt.
   * 
   * @param formModel
   */
  public void setFormModel(FormModel formModel)
  {
    this.formModel = formModel;
  }

  /**
   * Liefert das zuletzt per setFormModel() gesetzte FormModel zur�ck.
   * 
   * @return
   */
  public FormModel getFormModel()
  {
    return this.formModel;
  }

  /**
   * Der DocumentCommandInterpreter liest sich die Liste der setFragUrls()
   * gespeicherten Fragment-URLs hier aus, wenn die Dokumentkommandos
   * insertContent ausgef�hrt werden.
   * 
   * @return
   */
  public String[] getFragUrls()
  {
    return fragUrls;
  }

  /**
   * �ber diese Methode kann der openDocument-Eventhandler die Liste der mit
   * einem insertContent-Kommando zu �ffnenden frag-urls speichern.
   */
  public void setFragUrls(String[] fragUrls)
  {
    this.fragUrls = fragUrls;
  }

  /**
   * Setzt die Instanz des aktuell ge�ffneten, zu diesem Dokument geh�renden
   * FormularMax4000.
   * 
   * @param max
   */
  public void setCurrentFormularMax4000(FormularMax4000 max)
  {
    currentMax4000 = max;
  }

  /**
   * Liefert die Instanz des aktuell ge�ffneten, zu diesem Dokument geh�renden
   * FormularMax4000 zur�ck, oder null, falls kein FormularMax gestartet wurde.
   * 
   * @return
   */
  public FormularMax4000 getCurrentFormularMax4000()
  {
    return currentMax4000;
  }

  /**
   * TODO: comment
   * 
   * @param printFunctionName
   */
  public void setPrintFunctionName(String printFunctionName)
  {
    this.printFunctionName = printFunctionName;
  }

  /**
   * TODO: comment
   * 
   * @return
   */
  public String getPrintFunctionName()
  {
    return "SachleitendeVerfuegung";
    // return this.printFunctionName;
  }

  /**
   * Setzt das Fensters des TextDokuments auf Sichtbar (visible==true) oder
   * unsichtbar (visible == false).
   * 
   * @param visible
   */
  public void setWindowVisible(boolean visible)
  {
    XModel xModel = UNO.XModel(doc);
    if (xModel != null)
    {
      XFrame frame = xModel.getCurrentController().getFrame();
      if (frame != null)
      {
        frame.getContainerWindow().setVisible(visible);
      }
    }
  }

  /**
   * Diese Methode setzt den DocumentModified-Status auf state.
   * 
   * @param state
   */
  public void setDocumentModified(boolean state)
  {
    try
    {
      UNO.XModifiable(doc).setModified(state);
    }
    catch (java.lang.Exception x)
    {
    }
  }

  /**
   * Diese Methode blockt/unblocked die Contoller, die f�r das Rendering der
   * Darstellung in den Dokumenten zust�ndig sind, jedoch nur, wenn nicht der
   * debug-modus gesetzt ist.
   * 
   * @param state
   */
  public void setLockControllers(boolean lock)
  {
    try
    {
      if (WollMuxSingleton.getInstance().isDebugMode() == false
          && UNO.XModel(doc) != null)
      {
        if (lock)
          UNO.XModel(doc).lockControllers();
        else
          UNO.XModel(doc).unlockControllers();
      }
    }
    catch (java.lang.Exception e)
    {
    }
  }

  /**
   * Setzt die Position des Fensters auf die �bergebenen Koordinaten, wobei die
   * Nachteile der UNO-Methode setWindowPosSize greifen, bei der die
   * Fensterposition nicht mit dem �usseren Fensterrahmen beginnt, sondern mit
   * der grauen Ecke links �ber dem File-Men�.
   * 
   * @param docX
   * @param docY
   * @param docWidth
   * @param docHeight
   */
  public void setWindowPosSize(int docX, int docY, int docWidth, int docHeight)
  {
    try
    {
      UNO.XModel(doc).getCurrentController().getFrame().getContainerWindow()
          .setPosSize(docX, docY, docWidth, docHeight, PosSize.POSSIZE);
    }
    catch (java.lang.Exception e)
    {
    }
  }

  /**
   * Schliesst das XTextDocument, das diesem Model zugeordnet ist. Ist der
   * closeListener registriert (was WollMuxSingleton bereits bei der Erstellung
   * des TextDocumentModels standardm�ig macht), so wird nach dem close() auch
   * automatisch die dispose()-Methode aufgerufen.
   */
  public void close()
  {
    // Damit OOo vor dem Schlie�en eines ver�nderten Dokuments den
    // save/dismiss-Dialog anzeigt, muss die suspend()-Methode aller
    // XController gestartet werden, die das Model der Komponente enthalten.
    // Man bekommt alle XController �ber die Frames, die der Desktop liefert.
    Object desktop = UNO.createUNOService("com.sun.star.frame.Desktop");
    if (UNO.XFramesSupplier(desktop) != null)
    {
      XFrame[] frames = UNO.XFramesSupplier(desktop).getFrames().queryFrames(
          FrameSearchFlag.ALL);
      for (int i = 0; i < frames.length; i++)
      {
        XController c = frames[i].getController();
        if (c != null && UnoRuntime.areSame(c.getModel(), doc))
          c.suspend(true);
      }
    }

    // Hier das eigentliche Schlie�en:
    try
    {
      if (UNO.XCloseable(doc) != null) UNO.XCloseable(doc).close(true);
    }
    catch (CloseVetoException e)
    {
    }
  }

  /**
   * Ruft die Dispose-Methoden von allen aktiven, dem TextDocumentModel
   * zugeordneten Dialogen auf und gibt den Speicher des TextDocumentModels
   * frei.
   */
  public void dispose()
  {
    if (currentMax4000 != null) currentMax4000.dispose();
    currentMax4000 = null;

    if (formModel != null) formModel.dispose();
    formModel = null;

    // L�scht das TextDocumentModel aus dem WollMux-Singleton.
    WollMuxSingleton.getInstance().disposedTextDocumentModel(this);
  }

  /**
   * Registriert genau einen XCloseListener in der Komponente des
   * XTextDocuments, so dass beim Schlie�en des Dokuments die entsprechenden
   * WollMuxEvents ausgef�hrt werden - ist in diesem TextDocumentModel bereits
   * ein XCloseListener registriert, so wird nichts getan.
   */
  public void registerCloseListener()
  {
    if (closeListener == null && UNO.XCloseable(doc) != null)
    {
      closeListener = new XCloseListener()
      {
        public void disposing(EventObject arg0)
        {
          WollMuxEventHandler.handleTextDocumentClosed(doc);
        }

        public void notifyClosing(EventObject arg0)
        {
          WollMuxEventHandler.handleTextDocumentClosed(doc);
        }

        public void queryClosing(EventObject arg0, boolean arg1)
            throws CloseVetoException
        {
        }
      };
      UNO.XCloseable(doc).addCloseListener(closeListener);
    }
  }

  /**
   * TODO: comment
   * 
   * @return
   */
  public XPrintModel getPrintModel()
  {
    return printModel;
  }

  /**
   * TODO: comment
   * 
   * @author christoph.lutz
   * 
   */
  public class PrintModel implements XPrintModel
  {
    private boolean[] lock = new boolean[] { true };

    public void print(short numberOfCopies)
    {
      setLock();
      WollMuxEventHandler.handleDoPrint(doc, numberOfCopies, unlockActionListener);
      waitForUnlock();
    }

    /**
     * TODO: doku anpassen: Setzt einen lock, der in Verbindung mit setUnlock
     * und der waitForUnlock-Methode verwendet werden kann, um quasi Modalit�t
     * f�r nicht modale Dialoge zu realisieren. setLock() sollte stets vor dem
     * Aufruf des nicht modalen Dialogs erfolgen, nach dem Aufruf des nicht
     * modalen Dialogs folgt der Aufruf der waitForUnlock()-Methode. Der nicht
     * modale Dialog erzeugt bei der Beendigung ein ActionEvent, das daf�r
     * sorgt, dass setUnlock aufgerufen wird.
     */
    protected void setLock()
    {
      lock[0] = true;
    }

    /**
     * TODO: doku anpassen. Macht einen mit setLock() gesetzten Lock r�ckg�ngig
     * und bricht damit eine evtl. wartende waitForUnlock()-Methode ab.
     */
    protected void setUnlock()
    {
      synchronized (lock)
      {
        lock[0] = false;
        lock.notifyAll();
      }
    }

    /**
     * TODO: doku anpassen. Wartet so lange, bis der vorher mit setLock()
     * gesetzt lock mit der Methode setUnlock() aufgehoben wird. So kann die
     * quasi Modalit�t nicht modale Dialoge zu realisiert werden. setLock()
     * sollte stets vor dem Aufruf des nicht modalen Dialogs erfolgen, nach dem
     * Aufruf des nicht modalen Dialogs folgt der Aufruf der
     * waitForUnlock()-Methode. Der nicht modale Dialog erzeugt bei der
     * Beendigung ein ActionEvent, das daf�r sorgt, dass setUnlock aufgerufen
     * wird.
     */
    protected void waitForUnlock()
    {
      try
      {
        synchronized (lock)
        {
          while (lock[0] == true)
            lock.wait();
        }
      }
      catch (InterruptedException e)
      {
      }
    }

    /**
     * TODO: doku anpassen: Dieser ActionListener kann nicht modalen Dialogen
     * �bergeben werden und sorgt in Verbindung mit den Methoden setLock() und
     * waitForUnlock() daf�r, dass quasi modale Dialoge realisiert werden
     * k�nnen.
     */
    protected UnlockActionListener unlockActionListener = new UnlockActionListener();

    protected class UnlockActionListener implements ActionListener
    {
      public ActionEvent actionEvent = null;

      public void actionPerformed(ActionEvent arg0)
      {
        setUnlock();
        actionEvent = arg0;
      }
    };

  }

}
