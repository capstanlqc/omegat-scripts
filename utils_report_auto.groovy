//:name = Utils - Calculate auto and enforce :description=

import java.awt.datatransfer.StringSelection
import java.awt.Toolkit
import org.omegat.core.statistics.Statistics
import org.omegat.core.data.ProtectedPart

copyToClipboard = false
tsvExport = true

prop = project.projectProperties
if (!prop) {
	console.println("No project open")
	return
}

projectRoot = prop.projectRoot
projectName = prop.projectName

auto = []
enforced = []
autoWords = 0
enforceWords = 0

project.allEntries.each { ste ->
    info = project.getTranslationInfo(ste)
    linked = info.linked.toString()
    if (linked == "xAUTO") {
    	auto.add(ste)
    	autoSource = ste.srcText
	for (ProtectedPart pp : ste.getProtectedParts()) {
		autoSource = autoSource.replace(pp.getTextInSourceSegment(), pp.getReplacementWordsCountCalculation())
	}
	autoWords += Statistics.numberOfWords(autoSource)
    }
    if (linked == "xENFORCED") {
    	enforced.add(ste)
    	enforceSource = ste.srcText
	for (ProtectedPart pp : ste.getProtectedParts()) {
		enforceSource = enforceSource.replace(pp.getTextInSourceSegment(), pp.getReplacementWordsCountCalculation())
	}
	enforceWords += Statistics.numberOfWords(enforceSource)
    }
}
console.println("Translated from /tm/auto: ${autoWords} words in ${auto.size()} segments")
console.println("Translated from /tm/enforce: ${enforceWords} words in ${enforced.size()} segments")
report = """\"TM Folder\"\t\"Words\"\t\"Segments\"
\"tm/auto\"\t\"${autoWords}\"\t\"${auto.size()}\"
\"tm/enforce\"\t\"${enforceWords}\"\t\"${enforced.size()}\""""

if (copyToClipboard) {
	clipboard = Toolkit.getDefaultToolkit().getSystemClipboard()
	clipboard.setContents(new StringSelection(report.replaceAll(/\"/, "")), null)
}
if (tsvExport) {
	new File(projectRoot + projectName + "_auto_enforce.tsv").write(report, "UTF-8")
}
