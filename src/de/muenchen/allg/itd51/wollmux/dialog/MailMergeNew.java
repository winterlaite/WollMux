/*
* Dateiname: MailMergeNew.java
* Projekt  : WollMux
* Funktion : Die neuen erweiterten Serienbrief-Funktionalit�ten
* 
* Copyright: Landeshauptstadt M�nchen
*
* �nderungshistorie:
* Datum      | Wer | �nderungsgrund
* -------------------------------------------------------------------
* 11.10.2007 | BNK | Erstellung
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.dialog;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.border.Border;

import com.sun.star.awt.XTopWindow;
import com.sun.star.container.XEnumeration;
import com.sun.star.document.XEventListener;
import com.sun.star.lang.EventObject;
import com.sun.star.sheet.XCellRangesQuery;
import com.sun.star.sheet.XSheetCellRanges;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.sheet.XSpreadsheets;
import com.sun.star.table.CellRangeAddress;
import com.sun.star.table.XCellRange;
import com.sun.star.text.XTextDocument;
import com.sun.star.ui.dialogs.XFilePicker;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.util.CloseVetoException;
import com.sun.star.util.XCloseListener;
import com.sun.star.util.XModifiable;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.L;
import de.muenchen.allg.itd51.wollmux.Logger;
import de.muenchen.allg.itd51.wollmux.TextDocumentModel;
import de.muenchen.allg.itd51.wollmux.UnavailableException;
import de.muenchen.allg.itd51.wollmux.XPrintModel;
import de.muenchen.allg.itd51.wollmux.TextDocumentModel.FieldSubstitution;
import de.muenchen.allg.itd51.wollmux.TextDocumentModel.ReferencedFieldID;
import de.muenchen.allg.itd51.wollmux.db.ColumnNotFoundException;
import de.muenchen.allg.itd51.wollmux.db.Dataset;
import de.muenchen.allg.itd51.wollmux.db.Datasource;
import de.muenchen.allg.itd51.wollmux.db.OOoDatasource;
import de.muenchen.allg.itd51.wollmux.db.QueryResults;
import de.muenchen.allg.itd51.wollmux.db.QueryResultsList;
import de.muenchen.allg.itd51.wollmux.dialog.trafo.GenderDialog;
import de.muenchen.allg.itd51.wollmux.dialog.trafo.TrafoDialog;
import de.muenchen.allg.itd51.wollmux.dialog.trafo.TrafoDialogFactory;
import de.muenchen.allg.itd51.wollmux.dialog.trafo.TrafoDialogParameters;

/**
 * Die neuen erweiterten Serienbrief-Funktionalit�ten.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class MailMergeNew
{
  /**
   * ID der Property in der die Serienbriefdaten gespeichert werden.
   */
  private static final String PROP_QUERYRESULTS = "MailMergeNew_QueryResults";

  /**
   * Das {@link TextDocumentModel} zu dem Dokument an dem diese Toolbar h�ngt.
   */
  private TextDocumentModel mod;
  
  /**
   * Stellt die Felder und Datens�tze f�r die Serienbriefverarbeitung bereit.
   */
  private MailMergeDatasource ds;
  
  /**
   * true gdw wir uns im Vorschau-Modus befinden.
   */
  private boolean previewMode;
  
  /**
   * Die Nummer des zu previewenden Datensatzes.
   * ACHTUNG! Kann aufgrund von Ver�nderung der Daten im Hintergrund gr��er sein
   * als die Anzahl der Datens�tze. Darauf muss geachtet werden.
   */
  private int previewDatasetNumber = 1;
  
  /**
   * Wird auf true gesetzt, wenn der Benutzer beim Seriendruck ausw�hlt, dass er
   * die Ausgabe in einem neuen Dokument haben m�chte.
   */
  private boolean printIntoDocument = true;
  
  /**
   * Das Textfield in dem Benutzer direkt eine Datensatznummer f�r die Vorschau
   * eingeben k�nnen.
   */
  private JTextField previewDatasetNumberTextfield;
  
  /**
   * Das Toolbar-Fenster.
   */
  private JFrame myFrame;
  
  /**
   * Der WindowListener, der an {@link #myFrame} h�ngt.
   */
  private MyWindowListener oehrchen;
  
  /**
   * Falls nicht null wird dieser Listener aufgerufen nachdem der MailMergeNew
   * geschlossen wurde.
   */
  private ActionListener abortListener = null;

  /**
   * Die zentrale Klasse, die die Serienbrieffunktionalit�t bereitstellt.
   * @param mod das {@link TextDocumentModel} an dem die Toolbar h�ngt.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  public MailMergeNew(TextDocumentModel mod, ActionListener abortListener)
  {
    this.mod = mod;
    this.ds = new MailMergeDatasource(mod);
    this.abortListener = abortListener;
    
//  GUI im Event-Dispatching Thread erzeugen wg. Thread-Safety.
    try{
      javax.swing.SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          try{createGUI();}catch(Exception x){Logger.error(x);};
        }
      });
    }
    catch(Exception x) {Logger.error(x);}
  }
  
  private void createGUI()
  {
    myFrame = new JFrame(L.m("Seriendruck (WollMux)"));
    myFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    oehrchen = new MyWindowListener();
    myFrame.addWindowListener(oehrchen);
    
    Box hbox = Box.createHorizontalBox();
    myFrame.add(hbox);
    JButton button;
    button = new JButton(L.m("Datenquelle"));
    button.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e)
      {
        ds.showDatasourceSelectionDialog(myFrame);
      }
    });
    hbox.add(button);
    
    hbox.add(new JSeparator(SwingConstants.VERTICAL));
    
    //FIXME: Ausgrauen, wenn kein Datenquelle ausgew�hlt
    button = new JPotentiallyOverlongPopupMenuButton(L.m("Serienbrieffeld"),new Iterable()
        {public Iterator<Action> iterator(){
            return getInsertFieldActionList().iterator();
          }});
    hbox.add(button);
    
    button = new JButton(L.m("Spezialfeld"));
    final JButton specialFieldButton = button;
    button.addActionListener(new ActionListener()
        {
      public void actionPerformed(ActionEvent e)
      {
        showInsertSpecialFieldPopup(specialFieldButton, 0, specialFieldButton.getSize().height);
      }
        });
    hbox.add(button);
    
    hbox.add(new JSeparator(SwingConstants.VERTICAL));
    
    final String VORSCHAU = L.m("   Vorschau   ");
    button = new JButton(VORSCHAU);
    previewMode = false;
    mod.setFormFieldsPreviewMode(previewMode); //TODO updatePreviewFields()
    final JButton previewButton = button;
    button.addActionListener(new ActionListener()
        {
      public void actionPerformed(ActionEvent e)
      {
        if (!ds.hasDatasource()) return;
        if (previewMode)
        {
          mod.collectNonWollMuxFormFields();
          previewButton.setText(VORSCHAU);
          previewMode = false;
          mod.setFormFieldsPreviewMode(false);//TODO updatePreviewFields()
        }
        else
        {
          mod.collectNonWollMuxFormFields();
          previewButton.setText(L.m("<Feldname>"));
          previewMode = true;
          //TODO updatePreviewFields();
          mod.setFormFieldsPreviewMode(true);          
        }
      }
        });
    hbox.add(DimAdjust.fixedSize(button));
    
    //  FIXME: Muss ausgegraut sein, wenn nicht im Vorschau-Modus.
    button = new JButton("|<");
    button.addActionListener(new ActionListener()
        {
      public void actionPerformed(ActionEvent e)
      {
        previewDatasetNumber = 1;
        //TODO updatePreviewFields();
      }
        });
    hbox.add(button);
    
    //FIXME: Muss ausgegraut sein, wenn nicht im Vorschau-Modus.
    button = new JButton("<");
    button.addActionListener(new ActionListener()
        {
      public void actionPerformed(ActionEvent e)
      {
        --previewDatasetNumber;
        if (previewDatasetNumber < 1) previewDatasetNumber = 1;
        //TODO updatePreviewFields();
      }
        });
    hbox.add(button);
    
    //  FIXME: Muss ausgegraut sein, wenn nicht im Vorschau-Modus.
    previewDatasetNumberTextfield = new JTextField("1",3);
    previewDatasetNumberTextfield.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        String tfValue = previewDatasetNumberTextfield.getText();
        try{
          int newValue = Integer.parseInt(tfValue);
          previewDatasetNumber = newValue;
        }catch(Exception x)
        {
          previewDatasetNumberTextfield.setText(""+previewDatasetNumber);
        }
        //TODO updatePreviewFields();
      }
    });
    previewDatasetNumberTextfield.setMaximumSize(new Dimension(Integer.MAX_VALUE,button.getPreferredSize().height));
    hbox.add(previewDatasetNumberTextfield);
    
    //  FIXME: Muss ausgegraut sein, wenn nicht im Vorschau-Modus.
    button = new JButton(">");
    button.addActionListener(new ActionListener()
        {
      public void actionPerformed(ActionEvent e)
      {
        ++previewDatasetNumber;
        //TODO updatePreviewFields();
      }
        });
    hbox.add(button);
    
    //  FIXME: Muss ausgegraut sein, wenn nicht im Vorschau-Modus.
    button = new JButton(">|");
    button.addActionListener(new ActionListener()
        {
      public void actionPerformed(ActionEvent e)
      {
        previewDatasetNumber = Integer.MAX_VALUE;
        //TODO updatePreviewFields();
      }
        });
    hbox.add(button);
    
    hbox.add(new JSeparator(SwingConstants.VERTICAL));

    //FIXME: Ausgrauen, wenn keine Datenquelle gew�hlt ist.
    button = new JButton(L.m("Drucken"));
    button.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        if (ds.hasDatasource())
          showMailmergeTypeSelectionDialog();
      }
    });
    hbox.add(button);
    
    hbox.add(new JSeparator(SwingConstants.VERTICAL));
    
    final JPopupMenu tabelleMenu = new JPopupMenu();
    JMenuItem item = new JMenuItem(L.m("Tabelle bearbeiten"));
    item.addActionListener(new ActionListener()
        {
      public void actionPerformed(ActionEvent e)
      {
        ds.toFront();
      }
        });
    tabelleMenu.add(item);

    final JMenuItem addColumnsMenuItem = new JMenuItem(L.m("Tabellenspalten erg�nzen"));
    addColumnsMenuItem.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        showAddMissingColumnsDialog();
      }
    });
    tabelleMenu.add(addColumnsMenuItem);

    final JMenuItem adjustFieldsMenuItem = new JMenuItem(L.m("Alle Felder anpassen"));
    adjustFieldsMenuItem.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        showAdjustFieldsDialog();
      }
    });
    tabelleMenu.add(adjustFieldsMenuItem);

//  FIXME: Button darf nur angezeigt werden, wenn tats�chlich eine Calc-Tabelle
    //ausgew�hlt ist.
    button = new JButton(L.m("Tabelle"));
    final JButton tabelleButton = button;
    button.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {        
        // Anpassen des Men�punktes "Felder anpassen"
        if(mod.hasSelection()) {
          adjustFieldsMenuItem.setText(L.m("Ausgew�hlte Felder anpassen"));
        } else {
          adjustFieldsMenuItem.setText(L.m("Alle Felder anpassen"));
        }
        
        // Ausgrauen der Anpassen-Kn�pfe, wenn alle Felder mit den
        // entsprechenden Datenquellenfeldern zugeordnet werden k�nnen.
        boolean hasUnmappedFields = mod.getReferencedFieldIDsThatAreNotInSchema(new HashSet<String>(ds.getColumnNames())).length > 0; 
        adjustFieldsMenuItem.setEnabled(hasUnmappedFields);
        //TODO: einkommentieren wenn implementiert:
        //addColumnsMenuItem.setEnabled(hasUnmappedFields);
        addColumnsMenuItem.setEnabled(false);
        
        tabelleMenu.show(tabelleButton, 0, tabelleButton.getSize().height);
      }
    });
    hbox.add(button);
    
    myFrame.setAlwaysOnTop(true);
    myFrame.pack();
    int frameWidth = myFrame.getWidth();
    int frameHeight = myFrame.getHeight();
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    int x = screenSize.width/2 - frameWidth/2; 
    int y = frameHeight*3;//screenSize.height/2 - frameHeight/2;
    myFrame.setLocation(x,y);
    myFrame.setResizable(false);
    mod.addCoupledWindow(myFrame);
    myFrame.setVisible(true);
    
    if (!ds.hasDatasource()) ds.showDatasourceSelectionDialog(myFrame);
  }

  /**
   * Diese Methode zeigt den Dialog an, mit dem die Felder im Dokument an eine
   * Datenquelle angepasst werden k�nnen, die nicht die selben Spalten enth�lt
   * wie die Datenquelle, f�r die das Dokument gemacht wurde.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  protected void showAdjustFieldsDialog()
  {
    ReferencedFieldID[] fieldIDs = mod.getReferencedFieldIDsThatAreNotInSchema(new HashSet<String>(ds.getColumnNames()));
    ActionListener submitActionListener = new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        HashMap mapIdToSubstitution = (HashMap) e.getSource();
        for (Iterator iter = mapIdToSubstitution.keySet().iterator(); iter
            .hasNext();)
        {
          String fieldId = (String) iter.next();
          FieldSubstitution subst = (FieldSubstitution) mapIdToSubstitution
              .get(fieldId);
          mod.applyFieldSubstitution(fieldId, subst);
        }
      }
    };
    showFieldMappingDialog(fieldIDs, L.m("Felder anpassen"), L.m("Altes Feld"), L.m("Neue Belegung"), L.m("Felder anpassen"), submitActionListener);
  }

  /**
   * Diese Methode zeigt den Dialog an, mit dem die Spalten der Tabelle erg�nzt
   * werden k�nnen, wenn es zu Feldern im Dokument keine passenden Spalten in
   * der Tabelle gibt.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  protected void showAddMissingColumnsDialog()
  {
    ReferencedFieldID[] fieldIDs = mod.getReferencedFieldIDsThatAreNotInSchema(new HashSet<String>(ds.getColumnNames()));
    ActionListener submitActionListener = new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        // Enth�lt die Zuordnung ID -> de.muenchen.allg.itd51.wollmux.TextDocumentModel.FieldSubstitution,
        // in der die anzuwendende Ersetzungsregel beschrieben ist.

        //HashMap mapIdToSubstitution = (HashMap) e.getSource();
        // TODO: tabellenspalten wie in mapIdToSubstitution beschrieben erg�nzen
      }
    };
    showFieldMappingDialog(fieldIDs, L.m("Tabellenspalten erg�nzen"), L.m("Spalte"), L.m("Vorbelegung"), L.m("Spalten erg�nzen"), submitActionListener);
  }
  
  /**
   * Zeigt einen Dialog mit dem bestehende Felder fieldIDs �ber ein Textfeld neu
   * belegt werden k�nnen; f�r die neue Belegung stehen die neuen Felder der
   * aktuellen Datasource und Freitext zur Verf�gung. Die Felder fieldIDs werden
   * dabei in der Reihenfolge angezeigt, in der sie in der Liste aufgef�hrt
   * sind, ein bereits aufgef�hrtes Feld wird aber nicht zweimal angezeigt. Ist
   * bei einem Feld die Eigenschaft isTransformed()==true, dann wird f�r dieses
   * Feld nur die Eingabe einer 1-zu-1 Zuordnung von Feldern akzeptiert, das
   * andere Zuordnungen f�r transformierte Felder derzeit nicht unterst�tzt
   * werden.
   * 
   * @param fieldIDs
   *          Die field-IDs der alten, bereits im Dokument enthaltenen Felder,
   *          die in der gegebenen Reihenfolge angezeigt werden, Dupletten
   *          werden aber entfernt.
   * @param title
   *          Die Titelzeile des Dialogs
   * @param labelOldFields
   *          Die Spalten�berschrift f�r die linke Spalte, in der die alten
   *          Felder angezeigt werden.
   * @param labelNewFields
   *          Die Spalten�berschrift f�r die rechte Spalte, in dem die neue
   *          Zuordnung getroffen wird.
   * @param labelSubmitButton
   *          Die Beschriftung des Submit-Knopfes unten rechts, der die
   *          entsprechende Aktion ausl�st.
   * @param submitActionListener
   *          Nach Beendigung des Dialogs �ber den Submit-Knopf (unten rechts)
   *          wird die Methode submitActionListener.actionPerformed(actionEvent)
   *          in einem separaten Thread aufgerufen. Dort kann der Code stehen,
   *          der gew�nschten Aktionen durchf�hrt. Der ActionListener bekommt
   *          dabei in actionEvent eine HashMap �bergeben, die eine Zuordnung
   *          von den alten fieldIDs auf den jeweiligen im Dialog gew�hlten
   *          Ersatzstring enth�lt.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private void showFieldMappingDialog(ReferencedFieldID[] fieldIDs,
      String title, String labelOldFields, String labelNewFields,
      String labelSubmitButton, final ActionListener submitActionListener)
  {
    final JDialog dialog = new JDialog(myFrame, title, true);
    dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

    final TextComponentTags[] currentField = new TextComponentTags[] { null };
    final HashMap<TextComponentTags, String> mapTextComponentTagsToFieldname = new HashMap<TextComponentTags, String>();

    Box headers = Box.createHorizontalBox();
    final JButton insertFieldButton = new JPotentiallyOverlongPopupMenuButton(
        L.m("Serienbrieffeld"), new Iterable()
        {
          public Iterator<Action> iterator()
          {
            List<Action> actions = new Vector<Action>();
            List<String> columnNames = ds.getColumnNames();

            Collections.sort(columnNames);

            Iterator<String> iter = columnNames.iterator();
            while (iter.hasNext())
            {
              final String name = iter.next();
              Action button = new AbstractAction(name)
              {
                private static final long serialVersionUID = 0;

                public void actionPerformed(ActionEvent e)
                {
                  if (currentField[0] != null) currentField[0].insertTag(name);
                }
              };
              actions.add(button);
            }

            return actions.iterator();
          }
        });
    insertFieldButton.setFocusable(false);
    headers.add(Box.createHorizontalGlue());
    headers.add(insertFieldButton);

    Box itemBox = Box.createVerticalBox();
    ArrayList<JLabel> labels = new ArrayList<JLabel>();
    int maxLabelWidth = 0;

    Box hbox = Box.createHorizontalBox();
    JLabel label = new JLabel(labelOldFields);
    labels.add(label);
    maxLabelWidth = DimAdjust.maxWidth(maxLabelWidth, label);
    label.setBorder(BorderFactory.createEmptyBorder(5, 5, 10, 5));
    hbox.add(label);
    label = new JLabel(labelNewFields);
    label.setBorder(BorderFactory.createEmptyBorder(5, 5, 10, 5));
    DimAdjust.maxHeightIsPrefMaxWidthUnlimited(label);
    hbox.add(label);
    hbox.add(Box.createHorizontalStrut(200));
    DimAdjust.maxHeightIsPrefMaxWidthUnlimited(hbox);
    itemBox.add(hbox);

    HashSet<String> addedFields = new HashSet<String>();
    for (int i = 0; i < fieldIDs.length; i++)
    {
      String fieldId = fieldIDs[i].getFieldId();
      if (addedFields.contains(fieldId)) continue;
      final boolean isTransformed = fieldIDs[i].isTransformed();
      addedFields.add(fieldId);

      hbox = Box.createHorizontalBox();

      label = new JLabel(fieldId);
      label.setFont(label.getFont().deriveFont(Font.PLAIN));
      labels.add(label);
      maxLabelWidth = DimAdjust.maxWidth(maxLabelWidth, label);
      label.setBorder(BorderFactory.createEmptyBorder(5, 5, 10, 5));
      hbox.add(label);

      final TextComponentTags field = new TextComponentTags(new JTextField()) {
          public boolean isContentValid() {
            if(!isTransformed) return true;
            List c = getContent();
            if(c.size() == 0) return true;
            return c.size() == 1 && ((TextComponentTags.ContentElement) c.get(0)).isTag();  
          }
      };
      mapTextComponentTagsToFieldname.put(field, fieldId);
      Box fbox = Box.createHorizontalBox();
      hbox.add(fbox); // fbox f�r zeilenb�ndige Ausrichtung ben�tigt
      fbox.setBorder(BorderFactory.createEmptyBorder(5, 5, 10, 5));
      field.getJTextComponent().addFocusListener(new FocusListener()
      {
        public void focusLost(FocusEvent e)
        {
        }

        public void focusGained(FocusEvent e)
        {
          currentField[0] = field;
        }
      });
      DimAdjust.maxHeightIsPrefMaxWidthUnlimited(field.getJTextComponent());
      fbox.add(field.getJTextComponent());

      itemBox.add(hbox);
    }

    // einheitliche Breite f�r alle Labels vergeben:
    for (Iterator<JLabel> iter = labels.iterator(); iter.hasNext();)
    {
      label = iter.next();

      Dimension d = label.getPreferredSize();
      d.width = maxLabelWidth + 10;
      label.setPreferredSize(d);
    }

    Box buttonBox = Box.createHorizontalBox();
    JButton button = new JButton(L.m("Abbrechen"));
    button.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        dialog.dispose();
      }
    });
    buttonBox.add(button);

    buttonBox.add(Box.createHorizontalGlue());

    button = new JButton(labelSubmitButton);
    button.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        final HashMap<String, FieldSubstitution> result = new HashMap<String, FieldSubstitution>();
        for (Iterator<TextComponentTags> iter = mapTextComponentTagsToFieldname.keySet().iterator(); iter
            .hasNext();)
        {
          TextComponentTags f = iter.next();
          if(!f.isContentValid()) continue;
          String fieldId = "" + mapTextComponentTagsToFieldname.get(f);
          FieldSubstitution subst = new TextDocumentModel.FieldSubstitution();
          for (Iterator contentIter = f.getContent().iterator(); contentIter.hasNext();)
          {
            TextComponentTags.ContentElement ce = (TextComponentTags.ContentElement) contentIter.next();
            if (ce.isTag())
              subst.addField(ce.toString());
            else
              subst.addFixedText(ce.toString());
          }
          result.put(fieldId, subst);
        }

        dialog.dispose();

        if (submitActionListener != null) new Thread()
        {
          public void run()
          {
            submitActionListener.actionPerformed(new ActionEvent(result, 0,
                "showSubstitutionDialogReturned"));
          }
        }.start();
      }
    });
    buttonBox.add(button);

    JScrollPane spane = new JScrollPane(itemBox,
        ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    spane.setBorder(BorderFactory.createEmptyBorder());
    dialog.add(spane);

    Box vbox = Box.createVerticalBox();
    vbox.add(headers);
    vbox.add(spane);
    vbox.add(Box.createVerticalGlue());
    vbox.add(buttonBox);
    vbox.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

    dialog.add(vbox);

    dialog.pack();
    int frameWidth = dialog.getWidth();
    int frameHeight = dialog.getHeight();
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    int x = screenSize.width / 2 - frameWidth / 2;
    int y = screenSize.height / 2 - frameHeight / 2;
    dialog.setLocation(x, y);
    dialog.setVisible(true);
  }

    /**
     * Schliesst den MailMergeNew und alle zugeh�rigen Fenster.
     * 
     * @author Christoph Lutz (D-III-ITD 5.1)
     */
  public void dispose()
  {
    try{
      javax.swing.SwingUtilities.invokeLater(new Runnable() {
        public void run() {
            try{abort();}catch(Exception x){};
        }
      });
    }
    catch(Exception x) {}
  }

  /**
   * Zeigt den Dialog an, der die Serienbriefverarbeitung (Direktdruck oder in neues Dokument)
   * anwirft.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TODO Testen
   */
  private void showMailmergeTypeSelectionDialog()
  {
    final JDialog dialog = new JDialog(myFrame, L.m("Seriendruck"), true);
    dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    
    Box vbox = Box.createVerticalBox();
    dialog.add(vbox);
    
    Box hbox = Box.createHorizontalBox();
    JLabel label = new JLabel(L.m("Serienbriefe"));
    hbox.add(label);
    hbox.add(Box.createHorizontalStrut(5));
    
    Vector<String> types = new Vector<String>();
    types.add(L.m("in neues Dokument schreiben"));
    types.add(L.m("auf dem Drucker ausgeben"));
    final JComboBox typeBox = new JComboBox(types);
    typeBox.addItemListener(new ItemListener(){
      public void itemStateChanged(ItemEvent e)
      {
        printIntoDocument = (typeBox.getSelectedIndex() == 0);
      }});
    hbox.add(typeBox);
    
    //FIXME: darf nur sichtbar sein, wenn in typeBox "auf dem Drucker ausgeben" gew�hlt ist
    JButton button = new JButton(L.m("Drucker einrichten"));
    button.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        //TODO Drucker einrichten Button
      }
    });
    hbox.add(button);
    
    vbox.add(hbox);
    
    hbox = Box.createHorizontalBox();
    Border border = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY), L.m("Folgende Datens�tze verwenden"));
    hbox.setBorder(border);
    
    ButtonGroup radioGroup = new ButtonGroup();
    JRadioButton rbutton;
    rbutton = new JRadioButton(L.m("Alle"), true);
    hbox.add(rbutton);
    radioGroup.add(rbutton);
    rbutton = new JRadioButton(L.m("Von"), false);
    hbox.add(rbutton);
    radioGroup.add(rbutton);
    JTextField start = new JTextField("     "); //TODO Handler, der Eingabe validiert (nur Zahl erlaubt) und evtl. das end Textfield anpasst (insbes. wenn dort noch nichts drinsteht). Hierzu sind bereits Zugriffe auf die Datenquelle erforderlich. Auch der Von-Radiobutton muss angew�hlt werden.
    hbox.add(start);
    label = new JLabel("Bis");
    hbox.add(label);
    JTextField end = new JTextField("     "); //TODO Handler wie bei start TextField
    hbox.add(end);
    rbutton = new JRadioButton(""); //TODO Anwahl muss selben Effekt haben wie das Dr�cken des "Einzelauswahl" Buttons
    hbox.add(rbutton);
    radioGroup.add(rbutton);
    
    button = new JButton(L.m("Einzelauswahl..."));
    button.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
//      TODO implementieren. Muss auch den davorstehenden Radio-Button selektieren.
      }
    });
    hbox.add(button);
    
    vbox.add(hbox);
    
    hbox = Box.createHorizontalBox();
    button = new JButton(L.m("Abbrechen"));
    button.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        dialog.dispose();
      }
    });
    hbox.add(button);

    hbox.add(Box.createHorizontalGlue());
    
    button = new JButton(L.m("Los geht's!"));
    button.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        dialog.dispose();
        doMailMerge();
      }
    });
    hbox.add(button);

    vbox.add(hbox);
    
    dialog.pack();
    int frameWidth = dialog.getWidth();
    int frameHeight = dialog.getHeight();
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    int x = screenSize.width/2 - frameWidth/2; 
    int y = screenSize.height/2 - frameHeight/2;
    dialog.setLocation(x,y);
    dialog.setResizable(false);
    dialog.setVisible(true);
  }

  /**
   * Erzeugt eine Liste mit {@link javax.swing.Action}s f�r 
   * alle Namen aus {@link #ds},getColumnNames(), die ein entsprechendes
   * Seriendruckfeld einf�gen.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private List<Action> getInsertFieldActionList()
  {
    List<Action> actions = new Vector<Action>();
    List<String> columnNames = ds.getColumnNames();

    Collections.sort(columnNames);
    
    Iterator<String> iter = columnNames.iterator();
    while (iter.hasNext())
    {
      final String name = iter.next();
      Action button = new AbstractAction(name)
      {
        private static final long serialVersionUID = 0; //Eclipse-Warnung totmachen

        public void actionPerformed(ActionEvent e)
        {
          mod.insertMailMergeFieldAtCursorPosition(name);
        }
      };
      actions.add(button);
    }
    
    return actions;
  }

    /**
   * Erzeugt ein JPopupMenu, das Eintr�ge f�r das Einf�gen von Spezialfeldern
   * enth�lt und zeigt es an neben invoker an der relativen
   * Position x,y.
   * @param invoker zu welcher Komponente geh�rt das Popup
   * @param x Koordinate des Popups im Koordinatenraum von invoker.
   * @param y Koordinate des Popups im Koordinatenraum von invoker.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TODO Testen
   */
  private void showInsertSpecialFieldPopup(JComponent invoker, int x, int y)
  {
    boolean dsHasFields = ds.getColumnNames().size() > 0;
    final TrafoDialog editFieldDialog = getTrafoDialogForCurrentSelection();
    
    JPopupMenu menu = new JPopupMenu();
    
    JMenuItem button;

    final String genderButtonName = L.m("Gender");
    button = new JMenuItem(genderButtonName);
    button.setEnabled(dsHasFields);
    button.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        // ConfigThingy f�r leere Gender-Funktion zusammenbauen.
        ConfigThingy genderConf = GenderDialog.generateGenderTrafoConf(
          ds.getColumnNames().get(0).toString(), "", "", "");
        insertFieldFromTrafoDialog(ds.getColumnNames(), genderButtonName, genderConf);
      }
    });
    menu.add(button);

    final String iteButtonName = L.m("Wenn...Dann...Sonst...");
    button = new JMenuItem(iteButtonName);
    button.setEnabled(dsHasFields);
    button.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        // ConfigThingy f�r leere WennDannSonst-Funktion zusammenbauen. Aufbau:
        // IF(STRCMP(VALUE '<firstField>', '') THEN('') ELSE(''))
        ConfigThingy ifConf = new ConfigThingy("IF");
        ConfigThingy strCmpConf = ifConf.add("STRCMP");
        strCmpConf.add("VALUE").add(ds.getColumnNames().get(0).toString());
        strCmpConf.add("");
        ifConf.add("THEN").add("");
        ifConf.add("ELSE").add("");
        insertFieldFromTrafoDialog(ds.getColumnNames(), iteButtonName, ifConf);
      }
    });
    menu.add(button);
    
    button = new JMenuItem(L.m("Datensatznummer"));
    button.setEnabled(false); // NOT YET IMPLEMENTED
    button.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        //TODO insertDatasetIndex();
      }
    });
    menu.add(button);
    
    button = new JMenuItem(L.m("Serienbriefnummer"));
    button.setEnabled(false); // NOT YET IMPLEMENTED
    button.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        //TODO insertMailMergeIndex();
      }
    });
    menu.add(button);
    
    button = new JMenuItem(L.m("Feld bearbeiten..."));
    button.setEnabled(editFieldDialog != null);
    button.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        editFieldDialog.show(L.m("Spezialfeld bearbeiten"), myFrame);
      }

    });
    menu.add(button);
    
    menu.show(invoker, x, y);
  }
  
  /**
   * �ffnet den Dialog zum Einf�gen eines Spezialfeldes, das �ber die Funktion
   * trafoConf beschrieben ist, erzeugt daraus ein transformiertes Feld und f�gt
   * dieses Feld in das Dokument mod ein; Es erwartet dar�ber hinaus den Namen
   * des Buttons buttonName, aus dem das Label des Dialogs, und sp�ter der
   * Mouse-Over hint erzeugt wird und die Liste der aktuellen Felder, die evtl.
   * im Dialog zur Verf�gung stehen sollen.
   * 
   * @param fieldNames
   *          Eine Liste der Feldnamen, die der Dialog anzeigt, falls er Buttons
   *          zum Einf�gen von Serienbrieffeldern bereitstellt.
   * @param buttonName
   *          Der Name des Buttons, aus dem die Titelzeile des Dialogs und der
   *          Mouse-Over Hint des neu erzeugten Formularfeldes generiert wird.
   * @param trafoConf
   *          ConfigThingy, das die Funktion und damit den aufzurufenden Dialog
   *          spezifiziert. Der von den Dialogen ben�tigte �u�ere Knoten
   *          "Func(...trafoConf...) wird dabei von dieser Methode erzeugt, so
   *          dass trafoConf nur die eigentliche Funktion darstellen muss.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  protected void insertFieldFromTrafoDialog(List<String> fieldNames,
      final String buttonName, ConfigThingy trafoConf)
  {
    TrafoDialogParameters params = new TrafoDialogParameters();
    params.conf = new ConfigThingy("Func");
    params.conf.addChild(trafoConf);
    params.isValid = true;
    params.fieldNames = fieldNames;
    params.closeAction = new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        TrafoDialog dialog = (TrafoDialog) e.getSource();
        TrafoDialogParameters status = dialog.getExitStatus();
        if (status.isValid)
        {
          try
          {
            mod.replaceSelectionWithTrafoField(status.conf, buttonName);
          }
          catch (Exception x)
          {
            Logger.error(x);
          }
        }
      }
    };

    try
    {
      TrafoDialogFactory.createDialog(params).show(
        L.m("Spezialfeld %1 einf�gen", buttonName), myFrame);
    }
    catch (UnavailableException e)
    {
      Logger.error(L.m("Das darf nicht passieren!"));
    }
  }

  /**
   * Pr�ft, ob sich in der akutellen Selektion ein transformiertes Feld befindet
   * und liefert ein mit Hilfe der TrafoDialogFactory erzeugtes zugeh�riges
   * TrafoDialog-Objekt zur�ck, oder null, wenn keine transformierte Funktion
   * selektiert ist oder f�r die Trafo kein Dialog existiert.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private TrafoDialog getTrafoDialogForCurrentSelection()
  {
    ConfigThingy trafoConf = mod.getFormFieldTrafoFromSelection();
    if (trafoConf == null) return null;

    final String trafoName = trafoConf.getName();

    TrafoDialogParameters params = new TrafoDialogParameters();
    params.conf = trafoConf;
    params.isValid = true;
    params.fieldNames = ds.getColumnNames();
    params.closeAction = new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        TrafoDialog dialog = (TrafoDialog) e.getSource();
        TrafoDialogParameters status = dialog.getExitStatus();
        if (status.isValid)
        {
          try
          {
            mod.setTrafo(trafoName, status.conf);
          }
          catch (Exception x)
          {
            Logger.error(x);
          }
        }
      }
    };

    try
    {
      return TrafoDialogFactory.createDialog(params);
    }
    catch (UnavailableException e)
    {
      return null;
    }
  }
  
  /**
   * F�hrt den Seriendruck durch.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void doMailMerge()
  {
    //TODO Fortschrittsanzeiger
    //TODO hier kann man mit lockControllers auf das Gesamtdokument vielleicht noch etwas Performance rausholen - das bitte testen.
    mod.collectNonWollMuxFormFields();
    QueryResultsWithSchema data = ds.getData();
    final XPrintModel pmod = mod.createPrintModel(true);
    try{
      pmod.setPropertyValue("MailMergeNew_Schema", data.getSchema());
      pmod.setPropertyValue(PROP_QUERYRESULTS, data);
    }catch(Exception x)
    {
      Logger.error(x);
      return;
    }
    pmod.usePrintFunction("MailMergeNewSetFormValue");
    if (printIntoDocument) pmod.usePrintFunction("Gesamtdokument");

    // Drucken im Hintergrund, damit der EDT nicht blockiert.
    new Thread()
    {
      public void run()
      {
        mod.setFormFieldsPreviewMode(true);
        pmod.printWithProps();
        mod.setFormFieldsPreviewMode(previewMode);
      }
    }.start();
  }
  
  
  /**
   * PrintFunction, die das jeweils n�chste Element der Seriendruckdaten
   * nimmt und die Seriendruckfelder im Dokument entsprechend setzt.
   * Herangezogen werden die Properties {@link #PROP_QUERYRESULTS}
   * (ein Objekt vom Typ {@link QueryResults}) und 
   * "MailMergeNew_Schema", was ein Set mit den Spaltennamen enth�lt.
   * Dies funktioniert nat�rlich nur dann, wenn pmod kein Proxy ist.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  public static void mailMergeNewSetFormValue(XPrintModel pmod) throws Exception
  {
    QueryResults data = (QueryResults)pmod.getPropertyValue(PROP_QUERYRESULTS);
    Collection schema = (Collection)pmod.getPropertyValue("MailMergeNew_Schema");
    
    Iterator iter = data.iterator();
    
    while (iter.hasNext())
    {
      Dataset ds = (Dataset)iter.next();
      Iterator schemaIter = schema.iterator();
      while (schemaIter.hasNext())
      {
        String spalte = (String)schemaIter.next();
        pmod.setFormValue(spalte, ds.get(spalte));
      }
      pmod.printWithProps();
    }
  }
  
  
  /**
   * Liefert die sichtbaren Zellen des Arbeitsblattes mit Namen sheetName aus dem Calc 
   * Dokument doc. Die erste sichtbare Zeile der Calc-Tabelle wird herangezogen
   * als Spaltennamen. Diese Spaltennamen werden zu schema hinzugef�gt.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  private static QueryResults getVisibleCalcData(XSpreadsheetDocument doc, String sheetName, Set<String> schema)
  {
    CalcCellQueryResults results = new CalcCellQueryResults();
    
    try{
      if (doc != null)
      {
        XCellRangesQuery sheet = UNO.XCellRangesQuery(doc.getSheets().getByName(sheetName));
        if (sheet != null)
        {
          SortedSet<Integer> columnIndexes = new TreeSet<Integer>();
          SortedSet<Integer> rowIndexes = new TreeSet<Integer>();
          getVisibleNonemptyRowsAndColumns(sheet, columnIndexes, rowIndexes);
          
          if (columnIndexes.size() > 0 && rowIndexes.size() > 0)
          {
            XCellRange sheetCellRange = UNO.XCellRange(sheet);
            
            /*
             * Erste sichtbare Zeile durchscannen und alle nicht-leeren Zelleninhalte als
             * Tabellenspaltennamen interpretieren. Ein Mapping in
             * mapColumnNameToIndex wird erzeugt, wobei NICHT auf den Index in
             * der Calc-Tabelle gemappt wird, sondern auf den Index im sp�ter f�r jeden
             * Datensatz existierenden String[]-Array.
             */
            int ymin = rowIndexes.first().intValue();
            Map<String, Integer> mapColumnNameToIndex = new HashMap<String, Integer>();
            int idx = 0;
            Iterator<Integer> iter = columnIndexes.iterator();
            while (iter.hasNext())
            {
              int x = iter.next().intValue();
              String columnName = UNO.XTextRange(sheetCellRange.getCellByPosition(x, ymin)).getString();
              if (columnName.length() > 0)
              {
                mapColumnNameToIndex.put(columnName, new Integer(idx));
                schema.add(columnName);
                ++idx;  
              }
              else 
                iter.remove(); //Spalten mit leerem Spaltennamen werden nicht ben�tigt.
            }
            
            results.setColumnNameToIndexMap(mapColumnNameToIndex);
            
            /*
             * Datens�tze erzeugen
             */
            Iterator<Integer> rowIndexIter = rowIndexes.iterator();
            rowIndexIter.next(); //erste Zeile enth�lt die Tabellennamen, keinen Datensatz
            while (rowIndexIter.hasNext())
            {
              int y = rowIndexIter.next().intValue();
              String[] data = new String[columnIndexes.size()];
              Iterator<Integer> columnIndexIter = columnIndexes.iterator();
              idx = 0;
              while (columnIndexIter.hasNext())
              {
                int x = columnIndexIter.next().intValue();
                String value = UNO.XTextRange(sheetCellRange.getCellByPosition(x, y)).getString();
                data[idx++] = value;
              }
              
              results.addDataset(data);
            }
          }
        }
      }
    }catch(Exception x)
    {
      Logger.error(x);
    }
    
    return results;
  }

  
  /**
   * Liefert von Tabellenblatt sheet die Indizes aller Zeilen und Spalten, in denen
   * mindestens eine sichtbare nicht-leere Zelle existiert.
   * @param sheet das zu scannende Tabellenblatt
   * @param columnIndexes diesem Set werden die Spaltenindizes hinzugef�gt
   * @param rowIndexes diesem Set werden die Zeilenindizes hinzugef�gt
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private static void getVisibleNonemptyRowsAndColumns(XCellRangesQuery sheet, SortedSet<Integer> columnIndexes, SortedSet<Integer> rowIndexes)
  {
    XSheetCellRanges visibleCellRanges = sheet.queryVisibleCells();
    XSheetCellRanges nonEmptyCellRanges = sheet
        .queryContentCells((short) ( com.sun.star.sheet.CellFlags.VALUE
                                   | com.sun.star.sheet.CellFlags.DATETIME
                                   | com.sun.star.sheet.CellFlags.STRING 
                                   | com.sun.star.sheet.CellFlags.FORMULA));
    CellRangeAddress[] nonEmptyCellRangeAddresses = nonEmptyCellRanges.getRangeAddresses();
    for (int i = 0; i < nonEmptyCellRangeAddresses.length; ++i)
    {
      XSheetCellRanges ranges = UNO.XCellRangesQuery(visibleCellRanges).queryIntersection(nonEmptyCellRangeAddresses[i]);
      CellRangeAddress[] rangeAddresses = ranges.getRangeAddresses();
      for (int k = 0; k < rangeAddresses.length; ++k)
      {
        CellRangeAddress addr = rangeAddresses[k];
        for (int x = addr.StartColumn; x <= addr.EndColumn; ++x)
          columnIndexes.add(new Integer(x));
        
        for (int y = addr.StartRow; y <= addr.EndRow; ++y)
          rowIndexes.add(new Integer(y));
      }
    }
  }


  
  /**
   * Stellt eine OOo-Datenquelle oder ein offenes Calc-Dokument �ber ein gemeinsames
   * Interface zur Verf�gung. Ist auch zust�ndig daf�r, das Calc-Dokument falls n�tig
   * wieder zu �ffnen und �nderungen seines Fenstertitels und/oder seiner
   * Speicherstelle zu �berwachen. Stellt auch
   * Dialoge zur Verf�gung zur Auswahl der Datenquelle.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private static class MailMergeDatasource
  {
    /**
     * Wert f�r {@link #sourceType}, der anzeigt, dass keine Datenquelle ausgew�hlt ist.
     */
    private static final int SOURCE_NONE = 0;
    
    /**
     * Wert f�r {@link #sourceType}, der anzeigt, dass eine Calc-Tabelle als Datenquelle 
     * ausgew�hlt ist.
     */
    private static final int SOURCE_CALC = 1;
    
    /**
     * Wert f�r {@link #sourceType}, der anzeigt, dass eine OOo Datenquelle
     * als Datenquelle ausgew�hlt ist.
     */
    private static final int SOURCE_DB = 2;

    /**
     * Wenn nach dieser Zeit in ms nicht alle Daten des Seriendruckauftrags
     * ausgelesen werden konnten, dann wird der Druckauftrag nicht ausgef�hrt
     * (und muss eventuell �ber die Von Bis Auswahl in mehrere Auftr�ge
     * zerteilt werden). 
     */
    private static final long MAILMERGE_GETCONTENTS_TIMEOUT = 60000;
    
    /**
     * Zeigt an, was derzeit als Datenquelle ausgew�hlt ist.
     */
    private int sourceType = SOURCE_NONE;
    
    /**
     * Wenn {@link #sourceType} == {@link #SOURCE_CALC} und das Calc-Dokument derzeit
     * offen ist, dann ist diese Variable != null. Falls das Dokument nicht offen ist,
     * so ist seine URL in {@link #calcUrl} zu finden. Die Kombination 
     * calcDoc == null && calcUrl == null && sourceType == SOURCE_CALC ist
     * unzul�ssig.
     */
    private XSpreadsheetDocument calcDoc = null;
    
    /**
     * Wenn {@link #sourceType} == {@link #SOURCE_CALC} und das Calc-Dokument bereits
     * einmal gespeichert wurde, findet sich hier die URL des Dokuments, ansonsten
     * ist der Wert null. Falls das
     * Dokument nur als UnbenanntX im Speicher existiert, so ist eine
     * Referenz auf das Dokument in {@link #calcDoc} zu finden.
     * Die Kombination 
     * calcDoc == null && calcUrl == null && sourceType == SOURCE_CALC ist
     * unzul�ssig.
     */
    private String calcUrl = null;
    
    /**
     * Falls {@link #sourceType} == {@link #SOURCE_DB}, so ist hier der Name der
     * ausgew�hlten OOo-Datenquelle gespeichert, ansonsten null.
     */
    private String oooDatasourceName = null;
    
    /**
     * Speichert den Namen der Tabelle bzw, des Tabellenblattes, die als
     * Quelle der Serienbriefdaten ausgew�hlt wurde. Ist niemals null, kann
     * aber der leere String sein oder ein Name, der gar nicht in der
     * entsprechenden Datenquelle existiert.
     */
    private String tableName = "";
    
    /**
     * Wenn als aktuelle Datenquelle ein Calc-Dokument ausgew�hlt ist, dann
     * wird dieser Listener darauf registriert um �nderungen des Speicherorts,
     * so wie das Schlie�en des Dokuments zu �berwachen.
     */
    private MyCalcListener myCalcListener = new MyCalcListener();
    
    /**
     * Wird verwendet zum Speichern/Wiedereinlesen der zuletzt ausgew�hlten
     * Datenquelle.
     */
    private TextDocumentModel mod;
    
    /**
     * Erzeugt eine neue Datenquelle.
     * @param mod wird verwendet zum Speichern/Wiedereinlesen der zuletzt ausgew�hlten
     *        Datenquelle.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public MailMergeDatasource(TextDocumentModel mod)
    {
      this.mod = mod;      
      openDatasourceFromLastStoredSettings();
    }

    /**
     * �ffnet die Datenquelle die durch einen fr�heren Aufruf von
     * storeDatasourceSettings() im Dokument hinterlegt wurde.
     * 
     * @author Christoph Lutz (D-III-ITD-5.1) TODO: Testen
     */
    private void openDatasourceFromLastStoredSettings()
    {
      ConfigThingy mmconf = mod.getMailmergeConfig();
      ConfigThingy datenquelle = new ConfigThingy("");
      try{
        datenquelle = mmconf.query("Datenquelle").getLastChild();
      }catch (NodeNotFoundException e){}
      
      String type = null;
      try{
        type = datenquelle.get("TYPE").toString();
      }catch (NodeNotFoundException e){}
      
      if("calc".equalsIgnoreCase(type)) {
        try{
          String url = datenquelle.get("URL").toString();
          String table = datenquelle.get("TABLE").toString();
          try{
            Object d = getCalcDoc(url);
            if(d != null) setTable(table);
          }catch (UnavailableException e){Logger.debug(e);}
        }catch (NodeNotFoundException e){
          Logger.error(L.m("Fehlendes Argument f�r Datenquelle vom Typ '%1':", type), e);
        }
      } else if("ooo".equalsIgnoreCase(type)) {
        try{
          @SuppressWarnings("unused") String source = datenquelle.get("SOURCE").toString();
          @SuppressWarnings("unused") String table = datenquelle.get("TABLE").toString();
          // TODO: bestehende OOo-Datenbank verwenden
        }catch (NodeNotFoundException e){
          Logger.error(L.m("Fehlendes Argument f�r Datenquelle vom Typ '%1':", type), e);
        }        
      } else if (type != null) {
        Logger.error(L.m("Ignoriere Datenquelle mit unbekanntem Typ '%1'", type));
      }
    }

    /**
     * Speichert die aktuellen Einstellungen zu dieser Datenquelle im
     * zugeh�rigen Dokument persistent ab, damit die Datenquelle beim n�chsten
     * mal wieder automatisch ge�ffnet/verbunden werden kann.
     * 
     * @author Christoph Lutz (D-III-ITD-5.1) TODO: Testen
     */
    private void storeDatasourceSettings()
    {
      // ConfigThingy f�r Einstellungen der Datenquelle erstellen:
      ConfigThingy dq = new ConfigThingy("Datenquelle");
      ConfigThingy arg;
      switch (sourceType)
      {
        case SOURCE_CALC:
          if(calcUrl == null || tableName.length() == 0) break;
          arg = new ConfigThingy("TYPE");
          arg.addChild(new ConfigThingy("calc"));
          dq.addChild(arg);
          arg = new ConfigThingy("URL");
          arg.addChild(new ConfigThingy(calcUrl));
          dq.addChild(arg);
          arg = new ConfigThingy("TABLE");
          arg.addChild(new ConfigThingy(tableName));
          dq.addChild(arg);
          break;
        case SOURCE_DB:
          if(oooDatasourceName == null || tableName.length() == 0) break;
          arg = new ConfigThingy("TYPE");
          arg.addChild(new ConfigThingy("ooo"));
          dq.addChild(arg);
          arg = new ConfigThingy("SOURCE");
          arg.addChild(new ConfigThingy(oooDatasourceName));
          dq.addChild(arg);
          arg = new ConfigThingy("TABLE");
          arg.addChild(new ConfigThingy(tableName));
          dq.addChild(arg);
          break;
      }

      ConfigThingy seriendruck = new ConfigThingy("Seriendruck");
      if(dq.count() > 0) seriendruck.addChild(dq);
      mod.setMailmergeConfig(seriendruck);
    }    
    
    /**
     * Liefert die Titel der Spalten der aktuell ausgew�hlten Tabelle.
     * Ist derzeit keine Tabelle ausgew�hlt oder enth�lt die ausgew�hlte
     * Tabelle keine benannten Spalten, so wird ein leerer Vector geliefert.
     * @return
     * @author Matthias Benkmann (D-III-ITD 5.1)
     * TESTED
     */
    public List<String> getColumnNames()
    {
      try{
        switch(sourceType)
        {
          case SOURCE_CALC: return getColumnNames(getCalcDoc(), tableName);
          case SOURCE_DB: return getDbColumnNames(oooDatasourceName, tableName);
          default: return new Vector<String>();
        }
      }catch(Exception x)
      {
        Logger.error(x);
        return new Vector<String>();
      }
    }
    
    
    /**
     * Liefert die Spaltennamen der Tabelle tableName aus der
     * OOo-Datenquelle oooDatasourceName.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     * TODO Testen
     */
    private List<String> getDbColumnNames(String oooDatasourceName, String tableName)
    {
      return new Vector<String>(); //FIXME: getDbColumnNames()
    }

    /**
     * Liefert die Inhalte (als Strings) der nicht-leeren Zellen der ersten
     * sichtbaren Zeile von Tabellenblatt tableName in Calc-Dokument calcDoc.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     * TESTED
     */
    private List<String> getColumnNames(XSpreadsheetDocument calcDoc, String tableName)
    {
      List<String> columnNames = new Vector<String>();
      if (calcDoc == null) return columnNames;
      try{
        XCellRangesQuery sheet = UNO.XCellRangesQuery(calcDoc.getSheets().getByName(tableName));
        SortedSet<Integer> columnIndexes = new TreeSet<Integer>();
        SortedSet<Integer> rowIndexes = new TreeSet<Integer>();
        getVisibleNonemptyRowsAndColumns(sheet, columnIndexes, rowIndexes);
        
        if (columnIndexes.size() > 0 && rowIndexes.size() > 0)
        {
          XCellRange sheetCellRange = UNO.XCellRange(sheet);
          
          /*
           * Erste sichtbare Zeile durchscannen und alle nicht-leeren Zelleninhalte als
           * Tabellenspaltennamen interpretieren. 
           */
          int ymin = rowIndexes.first().intValue();
          Iterator<Integer> iter = columnIndexes.iterator();
          while (iter.hasNext())
          {
            int x = iter.next().intValue();
            String columnName = UNO.XTextRange(sheetCellRange.getCellByPosition(x, ymin)).getString();
            if (columnName.length() > 0)
            {
              columnNames.add(columnName);
            }
          }
        }        
      }catch(Exception x)
      {
        Logger.error(L.m("Kann Spaltennamen nicht bestimmen"),x);
      }
      return columnNames;  
    }
    
    /**
     * Liefert den Inhalt der aktuell ausgew�hlten Serienbriefdatenquelle (leer, wenn
     * keine ausgew�hlt).
     * @author Matthias Benkmann (D-III-ITD 5.1)
     * TODO Testen
     */
    public QueryResultsWithSchema getData()
    {
      try{
        switch(sourceType)
        {
          case SOURCE_CALC: return getData(getCalcDoc(), tableName);
          case SOURCE_DB: return getDbData(oooDatasourceName, tableName);
          default: return new QueryResultsWithSchema();
        }
      }catch(Exception x)
      {
        Logger.error(x);
        return new QueryResultsWithSchema();
      }
    }

    /**
     * Liefert den Inhalt der Tabelle tableName aus der OOo Datenquelle 
     * mit Namen oooDatasourceName.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     * TODO Testen
     */
    private QueryResultsWithSchema getDbData(String oooDatasourceName, String tableName) throws Exception, ConfigurationErrorException
    {
      ConfigThingy conf = new ConfigThingy("Datenquelle");
      conf.add("NAME").add("Knuddel");
      conf.add("TABLE").add(tableName);
      conf.add("SOURCE").add(oooDatasourceName);
      Datasource ds;
      ds = new OOoDatasource(new HashMap(),conf,new URL("file:///"), true);
      
      Set<String> schema = ds.getSchema();
      QueryResults res = ds.getContents(MAILMERGE_GETCONTENTS_TIMEOUT);
      return new QueryResultsWithSchema(res, schema);
    }

    /**
     * Liefert die sichtbaren Zellen aus der Tabelle tableName des Dokuments
     * calcDoc als QueryResultsWithSchema zur�ck.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     * TODO Testen
     */
    private QueryResultsWithSchema getData(XSpreadsheetDocument calcDoc, String tableName)
    {
      Set<String> schema = new HashSet<String>();
      QueryResults res = getVisibleCalcData(calcDoc, tableName, schema);
      return new QueryResultsWithSchema(res, schema);
    }
    
    /**
     * Liefert true, wenn derzeit eine Datenquelle ausgew�hlt ist.
     * @return
     * @author Matthias Benkmann (D-III-ITD 5.1)
     * TODO Testen
     */
    public boolean hasDatasource()
    {
      return sourceType != SOURCE_NONE;
    }

    /**
     * L�sst den Benutzer �ber einen Dialog die Datenquelle ausw�hlen.
     * @param parent der JFrame, zu dem dieser Dialog geh�ren soll.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     * TESTED
     */
    public void showDatasourceSelectionDialog(final JFrame parent)
    {
      final JDialog datasourceSelector = new JDialog(parent, L.m("Wo sind Ihre Serienbriefdaten ?"), true);
      
      Box vbox = Box.createVerticalBox();
      datasourceSelector.add(vbox);
      
      JLabel label = new JLabel(L.m("Wo sind Ihre Serienbriefdaten ?"));
      vbox.add(label);
      
      JButton button;
      button = createDatasourceSelectorCalcWindowButton();
      if (button != null) 
      {
        button.addActionListener(new ActionListener(){
          public void actionPerformed(ActionEvent e)
          {
            datasourceSelector.dispose();
            selectOpenCalcWindowAsDatasource(parent);
          }});
        vbox.add(DimAdjust.maxWidthUnlimited(button));
      }
      
      button = new JButton(L.m("Datei..."));
      button.addActionListener(new ActionListener(){
        public void actionPerformed(ActionEvent e)
        {
          datasourceSelector.dispose();
          selectFileAsDatasource(parent);
        }
      });
      vbox.add(DimAdjust.maxWidthUnlimited(button));
      
      button = new JButton(L.m("Neue Calc-Tabelle..."));
      button.addActionListener(new ActionListener(){
        public void actionPerformed(ActionEvent e)
        {
          datasourceSelector.dispose();
          openAndselectNewCalcTableAsDatasource(parent);
        }
      });
      vbox.add(DimAdjust.maxWidthUnlimited(button));
      
      button = new JButton(L.m("Datenbank..."));
      button.addActionListener(new ActionListener(){
        public void actionPerformed(ActionEvent e)
        {
          //TODO selectOOoDatasourceAsDatasource();
        }
      });
      vbox.add(DimAdjust.maxWidthUnlimited(button));
      
      label = new JLabel(L.m("Aktuell ausgew�hlte Tabelle"));
      vbox.add(label);
      String str = L.m("<keine>");
      if (sourceType == SOURCE_CALC)
      { //TODO Testen
        if (calcDoc != null)
        {
          String title = (String)UNO.getProperty(UNO.XModel(calcDoc).getCurrentController().getFrame(),"Title");
          if (title == null) title = "?????";
          str = stripOpenOfficeFromWindowName(title);
        }
        else
        {
          str = calcUrl;
        }
      } else if (sourceType == SOURCE_DB)
      {
        str = oooDatasourceName;
      }

      if (tableName.length() > 0)
        str = str + "." + tableName;
      
      label = new JLabel(str);
      vbox.add(label);
      
      button = new JButton(L.m("Abbrechen"));
      button.addActionListener(new ActionListener(){
        public void actionPerformed(ActionEvent e)
        {
          datasourceSelector.dispose();
        }
      });
      vbox.add(DimAdjust.maxWidthUnlimited(button));
      
      datasourceSelector.pack();
      int frameWidth = datasourceSelector.getWidth();
      int frameHeight = datasourceSelector.getHeight();
      Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
      int x = screenSize.width/2 - frameWidth/2; 
      int y = screenSize.height/2 - frameHeight/2;
      datasourceSelector.setLocation(x,y);
      datasourceSelector.setResizable(false);
      datasourceSelector.setVisible(true);
    }
    
    /**
     * Pr�sentiert dem Benutzer einen Dialog, in dem er aus allen offenen Calc-Fenstern
     * eines als Datenquelle ausw�hlen kann. Falls es nur ein
     * offenes Calc-Fenster gibt, wird dieses automatisch gew�hlt.
     * 
     * @param parent der JFrame zu dem der die Dialoge geh�ren sollen.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     * TESTED
     */
    private void selectOpenCalcWindowAsDatasource(final JFrame parent)
    {
      OpenCalcWindows win = getOpenCalcWindows();
      List<String> names = win.titles;
      
      if (names.isEmpty()) return;
      
      if (names.size() == 1)
      {
        getCalcDoc(win.docs.get(0));
        selectTable(parent);
        return;
      }
      
      final JDialog calcWinSelector = new JDialog(parent, L.m("Welche Tabelle m�chten Sie verwenden ?"), true);
      
      Box vbox = Box.createVerticalBox();
      calcWinSelector.add(vbox);
      
      JLabel label = new JLabel(L.m("Welches Calc-Dokument m�chten Sie verwenden ?"));
      vbox.add(label);
      
      for (int i = 0; i < names.size(); ++i)
      {
        final String name = names.get(i);
        final XSpreadsheetDocument spread = win.docs.get(i);
        JButton button;
        button = new JButton(name);
        button.addActionListener(new ActionListener(){
          public void actionPerformed(ActionEvent e)
          {
            calcWinSelector.dispose();
            getCalcDoc(spread);
            selectTable(parent);
          }
        });
        vbox.add(DimAdjust.maxWidthUnlimited(button));
      }
      
      calcWinSelector.pack();
      int frameWidth = calcWinSelector.getWidth();
      int frameHeight = calcWinSelector.getHeight();
      Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
      int x = screenSize.width/2 - frameWidth/2; 
      int y = screenSize.height/2 - frameHeight/2;
      calcWinSelector.setLocation(x,y);
      calcWinSelector.setResizable(false);
      calcWinSelector.setVisible(true);
    }
    
    /**
     * �ffnet ein neues Calc-Dokument und setzt es als Seriendruckdatenquelle.
     * 
     * @param parent der JFrame zu dem der die Dialoge geh�ren sollen.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     * TESTED
     */
    private void openAndselectNewCalcTableAsDatasource(JFrame parent)
    {
      try
      {
        Logger.debug(L.m("�ffne neues Calc-Dokument als Datenquelle f�r Seriendruck"));
        XSpreadsheetDocument spread = UNO.XSpreadsheetDocument(UNO.loadComponentFromURL("private:factory/scalc", true, true));
        XSpreadsheets sheets = spread.getSheets();
        String[] sheetNames = sheets.getElementNames();

        // L�sche alle bis auf das erste Tabellenblatt ohne �nderung des
        // Modified-Status.
        XModifiable xmo = UNO.XModifiable(spread);
        boolean modified = (xmo != null)? xmo.isModified() : false;
        for (int i = 1; i < sheetNames.length; ++i)
          sheets.removeByName(sheetNames[i]);
        if(xmo != null) xmo.setModified(modified);
        
        getCalcDoc(spread);
        selectTable(parent);
      }
      catch (Exception e)
      {
        Logger.error(e);
      }
    }
    
    /**
     * �ffnet einen FilePicker und falls der Benutzer dort eine Tabelle ausw�hlt, wird diese
     * ge�ffnet und als Datenquelle verwendet.
     * 
     * @param parent der JFrame zu dem der die Dialoge geh�ren sollen.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     * TESTED
     */
    private void selectFileAsDatasource(JFrame parent)
    {
      XFilePicker picker = UNO.XFilePicker(UNO.createUNOService("com.sun.star.ui.dialogs.FilePicker"));
      short res = picker.execute();
      if (res == com.sun.star.ui.dialogs.ExecutableDialogResults.OK)
      {
        String[] files = picker.getFiles();
        if (files.length == 0) return;
        try
        {
          Logger.debug(L.m("�ffne %1 als Datenquelle f�r Seriendruck", files[0]));
          try{
            getCalcDoc(files[0]);
          }catch(UnavailableException x)
          {
            return;
          }
          selectTable(parent);
        }
        catch (Exception e)
        {
          Logger.error(e);
        }
      }
    }
    
    /**
     * Bringt einen Dialog, mit dem der Benutzer in der aktuell ausgew�hlten
     * Datenquelle eine Tabelle ausw�hlen kann. Falls die Datenquelle genau eine
     * nicht-leere Tabelle hat, so wird diese ohne Dialog automatisch ausgew�hlt.
     * Falls der Benutzer den Dialog abbricht, so wird die erste Tabelle gew�hlt.
     * 
     * @author Matthias Benkmann (D-III-ITD 5.1)
     * @parent Das Hauptfenster, zu dem dieser Dialog geh�rt.
     * TESTED
     */
    private void selectTable(JFrame parent)
    {
      List<String> names = getNamesOfNonEmptyTables();
      List<String> allNames = getTableNames();
      if (allNames.isEmpty())
      {
        setTable("");
        return;
      }
      if (names.isEmpty()) names = allNames;
      
      setTable(names.get(0)); //Falls der Benutzer den Dialog abbricht ohne Auswahl
      
      if (names.size() == 1) return; //Falls es nur eine Tabelle gibt, Dialog unn�tig.
      
      final JDialog tableSelector = new JDialog(parent, L.m("Welche Tabelle m�chten Sie verwenden ?"), true);
      
      Box vbox = Box.createVerticalBox();
      tableSelector.add(vbox);
      
      JLabel label = new JLabel(L.m("Welche Tabelle m�chten Sie verwenden ?"));
      vbox.add(label);
      
      Iterator<String> iter = names.iterator();
      while (iter.hasNext())
      {
        final String name = iter.next();
        JButton button;
        button = new JButton(name);
        button.addActionListener(new ActionListener(){
          public void actionPerformed(ActionEvent e)
          {
            tableSelector.dispose();
            setTable(name);
          }
        });
        vbox.add(DimAdjust.maxWidthUnlimited(button));
      }
      
      tableSelector.pack();
      int frameWidth = tableSelector.getWidth();
      int frameHeight = tableSelector.getHeight();
      Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
      int x = screenSize.width/2 - frameWidth/2; 
      int y = screenSize.height/2 - frameHeight/2;
      tableSelector.setLocation(x,y);
      tableSelector.setResizable(false);
      tableSelector.setVisible(true);
    }

    /**
     * Setzt die zu verwendende Tabelle auf den Namen name und speichert die
     * Einstellungen persistent im zugeh�rigen Dokument ab, damit sie bei der
     * n�chsten Bearbeitung des Dokuments wieder verf�gbar sind.
     * 
     * @param name Name der Tabelle die aktuell eingestellt werden soll.
     * 
     * @author Christoph Lutz (D-III-ITD-5.1)
     */
    private void setTable(String name)
    {
      if (name == null) tableName = "";
      else tableName = name;
      storeDatasourceSettings();
    }

    /**
     * Registriert {@link #myCalcListener} auf calcDoc, falls calcDoc != null.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    private void setListeners(XSpreadsheetDocument calcDoc)
    {
      //FIXME: Das �ndern des Names eines Sheets muss �berwacht werden damit tableName angepasst wird.
      if (calcDoc == null) return;
      try{
        UNO.XCloseBroadcaster(calcDoc).addCloseListener(myCalcListener);
      }catch(Exception x)
      {
        Logger.error(L.m("Kann CloseListener nicht auf Calc-Dokument registrieren"),x);
      }
      try{
        UNO.XEventBroadcaster(calcDoc).addEventListener(myCalcListener);
      }catch(Exception x)
      {
        Logger.error(L.m("Kann EventListener nicht auf Calc-Dokument registrieren"),x);
      }
    }

    /**
     * Falls calcDoc != null wird versucht, {@link #myCalcListener} davon zu deregistrieren.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    private void removeListeners(XSpreadsheetDocument calcDoc)
    {
      if (calcDoc == null) return;
      
      try{
        UNO.XCloseBroadcaster(calcDoc).removeCloseListener(myCalcListener);
      }catch(Exception x)
      {
        Logger.error(L.m("Konnte alten XCloseListener nicht deregistrieren"),x);
      }
      try{
        UNO.XEventBroadcaster(calcDoc).removeEventListener(myCalcListener);
      }catch(Exception x)
      {
        Logger.error(L.m("Konnte alten XEventListener nicht deregistrieren"),x);
      }
      
    }

    private static String stripOpenOfficeFromWindowName(String str)
    {
      int idx = str.indexOf(" - OpenOffice");
      //FIXME: kann unter StarOffice nat�rlich anders heissen oder bei einer anderen Office-Version
      if (idx > 0) str = str.substring(0, idx);
      return str;
    }
    
    /**
     * Erzeugt einen Button zur Auswahl der Datenquelle aus den aktuell offenen Calc-Fenstern,
     * dessen Beschriftung davon abh�ngt, was zur Auswahl steht oder liefert null, wenn nichts
     * zur Auswahl steht.
     * Falls es keine offenen Calc-Fenster gibt, wird null geliefert.
     * Falls es genau ein offenes Calc-Fenster gibt und dieses genau ein 
     * nicht-leeres Tabellenblatt hat,
     * so zeigt der Button die Beschriftung "<Fenstername>.<Tabellenname>".
     * Falls es genau ein offenes Calc-Fenster gibt und dieses mehr als ein nicht-leeres
     * oder kein nicht-leeres Tabellenblatt hat, so zeigt der Button die Beschriftung 
     * "<Fenstername>".
     * Falls es mehrere offene Calc-Fenster gibt, so zeigt der Button die Beschriftung
     * "Offenes Calc-Fenster...".
     * @return JButton oder null.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     * TESTED
     */
    private JButton createDatasourceSelectorCalcWindowButton()
    {
      OpenCalcWindows win = getOpenCalcWindows();
      
      if (win.titles.isEmpty()) return null;
      if (win.titles.size() > 1) return new JButton(L.m("Offenes Calc-Fenster..."));
      
      //Es gibt offenbar genau ein offenes Calc-Fenster
      //das XSpreadsheetDocument dazu ist in calcSheet zu finden
      List<String> nonEmptyTableNames = getNamesOfNonEmptyTables(win.docs.get(0));
      
      String str = win.titles.get(0);
      if (nonEmptyTableNames.size() == 1) str = str + "." + nonEmptyTableNames.get(0);
      
      return new JButton(str);
    }

    private static class OpenCalcWindows
    {
      public List<String> titles;
      public List<XSpreadsheetDocument> docs;
    }
    
    /**
     * Liefert die Titel und zugeh�rigen XSpreadsheetDocuments aller offenen Calc-Fenster.
     * @return ein Objekt mit 2 Elementen. Das erste ist eine Liste aller Titel von Calc-Fenstern,
     *         wobei jeder Titel bereits mit {@link #stripOpenOfficeFromWindowName(String)}
     *         bearbeitet wurde. Das zweite Element ist eine Liste von
     *         XSpreadsheetDocuments, wobei jeder Eintrag zum Fenstertitel mit dem selben
     *         Index in der ersten Liste geh�rt.
     *         Im Fehlerfalle sind beide Listen leer.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    private OpenCalcWindows getOpenCalcWindows()
    {
      OpenCalcWindows win = new OpenCalcWindows();
      win.titles = new Vector<String>();
      win.docs = new Vector<XSpreadsheetDocument>();
      try{
        XSpreadsheetDocument spread = null;
        XEnumeration xenu = UNO.desktop.getComponents().createEnumeration();
        while(xenu.hasMoreElements())
        {
          spread = UNO.XSpreadsheetDocument(xenu.nextElement());
          if (spread != null)
          {
            String title = (String)UNO.getProperty(UNO.XModel(spread).getCurrentController().getFrame(),"Title");
            win.titles.add(stripOpenOfficeFromWindowName(title));
            win.docs.add(spread);
          }
        }
      }catch(Exception x)
      {
        Logger.error(x);
      }
      return win;
    }

    /**
     * Falls aktuell eine Calc-Tabelle als Datenquelle ausgew�hlt ist, so
     * wird versucht, diese zur�ckzuliefern. Falls n�tig wird die Datei
     * anhand von {@link #calcUrl} neu ge�ffnet. Falls es aus irgendeinem
     * Grund nicht m�glich ist, diese zur�ckzuliefern, wird eine
     * {@link de.muenchen.allg.itd51.wollmux.UnavailableException} geworfen.
     * ACHTUNG! Das zur�ckgelieferte Objekt k�nnte bereits disposed sein!
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    private XSpreadsheetDocument getCalcDoc() throws UnavailableException
    {
      if (sourceType != SOURCE_CALC) throw new UnavailableException(L.m("Keine Calc-Tabelle ausgew�hlt"));
      if (calcDoc != null) return calcDoc;
      return getCalcDoc(calcUrl);
    }
    
    /**
     * Falls url bereits offen ist oder ge�ffnet werden kann und ein
     * Tabellendokument ist, so wird der {@link #sourceType} auf 
     * {@link #SOURCE_CALC} gestellt und die Calc-Tabelle als neue
     * Datenquelle ausgew�hlt.
     * @return das Tabellendokument
     * @throws UnavailableException falls ein Fehler auftritt oder die
     *         url kein Tabellendokument beschreibt. 
     * @author Matthias Benkmann (D-III-ITD 5.1)
     * TESTED
     */
    private XSpreadsheetDocument getCalcDoc(String url) throws UnavailableException
    {
      /**
       * Falls schon ein offenes Fenster mit der entsprechenden URL
       * existiert, liefere dieses zur�ck und setze {@link #calcDoc}.
       */
      XSpreadsheetDocument newCalcDoc = null;
      try{
        XSpreadsheetDocument spread;
        XEnumeration xenu = UNO.desktop.getComponents().createEnumeration();
        while(xenu.hasMoreElements())
        {
          spread = UNO.XSpreadsheetDocument(xenu.nextElement());
          if (spread != null && url.equals(UNO.XModel(spread).getURL()))
          {
            newCalcDoc = spread;
            break;
          }
        }
      }catch(Exception x)
      {
        Logger.error(x);
      }
      
      /**
       * Ansonsten versuchen wir das Dokument zu �ffnen.
       */
      if (newCalcDoc == null)
      {
        try{
          Object ss = UNO.loadComponentFromURL(url, false, true); //FIXME: Dragndrop-Problem
          newCalcDoc = UNO.XSpreadsheetDocument(ss);
          if (newCalcDoc == null) throw new UnavailableException(L.m("URL \"%1\" ist kein Tabellendokument", url));
        }catch(Exception x) 
        {
          throw new UnavailableException(x);
        }
      }

      getCalcDoc(newCalcDoc);
      return calcDoc;
    }

    /**
     * Setzt newCalcDoc als Datenquelle f�r den Seriendruck.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    private void getCalcDoc(XSpreadsheetDocument newCalcDoc)
    {
      try{
        calcUrl = UNO.XModel(newCalcDoc).getURL();
      }catch(Exception x) //typischerweise DisposedException  
      { 
        return;
      }
      if (calcUrl.length() == 0) calcUrl = null;
      sourceType = SOURCE_CALC;
      removeListeners(calcDoc); //falls altes calcDoc vorhanden, dort deregistrieren.
      calcDoc = newCalcDoc;
      setListeners(calcDoc);
      storeDatasourceSettings();
    }
    
    
    /**
     * Liefert die Namen aller nicht-leeren Tabellenbl�tter der aktuell
     * ausgew�hlten Datenquelle. Wenn keine Datenquelle ausgew�hlt ist, oder
     * es keine nicht-leere Tabelle gibt, so wird eine leere Liste geliefert.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    private List<String> getNamesOfNonEmptyTables()
    {
      try{
        switch(sourceType)
        {
          case SOURCE_CALC: return getNamesOfNonEmptyTables(getCalcDoc());
          case SOURCE_DB: return getNamesOfNonEmptyDbTables();
          default: return new Vector<String>();
        }
      }catch(Exception x)
      {
        Logger.error(x);
        return new Vector<String>();
      }
    }

    
    /**
     * Liefert die Namen aller Tabellen der aktuell
     * ausgew�hlten Datenquelle. Wenn keine Datenquelle ausgew�hlt ist, oder
     * es keine nicht-leere Tabelle gibt, so wird eine leere Liste geliefert.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    private List<String> getTableNames()
    {
      try{
        switch(sourceType)
        {
          case SOURCE_CALC: return getTableNames(getCalcDoc());
          case SOURCE_DB: return getDbTableNames();
          default: return new Vector<String>();
        }
      }catch(Exception x)
      {
        Logger.error(x);
        return new Vector<String>();
      }
    }
    
    /**
     * Liefert die Namen aller Tabellen der aktuell
     * ausgew�hlten OOo-Datenquelle. Wenn keine OOo-Datenquelle ausgew�hlt ist, oder
     * es keine nicht-leere Tabelle gibt, so wird eine leere Liste geliefert.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    private List<String> getDbTableNames()
    {
      return new Vector<String>();
    }
    
    /**
     * Liefert die Namen aller Tabellenbl�tter von calcDoc.
     * Falls calcDoc == null, wird eine leere Liste geliefert.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    private List<String> getTableNames(XSpreadsheetDocument calcDoc)
    {
      List<String> nonEmptyTableNames = new Vector<String>();
      if (calcDoc != null) 
      try{
        XSpreadsheets sheets = calcDoc.getSheets();
        String[] tableNames = sheets.getElementNames();
        nonEmptyTableNames.addAll(Arrays.asList(tableNames));
      }catch(Exception x)
      {
        Logger.error(x);
      }
      return nonEmptyTableNames;
    }
    
    /**
     * Liefert die Namen aller nicht-leeren Tabellen der aktuell
     * ausgew�hlten OOo-Datenquelle. Wenn keine OOo-Datenquelle ausgew�hlt ist, oder
     * es keine nicht-leere Tabelle gibt, so wird eine leere Liste geliefert.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    private List<String> getNamesOfNonEmptyDbTables()
    {
      return new Vector<String>();
    }
    
    
    /**
     * Liefert die Namen aller nicht-leeren Tabellenbl�tter von calcDoc.
     * Falls calcDoc == null wird eine leere Liste geliefert.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     * TESTED*/
    private List<String> getNamesOfNonEmptyTables(XSpreadsheetDocument calcDoc)
    {
      List<String> nonEmptyTableNames = new Vector<String>();
      if (calcDoc != null) 
      try{
        XSpreadsheets sheets = calcDoc.getSheets();
        String[] tableNames = sheets.getElementNames();
        SortedSet<Integer> columns = new TreeSet<Integer>();
        SortedSet<Integer> rows = new TreeSet<Integer>();
        for (int i = 0; i < tableNames.length; ++i)
        {
          try{
            XCellRangesQuery sheet = UNO.XCellRangesQuery(sheets.getByName(tableNames[i]));
            columns.clear();
            rows.clear();
            getVisibleNonemptyRowsAndColumns(sheet, columns, rows);
            if (columns.size() > 0 && rows.size() > 0)
            {
              nonEmptyTableNames.add(tableNames[i]);
            }
          }catch(Exception x)
          {
            Logger.error(x);
          }
        }
      }catch(Exception x)
      {
        Logger.error(x);
      }
      return nonEmptyTableNames;
    }
    
    private class MyCalcListener implements XCloseListener, XEventListener
    {

      public void queryClosing(EventObject arg0, boolean arg1) throws CloseVetoException
      {
      }

      public void notifyClosing(EventObject arg0)
      {
        Logger.debug(L.m("Calc-Datenquelle wurde unerwartet geschlossen"));
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            calcDoc = null;
          }
        });
      }

      public void disposing(EventObject arg0)
      {
        Logger.debug(L.m("Calc-Datenquelle wurde disposed()"));
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            calcDoc = null;
          }
        });
      }
    
      public void notifyEvent(com.sun.star.document.EventObject event)
      {  
        if (event.EventName.equals("OnSaveAsDone") && UnoRuntime.areSame(UNO.XInterface(event.Source), calcDoc))
        {
          javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
              calcUrl = UNO.XModel(calcDoc).getURL();
              Logger.debug(L.m("Speicherort der Tabelle hat sich ge�ndert: \"%1\"", calcUrl));
              storeDatasourceSettings();
            }
          });
        }
      }
    }

    /**
     * Versucht die Datenquelle in den Vordergrund zu holen und wird vom Button
     * "Tabelle bearbeiten" aufgerufen.
     * 
     * @author Christoph Lutz (D-III-ITD-5.1)
     */
    public void toFront()
    {
      if (sourceType == SOURCE_CALC)
      {
        if (UNO.XModel(calcDoc) != null)
        {
          XTopWindow win = UNO.XTopWindow(UNO.XModel(calcDoc)
              .getCurrentController().getFrame().getContainerWindow());
          win.toFront();
        }
      }
      // TODO: Behandlung der anderen Datenquellentypen
    }
    
    /**
     * Gibt Ressourcen frei und deregistriert Listener.
     * 
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public void dispose()
    {
      removeListeners(calcDoc);
    }    
  }
  
  private static class QueryResultsWithSchema implements QueryResults
  {
    protected QueryResults results;
    protected Set<String> schema;
    
    /**
     * Constructs an empty QueryResultsWithSchema.
     */
    public QueryResultsWithSchema()
    {
      results = new QueryResultsList(new ArrayList<Dataset>());
      schema = new HashSet<String>();
    }
    
    /**
     * Erzeugt ein neues QueryResultsWithSchema, das den Inhalt von res und das Schema
     * schema zusammenfasst. ACHTUNG! res und schema werden als Referenzen �bernommen.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public QueryResultsWithSchema(QueryResults res, Set<String> schema)
    {
      this.schema = schema;
      this.results = res;
    }

    public int size()
    {
      return results.size();
    }

    public Iterator<Dataset> iterator()
    {
      return results.iterator();
    }

    public boolean isEmpty()
    {
      return results.isEmpty();
    }
    
    public Set<String> getSchema() { return new HashSet<String>(schema);}
    
  }
  
  private static class CalcCellQueryResults implements QueryResults
  {
    /**
     * Bildet einen Spaltennamen auf den Index in dem zu dem Datensatz geh�renden
     * String[]-Array ab.
     */
    private Map<String, Integer> mapColumnNameToIndex;
   
    private List<Dataset> datasets = new ArrayList<Dataset>();
    
    public int size()
    {
      return datasets.size();
    }

    public Iterator<Dataset> iterator()
    {
      return datasets.iterator();
    }

    public boolean isEmpty()
    {
      return datasets.isEmpty();
    }

    public void setColumnNameToIndexMap(Map<String, Integer> mapColumnNameToIndex)
    {
      this.mapColumnNameToIndex = mapColumnNameToIndex;
    }

    public void addDataset(String[] data)
    {
      datasets.add(new MyDataset(data));
    }
    
    private class MyDataset implements Dataset
    {
      private String[] data;
      public MyDataset(String[] data)
      {
        this.data = data;
      }

      public String get(String columnName) throws ColumnNotFoundException
      {
        Number idx = mapColumnNameToIndex.get(columnName);
        if (idx == null) throw new ColumnNotFoundException(L.m("Spalte %1 existiert nicht!", columnName));
        return data[idx.intValue()];
      }

      public String getKey()
      {
        return "key";
      }
      
    }
    
  }
  
  private class MyWindowListener implements WindowListener
  {
    public void windowOpened(WindowEvent e) {}
    public void windowClosing(WindowEvent e) {abort(); }
    public void windowClosed(WindowEvent e) {}
    public void windowIconified(WindowEvent e) {}
    public void windowDeiconified(WindowEvent e) {}
    public void windowActivated(WindowEvent e) {}
    public void windowDeactivated(WindowEvent e){}   
    
  }
  
  private void abort()
  {
    mod.removeCoupledWindow(myFrame);
    /*
     * Wegen folgendem Java Bug (WONTFIX) 
     *   http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4259304
     * sind die folgenden 3 Zeilen n�tig, damit der MailMerge gc'ed werden
     * kann. Die Befehle sorgen daf�r, dass kein globales Objekt (wie z.B.
     * der Keyboard-Fokus-Manager) indirekt �ber den JFrame den MailMerge kennt.  
     */
    myFrame.removeWindowListener(oehrchen);
    myFrame.getContentPane().remove(0);
    myFrame.setJMenuBar(null);
    
    myFrame.dispose();
    myFrame = null;
    
    ds.dispose();
    
    if (abortListener != null)
      abortListener.actionPerformed(new ActionEvent(this, 0, ""));
  }
 
  public static void main(String[] args) throws Exception
  {
     UNO.init();
     Logger.init(Logger.ALL);
     XTextDocument doc = UNO.XTextDocument(UNO.desktop.getCurrentComponent());
     if (doc == null) 
     {
       System.err.println(L.m("Vordergrunddokument ist kein XTextDocument!"));
       System.exit(1);
     }
     
     MailMergeNew mm = new MailMergeNew(new TextDocumentModel(doc), null);
     
     while(mm.myFrame == null) Thread.sleep(1000);
     while(mm.myFrame != null) Thread.sleep(1000);
     System.exit(0);
  }
}