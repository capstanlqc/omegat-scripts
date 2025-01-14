/* :name =   Write Source and Target to TXT :description = Write all source segments to a file
 * #output:  Writes 'source_target.txt' in the 'script_output' subfolder
 *           in the current project's root
 * 
 * @author:  Kos Ivantsov
 * @date:    2013-07-16
 * @version: 0.3
 * @changed: Kos -- 2025-01-14 -- disable GUI dialog if run in CLI mode
 *                                run only once in --mode=console-translate
 */

// Delimiters for source and target, and between segments
source_target_delim='\t' //tab
segment_delim="\n"*1 //one line break

import static javax.swing.JOptionPane.*
import static org.omegat.core.events.IProjectEventListener.PROJECT_CHANGE_TYPE.*
import static org.omegat.util.Platform.*


//// CLI or GUI probing
def echo
def cli
try {
    mainWindow.statusLabel.getText()
    echo = {
        k -> console.println(k.toString())
    }
    cli = false
} catch(Exception e) {
    echo = { k -> 
        println("\n~~~ Script output ~~~\n\n" + k.toString() + "\n\n^^^^^^^^^^^^^^^^^^^^^\n")
    }
    cli = true
}

// Abort if a project is not opened yet
def prop = project.projectProperties
def msg
def title = 'Source and Target to File'
if (!prop) {
    msg = 'Please try again after you open a project.'
    if (!cli) {
        showMessageDialog null, msg, title, INFORMATION_MESSAGE
    } else {
        echo(title + "\n" + "="*title.size() + "\n" + msg)
    }
    return
}

// Output file
def folder = prop.projectRoot + File.separator + 'script_output'
def fileloc = folder + File.separator + 'source_target.txt'
writefile = new File(fileloc)
if (! (new File(folder)).exists()) {
    (new File(folder)).mkdir()
}

// Closure that writes the TXT file
produceTXT = {
    writefileContent = new StringWriter()
    def count = 0
    
    files = project.projectFiles
    for (i in 0 ..< files.size()) {
        fi = files[i];
        marker = "+${'='*fi.filePath.size()}+\n"
        /* writefile.append("$marker|$fi.filePath|\n$marker", 'UTF-8') */
        writefileContent << "$fi.filePath\t$fi.filePath\n"
        for (j in 0 ..< fi.entries.size()) {
            ste = fi.entries[j]
            source = ste.getSrcText()
            target = project.getTranslationInfo(ste) ? project.getTranslationInfo(ste).translation : null
            if (target == '') {
                target = '<EMPTY>'
            }
            if (target == null ) {
                target = ''
            }
        writefileContent << source + source_target_delim + target+segment_delim
        count++
        }
    }
    writefile.write(writefileContent.toString(), 'UTF-8')
    msg = count + " segments written to\n" + writefile
}

// GUI dialog in GUI mode, console output in CLI mode
if (!cli) {
    produceTXT()
    echo(count + " segments written to " + writefile)
    showMessageDialog null, msg, title, INFORMATION_MESSAGE
    return
} else {
    if (eventType == COMPILE) {
        produceTXT()
        echo(title + "\n" + "="*title.size() + "\n" + msg)
        return
    }
}
