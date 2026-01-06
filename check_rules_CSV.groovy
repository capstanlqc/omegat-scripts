/* :name=QA Checks (CSV output) :description=QA script with GUI and CSV output
 * 
 * @author  Briac Pilpre
 * @author  Piotr Kulik
 * @author  Kos Ivantsov
 * @author  Didier Briel
 * @date    2018-06-28
 * @latest  2025-09-12
 *              check for: one source -> many targets, many sources -> one target
 *              select all button
 *              close on Esc
 * @version 0.8
 */

// if FALSE only current file will be checked
checkWholeProject = true
// 0 - segment number, 1 - rule, 2 - source, 3 - target, 4 - translation type (Default/Alternative)
defaultSortColumn = 0
// if TRUE column will be sorted in reverse
defaultSortOrderDescending = false

@Grab(group='org.apache.commons', module='commons-csv', version='1.10.0')
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter

import groovy.beans.Bindable
import groovy.swing.SwingBuilder
import groovy.transform.Field

import java.awt.BorderLayout as BL
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.Point
import java.awt.Rectangle
import java.awt.event.*
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import javax.swing.AbstractAction
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JTable
import javax.swing.JTextArea
import javax.swing.RowSorter
import javax.swing.RowSorter.SortKey
import javax.swing.SortOrder
import javax.swing.border.EmptyBorder
import javax.swing.event.*
import javax.swing.table.*
import javax.swing.table.TableCellRenderer

import org.languagetool.JLanguageTool
import org.languagetool.Language
import org.languagetool.rules.RuleMatch
import org.languagetool.rules.bitext.BitextRule
import org.languagetool.rules.bitext.DifferentLengthRule
import org.languagetool.rules.bitext.SameTranslationRule
import org.languagetool.tools.Tools

import org.omegat.core.Core
import org.omegat.languagetools.LanguageToolNativeBridge
import org.omegat.tokenizer.ITokenizer.StemmingMode

import static javax.swing.JOptionPane.*

import javax.swing.JTextArea
import javax.swing.border.EmptyBorder
import javax.swing.BorderFactory
import java.awt.Color
import javax.swing.table.TableCellRenderer

//External resources hack (use hardcoded strings if .properties file isn't found)
resBundle = { k,v ->
    try {
        v = res.getString(k)
    }
    catch (MissingResourceException e) {
        v
    }
}
//UI Strings
name="QA Checks New"
description="A simple QA script for OmegaT project"
title="Check Rules"
noProjMsg="Please try again after you open a project."
errors_count="Errors found: "
report_written="Report generated: "
segment="Segment"
rule="Rule"
targetLabel="Target"
sourceLabel="Source"
altDefaultLabel="Default/Alternative"
nameLanguageTools="LanguageTools rules"
nameLeadSpace="Whitespace - Leading"
nameTrailSpace="Whitespace - Trailing"
nameDoubleSpace="Doubled blanks"
nameDoubleWords="Doubled words"
nameTargetShorter="Shorter target"
nameTargetLonger="Longer target"
nameDiffPunctuation="Different punctuation"
nameDiffStartCase="Different start case"
nameEqualSourceTarget="Equal source & target"
nameUntranslated="Untranslated segment"
nameTagNumber="Tags - Number"
nameTagSpace="Tags - Spaces"
nameTagOrder="Tags - Order"
nameNumErr="Inconsistent numbers"
nameSpellErr="Spelling errors(s)"
nameMultipleTranslations="Multiple translations for same source"
nameSameTranslations="Same translation for different sources"
checkWholeProjectLabel="Whole project"
checkLeadSpaceLabel="Leading whitespace"
checkLanguageToolsLabel="LanguageTools rules"
checkTrailSpaceLabel="Trailing whitespace"
checkDoubleSpaceLabel="Doubled blanks"
checkDoubleWordsLabel="Doubled words"
checkTargetShorterLabel="Shorter target"
checkTargetLongerLabel="Longer target"
checkDiffPunctuationLabel="Punctuation at segment end"
checkDiffStartCaseLabel="Start case"
checkEqualSourceTargetLabel="Equal source & target"
checkUntranslatedLabel="Untranslated segments"
checkTagNumberLabel="Number of tags"
checkTagSpaceLabel="Spaces around tags"
checkTagOrderLabel="Sequence of tags"
checkNumErrLabel="Inconsistent numbers"
checkSpellErrLabel="Segments with spelling errors"
checkMultipleTranslationsLabel="Multiple translations of same source"
checkSameTranslationsLabel="Same translations of different sources"
refresh="Refresh"
toggleAll="Select/Deselect All"
// End of UI Strings
def mainWindow = Core.getMainWindow()

def prop = project.projectProperties
if (!prop) {
    final def title = resBundle("title", title)
    final def msg   = resBundle("noProjMsg", noProjMsg)
    console.clear()
    console.println(title + "\n${"-"*15}\n" + msg)
    showMessageDialog mainWindow, msg, title, INFORMATION_MESSAGE
    return
}

class TextAreaBorderRenderer extends JTextArea implements TableCellRenderer {
    TextAreaBorderRenderer() {
        setLineWrap(true)
        setWrapStyleWord(true)
        setOpaque(true)
        setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY))
        setMargin(new java.awt.Insets(2,2,2,2))
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                                                   int row, int column) {
        setText(value == null ? "" : value.toString())
        if (isSelected) {
            setBackground(table.getSelectionBackground())
            setForeground(table.getSelectionForeground())
        } else {
            setBackground(table.getBackground())
            setForeground(table.getForeground())
        }
//        setSize(table.getColumnModel().getColumn(column).getWidth(), Short.MAX_VALUE)
        int columnWidth = table.getColumnModel().getColumn(column).getWidth()
        setSize(columnWidth, Short.MAX_VALUE)
        int preferredHeight = getPreferredSize().height
        if (table.getRowHeight(row) != preferredHeight) {
            table.setRowHeight(row, preferredHeight)
        }
        return this
    }
}

class QACheckData {
    @Bindable data = []
}
public class IntegerComparator implements Comparator<Integer> {
    public int compare(Integer o1, Integer o2) {
        return o1 - o2
    }
}
def checker = Core.getSpellChecker()
def tokenizer = project.getTargetTokenizer()
swing = new SwingBuilder()
bRules = null
sourceLang = getLTLanguage(project.getProjectProperties().getSourceLanguage())
targetLang = getLTLanguage(project.getProjectProperties().getTargetLanguage())
sourceLt = null
targetLt = null
if (sourceLang != null) {
    sourceLt = getLanguageToolInstance(sourceLang)
}
if (targetLang != null) {
    targetLt = getLanguageToolInstance(targetLang)
}
if (sourceLt != null && targetLt != null) {
    bRules = getBiTextRules(sourceLang, targetLang)
}

//Enable or disable checks
checkLanguageTools = false
nameLanguageTools = resBundle("nameLanguageTools", nameLanguageTools)
checkLeadSpace = true
nameLeadSpace = resBundle("nameLeadSpace", nameLeadSpace)
checkTrailSpace = true
nameTrailSpace = resBundle("nameTrailSpace", nameTrailSpace)
checkDoubleSpace = true
nameDoubleSpace = resBundle("nameDoubleSpace", nameDoubleSpace)
checkDoubleWords = true
nameDoubleWords = resBundle("nameDoubleWords", nameDoubleWords)
checkTargetShorter = true
nameTargetShorter = resBundle("nameTargetShorter", nameTargetShorter)
checkTargetLonger = true
nameTargetLonger = resBundle("nameTargetLonger", nameTargetLonger)
checkDiffPunctuation = true
nameDiffPunctuation = resBundle("nameDiffPunctuation", nameDiffPunctuation)
checkDiffStartCase = true
nameDiffStartCase = resBundle("nameDiffStartCase", nameDiffStartCase)
checkEqualSourceTarget = true
nameEqualSourceTarget = resBundle("nameEqualSourceTarget", nameEqualSourceTarget)
checkUntranslated = true
nameUntranslated = resBundle("nameUntranslated", nameUntranslated)
checkTagNumber = true
nameTagNumber = resBundle("nameTagNumber", nameTagNumber)
checkTagSpace = true
nameTagSpace = resBundle("nameTagSpace", nameTagSpace)
checkTagOrder = true
nameTagOrder = resBundle("nameTagOrder", nameTagOrder)
checkNumErr = true
nameNumErr = resBundle("nameNumErr", nameNumErr)
checkSpellErr = false
nameSpellErr = resBundle("nameSpellErr", nameSpellErr)
checkMultipleTranslations = true
nameMultipleTranslations = resBundle("nameMultipleTranslations", nameMultipleTranslations)
checkSameTranslations = true
nameSameTranslations = resBundle("nameSameTranslations", nameSameTranslations)

// Declare these maps at script level so accessible inside QAcheck()
@Field def sourceToTranslations = [:].withDefault { new HashSet<String>() }
@Field def translationToSources = [:].withDefault { new HashSet<String>() }

def segment_count

// Ruleset map
ruleset = [
    (nameLeadSpace): { s, t ->  t =~ /^\s+/ },
    (nameTrailSpace): { s, t -> t =~ /\s+$/ },
    (nameDoubleSpace): { s, t -> t =~ /[\s\u00A0]{2}/ },
    (nameDoubleWords): { s, t -> t =~ /(?i)(\b\w+)\s+\1\b/ },
    (nameTargetShorter): { s, t -> if (t != QA_empty){(t.length() / s.length() * 100) < minCharLengthAbove} },
    (nameTargetLonger): { s, t -> if (t != QA_empty){(t.length() / s.length() * 100) > maxCharLengthAbove} },
    (nameDiffPunctuation): { s, t -> if (t != QA_empty){def s1 = s[-1], t1 = t[-1]; '.!?;:'.contains(s1) ? s1 != t1 : '.!?;:'.contains(t1)} },
    (nameDiffStartCase): { s, t -> if (t != QA_empty){def s1 = s[0] =~ /^\p{Lu}/ ? 'up' : 'low'; t1 = t[0] =~ /^\p{Lu}/ ? 'up' : 'low'; s1 != t1 } },
    (nameEqualSourceTarget): { s, t -> t == s },
    (nameUntranslated): { s, t -> t == QA_empty },
    (nameTagNumber): { s, t -> if (t != QA_empty){def tt = t.findAll(/<\/?[a-z]+[0-9]* ?\/?>/), st = s.findAll(/<\/?[a-z]+[0-9]* ?\/?>/); st.size() != tt.size()} },
    (nameTagSpace): { s, t -> if (t != QA_empty){def tt = t.findAll(/\s?<\/?[a-z]+[0-9]* ?\/?>\s?/), st = s.findAll(/\s?<\/?[a-z]+[0-9]* ?\/?>\s?/); if (st.size() == tt.size()) st.sort() != tt.sort()} },
    (nameTagOrder): { s, t -> if (t != QA_empty){def tt = t.findAll(/<\/?[a-z]+[0-9]* ?\/?>/), st = s.findAll(/<\/?[a-z]+[0-9]* ?\/?>/); if (st.size() == tt.size()) st != tt} },
    (nameNumErr): { s, t -> if (t != QA_empty){def tt = t.replaceAll(/<\/?[a-z]+[0-9]* ?\/?>/, ''), st = s.replaceAll(/<\/?[a-z]+[0-9]* ?\/?>/, ''), tn = tt.findAll(/\d+/), sn = st.findAll(/\d+/); sn != tn} },
    (nameSpellErr): { s, t -> if (t != QA_empty) {
        def spellerror = []
        tokenizer.tokenizeWords(t, StemmingMode.NONE).each {
            def (int a, int b) = [it.offset, it.offset + it.length]
            def word = t.substring( a, b )
            if (!checker.isCorrect(word)) {
                spellerror.add([word])
            }
        }
        spellerror.size()
    }}
]

def QAcheck() {
    // Clear previous collected errors and maps
    rules = ruleset.clone()
    maxCharLengthAbove=240
    minCharLengthAbove=40
    QA_empty = ''
    sourceToTranslations.clear()
    translationToSources.clear()
    // Remove unchecked rules
    if (!checkLeadSpace) {
        rules.remove(nameLeadSpace)
    }
    if (!checkTrailSpace) {
        rules.remove(nameTrailSpace)
    }
    if (!checkDoubleSpace) {
        rules.remove(nameDoubleSpace)
    }
    if (!checkDoubleWords) {
        rules.remove(nameDoubleWords)
    }
    if (!checkTargetShorter) {
        rules.remove(nameTargetShorter)
    }
    if (!checkTargetLonger) {
        rules.remove(nameTargetLonger)
    }
    if (!checkDiffPunctuation) {
        rules.remove(nameDiffPunctuation)
    }
    if (!checkDiffStartCase) {
        rules.remove(nameDiffStartCase)
    }
    if (!checkEqualSourceTarget) {
        rules.remove(nameEqualSourceTarget)
    }
    if (!checkUntranslated) {
        rules.remove(nameUntranslated)
    }
    if (!checkTagNumber) {
        rules.remove(nameTagNumber)
    }
    if (!checkTagSpace) {
        rules.remove(nameTagSpace)
    }
    if (!checkTagOrder) {
        rules.remove(nameTagOrder)
    }
    if (!checkNumErr) {
        rules.remove(nameNumErr)
    }
    if (!checkSpellErr) {
        rules.remove(nameSpellErr)
    }
    def runMultiSourceCheck = checkMultipleTranslations || checkSameTranslations
    if (runMultiSourceCheck) {
        rules.remove(nameMultipleTranslations)
        rules.remove(nameSameTranslations)
    }
    
    model = new QACheckData()
    segment_count = 0
    console.clear()
    console.println(resBundle("title", title)+"\n${'-'*15}")
    files = project.projectFiles
    if (!checkWholeProject) {
        files = project.projectFiles.subList(editor.@displayedFileIndex, editor.@displayedFileIndex + 1)
    }

    def segmentTranslations = [:] // segNum -> [source, target, altDefault]
    
    fileLoop:
    for (i in 0 ..< files.size()) {
        fi = files[i]
        for (j in 0 ..< fi.entries.size()) {
            if (java.lang.Thread.interrupted()) {
                break fileLoop
            }
            ste = fi.entries[j]
            source = ste.getSrcText()
            def info = project.getTranslationInfo(ste)
            target = info ? info.translation : null
            def altDefault = "N/A"
            if (target != null) {
                altDefault = info.defaultTranslation ? 'Default' : 'Alternative'
                if (target.length() == 0) {
                    target = QA_empty
                }
                sourceToTranslations[source].add(target)
                translationToSources[target].add(source)
            } else {
                target = QA_empty
                altDefault = "N/A"
            }
            segmentTranslations[ste.entryNum()] = [source, target, altDefault]
            fileName = fi.filePath.toString()
            // LanguageTools check
            if (checkLanguageTools) {
                for (RuleMatch rule : getRuleMatchesForEntry(source, target)) {
                    model.data.add([ seg: ste.entryNum(), rule: rule.getMessage(), source: source, target: target, altDef: altDefault ])
                    segment_count++
                }
            }
            rules.each { k, v ->
                if (rules[k](source, target)) {
                    model.data.add([ seg: ste.entryNum(), rule: k, source: source, target: target, altDef: altDefault ])
                    segment_count++
                }
            }
        }
    }
    if (runMultiSourceCheck) {
        if (checkMultipleTranslations) {
            sourceToTranslations.each { src, translationsSet ->
                if (translationsSet.size() > 1) {
                    segmentTranslations.findAll { it.value[0] == src }.each { segNum, data ->
                        def (s, t, altDef) = data
                        model.data.add([ seg: segNum, rule: nameMultipleTranslations, source: s, target: t, altDef: altDef ])
                        segment_count++
                    }
                }
            }
        }
        if (checkSameTranslations) {
            translationToSources.each { trans, sourcesSet ->
                if (trans != QA_empty && sourcesSet.size() > 1) {
                    segmentTranslations.findAll { it.value[1] == trans }.each { segNum, data ->
                        def (s, t, altDef) = data
                        model.data.add([ seg: segNum, rule: nameSameTranslations, source: s, target: t, altDef: altDef ])
                        segment_count++
                    }
                }
            }
        }
    }
    def projectDir = project.projectProperties.projectRoot
    def outputDir = new File(projectDir, "script_output")
    def reportFile = new File(outputDir, "QA_Report.csv")
    if (segment_count > 0) {
        try {
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }
            def out = new OutputStreamWriter(new FileOutputStream(reportFile, false), "UTF-8")
            CSVPrinter printer = new CSVPrinter(out, CSVFormat.DEFAULT.withHeader("Segment", "Rule", "Source", "Target", "Default/Alternative"))
            model.data.each { row ->
                printer.printRecord(row.seg, row.rule, row.source, row.target, row.altDef ?: "N/A")
            }
            printer.close()
            console.println(resBundle("report_written", report_written) + reportFile.getAbsolutePath())
        } catch (Exception e) {
            showMessageDialog null, "Failed to write QA report file:\n${e.message}", title, ERROR_MESSAGE
        }
    } else {
        if (reportFile.exists()) {
            reportFile.delete()
        }
    }
    console.print(resBundle("errors_count", errors_count) + segment_count)
}

def interfejs(locationxy = new Point(0, 0), width = 1200, height = 650, scrollpos = 0, sortColumn = defaultSortColumn, sortOrderDescending = defaultSortOrderDescending) {
    def frame
    frame = swing.frame(title: resBundle("title", title) + ". " + resBundle("errors_count", errors_count) + segment_count, minimumSize: [width, height], pack: true, show: true) {
        def tab
        def skroll
        skroll = scrollPane {      
            tab = table() {
                tableModel(list: model.data) {
                    propertyColumn(editable: true, header:resBundle("segment", segment), propertyName:'seg', minWidth: 80, maxWidth: 80, preferredWidth: 80,
                        cellEditor: new TableCellEditor()
                        {
                            public void cancelCellEditing()                             {   }
                            public boolean stopCellEditing()                            {   return false;   }
                            public Object getCellEditorValue()                          {   return value;   }
                            public boolean isCellEditable(EventObject anEvent)          {   return true;    }
                            public boolean shouldSelectCell(EventObject anEvent)        {   return true;   }
                            public void addCellEditorListener(CellEditorListener l)     {}
                            public void removeCellEditorListener(CellEditorListener l)  {}
                            public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column)
                            {
                                org.omegat.core.Core.getEditor().gotoEntry(value)
                            }
                        },
                        cellRenderer: new TableCellRenderer()
                        {
                            public Component getTableCellRendererComponent(JTable table,
                                Object value,
                                boolean isSelected,
                                boolean hasFocus,
                                int row,
                                int column)
                            {
                                def btn = new JButton()
                                btn.setText(value.toString())
                                return btn
                            }
                        }
                    )
                    propertyColumn(editable: false, header:resBundle("rule", rule), propertyName:'rule', minWidth: 140, preferredWidth: 180, cellRenderer: new TextAreaBorderRenderer())
                    propertyColumn(editable: false, header:resBundle("source", sourceLabel), propertyName:'source', minWidth: 220, preferredWidth: 320,
                        cellRenderer: new TextAreaBorderRenderer()
                    )
                    propertyColumn(editable: false, header:resBundle("target", targetLabel), propertyName:'target', minWidth: 220, preferredWidth: 320,
                        cellRenderer: new TextAreaBorderRenderer()
                    )
                    propertyColumn(editable: false, header: altDefaultLabel, propertyName: 'altDef', minWidth: 100, preferredWidth: 120, cellRenderer: new TextAreaBorderRenderer())
                }
            }
            tab.getTableHeader().setReorderingAllowed(false)
        }
        rowSorter = new TableRowSorter(tab.model)
        rowSorter.setComparator(0, new IntegerComparator())
        sortKeyz = new ArrayList<RowSorter.SortKey>()
        sortKeyz.add(new RowSorter.SortKey(sortColumn, sortOrderDescending ? SortOrder.DESCENDING : SortOrder.ASCENDING))
        rowSorter.setSortKeys(sortKeyz)
        tab.setRowSorter(rowSorter)
        skroll.getVerticalScrollBar().setValue(scrollpos)
        tab.scrollRectToVisible(new Rectangle (0, scrollpos, 1, scrollpos + 1))
        skroll.repaint()
        panel(constraints:BL.SOUTH) {
            gridBagLayout()
            def checkBoxes = [:]
            checkBoxes['checkWholeProject'] = checkBox(text:resBundle("checkWholeProject", checkWholeProjectLabel),
                selected: checkWholeProject,
                actionPerformed: { checkWholeProject = !checkWholeProject },
                constraints:gbc(gridx:0, gridy:0, weightx: 0.5, fill:GridBagConstraints.HORIZONTAL, insets:[0,5,0,0]))
            checkBoxes['checkSpellErr'] = checkBox(text:resBundle("checkSpellErr", checkSpellErrLabel),
                selected: checkSpellErr,
                actionPerformed: { checkSpellErr = !checkSpellErr },
                constraints:gbc(gridx:0, gridy:1, weightx: 0.5, fill:GridBagConstraints.HORIZONTAL, insets:[0,5,0,0]))
            checkBoxes['checkLanguageTools'] = checkBox(text:resBundle("checkLanguageTools", checkLanguageToolsLabel),
                selected: checkLanguageTools,
                actionPerformed: { checkLanguageTools = !checkLanguageTools },
                constraints:gbc(gridx:0, gridy:2, weightx: 0.5, fill:GridBagConstraints.HORIZONTAL, insets:[0,5,0,0]))

            checkBoxes['checkLeadSpace'] = checkBox(text:resBundle("checkLeadSpace", checkLeadSpaceLabel),
                selected: checkLeadSpace,
                actionPerformed: { checkLeadSpace = !checkLeadSpace },
                constraints:gbc(gridx:1, gridy:0, weightx: 0.5, fill:GridBagConstraints.HORIZONTAL, insets:[0,5,0,0]))
            checkBoxes['checkTrailSpace'] = checkBox(text:resBundle("checkTrailSpace", checkTrailSpaceLabel),
                selected: checkTrailSpace,
                actionPerformed: { checkTrailSpace = !checkTrailSpace },
                constraints:gbc(gridx:1, gridy:1, weightx: 0.5, fill:GridBagConstraints.HORIZONTAL, insets:[0,5,0,0]))
            checkBoxes['checkDoubleSpace'] = checkBox(text:resBundle("checkDoubleSpace", checkDoubleSpaceLabel),
                selected: checkDoubleSpace,
                actionPerformed: { checkDoubleSpace = !checkDoubleSpace },
                constraints:gbc(gridx:1, gridy:2, weightx: 0.5, fill:GridBagConstraints.HORIZONTAL, insets:[0,5,0,0]))
            checkBoxes['checkDoubleWords'] = checkBox(text:resBundle("checkDoubleWords", checkDoubleWordsLabel),
                selected: checkDoubleWords,
                actionPerformed: { checkDoubleWords = !checkDoubleWords },
                constraints:gbc(gridx:1, gridy:3, weightx: 0.5, fill:GridBagConstraints.HORIZONTAL, insets:[0,5,0,0]))
            checkBoxes['checkDiffStartCase'] = checkBox(text:resBundle("checkDiffStartCase", checkDiffStartCaseLabel),
                selected: checkDiffStartCase,
                actionPerformed: { checkDiffStartCase = !checkDiffStartCase },
                constraints:gbc(gridx:1, gridy:4, weightx: 0.5, fill:GridBagConstraints.HORIZONTAL, insets:[0,5,0,0]))
            checkBoxes['checkDiffPunctuation'] = checkBox(text:resBundle("checkDiffPunctuation", checkDiffPunctuationLabel),
                selected: checkDiffPunctuation,
                actionPerformed: { checkDiffPunctuation = !checkDiffPunctuation },
                constraints:gbc(gridx:1, gridy:5, weightx: 0.5, fill:GridBagConstraints.HORIZONTAL, insets:[0,5,0,0]))
            checkBoxes['checkNumErr'] = checkBox(text:resBundle("checkNumErr", checkNumErrLabel),
                selected: checkNumErr,
                actionPerformed: { checkNumErr = !checkNumErr },
                constraints:gbc(gridx:1, gridy:6, weightx: 0.5, fill:GridBagConstraints.HORIZONTAL, insets:[0,5,0,0]))

            checkBoxes['checkTargetShorter'] = checkBox(text:resBundle("checkTargetShorter", checkTargetShorterLabel),
                selected: checkTargetShorter,
                actionPerformed: { checkTargetShorter = !checkTargetShorter },
                constraints:gbc(gridx:2, gridy:0, weightx: 0.5, fill:GridBagConstraints.HORIZONTAL, insets:[0,5,0,0]))
            checkBoxes['checkTargetLonger'] = checkBox(text:resBundle("checkTargetLonger", checkTargetLongerLabel),
                selected: checkTargetLonger,
                actionPerformed: { checkTargetLonger = !checkTargetLonger },
                constraints:gbc(gridx:2, gridy:1, weightx: 0.5, fill:GridBagConstraints.HORIZONTAL, insets:[0,5,0,0]))
            checkBoxes['checkEqualSourceTarget'] = checkBox(text:resBundle("checkEqualSourceTarget", checkEqualSourceTargetLabel),
                selected: checkEqualSourceTarget,
                actionPerformed: { checkEqualSourceTarget = !checkEqualSourceTarget },
                constraints:gbc(gridx:2, gridy:2, weightx: 0.5, fill:GridBagConstraints.HORIZONTAL, insets:[0,5,0,0]))
            checkBoxes['checkUntranslated'] = checkBox(text:resBundle("checkUntranslated", checkUntranslatedLabel),
                selected: checkUntranslated,
                actionPerformed: { checkUntranslated = !checkUntranslated },
                constraints:gbc(gridx:2, gridy:3, weightx: 0.5, fill:GridBagConstraints.HORIZONTAL, insets:[0,5,0,0]))
            checkBoxes['checkTagNumber'] = checkBox(text:resBundle("checkTagNumber", checkTagNumberLabel),
                selected: checkTagNumber,
                actionPerformed: { checkTagNumber = !checkTagNumber },
                constraints:gbc(gridx:2, gridy:4, weightx: 0.5, fill:GridBagConstraints.HORIZONTAL, insets:[0,5,0,0]))
            checkBoxes['checkTagSpace'] = checkBox(text:resBundle("checkTagSpace", checkTagSpaceLabel),
                selected: checkTagSpace,
                actionPerformed: { checkTagSpace = !checkTagSpace },
                constraints:gbc(gridx:2, gridy:5, weightx: 0.5, fill:GridBagConstraints.HORIZONTAL, insets:[0,5,0,0]))
            checkBoxes['checkTagOrder'] = checkBox(text:resBundle("checkTagOrder", checkTagOrderLabel),
                selected: checkTagOrder,
                actionPerformed: { checkTagOrder = !checkTagOrder },
                constraints:gbc(gridx:2, gridy:6, weightx: 0.5, fill:GridBagConstraints.HORIZONTAL, insets:[0,5,0,0]))

            checkBoxes['checkMultipleTranslations'] = checkBox(text:resBundle("checkMultipleTranslations", checkMultipleTranslationsLabel),
                selected: checkMultipleTranslations,
                actionPerformed: { checkMultipleTranslations = !checkMultipleTranslations },
                constraints:gbc(gridx:3, gridy:0, weightx: 0.5, fill:GridBagConstraints.HORIZONTAL, insets:[0,5,0,0]))
            checkBoxes['checkSameTranslations'] = checkBox(text:resBundle("checkSameTranslations", checkSameTranslationsLabel),
                selected: checkSameTranslations,
                actionPerformed: { checkSameTranslations = !checkSameTranslations },
                constraints:gbc(gridx:3, gridy:1, weightx: 0.5, fill:GridBagConstraints.HORIZONTAL, insets:[0,5,0,0]))

            button(text: resBundle("toggleAll", toggleAll),
                toolTipText: "Toggle selection of all checkboxes",
                constraints: gbc(gridx: 0, gridy: 7, gridwidth: 4, weightx: 1.0, fill: GridBagConstraints.HORIZONTAL, insets: [5,5,5,5]),
                actionPerformed: {
                    boolean allSelected = checkBoxes.values().every { it.isSelected() }
                    checkBoxes.values().each { cb -> cb.setSelected(!allSelected) }
                    checkWholeProject = checkBoxes['checkWholeProject'].isSelected()
                    checkSpellErr = checkBoxes['checkSpellErr'].isSelected()
                    checkLanguageTools = checkBoxes['checkLanguageTools'].isSelected()
                    checkLeadSpace = checkBoxes['checkLeadSpace'].isSelected()
                    checkTrailSpace = checkBoxes['checkTrailSpace'].isSelected()
                    checkDoubleSpace = checkBoxes['checkDoubleSpace'].isSelected()
                    checkDoubleWords = checkBoxes['checkDoubleWords'].isSelected()
                    checkDiffStartCase = checkBoxes['checkDiffStartCase'].isSelected()
                    checkDiffPunctuation = checkBoxes['checkDiffPunctuation'].isSelected()
                    checkNumErr = checkBoxes['checkNumErr'].isSelected()
                    checkTargetShorter = checkBoxes['checkTargetShorter'].isSelected()
                    checkTargetLonger = checkBoxes['checkTargetLonger'].isSelected()
                    checkEqualSourceTarget = checkBoxes['checkEqualSourceTarget'].isSelected()
                    checkUntranslated = checkBoxes['checkUntranslated'].isSelected()
                    checkTagNumber = checkBoxes['checkTagNumber'].isSelected()
                    checkTagSpace = checkBoxes['checkTagSpace'].isSelected()
                    checkTagOrder = checkBoxes['checkTagOrder'].isSelected()
                    checkMultipleTranslations = checkBoxes['checkMultipleTranslations'].isSelected()
                    checkSameTranslations = checkBoxes['checkSameTranslations'].isSelected()
                })

            button(text:resBundle("refresh", refresh),
                actionPerformed: {
                    QAcheck()
                    locationxy = frame.getLocation()
                    sizerw = frame.getWidth()
                    sizerh = frame.getHeight()
                    skropos = skroll.getVerticalScrollBar().getValue()
                    sort = tab.getRowSorter().getSortKeys()[0]
                    frame.setVisible(false)
                    frame.dispose()
                    interfejs(locationxy, sizerw, sizerh, skropos, sort.getColumn(), sort.getSortOrder() == javax.swing.SortOrder.DESCENDING)},
                constraints:gbc(gridx:0, gridy:8, gridwidth:4, weightx:1.0, fill:GridBagConstraints.HORIZONTAL, insets:[5,5,5,5]))
        }
    }
    frame.getRootPane().getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW).put(
        javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
        "closeWindow"
    )

    frame.getRootPane().getActionMap().put("closeWindow", new AbstractAction() {
        @Override
        void actionPerformed(java.awt.event.ActionEvent e) {
            frame.dispose()
        }
    })

    if (mainWindow != null) {
        def mainBounds = mainWindow.getBounds()
        def frameSize = frame.getSize()
        int centerX = mainBounds.x + (mainBounds.width - frameSize.width) / 2
        int centerY = mainBounds.y + (mainBounds.height - frameSize.height) / 2
        frame.setLocation(centerX, centerY)
    } else {
        frame.setLocationRelativeTo(null)
    }
}

def getLanguageToolInstance(ltLang) {
    return new JLanguageTool(ltLang)
}
def getLTLanguage(lang) {
   return LanguageToolNativeBridge.getLTLanguage(lang)
}
def getBiTextRules(Language sourceLang, Language targetLang) {
    return Tools.getBitextRules(sourceLang, targetLang)
}
def getRuleMatchesForEntry(sourceText, translationText) {
    if (translationText == null) {
        return null
    }
    def ltSource = sourceLt
    def ltTarget = targetLt
    if (ltTarget == null || !ltTarget) {
        return null
    }
    List<RuleMatch> r = new ArrayList<RuleMatch>()
    List<RuleMatch> matches
    if (ltSource != null && !ltTarget && bRules != null) {
        matches = Tools.checkBitext(sourceText, translationText, ltSource, ltTarget, bRules)
    } else {
        matches = ltTarget.check(translationText)
    }
    for (RuleMatch match : matches) {
        r.add(match)
    }
    return r
}

QAcheck()
interfejs()
