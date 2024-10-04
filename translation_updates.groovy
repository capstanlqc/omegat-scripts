/* :name = Translation updates :description=
 * 
 * @author      Manuel Souto Pico, Kos Ivantsov
 * @date        2024-10-04
 * @version     0.0.1
 */

/* 
 * @versions: 
 * 0.0.1: 	Based on pseudo-translate script
 */

/* :name = Translation updates :description=
 * 
 * @author      Manuel Souto Pico, Kos Ivantsov
 * @date        2024-10-04
 * @version     0.0.1
 */

/* 
 * @versions: 
 * 0.0.1: 	Based on pseudo-translate script
 */

@Grab(group='org.apache.poi', module='poi-ooxml', version='5.2.3')

import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.FileInputStream

// ask user to provide the file if file below is not defined
def file = new FileInputStream("/home/souto/Documents/241001_Trailing_Zero_change-requests.xlsx")
// def file = new FileInputStream("/path/to/file.xlsx")

Workbook workbook = new XSSFWorkbook(file)
Sheet sheet = workbook.getSheet("updates")

Map<Integer, List<String>> sheetData = [:]
List<String> headers = []

if (sheet != null) {
	console.println("Sheet exists")
    sheet.each { Row row ->
    	def rowNum = row.getRowNum() // 0-based
    	// list to store string data from each row
    	List<String> rowData = []

        // header row
        if (rowNum == 0) {
            // console.println("processing header row: ${rowNum}")
            row.each { Cell cell ->
                if ((cell.stringCellValue != null) && (cell.stringCellValue != '')) {
                	headers.add(cell.stringCellValue)
                }
            }
        } else {
	        row.each { Cell cell ->
	            // convert numeric or boolean cells to string before usage
	            def cellValue
	            switch (cell.cellType) {
	                case CellType.NUMERIC:
	                    // convert numeric cell to string
	                    if (DateUtil.isCellDateFormatted(cell)) {
	                        cellValue = cell.dateCellValue.toString() // convert date to string
	                    } else {
	                        cellValue = cell.numericCellValue.toString() // convert number to string
	                    }
	                    break
	                case CellType.BOOLEAN:
	                    // convert boolean cell to string
	                    cellValue = cell.booleanCellValue.toString()
	                    break
	                case CellType.STRING:
	                    // no conversion needed for string cells
	                    cellValue = cell.stringCellValue
	                    break
	                case CellType.FORMULA:
	                    // evaluate formula and convert the result to string
	                    cellValue = cell.cellFormula // use the formula itself or evaluate it
	                    break
	                default:
	                    cellValue = ""
	            }
	            // add cell value to the row list
	            rowData.add(cellValue)
	        }
	        // add the row data to the map with rowNum as the key
        	sheetData[rowNum] = rowData

        }
    }

} else {
    console.println("Sheet 'updates' not found.")
}


file.close()
workbook.close()

// headers
console.println(headers)
console.println(sheetData)

// stored data
/*sheetData.each { rowNum, rowData ->
    console.println("Row ${rowNum}: ${rowData}")
}*/

def mergedMap = [:]
sheetData.each { id, values ->
    def tempMap = [:]

    headers.eachWithIndex { key, index ->
        tempMap[key] = values[index]
    }
    mergedMap[id] = tempMap
}

console.println(mergedMap)



// run some checks in the structure of the file: find the expected sheet and columns in it
 
def gui(){
	def segm_count = 0;

	project.allEntries.each { ste ->
	
		editor.gotoEntry(ste.entryNum())
		
		def sourceText = ste.getSrcText();
		def targetText = project.getTranslationInfo(ste) ? project.getTranslationInfo(ste).translation : null;

		def key = "tu4_0"
		def fpath = "batch/S24030067.html"

		def idProp = ste.key ? ste.key.id : null;
		def fileProp = ste.key ? ste.key.file : null;

		console.println("${sourceText} => ${targetText}")
		console.println("${idProp} + ${fileProp} ")

		// for each dict in mergedMap
		// if key, file, source match
		// then set target to target
		
		// search = "FRED"
		// replac = "MATT"
		// target = target.replace(search, replac)
			
		// segm_count++;
		// console.println(ste.entryNum() + ": '" + ste.getSrcText() + "' pseudo-translated as '" + target + "'")
		// editor.replaceEditText(target)
	}
	console.println(segm_count + " segments modified")
}