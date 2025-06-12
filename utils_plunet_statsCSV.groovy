/* :name=   Utils - Match Statistics in CSV :description=\
 *          Create a Plunet-compatible CSV file with match statistics for the project
 *
 *  @author:    Kos Ivantsov
 *  @version:   0.3
 *  @creation:  2025.06.10
 */

import org.omegat.core.data.ProtectedPart
import org.omegat.core.Core
import org.omegat.core.statistics.*

class StatsConsumer implements IStatsConsumer {
    MatchStatCounts matchCounts
    void showProgress(int percent) {}
    void finishData() {}
    void setTextData(String data) {}
    void setDataFile(String path) {}
    void setTable(String[] headers, String[][] data) {}
    void appendTable(String title, String[] headers, String[][] data) {}
    void appendTextData(String result) {}
    void setFilesTableData(String[] headers, String[][] filesData) {}
    void setProjectTableData(String[] headers, String[][] projectData) {}
    // This method is called by OmegaT to provide match stats
    void setMatchStatCounts(MatchStatCounts counts) { matchCounts = counts }
}

// Run statistics calculation
def consumer = new StatsConsumer()
def statsThread = new CalcMatchStatistics(consumer, false)
statsThread.start()
statsThread.join()

def project = Core.getProject()
if (!project) {
    console.println("No active project found")
    return
}

// Statistic files (OmegaT and the script output
def projectPath = project.projectProperties.getProjectRoot()
def projectName = new File(projectPath).getName().toString()
def srcCode = project.projectProperties.sourceLanguage
def tgtCode = project.projectProperties.targetLanguage

def statsFile = new File(project.projectProperties.projectInternal, "project_stats_match.txt")
def plunetStats = new File(project.projectProperties.projectInternal, "Statistics_${projectName}_${tgtCode}.csv")
def csvStats = new File(project.projectProperties.projectInternal, "project_stats_ice_match.csv")
if (!statsFile.exists()) {
    console.println("No match statistics file found. Please run Statistics in OmegaT first.")
    return
}

def lines = statsFile.readLines().findAll { it.trim() } // Remove empty lines

// Parse header
def headerLine = lines[0]
def headers = headerLine.split('\t').collect { it.trim() }.findAll { it }

// Prepare result map
def parsedStats = [:]

// Parse each data line
lines[1..-1].each { line ->
    def parts = line.split('\t').collect { it.trim() }
    if (!parts || parts.size() < 2) return
    def label = parts[0].replaceAll(/:$/, '')
    def values = parts[1..-1].collect { it.replace(',', '').isInteger() ? it.toInteger() : 0 }
    if (values.size() == headers.size()) {
        parsedStats[label] = [ : ]
        headers.eachWithIndex { h, i ->
            parsedStats[label][h] = values[i]
        }
    }
}

def altSeg = 0
def altWords = 0
def altChar = 0

project.allEntries.each {
    ste = it
    info = project.getTranslationInfo(ste)
    src = ste.getSrcText()
    targ = project.getTranslationInfo(ste) ? project.getTranslationInfo(ste).translation : null
    def isAlt = info.defaultTranslation ? false : true
    if (isAlt) {
        for (ProtectedPart pp : ste.getProtectedParts()) {
            src = src.replace(pp.getTextInSourceSegment(), pp.getReplacementWordsCountCalculation())
        }
        altSeg++
        altWords += Statistics.numberOfWords(src)
        altChar += Statistics.numberOfCharactersWithoutSpaces(src)
    }
}

def repSeg = parsedStats['Repetitions']['Segments']
def exactSeg = (parsedStats['Exact match']['Segments'] - altSeg)
def s95_100Seg = parsedStats['95%-100%']['Segments']
def s85_94Seg = parsedStats['85%-94%']['Segments']
def s75_84Seg = parsedStats['75%-84%']['Segments']
def s50_74Seg = parsedStats['50%-74%']['Segments']
def noMatchSeg = parsedStats['No match']['Segments']
def totalSeg = parsedStats['Total']['Segments']

def repWords = parsedStats['Repetitions']['Words']
def exactWords = (parsedStats['Exact match']['Words'] - altWords)
def w95_100Words = parsedStats['95%-100%']['Words']
def w85_94Words = parsedStats['85%-94%']['Words']
def w75_84Words = parsedStats['75%-84%']['Words']
def w50_74Words = parsedStats['50%-74%']['Words']
def noMatchWords = parsedStats['No match']['Words']
def totalWords = parsedStats['Total']['Words']

def repChar = parsedStats['Repetitions']['Characters (w/o spaces)']
def exactChar = (parsedStats['Exact match']['Characters (w/o spaces)'] - altChar)
def c95_100Char = parsedStats['95%-100%']['Characters (w/o spaces)']
def c85_94Char = parsedStats['85%-94%']['Characters (w/o spaces)']
def c75_84Char = parsedStats['75%-84%']['Characters (w/o spaces)']
def c50_74Char = parsedStats['50%-74%']['Characters (w/o spaces)']
def noMatchChar = parsedStats['No match']['Characters (w/o spaces)']
def totalChar = parsedStats['Total']['Characters (w/o spaces)']

def charPerWord = (totalChar / totalWords).round(2)

csvData = """,Segments,Words,Characters
Repetitions:,${repSeg},${repWords},${repChar}
ICE match:,${altSeg},${altWords},${altChar}
Exact match:,${exactSeg},${exactWords},${exactChar}
95%-100%:,${s95_100Seg},${w95_100Words},${c95_100Char}
85%-94%:,${s85_94Seg},${w85_94Words},${c85_94Char}
75%-84%:,${s75_84Seg},${w75_84Words},${c75_84Char}
50%-74%:,${s50_74Seg},${w50_74Words},${c50_74Char}
No match:,${noMatchSeg},${noMatchWords},${noMatchChar}
Total:,${totalSeg},${totalWords},${totalChar}
"""
plunetData = """;;X-translated;;;;;;;;101%;;;;;;;;Repetitions;;;;;;;;100%;;;;;;;;95% - 99%;;;;;;;;85% - 94%;;;;;;;;75% - 84%;;;;;;;;50% - 74%;;;;;;;;No match;;;;;;;;Fragments;;;;;;;;Total;;;;;;;;
Project path (language);Char/Word;Segments;Words;Characters;Asian characters;Tags;Reserved1;Reserved2;Reserved3;Segments;Words;Characters;Asian characters;Tags;Reserved1;Reserved2;Reserved3;Segments;Words;Characters;Asian characters;Tags;Reserved1;Reserved2;Reserved3;Segments;Words;Characters;Asian characters;Tags;Reserved1;Reserved2;Reserved3;Segments;Words;Characters;Asian characters;Tags;Reserved1;Reserved2;Reserved3;Segments;Words;Characters;Asian characters;Tags;Reserved1;Reserved2;Reserved3;Segments;Words;Characters;Asian characters;Tags;Reserved1;Reserved2;Reserved3;Segments;Words;Characters;Asian characters;Tags;Reserved1;Reserved2;Reserved3;Segments;Words;Characters;Asian characters;Tags;Reserved1;Reserved2;Reserved3;Segments;Words;Characters;Asian characters;Tags;Reserved1;Reserved2;Reserved3;Segments;Words;Characters;Asian characters;Tags;Reserved1;Reserved2;Reserved3;
"${projectPath} (${tgtCode})";${charPerWord};0;0;0;0;0;0;0;0;${altSeg};${altWords};${altChar};0;0;0;0;0;${repSeg};${repWords};${repChar};0;0;0;0;0;${exactSeg};${exactWords};${exactChar};0;0;0;0;0;${s95_100Seg};${w95_100Words};${c95_100Char};0;0;0;0;0;${s85_94Seg};${w85_94Words};${c85_94Char};0;0;0;0;0;${s75_84Seg};${w75_84Words};${c75_84Char};0;0;0;0;0;${s50_74Seg};${w50_74Words};${c50_74Char};0;0;0;0;0;${noMatchSeg};${noMatchWords};${noMatchChar};0;0;0;0;0;0;0;0;0;0;0;0;0;${totalSeg};${totalWords};${totalChar};0;0;0;0;0;
"""
plunetData = plunetData.replaceAll(/\r?\n/, '\r\n')
plunetStats.write(plunetData, "UTF-16")
csvStats.write(csvData, "UTF-16")
